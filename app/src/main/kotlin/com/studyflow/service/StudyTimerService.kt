package com.studyflow.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import androidx.core.app.NotificationCompat
import com.studyflow.R
import com.studyflow.data.db.entity.StudySession
import com.studyflow.receiver.NotificationActionReceiver
import com.studyflow.ui.SessionActivity
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Foreground service that keeps the active study session alive while the user
 * is away from the app.
 *
 * Responsibilities (spec §9):
 *  - Acquire a partial wake lock so the timer doesn't drift on screen-off
 *  - Show a persistent low-importance notification with subject + elapsed time
 *  - Expose "Take Break" and "Stop" actions
 *  - Update every second while ACTIVE; tick down while ON_BREAK
 *
 * Communication:
 *  - Intents with action = ACTION_START / ACTION_STOP / ACTION_BREAK / ACTION_RESUME
 *  - Result broadcast ACTION_STATE_BROADCAST so SessionActivity can update UI
 */
@AndroidEntryPoint
class StudyTimerService : Service() {

    @Inject lateinit var sessionHolder: ActiveSessionHolder

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var tickJob: Job? = null
    private var wakeLock: PowerManager.WakeLock? = null

    private var state: TimerState = TimerState.IDLE
    private var subject: String = ""
    private var chapter: String? = null
    private var mode: String? = null
    private var startEpochMs: Long = 0L
    private var breakEndEpochMs: Long = 0L
    private var accumulatedBreakMs: Long = 0L

    override fun onCreate() {
        super.onCreate()
        ensureChannels(this)
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> {
                subject = intent.getStringExtra(EXTRA_SUBJECT) ?: "Study"
                chapter = intent.getStringExtra(EXTRA_CHAPTER)
                mode = intent.getStringExtra(EXTRA_MODE)
                startEpochMs = System.currentTimeMillis()
                accumulatedBreakMs = 0L
                state = TimerState.ACTIVE
                sessionHolder.setActive(subject, chapter, mode, startEpochMs)
                startForegroundCompat(buildNotification())
                startTicking()
                acquireWakeLock()
            }
            ACTION_STOP -> {
                stopEverything()
            }
            ACTION_BREAK -> {
                if (state == TimerState.ACTIVE) {
                    state = TimerState.ON_BREAK
                    val breakMs = intent.getLongExtra(EXTRA_BREAK_MS, 5 * 60 * 1000L)
                    breakEndEpochMs = System.currentTimeMillis() + breakMs
                    refreshNotification()
                }
            }
            ACTION_RESUME -> {
                if (state == TimerState.ON_BREAK) {
                    accumulatedBreakMs += (breakEndEpochMs - System.currentTimeMillis()).coerceAtLeast(0)
                    state = TimerState.ACTIVE
                    refreshNotification()
                }
            }
        }
        return START_STICKY
    }

    private fun startTicking() {
        tickJob?.cancel()
        tickJob = scope.launch {
            while (true) {
                delay(1000L)
                if (state == TimerState.ON_BREAK && System.currentTimeMillis() >= breakEndEpochMs) {
                    // Break over — auto-resume
                    state = TimerState.ACTIVE
                }
                refreshNotification()
                sendStateBroadcast()
            }
        }
    }

    private fun stopEverything() {
        tickJob?.cancel()
        tickJob = null
        state = TimerState.IDLE
        sessionHolder.clear()
        releaseWakeLock()
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun acquireWakeLock() {
        if (wakeLock?.isHeld == true) return
        val pm = getSystemService(POWER_SERVICE) as PowerManager
        wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "studyflow:timer").apply {
            setReferenceCounted(false)
            acquire(8 * 60 * 60 * 1000L /* 8h max */)
        }
    }

    private fun releaseWakeLock() {
        wakeLock?.takeIf { it.isHeld }?.release()
        wakeLock = null
    }

    private fun refreshNotification() {
        val nm = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        nm.notify(NOTIF_ID, buildNotification())
    }

    private fun buildNotification(): Notification {
        val now = System.currentTimeMillis()
        val elapsedMs = when (state) {
            TimerState.ACTIVE -> now - startEpochMs - accumulatedBreakMs
            TimerState.ON_BREAK -> breakEndEpochMs - now
            else -> 0L
        }
        val isBreak = state == TimerState.ON_BREAK
        val title = if (isBreak) getString(R.string.notif_session_title_break, subject)
                    else getString(R.string.notif_session_title_studying, subject)
        val text = if (isBreak) "Resuming in ${formatMs(elapsedMs.coerceAtLeast(0))}"
                   else "Elapsed: ${formatMs(elapsedMs.coerceAtLeast(0))}"

        val openIntent = Intent(this, SessionActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val openPi = PendingIntent.getActivity(this, 0, openIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)

        val stopPi = broadcastPi(NotificationActionReceiver.ACTION_STOP, 1)
        val breakPi = broadcastPi(NotificationActionReceiver.ACTION_BREAK, 2)
        val resumePi = broadcastPi(NotificationActionReceiver.ACTION_RESUME, 3)

        val builder = NotificationCompat.Builder(this, CHANNEL_SESSION)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(text)
            .setContentIntent(openPi)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setShowWhen(false)
            .setUsesChronometer(false)
            .setColorized(true)
            .setColor(if (isBreak) 0xFF48C9B0.toInt() else 0xFF7C6AF7.toInt())
            .setPriority(NotificationCompat.PRIORITY_LOW)

        when (state) {
            TimerState.ACTIVE -> builder.addAction(0, getString(R.string.notif_action_break), breakPi)
            TimerState.ON_BREAK -> builder.addAction(0, getString(R.string.notif_action_resume), resumePi)
            else -> {}
        }
        builder.addAction(0, getString(R.string.notif_action_stop), stopPi)
        return builder.build()
    }

    private fun broadcastPi(action: String, requestCode: Int): PendingIntent {
        val intent = Intent(this, NotificationActionReceiver::class.java).apply {
            this.action = action
        }
        return PendingIntent.getBroadcast(this, requestCode, intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
    }

    private fun sendStateBroadcast() {
        val intent = Intent(ACTION_STATE_BROADCAST).apply {
            setPackage(packageName)
            putExtra(EXTRA_STATE, state.name)
            putExtra(EXTRA_SUBJECT, subject)
            putExtra(EXTRA_START_MS, startEpochMs)
            putExtra(EXTRA_ACCUMULATED_BREAK_MS, accumulatedBreakMs)
            if (state == TimerState.ON_BREAK) putExtra(EXTRA_BREAK_END_MS, breakEndEpochMs)
        }
        // setPackage() restricts delivery to receivers in our own package — safe on all API levels.
        sendBroadcast(intent)
    }

    private fun startForegroundCompat(notification: Notification) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(NOTIF_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
        } else {
            startForeground(NOTIF_ID, notification)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        tickJob?.cancel()
        scope.cancel()
        releaseWakeLock()
    }

    enum class TimerState { IDLE, ACTIVE, ON_BREAK }

    companion object {
        const val CHANNEL_SESSION = "study_session_channel"
        const val CHANNEL_REMINDERS = "study_reminder_channel"
        const val NOTIF_ID = 4242

        const val ACTION_START = "com.studyflow.action.START"
        const val ACTION_STOP = "com.studyflow.action.STOP"
        const val ACTION_BREAK = "com.studyflow.action.BREAK"
        const val ACTION_RESUME = "com.studyflow.action.RESUME"
        const val ACTION_STATE_BROADCAST = "com.studyflow.action.STATE_BROADCAST"

        const val EXTRA_SUBJECT = "subject"
        const val EXTRA_CHAPTER = "chapter"
        const val EXTRA_MODE = "mode"
        const val EXTRA_BREAK_MS = "break_ms"
        const val EXTRA_STATE = "state"
        const val EXTRA_START_MS = "start_ms"
        const val EXTRA_ACCUMULATED_BREAK_MS = "accumulated_break_ms"
        const val EXTRA_BREAK_END_MS = "break_end_ms"

        fun ensureChannels(context: Context) {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
            val nm = context.getSystemService(NOTIFICATION_SERVICE) as NotificationManager
            nm.createNotificationChannel(NotificationChannel(CHANNEL_SESSION,
                context.getString(R.string.notif_channel_session),
                NotificationManager.IMPORTANCE_LOW).apply {
                description = "Active study session timer"
                setShowBadge(false)
            })
            nm.createNotificationChannel(NotificationChannel(CHANNEL_REMINDERS,
                context.getString(R.string.notif_channel_reminders),
                NotificationManager.IMPORTANCE_DEFAULT).apply {
                description = "Reminders to study and streak nudges"
            })
        }

        fun formatMs(ms: Long): String {
            val totalSec = ms / 1000
            val h = totalSec / 3600
            val m = (totalSec % 3600) / 60
            val s = totalSec % 60
            return if (h > 0) String.format("%02d:%02d:%02d", h, m, s)
                   else String.format("%02d:%02d", m, s)
        }
    }
}

/** Holds the currently-active session metadata in memory for the UI to consult. */
@javax.inject.Singleton
class ActiveSessionHolder @javax.inject.Inject constructor() {
    @Volatile var subject: String = ""
    @Volatile var chapter: String? = null
    @Volatile var mode: String? = null
    @Volatile var startEpochMs: Long = 0L

    fun setActive(subject: String, chapter: String?, mode: String?, start: Long) {
        this.subject = subject
        this.chapter = chapter
        this.mode = mode
        this.startEpochMs = start
    }
    fun clear() {
        subject = ""
        chapter = null
        mode = null
        startEpochMs = 0L
    }
}
