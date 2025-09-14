package com.example.ikutio_mobile.data.repository

import com.example.ikutio_mobile.data.local.dao.LocationDao
import com.example.ikutio_mobile.data.local.entity.LocationPointEntity
import javax.inject.Inject

// 位置情報データに関する操作をまとめるクラス
class LocationRepository @Inject constructor(
    private val locationDao: LocationDao // HiltがLocationDaoを注入してくれる
) {

    // 位置情報をデータベースに追加する
    suspend fun addLocationPoint(latitude: Double, longitude: Double, timestamp: Long) {
        val point = LocationPointEntity(
            latitude = latitude,
            longitude = longitude,
            timestamp = timestamp
        )
        locationDao.insertLocationPoint(point)
    }

    // 保存されている全ての位置情報を取得する
    suspend fun getAllLocationPoints(): List<LocationPointEntity> {
        return locationDao.getAllLocationPoints()
    }

    // 保存されている全ての位置情報を削除する
    suspend fun clearAllLocationPoints() {
        locationDao.deleteAllLocationPoints()
    }
}