package com.studyflow.data.db.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.studyflow.data.db.entity.Subject
import kotlinx.coroutines.flow.Flow

@Dao
interface SubjectDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(subject: Subject): Long

    @Update
    suspend fun update(subject: Subject)

    @Delete
    suspend fun delete(subject: Subject)

    @Query("SELECT * FROM subjects WHERE is_active = 1 ORDER BY sort_order ASC, name ASC")
    fun observeActive(): Flow<List<Subject>>

    @Query("SELECT * FROM subjects ORDER BY sort_order ASC, name ASC")
    fun observeAll(): Flow<List<Subject>>

    @Query("SELECT * FROM subjects WHERE is_active = 1 ORDER BY sort_order ASC, name ASC")
    suspend fun getActive(): List<Subject>

    @Query("SELECT * FROM subjects WHERE id = :id")
    suspend fun getById(id: Long): Subject?

    @Query("SELECT * FROM subjects WHERE name = :name LIMIT 1")
    suspend fun getByName(name: String): Subject?

    @Query("UPDATE subjects SET is_active = :active WHERE id = :id")
    suspend fun setActive(id: Long, active: Int)

    @Query("UPDATE subjects SET sort_order = :order WHERE id = :id")
    suspend fun setSortOrder(id: Long, order: Int)

    @Query("DELETE FROM subjects")
    suspend fun clearAll()
}
