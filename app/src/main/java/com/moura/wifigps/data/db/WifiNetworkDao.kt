package com.moura.wifigps.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.moura.wifigps.data.model.WifiNetwork
import kotlinx.coroutines.flow.Flow

@Dao
interface WifiNetworkDao {
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(network: WifiNetwork): Long

    @Query("SELECT * FROM wifi_network ORDER BY ssid COLLATE NOCASE ASC")
    fun observeAll(): Flow<List<WifiNetwork>>

    @Query("SELECT * FROM wifi_network WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): WifiNetwork?

    @Update
    suspend fun update(network: WifiNetwork): Int

    @Delete
    suspend fun delete(network: WifiNetwork): Int
}
