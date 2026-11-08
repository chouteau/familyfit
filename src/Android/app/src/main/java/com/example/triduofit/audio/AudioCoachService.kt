package com.example.triduofit.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.os.Handler
import android.os.Looper
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import com.example.triduofit.data.SettingsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.security.MessageDigest
import java.util.Locale
import java.util.concurrent.TimeUnit

class AudioCoachService(
    private val context: Context,
    private val settingsRepository: SettingsRepository,
    private val onDuckingChanged: (Boolean) -> Unit
) {
    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .build()

    private val audioScope = CoroutineScope(Dispatchers.IO)
    private var nativeTts: TextToSpeech? = null
    private var isNativeTtsReady = false
    private var activeMediaPlayer: MediaPlayer? = null
    private var currentSpeechJob: Job? = null
    private val mainHandler = Handler(Looper.getMainLooper())

    private val openAiCacheDir = File(context.cacheDir, "openai_tts_cache").apply { mkdirs() }
    private val googleCacheDir = File(context.cacheDir, "google_tts_cache").apply { mkdirs() }

    init {
        // Initialisation du fallback Android natif à un débit posé et calme
        nativeTts = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                nativeTts?.language = Locale.FRENCH
                nativeTts?.setSpeechRate(0.92f) // Débit ralenti et posé pour éliminer toute précipitation
                nativeTts?.setPitch(1.0f)
                nativeTts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                    override fun onStart(utteranceId: String?) {
                        mainHandler.post { onDuckingChanged(true) }
                    }

                    override fun onDone(utteranceId: String?) {
                        mainHandler.post { onDuckingChanged(false) }
                    }

                    @Deprecated("Deprecated in Java")
                    override fun onError(utteranceId: String?) {
                        mainHandler.post { onDuckingChanged(false) }
                    }
                })
                isNativeTtsReady = true
            }
        }
    }

    fun speak(text: String, onDone: (() -> Unit)? = null) {
        val engine = settingsRepository.getTtsEngine()
        val googleKey = settingsRepository.getGoogleApiKey()
        val openAiKey = settingsRepository.getOpenAiApiKey()

        currentSpeechJob?.cancel()

        currentSpeechJob = audioScope.launch {
            var audioFile: File? = null

            if (engine == "google" && googleKey.isNotBlank()) {
                audioFile = getOrFetchGoogleTts(text, googleKey, settingsRepository.getGoogleVoice())
            } else if (engine == "openai" && openAiKey.isNotBlank()) {
                audioFile = getOrFetchOpenAiTts(text, openAiKey, settingsRepository.getSelectedVoice())
            } else if (googleKey.isNotBlank()) {
                audioFile = getOrFetchGoogleTts(text, googleKey, settingsRepository.getGoogleVoice())
            } else if (openAiKey.isNotBlank()) {
                audioFile = getOrFetchOpenAiTts(text, openAiKey, settingsRepository.getSelectedVoice())
            }

            withContext(Dispatchers.Main) {
                if (audioFile != null && audioFile.exists()) {
                    playAudioFile(audioFile, onDone)
                } else {
                    speakNativeTts(text, onDone)
                }
            }
        }
    }

    /**
     * Synthèse vocale haute fidélité via Google Cloud Text-to-Speech (Voix Journey / Studio / Neural2)
     * Offre 1 million de caractères gratuits par mois.
     */
    private fun getOrFetchGoogleTts(text: String, apiKey: String, voiceName: String): File? {
        val hash = hashString("google-$voiceName-$text")
        val cachedFile = File(googleCacheDir, "$hash.mp3")

        if (cachedFile.exists() && cachedFile.length() > 0) {
            return cachedFile
        }

        try {
            val jsonPayload = JSONObject().apply {
                put("input", JSONObject().apply {
                    put("text", text)
                })
                put("voice", JSONObject().apply {
                    put("languageCode", "fr-FR")
                    put("name", voiceName)
                })
                put("audioConfig", JSONObject().apply {
                    put("audioEncoding", "MP3")
                    put("speakingRate", 0.95) // Débit naturel & articulé
                    put("pitch", 0.0)
                })
            }

            val request = Request.Builder()
                .url("https://texttospeech.googleapis.com/v1/text:synthesize?key=$apiKey")
                .header("Content-Type", "application/json; charset=utf-8")
                .post(jsonPayload.toString().toRequestBody("application/json".toMediaType()))
                .build()

            httpClient.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val bodyString = response.body?.string()
                    if (!bodyString.isNullOrBlank()) {
                        val respJson = JSONObject(bodyString)
                        val audioContentBase64 = respJson.optString("audioContent", "")
                        if (audioContentBase64.isNotBlank()) {
                            val audioBytes = android.util.Base64.decode(audioContentBase64, android.util.Base64.DEFAULT)
                            FileOutputStream(cachedFile).use { it.write(audioBytes) }
                            return cachedFile
                        }
                    }
                }
            }
        } catch (_: Exception) {}
        return null
    }

    private fun getOrFetchOpenAiTts(text: String, apiKey: String, voice: String): File? {
        val hash = hashString("openai-$text-$voice-v2")
        val cachedFile = File(openAiCacheDir, "$hash.mp3")

        if (cachedFile.exists() && cachedFile.length() > 0) {
            return cachedFile
        }

        try {
            val jsonPayload = JSONObject().apply {
                put("model", "tts-1")
                put("input", text)
                put("voice", voice)
                put("response_format", "mp3")
                put("speed", 0.92) // Voix OpenAI calme, posée et articulée
            }

            val request = Request.Builder()
                .url("https://api.openai.com/v1/audio/speech")
                .header("Authorization", "Bearer $apiKey")
                .header("Content-Type", "application/json")
                .post(jsonPayload.toString().toRequestBody("application/json".toMediaType()))
                .build()

            httpClient.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val bytes = response.body?.bytes()
                    if (bytes != null && bytes.isNotEmpty()) {
                        FileOutputStream(cachedFile).use { it.write(bytes) }
                        return cachedFile
                    }
                }
            }
        } catch (_: Exception) {}
        return null
    }

    private fun playAudioFile(file: File, onDone: (() -> Unit)?) {
        try {
            activeMediaPlayer?.stop()
            activeMediaPlayer?.release()
            activeMediaPlayer = null

            onDuckingChanged(true) // Baisse le volume de la musique

            activeMediaPlayer = MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ASSISTANCE_NAVIGATION_GUIDANCE)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                        .build()
                )
                setDataSource(file.absolutePath)
                setOnPreparedListener { mp ->
                    mp.start()
                }
                setOnCompletionListener {
                    onDuckingChanged(false)
                    onDone?.invoke()
                }
                setOnErrorListener { _, _, _ ->
                    onDuckingChanged(false)
                    true
                }
                prepareAsync()
            }
        } catch (e: Exception) {
            onDuckingChanged(false)
            speakNativeTts(file.nameWithoutExtension, onDone)
        }
    }

    private fun speakNativeTts(text: String, onDone: (() -> Unit)?) {
        onDuckingChanged(true)
        val utteranceId = "coach_${System.currentTimeMillis()}"
        val params = android.os.Bundle().apply {
            putString(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, utteranceId)
        }
        nativeTts?.speak(text, TextToSpeech.QUEUE_FLUSH, params, utteranceId)
    }

    private fun hashString(input: String): String {
        val bytes = MessageDigest.getInstance("MD5").digest(input.toByteArray())
        return bytes.joinToString("") { "%02x".format(it) }
    }

    fun stop() {
        try {
            currentSpeechJob?.cancel()
            activeMediaPlayer?.stop()
            activeMediaPlayer?.release()
            activeMediaPlayer = null
            nativeTts?.stop()
            onDuckingChanged(false)
        } catch (_: Exception) {}
    }

    fun release() {
        stop()
        nativeTts?.shutdown()
        nativeTts = null
    }
}
