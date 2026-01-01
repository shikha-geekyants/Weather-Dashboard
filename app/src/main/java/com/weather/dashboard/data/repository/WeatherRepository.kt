package com.weather.dashboard.data.repository

import com.weather.dashboard.data.api.WeatherApiService
import com.weather.dashboard.data.model.WeatherResponse
import com.weather.dashboard.domain.model.WeatherData
import com.weather.dashboard.domain.model.WeatherError
import javax.inject.Inject

class WeatherRepository @Inject constructor(
    private val apiService: WeatherApiService,
    private val apiKey: String
) {
    suspend fun getWeather(city: String): Result<WeatherData> {
        return try {
            val response = apiService.getCurrentWeather(city, apiKey)
            Result.success(response.toWeatherData())
        } catch (e: retrofit2.HttpException) {
            val errorMessage = when (e.code()) {
                401 -> "Invalid API key. Please verify your OpenWeatherMap API key is correct and activated. New keys may take up to 2 hours to activate."
                404 -> "City not found. Please check the city name."
                429 -> "API rate limit exceeded. Please try again later."
                else -> "HTTP error ${e.code()}: ${e.message()}"
            }
            Result.failure(
                WeatherError.NetworkError(
                    message = errorMessage,
                    cause = e
                )
            )
        } catch (e: java.net.UnknownHostException) {
            Result.failure(
                WeatherError.NetworkError(
                    message = "No internet connection. Please check your network settings and ensure the emulator/device has internet access.",
                    cause = e
                )
            )
        } catch (e: java.net.SocketTimeoutException) {
            Result.failure(
                WeatherError.NetworkError(
                    message = "Connection timeout. Please check your internet connection and try again.",
                    cause = e
                )
            )
        } catch (e: java.io.IOException) {
            Result.failure(
                WeatherError.NetworkError(
                    message = "Network error: ${e.message ?: "Unable to connect to the server. Please check your internet connection."}",
                    cause = e
                )
            )
        } catch (e: Exception) {
            Result.failure(
                WeatherError.NetworkError(
                    message = e.message ?: "Unknown error occurred",
                    cause = e
                )
            )
        }
    }
}

private fun WeatherResponse.toWeatherData(): WeatherData {
    return WeatherData(
        cityName = cityName,
        temperature = main.temperature,
        condition = weather.firstOrNull()?.condition ?: "Unknown",
        description = weather.firstOrNull()?.description ?: "",
        humidity = main.humidity,
        windSpeed = wind.speed,
        lastUpdated = timestamp
    )
}

