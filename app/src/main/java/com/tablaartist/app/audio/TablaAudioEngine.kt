package com.tablaartist.app.audio

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.AudioTrack
import android.media.MediaRecorder
import kotlinx.coroutines.*
import kotlin.math.*

/**
 * TablaAudioEngine — High-performance real-time audio processing and synthesis
 * with CarlTune Chromatic Tuner engine, Tanpura acoustic drone, and Pitch Pipe tone generator.
 */
object TablaAudioEngine {
    private const val SAMPLE_RATE = 44100

    @Volatile private var tanpuraTrack: AudioTrack? = null
    private var tanpuraJob: Job? = null
    @Volatile var isTanpuraPlaying = false
        private set

    // CarlTune Reference Concert Pitch (A4: 430.0 - 450.0 Hz, default 440.0 Hz)
    @Volatile var concertPitchA4: Double = 440.0

    // Tabla Drum Acoustic Filtering & Sensitivity Gate
    enum class TunerFilterMode(val displayName: String, val hindiName: String, val minFreq: Float, val maxFreq: Float) {
        DAYAN("Dayan Mode", "दायाँ (Treble)", 100f, 480f),
        BAYAN("Bayan Mode", "बायाँ (Bass Dagga)", 28f, 200f),
        VOICE("Low Voice", "स्वर / खरज (Deep Vocal)", 25f, 360f),
        WIDEBAND("Wideband", "पूर्ण स्वर", 25f, 1400f)
    }

    @Volatile var filterMode: TunerFilterMode = TunerFilterMode.DAYAN
    @Volatile var noiseGateDb: Float = -46.0f

    // Tuning Pitch Detection — Hardened hardware mic lifecycle
    @Volatile private var activeAudioRecord: AudioRecord? = null
    private var recordJob: Job? = null
    @Volatile var isRecording = false
        private set

    val NOTE_NAMES = listOf("C", "C#", "D", "D#", "E", "F", "F#", "G", "G#", "A", "A#", "B")

    /**
     * Start acoustic Tanpura drone synthesis.
     * Uses phase-continuous additive harmonic synthesis inspired by VoiceStudio.
     */
    fun startTanpura(baseNote: String = "Sa", baseFrequency: Float = 138.59f /* C#3 */, scope: CoroutineScope) {
        if (isTanpuraPlaying) stopTanpura()
        isTanpuraPlaying = true

        val bufferSize = AudioTrack.getMinBufferSize(
            SAMPLE_RATE,
            AudioFormat.CHANNEL_OUT_MONO,
            AudioFormat.ENCODING_PCM_16BIT
        ) * 2

        tanpuraTrack = AudioTrack.Builder()
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build()
            )
            .setAudioFormat(
                AudioFormat.Builder()
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .setSampleRate(SAMPLE_RATE)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                    .build()
            )
            .setBufferSizeInBytes(bufferSize)
            .setTransferMode(AudioTrack.MODE_STREAM)
            .build()

        tanpuraTrack?.play()

        tanpuraJob = scope.launch(Dispatchers.Default) {
            val shortBuffer = ShortArray(1024)
            var phase1 = 0.0
            var phase2 = 0.0
            var phase3 = 0.0
            var phase4 = 0.0

            // Swaras: Pa (1.5x) or Ma (1.33x) or Ni (1.875x) + Sa (1.0x) + Sa (1.0x) + Kharaj Sa (0.5x)
            val firstStringMult = when (baseNote) {
                "Pa" -> 1.5f
                "Ma" -> 1.3333f
                "Ni" -> 1.875f
                else -> 1.5f
            }

            val freq1 = baseFrequency * firstStringMult
            val freq2 = baseFrequency
            val freq3 = baseFrequency
            val freq4 = baseFrequency * 0.5f

            val delta1 = (2.0 * Math.PI * freq1) / SAMPLE_RATE
            val delta2 = (2.0 * Math.PI * freq2) / SAMPLE_RATE
            val delta3 = (2.0 * Math.PI * freq3) / SAMPLE_RATE
            val delta4 = (2.0 * Math.PI * freq4) / SAMPLE_RATE

            while (isActive && isTanpuraPlaying) {
                for (i in shortBuffer.indices) {
                    val s1 = sin(phase1) + 0.3 * sin(2 * phase1) + 0.15 * sin(3 * phase1)
                    val s2 = sin(phase2) + 0.25 * sin(2 * phase2) + 0.1 * sin(3 * phase2)
                    val s3 = sin(phase3) + 0.25 * sin(2 * phase3) + 0.1 * sin(3 * phase3)
                    val s4 = sin(phase4) + 0.4 * sin(2 * phase4) + 0.2 * sin(3 * phase4)

                    val mixed = (s1 * 0.25 + s2 * 0.25 + s3 * 0.25 + s4 * 0.25)
                    shortBuffer[i] = (mixed * 22000.0).toInt().coerceIn(-32768, 32767).toShort()

                    phase1 = (phase1 + delta1) % (2.0 * Math.PI)
                    phase2 = (phase2 + delta2) % (2.0 * Math.PI)
                    phase3 = (phase3 + delta3) % (2.0 * Math.PI)
                    phase4 = (phase4 + delta4) % (2.0 * Math.PI)
                }
                tanpuraTrack?.write(shortBuffer, 0, shortBuffer.size)
            }
        }
    }

    fun stopTanpura() {
        isTanpuraPlaying = false
        tanpuraJob?.cancel()
        tanpuraJob = null
        try {
            tanpuraTrack?.stop()
            tanpuraTrack?.release()
        } catch (_: Exception) {}
        tanpuraTrack = null
    }

    /**
     * Pitch detection listener for Tabla Dayan/Bayan CarlTune Chromatic alignment.
     * Uses autocorrelation pitch tracking with input level dB metering.
     */
    fun startPitchDetection(
        scope: CoroutineScope,
        onPitchDetected: (note: String, cents: Int, frequencyHz: Float, octave: Int, dbLevel: Float) -> Unit
    ) {
        if (isRecording) stopPitchDetection()
        isRecording = true

        val bufferSize = max(
            AudioRecord.getMinBufferSize(
                SAMPLE_RATE,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT
            ),
            8192
        )

        recordJob = scope.launch(Dispatchers.Default) {
            var audioRecord: AudioRecord? = null
            try {
                audioRecord = AudioRecord(
                    MediaRecorder.AudioSource.MIC,
                    SAMPLE_RATE,
                    AudioFormat.CHANNEL_IN_MONO,
                    AudioFormat.ENCODING_PCM_16BIT,
                    bufferSize
                )

                if (audioRecord.state != AudioRecord.STATE_INITIALIZED) {
                    isRecording = false
                    return@launch
                }

                activeAudioRecord = audioRecord
                audioRecord.startRecording()
                val audioData = ShortArray(4096)

                while (isActive && isRecording) {
                    val read = audioRecord.read(audioData, 0, audioData.size)
                    if (read > 0) {
                        var sumSq = 0.0
                        for (i in 0 until read) {
                            val sample = audioData[i].toDouble()
                            sumSq += sample * sample
                        }
                        val rms = sqrt(sumSq / read)
                        val dbLevel = (20 * log10(rms.coerceAtLeast(1.0) / 32767.0)).toFloat().coerceIn(-60f, 0f)

                        val pitchHz = detectPitchAutocorrelation(audioData, read, SAMPLE_RATE, filterMode)
                        if (pitchHz >= filterMode.minFreq && pitchHz <= filterMode.maxFreq && dbLevel >= noiseGateDb) {
                            val (note, cents, octave) = frequencyToNoteCentsOctave(pitchHz)
                            withContext(Dispatchers.Main) {
                                onPitchDetected(note, cents, pitchHz, octave, dbLevel)
                            }
                        } else {
                            withContext(Dispatchers.Main) {
                                onPitchDetected("", 0, 0f, 0, dbLevel)
                            }
                        }
                    }
                    delay(30)
                }
            } catch (se: SecurityException) {
                isRecording = false
            } catch (_: Exception) {
                isRecording = false
            } finally {
                try {
                    audioRecord?.stop()
                    audioRecord?.release()
                } catch (_: Exception) {}
                if (activeAudioRecord === audioRecord) {
                    activeAudioRecord = null
                }
            }
        }
    }

    /**
     * Immediately stops pitch detection and releases microphone hardware.
     */
    fun stopPitchDetection() {
        isRecording = false
        recordJob?.cancel()
        recordJob = null
        val record = activeAudioRecord
        activeAudioRecord = null
        try {
            if (record != null) {
                if (record.recordingState == AudioRecord.RECORDSTATE_RECORDING) {
                    record.stop()
                }
                record.release()
            }
        } catch (_: Exception) {}
    }

    /**
     * Plays a reference tone matching CarlTune's pitch pipe feature.
     */
    fun playReferenceTone(freqHz: Float, durationMs: Long = 1200) {
        CoroutineScope(Dispatchers.Default).launch {
            try {
                val numSamples = ((SAMPLE_RATE * durationMs) / 1000).toInt()
                val buffer = ShortArray(numSamples)
                val attack = (SAMPLE_RATE * 0.04).toInt()
                val release = (SAMPLE_RATE * 0.12).toInt()

                for (i in 0 until numSamples) {
                    val t = i.toDouble() / SAMPLE_RATE
                    val env = when {
                        i < attack -> i.toDouble() / attack
                        i > numSamples - release -> (numSamples - i).toDouble() / release
                        else -> 1.0
                    }
                    val wave = sin(2.0 * Math.PI * freqHz * t) + 0.22 * sin(4.0 * Math.PI * freqHz * t)
                    buffer[i] = (wave * env * 22000.0).toInt().coerceIn(-32768, 32767).toShort()
                }

                val track = AudioTrack.Builder()
                    .setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_MEDIA)
                            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                            .build()
                    )
                    .setAudioFormat(
                        AudioFormat.Builder()
                            .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                            .setSampleRate(SAMPLE_RATE)
                            .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                            .build()
                    )
                    .setBufferSizeInBytes(buffer.size * 2)
                    .setTransferMode(AudioTrack.MODE_STATIC)
                    .build()

                track.write(buffer, 0, buffer.size)
                track.play()
                delay(durationMs + 100)
                track.release()
            } catch (_: Exception) {}
        }
    }

    /**
     * Synthesize and play low-latency authentic tabla bol stroke tone (Dha, Dhin, Tin, Ta, Na, Ge, Kat, etc.)
     */
    fun playBolStroke(bol: String) {
        CoroutineScope(Dispatchers.Default).launch {
            try {
                val b = bol.trim().lowercase()
                val isDha = b.contains("dha") || b.contains("धा")
                val isDhin = b.contains("dhin") || b.contains("धिन") || b.contains("dhi") || b.contains("धी")
                val isTaNa = b.contains("ta") || b.contains("ता") || b.contains("na") || b.contains("ना")
                val isTin = b.contains("tin") || b.contains("तिन") || b.contains("तू")
                val isBayan = b.contains("ge") || b.contains("गे") || isDha || isDhin
                val isKat = b.contains("kat") || b.contains("कत") || b.contains("ke") || b.contains("के")

                val durationMs = if (isDha || isDhin) 180 else 90
                val numSamples = (SAMPLE_RATE * durationMs) / 1000
                val buffer = ShortArray(numSamples)

                val dayanFreq = when {
                    isTaNa -> 600.0
                    isTin -> 310.0
                    isDha || isDhin -> 277.18
                    else -> 440.0
                }

                for (i in 0 until numSamples) {
                    val t = i.toDouble() / SAMPLE_RATE
                    val env = exp(-5.0 * (i.toDouble() / numSamples))
                    var wave = sin(2.0 * Math.PI * dayanFreq * t) + 0.25 * sin(4.0 * Math.PI * dayanFreq * t)

                    if (isBayan) {
                        // Bayan Meend downward slide: 110Hz -> 65Hz
                        val bayanFreq = 110.0 * (1.0 - 0.4 * (i.toDouble() / numSamples))
                        val bassEnv = exp(-3.5 * (i.toDouble() / numSamples))
                        wave += 0.9 * sin(2.0 * Math.PI * bayanFreq * t) * bassEnv
                    } else if (isKat) {
                        val slapEnv = exp(-18.0 * (i.toDouble() / numSamples))
                        wave = (sin(2.0 * Math.PI * 120.0 * t) * slapEnv)
                    }

                    buffer[i] = (wave * env * 22000.0).toInt().coerceIn(-32768, 32767).toShort()
                }

                val track = AudioTrack.Builder()
                    .setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_MEDIA)
                            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                            .build()
                    )
                    .setAudioFormat(
                        AudioFormat.Builder()
                            .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                            .setSampleRate(SAMPLE_RATE)
                            .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                            .build()
                    )
                    .setBufferSizeInBytes(buffer.size * 2)
                    .setTransferMode(AudioTrack.MODE_STATIC)
                    .build()

                track.write(buffer, 0, buffer.size)
                track.play()
                delay(durationMs.toLong() + 50)
                track.release()
            } catch (_: Exception) {}
        }
    }

    /**
     * Synthesize and play low-latency tabla stroke tone (Sam, Taali, Khali).
     */
    fun playBolSound(isSam: Boolean, isKhali: Boolean) {
        CoroutineScope(Dispatchers.Default).launch {
            try {
                val durationMs = if (isSam) 120 else 70
                val numSamples = (SAMPLE_RATE * durationMs) / 1000
                val buffer = ShortArray(numSamples)
                val freq = when {
                    isSam -> 554.37f
                    isKhali -> 138.59f
                    else -> 440.0f
                }
                val decay = if (isSam) 4.0 else 7.0

                for (i in 0 until numSamples) {
                    val t = i.toDouble() / SAMPLE_RATE
                    val envelope = exp(-decay * (i.toDouble() / numSamples))
                    val wave = sin(2.0 * Math.PI * freq * t) + 0.3 * sin(4.0 * Math.PI * freq * t)
                    buffer[i] = (wave * envelope * 24000.0).toInt().coerceIn(-32768, 32767).toShort()
                }

                val track = AudioTrack.Builder()
                    .setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_MEDIA)
                            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                            .build()
                    )
                    .setAudioFormat(
                        AudioFormat.Builder()
                            .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                            .setSampleRate(SAMPLE_RATE)
                            .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                            .build()
                    )
                    .setBufferSizeInBytes(buffer.size * 2)
                    .setTransferMode(AudioTrack.MODE_STATIC)
                    .build()

                track.write(buffer, 0, buffer.size)
                track.play()
                delay(durationMs.toLong() + 50)
                track.release()
            } catch (_: Exception) {}
        }
    }

    /**
     * Normalized Autocorrelation & MPM / YIN First-Peak Detection algorithm.
     * Accurately catches deep sub-bass frequencies and low vocal registers down to 25 Hz.
     */
    private fun detectPitchAutocorrelation(
        buffer: ShortArray,
        size: Int,
        sampleRate: Int,
        mode: TunerFilterMode = filterMode
    ): Float {
        var sumSquares = 0.0
        for (i in 0 until size) {
            val v = buffer[i].toDouble()
            sumSquares += v * v
        }
        val rms = sqrt(sumSquares / size)
        if (rms < 220.0) return -1f // Dynamic noise floor

        val minPeriod = (sampleRate / mode.maxFreq).toInt().coerceAtLeast(16)
        val maxPeriod = (sampleRate / mode.minFreq).toInt().coerceAtMost(size - 1800)

        val windowSize = 1700.coerceAtMost(size - maxPeriod)
        var sum0 = 0.0
        for (i in 0 until windowSize step 2) {
            val v = buffer[i].toDouble()
            sum0 += v * v
        }
        if (sum0 < 1e-4) return -1f

        val r = FloatArray(maxPeriod + 2)
        var globalMaxR = -1f

        for (period in minPeriod..maxPeriod) {
            var dot = 0.0
            var sumOffset = 0.0
            for (i in 0 until windowSize step 2) {
                val a = buffer[i].toDouble()
                val b = buffer[i + period].toDouble()
                dot += a * b
                sumOffset += b * b
            }
            val norm = sqrt(sum0 * sumOffset + 1e-9)
            val valR = (dot / norm).toFloat()
            r[period] = valR
            if (valR > globalMaxR) globalMaxR = valR
        }

        // McLeod / YIN peak picking: select first peak >= 80% of max and >= 0.38
        val threshold = max(0.38f, globalMaxR * 0.82f)
        var chosenPeriod = -1

        for (period in (minPeriod + 1) until maxPeriod) {
            if (r[period] > r[period - 1] && r[period] >= r[period + 1] && r[period] >= threshold) {
                chosenPeriod = period
                break
            }
        }

        if (chosenPeriod > 0) {
            val y0 = r[chosenPeriod - 1].toDouble()
            val y1 = r[chosenPeriod].toDouble()
            val y2 = r[chosenPeriod + 1].toDouble()
            val denom = 2.0 * (2.0 * y1 - y0 - y2 + 1e-9)
            val delta = (y2 - y0) / denom
            val exactPeriod = chosenPeriod + delta
            return (sampleRate / exactPeriod).toFloat()
        }

        return -1f
    }

    fun frequencyToNoteCentsOctave(freq: Float): Triple<String, Int, Int> {
        val a4 = concertPitchA4
        val midi = 69 + 12 * log2(freq.toDouble() / a4)
        val roundedMidi = round(midi).toInt()
        val cents = ((midi - roundedMidi) * 100).toInt()
        val noteIndex = (roundedMidi % 12 + 12) % 12
        val octave = (roundedMidi / 12) - 1
        return Triple(NOTE_NAMES[noteIndex], cents, octave)
    }

    fun noteToFrequency(noteName: String, octave: Int): Float {
        val idx = NOTE_NAMES.indexOf(noteName)
        if (idx < 0) return 440f
        val midi = (octave + 1) * 12 + idx
        return (concertPitchA4 * 2.0.pow((midi - 69) / 12.0)).toFloat()
    }
}
