package org.example.app_listener_kmp.platform

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import org.example.app_listener_kmp.data.AndroidBlockConfigRepository
import org.example.app_listener_kmp.receiver.SleepModeReceiver
import org.example.app_listener_kmp.services.AppBlockerService
import java.util.Calendar

object SleepModeScheduler {

    fun schedule(context: Context) {
        val repository = AndroidBlockConfigRepository(context)
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val range = repository.getSleepRange()
        val now = Calendar.getInstance()

        // Programamos la alarma de inicio
        scheduleAlarm(
            context = context,
            alarmManager = alarmManager,
            hour = range.startHour,
            minute = range.startMinute,
            action = SleepModeReceiver.ACTION_SLEEP_START,
            requestCode = 100
        )

        // Programamos la alarma de fin
        scheduleAlarm(
            context = context,
            alarmManager = alarmManager,
            hour = range.endHour,
            minute = range.endMinute,
            action = SleepModeReceiver.ACTION_SLEEP_END,
            requestCode = 101
        )

        if (range.isNowInside(now.get(Calendar.HOUR_OF_DAY), now.get(Calendar.MINUTE))) {
            val intent = Intent(context, AppBlockerService::class.java).apply {
                action = SleepModeReceiver.ACTION_SLEEP_START
            }
            context.startForegroundService(intent)
        } else {
            val intent = Intent(context, AppBlockerService::class.java).apply {
                action = SleepModeReceiver.ACTION_SLEEP_END
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
        // Construimos el Calendar con la hora deseada para HOY
        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        // Si la hora ya pasó hoy, programamos para mañana
        // Sin esto, si son las 11pm y programas para las 10pm,
        // la alarma dispararía inmediatamente en lugar de mañana a las 10pm
        if (calendar.timeInMillis <= System.currentTimeMillis()) {
            calendar.add(Calendar.DAY_OF_YEAR, 1)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            Intent(context, SleepModeReceiver::class.java).apply { this.action = action },
            // FLAG_IMMUTABLE es requerido en Android 12+
            // FLAG_UPDATE_CURRENT actualiza la alarma si ya existía
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        // setRepeating repetiría cada 24 horas automáticamente pero
        // Android lo throttlea en versiones modernas para ahorrar batería.
        // La forma correcta es usar setExactAndAllowWhileIdle y
        // reprogramar la siguiente alarma cada vez que dispara
        alarmManager.setExactAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP, // despierta el dispositivo aunque esté en reposo
            calendar.timeInMillis,
            pendingIntent
        )
    }

    // Cancelamos ambas alarmas cuando el usuario desactiva el modo sueño
    fun cancel(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        listOf(
            100 to SleepModeReceiver.ACTION_SLEEP_START,
            101 to SleepModeReceiver.ACTION_SLEEP_END
        ).forEach { (requestCode, action) ->
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                requestCode,
                Intent(context, SleepModeReceiver::class.java).apply { this.action = action },
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )
            alarmManager.cancel(pendingIntent)
        }
    }
}
