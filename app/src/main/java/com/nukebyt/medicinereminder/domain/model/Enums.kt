package com.nukebyt.medicinereminder.domain.model

enum class MealRelation { BEFORE_MEAL, AFTER_MEAL, WITH_MEAL, EMPTY_STOMACH, ANY_TIME }

enum class MedForm { TABLET, CAPSULE, SYRUP, DROPS, INJECTION, INHALER, CREAM, OTHER }

/** Only explicit user actions are stored. */
enum class DoseStatus { TAKEN, SKIPPED }

/** Derived, never stored. See ROADMAP section 5. */
enum class DoseState {
    UPCOMING, OVERDUE, TAKEN, TAKEN_LATE, SKIPPED, MISSED;

    /** True once the user has acted on the dose. */
    val isResolved: Boolean get() = this == TAKEN || this == TAKEN_LATE || this == SKIPPED

    val isTaken: Boolean get() = this == TAKEN || this == TAKEN_LATE
}
