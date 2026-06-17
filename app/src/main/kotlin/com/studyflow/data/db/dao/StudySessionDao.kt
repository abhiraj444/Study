package com.studyflow.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.studyflow.data.db.entity.StudySession
import kotlinx.coroutines.flow.Flow

@Dao
interface StudySessionDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(session: StudySession): Long

    @Update
    suspend fun update(session: StudySession)

    /** The single active session (end_time IS NULL), if any. */
    @Query("SELECT * FROM study_sessions WHERE end_time IS NULL ORDER BY start_time DESC LIMIT 1")
    fun observeActive(): Flow<StudySession?>

    @Query("SELECT * FROM study_sessions WHERE end_time IS NULL ORDER BY start_time DESC LIMIT 1")
    suspend fun getActive(): StudySession?

    @Query("SELECT * FROM study_sessions WHERE id = :id")
    suspend fun getById(id: Long): StudySession?

    @Query("SELECT * FROM study_sessions WHERE end_time IS NOT NULL ORDER BY start_time DESC")
    fun observeAll(): Flow<List<StudySession>>

    @Query("""
        SELECT * FROM study_sessions
        WHERE end_time IS NOT NULL
          AND date(start_time/1000, 'unixepoch', 'localtime') = :dateIso
        ORDER BY start_time DESC
    """)
    fun observeForDate(dateIso: String): Flow<List<StudySession>>

    @Query("""
        SELECT * FROM study_sessions
        WHERE end_time IS NOT NULL
          AND date(start_time/1000, 'unixepoch', 'localtime') BETWEEN :fromIso AND :toIso
        ORDER BY start_time ASC
    """)
    suspend fun getBetween(fromIso: String, toIso: String): List<StudySession>

    @Query("""
        SELECT COALESCE(SUM(duration_seconds), 0) FROM study_sessions
        WHERE end_time IS NOT NULL
          AND date(start_time/1000, 'unixepoch', 'localtime') = :dateIso
    """)
    suspend fun totalSecondsForDate(dateIso: String): Long

    @Query("""
        SELECT subject, SUM(duration_seconds) AS total
        FROM study_sessions
        WHERE end_time IS NOT NULL
          AND start_time >= :sinceEpochMs
        GROUP BY subject
        ORDER BY total DESC
    """)
    suspend fun totalsBySubject(sinceEpochMs: Long): List<SubjectTotal>

    @Query("""
        SELECT mode, SUM(duration_seconds) AS total
        FROM study_sessions
        WHERE end_time IS NOT NULL
          AND start_time >= :sinceEpochMs
        GROUP BY mode
    """)
    suspend fun totalsByMode(sinceEpochMs: Long): List<ModeTotal>

    @Query("""
        SELECT date(start_time/1000, 'unixepoch', 'localtime') AS day,
               SUM(duration_seconds) AS total
        FROM study_sessions
        WHERE end_time IS NOT NULL AND start_time >= :sinceEpochMs
        GROUP BY day
        ORDER BY day ASC
    """)
    suspend fun dailyTotals(sinceEpochMs: Long): List<DayTotal>

    @Query("""
        SELECT (start_time/1000 % 86400) / 3600 AS hour, COUNT(*) AS count
        FROM study_sessions
        WHERE end_time IS NOT NULL AND start_time >= :sinceEpochMs
        GROUP BY hour
        ORDER BY count DESC
        LIMIT 1
    """)
    suspend fun mostProductiveHour(sinceEpochMs: Long): HourCount?

    @Query("""
        SELECT MAX(duration_seconds) AS longest
        FROM study_sessions
        WHERE end_time IS NOT NULL AND start_time >= :sinceEpochMs
    """)
    suspend fun longestSession(sinceEpochMs: Long): Long?

    @Query("""
        SELECT AVG(duration_seconds) AS avg
        FROM study_sessions
        WHERE end_time IS NOT NULL AND start_time >= :sinceEpochMs
    """)
    suspend fun averageSessionSeconds(sinceEpochMs: Long): Long?

    @Query("SELECT COUNT(*) FROM study_sessions WHERE end_time IS NOT NULL AND start_time >= :sinceEpochMs")
    suspend fun countSince(sinceEpochMs: Long): Int

    @Query("SELECT DISTINCT chapter FROM study_sessions WHERE subject = :subject AND chapter IS NOT NULL ORDER BY start_time DESC LIMIT 10")
    suspend fun recentChaptersFor(subject: String): List<String>

    @Query("DELETE FROM study_sessions WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM study_sessions")
    suspend fun clearAll()
}

data class SubjectTotal(val subject: String, val total: Long)
data class ModeTotal(val mode: String, val total: Long)
data class DayTotal(val day: String, val total: Long)
data class HourCount(val hour: Int, val count: Int)
