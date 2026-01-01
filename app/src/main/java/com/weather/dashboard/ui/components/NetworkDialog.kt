package com.weather.dashboard.ui.components

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
fun NetworkDialog(
    onDismiss: () -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("No Internet Connection")
        },
        text = {
            Text("Please check your internet connection and try again. Make sure Wi-Fi or mobile data is turned on.")
        },
        confirmButton = {
            Button(onClick = onRetry) {
                Text("Check Again")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Dismiss")
            }
        },
        modifier = modifier
    )
}

