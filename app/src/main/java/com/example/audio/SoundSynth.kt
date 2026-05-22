package com.example.audio

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import android.os.Build
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlin.math.sin

object SoundSynth {
    private const val TAG = "SoundSynth"
    private val scope = CoroutineScope(Dispatchers.Default)

    /**
     * Synthesizes audio using custom sample formulas and plays it on a background thread.
     */
    private fun playSynth(durationMs: Int, sampleRate: Int = 22050, formula: (sampleRate: Int, index: Int) -> Double) {
        scope.launch {
            var audioTrack: AudioTrack? = null
            try {
                val numSamples = (sampleRate * (durationMs / 1000.0)).toInt()
                val buffer = ShortArray(numSamples)
                
                for (i in 0 until numSamples) {
                    val sample = formula(sampleRate, i).coerceIn(-1.0, 1.0)
                    buffer[i] = (sample * 32767.0).toInt().toShort()
                }

                // Create and play AudioTrack
                val minBufferSize = AudioTrack.getMinBufferSize(
                    sampleRate,
                    AudioFormat.CHANNEL_OUT_MONO,
                    AudioFormat.ENCODING_PCM_16BIT
                )
                
                val trackSize = numSamples * 2 // 2 bytes per 16-bit sample
                val bufferSize = if (trackSize > minBufferSize) trackSize else minBufferSize

                val audioAttributes = AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_GAME)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()

                val audioFormat = AudioFormat.Builder()
                    .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .setSampleRate(sampleRate)
                    .build()

                audioTrack = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    AudioTrack.Builder()
                        .setAudioAttributes(audioAttributes)
                        .setAudioFormat(audioFormat)
                        .setBufferSizeInBytes(bufferSize)
                        .setTransferMode(AudioTrack.MODE_STATIC)
                        .build()
                } else {
                    @Suppress("DEPRECATION")
                    AudioTrack(
                        AudioManager.STREAM_MUSIC,
                        sampleRate,
                        AudioFormat.CHANNEL_OUT_MONO,
                        AudioFormat.ENCODING_PCM_16BIT,
                        bufferSize,
                        AudioTrack.MODE_STATIC
                    )
                }

                if (audioTrack.state == AudioTrack.STATE_INITIALIZED) {
                    audioTrack.write(buffer, 0, numSamples)
                    audioTrack.play()
                    
                    // Allow completion
                    kotlinx.coroutines.delay(durationMs.toLong() + 100)
                } else {
                    Log.e(TAG, "AudioTrack was not initialized properly")
                }
            } catch (e: Throwable) {
                Log.e(TAG, "Audio synthesis playback error", e)
            } finally {
                try {
                    audioTrack?.stop()
                } catch (ignored: Throwable) {}
                try {
                    audioTrack?.release()
                } catch (ignored: Throwable) {}
            }
        }
    }

    /**
     * Classic emergency alarm siren (Two-tone high frequency warble)
     */
    fun playEmergencyAlarm() {
        playSynth(durationMs = 1800) { sampleRate, index ->
            val time = index.toDouble() / sampleRate
            // High/low oscillation every 0.3s
            val speedFactor = (time / 0.3).toInt()
            val baseFreq = if (speedFactor % 2 == 0) 880.0 else 660.0
            // Subtle vibrato
            val vibrato = sin(2.0 * Math.PI * 8.0 * time) * 15.0
            sin(2.0 * Math.PI * (baseFreq + vibrato) * time)
        }
    }

    /**
     * Body reported sweep (Rising fast futuristic chirp)
     */
    fun playBodyReported() {
        playSynth(durationMs = 1200) { sampleRate, index ->
            val time = index.toDouble() / sampleRate
            // Rapid pitch sweeps upward in triplets
            val sweepCycle = (time * 5.0) % 1.0
            val startFreq = 400.0
            val endFreq = 1600.0
            val currentFreq = startFreq + (endFreq - startFreq) * sweepCycle
            val volumeEnvelope = (1.0 - sweepCycle) * 0.8
            sin(2.0 * Math.PI * currentFreq * time) * volumeEnvelope
        }
    }

    /**
     * Sharp cyber kill effect (Dread snap + low rumble slide)
     */
    fun playKill() {
        playSynth(durationMs = 800) { sampleRate, index ->
            val time = index.toDouble() / sampleRate
            if (time < 0.15) {
                // Piercing snap
                val snapFreq = 2200.0 - (1600.0 * (time / 0.15))
                sin(2.0 * Math.PI * snapFreq * time) * (1.0 - (time / 0.15))
            } else {
                // Eerie sliding bass tone combined with metal clang
                val progress = (time - 0.15) / 0.65
                val sliceFreq = 450.0 - (380.0 * progress)
                val noise = (Math.random() * 2.0 - 1.0) * 0.15
                val tonal = sin(2.0 * Math.PI * sliceFreq * time)
                (tonal + noise) * (1.0 - progress) * 0.7
            }
        }
    }

    /**
     * Air vent whoosh sound (White noise sweep)
     */
    fun playVent() {
        playSynth(durationMs = 700) { sampleRate, index ->
            val time = index.toDouble() / sampleRate
            val randomFactor = Math.random() * 2.0 - 1.0
            // Low pass noise filter approximation with sweep envelope
            val envelope = sin(Math.PI * time / 0.7)
            randomFactor * envelope * 0.45
        }
    }

    /**
     * Sabotage klaxon siren (Repeating heavy low drone)
     */
    fun playSabotageSiren() {
        playSynth(durationMs = 1500) { sampleRate, index ->
            val time = index.toDouble() / sampleRate
            val repeatingTime = (time * 1.5) % 1.0
            // Slow falling heavy sawtooth drone
            val freq = 440.0 - (180.0 * repeatingTime)
            val baseWave = sin(2.0 * Math.PI * freq * time)
            // Add a sub-bass octave to make it heavy
            val subWave = sin(2.0 * Math.PI * (freq / 2.0) * time)
            val volume = sin(Math.PI * repeatingTime) * 0.7
            (baseWave * 0.5 + subWave * 0.5) * volume
        }
    }

    /**
     * High pitched digital task action complete chime
     */
    fun playTaskChime() {
        playSynth(durationMs = 600) { sampleRate, index ->
            val time = index.toDouble() / sampleRate
            // High arpeggio: C6 then E6 then G6
            val freq = when {
                time < 0.2 -> 1046.50 // C6
                time < 0.4 -> 1318.51 // E6
                else -> 1567.98 // G6
            }
            val envelope = (1.0 - (time % 0.2) / 0.2)
            sin(2.0 * Math.PI * freq * time) * envelope * 0.6
        }
    }

    /**
     * Satisfying electronic success sequence (Won game)
     */
    fun playVictory() {
        playSynth(durationMs = 1800) { sampleRate, index ->
            val time = index.toDouble() / sampleRate
            // Upward synthesizer sweep (harmonious triad)
            val basePitch = 523.25 // C5
            val currentOctave = (time / 0.45).toInt().coerceIn(0, 3)
            val ratio = when (currentOctave) {
                0 -> 1.0 // Root C
                1 -> 1.25 // Major E
                2 -> 1.5 // G
                else -> 2.0 // C Octave
            }
            val volume = 0.6 * (1.0 - (time % 0.45) / 0.45)
            sin(2.0 * Math.PI * (basePitch * ratio) * time) * volume
        }
    }

    /**
     * Dreadful minor failure chord sequence (Lost game / eject)
     */
    fun playDefeat() {
        playSynth(durationMs = 2000) { sampleRate, index ->
            val time = index.toDouble() / sampleRate
            // Low creepy dissonant minor chord
            val f1 = 196.00 // G3
            val f2 = 233.08 // Bb3 (Minor third)
            val f3 = 293.66 // D4
            val fDread = 220.00 // A3 (Adds tension cluster)
            val wave = sin(2.0 * Math.PI * f1 * time) + 
                       sin(2.0 * Math.PI * f2 * time) + 
                       sin(2.0 * Math.PI * f3 * time) + 
                       sin(2.0 * Math.PI * fDread * time)
            
            // Apply slow decay to the entire sound
            val decay = (1.0 - (time / 2.0)).coerceIn(0.0, 1.0)
            (wave / 4.0) * decay * 0.8
        }
    }
}
