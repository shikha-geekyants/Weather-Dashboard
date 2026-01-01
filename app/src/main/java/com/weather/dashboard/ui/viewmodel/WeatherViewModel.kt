package com.weather.dashboard.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.weather.dashboard.data.repository.WeatherRepository
import com.weather.dashboard.domain.model.WeatherError
import com.weather.dashboard.ui.state.WeatherUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class WeatherViewModel @Inject constructor(
    private val weatherRepository: WeatherRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(WeatherUiState())
    val uiState: StateFlow<WeatherUiState> = _uiState.asStateFlow()

    private val _currentCity = MutableStateFlow(DEFAULT_CITIES.first())
    val currentCity: StateFlow<String> = _currentCity.asStateFlow()

    private var autoRefreshJob: Job? = null
    private var isAutoRefreshActive = false

    companion object {
        const val AUTO_REFRESH_INTERVAL_MS = 30_000L // 30 seconds
        val DEFAULT_CITIES = listOf("London", "New York", "Tokyo")
    }

    init {
        loadWeather(_currentCity.value)
        startAutoRefresh()
    }

    fun selectCity(city: String) {
        if (city != _currentCity.value) {
            _currentCity.value = city
            loadWeather(city)
            stopAutoRefresh()
            startAutoRefresh()
        }
    }

    fun refreshWeather() {
        if (!_uiState.value.isRefreshing && !_uiState.value.isLoading) {
            loadWeather(_currentCity.value, isManualRefresh = true)
        }
    }

    fun retry() {
        _uiState.update { it.copy(error = null) }
        loadWeather(_currentCity.value)
    }

    private fun loadWeather(city: String, isManualRefresh: Boolean = false) {
        viewModelScope.launch {
            _uiState.update { state ->
                state.copy(
                    isLoading = !isManualRefresh && state.weatherData == null,
                    isRefreshing = isManualRefresh,
                    error = null
                )
            }

            weatherRepository.getWeather(city)
                .onSuccess { weatherData ->
                    _uiState.update {
                        it.copy(
                            weatherData = weatherData,
                            isLoading = false,
                            isRefreshing = false,
                            error = null,
                            lastUpdated = System.currentTimeMillis()
                        )
                    }
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            isRefreshing = false,
                            error = if (error is WeatherError) error else WeatherError.UnknownError(
                                error.message ?: "Unknown error occurred",
                                error
                            )
                        )
                    }
                }
        }
    }

    private fun startAutoRefresh() {
        if (isAutoRefreshActive) return
        isAutoRefreshActive = true

        autoRefreshJob = viewModelScope.launch {
            while (isAutoRefreshActive) {
                delay(AUTO_REFRESH_INTERVAL_MS)
                if (isAutoRefreshActive) {
                    loadWeather(_currentCity.value, isManualRefresh = false)
                }
            }
        }
    }

    private fun stopAutoRefresh() {
        isAutoRefreshActive = false
        autoRefreshJob?.cancel()
        autoRefreshJob = null
    }

    fun pauseAutoRefresh() {
        stopAutoRefresh()
    }

    fun resumeAutoRefresh() {
        if (!isAutoRefreshActive) {
            startAutoRefresh()
        }
    }

    override fun onCleared() {
        super.onCleared()
        stopAutoRefresh()
    }
}

