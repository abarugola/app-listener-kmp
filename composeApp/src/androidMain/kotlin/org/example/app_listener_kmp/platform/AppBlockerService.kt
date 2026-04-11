package org.example.app_listener_kmp.platform

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.HandlerThread
import android.os.IBinder
import android.os.Looper
import androidx.core.app.NotificationCompat
import org.example.app_listener_kmp.ui.BlockActivity

class AppBlockerService: Service() {

    // Nombre del archivo de preferencias — como el "nombre del cajón"
    // donde guardamos nuestras cosas
    private val PREFS_NAME = "blockish_prefs"

    // Enum que representa los tres estados posibles del sistema
    // Usar un enum en lugar de Strings o Ints evita errores de typo
    // y hace el código más legible
    enum class BlockState {
        STOPPED, FOCUSING, BREAK
    }

    // HandlerThread es un hilo de background con su propio Looper
    // Le damos un nombre descriptivo para poder identificarlo en logs si es necesario
    private val handlerThread = HandlerThread("AppBlockerThread")

    // El Handler ahora vivirá en ese hilo dedicado, no en el hilo principal
    // Lo inicializamos como lateinit porque necesitamos arrancar handlerThread primero
    private lateinit var handler: Handler
    private val blockedPackages = mutableSetOf<String>()

    // Estado actual del sistema — arranca en STOPPED
    private var state = BlockState.STOPPED

    // Guardamos el momento exacto en que empezó cada período
    // Usamos Long porque currentTimeMillis() devuelve milisegundos como Long
    private var focusStartTime = 0L
    private var breakStartTime = 0L

    // Constantes de tiempo en milisegundos
    // Las definimos aquí arriba para que sean fáciles de cambiar durante pruebas
    // Durante desarrollo podrías cambiarlas a 1 minuto para no esperar 25 minutos
    private val focusDurationMs = 25 * 60 * 1000L  // 25 minutos
    private val breakDurationMs = 5 * 60 * 1000L   // 5 minutos

    private val checkRunnable = object : Runnable {
        override fun run() {
            val foreground = getForegroundApp()
            val now = System.currentTimeMillis()

            when (state) {
                BlockState.FOCUSING -> {
                    // ¿Cuánto tiempo llevamos en modo enfoque?
                    val focusElapsed = now - focusStartTime
                    val focusComplete = focusElapsed >= focusDurationMs

                    if (foreground != null && blockedPackages.contains(foreground)) {
                        if (focusComplete) {
                            // El usuario completó el período mínimo de enfoque
                            // y abrió una app bloqueada — se ganó el descanso
                            state = BlockState.BREAK
                            breakStartTime = now
                            saveState() // persistimos el nuevo estado inmediatamente
                            // No mostramos BlockActivity — el usuario puede pasar
                            updateNotification("🌿 Descanso — 5 minutos")
                        } else {
                            // Aún no completa los 25 minutos — bloqueamos
                            launchBlockActivity(foreground)
                        }
                    }

                    // Si foreground no está bloqueada, no hacemos nada
                    // El enfoque puede durar más de 25 minutos sin problema
                }

                BlockState.BREAK -> {
                    val breakElapsed = now - breakStartTime

                    if (breakElapsed >= breakDurationMs) {
                        // Terminó el descanso — regresamos a FOCUSING
                        state = BlockState.FOCUSING
                        focusStartTime = now  // reiniciamos el timer de enfoque
                        saveState() // persistimos de nuevo
                        updateNotification("🎯 Enfocado — 25 minutos")

                        // Si el usuario sigue en una app bloqueada cuando termina
                        // el descanso, la bloqueamos inmediatamente
                        if (foreground != null && blockedPackages.contains(foreground)) {
                            launchBlockActivity(foreground)
                        }
                    }
                    // Si el descanso no ha terminado, no hacemos nada — el usuario
                    // puede usar libremente las apps bloqueadas
                }

                BlockState.STOPPED -> {
                    // No hacemos nada — el Service no debería estar corriendo
                    // en este estado, pero por seguridad lo manejamos
                }
            }

            handler.postDelayed(this, 1000) // revisa cada segundo
        }
    }

    override fun onCreate() {
        super.onCreate()

        // Arrancamos el hilo ANTES de crear el Handler
        // Si intentaras crear el Handler antes, el Looper no existiría todavía
        handlerThread.start()

        // Ahora sí creamos el Handler usando el Looper del hilo dedicado
        // Esto es lo que separa el loop del hilo principal de la UI
        handler = Handler(handlerThread.looper)

        // Creamos el canal UNA sola vez aquí, no dentro de buildNotification
        // Android ignora esto si el canal ya existe, así que es seguro llamarlo aquí
        val channel = NotificationChannel(
            CHANNEL_ID,
            "App Blocker",
            NotificationManager.IMPORTANCE_LOW
        )
        getSystemService(NotificationManager::class.java).createNotificationChannel(channel)

        startForeground(1, buildNotification("🎯 Iniciando enfoque..."))
    }

    // Guardamos el estado antes de que el Service muera
    private fun saveState() {
        val prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().apply {
            // Guardamos los packageNames bloqueados como un Set de Strings
            putStringSet("blocked_packages", blockedPackages)
            // Guardamos el estado actual como String
            putString("state", state.name)
            // Guardamos los timestamps para que los timers puedan continuar
            // donde los dejaron, no reiniciarse desde cero
            putLong("focus_start_time", focusStartTime)
            putLong("break_start_time", breakStartTime)
            apply() // apply() es asíncrono — más eficiente que commit() para esto
        }
    }

    // Restauramos el estado cuando el Service arranca de nuevo
    private fun restoreState() {
        val prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

        val savedPackages = prefs.getStringSet("blocked_packages", emptySet()) ?: emptySet()
        blockedPackages.addAll(savedPackages)

        // Restauramos el estado usando el nombre del enum guardado como String
        // Si no había estado guardado, comenzamos en STOPPED
        state = BlockState.valueOf(
            prefs.getString("state", BlockState.STOPPED.name) ?: BlockState.STOPPED.name
        )

        focusStartTime = prefs.getLong("focus_start_time", 0L)
        breakStartTime = prefs.getLong("break_start_time", 0L)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // Si intent es null, significa que Android reinició el Service
        // automáticamente después de matarlo — perdimos todo el estado
        android.util.Log.d("BlockerService", "onStartCommand - intent es null: ${intent == null}")
        android.util.Log.d("BlockerService", "blockedPackages: $blockedPackages")
        android.util.Log.d("BlockerService", "state: $state")

        // Si intent es null, Android reinició el Service automáticamente
        // Solo necesitamos restaurar el estado y reanudar el loop
        if (intent == null) {
            restoreState()
            handler.removeCallbacks(checkRunnable)
            handler.post(checkRunnable)
            return START_STICKY
        }

        val packageName = intent?.getStringExtra("BLOCK_PACKAGE") ?: return START_STICKY

        when (intent.action) {
            "UNBLOCK" -> {
                blockedPackages.remove(packageName)

                if (blockedPackages.isEmpty()) {
                    state = BlockState.STOPPED
                    stopSelf()
                }
            }
            else -> {
                blockedPackages.add(packageName)

                // Solo iniciamos el timer de enfoque la primera vez que
                // se agrega una app — no queremos reiniciarlo si el usuario
                // bloquea una segunda app mientras ya está en FOCUSING
                if (state == BlockState.STOPPED) {
                    state = BlockState.FOCUSING
                    focusStartTime = System.currentTimeMillis()
                    updateNotification("🎯 Enfocado — 25 minutos")
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
            .setSmallIcon(android.R.drawable.ic_lock_lock)
            .build()
    }

    private fun updateNotification(text: String) {
        getSystemService(NotificationManager::class.java).notify(1, buildNotification(text))
    }

    // Llamamos saveState() cada vez que el estado cambia
    // — en onStartCommand cuando bloqueamos/desbloqueamos
    // — en el checkRunnable cuando cambiamos entre FOCUSING y BREAK
    override fun onDestroy() {
        saveState() // guardamos antes de morir
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