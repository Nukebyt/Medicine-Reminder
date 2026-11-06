package com.nukebyt.medicinereminder.domain

/** Phone makers whose battery managers kill background apps. Each gets its own checklist (ROADMAP section 10). */
enum class Oem {
    XIAOMI, REALME_OPPO, VIVO, ONEPLUS, SAMSUNG, HUAWEI, OTHER;

    companion object {
        fun detect(manufacturer: String?, brand: String? = null): Oem {
            val text = "${manufacturer.orEmpty()} ${brand.orEmpty()}".lowercase()
            return when {
                listOf("xiaomi", "redmi", "poco").any { it in text } -> XIAOMI
                listOf("realme", "oppo").any { it in text } -> REALME_OPPO
                listOf("vivo", "iqoo").any { it in text } -> VIVO
                "oneplus" in text -> ONEPLUS
                "samsung" in text -> SAMSUNG
                listOf("huawei", "honor").any { it in text } -> HUAWEI
                else -> OTHER
            }
        }
    }
}
