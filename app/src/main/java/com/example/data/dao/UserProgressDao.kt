package com.example.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.model.UserProgress
import kotlinx.coroutines.flow.Flow

@Dao
interface UserProgressDao {
    @Query("SELECT * FROM user_progress WHERE id = 1 LIMIT 1")
    fun getUserProgress(): Flow<UserProgress?>

    @Query("SELECT * FROM user_progress WHERE id = 1 LIMIT 1")
    suspend fun getUserProgressOnce(): UserProgress?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(progress: UserProgress)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertIfMissing(progress: UserProgress)

    @Query("UPDATE user_progress SET totalXp = totalXp + :xp WHERE id = 1")
    suspend fun addXp(xp: Int)

    @Query("UPDATE user_progress SET quizzesCompleted = quizzesCompleted + 1 WHERE id = 1")
    suspend fun incrementQuizzesCompleted()

    @Query("UPDATE user_progress SET totalXp = totalXp + :xp, quizzesCompleted = quizzesCompleted + 1 WHERE id = 1")
    suspend fun addQuizResult(xp: Int)

    @Query("UPDATE user_progress SET currentStreak = :streak, bestStreak = CASE WHEN :streak > bestStreak THEN :streak ELSE bestStreak END, lastActiveDate = :today WHERE id = 1")
    suspend fun updateStreak(streak: Int, today: String)
}
