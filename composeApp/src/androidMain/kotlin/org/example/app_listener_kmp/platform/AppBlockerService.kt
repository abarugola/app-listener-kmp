package org.example.app_listener_kmp.platform

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import androidx.core.app.NotificationCompat
import org.example.app_listener_kmp.ui.BlockActivity

class AppBlockerService: Service() {

    private val handler = Handler(Looper.getMainLooper())
    private val blockedPackages = mutableSetOf<String>()

    private val checkRunnable = object : Runnable {
        override fun run() {
            val foreground = getForegroundApp()

            // si la app bloqueada esta al frente, lanzamos la pantalla de aviso
            if (foreground != null && blockedPackages.contains(foreground)) {
                val intent = Intent(appContext, BlockActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    putExtra("BLOCKED_PACKAGE", foreground)
                }
                startActivity(intent)
            }

            handler.postDelayed(this, 1000) // revisa cada segundo
        }
    }

    override fun onCreate() {
        super.onCreate()
        startForeground(1, buildNotification())
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val packageName = intent?.getStringExtra("BLOCK_PACKAGE") ?: return START_STICKY

        when (intent.action) {
            "UNBLOCK" -> {
                blockedPackages.remove(packageName)

                if (blockedPackages.isEmpty()) {
                    stopSelf()
                }
            }
            else -> {
                blockedPackages.add(packageName)
            }
        }

        handler.removeCallbacks(checkRunnable)
        handler.post(checkRunnable) // arranca el loop
        return START_STICKY // le dice a Android que reinicie el servicio si lo mata
    }

    private fun buildNotification(): Notification {
        val channelId = "app_blocker_channel"

        // En androidd +8 es obligatorio  crear un Notification Channel
        val channel = NotificationChannel(
            channelId,
            "App Blocker",
            NotificationManager.IMPORTANCE_LOW // LOW = sin sonido solo icono
        )
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(channel)

        return NotificationCompat.Builder(this,channelId)
            .setContentTitle("Blockish activo")
            .setContentText("Monitoreando apps bloqueadas")
            .setSmallIcon(android.R.drawable.ic_lock_lock) // icono temporal ddel sistema
            .build()
    }

    override fun onDestroy() {
        handler.removeCallbacks(checkRunnable)
        super.onDestroy()
    }

    override fun onBind(p0: Intent?): IBinder? = null

    private fun launchBlockScreen() {
         // lanzamos una Activity normal pero con un flag especial
        // FLAG_ACTIVITY_NEW_TASK  es obligatorio cuando se lanza desde un Service
        val intent = Intent(this, BlockActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        startActivity(intent)
    }
}