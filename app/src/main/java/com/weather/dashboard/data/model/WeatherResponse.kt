package com.weather.dashboard.data.model

import com.google.gson.annotations.SerializedName

data class WeatherResponse(
    @SerializedName("name") val cityName: String,
    @SerializedName("main") val main: Main,
    @SerializedName("weather") val weather: List<Weather>,
    @SerializedName("wind") val wind: Wind,
    @SerializedName("dt") val timestamp: Long
)

data class Main(
    @SerializedName("temp") val temperature: Double,
    @SerializedName("humidity") val humidity: Int
)

data class Weather(
    @SerializedName("main") val condition: String,
    @SerializedName("description") val description: String
)

data class Wind(
    @SerializedName("speed") val speed: Double
)

