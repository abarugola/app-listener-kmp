package org.example.app_listener_kmp.domain.model

data class TimeRange(
    val startHour: Int,
    val startMinute: Int,
    val endHour: Int,
    val endMinute: Int
) {
    fun isNowInside(currentHour: Int, currentMinute: Int): Boolean {
        val now = currentHour * 60 + currentMinute
        val start = startHour * 60 + startMinute
        val end = endHour * 60 + endMinute

        return if (start <= end) now in start until end
            else now !in end..<start
    }
}
