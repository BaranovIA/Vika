package com.baranova.multiplication

import android.content.Context
import android.speech.tts.TextToSpeech
import java.util.Locale

/** Озвучка текста встроенным синтезатором речи Android (русский язык). */
class Speaker(context: Context) {
    private var ready = false
    private var pending: String? = null
    private var tts: TextToSpeech? = null

    init {
        tts = TextToSpeech(context.applicationContext) { status ->
            if (status == TextToSpeech.SUCCESS) {
                val engine = tts
                val result = engine?.setLanguage(Locale.forLanguageTag("ru-RU"))
                ready = result != null &&
                    result != TextToSpeech.LANG_MISSING_DATA &&
                    result != TextToSpeech.LANG_NOT_SUPPORTED
                engine?.setSpeechRate(0.95f)
                val text = pending
                pending = null
                if (ready && text != null) speak(text)
            }
        }
    }

    fun speak(text: String) {
        if (!ready) {
            pending = text
            return
        }
        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "trainer")
    }

    fun stop() {
        pending = null
        tts?.stop()
    }

    fun shutdown() {
        tts?.stop()
        tts?.shutdown()
        tts = null
        ready = false
    }
}
