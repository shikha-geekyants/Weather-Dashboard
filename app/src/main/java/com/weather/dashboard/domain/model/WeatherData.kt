package com.weather.dashboard.domain.model

data class WeatherData(
    val cityName: String,
    val temperature: Double,
    val condition: String,
    val description: String,
    val humidity: Int,
    val windSpeed: Double,
    val lastUpdated: Long
)

sealed class WeatherError(message: String, cause: Throwable? = null) : Exception(message, cause) {
    class NetworkError(message: String, cause: Throwable? = null) : WeatherError(message, cause)
    class UnknownError(message: String, cause: Throwable? = null) : WeatherError(message, cause)
}

