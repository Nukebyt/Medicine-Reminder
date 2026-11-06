package com.nukebyt.medicinereminder.alarm

import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.MediaPlayer
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.os.VibrationAttributes
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import androidx.core.content.getSystemService
import androidx.core.net.toUri
import com.nukebyt.medicinereminder.R
import com.nukebyt.medicinereminder.data.prefs.AppSettings
import com.nukebyt.medicinereminder.data.prefs.SettingsRepository
import com.nukebyt.medicinereminder.domain.TimeSource
import com.nukebyt.medicinereminder.domain.model.DoseUi
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.Instant
import javax.inject.Inject

/**
 * Rings the escalation alarm: looping beep on the alarm stream (so it sounds in silent mode), repeating vibration,
 * a partial wake lock, and a full-screen intent to [AlarmActivity]. Commands arrive as intent actions:
 *  - START: an escalation fired for a slot (a second one while ringing is merged, never a second player)
 *  - STOP: swipe to stop. Silence only; the doses stay overdue.
 *  - TOOK: "I took it": mark every pending dose taken, then stop.
 *  - SNOOZE: schedule a snooze alarm for every ringing slot, then stop.
 *  - REFRESH: a dose changed elsewhere (Today toggle, notification action); re-read and stop if none are left.
 */
@AndroidEntryPoint
class AlarmService : Service() {
    @Inject lateinit var gateway: DoseGateway
    @Inject lateinit var notifier: ReminderNotifier
    @Inject lateinit var scheduler: AlarmScheduler
    @Inject lateinit var settingsRepo: SettingsRepository
    @Inject lateinit var alarmState: AlarmStateHolder
    @Inject lateinit var time: TimeSource

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val slots = LinkedHashSet<SlotKey>()
    private var doses: List<DoseUi> = emptyList()
    private var settings = AppSettings()
    private var ringing = false

    private var player: MediaPlayer? = null
    private var vibrator: Vibrator? = null
    private var wakeLock: PowerManager.WakeLock? = null
    private var focusRequest: AudioFocusRequest? = null
    private var autoStopJob: Job? = null
    private var rampJob: Job? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            AlarmContract.ACTION_ALARM_START -> {
                val key = intent.slotKey()
                if (key == null) {
                    AppLog.w("alarm start without a slot, ignoring")
                    if (!ringing) stopSelf()
                } else {
                    onStart(key)
                }
            }
            AlarmContract.ACTION_ALARM_STOP -> if (ringing) silence() else stopSelf()
            AlarmContract.ACTION_ALARM_TOOK -> if (ringing) took() else stopSelf()
            AlarmContract.ACTION_ALARM_SNOOZE -> if (ringing) snoozeAll() else stopSelf()
            AlarmContract.ACTION_ALARM_REFRESH -> if (ringing) refresh() else stopSelf()
            else -> if (!ringing) stopSelf()
        }
        return START_NOT_STICKY
    }

    // ---- commands ----

    private fun onStart(key: SlotKey) {
        slots += key
        AppLog.d("alarm start for ${key.date} ${key.slot} (already ringing=$ringing)")
        if (!ringing) {
            // The 5 second startForeground deadline: do this before any database or audio work.
            if (!enterForeground(notifier.buildAlarmNotification(emptyList(), settings.snoozeMin))) {
                AppLog.e("could not start the alarm as a foreground service")
                slots.clear()
                stopSelf()
                return
            }
            ringing = true
            publish(emptyList()) // lets AlarmActivity start without finishing itself while doses load
            scope.launch {
                settings = settingsRepo.current()
                startRinging()
                reload()
            }
        } else {
            AppLog.d("alarm already ringing, merging ${key.slot}")
            scope.launch { reload() }
        }
    }

    private fun silence() {
        AppLog.d("alarm stopped by swipe: doses stay overdue")
        stopAll()
    }

    private fun took() {
        scope.launch {
            val keys = slots.toList()
            keys.forEach { gateway.markTaken(it) }
            AppLog.d("alarm: marked ${keys.size} slot(s) taken")
            stopAll()
        }
    }

    private fun snoozeAll() {
        scope.launch {
            val minutes = settingsRepo.current().snoozeMin
            val at = time.now().plusMinutes(minutes.toLong()).toInstant()
            slots.toList().forEach { key ->
                scheduler.cancelEscalation(key)
                scheduler.scheduleSnooze(key, at)
                notifier.cancelReminder(key)
            }
            AppLog.d("alarm snoozed for $minutes min")
            stopAll()
        }
    }

    private fun refresh() {
        scope.launch { reload() }
    }

    /** Re-read what is still pending across every ringing slot; stop if the user already took everything. */
    private suspend fun reload() {
        val keep = LinkedHashSet<SlotKey>()
        val all = ArrayList<DoseUi>()
        for (key in slots) {
            val pending = gateway.pending(key)
            if (pending.isNotEmpty()) {
                keep += key
                all += pending
            }
        }
        slots.retainAll(keep)
        if (keep.isEmpty()) {
            AppLog.d("alarm: nothing pending any more, stopping")
            stopAll()
            return
        }
        doses = all.sortedBy { it.time }
        publish(doses)
        notifier.updateAlarmNotification(doses, settings.snoozeMin)
    }

    private fun publish(list: List<DoseUi>) {
        alarmState.set(AlarmUiState(slots.toSet(), list, settings.snoozeMin, Instant.now()))
    }

    // ---- foreground ----

    private fun enterForeground(notification: android.app.Notification): Boolean {
        // Foreground service types only exist from API 29, and systemExempted only from API 34 (and needs the
        // exact-alarm permission). Everywhere else fall back to mediaPlayback, then to no type at all.
        val candidates = buildList {
            if (Build.VERSION.SDK_INT >= 34 && scheduler.canScheduleExact()) add(ServiceInfo.FOREGROUND_SERVICE_TYPE_SYSTEM_EXEMPTED)
            if (Build.VERSION.SDK_INT >= 29) add(ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK) else add(0)
        }
        for (type in candidates) {
            try {
                ServiceCompat.startForeground(this, AlarmContract.NOTIF_ALARM, notification, type)
                return true
            } catch (e: Exception) {
                AppLog.w("startForeground failed for type $type", e)
            }
        }
        return false
    }

    // ---- sound, vibration, wake lock ----

    private fun startRinging() {
        acquireWakeLock()
        requestAudioFocus()
        startSound()
        if (settings.vibrate) startVibration()
        autoStopJob?.cancel()
        autoStopJob = scope.launch {
            delay(settings.alarmAutoStopMin * 60_000L)
            autoStop()
        }
    }

    private fun alarmAttributes(): AudioAttributes = AudioAttributes.Builder()
        .setUsage(AudioAttributes.USAGE_ALARM)
        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
        .build()

    /** A player for the chosen sound, or the built-in beep if no custom sound is set or it cannot be opened. */
    private fun createPlayer(): MediaPlayer {
        val custom = settings.alarmSoundUri
        if (custom != null) {
            val mp = MediaPlayer()
            try {
                mp.setAudioAttributes(alarmAttributes())
                mp.setDataSource(this, custom.toUri())
                return mp
            } catch (e: Exception) {
                AppLog.w("custom alarm sound unavailable, using the built-in beep", e)
                mp.release()
            }
        }
        val mp = MediaPlayer()
        mp.setAudioAttributes(alarmAttributes())
        resources.openRawResourceFd(R.raw.alarm_beep).use { mp.setDataSource(it.fileDescriptor, it.startOffset, it.length) }
        return mp
    }

    private fun startSound() {
        try {
            val mp = createPlayer()
            mp.isLooping = true
            val startVolume = if (settings.graduallyIncreaseVolume) 0.15f else 1f
            mp.setVolume(startVolume, startVolume)
            mp.prepare()
            mp.start()
            player = mp
            AppLog.d("alarm sound started (ramp=${settings.graduallyIncreaseVolume})")
            if (settings.graduallyIncreaseVolume) {
                rampJob = scope.launch {
                    val steps = 30
                    for (i in 1..steps) {
                        delay(1_000)
                        val v = startVolume + (1f - startVolume) * i / steps
                        player?.setVolume(v, v)
                    }
                }
            }
        } catch (e: Exception) {
            AppLog.e("could not start alarm sound", e)
        }
    }

    private fun startVibration() {
        val v: Vibrator? = if (Build.VERSION.SDK_INT >= 31) {
            getSystemService<VibratorManager>()?.defaultVibrator
        } else {
            @Suppress("DEPRECATION") getSystemService<Vibrator>()
        }
        vibrator = v
        if (v == null || !v.hasVibrator()) return
        val effect = VibrationEffect.createWaveform(longArrayOf(0, 700, 500), 0)
        if (Build.VERSION.SDK_INT >= 33) {
            v.vibrate(effect, VibrationAttributes.createForUsage(VibrationAttributes.USAGE_ALARM))
        } else {
            @Suppress("DEPRECATION") v.vibrate(effect, alarmAttributes())
        }
    }

    private fun acquireWakeLock() {
        val pm = getSystemService<PowerManager>() ?: return
        wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "MedicineReminder:alarm").apply {
            // Timeout equals the auto-stop time (plus a margin) so it can never be held forever.
            acquire(settings.alarmAutoStopMin * 60_000L + 60_000L)
        }
    }

    /** Pause music while the alarm rings. */
    private fun requestAudioFocus() {
        val am = getSystemService<AudioManager>() ?: return
        val request = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT)
            .setAudioAttributes(alarmAttributes())
            .setOnAudioFocusChangeListener { }
            .build()
        focusRequest = request
        am.requestAudioFocus(request)
    }

    // ---- stopping ----

    /** Nobody answered for the configured time: stop and leave a "missed" notification per slot. */
    private suspend fun autoStop() {
        AppLog.d("alarm auto-stopped after ${settings.alarmAutoStopMin} min, posting missed notification(s)")
        for (key in slots.toList()) {
            val pending = gateway.pending(key)
            notifier.showMissed(key, pending)
        }
        stopAll()
    }

    private fun stopAll() {
        autoStopJob?.cancel(); autoStopJob = null
        rampJob?.cancel(); rampJob = null
        runCatching { player?.stop() }
        player?.release(); player = null
        vibrator?.cancel(); vibrator = null
        wakeLock?.let { if (it.isHeld) it.release() }; wakeLock = null
        focusRequest?.let { getSystemService<AudioManager>()?.abandonAudioFocusRequest(it) }; focusRequest = null
        ringing = false
        slots.clear()
        doses = emptyList()
        alarmState.set(null)
        notifier.cancelAlarmNotification()
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    override fun onDestroy() {
        if (ringing) stopAll()
        scope.cancel()
        super.onDestroy()
    }

    companion object {
        /** An escalation fired: start (or merge into) the alarm. Must be called while the app may start a foreground service. */
        fun start(context: Context, key: SlotKey) {
            val intent = Intent(context, AlarmService::class.java).setAction(AlarmContract.ACTION_ALARM_START).apply {
                putExtra(AlarmContract.EXTRA_DATE, key.date.toEpochDay())
                putExtra(AlarmContract.EXTRA_SLOT, key.slot.minuteOfDayInt())
                putExtra(AlarmContract.EXTRA_TEST, key.test)
            }
            ContextCompat.startForegroundService(context, intent)
        }

        /** Send STOP / TOOK / SNOOZE / REFRESH to a service that is already running in the foreground. */
        fun send(context: Context, action: String) {
            context.startService(Intent(context, AlarmService::class.java).setAction(action))
        }
    }
}
