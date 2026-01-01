package com.weather.dashboard.di

import com.weather.dashboard.data.repository.WeatherRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
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
}

