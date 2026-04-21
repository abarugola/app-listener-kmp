package org.example.app_listener_kmp.domain.model

import org.example.app_listener_kmp.domain.PlatformIcon

data class AppInfo (
    val name: String,
    val packageName: String,
    val icon: PlatformIcon,
    val usageTime: Long = 0L
)