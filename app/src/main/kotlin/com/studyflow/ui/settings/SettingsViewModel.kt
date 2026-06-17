package com.studyflow.ui.settings

import android.app.Application
import android.content.Intent
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.studyflow.data.datastore.UserPreferencesDataStore
import com.studyflow.data.repository.SessionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    application: Application,
    private val prefs: UserPreferencesDataStore,
    private val repo: SessionRepository,
) : AndroidViewModel(application) {

    val dailyGoalMinutes = prefs.dailyGoalMinutes.stateIn(viewModelScope, SharingStarted.Eagerly, 120)
    val pomodoroWork = prefs.pomodoroWork.stateIn(viewModelScope, SharingStarted.Eagerly, 25)
    val pomodoroBreak = prefs.pomodoroBreak.stateIn(viewModelScope, SharingStarted.Eagerly, 5)
    val defaultMode = prefs.defaultMode.stateIn(viewModelScope, SharingStarted.Eagerly, "theory")
    val theme = prefs.theme.stateIn(viewModelScope, SharingStarted.Eagerly, "system")
    val firstDayOfWeek = prefs.firstDayOfWeek.stateIn(viewModelScope, SharingStarted.Eagerly, "mon")
    val reminderDailyEnabled = prefs.reminderDailyEnabled.stateIn(viewModelScope, SharingStarted.Eagerly, true)
    val reminderStreakEnabled = prefs.reminderStreakEnabled.stateIn(viewModelScope, SharingStarted.Eagerly, true)
    val maxBreakMinutes = prefs.maxBreakMinutes.stateIn(viewModelScope, SharingStarted.Eagerly, 15)

    fun setDailyGoal(v: Int) = viewModelScope.launch { prefs.setDailyGoalMinutes(v) }
    fun setPomodoroWork(v: Int) = viewModelScope.launch { prefs.setPomodoroWork(v) }
    fun setPomodoroBreak(v: Int) = viewModelScope.launch { prefs.setPomodoroBreak(v) }
    fun setDefaultMode(v: String) = viewModelScope.launch { prefs.setDefaultMode(v) }
    fun setTheme(v: String) = viewModelScope.launch { prefs.setTheme(v) }
    fun setFirstDayOfWeek(v: String) = viewModelScope.launch { prefs.setFirstDayOfWeek(v) }
    fun setReminderDailyEnabled(v: Boolean) = viewModelScope.launch { prefs.setReminderDailyEnabled(v) }
    fun setReminderStreakEnabled(v: Boolean) = viewModelScope.launch { prefs.setReminderStreakEnabled(v) }
    fun setMaxBreak(v: Int) = viewModelScope.launch { prefs.setMaxBreakMinutes(v) }

    fun clearAll() = viewModelScope.launch {
        repo.clearAllSessions()
        repo.clearAllSubjects()
    }

    fun exportCsv() = viewModelScope.launch {
        val ctx = getApplication<Application>()
        val df = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        val csv = StringBuilder().apply {
            appendLine("id,subject,chapter,mode,start_time,end_time,duration_seconds,break_seconds,source,notes")
            // Pull a snapshot of all completed sessions.
            // observeAll() returns a Flow, so we take its first emission.
            val sessions = repo.observeAll().first()
            sessions.forEach { s ->
                appendLine(listOf(
                    s.id,
                    csvEscape(s.subject),
                    csvEscape(s.chapter ?: ""),
                    csvEscape(s.mode ?: ""),
                    df.format(Date(s.startTime)),
                    s.endTime?.let { df.format(Date(it)) } ?: "",
                    s.durationSeconds,
                    s.breakDurationSeconds,
                    s.source,
                    csvEscape(s.notes ?: ""),
                ).joinToString(","))
            }
        }.toString()
        val send = Intent(Intent.ACTION_SEND).apply {
            type = "text/csv"
            putExtra(Intent.EXTRA_SUBJECT, "StudyFlow sessions.csv")
            putExtra(Intent.EXTRA_TEXT, csv)
        }
        ctx.startActivity(Intent.createChooser(send, "Export CSV").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }

    private fun csvEscape(s: String): String {
        return if (s.contains(',') || s.contains('"') || s.contains('\n')) {
            "\"" + s.replace("\"", "\"\"") + "\""
        } else s
    }
}
