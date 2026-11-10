package com.moura.wifigps.data.repository

import com.moura.wifigps.data.db.WifiNetworkDao
import com.moura.wifigps.data.model.WifiNetwork
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

class WifiRepository(
    private val wifiNetworkDao: WifiNetworkDao
) {
    suspend fun insertNetwork(network: WifiNetwork): Long =
        wifiNetworkDao.insert(network)

    fun observeNetworks(): Flow<List<WifiNetwork>> =
        wifiNetworkDao.observeAll()

    fun observeNetworksSortedByDistance(
        latitude: Double,
        longitude: Double
    ): Flow<List<WifiNetwork>> =
        wifiNetworkDao.observeAll().map { networks ->
            networks.sortedBy { network ->
                distanceBetweenMeters(
                    latitude,
                    longitude,
                    network.latitude,
                    network.longitude
                )
            }
        }

    suspend fun getNetwork(id: Long): WifiNetwork? =
        wifiNetworkDao.getById(id)

    suspend fun updateNetwork(network: WifiNetwork): Int =
        wifiNetworkDao.update(network.copy(updatedAt = System.currentTimeMillis()))

    suspend fun deleteNetwork(network: WifiNetwork): Int =
        wifiNetworkDao.delete(network)

    private fun distanceBetweenMeters(
        startLatitude: Double,
        startLongitude: Double,
        endLatitude: Double,
        endLongitude: Double
    ): Double {
        val latitudeDelta = Math.toRadians(endLatitude - startLatitude)
        val longitudeDelta = Math.toRadians(endLongitude - startLongitude)
        val startLatitudeRadians = Math.toRadians(startLatitude)
        val endLatitudeRadians = Math.toRadians(endLatitude)
        val haversine = sin(latitudeDelta / 2) * sin(latitudeDelta / 2) +
            cos(startLatitudeRadians) * cos(endLatitudeRadians) *
            sin(longitudeDelta / 2) * sin(longitudeDelta / 2)
        val boundedHaversine = haversine.coerceIn(0.0, 1.0)

        return 2 * EARTH_RADIUS_METERS *
            atan2(sqrt(boundedHaversine), sqrt(1 - boundedHaversine))
    }

    private companion object {
        const val EARTH_RADIUS_METERS = 6_371_000.0
    }
}
