package org.example.app_listener_kmp.platform

import org.example.app_listener_kmp.domain.model.AppInfo

expect class AppProvider  {
    fun getInstalledApps(): List<AppInfo>
}