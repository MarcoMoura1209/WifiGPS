package com.moura.wifigps.data.model

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "wifi_network",
    indices = [
        Index(value = ["ssid"]),
        Index(value = ["latitude", "longitude"])
    ]
)
data class WifiNetwork(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val ssid: String,
    val notes: String = "",
    val latitude: Double,
    val longitude: Double,
    val locationLabel: String = "",
    val passwordEncrypted: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
