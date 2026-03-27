package org.example.app_listener_kmp.platform

import android.content.Context
import android.content.Intent
import org.example.app_listener_kmp.domain.AppInfo
import org.example.app_listener_kmp.domain.PlatformIcon

actual class AppProvider(private val context: Context) {
    actual fun getInstalledApps(): List<AppInfo> {
        val pm = context.packageManager
        val intent = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
        }
        val apps = pm.queryIntentActivities(intent, 0)
            .filter { it.activityInfo.packageName != context.packageName }

        return apps.map {
            AppInfo(
                name = it.loadLabel(pm).toString(),
                packageName = it.activityInfo.packageName,
                icon = PlatformIcon(it.activityInfo.loadIcon(pm))
            )
        }
    }
}