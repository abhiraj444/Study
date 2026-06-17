package com.studyflow.data.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "subjects")
data class Subject(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    @ColumnInfo(name = "name") val name: String,
    @ColumnInfo(name = "color_hex") val colorHex: String = "#7C6AF7",
    @ColumnInfo(name = "icon_name") val iconName: String = "book",
    @ColumnInfo(name = "daily_goal_minutes") val dailyGoalMinutes: Int = 60,
    @ColumnInfo(name = "is_active") val isActive: Int = 1,
    @ColumnInfo(name = "sort_order") val sortOrder: Int = 0,
)
