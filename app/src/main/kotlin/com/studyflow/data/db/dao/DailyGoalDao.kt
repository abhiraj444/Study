package com.studyflow.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.studyflow.data.db.entity.DailyGoal
import kotlinx.coroutines.flow.Flow

@Dao
interface DailyGoalDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(goal: DailyGoal)

    @Query("SELECT * FROM daily_goals WHERE date = :dateIso LIMIT 1")
    suspend fun getByDate(dateIso: String): DailyGoal?

    @Query("SELECT * FROM daily_goals WHERE date BETWEEN :fromIso AND :toIso ORDER BY date ASC")
    fun observeRange(fromIso: String, toIso: String): Flow<List<DailyGoal>>

    @Query("""
        UPDATE daily_goals
        SET achieved_minutes = achieved_minutes + :delta,
            goal_met = CASE WHEN achieved_minutes + :delta >= target_minutes THEN 1 ELSE 0 END
        WHERE date = :dateIso
    """)
    suspend fun addAchieved(dateIso: String, delta: Int)

    @Query("""
        UPDATE daily_goals
        SET achieved_minutes = :achieved,
            goal_met = CASE WHEN :achieved >= target_minutes THEN 1 ELSE 0 END
        WHERE date = :dateIso
    """)
    suspend fun setAchieved(dateIso: String, achieved: Int)

    @Query("DELETE FROM daily_goals")
    suspend fun clearAll()
}
