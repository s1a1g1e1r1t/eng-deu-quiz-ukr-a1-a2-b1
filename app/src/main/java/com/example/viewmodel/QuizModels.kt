package com.example.viewmodel

import com.example.data.model.Sentence
import com.example.data.model.Word

enum class LearningLanguage(
    val code: String,
    val titleUa: String,
    val titleGenitiveUa: String,
    val adjectiveUa: String,
    val adjSingleUa: String,
    val adjPluralUa: String,
    val flag: String
) {
    EN(
        code = "EN",
        titleUa = "Англійська",
        titleGenitiveUa = "англійської",
        adjectiveUa = "англійською",
        adjSingleUa = "англійське",
        adjPluralUa = "англійських",
        flag = "🇬🇧"
    ),
    DE(
        code = "DE",
        titleUa = "Німецька",
        titleGenitiveUa = "німецької",
        adjectiveUa = "німецькою",
        adjSingleUa = "німецьке",
        adjPluralUa = "німецьких",
        flag = "🇩🇪"
    )
}

enum class QuizMode(
    val titleUa: String,
    val subtitleUa: String,
    val iconName: String
) {
    MULTIPLE_CHOICE(
        titleUa = "Вікторина 100 слів (5 кнопок)",
        subtitleUa = "100 слів, 5 варіантів відповіді для глибокого тестування",
        iconName = "quiz"
    ),
    FLASHCARDS(
        titleUa = "Флеш-картки",
        subtitleUa = "Перевертай картки, тренуй пам'ять та вимову",
        iconName = "cards"
    ),
    WORD_BUILDER(
        titleUa = "Конструктор слова",
        subtitleUa = "Склади іноземне слово з перемішаних літер",
        iconName = "spell"
    ),
    LISTENING(
        titleUa = "Аудіо-вікторина 100 слів",
        subtitleUa = "Слухай на слух вимову та обирай з 5 варіантів",
        iconName = "audio"
    ),
    SPEED_MATCH(
        titleUa = "Бліц-спринт (Правда/Ні)",
        subtitleUa = "Швидко визначай: переклад вірний чи ні за 30 сек",
        iconName = "timer"
    )
}

enum class CefrLevel(
    val code: String,
    val titleUa: String,
    val badge: String,
    val descriptionUa: String
) {
    A1(
        code = "A1",
        titleUa = "Рівень A1",
        badge = "Beginner",
        descriptionUa = "Початковий • Базові слова та фрази"
    ),
    A2(
        code = "A2",
        titleUa = "Рівень A2",
        badge = "Elementary",
        descriptionUa = "Елементарний • Повсякденне спілкування"
    ),
    B1(
        code = "B1",
        titleUa = "Рівень B1",
        badge = "Intermediate",
        descriptionUa = "Середній • Вільне вираження думок"
    )
}

sealed class Screen {
    object Home : Screen()
    object ModeSelection : Screen()
    data class Quiz(val mode: QuizMode, val category: String? = null) : Screen()
    object SentenceBuilder : Screen()
    object Dictionary : Screen()
    object Stats : Screen()
    object GeminiChat : Screen()
}

data class QuizQuestion(
    val targetWord: Word,
    val options: List<String>, // 5 options
    val correctAnswer: String,
    val promptType: PromptType // EN_TO_UA, UA_TO_EN, AUDIO
)

enum class PromptType {
    EN_TO_UA,
    UA_TO_EN,
    AUDIO
}

data class QuizSessionState(
    val mode: QuizMode,
    val language: LearningLanguage = LearningLanguage.EN,
    val level: CefrLevel = CefrLevel.A1,
    val questions: List<QuizQuestion> = emptyList(),
    val currentIndex: Int = 0,
    val selectedAnswer: String? = null,
    val isAnswered: Boolean = false,
    val isCorrect: Boolean = false,
    val correctCount: Int = 0,
    val isFinished: Boolean = false,
    val sessionXpEarned: Int = 0
)

data class FlashcardSessionState(
    val language: LearningLanguage = LearningLanguage.EN,
    val level: CefrLevel = CefrLevel.A1,
    val words: List<Word> = emptyList(),
    val currentIndex: Int = 0,
    val isFlipped: Boolean = false,
    val knownCount: Int = 0,
    val reviewCount: Int = 0,
    val isFinished: Boolean = false
)

data class LetterChip(val id: Int, val char: Char, val isUsed: Boolean = false)

data class WordBuilderState(
    val language: LearningLanguage = LearningLanguage.EN,
    val level: CefrLevel = CefrLevel.A1,
    val targetWord: Word? = null,
    val remainingWords: List<Word> = emptyList(),
    val availableLetters: List<LetterChip> = emptyList(),
    val enteredLetters: List<LetterChip> = emptyList(),
    val isChecked: Boolean = false,
    val isCorrect: Boolean = false,
    val score: Int = 0,
    val totalWordsCount: Int = 0,
    val currentWordIndex: Int = 0,
    val isFinished: Boolean = false
)

data class SpeedMatchState(
    val language: LearningLanguage = LearningLanguage.EN,
    val level: CefrLevel = CefrLevel.A1,
    val targetWord: Word? = null,
    val displayedTranslation: String = "",
    val isMatchTrue: Boolean = true,
    val remainingSeconds: Int = 30,
    val score: Int = 0,
    val correctCount: Int = 0,
    val wrongCount: Int = 0,
    val combo: Int = 0,
    val isFinished: Boolean = false,
    val isRunning: Boolean = false
)

// Sentence Construction (100 sentences per level) State
data class SentenceChip(
    val id: Int,
    val word: String,
    val isUsed: Boolean = false
)

data class SentenceBuilderSessionState(
    val language: LearningLanguage = LearningLanguage.EN,
    val level: CefrLevel = CefrLevel.A1,
    val sentences: List<Sentence> = emptyList(),
    val currentIndex: Int = 0,
    val targetSentence: Sentence? = null,
    val availableChips: List<SentenceChip> = emptyList(),
    val enteredChips: List<SentenceChip> = emptyList(),
    val isChecked: Boolean = false,
    val isCorrect: Boolean = false,
    val score: Int = 0,
    val completedCount: Int = 0,
    val isFinished: Boolean = false
)

enum class ChatAuthor {
    USER,
    GEMINI
}

data class GeminiChatMessage(
    val id: String,
    val author: ChatAuthor,
    val text: String,
    val isStreaming: Boolean = false
)

sealed interface GeminiRequestState {
    data object Idle : GeminiRequestState
    data object Loading : GeminiRequestState
    data object Streaming : GeminiRequestState
    data object Success : GeminiRequestState
    data class Error(val message: String) : GeminiRequestState
}

data class GeminiChatUiState(
    val messages: List<GeminiChatMessage> = emptyList(),
    val requestState: GeminiRequestState = GeminiRequestState.Idle,
    val lastFailedPrompt: String? = null
)
