package org.example.app_listener_kmp.data

import android.content.Context
import androidx.core.content.edit
import org.example.app_listener_kmp.domain.model.BlockState
import org.example.app_listener_kmp.domain.model.TimeRange
import org.example.app_listener_kmp.domain.repository.BlockConfigRepository
import kotlin.apply

private const val PREFS_NAME = "blockish_prefs"

// Focus mode config constants
private const val FOCUS_ENABLED_KEY = "focus_mode_enabled"
private const val FOCUS_MODE_START_HOUR_KEY = "focus_schedule_start_hour"
private const val FOCUS_MODE_START_MINUTE_KEY = "focus_schedule_start_minute"
private const val FOCUS_MODE_END_HOUR_KEY = "focus_schedule_end_hour"
private const val FOCUS_MODE_END_MINUTE_KEY = "focus_schedule_end_minute"


// Sleep mode configs constants
private const val SLEEP_MODE_ENABLED_KEY = "sleep_mode_enabled"
private const val SLEEP_START_HOUR_KEY = "sleep_start_hour"
private const val SLEEP_START_MINUTE_KEY = "sleep_start_minute"
private const val SLEEP_END_HOUR_KEY = "sleep_end_hour"
private const val SLEEP_END_MINUTE_KEY = "sleep_end_minute"

// Service logic constants
private const val BLOCKED_PACKAGES_KEY = "blocked_packages"
private const val SERVICE_SATE_KEY = "service_state"
private const val FOCUS_START_TIME_KEY = "focus_start_time"
private const val BREAK_START_TIME_KEY = "break_start_time"
private const val BLOCKS_COMPLETED_KEY = "blocks_completed"




class AndroidBlockConfigRepository(private val context: Context) : BlockConfigRepository {

    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)


    override fun isFocusModeEnabled(): Boolean = prefs.getBoolean(FOCUS_ENABLED_KEY, false)

    override fun setFocusModeEnabled(enabled: Boolean) = prefs.edit {
        putBoolean(FOCUS_ENABLED_KEY, enabled)
    }

    override fun isSleepModeEnabled(): Boolean = prefs.getBoolean(SLEEP_MODE_ENABLED_KEY, false)

    override fun setSleepModeEnabled(enabled: Boolean) = prefs.edit {
        putBoolean(SLEEP_MODE_ENABLED_KEY, enabled)
    }

    override fun getBlockedPackages(): Set<String> = prefs.getStringSet(BLOCKED_PACKAGES_KEY, emptySet()) ?: emptySet()
    override fun setBlockedPackages(packages: Set<String>) = prefs.edit {
        putStringSet(BLOCKED_PACKAGES_KEY, packages)
    }

    override fun addBlockedPackage(packageName: String) {
        val packages = getBlockedPackages() + packageName
        prefs.edit {
            putStringSet(BLOCKED_PACKAGES_KEY, packages)
        }
    }

    override fun removeBlockedPackage(packageName: String) {
        val packages = getBlockedPackages() - packageName
        prefs.edit {
            putStringSet(BLOCKED_PACKAGES_KEY, packages)
        }
    }

    override fun getFocusRange() = TimeRange(
            startHour = prefs.getInt(FOCUS_MODE_START_HOUR_KEY,9),
            startMinute = prefs.getInt(FOCUS_MODE_START_MINUTE_KEY, 0),
            endHour = prefs.getInt(FOCUS_MODE_END_HOUR_KEY, 18),
            endMinute = prefs.getInt(FOCUS_MODE_END_MINUTE_KEY, 0)
        )

    override fun setFocusRange(range: TimeRange) = prefs.edit {
        putInt(FOCUS_MODE_START_HOUR_KEY, range.startHour)
        putInt(FOCUS_MODE_START_MINUTE_KEY, range.startMinute)
        putInt(FOCUS_MODE_END_HOUR_KEY, range.endHour)
        putInt(FOCUS_MODE_END_MINUTE_KEY, range.endMinute)
    }

    override fun getSleepRange() = TimeRange(
            startHour = prefs.getInt(SLEEP_START_HOUR_KEY,9),
            startMinute = prefs.getInt(SLEEP_START_MINUTE_KEY, 0),
            endHour = prefs.getInt(SLEEP_END_HOUR_KEY, 18),
            endMinute = prefs.getInt(SLEEP_END_MINUTE_KEY, 0)
        )

    override fun setSleepRange(range: TimeRange) {
        prefs.edit {
            putInt(SLEEP_START_HOUR_KEY, range.startHour)
            putInt(SLEEP_START_MINUTE_KEY, range.startMinute)
            putInt(SLEEP_END_HOUR_KEY, range.endHour)
            putInt(SLEEP_END_MINUTE_KEY, range.endMinute)
        }
    }

    override fun getServiceState() = prefs.getString(SERVICE_SATE_KEY, BlockState.STOPPED.name) ?: BlockState.STOPPED.name

    override fun setServiceState(state: String) = prefs.edit { putString(SERVICE_SATE_KEY, state) }

    override fun getFocusStartTime(): Long = prefs.getLong(FOCUS_START_TIME_KEY, 0L)

    override fun setFocusStartTime(time: Long) = prefs.edit { putLong(FOCUS_START_TIME_KEY, time) }

    override fun getBreakStartTime(): Long = prefs.getLong(BREAK_START_TIME_KEY, 0L)

    override fun setBreakStartTime(time: Long) = prefs.edit { putLong(BREAK_START_TIME_KEY, time) }

    override fun getBlocksCompleted(): Int = prefs.getInt(BLOCKS_COMPLETED_KEY, 0)

    override fun setBlocksCompleted(count: Int) = prefs.edit { putInt(BLOCKS_COMPLETED_KEY, count) }

}