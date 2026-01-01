package com.weather.dashboard.di

import android.content.Context
import com.weather.dashboard.data.repository.WeatherRepository
import com.weather.dashboard.util.NetworkMonitor
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Named
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object RepositoryModule {

    @Provides
    @Singleton
    fun provideWeatherRepository(
        weatherApiService: com.weather.dashboard.data.api.WeatherApiService,
        @Named("api_key") apiKey: String
    ): WeatherRepository {
        return WeatherRepository(weatherApiService, apiKey)
    }

    @Provides
    @Singleton
    fun provideNetworkMonitor(
        @ApplicationContext context: Context
    ): NetworkMonitor {
        return NetworkMonitor(context)
    }
}

