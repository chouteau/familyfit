package com.example.triduofit.audio

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import com.example.triduofit.data.SettingsRepository
import java.util.Locale

enum class VoiceCommand {
    GO,
    PAUSE,
    RESET_TIMER,
    RESUME,
    NEXT,
    REPEAT,
    UNKNOWN
}

class VoiceCommandRecognizer(
    private val context: Context,
    private val settingsRepository: SettingsRepository,
    private val onCommandDetected: (VoiceCommand, String) -> Unit
) {
    private var nativeSpeechRecognizer: SpeechRecognizer? = null
    private val mainHandler = Handler(Looper.getMainLooper())
    private var isContinuousListeningActive = false

    init {
        mainHandler.post {
            initNativeRecognizer()
        }
    }

    private fun initNativeRecognizer() {
        if (!SpeechRecognizer.isRecognitionAvailable(context)) return

        try {
            nativeSpeechRecognizer?.destroy()
            nativeSpeechRecognizer = SpeechRecognizer.createSpeechRecognizer(context).apply {
                setRecognitionListener(object : RecognitionListener {
                    override fun onResults(results: Bundle?) {
                        val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        val text = matches?.firstOrNull()?.lowercase(Locale.FRENCH)?.trim() ?: ""
                        if (text.isNotBlank()) {
                            parseAndDispatchCommand(text)
                        }
                        restartListeningDelay(200)
                    }

                    override fun onError(error: Int) {
                        // En cas de silence, timeout ou bruit ambiant, on relance immédiatement l'écoute permanente
                        restartListeningDelay(250)
                    }

                    override fun onEndOfSpeech() {
                        // La parole s'est arrêtée, on attend les résultats puis le listener relance
                    }

                    override fun onReadyForSpeech(params: Bundle?) {}
                    override fun onBeginningOfSpeech() {}
                    override fun onRmsChanged(rmsdB: Float) {}
                    override fun onBufferReceived(buffer: ByteArray?) {}
                    override fun onPartialResults(partialResults: Bundle?) {
                        val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        val text = matches?.firstOrNull()?.lowercase(Locale.FRENCH)?.trim() ?: ""
                        // Détection anticipée et ultra-rapide des mots clés prioritaires
                        if (text.contains("stop") || text.contains("pause") || text.contains("zéro")) {
                            parseAndDispatchCommand(text)
                        }
                    }
                    override fun onEvent(eventType: Int, params: Bundle?) {}
                })
            }
        } catch (_: Exception) {}
    }

    fun startListening() {
        isContinuousListeningActive = true
        mainHandler.post {
            launchRecognizerIntent()
        }
    }

    private fun launchRecognizerIntent() {
        if (!isContinuousListeningActive) return

        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "fr-FR")
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, "fr-FR")
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 1500L)
            putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS, 1500L)
        }

        try {
            nativeSpeechRecognizer?.startListening(intent)
        } catch (_: Exception) {
            initNativeRecognizer()
            restartListeningDelay(500)
        }
    }

    private fun restartListeningDelay(delayMillis: Long) {
        if (!isContinuousListeningActive) return
        mainHandler.removeCallbacksAndMessages(null)
        mainHandler.postDelayed({
            launchRecognizerIntent()
        }, delayMillis)
    }

    fun stopListening() {
        isContinuousListeningActive = false
        mainHandler.removeCallbacksAndMessages(null)
        mainHandler.post {
            try {
                nativeSpeechRecognizer?.stopListening()
                nativeSpeechRecognizer?.cancel()
            } catch (_: Exception) {}
        }
    }

    private fun parseAndDispatchCommand(rawText: String) {
        val text = rawText.lowercase(Locale.FRENCH)

        val command = when {
            // 1. Commande REVIENS À ZÉRO / RESET
            text.contains("zéro") || text.contains("zero") || text.contains("recommence") ||
            text.contains("repart à zéro") || text.contains("reset") -> VoiceCommand.RESET_TIMER

            // 2. Commande PAUSE / METS EN PAUSE
            text.contains("pause") || text.contains("mets en pause") || text.contains("met en pause") ||
            text.contains("stop") || text.contains("arrête") || text.contains("bloque") || text.contains("attends") -> VoiceCommand.PAUSE

            // 3. Commande GO / DÉMARRE
            text.contains("go") || text.contains("c'est parti") || text.contains("on y va") ||
            text.contains("démarre") || text.contains("départ") || text.contains("top") -> VoiceCommand.GO

            // 4. Commande REPRENDS
            text.contains("reprends") || text.contains("reprendre") || text.contains("continue") ||
            text.contains("repart") -> VoiceCommand.RESUME

            // 5. Commande SUIVANT
            text.contains("suivant") || text.contains("suivante") || text.contains("passe") ||
            text.contains("prochain") || text.contains("après") -> VoiceCommand.NEXT

            // 6. Commande RÉPÈTE / AIDE
            text.contains("répète") || text.contains("repete") || text.contains("conseil") ||
            text.contains("aide") || text.contains("consigne") -> VoiceCommand.REPEAT

            else -> VoiceCommand.UNKNOWN
        }

        if (command != VoiceCommand.UNKNOWN) {
            onCommandDetected(command, rawText)
        }
    }

    fun release() {
        stopListening()
        mainHandler.post {
            try {
                nativeSpeechRecognizer?.destroy()
                nativeSpeechRecognizer = null
            } catch (_: Exception) {}
        }
    }
}
