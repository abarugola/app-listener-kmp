package org.example.app_listener_kmp.services

import android.R
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.icu.util.Calendar
import android.os.Build
import android.os.Handler
import android.os.HandlerThread
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import org.example.app_listener_kmp.data.AndroidBlockConfigRepository
import org.example.app_listener_kmp.domain.model.BlockState
import org.example.app_listener_kmp.domain.repository.BlockConfigRepository
import org.example.app_listener_kmp.platform.getForegroundApp
import org.example.app_listener_kmp.receiver.FocusModeReceiver
import org.example.app_listener_kmp.receiver.SleepModeReceiver
import org.example.app_listener_kmp.ui.BlockActivity

class AppBlockerService: Service() {



    // HandlerThread es un hilo de background con su propio Looper
    // Le damos un nombre descriptivo para poder identificarlo en logs si es necesario
    private val handlerThread = HandlerThread("AppBlockerThread")

    // El Handler ahora vivirá en ese hilo dedicado, no en el hilo principal
    // Lo inicializamos como lateinit porque necesitamos arrancar handlerThread primero
    private lateinit var handler: Handler
    private val focusDurationMs = 25 * 60 * 1000L  // 25 minutos
    private var breakShortMs = 5 * 60 * 1000L
    private var breakLongMs = 15 * 60 * 1000L

    private lateinit var repository: BlockConfigRepository
    private var state = BlockState.STOPPED

    private val checkRunnable = object : Runnable {
        override fun run() {
            val foreground = getForegroundApp()
            val focusEnabled = repository.isFocusModeEnabled()
            val blockedPackages = repository.getBlockedPackages()
            val now = System.currentTimeMillis()

            when (state) {
                BlockState.FOCUSING -> {
                    if (!focusEnabled) {
                        handler.postDelayed(this, 1000)
                        return
                    }

                    val range = repository.getFocusRange()
                    val cal = Calendar.getInstance()
                    if (!range.isNowInside(cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE))) {
                        updateState(BlockState.STOPPED, "⏸ Horario de enfoque terminado")
                        //handler.postDelayed(this, 1000)
                        return
                    }

                    // ¿Cuánto tiempo llevamos en modo enfoque?
                    if (foreground != null && blockedPackages.contains(foreground)) {
                        val elapsed = now - repository.getFocusStartTime()
                        val focusComplete = elapsed >= focusDurationMs
                        if (focusComplete) {
                            val total = repository.getBlocksCompleted() + 1
                            repository.setBlocksCompleted(total)
                            repository.setBreakStartTime(now)
                            if (total % 4 == 0) {
                                updateState(BlockState.BREAK_LONG, "🛋 Descanso largo — 15 minutos")
                            } else {
                                updateState(BlockState.BREAK_SHORT, "🌿 Descanso corto — 5 minutos")
                            }
                        } else {
                            launchBlockActivity(foreground)
                        }
                    }

                    // Si foreground no está bloqueada, no hacemos nada
                    // El enfoque puede durar más de 25 minutos sin problema
                }

                BlockState.BREAK_SHORT -> {
                    if (!focusEnabled) {
                        handler.postDelayed(this, 1000)
                        return
                    }

                    val range = repository.getFocusRange()
                    val cal = Calendar.getInstance()
                    if (!range.isNowInside(cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE))) {
                        updateState(BlockState.STOPPED, "⏸ Horario de enfoque terminado")
                        //handler.postDelayed(this, 1000)
                        return
                    }

                    val start = repository.getBreakStartTime()

                    if (now - start >= breakShortMs) {
                        repository.setFocusStartTime(System.currentTimeMillis())
                        updateState(BlockState.FOCUSING, "🎯 Bloque ${repository.getBlocksCompleted() + 1} — 25 minutos")
                        if (foreground != null && blockedPackages.contains(foreground)) {
                            launchBlockActivity(foreground)
                        }
                    }
                }

                BlockState.BREAK_LONG -> {
                    if (!focusEnabled) {
                        handler.postDelayed(this, 1000)
                        return
                    }

                    val range = repository.getFocusRange()
                    val cal = Calendar.getInstance()
                    if (!range.isNowInside(cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE))) {
                        updateState(BlockState.STOPPED, "⏸ Horario de enfoque terminado")
                        //handler.postDelayed(this, 1000)
                        return
                    }

                    val start = repository.getBreakStartTime()

                    if (now - start >= breakLongMs) {
                        repository.setFocusStartTime(System.currentTimeMillis())
                        updateState(BlockState.FOCUSING, "🎯 Bloque ${repository.getBlocksCompleted() + 1} — 25 minutos")

                        if(foreground != null && blockedPackages.contains(foreground)) {
                            launchBlockActivity(foreground)
                        }
                    }
                }

                BlockState.STOPPED -> { }

                BlockState.SLEEP -> {
                    // En modo sueño bloqueamos las mismas apps del Pomodoro
                    // El modo sueño tiene prioridad — no importa si hay un Pomodoro activo
                    if (foreground != null && blockedPackages.contains(foreground)) {
                        launchBlockActivity(foreground)
                    }
                }
            }

            handler.postDelayed(this, 1000) // revisa cada segundo
        }
    }

    private fun updateState(newState: BlockState, notificationText: String? = null) {
        state = newState
        repository.setServiceState(newState.name)
        notificationText?.let { updateNotification(it) }
    }

    override fun onCreate() {
        super.onCreate()

        repository = AndroidBlockConfigRepository(applicationContext)
        val savedStateName = repository.getServiceState()
        state = try {
            BlockState.valueOf(savedStateName)
        } catch (e: Exception) {
            BlockState.STOPPED
        }

        // Arrancamos el hilo ANTES de crear el Handler
        // Si intentaras crear el Handler antes, el Looper no existiría todavía
        handlerThread.start()

        // Ahora sí creamos el Handler usando el Looper del hilo dedicado
        // Esto es lo que separa el loop del hilo principal de la UI
        handler = Handler(handlerThread.looper)

        // Creamos el canal UNA sola vez aquí, no dentro de buildNotification
        // Android ignora esto si el canal ya existe, así que es seguro llamarlo aquí
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "App Blocker",
                NotificationManager.IMPORTANCE_LOW
            )
            getSystemService(NotificationManager::class.java)?.createNotificationChannel(channel)
        }

        startForeground(1, buildNotification("🎯 Blockish is running ..."))

        if (state != BlockState.STOPPED) {
            handler.post(checkRunnable)
        }

    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // Si intent es null, significa que Android reinició el Service
        // automáticamente después de matarlo — perdimos todo el estado
        Log.d("BlockerService", "onStartCommand - intent es null: ${intent == null}")
        Log.d("BlockerService", "blockedPackages: ${repository.getBlockedPackages()}")
        Log.d("BlockerService", "state: ${repository.getServiceState()}")

        // Si intent es null, Android reinició el Service automáticamente
        // Solo necesitamos restaurar el estado y reanudar el loop
        if (intent == null) {
            if (state != BlockState.STOPPED) {
                handler.removeCallbacks(checkRunnable)
                handler.post(checkRunnable)
            }
            return START_STICKY
        }

        // Nuevas acciones del modo sueño
        when (intent.action) {
            SleepModeReceiver.Companion.ACTION_SLEEP_START -> {
                // Modo sueño tiene prioridad — guardamos el estado anterior
                // para restaurarlo cuando termine el modo sueño
                if (state != BlockState.SLEEP) {
                    updateState(BlockState.SLEEP, "🌙 Modo sueño activo")
                    handler.removeCallbacks(checkRunnable)
                    handler.post(checkRunnable)
                }
                return START_STICKY
            }
            SleepModeReceiver.Companion.ACTION_SLEEP_END -> {
                if (state == BlockState.SLEEP) {
                    updateState(BlockState.STOPPED, "😴 Modo sueño terminado")
                    handler.removeCallbacks(checkRunnable)
                    handler.post(checkRunnable)
                }
                return START_STICKY
            }

            FocusModeReceiver.ACTION_FOCUS_START -> {
                if (state == BlockState.STOPPED &&
                    repository.isFocusModeEnabled() &&
                    repository.getFocusRange().isNowInside(
                        Calendar.getInstance().get(Calendar.HOUR_OF_DAY),
                        Calendar.getInstance().get(Calendar.MINUTE)
                    )
                ) {
                    repository.setFocusStartTime(System.currentTimeMillis())
                    updateState(BlockState.FOCUSING, "🎯 Bloque 1 — 25 minutos")
                    handler.removeCallbacks(checkRunnable)
                    handler.post(checkRunnable)
                }
                return START_STICKY
            }

            FocusModeReceiver.ACTION_FOCUS_END -> {
                if (state == BlockState.FOCUSING ||
                    state == BlockState.BREAK_SHORT ||
                    state == BlockState.BREAK_LONG
                ) {
                    repository.setBlocksCompleted(0)
                    updateState(BlockState.STOPPED, "⏸ Horario de enfoque terminado")
                }
                return START_STICKY
            }
        }

        val packageName = intent.getStringExtra("BLOCK_PACKAGE") ?: return START_STICKY // todo: BLOCK_PACKAGE a constant

        when (intent.action) {
            "UNBLOCK" -> {// todo: a constant
                repository.removeBlockedPackage(packageName)

                if (repository.getBlockedPackages().isEmpty()) {
                    updateState(BlockState.STOPPED)
                    stopSelf()
                }
            }
            else -> {
                repository.addBlockedPackage(packageName)

                // Solo iniciamos el timer de enfoque la primera vez que
                // se agrega una app — no queremos reiniciarlo si el usuario
                // bloquea una segunda app mientras ya está en FOCUSING
                if (state == BlockState.STOPPED) {
                    repository.setFocusStartTime(System.currentTimeMillis())
                    updateState(BlockState.FOCUSING, "🎯 Enfocado — 25 minutos")
                }
            }
        }

        handler.removeCallbacks(checkRunnable)
        handler.post(checkRunnable) // arranca el loop
        return START_STICKY // le dice a Android que reinicie el servicio si lo mata
    }

    private fun launchBlockActivity(packageName: String) {
        val intent = Intent(applicationContext, BlockActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
            putExtra("BLOCKED_PACKAGE", packageName)
        }
        startActivity(intent)
    }

    // Ahora buildNotification solo construye, no crea canales
    private fun buildNotification(text: String): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Blockish activo")
            .setContentText(text)
            .setSmallIcon(R.drawable.ic_lock_lock)
            .build()
    }

    private fun updateNotification(text: String) {
        getSystemService(NotificationManager::class.java)?.notify(1, buildNotification(text))
    }

    // Llamamos saveState() cada vez que el estado cambia
    // — en onStartCommand cuando bloqueamos/desbloqueamos
    // — en el checkRunnable cuando cambiamos entre FOCUSING y BREAK
    override fun onDestroy() {
        handler.removeCallbacks(checkRunnable)
        // Es importante terminar el HandlerThread cuando el Service muere
        // Si no lo haces, el hilo queda huérfano consumiendo recursos
        handlerThread.quitSafely()
        super.onDestroy()
    }

    override fun onBind(p0: Intent?): IBinder? = null

    // Definimos el channelId como constante del companion object
// para no escribir el String "app_blocker_channel" en múltiples lugares
// (un typo en un String es un bug muy difícil de detectar)
    companion object {
        const val CHANNEL_ID = "app_blocker_channel"
    }
}
