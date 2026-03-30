package org.example.app_listener_kmp.ui

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import org.example.app_listener_kmp.domain.AppInfo
import org.example.app_listener_kmp.platform.AppProvider
import org.example.app_listener_kmp.platform.getUsageStats
import org.example.app_listener_kmp.platform.hasUsageStatsPermission
import org.example.app_listener_kmp.platform.provideContext
import org.example.app_listener_kmp.platform.requestUsageStatsPermission
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner


var cachedApps: List<AppInfo>? = null

@Composable
fun MainContent() {

    val context = LocalContext.current
    var hasPermission by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        provideContext(context)
        hasPermission = hasUsageStatsPermission(context)
        if (!hasPermission) {
            requestUsageStatsPermission(context)
        }
    }

    var apps by remember { mutableStateOf<List<AppInfo>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var reloadTrigger by remember { mutableStateOf(0)}

    LaunchedEffect(reloadTrigger) {
        isLoading = true

        if (cachedApps != null) {
            apps = cachedApps!!
            isLoading = false
            return@LaunchedEffect
        }

        val provider = AppProvider(context)
        val installedApps = provider.getInstalledApps()
        val usageMaps = getUsageStats()

        apps = installedApps.map {
            it.copy(
                usageTime = usageMaps[it.packageName] ?: 0L
            )
        }.sortedByDescending { it.usageTime }

        apps.forEach {
            Log.d("APPS", "${it.name} -> ${it.usageTime}")
        }

        cachedApps = apps

        isLoading = false
    }

    when {
        !hasPermission -> {
            // Puedes mostrar un mensaje o nada mientras el usuario da permisos
            Text(text = "Dando permisos ....")
        }
        isLoading -> {
            CircularProgressIndicator()
        }
        else -> {
            AppScreen(apps)
        }
    }

    val lifecycleOwner = LocalLifecycleOwner.current

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                val permissionGranted = hasUsageStatsPermission(context)
                hasPermission = permissionGranted

                if (hasPermission) {
                    //recargar datos
                    cachedApps = null
                    reloadTrigger++ //fuerza reload
                }
            }
        }

        lifecycleOwner.lifecycle.addObserver(observer)

        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }
}