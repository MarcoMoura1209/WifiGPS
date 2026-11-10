package com.moura.wifigps.data.repository

import com.moura.wifigps.data.db.WifiNetworkDao
import com.moura.wifigps.data.model.WifiNetwork
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test

class WifiRepositoryTest {
    @Test
    fun observeNetworksSortedByDistance_emitsNearestFirst() = runBlocking {
        val networks = listOf(
            WifiNetwork(ssid = "Far", latitude = 1.0, longitude = 0.0),
            WifiNetwork(ssid = "Near", latitude = 0.1, longitude = 0.0)
        )
        val repository = WifiRepository(FakeWifiNetworkDao(networks))

        val sortedNetworks = repository.observeNetworksSortedByDistance(
            latitude = 0.0,
            longitude = 0.0
        ).first()

        assertEquals(listOf("Near", "Far"), sortedNetworks.map(WifiNetwork::ssid))
    }

    private class FakeWifiNetworkDao(
        private val networks: List<WifiNetwork>
    ) : WifiNetworkDao {
        override suspend fun insert(network: WifiNetwork): Long =
            throw UnsupportedOperationException()

        override fun observeAll(): Flow<List<WifiNetwork>> =
            kotlinx.coroutines.flow.flowOf(networks)

        override suspend fun getById(id: Long): WifiNetwork? =
            throw UnsupportedOperationException()

        override suspend fun update(network: WifiNetwork): Int =
            throw UnsupportedOperationException()

        override suspend fun delete(network: WifiNetwork): Int =
            throw UnsupportedOperationException()
    }
}
