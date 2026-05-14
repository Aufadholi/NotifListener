package com.example.wanotification.audio

import android.content.Context
import android.speech.tts.TextToSpeech
import android.util.Log
import java.util.Locale

import com.example.wanotification.queue.SpeechQueueManager

class TTSManager(
    context: Context
) : TextToSpeech.OnInitListener {

    companion object {
        private const val TAG = "TTSManager"
    }

    private var tts: TextToSpeech =
        TextToSpeech(context.applicationContext, this)

    private var isReady = false

    override fun onInit(
        status: Int
    ) {

        if (status == TextToSpeech.SUCCESS) {

            val preferredLocales = listOf(
                Locale("id", "ID"),
                Locale.getDefault(),
                Locale.US
            )

            var languageSet: Locale? = null

            for (locale in preferredLocales) {
                val result = tts.setLanguage(locale)

                if (result != TextToSpeech.LANG_MISSING_DATA &&
                    result != TextToSpeech.LANG_NOT_SUPPORTED
                ) {
                    languageSet = locale
                    break
                }
            }

            if (languageSet == null) {
                Log.e(TAG, "No supported TTS language available")
            } else {
                Log.d(TAG, "TTS language set to $languageSet")
            }

            tts.setSpeechRate(1.0f)

            tts.setPitch(1.0f)

            isReady = true

            flushPendingSpeech()
        } else {
            Log.e(TAG, "TTS initialization failed with status=$status")
        }
    }

    fun speak(
        text: String
    ) {

        if (text.isBlank()) return

        if (!isReady) {

            Log.d(TAG, "TTS not ready yet; queueing speech")

            SpeechQueueManager.enqueue(text)

            return
        }

        speakNow(text)
    }

    private fun flushPendingSpeech() {

        if (!isReady) return

        val pending = SpeechQueueManager.drain()

        pending.forEach { item ->
            speakNow(item)
        }
    }

    private fun speakNow(
        text: String
    ) {

        val utteranceId = "notif_${System.currentTimeMillis()}"

        val result = tts.speak(
            text,
            TextToSpeech.QUEUE_ADD,
            null,
            utteranceId
        )

        if (result == TextToSpeech.ERROR) {
            Log.e(TAG, "TTS speak() returned ERROR for utterance=$utteranceId")
        } else {
            Log.d(TAG, "Queued speech utterance=$utteranceId")
        }
    }

}