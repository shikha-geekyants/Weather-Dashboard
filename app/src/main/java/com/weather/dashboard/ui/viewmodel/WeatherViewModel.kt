package com.weather.dashboard.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.weather.dashboard.data.repository.WeatherRepository
import com.weather.dashboard.domain.model.LoadingType
import com.weather.dashboard.domain.model.State
import com.weather.dashboard.domain.model.WeatherError
import com.weather.dashboard.ui.state.WeatherUiState
import com.weather.dashboard.util.ConnectionType
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
        val DEFAULT_CITIES = listOf("London", "New York", "Tokyo") // add in consts file
    }

    init {
        // Monitor network connectivity
        networkMonitor.isConnected
            .onEach { isConnected ->
                val connectionType = networkMonitor.getConnectionType()
                val isSlowConnection = connectionType == ConnectionType.MOBILE || 
                                       connectionType == ConnectionType.UNKNOWN
                val isNoConnection = connectionType == ConnectionType.NONE
                
                _uiState.update { 
                    it.copy(
                        isNetworkAvailable = isConnected,
                        connectionType = connectionType
                    ) 
                }
                
                // Show dialog for slow connection (when network is available but slow)
                // or no connection
                if (isNoConnection || !isConnected || (isSlowConnection && isConnected)) {
                    _uiState.update { it.copy(showNetworkDialog = true) }
                }
            }
            .catch { }
            .launchIn(viewModelScope)

        // Check initial network state
        val initialNetworkStatus = networkMonitor.getNetworkStatus()
        val isInitialSlowConnection = initialNetworkStatus.connectionType == ConnectionType.MOBILE || 
                                     initialNetworkStatus.connectionType == ConnectionType.UNKNOWN
        val isInitialNoConnection = initialNetworkStatus.connectionType == ConnectionType.NONE
        
        _uiState.update { 
            it.copy(
                isNetworkAvailable = initialNetworkStatus.isConnected,
                connectionType = initialNetworkStatus.connectionType
            ) 
        }
        
        // Show dialog if no connection or slow connection (when network is available)
        if (!initialNetworkStatus.isConnected || isInitialNoConnection || 
            (isInitialSlowConnection && initialNetworkStatus.isConnected)) {
            _uiState.update { it.copy(showNetworkDialog = true) }
        }

        if (initialNetworkStatus.isConnected) {
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
        val currentState = _uiState.value
        if (!currentState.isNetworkAvailable || currentState.isSlowConnection) {
            _uiState.update { it.copy(showNetworkDialog = true) }
            return
        }
        if (!currentState.isRefreshing && !currentState.isLoading) {
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
        val networkStatus = networkMonitor.getNetworkStatus()
        val isSlowConnection = networkStatus.connectionType == ConnectionType.MOBILE || 
                               networkStatus.connectionType == ConnectionType.UNKNOWN
        
        if (networkStatus.isConnected && !isSlowConnection) {
            _uiState.update { 
                it.copy(
                    showNetworkDialog = false, 
                    isNetworkAvailable = true,
                    connectionType = networkStatus.connectionType
                ) 
            }
            loadWeather(_currentCity.value)
            if (!isAutoRefreshActive) {
                startAutoRefresh()
            }
        } else {
            _uiState.update { 
                it.copy(
                    showNetworkDialog = true,
                    connectionType = networkStatus.connectionType
                ) 
            }
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
                WeatherError.NetworkError("No internet connection available") // add strings in strings file
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
                    message = e.message ?: "Error searching for city",// add strings in strings file
                    cause = e
                )
            )
        }
    }

    private fun loadWeather(city: String, isManualRefresh: Boolean = false) {
        // Check network before making API call
        val networkStatus = networkMonitor.getNetworkStatus()
        val isSlowConnection = (networkStatus.connectionType == ConnectionType.MOBILE || 
                               networkStatus.connectionType == ConnectionType.UNKNOWN) && networkStatus.isConnected
        
        // If no connection, show dialog and don't proceed
        if (!networkStatus.isConnected) {
            _uiState.update { 
                it.copy(
                    isLoading = false,
                    isRefreshing = false,
                    showNetworkDialog = true,
                    isNetworkAvailable = false,
                    connectionType = networkStatus.connectionType
                )
            }
            return
        }
        
        // If slow connection, show dialog but still proceed (warn user)
        if (isSlowConnection) {
            _uiState.update { 
                it.copy(
                    showNetworkDialog = true,
                    connectionType = networkStatus.connectionType
                )
            }
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

