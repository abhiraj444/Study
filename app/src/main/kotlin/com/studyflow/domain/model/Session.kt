package com.studyflow.domain.model

import com.studyflow.data.db.entity.StudySession

/** UI-facing representation of a study session. */
data class SessionInfo(
    val id: Long,
    val subject: String,
    val chapter: String?,
    val mode: String?,
    val startTimeEpochMs: Long,
    val endTimeEpochMs: Long?,
    val durationSeconds: Long,
    val breakDurationSeconds: Long,
    val notes: String?,
    val mood: Int?,
    val focus: Int?,
    val source: String,
)

fun StudySession.toInfo() = SessionInfo(
    id = id,
    subject = subject,
    chapter = chapter,
    mode = mode,
    startTimeEpochMs = startTime,
    endTimeEpochMs = endTime,
    durationSeconds = durationSeconds,
    breakDurationSeconds = breakDurationSeconds,
    notes = notes,
    mood = moodRating,
    focus = focusScore,
    source = source,
)

data class StreakInfo(
    val current: Int,
    val longest: Int,
    val lastStudyDate: String?,
)
