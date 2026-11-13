package com.example.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.Word
import kotlinx.coroutines.flow.Flow

@Dao
interface WordDao {
    @Query("SELECT * FROM words ORDER BY CASE language WHEN 'EN' THEN 1 WHEN 'DE' THEN 2 ELSE 3 END, CASE level WHEN 'A1' THEN 1 WHEN 'A2' THEN 2 WHEN 'B1' THEN 3 ELSE 4 END, id ASC")
    fun getAllWords(): Flow<List<Word>>

    @Query("SELECT * FROM words WHERE level = :level ORDER BY id ASC")
    fun getWordsByLevel(level: String): Flow<List<Word>>

    @Query("SELECT * FROM words WHERE category = :category ORDER BY id ASC")
    fun getWordsByCategory(category: String): Flow<List<Word>>

    @Query("SELECT * FROM words WHERE isBookmarked = 1 ORDER BY id ASC")
    fun getBookmarkedWords(): Flow<List<Word>>

    @Query("SELECT * FROM words WHERE wrongAttempts > 0 ORDER BY wrongAttempts DESC")
    fun getMistakeWords(): Flow<List<Word>>

    @Query("SELECT DISTINCT category FROM words ORDER BY category ASC")
    fun getAllCategories(): Flow<List<String>>

    @Query("SELECT * FROM words WHERE id = :id LIMIT 1")
    suspend fun getWordById(id: Int): Word?

    @Query("SELECT COUNT(*) FROM words")
    suspend fun getWordCount(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(words: List<Word>)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertMissing(words: List<Word>)

    @Update
    suspend fun updateWord(word: Word)

    @Query("UPDATE words SET isBookmarked = :isBookmarked WHERE id = :id")
    suspend fun toggleBookmark(id: Int, isBookmarked: Boolean)

    @Query("""
        UPDATE words 
        SET correctAttempts = correctAttempts + :correctInc,
            wrongAttempts = wrongAttempts + :wrongInc,
            masteryLevel = CASE 
                WHEN :correctInc = 1 AND masteryLevel < 3 THEN masteryLevel + 1
                WHEN :wrongInc = 1 AND masteryLevel > 0 THEN masteryLevel - 1
                ELSE masteryLevel
            END,
            lastReviewedTimestamp = :timestamp
        WHERE id = :id
    """)
    suspend fun recordAttempt(id: Int, correctInc: Int, wrongInc: Int, timestamp: Long)

    @Query("UPDATE words SET masteryLevel = 0, correctAttempts = 0, wrongAttempts = 0")
    suspend fun resetAllProgress()
}
