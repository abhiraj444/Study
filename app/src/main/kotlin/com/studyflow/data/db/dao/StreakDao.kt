package com.studyflow.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.studyflow.data.db.entity.Streak

@Dao
interface StreakDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(streak: Streak)

    @Query("SELECT * FROM streaks WHERE id = 1")
    suspend fun get(): Streak?

    @Query("SELECT * FROM streaks WHERE id = 1")
    fun observe(): kotlinx.coroutines.flow.Flow<Streak?>
}
