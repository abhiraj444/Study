package com.studyflow.domain.usecase

import android.content.Context
import android.content.Intent
import com.studyflow.data.db.entity.StudySession
import com.studyflow.data.repository.SessionRepository
import com.studyflow.service.StudyTimerService
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject

/**
 * Starts a new study session.
 *
 * Business rules (from spec §11):
 *  - If a session is already active, auto-stop it first and surface a snackbar.
 *  - Insert a new session row with end_time = NULL.
 *  - Start the foreground StudyTimerService.
 *  - Ensure today's daily_goal row exists.
 */
class StartSessionUseCase @Inject constructor(
    private val repo: SessionRepository,
    @ApplicationContext private val context: Context,
) {
    /** Returns true if a previous session had to be auto-stopped. */
    suspend operator fun invoke(
        subject: String,
        chapter: String? = null,
        mode: String? = null,
        source: String = StudySession.SOURCE_VOICE,
    ): Boolean {
        var autoStopped = false
        repo.getActive()?.let { active ->
            stopInternal(active)
            autoStopped = true
        }

        val now = System.currentTimeMillis()
        repo.insert(
            StudySession(
                subject = subject,
                chapter = chapter,
                mode = mode,
                startTime = now,
                source = source,
            )
        )
        repo.ensureDailyGoal(LocalDate.now(), targetMinutes = 120)

        val svc = Intent(context, StudyTimerService::class.java).apply {
            action = StudyTimerService.ACTION_START
            putExtra(StudyTimerService.EXTRA_SUBJECT, subject)
            putExtra(StudyTimerService.EXTRA_CHAPTER, chapter)
            putExtra(StudyTimerService.EXTRA_MODE, mode)
        }
        context.startForegroundService(svc)
        return autoStopped
    }

    private suspend fun stopInternal(s: StudySession) {
        val end = System.currentTimeMillis()
        val durationSeconds = (end - s.startTime) / 1000
        val updated = s.copy(
            endTime = end,
            durationSeconds = durationSeconds,
        )
        repo.update(updated)
        val endDate = LocalDate.ofInstant(java.time.Instant.ofEpochMilli(end), ZoneId.systemDefault())
        repo.updateStreakOnSessionComplete(endDate, durationSeconds)
        val minutes = (durationSeconds / 60).toInt().coerceAtLeast(0)
        val startDate = LocalDate.ofInstant(java.time.Instant.ofEpochMilli(s.startTime), ZoneId.systemDefault())
        repo.addAchievedMinutes(startDate, minutes)
    }
}
