package org.example.app_listener_kmp.usecase

import org.example.app_listener_kmp.domain.model.AppInfo
import org.example.app_listener_kmp.platform.AppProvider

class GetInstalledApps (
    private val provider: AppProvider
) {
    fun execute(): List<AppInfo> {
        return provider.getInstalledApps()
    }
}