package com.moura.wifigps.di

import android.content.Context
import androidx.room.Room
import com.moura.wifigps.data.db.AppDatabase
import com.moura.wifigps.data.db.WifiNetworkDao
import com.moura.wifigps.data.repository.WifiRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    @Provides
    @Singleton
    fun provideAppDatabase(
        @ApplicationContext context: Context
    ): AppDatabase =
        Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "wifi_gps_database"
        ).build()

    @Provides
    fun provideWifiNetworkDao(database: AppDatabase): WifiNetworkDao =
        database.wifiNetworkDao()

    @Provides
    fun provideWifiRepository(wifiNetworkDao: WifiNetworkDao): WifiRepository =
        WifiRepository(wifiNetworkDao)
}
