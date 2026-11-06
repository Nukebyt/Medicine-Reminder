package com.nukebyt.medicinereminder.domain

import org.junit.Assert.assertEquals
import org.junit.Test

class OemTest {
    @Test fun xiaomiFamily() {
        assertEquals(Oem.XIAOMI, Oem.detect("Xiaomi"))
        assertEquals(Oem.XIAOMI, Oem.detect("Xiaomi", "POCO"))
        assertEquals(Oem.XIAOMI, Oem.detect("xiaomi", "Redmi"))
    }

    @Test fun otherMakers() {
        assertEquals(Oem.REALME_OPPO, Oem.detect("realme"))
        assertEquals(Oem.REALME_OPPO, Oem.detect("OPPO"))
        assertEquals(Oem.VIVO, Oem.detect("vivo"))
        assertEquals(Oem.VIVO, Oem.detect("BBK", "iQOO"))
        assertEquals(Oem.ONEPLUS, Oem.detect("OnePlus"))
        assertEquals(Oem.SAMSUNG, Oem.detect("samsung"))
        assertEquals(Oem.HUAWEI, Oem.detect("HUAWEI", "HONOR"))
    }

    @Test fun unknownAndNull() {
        assertEquals(Oem.OTHER, Oem.detect("Google", "google"))
        assertEquals(Oem.OTHER, Oem.detect(null))
    }
}
