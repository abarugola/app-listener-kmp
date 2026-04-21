package org.example.app_listener_kmp.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import org.example.app_listener_kmp.data.AndroidBlockConfigRepository
import org.example.app_listener_kmp.platform.SleepModeScheduler
import org.example.app_listener_kmp.services.AppBlockerService

class SleepModeReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val repository = AndroidBlockConfigRepository(context)

        // Si no hay apps configuradas, la alarma no tiene efecto
        if (!repository.getBlockedPackages().isEmpty()) return
        // Distinguimos si es la alarma de inicio o de fin
        // usando la acción del Intent — igual que hicimos con BLOCK/UNBLOCK
        when (intent.action) {
            ACTION_SLEEP_START -> {
                // Le decimos al Service que active el modo sueño
                val serviceIntent = Intent(context, AppBlockerService::class.java).apply {
                    action = ACTION_SLEEP_START
                }
                context.startForegroundService(serviceIntent)
            }
            ACTION_SLEEP_END -> {
                // Le decimos al Service que desactive el modo sueño
                val serviceIntent = Intent(context, AppBlockerService::class.java).apply {
                    action = ACTION_SLEEP_END
                }
                context.startForegroundService(serviceIntent)

                SleepModeScheduler.schedule(context)
            }
        }
    }

    companion object {
        const val ACTION_SLEEP_START = "SLEEP_START"
        const val ACTION_SLEEP_END = "SLEEP_END"
    }
}
