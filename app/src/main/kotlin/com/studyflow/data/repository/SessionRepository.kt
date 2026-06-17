package com.studyflow.data.repository

import com.studyflow.data.db.dao.DailyGoalDao
import com.studyflow.data.db.dao.StreakDao
import com.studyflow.data.db.dao.StudySessionDao
import com.studyflow.data.db.dao.SubjectDao
import com.studyflow.data.db.dao.DayTotal
import com.studyflow.data.db.dao.ModeTotal
import com.studyflow.data.db.dao.SubjectTotal
import com.studyflow.data.db.entity.DailyGoal
import com.studyflow.data.db.entity.Streak
import com.studyflow.data.db.entity.StudySession
import com.studyflow.data.db.entity.Subject
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SessionRepository @Inject constructor(
    private val sessionDao: StudySessionDao,
    private val subjectDao: SubjectDao,
    private val dailyGoalDao: DailyGoalDao,
    private val streakDao: StreakDao,
) {
    private val isoDate = DateTimeFormatter.ISO_LOCAL_DATE

    fun observeActive(): Flow<StudySession?> = sessionDao.observeActive()
    suspend fun getActive(): StudySession? = sessionDao.getActive()
    suspend fun getById(id: Long): StudySession? = sessionDao.getById(id)

    suspend fun insert(session: StudySession): Long = sessionDao.insert(session)
    suspend fun update(session: StudySession) = sessionDao.update(session)
    suspend fun deleteById(id: Long) = sessionDao.deleteById(id)

    fun observeAll(): Flow<List<StudySession>> = sessionDao.observeAll()
    fun observeForDate(date: LocalDate): Flow<List<StudySession>> =
        sessionDao.observeForDate(date.format(isoDate))

    suspend fun totalSecondsForDate(date: LocalDate): Long =
        sessionDao.totalSecondsForDate(date.format(isoDate))

    suspend fun totalsBySubject(sinceEpochMs: Long): List<SubjectTotal> =
        sessionDao.totalsBySubject(sinceEpochMs)

    suspend fun totalsByMode(sinceEpochMs: Long): List<ModeTotal> =
        sessionDao.totalsByMode(sinceEpochMs)

    suspend fun dailyTotals(sinceEpochMs: Long): List<DayTotal> =
        sessionDao.dailyTotals(sinceEpochMs)

    suspend fun mostProductiveHour(sinceEpochMs: Long) = sessionDao.mostProductiveHour(sinceEpochMs)
    suspend fun longestSession(sinceEpochMs: Long) = sessionDao.longestSession(sinceEpochMs) ?: 0L
    suspend fun averageSessionSeconds(sinceEpochMs: Long) = sessionDao.averageSessionSeconds(sinceEpochMs) ?: 0L
    suspend fun countSince(sinceEpochMs: Long) = sessionDao.countSince(sinceEpochMs)

    suspend fun recentChaptersFor(subject: String): List<String> =
        sessionDao.recentChaptersFor(subject)

    suspend fun clearAllSessions() = sessionDao.clearAll()

    // ===== Daily goals =====
    suspend fun ensureDailyGoal(date: LocalDate, targetMinutes: Int) {
        val existing = dailyGoalDao.getByDate(date.format(isoDate))
        if (existing == null) {
            dailyGoalDao.upsert(DailyGoal(date = date.format(isoDate), targetMinutes = targetMinutes))
        }
    }

    suspend fun addAchievedMinutes(date: LocalDate, deltaMinutes: Int) {
        ensureDailyGoal(date, targetMinutes = 120)
        dailyGoalDao.addAchieved(date.format(isoDate), deltaMinutes)
    }

    // ===== Streaks =====
    suspend fun updateStreakOnSessionComplete(sessionEnd: LocalDate, durationSeconds: Long) {
        val current = streakDao.get() ?: Streak()
        val today = sessionEnd.format(isoDate)

        if (durationSeconds < StudySession.MIN_STREAK_SECONDS) return

        val updated = when (current.lastStudyDate) {
            today -> current // already counted today
            sessionEnd.minusDays(1).format(isoDate) -> current.copy(
                currentStreak = current.currentStreak + 1,
                longestStreak = maxOf(current.longestStreak, current.currentStreak + 1),
                lastStudyDate = today,
            )
            else -> current.copy(currentStreak = 1, lastStudyDate = today) // streak reset
        }
        streakDao.upsert(updated)
    }

    fun observeStreak(): Flow<Streak?> = streakDao.observe()

    // ===== Subjects =====
    fun observeActiveSubjects(): Flow<List<Subject>> = subjectDao.observeActive()
    suspend fun getActiveSubjects(): List<Subject> = subjectDao.getActive()
    suspend fun upsertSubject(subject: Subject): Long = subjectDao.upsert(subject)
    suspend fun deleteSubject(subject: Subject) = subjectDao.delete(subject)
    suspend fun archiveSubject(id: Long) = subjectDao.setActive(id, 0)
    suspend fun getSubjectByName(name: String): Subject? = subjectDao.getByName(name)
    suspend fun clearAllSubjects() = subjectDao.clearAll()
}
