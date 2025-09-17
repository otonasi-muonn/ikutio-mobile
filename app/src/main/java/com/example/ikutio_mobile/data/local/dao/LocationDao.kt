package com.example.ikutio_mobile.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.example.ikutio_mobile.data.local.entity.LocationPointEntity

@Dao
interface LocationDao {

    @Insert
    suspend fun insertLocationPoint(point: LocationPointEntity)

    @Query("SELECT * FROM location_points ORDER BY timestamp ASC")
    suspend fun getAllLocationPoints(): List<LocationPointEntity>

    // これを追加します
    @Query("SELECT * FROM location_points ORDER BY timestamp DESC LIMIT 1")
    suspend fun getLatestLocationPoint(): LocationPointEntity?

    @Query("DELETE FROM location_points")
    suspend fun deleteAllLocationPoints()
}
