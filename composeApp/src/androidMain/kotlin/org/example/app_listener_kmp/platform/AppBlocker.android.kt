package org.example.app_listener_kmp.platform

import android.content.Intent
import org.example.app_listener_kmp.services.AppBlockerService

actual fun blockApp(packageName: String) {
    val intent = Intent(appContext, AppBlockerService::class.java).apply {
        putExtra("BLOCK_PACKAGE", packageName)
    }
    appContext.startService(intent)
}

actual fun unblockApp(packageName: String) {
    val intent = Intent(appContext, AppBlockerService::class.java).apply {
        action = "UNBLOCK"
        putExtra("BLOCK_PACKAGE", packageName)
    }
    appContext.startService(intent)
}