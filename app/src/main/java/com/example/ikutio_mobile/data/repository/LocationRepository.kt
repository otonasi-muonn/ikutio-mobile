package com.example.ikutio_mobile.data.repository

import com.example.ikutio_mobile.data.local.dao.LocationDao
import com.example.ikutio_mobile.data.local.entity.LocationPointEntity
import com.example.ikutio_mobile.data.remote.GameApiService
import com.example.ikutio_mobile.data.remote.dto.PathDataItem
import com.example.ikutio_mobile.data.remote.dto.PathDataRequest
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LocationRepository @Inject constructor(
    private val locationDao: LocationDao,
    private val gameApiService: GameApiService
) {

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

    suspend fun clearAllLocationPoints() {
        locationDao.deleteAllLocationPoints()
    }

    suspend fun sendPathDataToServer() {
        val localPoints = locationDao.getAllLocationPoints()

        if (localPoints.isEmpty()) {
            // 送信するデータがない場合は何もしない
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