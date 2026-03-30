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
