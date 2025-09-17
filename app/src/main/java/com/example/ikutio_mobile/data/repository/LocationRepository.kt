package com.example.ikutio_mobile.data.repository

import android.util.Log
import com.example.ikutio_mobile.data.local.dao.LocationDao
import com.example.ikutio_mobile.data.local.entity.LocationPointEntity
import com.example.ikutio_mobile.data.remote.GameApiService
import com.example.ikutio_mobile.data.remote.MapsApiService
import com.example.ikutio_mobile.data.remote.dto.PathDataItem
import com.example.ikutio_mobile.data.remote.dto.PathDataRequest
import com.example.ikutio_mobile.utils.calculateTotalDistance
import com.example.ikutio_mobile.utils.calculatePathDataItemDistance
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton
import com.example.ikutio_mobile.data.repository.InsufficientPointsException
import com.example.ikutio_mobile.data.repository.NoProcessablePathDataException

private const val TAG = "LocationRepository"

@Singleton
class LocationRepository @Inject constructor(
    private val locationDao: LocationDao,
    private val gameApiService: GameApiService,
    private val mapsApiService: MapsApiService
) {

    private val _latestRawLocation = MutableStateFlow<Pair<Double, Double>?>(null)
    val latestRawLocation: StateFlow<Pair<Double, Double>?> = _latestRawLocation.asStateFlow()

    private val _processedPathDistance = MutableStateFlow<Double?>(null)
    val processedPathDistance: StateFlow<Double?> = _processedPathDistance.asStateFlow()

    fun updateLatestRawLocation(latitude: Double, longitude: Double) {
        _latestRawLocation.value = Pair(latitude, longitude)
    }

    suspend fun addLocationPoint(latitude: Double, longitude: Double, timestamp: Long) {
        val point = LocationPointEntity(
            latitude = latitude,
            longitude = longitude,
            timestamp = timestamp
        )
        locationDao.insertLocationPoint(point)
    }

    suspend fun getAllLocationPoints(): List<LocationPointEntity> {
        return locationDao.getAllLocationPoints()
    }

    suspend fun getTotalDistanceTraveled(): Double {
        val points = locationDao.getAllLocationPoints()
        return points.calculateTotalDistance()
    }

    suspend fun clearAllLocationPoints() {
        locationDao.deleteAllLocationPoints()
        _latestRawLocation.value = null
        _processedPathDistance.value = null
    }

    fun resetProcessedPathDistance() {
        _processedPathDistance.value = null
        Log.d(TAG, "Processed path distance reset to null.")
    }

    suspend fun sendPathDataToServer() {
        val localRawPoints = locationDao.getAllLocationPoints()

        if (localRawPoints.size < 3) {
            Log.w(TAG, "Insufficient points: ${localRawPoints.size}. Min 3 required.")
            _processedPathDistance.value = null
            throw InsufficientPointsException("記録された座標が3点未満です。経路処理を中断しました。")
        }

        val pathDataItemsForServer: List<PathDataItem> = try {
            val pathString = localRawPoints.joinToString("|") { "${it.latitude},${it.longitude}" }
            Log.d(TAG, "Requesting snapToRoads for ${localRawPoints.size} points. Path: $pathString")
            val snapResponse = mapsApiService.snapToRoads(path = pathString, interpolate = true)

            if (snapResponse.snappedPoints.isNullOrEmpty()) {
                Log.w(TAG, "snapToRoads returned no points or failed. Using raw points.")
                localRawPoints.map { entity ->
                    PathDataItem(entity.latitude, entity.longitude, formatTimestamp(entity.timestamp))
                }
            } else {
                Log.d(TAG, "snapToRoads successful. Processing ${snapResponse.snappedPoints.size} points.")
                snapResponse.snappedPoints.mapNotNull { snappedPoint ->
                    val lat = snappedPoint.location?.latitude
                    val lng = snappedPoint.location?.longitude
                    val originalIdx = snappedPoint.originalIndex
                    if (lat != null && lng != null && originalIdx != null && originalIdx >= 0 && originalIdx < localRawPoints.size) {
                        PathDataItem(lat, lng, formatTimestamp(localRawPoints[originalIdx].timestamp))
                    } else {
                        Log.w(TAG, "Invalid snapped point or originalIndex: $snappedPoint (idx: $originalIdx). Skipping.")
                        null
                    }
                }.ifEmpty {
                    Log.w(TAG, "All snapped points were invalid. Using raw points.")
                    localRawPoints.map { entity ->
                        PathDataItem(entity.latitude, entity.longitude, formatTimestamp(entity.timestamp))
                    }
                }
            }
        } catch (e: Exception) {
            if (e is InsufficientPointsException) throw e
            Log.e(TAG, "Error during snapToRoads/processing. Using raw points as fallback.", e)
            _processedPathDistance.value = null
            localRawPoints.map { entity ->
                PathDataItem(entity.latitude, entity.longitude, formatTimestamp(entity.timestamp))
            }
        }

        if (pathDataItemsForServer.isEmpty()) {
            Log.w(TAG, "No processable path data found. Local data NOT cleared.")
            _processedPathDistance.value = null
            throw NoProcessablePathDataException("処理できる有効な経路データが見つかりませんでした。")
        }

        val processedDistance = pathDataItemsForServer.calculatePathDataItemDistance()
        _processedPathDistance.value = processedDistance
        Log.d(TAG, "Processed path distance for server: $processedDistance meters from ${pathDataItemsForServer.size} points")

        Log.d(TAG, "Attempting to send ${pathDataItemsForServer.size} items to game server.")
        val request = PathDataRequest(pathData = pathDataItemsForServer)

        try {
            val gameApiResponse = gameApiService.sendPathData(request)
            if (gameApiResponse.isSuccessful) {
                Log.i(TAG, "Path data successfully sent. Clearing local points.")
                locationDao.deleteAllLocationPoints()
                _latestRawLocation.value = null
            } else {
                Log.e(TAG, "Failed to send path data. Code: ${gameApiResponse.code()}")
                throw Exception("Game API call failed: ${gameApiResponse.code()} - ${gameApiResponse.errorBody()?.string() ?: gameApiResponse.message()}")
            }
        } catch (e: Exception) {
            if (e is InsufficientPointsException || e is NoProcessablePathDataException) throw e
            Log.e(TAG, "Exception during sending path data.", e)
            throw e
        }
    }

    private fun formatTimestamp(timestamp: Long): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US)
        sdf.timeZone = TimeZone.getTimeZone("UTC")
        return sdf.format(Date(timestamp))
    }
}
