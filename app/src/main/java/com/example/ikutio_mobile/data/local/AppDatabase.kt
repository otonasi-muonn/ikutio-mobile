package com.example.ikutio_mobile.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.example.ikutio_mobile.data.local.dao.LocationDao
import com.example.ikutio_mobile.data.local.entity.LocationPointEntity

@Database(
    entities = [LocationPointEntity::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun locationDao(): LocationDao
}