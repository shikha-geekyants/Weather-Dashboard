package com.weather.dashboard

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.weather.dashboard.ui.screen.WeatherScreen
import com.weather.dashboard.ui.theme.WeatherDashboardTheme
import com.weather.dashboard.ui.viewmodel.WeatherViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val viewModel: WeatherViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            WeatherDashboardTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val uiState = viewModel.uiState.collectAsStateWithLifecycle()
                    val currentCity = viewModel.currentCity.collectAsStateWithLifecycle()
                    WeatherScreen(
                        uiState = uiState.value,
                        cities = WeatherViewModel.DEFAULT_CITIES,
                        currentCity = currentCity.value,
                        onCitySelected = { viewModel.selectCity(it) },
                        onRefresh = { viewModel.refreshWeather() },
                        onRetry = { viewModel.retry() },
                        onDismissNetworkDialog = { viewModel.dismissNetworkDialog() },
                        onCheckNetwork = { viewModel.checkNetworkAndRetry() }
                    )
                }
            }
        }
    }

    override fun onPause() {
        super.onPause()
        viewModel.pauseAutoRefresh()
    }

    override fun onResume() {
        super.onResume()
        viewModel.resumeAutoRefresh()
    }
}

