package org.example.app_listener_kmp.platform

import android.app.usage.UsageStatsManager
import android.content.Context
import androidx.compose.ui.platform.LocalContext

lateinit var appContext: Context

fun provideContext(context: Context) {
    appContext = context
}

actual fun getUsageStats(): Map<String, Long> {
    val usm = appContext.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager

    val endTime = System.currentTimeMillis()
    val startTime =  endTime - 1000L * 60 * 60 * 24 // ultimas 24 horas

    val stats = usm.queryUsageStats(
        UsageStatsManager.INTERVAL_DAILY,
        startTime,
        endTime
    )

    return stats.associate {
        it.packageName to it.totalTimeInForeground
    }
}
