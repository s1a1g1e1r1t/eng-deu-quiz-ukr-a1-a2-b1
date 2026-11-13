package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.InitialSentences
import com.example.data.InitialWords
import com.example.data.database.AppDatabase
import com.example.data.repository.WordRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

    @Test
    fun `read string from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("Vocab A1", appName)
    }

    @Test
    fun `verify initial words dataset has exactly 535 words per level for EN and DE (A1, A2, B1)`() {
        val words = InitialWords.getInitialWords()
        assertEquals("Must contain exactly 3210 words across EN and DE (A1, A2, B1)", 3210, words.size)
        assertEquals("All word IDs must be unique", 3210, words.map { it.id }.toSet().size)

        listOf("EN", "DE").forEach { lang ->
            val langWords = words.filter { it.language == lang }
            assertEquals("Must contain 1605 words for $lang", 1605, langWords.size)

            val a1Words = langWords.filter { it.level == "A1" }
            val a2Words = langWords.filter { it.level == "A2" }
            val b1Words = langWords.filter { it.level == "B1" }

            assertEquals("Must contain exactly 535 $lang A1 words", 535, a1Words.size)
            assertEquals("Must contain exactly 535 $lang A2 words", 535, a2Words.size)
            assertEquals("Must contain exactly 535 $lang B1 words", 535, b1Words.size)

            if (lang == "DE") {
                assertEquals(
                    "All DE A1 words must be unique",
                    535,
                    a1Words.map { it.english.trim().lowercase() }.toSet().size
                )
            }
            assertEquals(
                "All $lang A2 words must be unique",
                535,
                a2Words.map { it.english.trim().lowercase() }.toSet().size
            )
            assertEquals(
                "All $lang B1 words must be unique",
                535,
                b1Words.map { it.english.trim().lowercase() }.toSet().size
            )
        }

        words.forEach { word ->
            assertTrue("Word english must not be blank", word.english.isNotBlank())
            assertTrue("Word ukrainian must not be blank", word.ukrainian.isNotBlank())
            assertTrue("Word transcription must not be blank", word.transcription.isNotBlank())
            assertTrue("Word category must not be blank", word.category.isNotBlank())
            assertTrue("Word example must not be blank", word.exampleEn.isNotBlank())
            assertTrue("Word language must be EN or DE", word.language in listOf("EN", "DE"))
            assertTrue("Word level must be A1, A2, or B1", word.level in listOf("A1", "A2", "B1"))
        }
    }

    @Test
    fun `verify initial sentences dataset contains exactly 100 sentences per level for EN and DE (A1, A2, B1)`() {
        val sentences = InitialSentences.getInitialSentences()
        assertEquals("Must contain exactly 600 sentences (100 for each level in EN and DE)", 600, sentences.size)
        assertEquals("All sentence IDs must be unique", 600, sentences.map { it.id }.toSet().size)

        listOf("EN", "DE").forEach { lang ->
            val langSentences = sentences.filter { it.language == lang }
            assertEquals("Must contain 300 sentences for $lang", 300, langSentences.size)

            val a1Sentences = langSentences.filter { it.level == "A1" }
            val a2Sentences = langSentences.filter { it.level == "A2" }
            val b1Sentences = langSentences.filter { it.level == "B1" }

            assertEquals("Must contain exactly 100 $lang A1 sentences", 100, a1Sentences.size)
            assertEquals("Must contain exactly 100 $lang A2 sentences", 100, a2Sentences.size)
            assertEquals("Must contain exactly 100 $lang B1 sentences", 100, b1Sentences.size)
        }

        sentences.forEach { sentence ->
            assertTrue("Sentence english must not be blank", sentence.english.isNotBlank())
            assertTrue("Sentence ukrainian must not be blank", sentence.ukrainian.isNotBlank())
            assertTrue("Sentence category must not be blank", sentence.category.isNotBlank())
            assertTrue("Sentence language must be EN or DE", sentence.language in listOf("EN", "DE"))
            assertTrue("Sentence level must be A1, A2, or B1", sentence.level in listOf("A1", "A2", "B1"))
        }
    }

    @Test
    fun `verify word and sentence progress persists and is not overwritten by re-initialization`() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()

        try {
            val repo = WordRepository(
                wordDao = db.wordDao(),
                userProgressDao = db.userProgressDao(),
                sentenceDao = db.sentenceDao()
            )

            // Initialize DB
            repo.ensureInitialized()

            val wordsAfterInit = repo.allWords.first()
            assertEquals(3210, wordsAfterInit.size)

            // Record attempts on EN A2/B1 and DE A1/B1 words
            val a2WordId = 867
            val b1WordId = 1206
            val deA1WordId = 2001
            val deB1WordId = 3071
            repo.recordAttempt(a2WordId, isCorrect = true)
            repo.recordAttempt(b1WordId, isCorrect = false)
            repo.recordAttempt(deA1WordId, isCorrect = true)
            repo.recordAttempt(deB1WordId, isCorrect = false)
            repo.markSentenceCompleted(1)
            repo.markSentenceCompleted(1001)
            repo.incrementQuizzesCompleted()

            val progress = db.userProgressDao().getUserProgressOnce()
            assertNotNull("UserProgress must exist", progress)
            assertEquals("Total XP should equal (10 + 2 + 10 + 2) + 15 + 15 = 54", 54, progress!!.totalXp)
            assertEquals("Quizzes completed should be 1", 1, progress.quizzesCompleted)
            assertTrue("Streak should be >= 1", progress.currentStreak >= 1)

            val updatedA2Word = repo.getWordById(a2WordId)!!
            assertEquals(1, updatedA2Word.correctAttempts)
            assertEquals(1, updatedA2Word.masteryLevel)

            val updatedB1Word = repo.getWordById(b1WordId)!!
            assertEquals(1, updatedB1Word.wrongAttempts)

            val updatedDeA1Word = repo.getWordById(deA1WordId)!!
            assertEquals("DE", updatedDeA1Word.language)
            assertEquals(1, updatedDeA1Word.correctAttempts)
            assertEquals(1, updatedDeA1Word.masteryLevel)

            val updatedDeB1Word = repo.getWordById(deB1WordId)!!
            assertEquals("DE", updatedDeB1Word.language)
            assertEquals(1, updatedDeB1Word.wrongAttempts)

            val sentences = repo.allSentences.first()
            assertEquals(2, sentences.count { it.isCompleted })

            // Re-running ensureInitialized must NOT overwrite progress
            repo.ensureInitialized()

            val persistedA2Word = repo.getWordById(a2WordId)!!
            assertEquals(1, persistedA2Word.correctAttempts)
            assertEquals(1, persistedA2Word.masteryLevel)

            val persistedDeA1Word = repo.getWordById(deA1WordId)!!
            assertEquals(1, persistedDeA1Word.correctAttempts)
            assertEquals(1, persistedDeA1Word.masteryLevel)

            val persistedSentences = repo.allSentences.first()
            assertEquals(2, persistedSentences.count { it.isCompleted })
        } finally {
            db.close()
        }
    }

    @Test
    fun `verify sentence chips tokenization`() {
        val sentence = "I have a cat."
        val words = sentence.trim().split(Regex("\\s+")).filter { it.isNotBlank() }
        assertEquals(4, words.size)
        assertEquals("I", words[0])
        assertEquals("have", words[1])
        assertEquals("a", words[2])
        assertEquals("cat.", words[3])
    }

    @Test
    fun `verify GeminiChatViewModel ignores blank prompts and clearChat resets state`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val ttsHelper = com.example.util.TtsHelper(context)
        try {
            val viewModel = com.example.viewmodel.GeminiChatViewModel(ttsHelper)
            viewModel.sendMessage(
                "   ",
                com.example.viewmodel.LearningLanguage.EN,
                com.example.viewmodel.CefrLevel.A1
            )
            assertTrue(viewModel.uiState.value.messages.isEmpty())
            assertEquals(
                com.example.viewmodel.GeminiRequestState.Idle,
                viewModel.uiState.value.requestState
            )

            viewModel.clearChat()
            assertTrue(viewModel.uiState.value.messages.isEmpty())
            assertEquals(
                com.example.viewmodel.GeminiRequestState.Idle,
                viewModel.uiState.value.requestState
            )
        } finally {
            ttsHelper.shutdown()
        }
    }

    @Test
    fun `verify configureAppCheck persists debug secret synchronously when provided in intent`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        if (com.google.firebase.FirebaseApp.getApps(context).isEmpty()) {
            com.google.firebase.FirebaseApp.initializeApp(context)
        }
        val expectedToken = "test-debug-token-1234"
        val intent = android.content.Intent().apply {
            putExtra("FIREBASE_APPCHECK_DEBUG_TOKEN", expectedToken)
        }
        com.example.util.configureAppCheck(context, intent)
        val persistenceKey = com.google.firebase.FirebaseApp.getInstance().persistenceKey
        val prefs = context.getSharedPreferences(
            "com.google.firebase.appcheck.debug.store.$persistenceKey",
            Context.MODE_PRIVATE
        )
        assertEquals(expectedToken, prefs.getString("com.google.firebase.appcheck.debug.DEBUG_SECRET", null))
    }
}
