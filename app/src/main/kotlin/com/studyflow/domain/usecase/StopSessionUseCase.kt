package com.studyflow.domain.usecase

import android.content.Context
import android.content.Intent
import com.studyflow.data.db.entity.StudySession
import com.studyflow.data.repository.SessionRepository
import com.studyflow.service.StudyTimerService
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject

/**
 * Stops the active study session (if any) and records it permanently.
 *
 * Returns the stopped session's id, or null if no active session existed.
 * Also signals the foreground service to stop.
 */
class StopSessionUseCase @Inject constructor(
    private val repo: SessionRepository,
    @ApplicationContext private val context: Context,
) {
    suspend operator fun invoke(): StudySession? {
        val active = repo.getActive() ?: return null
        val end = System.currentTimeMillis()
        val durationSeconds = (end - active.startTime) / 1000
        val updated = active.copy(
            endTime = end,
            durationSeconds = durationSeconds,
        )
        repo.update(updated)

        // Streaks + daily goal — only count if session was long enough.
        if (durationSeconds >= StudySession.MIN_STREAK_SECONDS) {
            val endDate = LocalDate.ofInstant(Instant.ofEpochMilli(end), ZoneId.systemDefault())
            repo.updateStreakOnSessionComplete(endDate, durationSeconds)
            val startDate = LocalDate.ofInstant(Instant.ofEpochMilli(active.startTime), ZoneId.systemDefault())
            val minutes = (durationSeconds / 60).toInt()
            repo.addAchievedMinutes(startDate, minutes)
        }

        val stopIntent = Intent(context, StudyTimerService::class.java).apply {
            action = StudyTimerService.ACTION_STOP
        }
        context.startService(stopIntent)
        return updated
    }
}
