package com.weather.dashboard.ui.state

import com.weather.dashboard.domain.model.WeatherData
import com.weather.dashboard.domain.model.WeatherError

data class WeatherUiState(
    val weatherData: WeatherData? = null,
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val error: WeatherError? = null,
    val lastUpdated: Long? = null,
    val isNetworkAvailable: Boolean = true,
    val showNetworkDialog: Boolean = false
) {
    val hasError: Boolean get() = error != null
    val hasData: Boolean get() = weatherData != null && !isLoading && !isRefreshing
}

