package org.example.app_listener_kmp.domain.repository

import org.example.app_listener_kmp.domain.model.TimeRange

interface BlockConfigRepository {
    fun isFocusModeEnabled(): Boolean
    fun setFocusModeEnabled(enabled: Boolean)

    fun isSleepModeEnabled(): Boolean
    fun setSleepModeEnabled(enabled: Boolean)

    fun getBlockedPackages(): Set<String>
    fun setBlockedPackages(packages: Set<String>)
    fun addBlockedPackage(packageName: String)
    fun removeBlockedPackage(packageName: String)

    fun getFocusRange(): TimeRange
    fun setFocusRange(range: TimeRange)

    fun getSleepRange(): TimeRange
    fun setSleepRange(range: TimeRange)

    // Estado del servicio (persistido para reinicios)
    fun getServiceState(): String
    fun setServiceState(state: String)
    fun getFocusStartTime(): Long
    fun setFocusStartTime(time: Long)
    fun getBreakStartTime(): Long
    fun setBreakStartTime(time: Long)
    fun getBlocksCompleted(): Int
    fun setBlocksCompleted(count: Int)
}
