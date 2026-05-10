package com.example.wanotification.audio

import android.content.Context
import android.speech.tts.TextToSpeech
import java.util.Locale

class TTSManager(
    context: Context
) : TextToSpeech.OnInitListener {

    private var tts: TextToSpeech =
        TextToSpeech(context, this)

    private var isReady = false

    override fun onInit(
        status: Int
    ) {

        if (status == TextToSpeech.SUCCESS) {

            tts.language = Locale("id", "ID")

            tts.setSpeechRate(1.0f)

            tts.setPitch(1.0f)

            isReady = true
        }
    }

    fun speak(
        text: String
    ) {

        if (!isReady) return

        tts.speak(
            text,
            TextToSpeech.QUEUE_FLUSH,
            null,
            null
        )
    }
}