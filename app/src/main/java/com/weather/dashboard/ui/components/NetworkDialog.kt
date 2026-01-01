package com.weather.dashboard.ui.components

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.weather.dashboard.R

@Composable
fun NetworkDialog(
    onDismiss: () -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(stringResource(R.string.no_internet_connection))
        },
        text = {
            Text(stringResource(R.string.check_internet_connection))
        },
        confirmButton = {
            Button(onClick = onRetry) {
                Text(stringResource(R.string.check_again))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.dismiss))
            }
        },
        modifier = modifier
    )
}

