package com.nukebyt.medicinereminder.domain

import java.time.LocalTime

/** Section headers on the Today screen. */
enum class TimeOfDay {
    MORNING, AFTERNOON, EVENING, NIGHT;

    companion object {
        /** Morning 05:00-11:59, afternoon 12:00-16:59, evening 17:00-21:59, night 22:00-04:59. */
        fun of(time: LocalTime): TimeOfDay = when (time.hour) {
            in 5..11 -> MORNING
            in 12..16 -> AFTERNOON
            in 17..21 -> EVENING
            else -> NIGHT
        }
    }
}
