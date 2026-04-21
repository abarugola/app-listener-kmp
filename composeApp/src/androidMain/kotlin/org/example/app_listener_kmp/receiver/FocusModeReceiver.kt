package org.example.app_listener_kmp.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import org.example.app_listener_kmp.data.AndroidBlockConfigRepository
import org.example.app_listener_kmp.platform.FocusModeScheduler
import org.example.app_listener_kmp.services.AppBlockerService

class FocusModeReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val repository = AndroidBlockConfigRepository(context)

        if (!repository.getBlockedPackages().isEmpty()) return

        when (intent.action) {
            ACTION_FOCUS_START -> {
                val serviceIntent = Intent(context, AppBlockerService::class.java).apply {
                    action = ACTION_FOCUS_START
                }
                context.startForegroundService(serviceIntent)
            }
            ACTION_FOCUS_END -> {
                val serviceIntent = Intent(context, AppBlockerService::class.java).apply {
                    action = ACTION_FOCUS_END
                }
                context.startForegroundService(serviceIntent)
            }
        }

        FocusModeScheduler.schedule(context)
    }

    companion object {
        const val ACTION_FOCUS_START = "FOCUS_SCHEDULE_START"
        const val ACTION_FOCUS_END = "FOCUS_SCHEDULE_END"
    }
}
