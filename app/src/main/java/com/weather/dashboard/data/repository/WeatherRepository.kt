package com.weather.dashboard.data.repository

import com.weather.dashboard.data.api.WeatherApiService
import com.weather.dashboard.data.model.WeatherResponse
import com.weather.dashboard.domain.model.LoadingType
import com.weather.dashboard.domain.model.State
import com.weather.dashboard.domain.model.WeatherData
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow
import javax.inject.Inject

class WeatherRepository @Inject constructor(
    private val apiService: WeatherApiService,
    private val apiKey: String
) {
    companion object {
        // create a consts file and add all the constants there
        private const val API_CONSTANT_GET_WEATHER = "GET_WEATHER"
    }

    suspend fun getWeather(city: String): Flow<State<WeatherData>> = flow {
        emit(State.Loading(apiConstant = API_CONSTANT_GET_WEATHER, type = LoadingType.LOADER))
        val response = apiService.getCurrentWeather(city, apiKey)
        emit(State.Success(data = response.toWeatherData(), apiConstant = API_CONSTANT_GET_WEATHER))
    }.catch { e ->
        val (message, code) = when (e) {
            is retrofit2.HttpException -> {
                // add static string in string.xml for this

                val errorMessage = when (e.code()) {
                    401 -> "Invalid API key. Please verify your OpenWeatherMap API key is correct and activated. New keys may take up to 2 hours to activate."
                    404 -> "City not found. Please check the city name."
                    429 -> "API rate limit exceeded. Please try again later."
                    else -> "HTTP error ${e.code()}: ${e.message()}"
                }
                errorMessage to e.code()
            }
            is java.net.UnknownHostException -> {
                "No internet connection. Please check your network settings and ensure the emulator/device has internet access." to 0
            }
            is java.net.SocketTimeoutException -> {
                "Connection timeout. Please check your internet connection and try again." to 0
            }
            is java.io.IOException -> {
                "Network error: ${e.message ?: "Unable to connect to the server. Please check your internet connection."}" to 0
            }
            else -> {
                (e.message ?: "Unknown error occurred") to 0
            }
        }
        emit(State.Error(message = message, code = code, apiConstant = API_CONSTANT_GET_WEATHER))
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

