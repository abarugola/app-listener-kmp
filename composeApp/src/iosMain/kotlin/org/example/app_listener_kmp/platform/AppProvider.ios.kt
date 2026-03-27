package org.example.app_listener_kmp.platform

import org.example.app_listener_kmp.domain.AppInfo

actual class AppProvider {
    actual fun getInstalledApps (): List<AppInfo> {
        return emptyList()
    }
}