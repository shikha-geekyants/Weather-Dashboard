package com.weather.dashboard.ui.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
fun CityTabs(
    cities: List<String>,
    selectedCity: String,
    onCitySelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    TabRow(
        selectedTabIndex = cities.indexOf(selectedCity).coerceAtLeast(0),
        modifier = modifier
    ) {
        cities.forEachIndexed { index, city ->
            Tab(
                selected = city == selectedCity,
                onClick = { onCitySelected(city) },
                text = { Text(city) }
            )
        }
    }
}

