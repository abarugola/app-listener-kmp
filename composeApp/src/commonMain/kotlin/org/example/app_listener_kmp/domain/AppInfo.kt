package org.example.app_listener_kmp.domain

data class AppInfo (
    val name: String,
    val packageName: String,
    val icon: PlatformIcon,
    val usageTime: Long = 0L
)