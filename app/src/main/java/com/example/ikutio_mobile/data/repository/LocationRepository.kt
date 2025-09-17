package com.example.ikutio_mobile.data.repository

import com.example.ikutio_mobile.data.local.dao.LocationDao
import com.example.ikutio_mobile.data.local.entity.LocationPointEntity
import com.example.ikutio_mobile.data.remote.GameApiService
import com.example.ikutio_mobile.data.remote.dto.PathDataItem
import com.example.ikutio_mobile.data.remote.dto.PathDataRequest
import com.example.ikutio_mobile.utils.calculateTotalDistance
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LocationRepository @Inject constructor(
    private val locationDao: LocationDao,
    private val gameApiService: GameApiService
) {

    private val _latestRawLocation = MutableStateFlow<Pair<Double, Double>?>(null)
    val latestRawLocation: StateFlow<Pair<Double, Double>?> = _latestRawLocation.asStateFlow()

    private val _latestNormalizedLocation = MutableStateFlow<Pair<Double, Double>?>(null)
    val latestNormalizedLocation: StateFlow<Pair<Double, Double>?> = _latestNormalizedLocation.asStateFlow()

    init {
        CoroutineScope(Dispatchers.IO).launch {
            val latestPoint = locationDao.getLatestLocationPoint()
            latestPoint?.let {
                _latestNormalizedLocation.value = Pair(it.latitude, it.longitude)
            }
        }
    }

    fun updateLatestRawLocation(latitude: Double, longitude: Double) {
        _latestRawLocation.value = Pair(latitude, longitude)
    }

    fun updateLatestNormalizedLocation(latitude: Double?, longitude: Double?) {
        if (latitude != null && longitude != null) {
            _latestNormalizedLocation.value = Pair(latitude, longitude)
        } else {
            _latestNormalizedLocation.value = null
        }
    }

    suspend fun addLocationPoint(latitude: Double, longitude: Double, timestamp: Long) {
        val point = LocationPointEntity(
            latitude = latitude,
            longitude = longitude,
            timestamp = timestamp
        )
        locationDao.insertLocationPoint(point)
        updateLatestNormalizedLocation(latitude, longitude)
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
        _latestNormalizedLocation.value = null
        _latestRawLocation.value = null
    }

    suspend fun sendPathDataToServer() {
        val localPoints = locationDao.getAllLocationPoints()

        if (localPoints.isEmpty()) {
            return
        }

        val pathDataItems = localPoints.map { entity ->
            PathDataItem(
                latitude = entity.latitude,
                longitude = entity.longitude,
                timestamp = formatTimestamp(entity.timestamp)
            )
        }

        val request = PathDataRequest(pathData = pathDataItems)
        val response = gameApiService.sendPathData(request)

        if (response.isSuccessful) {
            locationDao.deleteAllLocationPoints()
            _latestNormalizedLocation.value = null
            _latestRawLocation.value = null
        } else {
            throw Exception("API call failed with code: ${response.code()}")
        }
    }

    private fun formatTimestamp(timestamp: Long): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US)
        sdf.timeZone = TimeZone.getTimeZone("UTC")
        return sdf.format(Date(timestamp))
    }
}
