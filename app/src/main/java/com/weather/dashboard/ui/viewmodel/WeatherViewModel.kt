package com.weather.dashboard.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.weather.dashboard.data.repository.WeatherRepository
import com.weather.dashboard.domain.model.LoadingType
import com.weather.dashboard.domain.model.State
import com.weather.dashboard.domain.model.WeatherError
import com.weather.dashboard.ui.state.WeatherUiState
import com.weather.dashboard.util.NetworkMonitor
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class WeatherViewModel @Inject constructor(
    private val weatherRepository: WeatherRepository,
    private val networkMonitor: NetworkMonitor
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
        // Monitor network connectivity
        networkMonitor.isConnected
            .onEach { isConnected ->
                _uiState.update { it.copy(isNetworkAvailable = isConnected) }
                if (!isConnected) {
                    _uiState.update { it.copy(showNetworkDialog = true) }
                }
            }
            .catch { }
            .launchIn(viewModelScope)

        // Check initial network state
        val initialNetworkState = networkMonitor.checkCurrentConnectivity()
        _uiState.update { it.copy(isNetworkAvailable = initialNetworkState) }
        if (!initialNetworkState) {
            _uiState.update { it.copy(showNetworkDialog = true) }
        }

        if (initialNetworkState) {
            loadWeather(_currentCity.value)
            startAutoRefresh()
        }
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
        if (!_uiState.value.isNetworkAvailable) {
            _uiState.update { it.copy(showNetworkDialog = true) }
            return
        }
        if (!_uiState.value.isRefreshing && !_uiState.value.isLoading) {
            loadWeather(_currentCity.value, isManualRefresh = true)
        }
    }

    fun retry() {
        if (!_uiState.value.isNetworkAvailable) {
            _uiState.update { it.copy(showNetworkDialog = true) }
            return
        }
        _uiState.update { it.copy(error = null) }
        loadWeather(_currentCity.value)
    }

    fun dismissNetworkDialog() {
        _uiState.update { it.copy(showNetworkDialog = false) }
    }

    fun checkNetworkAndRetry() {
        if (networkMonitor.checkCurrentConnectivity()) {
            _uiState.update { it.copy(showNetworkDialog = false, isNetworkAvailable = true) }
            loadWeather(_currentCity.value)
            if (!isAutoRefreshActive) {
                startAutoRefresh()
            }
        } else {
            _uiState.update { it.copy(showNetworkDialog = true) }
        }
    }

    fun showSearchScreen() {
        _uiState.update { it.copy(showSearchScreen = true) }
    }

    fun hideSearchScreen() {
        _uiState.update { it.copy(showSearchScreen = false) }
    }

    suspend fun searchCities(query: String): Result<List<com.weather.dashboard.domain.model.WeatherData>> {
        if (query.length < 2) return Result.success(emptyList())
        if (!networkMonitor.checkCurrentConnectivity()) {
            return Result.failure(
                WeatherError.NetworkError("No internet connection available")
            )
        }

        return try {
            // Try to get weather for the city to verify it exists and get weather data
            val state = weatherRepository.getWeather(query)
                .first { it is State.Success || it is State.Error }
            
            when (state) {
                is State.Success -> {
                    Result.success(listOf(state.data))
                }
                is State.Error -> {
                    Result.success(emptyList())
                }
                is State.Loading -> {
                    Result.success(emptyList())
                }
                is State.Idle -> {
                    Result.success(emptyList())
                }
            }
        } catch (e: Exception) {
            Result.failure(
                WeatherError.NetworkError(
                    message = e.message ?: "Error searching for city",
                    cause = e
                )
            )
        }
    }

    private fun loadWeather(city: String, isManualRefresh: Boolean = false) {
        // Check network before making API call
        if (!networkMonitor.checkCurrentConnectivity()) {
            _uiState.update { 
                it.copy(
                    isLoading = false,
                    isRefreshing = false,
                    showNetworkDialog = true,
                    isNetworkAvailable = false
                )
            }
            return
        }

        viewModelScope.launch {
            _uiState.update { state ->
                state.copy(
                    isLoading = !isManualRefresh && state.weatherData == null,
                    isRefreshing = isManualRefresh,
                    error = null
                )
            }

            weatherRepository.getWeather(city).collect { state ->
                when (state) {
                    is State.Loading -> {
                        val isLoading = state.type == LoadingType.LOADER && !isManualRefresh
                        val isRefreshing = state.type == LoadingType.REFRESH || isManualRefresh
                        _uiState.update { currentState ->
                            currentState.copy(
                                isLoading = isLoading && currentState.weatherData == null,
                                isRefreshing = isRefreshing,
                                error = null
                            )
                        }
                    }
                    is State.Success -> {
                        _uiState.update {
                            it.copy(
                                weatherData = state.data,
                                isLoading = false,
                                isRefreshing = false,
                                error = null,
                                lastUpdated = System.currentTimeMillis(),
                                isNetworkAvailable = true
                            )
                        }
                    }
                    is State.Error -> {
                        val isNetworkError = state.message.contains("internet", ignoreCase = true) ||
                            state.message.contains("connection", ignoreCase = true) ||
                            state.code == 0
                        
                        val weatherError = WeatherError.NetworkError(
                            message = state.message,
                            cause = null
                        )
                        
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                isRefreshing = false,
                                error = weatherError,
                                showNetworkDialog = isNetworkError,
                                isNetworkAvailable = !isNetworkError
                            )
                        }
                    }
                    is State.Idle -> {
                        // Idle state - no action needed
                    }
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

