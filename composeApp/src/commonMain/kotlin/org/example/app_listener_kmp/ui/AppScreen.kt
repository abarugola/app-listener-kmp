package org.example.app_listener_kmp.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import org.example.app_listener_kmp.domain.AppInfo
import org.example.app_listener_kmp.platform.blockApp
import org.example.app_listener_kmp.platform.unblockApp

@Composable
fun AppScreen(apps: List<AppInfo>) {
    var blockedApps by remember { mutableStateOf(setOf<String>())}
    var showContent by remember { mutableStateOf(false) }

    MaterialTheme {

        Column(
            modifier = Modifier
                .background(MaterialTheme.colorScheme.primaryContainer)
                .safeContentPadding()
                .fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Column(modifier = Modifier.padding(vertical = 4.dp)) {
                TopAppsContent(apps)
            }
            Button(onClick = { showContent = !showContent }) {
                Text("Complete list")
            }
            AnimatedVisibility(showContent) {
                LazyColumn (
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(8.dp),
                ) {
                    items(
                        items = apps,
                        key = { it.packageName}
                    ) { app ->
                        AppItem(
                            app = app,
                            isBlocked = blockedApps.contains(app.packageName),
                            onToggleBlock = { shouldBlock ->
                                if (shouldBlock) {
                                    // Agregamos al set y arrancamos el servicio
                                    blockedApps = blockedApps + app.packageName
                                    blockApp(app.packageName)
                                } else {
                                    // removemos del set
                                    blockedApps = blockedApps - app.packageName
                                    unblockApp(app.packageName)
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun AppItem(
    app: AppInfo,
    isBlocked: Boolean,
    onToggleBlock: (Boolean) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp, vertical = 4.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isBlocked)
                MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)
            else
                MaterialTheme.colorScheme.surface
        )
    ) {
        Row (
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icono de la app
            Box(modifier = Modifier.size(44.dp)) {
                AppIcon(app.icon)
            }

            Spacer(modifier = Modifier.width(12.dp))

            // weight(1f) hace que tome todo el espacio disponible,
            //empujando el tiempo y el toogle hacia los extremos
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = app.name,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 1,
                    // si el nombre es muy largo agrega ... al final
                    overflow = TextOverflow.Ellipsis
                    )

                Text(
                    text = formatTime(app.usageTime),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }

            Switch(
                checked = isBlocked,
                onCheckedChange = onToggleBlock,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = MaterialTheme.colorScheme.error,
                    checkedTrackColor = MaterialTheme.colorScheme.errorContainer
                )
            )
        }
    }



        //add column to show usage time

}

@Composable
fun TopAppsContent(apps: List<AppInfo>) {

    val topApps = apps
        .sortedByDescending { it.usageTime }
        .take(5)

    val max = topApps.maxOfOrNull { it.usageTime } ?: 1L

    Card (
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(8.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {

            Text(
                text = "Top Apps Usage",
                style = MaterialTheme.typography.titleLarge
            )

            Spacer(modifier = Modifier.height(12.dp))

            topApps.forEachIndexed { index, app ->

                val ratio = app.usageTime.toFloat() / max

                val animatedRatio by animateFloatAsState(
                    targetValue = ratio,
                    label = "barAnimation"
                )

                val isTop = index == 0

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                        .animateContentSize()
                ) {

                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {

                        // ICONO (opcional)
                        AppIcon(app.icon)

                        Spacer(modifier = Modifier.width(8.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = app.name,
                                style = if (isTop)
                                    MaterialTheme.typography.titleMedium
                                else
                                    MaterialTheme.typography.bodyMedium
                            )

                            Text(
                                text = formatTime(app.usageTime),
                                style = MaterialTheme.typography.bodySmall
                            )
                        }

                        Text(
                            text = "${(ratio * 100).toInt()}%",
                            style = MaterialTheme.typography.labelMedium
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    // Fondo barra
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(12.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color.LightGray.copy(alpha = 0.3f))
                    ) {

                        // Barra animada
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(animatedRatio)
                                .fillMaxHeight()
                                .background(
                                    brush = Brush.horizontalGradient(
                                        colors = if (isTop)
                                            listOf(Color(0xFF7B61FF), Color(0xFF5AC8FA))
                                        else
                                            listOf(
                                                MaterialTheme.colorScheme.primary,
                                                MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
                                            )
                                    )
                                )
                        )
                    }
                }
            }
        }
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