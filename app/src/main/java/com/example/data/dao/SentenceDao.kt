package com.example.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.model.Sentence
import kotlinx.coroutines.flow.Flow

@Dao
interface SentenceDao {
    @Query("SELECT * FROM sentences ORDER BY id ASC")
    fun getAllSentences(): Flow<List<Sentence>>

    @Query("SELECT * FROM sentences WHERE level = :level ORDER BY id ASC")
    fun getSentencesByLevel(level: String): Flow<List<Sentence>>

    @Query("SELECT COUNT(*) FROM sentences")
    suspend fun getCount(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(sentences: List<Sentence>)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertMissing(sentences: List<Sentence>)

    @Query("UPDATE sentences SET isCompleted = 1, attempts = attempts + 1 WHERE id = :id")
    suspend fun markCompleted(id: Int)

    @Query("UPDATE sentences SET isCompleted = 0, attempts = 0")
    suspend fun resetAll()
}
