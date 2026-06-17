package com.studyflow.domain.usecase

import com.studyflow.data.repository.SessionRepository
import com.studyflow.domain.model.StreakInfo
import kotlinx.coroutines.flow.first
import javax.inject.Inject

/**
 * Computes the user's current streak. Reads the streaks table snapshot.
 *
 * A day counts toward the streak only if the user logged >= 15 minutes of
 * study time on that day (see StudySession.MIN_STREAK_SECONDS).
 */
class CalculateStreakUseCase @Inject constructor(
    private val repo: SessionRepository,
) {
    suspend operator fun invoke(): StreakInfo {
        val s = repo.observeStreak().first() ?: return StreakInfo(0, 0, null)
        return StreakInfo(s.currentStreak, s.longestStreak, s.lastStudyDate)
    }
}
