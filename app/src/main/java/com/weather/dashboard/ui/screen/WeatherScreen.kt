package com.weather.dashboard.ui.screen

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.weather.dashboard.domain.model.WeatherData
import com.weather.dashboard.ui.components.CityTabs
import com.weather.dashboard.ui.components.NetworkDialog
import com.weather.dashboard.ui.components.WeatherCard
import com.weather.dashboard.ui.components.WeatherErrorView
import com.weather.dashboard.ui.components.WeatherLoadingView
import com.weather.dashboard.ui.state.WeatherUiState
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WeatherScreen(
    uiState: WeatherUiState,
    cities: List<String>,
    currentCity: String,
    onCitySelected: (String) -> Unit,
    onRefresh: () -> Unit,
    onRetry: () -> Unit,
    onDismissNetworkDialog: () -> Unit,
    onCheckNetwork: () -> Unit,
    onShowSearch: () -> Unit,
    onHideSearch: () -> Unit,
    searchCities: suspend (String) -> List<WeatherData>,
    modifier: Modifier = Modifier
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Weather Dashboard") },
                actions = {
                    IconButton(
                        onClick = onShowSearch
                    ) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Search City"
                        )
                    }
                    IconButton(
                        onClick = onRefresh,
                        enabled = !uiState.isRefreshing && !uiState.isLoading
                    ) {
                        if (uiState.isRefreshing) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Refresh"
                            )
                        }
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // City Selection Tabs
            CityTabs(
                cities = cities,
                selectedCity = currentCity,
                onCitySelected = onCitySelected,
                modifier = Modifier.fillMaxWidth()
            )

            // Content
            when {
                uiState.isLoading && uiState.weatherData == null -> {
                    WeatherLoadingView(modifier = Modifier.fillMaxSize())
                }
                uiState.hasError && uiState.weatherData == null -> {
                    WeatherErrorView(
                        error = uiState.error,
                        onRetry = onRetry,
                        modifier = Modifier.fillMaxSize()
                    )
                }
                uiState.hasData -> {
                    WeatherContent(
                        weatherData = uiState.weatherData!!,
                        isRefreshing = uiState.isRefreshing,
                        lastUpdated = uiState.lastUpdated,
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                    )
                }
                else -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("No data available")
                    }
                }
            }
        }
    }

    // Network Dialog
    if (uiState.showNetworkDialog) {
        NetworkDialog(
            onDismiss = onDismissNetworkDialog,
            onRetry = onCheckNetwork
        )
    }

    // Search Screen
    if (uiState.showSearchScreen) {
        CitySearchScreen(
            onBackClick = onHideSearch,
            onCitySelected = onCitySelected,
            searchCities = searchCities
        )
    }
}

@Composable
fun WeatherContent(
    weatherData: WeatherData,
    isRefreshing: Boolean,
    lastUpdated: Long?,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Refreshing indicator
        if (isRefreshing) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(16.dp),
                    strokeWidth = 2.dp
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Refreshing...",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }

        // City Name
        Text(
            text = weatherData.cityName,
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )

        // Weather Card
        WeatherCard(weatherData = weatherData)

        // Last Updated
        lastUpdated?.let {
            Text(
                text = "Last updated: ${formatTimestamp(it)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                textAlign = TextAlign.Center
            )
        }
    }
}

private fun formatTimestamp(timestamp: Long): String {
    val sdf = SimpleDateFormat("MMM dd, yyyy HH:mm:ss", Locale.getDefault())
    return sdf.format(Date(timestamp))
}

