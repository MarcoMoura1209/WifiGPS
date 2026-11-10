package com.moura.wifigps.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import com.moura.wifigps.data.model.WifiNetwork

@Database(
    entities = [WifiNetwork::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun wifiNetworkDao(): WifiNetworkDao
}
