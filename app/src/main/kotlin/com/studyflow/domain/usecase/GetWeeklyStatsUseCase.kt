package com.studyflow.domain.usecase

import com.studyflow.data.db.dao.DayTotal
import com.studyflow.data.db.dao.HourCount
import com.studyflow.data.db.dao.ModeTotal
import com.studyflow.data.db.dao.SubjectTotal
import com.studyflow.data.repository.SessionRepository
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject

data class WeeklyStats(
    val dailyTotals: List<DayTotal>,
    val subjectTotals: List<SubjectTotal>,
    val modeTotals: List<ModeTotal>,
    val totalSessions: Int,
    val averageSessionSeconds: Long,
    val longestSessionSeconds: Long,
    val mostProductiveHour: HourCount?,
)

class GetWeeklyStatsUseCase @Inject constructor(
    private val repo: SessionRepository,
) {
    suspend operator fun invoke(days: Int = 7): WeeklyStats {
        val zone = ZoneId.systemDefault()
        val sinceMs = Instant.now().minusSeconds(days * 24 * 3600L).toEpochMilli()
        return WeeklyStats(
            dailyTotals = repo.dailyTotals(sinceMs),
            subjectTotals = repo.totalsBySubject(sinceMs),
            modeTotals = repo.totalsByMode(sinceMs),
            totalSessions = repo.countSince(sinceMs),
            averageSessionSeconds = repo.averageSessionSeconds(sinceMs),
            longestSessionSeconds = repo.longestSession(sinceMs),
            mostProductiveHour = repo.mostProductiveHour(sinceMs),
        )
    }
}
