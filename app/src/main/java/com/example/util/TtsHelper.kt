package com.example.util

import android.content.Context
import android.speech.tts.TextToSpeech
import java.util.Locale

class TtsHelper(context: Context) : TextToSpeech.OnInitListener {
    private var tts: TextToSpeech? = TextToSpeech(context.applicationContext, this)
    private var isReady = false
    private var currentLanguageCode: String = "EN"

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            val locale = localeFor(currentLanguageCode)
            val result = tts?.setLanguage(locale)
            if (result != TextToSpeech.LANG_MISSING_DATA && result != TextToSpeech.LANG_NOT_SUPPORTED) {
                isReady = true
                tts?.setSpeechRate(0.85f)
                tts?.setPitch(1.0f)
            } else {
                // Keep usable even if a specific TTS pack isn't preloaded
                tts?.setLanguage(Locale.US)
                isReady = true
                tts?.setSpeechRate(0.85f)
                tts?.setPitch(1.0f)
            }
        }
    }

    private fun localeFor(languageCode: String): Locale {
        return when (languageCode.uppercase()) {
            "DE" -> Locale.GERMANY
            else -> Locale.US
        }
    }

    fun speak(text: String, languageCode: String = "EN") {
        if (!isReady || text.isBlank()) return
        if (!currentLanguageCode.equals(languageCode, ignoreCase = true)) {
            currentLanguageCode = languageCode.uppercase()
            val result = tts?.setLanguage(localeFor(currentLanguageCode))
            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                if (currentLanguageCode == "DE") {
                    tts?.setLanguage(Locale.GERMAN)
                }
            }
        }
        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "vocab_tts_${System.currentTimeMillis()}")
    }

    fun shutdown() {
        tts?.stop()
        tts?.shutdown()
        tts = null
    }
}
