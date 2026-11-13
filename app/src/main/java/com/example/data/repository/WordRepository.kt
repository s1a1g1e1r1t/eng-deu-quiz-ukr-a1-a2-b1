package com.example.data.repository

import com.example.data.InitialSentences
import com.example.data.InitialWords
import com.example.data.dao.SentenceDao
import com.example.data.dao.UserProgressDao
import com.example.data.dao.WordDao
import com.example.data.model.Sentence
import com.example.data.model.UserProgress
import com.example.data.model.Word
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class WordRepository(
    private val wordDao: WordDao,
    private val userProgressDao: UserProgressDao,
    private val sentenceDao: SentenceDao
) {
    private val initMutex = Mutex()

    val allWords: Flow<List<Word>> = wordDao.getAllWords()
    val bookmarkedWords: Flow<List<Word>> = wordDao.getBookmarkedWords()
    val mistakeWords: Flow<List<Word>> = wordDao.getMistakeWords()
    val allCategories: Flow<List<String>> = wordDao.getAllCategories()
    val userProgress: Flow<UserProgress?> = userProgressDao.getUserProgress()
    val allSentences: Flow<List<Sentence>> = sentenceDao.getAllSentences()

    fun getWordsByLevel(level: String): Flow<List<Word>> = wordDao.getWordsByLevel(level)
    fun getSentencesByLevel(level: String): Flow<List<Sentence>> = sentenceDao.getSentencesByLevel(level)

    suspend fun ensureInitialized() = withContext(Dispatchers.IO) {
        initMutex.withLock {
            try {
                ensureUserProgressExists()

                val initialWords = InitialWords.getInitialWords()
                val count = wordDao.getWordCount()
                if (count < initialWords.size) {
                    wordDao.insertMissing(initialWords)
                }

                val initialSentences = InitialSentences.getInitialSentences()
                val sentenceCount = sentenceDao.getCount()
                if (sentenceCount < initialSentences.size) {
                    sentenceDao.insertMissing(initialSentences)
                }

                checkStreak()
            } catch (e: Exception) {
                android.util.Log.e("WordRepository", "Error initializing data", e)
            }
        }
    }

    private suspend fun ensureUserProgressExists() {
        if (userProgressDao.getUserProgressOnce() == null) {
            val today = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
            userProgressDao.insertIfMissing(
                UserProgress(
                    id = 1,
                    totalXp = 0,
                    quizzesCompleted = 0,
                    currentStreak = 1,
                    bestStreak = 1,
                    lastActiveDate = today
                )
            )
        }
    }

    suspend fun getWordById(id: Int): Word? = withContext(Dispatchers.IO) {
        wordDao.getWordById(id)
    }

    suspend fun toggleBookmark(wordId: Int, isBookmarked: Boolean) = withContext(Dispatchers.IO) {
        wordDao.toggleBookmark(wordId, isBookmarked)
    }

    suspend fun recordAttempt(wordId: Int, isCorrect: Boolean) = withContext(Dispatchers.IO) {
        ensureUserProgressExists()
        val timestamp = System.currentTimeMillis()
        if (isCorrect) {
            wordDao.recordAttempt(wordId, correctInc = 1, wrongInc = 0, timestamp = timestamp)
            userProgressDao.addXp(xp = 10)
        } else {
            wordDao.recordAttempt(wordId, correctInc = 0, wrongInc = 1, timestamp = timestamp)
            userProgressDao.addXp(xp = 2)
        }
        checkStreak()
    }

    suspend fun incrementQuizzesCompleted() = withContext(Dispatchers.IO) {
        ensureUserProgressExists()
        userProgressDao.incrementQuizzesCompleted()
    }

    suspend fun markSentenceCompleted(sentenceId: Int) = withContext(Dispatchers.IO) {
        ensureUserProgressExists()
        sentenceDao.markCompleted(sentenceId)
        userProgressDao.addXp(xp = 15)
        checkStreak()
    }

    suspend fun resetAllProgress() = withContext(Dispatchers.IO) {
        wordDao.resetAllProgress()
        sentenceDao.resetAll()
        userProgressDao.insertOrUpdate(
            UserProgress(
                id = 1,
                totalXp = 0,
                quizzesCompleted = 0,
                currentStreak = 1,
                bestStreak = 1,
                lastActiveDate = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
            )
        )
    }

    private suspend fun checkStreak() {
        val progress = userProgressDao.getUserProgressOnce() ?: return
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val today = sdf.format(Date())
        if (progress.lastActiveDate == today) return

        val cal = Calendar.getInstance()
        cal.add(Calendar.DAY_OF_YEAR, -1)
        val yesterday = sdf.format(cal.time)

        val newStreak = when (progress.lastActiveDate) {
            "" -> progress.currentStreak.coerceAtLeast(1)
            yesterday -> progress.currentStreak + 1
            else -> 1
        }
        userProgressDao.updateStreak(newStreak, today)
    }
}
