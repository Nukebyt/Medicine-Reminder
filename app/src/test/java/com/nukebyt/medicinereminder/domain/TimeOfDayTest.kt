package com.nukebyt.medicinereminder.domain

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalTime

class TimeOfDayTest {
    @Test fun boundaries() {
        assertEquals(TimeOfDay.NIGHT, TimeOfDay.of(LocalTime.of(4, 59)))
        assertEquals(TimeOfDay.MORNING, TimeOfDay.of(LocalTime.of(5, 0)))
        assertEquals(TimeOfDay.MORNING, TimeOfDay.of(LocalTime.of(11, 59)))
        assertEquals(TimeOfDay.AFTERNOON, TimeOfDay.of(LocalTime.of(12, 0)))
        assertEquals(TimeOfDay.AFTERNOON, TimeOfDay.of(LocalTime.of(16, 59)))
        assertEquals(TimeOfDay.EVENING, TimeOfDay.of(LocalTime.of(17, 0)))
        assertEquals(TimeOfDay.EVENING, TimeOfDay.of(LocalTime.of(21, 0)))
        assertEquals(TimeOfDay.NIGHT, TimeOfDay.of(LocalTime.of(22, 0)))
        assertEquals(TimeOfDay.NIGHT, TimeOfDay.of(LocalTime.MIDNIGHT))
    }
}
