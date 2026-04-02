package org.example.app_listener_kmp.ui

import android.content.Intent
import android.provider.Settings
import android.util.Log
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.net.toUri
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import org.example.app_listener_kmp.domain.AppInfo
import org.example.app_listener_kmp.platform.AppProvider
import org.example.app_listener_kmp.platform.getUsageStats
import org.example.app_listener_kmp.platform.hasUsageStatsPermission
import org.example.app_listener_kmp.platform.provideContext
import org.example.app_listener_kmp.platform.requestUsageStatsPermission


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

        // Verificamos si tenemos permiso para dibujar sobre otras apps
        // Settings.canDrawOverlays() es la funcion que Android provee para verificarlo
        if (!Settings.canDrawOverlays(context)) {
            // Al igual que Usage stats, mandamos al usuario a la apntalla  especifica
            // de Settings donde puede activar este permiso para tu app
            val intent = Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                "package:${context.packageName}".toUri() // abre directo tu app en settings
            )
            context.startActivity(intent)
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

                // Verificamos el permiso cada vez que el usuario regrese a la app
                // Esto cubre es claso donde el usuario acaba de activarlo en Settings
                if (!Settings.canDrawOverlays(context)) {
                    val intent = Intent(
                        Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                        "package:${context.packageName}".toUri()
                    )
                    context.startActivity(intent)
                }
            }
        }

        lifecycleOwner.lifecycle.addObserver(observer)

        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }
}