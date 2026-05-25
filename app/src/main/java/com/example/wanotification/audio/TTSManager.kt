package com.example.wanotification.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.os.Build
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import java.util.Locale
import java.util.concurrent.atomic.AtomicInteger

import com.example.wanotification.queue.SpeechQueueManager

class TTSManager(
    context: Context
) : TextToSpeech.OnInitListener {

    companion object {
        private const val TAG = "TTSManager"
    }

    private val appContext =
        context.applicationContext

    private var tts: TextToSpeech? = null

    private var isReady = false

    private val pendingCount =
        AtomicInteger(0)

    private val shutdownLock = Any()

    private var audioManager: AudioManager? = null

    private var audioFocusRequest: AudioFocusRequest? = null

    @Volatile
    private var hasAudioFocus = false

    private val focusChangeListener =
        AudioManager.OnAudioFocusChangeListener { }

    override fun onInit(
        status: Int
    ) {

        val ttsInstance =
            tts ?: return

        if (status == TextToSpeech.SUCCESS) {

            val preferredLocales = listOf(
                Locale("id", "ID"),
                Locale.getDefault(),
                Locale.US
            )

            var languageSet: Locale? = null

            for (locale in preferredLocales) {
                val result = ttsInstance.setLanguage(locale)

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

            ttsInstance.setSpeechRate(1.0f)

            ttsInstance.setPitch(1.0f)

            ttsInstance.setOnUtteranceProgressListener(
                object : UtteranceProgressListener() {
                    override fun onStart(utteranceId: String) {}

                    override fun onDone(utteranceId: String) {
                        handleCompletion()
                    }

                    override fun onError(utteranceId: String) {
                        handleCompletion()
                    }

                    override fun onError(
                        utteranceId: String,
                        errorCode: Int
                    ) {
                        handleCompletion()
                    }
                }
            )

            isReady = true

            flushPendingSpeech()
        } else {
            Log.e(TAG, "TTS initialization failed with status=$status")
            shutdownInternal()
        }
    }

    fun speak(
        text: String
    ) {

        if (text.isBlank()) return

        ensureTts()

        if (!isReady) {

            Log.d(TAG, "TTS not ready yet; queueing speech")

            SpeechQueueManager.enqueue(text)

            return
        }

        speakNow(text)
    }

    private fun ensureTts() {
        if (tts != null) return

        tts = TextToSpeech(appContext, this)

        audioManager =
            appContext.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    }

    private fun flushPendingSpeech() {

        if (!isReady) return

        val pending =
            SpeechQueueManager.drain()

        pending.forEach { item ->
            speakNow(item)
        }
    }

    private fun speakNow(
        text: String
    ) {

        val ttsInstance =
            tts ?: return

        requestAudioFocus()

        val utteranceId =
            "notif_${System.currentTimeMillis()}_${pendingCount.incrementAndGet()}"

        val result = ttsInstance.speak(
            text,
            TextToSpeech.QUEUE_ADD,
            null,
            utteranceId
        )

        if (result == TextToSpeech.ERROR) {
            Log.e(TAG, "TTS speak() returned ERROR for utterance=$utteranceId")
            handleCompletion()
        } else {
            Log.d(TAG, "Queued speech utterance=$utteranceId")
        }
    }

    private fun handleCompletion() {
        val remaining =
            pendingCount.decrementAndGet()

        if (remaining <= 0) {
            shutdownInternal()
        }
    }

    private fun requestAudioFocus() {
        val manager =
            audioManager ?: return

        if (hasAudioFocus) return

        val focusGranted =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val attrs = AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ASSISTANCE_ACCESSIBILITY)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                    .build()

                val request = AudioFocusRequest.Builder(
                    AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK
                )
                    .setAudioAttributes(attrs)
                    .setOnAudioFocusChangeListener(focusChangeListener)
                    .build()

                audioFocusRequest = request

                manager.requestAudioFocus(request) ==
                    AudioManager.AUDIOFOCUS_REQUEST_GRANTED
            } else {
                manager.requestAudioFocus(
                    focusChangeListener,
                    AudioManager.STREAM_MUSIC,
                    AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK
                ) == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
            }

        hasAudioFocus = focusGranted
    }

    private fun abandonAudioFocus() {
        if (!hasAudioFocus) return

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            audioFocusRequest?.let {
                audioManager?.abandonAudioFocusRequest(it)
            }
        } else {
            audioManager?.abandonAudioFocus(focusChangeListener)
        }

        hasAudioFocus = false
    }

    private fun shutdownInternal() {

        synchronized(shutdownLock) {
            if (pendingCount.get() > 0) {
                return
            }

            try {
                val ttsInstance = tts ?: return

                if (isReady) {
                    ttsInstance.stop()
                }

                ttsInstance.shutdown()
            } catch (ex: Exception) {
                Log.e(TAG, "Error while shutting down TTS", ex)
            } finally {
                isReady = false
                tts = null
                abandonAudioFocus()
            }
        }
    }

    /**
     * Shutdown the TextToSpeech engine and clear any queued speech.
     * Safe to call multiple times.
     */
    fun shutdown() {

        synchronized(shutdownLock) {
            try {
                SpeechQueueManager.clear()

                pendingCount.set(0)

                val ttsInstance = tts

                if (ttsInstance != null && isReady) {
                    ttsInstance.stop()
                }

                ttsInstance?.shutdown()
            } catch (ex: Exception) {
                Log.e(TAG, "Error while shutting down TTS", ex)
            } finally {
                isReady = false
                tts = null
                abandonAudioFocus()
            }
        }
    }
}
