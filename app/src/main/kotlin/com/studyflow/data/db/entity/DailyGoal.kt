package com.studyflow.data.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/** One row per calendar day (YYYY-MM-DD). */
@Entity(tableName = "daily_goals")
data class DailyGoal(
    @PrimaryKey @ColumnInfo(name = "date") val date: String,
    @ColumnInfo(name = "target_minutes") val targetMinutes: Int,
    @ColumnInfo(name = "achieved_minutes") val achievedMinutes: Int = 0,
    @ColumnInfo(name = "goal_met") val goalMet: Int = 0,
)
