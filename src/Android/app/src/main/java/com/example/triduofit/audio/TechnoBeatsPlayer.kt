package com.example.triduofit.audio

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.sin

class TechnoBeatsPlayer {

    private val sampleRate = 44100
    private var audioTrack: AudioTrack? = null
    private var playbackJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.Default)

    @Volatile
    private var isPlaying = false

    @Volatile
    private var targetVolume = 0.8f

    @Volatile
    private var currentVolume = 0.8f

    @Volatile
    var bpm: Int = 135

    fun start() {
        if (isPlaying) return
        isPlaying = true

        val bufferSize = AudioTrack.getMinBufferSize(
            sampleRate,
            AudioFormat.CHANNEL_OUT_MONO,
            AudioFormat.ENCODING_PCM_16BIT
        ) * 2

        audioTrack = AudioTrack.Builder()
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build()
            )
            .setAudioFormat(
                AudioFormat.Builder()
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .setSampleRate(sampleRate)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                    .build()
            )
            .setBufferSizeInBytes(bufferSize)
            .setTransferMode(AudioTrack.MODE_STREAM)
            .build()

        audioTrack?.play()

        playbackJob = scope.launch {
            generateTechnoLoop()
        }
    }

    private fun generateTechnoLoop() {
        val track = audioTrack ?: return

        var beatCounter = 0
        while (isPlaying && scope.isActive) {
            val beatDurationSec = 60.0 / bpm
            val samplesPerBeat = (sampleRate * beatDurationSec).toInt()
            val shortBuffer = ShortArray(samplesPerBeat)

            // Détermine le type de son sur cette pulsation (4/4 techno beat)
            val isKickBeat = true // Kick puissant sur chaque temps (four-on-the-floor)
            val isSnareBeat = (beatCounter % 2 == 1) // Clap/Snare sur temps 2 et 4
            val hiHatOffset = samplesPerBeat / 2 // Hi-hat sur les contre-temps (off-beat)

            for (i in 0 until samplesPerBeat) {
                // Lissage du volume (Audio Ducking smooth fade)
                currentVolume += (targetVolume - currentVolume) * 0.002f

                var sample = 0.0
                val t = i.toDouble() / sampleRate

                // 1. Synthèse du Kick Techno (descente de fréquence rapide 150Hz -> 45Hz)
                if (isKickBeat && t < 0.22) {
                    val kickFreq = 45.0 + 105.0 * exp(-t * 28.0)
                    val kickEnv = exp(-t * 12.0)
                    sample += sin(2.0 * PI * kickFreq * t) * kickEnv * 0.85
                }

                // 2. Off-beat Open Hi-Hat (bruit blanc filtré métallique)
                if (i >= hiHatOffset) {
                    val tHat = (i - hiHatOffset).toDouble() / sampleRate
                    if (tHat < 0.12) {
                        val noise = (Math.random() * 2.0 - 1.0)
                        val hatEnv = exp(-tHat * 35.0)
                        sample += noise * hatEnv * 0.35
                    }
                }

                // 3. Clap / Snare sur 2 et 4
                if (isSnareBeat && t < 0.18) {
                    val snareNoise = (Math.random() * 2.0 - 1.0) * exp(-t * 22.0)
                    val snareTone = sin(2.0 * PI * 210.0 * t) * exp(-t * 30.0)
                    sample += (snareNoise * 0.4 + snareTone * 0.3)
                }

                // 4. Ligne de basse techno roulante (Rolling bassline 16th notes)
                val sixteenthIndex = (i.toDouble() / samplesPerBeat * 4).toInt()
                val tSixteenth = (i % (samplesPerBeat / 4)).toDouble() / sampleRate
                if (tSixteenth < 0.12) {
                    val bassFreq = if (sixteenthIndex % 2 == 0) 55.0 else 65.41 // A1 -> C2
                    val bassEnv = exp(-tSixteenth * 18.0)
                    sample += sin(2.0 * PI * bassFreq * tSixteenth) * bassEnv * 0.45
                }

                // Application du volume avec saturation douce
                val finalSample = (sample * currentVolume).coerceIn(-1.0, 1.0)
                shortBuffer[i] = (finalSample * 32767.0).toInt().toShort()
            }

            track.write(shortBuffer, 0, shortBuffer.size)
            beatCounter = (beatCounter + 1) % 4
        }
    }

    fun setDucking(ducked: Boolean) {
        // En ducking, la musique baisse à 20%, sinon elle tourne à 80%
        targetVolume = if (ducked) 0.18f else 0.80f
    }

    fun stop() {
        isPlaying = false
        playbackJob?.cancel()
        playbackJob = null
        try {
            audioTrack?.stop()
            audioTrack?.release()
            audioTrack = null
        } catch (_: Exception) {}
    }
}
