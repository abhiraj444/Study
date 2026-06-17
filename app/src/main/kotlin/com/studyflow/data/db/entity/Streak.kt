package com.studyflow.data.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/** A single-row table holding streak stats. Id is always 1. */
@Entity(tableName = "streaks")
data class Streak(
    @PrimaryKey val id: Int = 1,
    @ColumnInfo(name = "current_streak") val currentStreak: Int = 0,
    @ColumnInfo(name = "longest_streak") val longestStreak: Int = 0,
    @ColumnInfo(name = "last_study_date") val lastStudyDate: String? = null,
)
