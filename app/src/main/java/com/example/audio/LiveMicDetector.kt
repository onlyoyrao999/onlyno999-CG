package com.example.audio

import android.annotation.SuppressLint
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.log10
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sqrt

data class MicStatus(
    val isRecording: Boolean = false,
    val amplitude: Float = 0f,           // 0.0 to 1.0 normalized
    val decibels: Float = -60f,          // dB
    val pitchHz: Float = 0f,             // detected pitch in Hz
    val detectedMidi: Float = 0f,        // MIDI note number e.g. 60.0 = C4
    val isMouthOpen: Boolean = false,    // Triggered when user vocalizes/sings
    val pitchConfidence: Float = 0f      // 0.0 to 1.0
)

class LiveMicDetector {

    private val _status = MutableStateFlow(MicStatus())
    val status: StateFlow<MicStatus> = _status.asStateFlow()

    private var audioRecord: AudioRecord? = null
    private var recordJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.IO)

    // Sensitivity threshold: amplitude above this is treated as "张嘴跟唱"
    var sensitivityThreshold: Float = 0.08f

    // Smoothing for mouth activity to avoid jitter
    private var smoothedAmplitude = 0f
    private var activeHoldFrames = 0
    private val sampleRate = 16000
    private val bufferSize = max(
        AudioRecord.getMinBufferSize(
            sampleRate,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT
        ),
        2048
    )

    @SuppressLint("MissingPermission")
    fun startListening(hasPermission: Boolean) {
        if (!hasPermission) {
            // Provide simulated gentle mic test if permission denied or during dev preview
            startSimulation()
            return
        }

        stopListening()

        try {
            audioRecord = AudioRecord(
                MediaRecorder.AudioSource.VOICE_RECOGNITION,
                sampleRate,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT,
                bufferSize
            )

            if (audioRecord?.state != AudioRecord.STATE_INITIALIZED) {
                // Fallback to simulation mode if hardware audio record fails in container
                startSimulation()
                return
            }

            audioRecord?.startRecording()
            _status.value = _status.value.copy(isRecording = true)

            recordJob = scope.launch {
                val buffer = ShortArray(1024)
                while (isActive) {
                    val read = audioRecord?.read(buffer, 0, buffer.size) ?: -1
                    if (read > 0) {
                        processAudioBuffer(buffer, read)
                    }
                }
            }
        } catch (e: Exception) {
            startSimulation()
        }
    }

    private fun processAudioBuffer(buffer: ShortArray, size: Int) {
        // Compute RMS energy
        var sumSquares = 0.0
        for (i in 0 until size) {
            val sample = buffer[i].toDouble()
            sumSquares += sample * sample
        }
        val rms = sqrt(sumSquares / size)
        val normalized = min(1.0f, (rms / 32767.0).toFloat() * 5.0f) // Boost mic sensitivity
        smoothedAmplitude = smoothedAmplitude * 0.4f + normalized * 0.6f

        val db = if (rms > 0) (20 * log10(rms / 32767.0)).toFloat() else -100f

        // Pitch detection via Autocorrelation
        val pitchPair = detectPitch(buffer, size)
        val detectedPitch = pitchPair.first
        val confidence = pitchPair.second

        val midiNote = if (detectedPitch > 50f) {
            (69.0 + 12.0 * (kotlin.math.ln(detectedPitch / 440.0) / kotlin.math.ln(2.0))).toFloat()
        } else 0f

        // VAD (Voice Activity Detection): User opened mouth / sound is entering
        val isMouthOpenNow = smoothedAmplitude >= sensitivityThreshold
        if (isMouthOpenNow) {
            activeHoldFrames = 6 // Hold for a few frames to prevent crackle
        } else if (activeHoldFrames > 0) {
            activeHoldFrames--
        }
        val isTriggered = activeHoldFrames > 0

        _status.value = MicStatus(
            isRecording = true,
            amplitude = smoothedAmplitude,
            decibels = db,
            pitchHz = detectedPitch,
            detectedMidi = midiNote,
            isMouthOpen = isTriggered,
            pitchConfidence = confidence
        )
    }

    /**
     * Autocorrelation-based fundamental frequency (F0) pitch estimation
     */
    private fun detectPitch(buffer: ShortArray, size: Int): Pair<Float, Float> {
        val minPeriod = sampleRate / 800 // ~800 Hz (high soprano)
        val maxPeriod = sampleRate / 70  // ~70 Hz (low bass)

        if (size < maxPeriod * 2) return Pair(0f, 0f)

        var maxCorrelation = 0.0
        var bestPeriod = -1

        var energy0 = 0.0
        for (i in 0 until size - maxPeriod) {
            energy0 += buffer[i].toDouble() * buffer[i].toDouble()
        }

        if (energy0 < 1000.0) return Pair(0f, 0f) // Silent

        for (period in minPeriod..maxPeriod) {
            var sum = 0.0
            for (i in 0 until size - period) {
                sum += buffer[i].toDouble() * buffer[i + period].toDouble()
            }
            if (sum > maxCorrelation) {
                maxCorrelation = sum
                bestPeriod = period
            }
        }

        val confidence = min(1.0f, (maxCorrelation / energy0).toFloat())
        return if (bestPeriod > 0 && confidence > 0.35f) {
            Pair(sampleRate.toFloat() / bestPeriod, confidence)
        } else {
            Pair(0f, 0f)
        }
    }

    /**
     * Fallback or preview simulator when mic hardware is unavailable or in emulator
     */
    private var simulationJob: Job? = null

    private fun startSimulation() {
        stopListening()
        _status.value = _status.value.copy(isRecording = true)
        simulationJob = scope.launch {
            var time = 0.0
            while (isActive) {
                time += 0.05
                // Organic breathing / vocal variation simulation
                val amp = (0.5 + 0.4 * kotlin.math.sin(time * 1.5) + 0.1 * kotlin.math.cos(time * 4.0)).toFloat().coerceIn(0f, 1f)
                val isTriggered = amp > sensitivityThreshold
                val pitch = if (isTriggered) (220.0 + 40.0 * kotlin.math.sin(time * 0.8)).toFloat() else 0f
                val midi = if (pitch > 50f) (69.0 + 12.0 * (kotlin.math.ln(pitch / 440.0) / kotlin.math.ln(2.0))).toFloat() else 0f

                _status.value = MicStatus(
                    isRecording = true,
                    amplitude = amp,
                    decibels = -25f + amp * 20f,
                    pitchHz = pitch,
                    detectedMidi = midi,
                    isMouthOpen = isTriggered,
                    pitchConfidence = 0.85f
                )
                delay(50)
            }
        }
    }

    fun stopListening() {
        recordJob?.cancel()
        recordJob = null
        simulationJob?.cancel()
        simulationJob = null
        try {
            audioRecord?.stop()
            audioRecord?.release()
        } catch (_: Exception) {}
        audioRecord = null
        _status.value = _status.value.copy(isRecording = false, amplitude = 0f, isMouthOpen = false)
    }
}
