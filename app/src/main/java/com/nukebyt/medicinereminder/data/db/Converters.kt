package com.nukebyt.medicinereminder.data.db

import androidx.room.TypeConverter
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime

/** Enums are stored by name, which Room does natively. */
class Converters {
    @TypeConverter fun dateToEpochDay(value: LocalDate?): Long? = value?.toEpochDay()
    @TypeConverter fun epochDayToDate(value: Long?): LocalDate? = value?.let(LocalDate::ofEpochDay)

    @TypeConverter fun timeToMinutes(value: LocalTime?): Int? = value?.let { it.hour * 60 + it.minute }
    @TypeConverter fun minutesToTime(value: Int?): LocalTime? = value?.let { LocalTime.of(it / 60, it % 60) }

    @TypeConverter fun instantToMillis(value: Instant?): Long? = value?.toEpochMilli()
    @TypeConverter fun millisToInstant(value: Long?): Instant? = value?.let(Instant::ofEpochMilli)
}
