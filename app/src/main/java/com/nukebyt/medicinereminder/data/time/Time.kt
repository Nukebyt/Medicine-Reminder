package com.nukebyt.medicinereminder.data.time

import com.nukebyt.medicinereminder.domain.TimeSource
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import java.time.ZonedDateTime
import javax.inject.Inject
import javax.inject.Singleton

/** Real clock. Reads the default zone on every call so a time-zone change is picked up. */
@Singleton
class SystemTimeSource @Inject constructor() : TimeSource {
    override fun now(): ZonedDateTime = ZonedDateTime.now()
}

/** Emits the current time now, then again at the start of every minute. */
@Singleton
class MinuteTicker @Inject constructor(private val time: TimeSource) {
    fun ticks(): Flow<ZonedDateTime> = flow {
        while (true) {
            val now = time.now()
            emit(now)
            val intoMinuteMs = now.second * 1000L + now.nano / 1_000_000L
            delay(60_000L - intoMinuteMs)
        }
    }
}
