package org.example.app_listener_kmp.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.example.app_listener_kmp.domain.AppInfo

@Composable
fun AppScreen(apps: List<AppInfo>) {

    val sortedApps = apps.sortedBy { it.name.lowercase() }
    MaterialTheme {
        var showContent by remember { mutableStateOf(false) }
        Column(
            modifier = Modifier
                .background(MaterialTheme.colorScheme.primaryContainer)
                .safeContentPadding()
                .fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            //solicitar permiso con el boton
            Button(onClick = { showContent = !showContent }) {
                Text("Click me!")
            }
            AnimatedVisibility(showContent) {
                LazyColumn (
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(8.dp),
                ) {
                    items(
                        items = sortedApps,
                        key = { it.packageName}
                    ) { app ->
                        AppItem(app)
                    }
                }
            }
        }
    }
}

@Composable
fun AppItem(app: AppInfo) {
    Row (
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically

    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
        ) {
            AppIcon(app.icon)
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column {
            Text(text = app.name)

            Text(
                text = app.packageName,
                style = MaterialTheme.typography.bodySmall
            )
        }

        Column {
            Text(text = formatTime(app.usageTime))
        }

        //add column to show usage time
    }
}

fun formatTime(ms: Long): String {
    val minutes = (ms / 1000) / 60
    val hours = (minutes / 60)

    return if (hours > 0) {
        "${hours}h ${minutes % 60}m"
    } else {
        "${minutes}m"
    }
}