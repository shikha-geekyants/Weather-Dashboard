package com.weather.dashboard.ui.screen

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.weather.dashboard.R
import com.weather.dashboard.domain.model.WeatherData
import com.weather.dashboard.ui.components.WeatherCard
import kotlinx.coroutines.delay

private sealed class SearchErrorType {
    data class NoCitiesFound(val query: String) : SearchErrorType()
    data class SearchError(val message: String) : SearchErrorType()
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CitySearchScreen(
    onBackClick: () -> Unit,
    onCitySelected: (String) -> Unit,
    searchCities: suspend (String) -> Result<List<WeatherData>>,
    modifier: Modifier = Modifier
) {
    var searchQuery by remember { mutableStateOf("") }
    var searchResults by remember { mutableStateOf<List<WeatherData>>(emptyList()) }
    var isSearching by remember { mutableStateOf(false) }
    var searchErrorType by remember { mutableStateOf<SearchErrorType?>(null) }

    // Format error message in composable context
    val searchError = when (val error = searchErrorType) {
        is SearchErrorType.NoCitiesFound -> stringResource(R.string.no_cities_found, error.query)
        is SearchErrorType.SearchError -> stringResource(R.string.error_searching, error.message)
        null -> null
    }

    // Debounce search
    LaunchedEffect(searchQuery) {
        if (searchQuery.isBlank()) {
            searchResults = emptyList()
            searchErrorType = null
            return@LaunchedEffect
        }

        if (searchQuery.length < 2) {
            searchResults = emptyList()
            return@LaunchedEffect
        }

        delay(1000) // Wait 1 second after user stops typing to avoid too frequent API calls
        isSearching = true
        searchErrorType = null

        val result = searchCities(searchQuery)
        result.fold(
            onSuccess = { results ->
                searchResults = results
                if (results.isEmpty()) {
                    searchErrorType = SearchErrorType.NoCitiesFound(searchQuery)
                }
            },
            onFailure = { error ->
                val errorMessage = if (error is com.weather.dashboard.domain.model.WeatherError) {
                    error.message ?: "Unknown error"
                } else {
                    error.message ?: "Unknown error"
                }
                searchErrorType = SearchErrorType.SearchError(errorMessage)
                searchResults = emptyList()
            }
        )
        isSearching = false
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.search_city)) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = stringResource(R.string.back)
                        )
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
            // Search Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                placeholder = { Text(stringResource(R.string.enter_city_name)) },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = stringResource(R.string.search)
                    )
                },
                singleLine = true,
                enabled = !isSearching
            )

            // Loading indicator
            if (isSearching) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }

            // Error message
            searchError?.let { error ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    )
                ) {
                    Text(
                        text = error,
                        modifier = Modifier.padding(16.dp),
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                }
            }

            // Search Results
            if (searchResults.isNotEmpty()) {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(searchResults) { weatherData ->
                        CitySearchResultItem(
                            weatherData = weatherData,
                            onClick = {
                                onCitySelected(weatherData.cityName)
                                onBackClick()
                            }
                        )
                    }
                }
            } else if (searchQuery.isBlank()) {
                // Empty state
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                        Text(
                            text = stringResource(R.string.search_for_city),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                        Text(
                            text = stringResource(R.string.enter_at_least_2_characters),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun CitySearchResultItem(
    weatherData: WeatherData,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // City Name - same style as main screen
        Text(
            text = weatherData.cityName,
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold
        )
        
        // Weather Card - exact same component as main screen
        WeatherCard(weatherData = weatherData)
    }
}

