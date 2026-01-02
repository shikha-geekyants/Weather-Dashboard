package com.weather.dashboard.ui.components

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.weather.dashboard.R
import com.weather.dashboard.util.ConnectionType

@Composable
fun NetworkDialog(
    connectionType: ConnectionType,
    isNetworkAvailable: Boolean,
    onDismiss: () -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isNoConnection = connectionType == ConnectionType.NONE || !isNetworkAvailable
    val isSlowConnection = (connectionType == ConnectionType.MOBILE || 
                           connectionType == ConnectionType.UNKNOWN) && isNetworkAvailable
    
    val title = if (isNoConnection) {
        stringResource(R.string.no_internet_connection)
    } else if (isSlowConnection) {
        stringResource(R.string.slow_network_connection)
    } else {
        stringResource(R.string.no_internet_connection)
    }
    
    val message = if (isNoConnection) {
        stringResource(R.string.check_internet_connection)
    } else if (isSlowConnection) {
        stringResource(R.string.slow_network_message)
    } else {
        stringResource(R.string.check_internet_connection)
    }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(title)
        },
        text = {
            Text(message)
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

