package com.example.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.util.TtsHelper
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.Content
import com.google.firebase.ai.type.FirebaseAIException
import com.google.firebase.ai.type.GenerativeBackend
import com.google.firebase.ai.type.RequestOptions
import com.google.firebase.ai.type.RequestTimeoutException
import com.google.firebase.ai.type.ServerException
import com.google.firebase.ai.type.ThinkingLevel
import com.google.firebase.ai.type.content
import com.google.firebase.ai.type.generationConfig
import com.google.firebase.ai.type.thinkingConfig
import com.google.firebase.appcheck.FirebaseAppCheck
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.withTimeoutOrNull
import java.io.IOException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import java.util.UUID
import kotlin.coroutines.resume

private const val TAG = "GeminiChatViewModel"
private const val OVERALL_TIMEOUT_MS = 60_000L
private const val STREAM_FIRST_CHUNK_TIMEOUT_MS = 20_000L
private const val APP_CHECK_PREFLIGHT_TIMEOUT_MS = 10_000L

class GeminiChatViewModel(
    private val ttsHelper: TtsHelper
) : ViewModel() {

    private val model by lazy {
        Firebase.ai(backend = GenerativeBackend.googleAI()).generativeModel(
            modelName = "gemini-3.8-flash",
            generationConfig = generationConfig {
                temperature = 0.7f
                maxOutputTokens = 1024
                thinkingConfig = thinkingConfig {
                    // ThinkingLevel.LOW keeps Gemini 3.8 Flash fast for conversational tutoring
                    thinkingLevel = ThinkingLevel.LOW
                }
            },
            systemInstruction = content {
                text(
                    """
                    Ти — дружній та досвідчений AI-репетитор Gemini у додатку для вивчення англійської (EN) та німецької (DE) мов для рівнів A1, A2, B1.
                    Спілкуйся з учнем переважно українською мовою, наводячи слова, приклади речень і діалоги цільовою мовою (англійською або німецькою залежно від поточного контексту учня) з перекладом та поясненнями.
                    Для німецької мови завжди вказуй артиклі іменників (der/die/das).
                    Якщо учень просить перевірити речення — вкажи на помилки м'яко, поясни правило і дай правильний варіант.
                    Відповідай структуровано, лаконічно та зрозуміло для рівня учня.
                    """.trimIndent()
                )
            },
            requestOptions = RequestOptions(55_000L)
        )
    }

    // Store committed conversation history separately so cancelling/timing out a Chat turn
    // never leaves Chat's internal Semaphore locked or pollutes history with failed prompts.
    private val conversationHistory = mutableListOf<Content>()
    private var generationJob: Job? = null

    private val _uiState = MutableStateFlow(GeminiChatUiState())
    val uiState: StateFlow<GeminiChatUiState> = _uiState.asStateFlow()

    fun sendMessage(
        rawPrompt: String,
        language: LearningLanguage,
        level: CefrLevel
    ) {
        val cleanPrompt = rawPrompt.trim()
        if (cleanPrompt.isEmpty()) return
        if (_uiState.value.requestState == GeminiRequestState.Loading ||
            _uiState.value.requestState == GeminiRequestState.Streaming
        ) {
            return
        }

        val userMessage = GeminiChatMessage(
            id = UUID.randomUUID().toString(),
            author = ChatAuthor.USER,
            text = cleanPrompt
        )
        val assistantMsgId = UUID.randomUUID().toString()
        val placeholderAssistantMsg = GeminiChatMessage(
            id = assistantMsgId,
            author = ChatAuthor.GEMINI,
            text = "",
            isStreaming = true
        )

        _uiState.update { state ->
            state.copy(
                messages = state.messages + userMessage + placeholderAssistantMsg,
                requestState = GeminiRequestState.Loading,
                lastFailedPrompt = null
            )
        }

        val contextualPrompt =
            "[Поточна мова учня: ${language.titleUa} (${language.code}), рівень: ${level.code}]\n$cleanPrompt"
        val historySnapshot = conversationHistory.toList()

        generationJob?.cancel()
        generationJob = viewModelScope.launch {
            val startedAt = System.currentTimeMillis()
            var appCheckDiag: String? = null
            try {
                val finalResponse = withTimeout(OVERALL_TIMEOUT_MS) {
                    withContext(Dispatchers.IO) {
                        appCheckDiag = preflightAppCheck()

                        val streamedText = streamWithWatchdog(
                            historySnapshot = historySnapshot,
                            contextualPrompt = contextualPrompt,
                            assistantMsgId = assistantMsgId
                        )

                        if (streamedText.isNotBlank()) {
                            streamedText
                        } else {
                            Log.d(TAG, "Falling back to unary sendMessage after stream produced no text.")
                            val unaryChat = model.startChat(history = historySnapshot)
                            val unaryResponse = unaryChat.sendMessage(contextualPrompt)
                            unaryResponse.text.orEmpty().trim()
                        }
                    }
                }

                if (finalResponse.isEmpty()) {
                    removeMessageById(assistantMsgId)
                    _uiState.update {
                        it.copy(
                            requestState = GeminiRequestState.Error("Модель повернула порожню відповідь. Спробуйте ще раз."),
                            lastFailedPrompt = cleanPrompt
                        )
                    }
                } else {
                    conversationHistory.add(content("user") { text(contextualPrompt) })
                    conversationHistory.add(content("model") { text(finalResponse) })
                    _uiState.update { state ->
                        state.copy(
                            messages = state.messages.map { msg ->
                                if (msg.id == assistantMsgId) {
                                    msg.copy(text = finalResponse, isStreaming = false)
                                } else {
                                    msg
                                }
                            },
                            requestState = GeminiRequestState.Success,
                            lastFailedPrompt = null
                        )
                    }
                }
            } catch (e: TimeoutCancellationException) {
                val elapsedMs = System.currentTimeMillis() - startedAt
                Log.e(TAG, "Gemini chat timed out after ${elapsedMs}ms (appCheck=$appCheckDiag)", e)
                removeMessageById(assistantMsgId)
                val details = appCheckDiag?.let { " ($it)" } ?: ""
                _uiState.update {
                    it.copy(
                        requestState = GeminiRequestState.Error(
                            "Час очікування відповіді вийшов (60 сек)$details. Перевірте інтернет або налаштування Firebase і спробуйте знову."
                        ),
                        lastFailedPrompt = cleanPrompt
                    )
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Gemini chat failed [${e::class.java.simpleName}]", e)
                removeMessageById(assistantMsgId)
                _uiState.update {
                    it.copy(
                        requestState = GeminiRequestState.Error(formatUserErrorMessage(e)),
                        lastFailedPrompt = cleanPrompt
                    )
                }
            }
        }
    }

    private suspend fun preflightAppCheck(): String? {
        val start = System.currentTimeMillis()
        return try {
            val result = withTimeoutOrNull(APP_CHECK_PREFLIGHT_TIMEOUT_MS) {
                suspendCancellableCoroutine { cont ->
                    FirebaseAppCheck.getInstance().getAppCheckToken(false)
                        .addOnSuccessListener { token ->
                            if (cont.isActive) {
                                cont.resume(Result.success(token.token))
                            }
                        }
                        .addOnFailureListener { exc ->
                            if (cont.isActive) {
                                cont.resume(Result.failure(exc))
                            }
                        }
                }
            }
            val elapsed = System.currentTimeMillis() - start
            when {
                result == null -> {
                    Log.w(TAG, "AppCheck preflight timed out after ${elapsed}ms")
                    "App Check не відповів за 10 сек"
                }
                result.isFailure -> {
                    val err = result.exceptionOrNull()
                    Log.w(TAG, "AppCheck preflight failed in ${elapsed}ms: ${err?.message}")
                    "помилка App Check: ${err?.localizedMessage ?: err?.javaClass?.simpleName}"
                }
                else -> {
                    Log.d(TAG, "AppCheck token ready in ${elapsed}ms")
                    null
                }
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.w(TAG, "AppCheck preflight check skipped: ${e.message}")
            null
        }
    }

    private suspend fun streamWithWatchdog(
        historySnapshot: List<Content>,
        contextualPrompt: String,
        assistantMsgId: String
    ): String {
        val accumulated = StringBuilder()
        var receivedFirstTextChunk = false
        var streamStalled = false

        coroutineScope {
            val streamChat = model.startChat(history = historySnapshot)
            val streamJob = launch {
                streamChat.sendMessageStream(contextualPrompt).collect { chunk ->
                    val chunkText = chunk.text.orEmpty()
                    if (chunkText.isNotEmpty()) {
                        receivedFirstTextChunk = true
                        accumulated.append(chunkText)
                        val currentText = accumulated.toString()
                        _uiState.update { state ->
                            state.copy(
                                messages = state.messages.map { msg ->
                                    if (msg.id == assistantMsgId) {
                                        msg.copy(text = currentText, isStreaming = true)
                                    } else {
                                        msg
                                    }
                                },
                                requestState = GeminiRequestState.Streaming
                            )
                        }
                    }
                }
            }

            val watchdogJob = launch {
                delay(STREAM_FIRST_CHUNK_TIMEOUT_MS)
                if (!receivedFirstTextChunk && streamJob.isActive) {
                    streamStalled = true
                    Log.w(
                        TAG,
                        "Stream did not emit text within ${STREAM_FIRST_CHUNK_TIMEOUT_MS}ms; cancelling stream for unary fallback."
                    )
                    streamJob.cancel(CancellationException("Stream first-chunk watchdog triggered"))
                }
            }

            streamJob.join()
            watchdogJob.cancel()
        }

        // Propagate parent cancellation (e.g., 60s timeout or clearChat()) before attempting fallback
        currentCoroutineContext().ensureActive()

        return if (streamStalled && accumulated.isEmpty()) {
            ""
        } else {
            accumulated.toString().trim()
        }
    }

    private fun formatUserErrorMessage(e: Exception): String {
        val raw = e.localizedMessage?.takeIf { it.isNotBlank() }
        return when (e) {
            is RequestTimeoutException, is SocketTimeoutException ->
                "Сервер Gemini не відповів вчасно. Спробуйте ще раз."
            is UnknownHostException, is IOException ->
                "Немає стабільного з'єднання з сервером Gemini. Перевірте інтернет."
            is ServerException ->
                if (raw != null && (raw.contains("403") || raw.contains("AppCheck", ignoreCase = true))) {
                    "Помилка доступу Firebase / App Check (403): $raw"
                } else {
                    raw ?: "Помилка сервера Gemini. Спробуйте пізніше."
                }
            is FirebaseAIException ->
                raw ?: "Помилка Firebase AI (${e::class.java.simpleName})."
            else ->
                raw ?: "Не вдалося отримати відповідь від Gemini (${e::class.java.simpleName})."
        }
    }

    private fun removeMessageById(messageId: String) {
        _uiState.update { state ->
            state.copy(messages = state.messages.filterNot { it.id == messageId })
        }
    }

    fun retryLastMessage(language: LearningLanguage, level: CefrLevel) {
        val lastPrompt = _uiState.value.lastFailedPrompt ?: return
        _uiState.update { state ->
            val updated = if (state.messages.lastOrNull()?.author == ChatAuthor.USER &&
                state.messages.lastOrNull()?.text == lastPrompt
            ) {
                state.messages.dropLast(1)
            } else {
                state.messages
            }
            state.copy(messages = updated, lastFailedPrompt = null, requestState = GeminiRequestState.Idle)
        }
        sendMessage(lastPrompt, language, level)
    }

    fun clearChat() {
        generationJob?.cancel()
        conversationHistory.clear()
        _uiState.value = GeminiChatUiState()
    }

    fun speak(text: String, language: LearningLanguage) {
        ttsHelper.speak(text, language.code)
    }
}
