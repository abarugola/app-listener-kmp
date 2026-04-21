package org.example.app_listener_kmp.platform

import org.example.app_listener_kmp.domain.model.AppInfo

actual class AppProvider {
    actual fun getInstalledApps (): List<AppInfo> {
        return emptyList()
    }
}