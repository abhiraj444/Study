package com.studyflow.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import com.studyflow.data.db.dao.DailyGoalDao
import com.studyflow.data.db.dao.StreakDao
import com.studyflow.data.db.dao.StudySessionDao
import com.studyflow.data.db.dao.SubjectDao
import com.studyflow.data.db.entity.DailyGoal
import com.studyflow.data.db.entity.Streak
import com.studyflow.data.db.entity.StudySession
import com.studyflow.data.db.entity.Subject

@Database(
    entities = [StudySession::class, Subject::class, DailyGoal::class, Streak::class],
    version = 1,
    exportSchema = false,
)
abstract class StudyFlowDatabase : RoomDatabase() {
    abstract fun sessionDao(): StudySessionDao
    abstract fun subjectDao(): SubjectDao
    abstract fun dailyGoalDao(): DailyGoalDao
    abstract fun streakDao(): StreakDao

    companion object {
        const val DB_NAME = "studyflow.db"
    }
}
