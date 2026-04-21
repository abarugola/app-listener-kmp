package org.example.app_listener_kmp.platform

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import org.example.app_listener_kmp.data.AndroidBlockConfigRepository
import org.example.app_listener_kmp.receiver.FocusModeReceiver
import org.example.app_listener_kmp.services.AppBlockerService
import java.util.Calendar

object FocusModeScheduler {

    fun schedule(context: Context) {
        val repository = AndroidBlockConfigRepository(context)
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val range = repository.getFocusRange()
        val now = Calendar.getInstance()

        scheduleAlarm(
            context, alarmManager,
            hour = range.startHour,
            minute = range.startMinute,
            action = FocusModeReceiver.ACTION_FOCUS_START,
            requestCode = 200
        )

        scheduleAlarm(
            context, alarmManager,
            hour = range.endHour,
            minute = range.endMinute,
            action = FocusModeReceiver.ACTION_FOCUS_END,
            requestCode = 201
        )

        if (range.isNowInside(now.get(Calendar.HOUR_OF_DAY), now.get(Calendar.MINUTE))) {
            val intent = Intent(context, AppBlockerService::class.java).apply {
                action = FocusModeReceiver.ACTION_FOCUS_START
            }
            context.startForegroundService(intent)
        } else {
            val intent = Intent(context, AppBlockerService::class.java).apply {
                action = FocusModeReceiver.ACTION_FOCUS_END
            }
            context.startForegroundService(intent)
        }
    }

    private fun scheduleAlarm(
        context: Context,
        alarmManager: AlarmManager,
        hour: Int,
        minute: Int,
        action: String,
        requestCode: Int
    ) {
        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        if (calendar.timeInMillis <= System.currentTimeMillis()) {
            calendar.add(Calendar.DAY_OF_YEAR, 1)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            Intent(context, FocusModeReceiver::class.java).apply { this.action = action },
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        alarmManager.setExactAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            calendar.timeInMillis,
            pendingIntent
        )
    }

    fun cancel(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        listOf(
            200 to FocusModeReceiver.ACTION_FOCUS_START,
            201 to FocusModeReceiver.ACTION_FOCUS_END
        ).forEach { (requestCode, action) ->
            val pendingIntent = PendingIntent.getBroadcast(
                context, requestCode,
                Intent(context, FocusModeReceiver::class.java).apply {  this.action = action },
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )
            alarmManager.cancel(pendingIntent)
        }

    }
}