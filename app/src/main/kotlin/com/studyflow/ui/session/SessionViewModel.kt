package com.studyflow.ui.session

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.studyflow.data.db.entity.StudySession
import com.studyflow.data.repository.SessionRepository
import com.studyflow.domain.usecase.StartSessionUseCase
import com.studyflow.domain.usecase.StopSessionUseCase
import com.studyflow.service.ActiveSessionHolder
import com.studyflow.service.StudyTimerService
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Parses free-text durations like "10 minutes", "half an hour", "5m". */
private fun parseDuration(text: String): Long {
    val lower = text.lowercase().trim()
    val minutesRegex = Regex("(\\d+)\\s*(min|mins|minute|minutes)")
    val hoursRegex = Regex("(\\d+)\\s*(hr|hrs|hour|hours)")
    val sec = Regex("(\\d+)\\s*(sec|secs|second|seconds)")

    minutesRegex.find(lower)?.let { return it.groupValues[1].toLong() * 60_000L }
    hoursRegex.find(lower)?.let { return it.groupValues[1].toLong() * 3_600_000L }
    sec.find(lower)?.let { return it.groupValues[1].toLong() * 1_000L }
    if (lower.contains("half an hour")) return 30 * 60_000L
    if (lower.contains("quarter hour") || lower.contains("15 min")) return 15 * 60_000L
    return 5 * 60_000L // safe default
}

@HiltViewModel
class SessionViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val repo: SessionRepository,
    private val startSession: StartSessionUseCase,
    private val stopSession: StopSessionUseCase,
    private val activeSessionHolder: ActiveSessionHolder,
) : ViewModel() {

    private val _toast = MutableStateFlow<String?>(null)
    val toast: StateFlow<String?> = _toast.asStateFlow()

    private val _showRatingSheet = MutableStateFlow(false)
    val showRatingSheet: StateFlow<Boolean> = _showRatingSheet.asStateFlow()

    private val _pendingRatingSessionId = MutableStateFlow<Long?>(null)

    /** Streams the active session (null when none). */
    private val activeSessionFlow = repo.observeActive().stateIn(
        viewModelScope, SharingStarted.Eagerly, null
    )

    private val _serviceState = MutableStateFlow(ServiceState())

    val uiState: StateFlow<SessionUiState> = combine(
        activeSessionFlow,
        _serviceState,
        _showRatingSheet,
    ) { session, svcState, showRating ->
        SessionUiState(
            activeSession = session,
            subject = session?.subject ?: activeSessionHolder.subject,
            chapter = session?.chapter ?: activeSessionHolder.chapter,
            mode = session?.mode ?: activeSessionHolder.mode,
            startEpochMs = session?.startTime ?: 0L,
            timerState = svcState.timerState,
            breakEndEpochMs = svcState.breakEndEpochMs,
            accumulatedBreakMs = svcState.accumulatedBreakMs,
            showRatingSheet = showRating,
        )
    }.stateIn(viewModelScope, SharingStarted.Eagerly, SessionUiState())

    /** Broadcast receiver for live service ticks. */
    private val stateReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action != StudyTimerService.ACTION_STATE_BROADCAST) return
            val stateName = intent.getStringExtra(StudyTimerService.EXTRA_STATE) ?: return
            _serviceState.value = ServiceState(
                timerState = runCatching { StudyTimerService.TimerState.valueOf(stateName) }
                    .getOrDefault(StudyTimerService.TimerState.IDLE),
                breakEndEpochMs = intent.getLongExtra(StudyTimerService.EXTRA_BREAK_END_MS, 0L),
                accumulatedBreakMs = intent.getLongExtra(StudyTimerService.EXTRA_ACCUMULATED_BREAK_MS, 0L),
            )
        }
    }

    init {
        // Package-internal broadcast — register as not-exported for Android 13+ compatibility.
        val flags = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU)
            Context.RECEIVER_NOT_EXPORTED else 0
        if (flags != 0) {
            context.registerReceiver(stateReceiver, IntentFilter(StudyTimerService.ACTION_STATE_BROADCAST), flags)
        } else {
            context.registerReceiver(stateReceiver, IntentFilter(StudyTimerService.ACTION_STATE_BROADCAST))
        }
    }

    override fun onCleared() {
        runCatching { context.unregisterReceiver(stateReceiver) }
        super.onCleared()
    }

    // ===== Voice handlers (called from SessionActivity.handleVoiceIntent) =====

    fun handleVoiceStart(subject: String, chapter: String?, mode: String?) {
        viewModelScope.launch {
            val autoStopped = startSession(
                subject = subject,
                chapter = chapter,
                mode = normalizeMode(mode),
                source = StudySession.SOURCE_VOICE,
            )
            _toast.value = if (autoStopped) "Previous session auto-stopped"
                           else "Session started: $subject"
        }
    }

    fun handleVoiceStop() {
        viewModelScope.launch {
            val stopped = stopSession()
            if (stopped == null) {
                _toast.value = "No active session found"
            } else {
                _pendingRatingSessionId.value = stopped.id
                _showRatingSheet.value = true
                _toast.value = "Session logged"
            }
        }
    }

    fun handleVoiceBreak(durationText: String) {
        val ms = parseDuration(durationText)
        val intent = Intent(context, StudyTimerService::class.java).apply {
            action = StudyTimerService.ACTION_BREAK
            putExtra(StudyTimerService.EXTRA_BREAK_MS, ms)
        }
        context.startService(intent)
    }

    // ===== Manual UI handlers =====

    fun onStopClicked() = handleVoiceStop()
    fun onBreakClicked() = handleVoiceBreak("5 minutes")
    fun onResumeClicked() {
        val intent = Intent(context, StudyTimerService::class.java).apply {
            action = StudyTimerService.ACTION_RESUME
        }
        context.startService(intent)
    }

    fun submitRating(mood: Int, focus: Int, note: String) {
        val sessionId = _pendingRatingSessionId.value ?: return
        viewModelScope.launch {
            val s = repo.getById(sessionId) ?: return@launch
            repo.update(s.copy(moodRating = mood, focusScore = focus, notes = note))
            _showRatingSheet.value = false
            _pendingRatingSessionId.value = null
        }
    }

    fun dismissRating() {
        _showRatingSheet.value = false
        _pendingRatingSessionId.value = null
    }

    fun consumeToast() { _toast.value = null }

    private fun normalizeMode(raw: String?): String? {
        if (raw == null) return null
        val l = raw.lowercase().trim()
        return when {
            l.startsWith("the") -> StudySession.MODE_THEORY
            l.startsWith("prac") -> StudySession.MODE_PRACTICE
            l.startsWith("rev") -> StudySession.MODE_REVISION
            l.startsWith("mock") -> StudySession.MODE_MOCK_TEST
            else -> StudySession.MODE_THEORY
        }
    }
}

data class SessionUiState(
    val activeSession: StudySession? = null,
    val subject: String = "",
    val chapter: String? = null,
    val mode: String? = null,
    val startEpochMs: Long = 0L,
    val timerState: StudyTimerService.TimerState = StudyTimerService.TimerState.IDLE,
    val breakEndEpochMs: Long = 0L,
    val accumulatedBreakMs: Long = 0L,
    val showRatingSheet: Boolean = false,
)

private data class ServiceState(
    val timerState: StudyTimerService.TimerState = StudyTimerService.TimerState.IDLE,
    val breakEndEpochMs: Long = 0L,
    val accumulatedBreakMs: Long = 0L,
)
