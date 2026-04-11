package org.example.app_listener_kmp.platform

import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import java.util.Calendar

lateinit var appContext: Context

fun provideContext(context: Context) {
    appContext = context
}

actual fun getUsageStats(): Map<String, Long> {
    val usageStatsManager =
        appContext.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager

    val (startTime, endTime) = getTodayRange()

    val events = usageStatsManager.queryEvents(startTime, endTime)

    val usageMap = mutableMapOf<String, Long>()
    val lastStartMap = mutableMapOf<String, Long>()

    val event = UsageEvents.Event()

    while (events.hasNextEvent()) {
        events.getNextEvent(event)

        when (event.eventType) {

            UsageEvents.Event.ACTIVITY_RESUMED -> {
                lastStartMap[event.packageName] = event.timeStamp
            }

            UsageEvents.Event.ACTIVITY_PAUSED -> {
                val start = lastStartMap[event.packageName]

                if (start != null) {
                    val duration = event.timeStamp - start

                    usageMap[event.packageName] =
                        (usageMap[event.packageName] ?: 0L) + duration

                    lastStartMap.remove(event.packageName)
                }
            }
        }
    }

    // 🔥 Manejar apps abiertas actualmente
    val now = System.currentTimeMillis()
    for ((pkg, start) in lastStartMap) {
        val duration = now - start
        usageMap[pkg] = (usageMap[pkg] ?: 0L) + duration
    }

    return usageMap
}

fun getTodayRange(): Pair<Long, Long> {
    val calendar = Calendar.getInstance()

    // 🔥 Inicio del día (00:00:00.000)
    calendar.apply {
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }

    val startOfDay = calendar.timeInMillis
    val now = System.currentTimeMillis()

    return startOfDay to now
}

fun getForegroundApp (): String? {
    val usm = appContext.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
    val now = System.currentTimeMillis()

    // Ampliamos la ventana a 10 minutos en lugar de 3 segundos
    // Esto nos permite encontrar el último ACTIVITY_RESUMED aunque
    // la app lleve varios minutos abierta sin producir nuevos eventos
    val events = usm.queryEvents(now - 10 * 60 * 1000L, now)
    val event = UsageEvents.Event()

    // En lugar de solo guardar el último RESUMED, rastreamos
    // el estado de cada app por separado
    // resumed guarda el timestamp del último RESUMED de cada app
    // paused guarda el timestamp del último PAUSED de cada app
    val resumedMap = mutableMapOf<String, Long>()
    val pausedMap = mutableMapOf<String, Long>()

    while (events.hasNextEvent()) {
        events.getNextEvent(event)
        when (event.eventType) {
            UsageEvents.Event.ACTIVITY_RESUMED -> {
                resumedMap[event.packageName] = event.timeStamp
            }
            UsageEvents.Event.ACTIVITY_PAUSED -> {
                pausedMap[event.packageName] = event.timeStamp
            }
        }
    }

    // La app en foreground es aquella que tiene un RESUMED más reciente
    // que su PAUSED — es decir, fue abierta pero no cerrada después
    return resumedMap
        .filter { (pkg, resumeTime) ->
            val pauseTime = pausedMap[pkg] ?: 0L
            // Si el último RESUMED es más reciente que el último PAUSED,
            // significa que la app está actualmente al frente
            resumeTime > pauseTime
        }
        // De todas las apps que "están al frente" (puede haber varias en transición),
        // tomamos la que tiene el RESUMED más reciente
        .maxByOrNull { it.value }
        ?.key
}