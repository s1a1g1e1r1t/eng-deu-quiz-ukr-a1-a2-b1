package com.example.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.InitialSentences
import com.example.data.InitialWords
import com.example.data.model.Sentence
import com.example.data.model.UserProgress
import com.example.data.model.Word
import com.example.data.repository.WordRepository
import com.example.util.TtsHelper
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlin.random.Random

class MainViewModel(
    private val repository: WordRepository,
    private val ttsHelper: TtsHelper
) : ViewModel() {

    val allWords: StateFlow<List<Word>> = repository.allWords
        .stateIn(viewModelScope, SharingStarted.Eagerly, InitialWords.getInitialWords())

    val bookmarkedWords: StateFlow<List<Word>> = repository.bookmarkedWords
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val mistakeWords: StateFlow<List<Word>> = repository.mistakeWords
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val categories: StateFlow<List<String>> = repository.allCategories
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val userProgress: StateFlow<UserProgress?> = repository.userProgress
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val allSentences: StateFlow<List<Sentence>> = repository.allSentences
        .stateIn(viewModelScope, SharingStarted.Eagerly, InitialSentences.getInitialSentences())

    // Navigation state
    private val _currentScreen = MutableStateFlow<Screen>(Screen.Home)
    val currentScreen: StateFlow<Screen> = _currentScreen.asStateFlow()

    private val screenStack = mutableListOf<Screen>(Screen.Home)

    // Active Learning Language (EN, DE)
    private val _selectedLanguage = MutableStateFlow<LearningLanguage>(LearningLanguage.EN)
    val selectedLanguage: StateFlow<LearningLanguage> = _selectedLanguage.asStateFlow()

    fun selectLanguage(language: LearningLanguage) {
        _selectedLanguage.value = language
    }

    // Active CEFR Level (A1, A2, B1)
    private val _selectedLevel = MutableStateFlow<CefrLevel>(CefrLevel.A1)
    val selectedLevel: StateFlow<CefrLevel> = _selectedLevel.asStateFlow()

    fun selectLevel(level: CefrLevel) {
        _selectedLevel.value = level
    }

    // Quiz Session State (Multiple Choice 100 words / 5 buttons & Listening)
    private val _quizState = MutableStateFlow<QuizSessionState?>(null)
    val quizState: StateFlow<QuizSessionState?> = _quizState.asStateFlow()

    // Flashcard Session State
    private val _flashcardState = MutableStateFlow<FlashcardSessionState?>(null)
    val flashcardState: StateFlow<FlashcardSessionState?> = _flashcardState.asStateFlow()

    // Word Builder State
    private val _wordBuilderState = MutableStateFlow<WordBuilderState?>(null)
    val wordBuilderState: StateFlow<WordBuilderState?> = _wordBuilderState.asStateFlow()

    // Speed Match State
    private val _speedMatchState = MutableStateFlow<SpeedMatchState?>(null)
    val speedMatchState: StateFlow<SpeedMatchState?> = _speedMatchState.asStateFlow()

    // Sentence Builder State (100 sentences per level)
    private val _sentenceBuilderState = MutableStateFlow<SentenceBuilderSessionState?>(null)
    val sentenceBuilderState: StateFlow<SentenceBuilderSessionState?> = _sentenceBuilderState.asStateFlow()

    private var timerJob: Job? = null
    private var quizAutoAdvanceJob: Job? = null
    private var sentenceAutoAdvanceJob: Job? = null
    private var wordBuilderAutoAdvanceJob: Job? = null

    init {
        viewModelScope.launch {
            repository.ensureInitialized()
        }
    }

    fun navigateTo(screen: Screen) {
        if (screen is Screen.SentenceBuilder) {
            startSentenceBuilder(0, _selectedLevel.value, _selectedLanguage.value)
            return
        }
        screenStack.add(screen)
        _currentScreen.value = screen
    }

    fun navigateBack(): Boolean {
        quizAutoAdvanceJob?.cancel()
        sentenceAutoAdvanceJob?.cancel()
        wordBuilderAutoAdvanceJob?.cancel()
        if (screenStack.size > 1) {
            screenStack.removeAt(screenStack.size - 1)
            val previous = screenStack.last()
            _currentScreen.value = previous
            if (previous == Screen.Home) {
                timerJob?.cancel()
            }
            return true
        }
        return false
    }

    fun speak(text: String, language: LearningLanguage = _selectedLanguage.value) {
        ttsHelper.speak(text, language.code)
    }

    fun toggleBookmark(wordId: Int, isBookmarked: Boolean) {
        viewModelScope.launch {
            repository.toggleBookmark(wordId, !isBookmarked)
        }
    }

    fun resetProgress() {
        viewModelScope.launch {
            repository.resetAllProgress()
        }
    }

    // --- Start Quizzes ---
    fun startQuizSession(
        mode: QuizMode,
        category: String? = null,
        onlyMistakes: Boolean = false,
        onlyBookmarks: Boolean = false,
        level: CefrLevel? = null,
        language: LearningLanguage? = null
    ) {
        val targetLang = language ?: _selectedLanguage.value
        val targetLevel = level ?: _selectedLevel.value
        val allWordList = if (allWords.value.isNotEmpty()) allWords.value else InitialWords.getInitialWords()
        val langPool = allWordList.filter { it.language.equals(targetLang.code, ignoreCase = true) }.ifEmpty { allWordList }
        val levelPool = langPool.filter { it.level.equals(targetLevel.code, ignoreCase = true) }.ifEmpty { langPool }
        val wordPool = when {
            onlyMistakes -> mistakeWords.value
                .filter { it.language.equals(targetLang.code, ignoreCase = true) && it.level.equals(targetLevel.code, ignoreCase = true) }
                .ifEmpty { mistakeWords.value.filter { it.language.equals(targetLang.code, ignoreCase = true) } }
                .ifEmpty { levelPool }
            onlyBookmarks -> bookmarkedWords.value
                .filter { it.language.equals(targetLang.code, ignoreCase = true) && it.level.equals(targetLevel.code, ignoreCase = true) }
                .ifEmpty { bookmarkedWords.value.filter { it.language.equals(targetLang.code, ignoreCase = true) } }
                .ifEmpty { levelPool }
            category != null -> levelPool.filter { it.category == category }.ifEmpty { levelPool }
            else -> levelPool
        }.shuffled()

        when (mode) {
            QuizMode.MULTIPLE_CHOICE, QuizMode.LISTENING -> {
                // Generate 100 questions (or as many as available in the level pool, up to 100)
                val questions = generateQuestions(wordPool, levelPool, mode)
                _quizState.value = QuizSessionState(
                    mode = mode,
                    language = targetLang,
                    level = targetLevel,
                    questions = questions,
                    currentIndex = 0,
                    correctCount = 0,
                    isFinished = false
                )
                navigateTo(Screen.Quiz(mode, category))
                if (mode == QuizMode.LISTENING && questions.isNotEmpty()) {
                    speak(questions.first().targetWord.english, targetLang)
                }
            }
            QuizMode.FLASHCARDS -> {
                _flashcardState.value = FlashcardSessionState(
                    language = targetLang,
                    level = targetLevel,
                    words = wordPool.take(30),
                    currentIndex = 0,
                    isFlipped = false,
                    knownCount = 0,
                    reviewCount = 0,
                    isFinished = false
                )
                navigateTo(Screen.Quiz(mode, category))
                wordPool.firstOrNull()?.let { speak(it.english, targetLang) }
            }
            QuizMode.WORD_BUILDER -> {
                val targetList = wordPool.take(20)
                val firstWord = targetList.first()
                _wordBuilderState.value = createWordBuilderState(
                    language = targetLang,
                    level = targetLevel,
                    target = firstWord,
                    remaining = targetList.drop(1),
                    currentIndex = 0,
                    total = targetList.size,
                    currentScore = 0
                )
                navigateTo(Screen.Quiz(mode, category))
                speak(firstWord.english, targetLang)
            }
            QuizMode.SPEED_MATCH -> {
                _speedMatchState.value = createSpeedMatchInitial(wordPool, targetLang, targetLevel)
                navigateTo(Screen.Quiz(mode, category))
                startSpeedTimer()
            }
        }
    }

    // --- Multiple Choice with 100 words and 5 options (5 buttons) ---
    private fun generateQuestions(pool: List<Word>, distractorPool: List<Word>, mode: QuizMode): List<QuizQuestion> {
        val allAvailable = if (distractorPool.size >= 5) distractorPool else allWords.value
        // Take 100 questions from pool
        val questionsCount = minOf(100, pool.size)
        val selectedWords = pool.take(questionsCount)

        return selectedWords.map { target ->
            val promptType = when (mode) {
                QuizMode.LISTENING -> PromptType.AUDIO
                else -> if (Random.nextBoolean()) PromptType.EN_TO_UA else PromptType.UA_TO_EN
            }

            val correctAnswer = when (promptType) {
                PromptType.EN_TO_UA, PromptType.AUDIO -> target.ukrainian
                PromptType.UA_TO_EN -> target.english
            }

            // 4 distractors + 1 correct = 5 buttons (options)
            val distractors = allAvailable
                .filter { it.id != target.id }
                .shuffled()
                .take(4)
                .map { distractor ->
                    when (promptType) {
                        PromptType.EN_TO_UA, PromptType.AUDIO -> distractor.ukrainian
                        PromptType.UA_TO_EN -> distractor.english
                    }
                }

            val options = (distractors + correctAnswer).shuffled()
            QuizQuestion(
                targetWord = target,
                options = options,
                correctAnswer = correctAnswer,
                promptType = promptType
            )
        }
    }

    fun submitQuizAnswer(answer: String) {
        val current = _quizState.value ?: return
        if (current.isAnswered || current.isFinished) return

        val question = current.questions.getOrNull(current.currentIndex) ?: return
        val isCorrect = answer.trim().equals(question.correctAnswer.trim(), ignoreCase = true)

        viewModelScope.launch {
            repository.recordAttempt(question.targetWord.id, isCorrect)
        }

        speak(question.targetWord.english, current.language)

        _quizState.value = current.copy(
            selectedAnswer = answer,
            isAnswered = true,
            isCorrect = isCorrect,
            correctCount = if (isCorrect) current.correctCount + 1 else current.correctCount,
            sessionXpEarned = current.sessionXpEarned + (if (isCorrect) 10 else 2)
        )

        // Automatically transition to the next word after answering (3 seconds delay to review correct answer)
        quizAutoAdvanceJob?.cancel()
        quizAutoAdvanceJob = viewModelScope.launch {
            delay(3000L)
            nextQuizQuestion()
        }
    }

    fun nextQuizQuestion() {
        quizAutoAdvanceJob?.cancel()
        val current = _quizState.value ?: return
        val nextIndex = current.currentIndex + 1
        if (nextIndex >= current.questions.size) {
            _quizState.value = current.copy(isFinished = true)
            viewModelScope.launch { repository.incrementQuizzesCompleted() }
        } else {
            _quizState.value = current.copy(
                currentIndex = nextIndex,
                selectedAnswer = null,
                isAnswered = false,
                isCorrect = false
            )
            val nextQ = current.questions.getOrNull(nextIndex)
            if (current.mode == QuizMode.LISTENING && nextQ != null) {
                speak(nextQ.targetWord.english, current.language)
            }
        }
    }

    // --- Flashcards Logic ---
    fun flipFlashcard() {
        val current = _flashcardState.value ?: return
        _flashcardState.value = current.copy(isFlipped = !current.isFlipped)
    }

    fun answerFlashcard(known: Boolean) {
        val current = _flashcardState.value ?: return
        val word = current.words.getOrNull(current.currentIndex)

        if (word != null) {
            viewModelScope.launch {
                repository.recordAttempt(word.id, known)
            }
        }

        val nextIndex = current.currentIndex + 1
        if (nextIndex >= current.words.size) {
            _flashcardState.value = current.copy(
                isFinished = true,
                knownCount = if (known) current.knownCount + 1 else current.knownCount,
                reviewCount = if (!known) current.reviewCount + 1 else current.reviewCount
            )
            viewModelScope.launch { repository.incrementQuizzesCompleted() }
        } else {
            _flashcardState.value = current.copy(
                currentIndex = nextIndex,
                isFlipped = false,
                knownCount = if (known) current.knownCount + 1 else current.knownCount,
                reviewCount = if (!known) current.reviewCount + 1 else current.reviewCount
            )
            current.words.getOrNull(nextIndex)?.let { speak(it.english, current.language) }
        }
    }

    // --- Word Builder Logic ---
    private fun createWordBuilderState(
        language: LearningLanguage,
        level: CefrLevel,
        target: Word,
        remaining: List<Word>,
        currentIndex: Int,
        total: Int,
        currentScore: Int
    ): WordBuilderState {
        val cleanWord = target.english.trim().uppercase()
        val letters = cleanWord.mapIndexed { index, c ->
            LetterChip(id = index, char = c, isUsed = false)
        }.shuffled()

        return WordBuilderState(
            language = language,
            level = level,
            targetWord = target,
            remainingWords = remaining,
            availableLetters = letters,
            enteredLetters = emptyList(),
            isChecked = false,
            isCorrect = false,
            score = currentScore,
            totalWordsCount = total,
            currentWordIndex = currentIndex,
            isFinished = false
        )
    }

    fun onLetterChipClicked(chip: LetterChip) {
        val current = _wordBuilderState.value ?: return
        if (current.isChecked || chip.isUsed) return

        val updatedAvailable = current.availableLetters.map {
            if (it.id == chip.id) it.copy(isUsed = true) else it
        }
        val updatedEntered = current.enteredLetters + chip

        _wordBuilderState.value = current.copy(
            availableLetters = updatedAvailable,
            enteredLetters = updatedEntered
        )
    }

    fun removeEnteredLetter(chip: LetterChip) {
        val current = _wordBuilderState.value ?: return
        if (current.isChecked) return

        val updatedEntered = current.enteredLetters.filterNot { it.id == chip.id }
        val updatedAvailable = current.availableLetters.map {
            if (it.id == chip.id) it.copy(isUsed = false) else it
        }

        _wordBuilderState.value = current.copy(
            availableLetters = updatedAvailable,
            enteredLetters = updatedEntered
        )
    }

    fun checkWordBuilder() {
        val current = _wordBuilderState.value ?: return
        val target = current.targetWord ?: return
        val assembled = current.enteredLetters.map { it.char }.joinToString("")
        val isCorrect = assembled.equals(target.english.trim().uppercase(), ignoreCase = true)

        viewModelScope.launch {
            repository.recordAttempt(target.id, isCorrect)
        }

        speak(target.english, current.language)

        _wordBuilderState.value = current.copy(
            isChecked = true,
            isCorrect = isCorrect,
            score = if (isCorrect) current.score + 10 else current.score
        )

        wordBuilderAutoAdvanceJob?.cancel()
        wordBuilderAutoAdvanceJob = viewModelScope.launch {
            delay(3000L)
            nextWordBuilder()
        }
    }

    fun nextWordBuilder() {
        wordBuilderAutoAdvanceJob?.cancel()
        val current = _wordBuilderState.value ?: return
        if (current.remainingWords.isEmpty()) {
            _wordBuilderState.value = current.copy(isFinished = true)
            viewModelScope.launch { repository.incrementQuizzesCompleted() }
        } else {
            val nextWord = current.remainingWords.first()
            _wordBuilderState.value = createWordBuilderState(
                language = current.language,
                level = current.level,
                target = nextWord,
                remaining = current.remainingWords.drop(1),
                currentIndex = current.currentWordIndex + 1,
                total = current.totalWordsCount,
                currentScore = current.score
            )
            speak(nextWord.english, current.language)
        }
    }

    // --- Speed Match (True/False Sprint) ---
    private fun createSpeedMatchInitial(
        pool: List<Word>,
        language: LearningLanguage,
        level: CefrLevel
    ): SpeedMatchState {
        val target = pool.random()
        val isTrueMatch = Random.nextBoolean()
        val translation = if (isTrueMatch) {
            target.ukrainian
        } else {
            pool.filter { it.id != target.id }.randomOrNull()?.ukrainian ?: target.ukrainian
        }
        return SpeedMatchState(
            language = language,
            level = level,
            targetWord = target,
            displayedTranslation = translation,
            isMatchTrue = (translation == target.ukrainian),
            remainingSeconds = 30,
            score = 0,
            correctCount = 0,
            wrongCount = 0,
            combo = 0,
            isFinished = false,
            isRunning = true
        )
    }

    private fun startSpeedTimer() {
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            while (true) {
                delay(1000)
                val state = _speedMatchState.value
                if (state == null || !state.isRunning) break

                val newSeconds = state.remainingSeconds - 1
                if (newSeconds <= 0) {
                    _speedMatchState.value = state.copy(
                        remainingSeconds = 0,
                        isRunning = false,
                        isFinished = true
                    )
                    viewModelScope.launch { repository.incrementQuizzesCompleted() }
                    break
                } else {
                    _speedMatchState.value = state.copy(remainingSeconds = newSeconds)
                }
            }
        }
    }

    fun answerSpeedMatch(userSaysTrue: Boolean) {
        val current = _speedMatchState.value ?: return
        if (!current.isRunning || current.isFinished) return

        val isCorrect = (userSaysTrue == current.isMatchTrue)
        val target = current.targetWord

        if (target != null) {
            viewModelScope.launch {
                repository.recordAttempt(target.id, isCorrect)
            }
        }

        val allPool = allWords.value
        val langCode = current.language.code
        val levelCode = current.level.code
        val langPool = allPool.filter { it.language.equals(langCode, ignoreCase = true) }.ifEmpty { allPool }
        val pool = langPool.filter { it.level.equals(levelCode, ignoreCase = true) }.ifEmpty { langPool }
        val nextTarget = pool.random()
        val nextIsTrue = Random.nextBoolean()
        val nextTranslation = if (nextIsTrue) {
            nextTarget.ukrainian
        } else {
            pool.filter { it.id != nextTarget.id }.randomOrNull()?.ukrainian ?: nextTarget.ukrainian
        }

        val newCombo = if (isCorrect) current.combo + 1 else 0
        val points = if (isCorrect) 10 + (newCombo * 2) else 0

        _speedMatchState.value = current.copy(
            targetWord = nextTarget,
            displayedTranslation = nextTranslation,
            isMatchTrue = (nextTranslation == nextTarget.ukrainian),
            score = current.score + points,
            correctCount = if (isCorrect) current.correctCount + 1 else current.correctCount,
            wrongCount = if (!isCorrect) current.wrongCount + 1 else current.wrongCount,
            combo = newCombo
        )
    }

    // --- SENTENCE BUILDER (100 SENTENCES PER LEVEL & LANGUAGE) ---
    fun startSentenceBuilder(
        startIndex: Int = 0,
        level: CefrLevel? = null,
        language: LearningLanguage? = null
    ) {
        val targetLang = language ?: _selectedLanguage.value
        val targetLevel = level ?: _selectedLevel.value
        val allSentenceList = if (allSentences.value.isNotEmpty()) allSentences.value else InitialSentences.getInitialSentences()
        val langSentences = allSentenceList.filter { it.language.equals(targetLang.code, ignoreCase = true) }
            .ifEmpty { allSentenceList }
        val sentences = langSentences.filter { it.level.equals(targetLevel.code, ignoreCase = true) }
            .ifEmpty { langSentences }
        val safeIndex = startIndex.coerceIn(0, (sentences.size - 1).coerceAtLeast(0))
        val target = sentences.getOrNull(safeIndex) ?: sentences.first()
        val chips = parseSentenceChips(target.english)

        val completed = sentences.count { it.isCompleted }

        _sentenceBuilderState.value = SentenceBuilderSessionState(
            language = targetLang,
            level = targetLevel,
            sentences = sentences,
            currentIndex = safeIndex,
            targetSentence = target,
            availableChips = chips,
            enteredChips = emptyList(),
            isChecked = false,
            isCorrect = false,
            score = 0,
            completedCount = completed,
            isFinished = false
        )

        if (_currentScreen.value != Screen.SentenceBuilder) {
            screenStack.add(Screen.SentenceBuilder)
            _currentScreen.value = Screen.SentenceBuilder
        }
    }

    private fun parseSentenceChips(english: String): List<SentenceChip> {
        // Tokenize words, preserving words with trailing punctuation or clean words
        val words = english.trim().split(Regex("\\s+")).filter { it.isNotBlank() }
        return words.mapIndexed { index, word ->
            SentenceChip(id = index, word = word, isUsed = false)
        }.shuffled()
    }

    fun onSentenceChipClicked(chip: SentenceChip) {
        val current = _sentenceBuilderState.value ?: return
        if (current.isChecked || chip.isUsed) return

        val updatedAvailable = current.availableChips.map {
            if (it.id == chip.id) it.copy(isUsed = true) else it
        }
        val updatedEntered = current.enteredChips + chip

        _sentenceBuilderState.value = current.copy(
            availableChips = updatedAvailable,
            enteredChips = updatedEntered
        )

        // If all chips have been placed, automatically check and advance
        if (updatedEntered.size == current.availableChips.size) {
            executeSentenceCheck(updatedEntered)
        }
    }

    fun removeSentenceChip(chip: SentenceChip) {
        sentenceAutoAdvanceJob?.cancel()
        val current = _sentenceBuilderState.value ?: return
        if (current.isChecked) return

        val updatedEntered = current.enteredChips.filterNot { it.id == chip.id }
        val updatedAvailable = current.availableChips.map {
            if (it.id == chip.id) it.copy(isUsed = false) else it
        }

        _sentenceBuilderState.value = current.copy(
            availableChips = updatedAvailable,
            enteredChips = updatedEntered
        )
    }

    fun clearEnteredSentence() {
        sentenceAutoAdvanceJob?.cancel()
        val current = _sentenceBuilderState.value ?: return
        if (current.isChecked) return

        val updatedAvailable = current.availableChips.map { it.copy(isUsed = false) }
        _sentenceBuilderState.value = current.copy(
            availableChips = updatedAvailable,
            enteredChips = emptyList()
        )
    }

    fun checkSentence() {
        val current = _sentenceBuilderState.value ?: return
        if (current.enteredChips.isEmpty() || current.isChecked) return
        executeSentenceCheck(current.enteredChips)
    }

    private fun executeSentenceCheck(entered: List<SentenceChip>) {
        sentenceAutoAdvanceJob?.cancel()
        val current = _sentenceBuilderState.value ?: return
        val target = current.targetSentence ?: return

        fun normalize(str: String): String =
            str.replace(Regex("[.,!?;:]"), "").trim().lowercase()

        val assembledText = entered.joinToString(" ") { it.word }
        val isCorrect = normalize(assembledText) == normalize(target.english)

        if (isCorrect) {
            viewModelScope.launch {
                repository.markSentenceCompleted(target.id)
            }
        }

        speak(target.english, current.language)

        val updatedSentences = if (isCorrect) {
            current.sentences.map { if (it.id == target.id) it.copy(isCompleted = true) else it }
        } else {
            current.sentences
        }

        _sentenceBuilderState.value = current.copy(
            sentences = updatedSentences,
            enteredChips = entered,
            isChecked = true,
            isCorrect = isCorrect,
            score = if (isCorrect) current.score + 15 else current.score,
            completedCount = updatedSentences.count { it.isCompleted }
        )

        // Automatically transition to the next sentence (3 seconds delay to review correct answer)
        sentenceAutoAdvanceJob = viewModelScope.launch {
            delay(3000L)
            nextSentence()
        }
    }

    fun nextSentence() {
        sentenceAutoAdvanceJob?.cancel()
        val current = _sentenceBuilderState.value ?: return
        val nextIndex = current.currentIndex + 1
        if (nextIndex >= current.sentences.size) {
            _sentenceBuilderState.value = current.copy(isFinished = true)
            viewModelScope.launch { repository.incrementQuizzesCompleted() }
        } else {
            val nextTarget = current.sentences[nextIndex]
            val chips = parseSentenceChips(nextTarget.english)
            _sentenceBuilderState.value = current.copy(
                currentIndex = nextIndex,
                targetSentence = nextTarget,
                availableChips = chips,
                enteredChips = emptyList(),
                isChecked = false,
                isCorrect = false
            )
        }
    }

    fun jumpToSentence(index: Int) {
        sentenceAutoAdvanceJob?.cancel()
        val current = _sentenceBuilderState.value ?: return
        val sentences = current.sentences
        val safeIndex = index.coerceIn(0, sentences.size - 1)
        val target = sentences[safeIndex]
        val chips = parseSentenceChips(target.english)
        _sentenceBuilderState.value = current.copy(
            currentIndex = safeIndex,
            targetSentence = target,
            availableChips = chips,
            enteredChips = emptyList(),
            isChecked = false,
            isCorrect = false,
            isFinished = false
        )
    }

    override fun onCleared() {
        super.onCleared()
        timerJob?.cancel()
        ttsHelper.shutdown()
    }
}
