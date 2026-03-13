package org.example.app_listener_kmp

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform