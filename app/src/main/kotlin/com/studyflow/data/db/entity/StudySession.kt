package com.studyflow.data.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * A single study session.
 *
 * `end_time` is null while the session is ongoing. A session is "active"
 * if and only if `end_time` is null — this is the single source of truth
 * the foreground service and UI consult.
 */
@Entity(tableName = "study_sessions")
data class StudySession(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    @ColumnInfo(name = "subject") val subject: String,
    @ColumnInfo(name = "chapter") val chapter: String? = null,
    @ColumnInfo(name = "mode") val mode: String? = null,   // theory | practice | revision | mock_test
    @ColumnInfo(name = "start_time") val startTime: Long,
    @ColumnInfo(name = "end_time") val endTime: Long? = null,
    @ColumnInfo(name = "duration_seconds") val durationSeconds: Long = 0L,
    @ColumnInfo(name = "break_duration_seconds") val breakDurationSeconds: Long = 0L,
    @ColumnInfo(name = "notes") val notes: String? = null,
    @ColumnInfo(name = "mood_rating") val moodRating: Int? = null,   // 1..5
    @ColumnInfo(name = "focus_score") val focusScore: Int? = null,   // 1..5
    @ColumnInfo(name = "source") val source: String = "manual",      // "voice" | "manual"
    @ColumnInfo(name = "created_at") val createdAt: Long = System.currentTimeMillis(),
) {
    companion object {
        const val MODE_THEORY = "theory"
        const val MODE_PRACTICE = "practice"
        const val MODE_REVISION = "revision"
        const val MODE_MOCK_TEST = "mock_test"

        const val SOURCE_VOICE = "voice"
        const val SOURCE_MANUAL = "manual"

        /** Sessions shorter than this are logged but excluded from streaks. */
        const val MIN_STREAK_SECONDS = 15L * 60
    }
}
