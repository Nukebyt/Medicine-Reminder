package com.nukebyt.medicinereminder.ui.util

import kotlinx.coroutines.flow.MutableStateFlow
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

/** History asks Today to show a given day ("Tapping a day jumps to that day on Today"). */
@Singleton
class DateJump @Inject constructor() {
    val requested = MutableStateFlow<LocalDate?>(null)

    fun request(date: LocalDate) {
        requested.value = date
    }

    fun consume() {
        requested.value = null
    }
}
