package com.nukebyt.medicinereminder.data.prefs

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

enum class ThemeMode { SYSTEM, LIGHT, DARK }

data class AppSettings(
    /** Minutes after the reminder before the alarm rings. 0 turns the escalation alarm off. */
    val escalationDelayMin: Int = 5,
    val snoozeMin: Int = 10,
    val alarmAutoStopMin: Int = 10,
    /** null = built-in beep, otherwise a ringtone URI string. */
    val alarmSoundUri: String? = null,
    val graduallyIncreaseVolume: Boolean = true,
    val vibrate: Boolean = true,
    val theme: ThemeMode = ThemeMode.SYSTEM,
    val onboardingDone: Boolean = false,
)

private val Context.settingsStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

@Singleton
class SettingsRepository @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private object Keys {
        val escalation = intPreferencesKey("escalation_delay_min")
        val snooze = intPreferencesKey("snooze_min")
        val autoStop = intPreferencesKey("alarm_auto_stop_min")
        val sound = stringPreferencesKey("alarm_sound_uri")
        val ramp = booleanPreferencesKey("gradual_volume")
        val vibrate = booleanPreferencesKey("vibrate")
        val theme = stringPreferencesKey("theme")
        val onboardingDone = booleanPreferencesKey("onboarding_done")
        val scheduledSlots = stringSetPreferencesKey("scheduled_slots")
    }

    private val store get() = context.settingsStore

    val settings: Flow<AppSettings> = store.data.map { p ->
        val defaults = AppSettings()
        AppSettings(
            escalationDelayMin = p[Keys.escalation] ?: defaults.escalationDelayMin,
            snoozeMin = p[Keys.snooze] ?: defaults.snoozeMin,
            alarmAutoStopMin = p[Keys.autoStop] ?: defaults.alarmAutoStopMin,
            alarmSoundUri = p[Keys.sound]?.takeIf { it.isNotEmpty() },
            graduallyIncreaseVolume = p[Keys.ramp] ?: defaults.graduallyIncreaseVolume,
            vibrate = p[Keys.vibrate] ?: defaults.vibrate,
            theme = p[Keys.theme]?.let { runCatching { ThemeMode.valueOf(it) }.getOrNull() } ?: defaults.theme,
            onboardingDone = p[Keys.onboardingDone] ?: defaults.onboardingDone,
        )
    }

    suspend fun current(): AppSettings = settings.first()

    suspend fun setEscalationDelay(min: Int) = store.edit { it[Keys.escalation] = min }
    suspend fun setSnooze(min: Int) = store.edit { it[Keys.snooze] = min }
    suspend fun setAlarmAutoStop(min: Int) = store.edit { it[Keys.autoStop] = min }
    suspend fun setAlarmSound(uri: String?) = store.edit { it[Keys.sound] = uri.orEmpty() }
    suspend fun setGradualVolume(on: Boolean) = store.edit { it[Keys.ramp] = on }
    suspend fun setVibrate(on: Boolean) = store.edit { it[Keys.vibrate] = on }
    suspend fun setTheme(mode: ThemeMode) = store.edit { it[Keys.theme] = mode.name }
    suspend fun setOnboardingDone(done: Boolean) = store.edit { it[Keys.onboardingDone] = done }

    /** Slots (minute of day) that currently have an alarm scheduled, so removed ones can be cancelled. */
    suspend fun scheduledSlots(): Set<Int> =
        store.data.first()[Keys.scheduledSlots].orEmpty().mapNotNull { it.toIntOrNull() }.toSet()

    suspend fun setScheduledSlots(slots: Set<Int>) =
        store.edit { it[Keys.scheduledSlots] = slots.map(Int::toString).toSet() }
}
