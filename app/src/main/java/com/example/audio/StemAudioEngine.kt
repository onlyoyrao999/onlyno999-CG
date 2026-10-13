package com.example.audio

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import com.example.model.PitchNote
import com.example.model.ReverbType
import com.example.model.Song
import com.example.model.VoiceProfile
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.sin

class StemAudioEngine {

    private val sampleRate = 44100
    private val bufferSize = AudioTrack.getMinBufferSize(
        sampleRate,
        AudioFormat.CHANNEL_OUT_STEREO,
        AudioFormat.ENCODING_PCM_16BIT
    ) * 2

    private var audioTrack: AudioTrack? = null
    private var playbackJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.Default)

    private val _playbackPositionMs = MutableStateFlow(0L)
    val playbackPositionMs: StateFlow<Long> = _playbackPositionMs.asStateFlow()

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    // Stem Mixer settings
    var bgmVolume: Float = 0.85f
    var aiVoiceVolume: Float = 0.95f
    var guideVocalVolume: Float = 0.0f
    var reverbType: ReverbType = ReverbType.KARAOKE
    var singingMode: com.example.model.SingingMode = com.example.model.SingingMode.REALTIME_MIC_TRIGGER

    // Current state references
    private var currentSong: Song? = null
    private var activeProfile: VoiceProfile = VoiceProfile(id = "default", name = "默认音色", subtitle = "系统预设音色")
    private var isMouthOpenSinging: Boolean = false
    private var userSingingPitchHz: Float = 0f

    // Reverb comb filter buffers
    private val reverbBufferL = FloatArray(sampleRate / 4)
    private val reverbBufferR = FloatArray(sampleRate / 4)
    private var reverbIndex = 0

    fun updateSingingTrigger(isMouthOpen: Boolean, livePitchHz: Float) {
        this.isMouthOpenSinging = isMouthOpen
        this.userSingingPitchHz = livePitchHz
    }

    fun setVoiceProfile(profile: VoiceProfile) {
        this.activeProfile = profile
    }

    fun loadSong(song: Song) {
        pause()
        currentSong = song
        _playbackPositionMs.value = 0L
    }

    fun play() {
        if (_isPlaying.value || currentSong == null) return

        try {
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
                        .setChannelMask(AudioFormat.CHANNEL_OUT_STEREO)
                        .build()
                )
                .setBufferSizeInBytes(bufferSize)
                .setTransferMode(AudioTrack.MODE_STREAM)
                .build()

            audioTrack?.play()
            _isPlaying.value = true

            startAudioSynthesisLoop()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun pause() {
        _isPlaying.value = false
        playbackJob?.cancel()
        playbackJob = null
        try {
            audioTrack?.stop()
            audioTrack?.release()
        } catch (_: Exception) {}
        audioTrack = null
    }

    fun seekTo(positionMs: Long) {
        val song = currentSong ?: return
        val clamped = positionMs.coerceIn(0L, song.durationMs)
        _playbackPositionMs.value = clamped
    }

    private fun startAudioSynthesisLoop() {
        playbackJob = scope.launch {
            val song = currentSong ?: return@launch
            val chunkSamples = 1024 // Stereo samples per write
            val pcmBuffer = ShortArray(chunkSamples * 2)

            var phaseVocal = 0.0
            var phaseHarmonic = 0.0
            var phaseBass = 0.0
            var phaseChord = 0.0
            var vocalEnvelope = 0.0f

            val msPerChunk = (chunkSamples * 1000L) / sampleRate

            while (isActive && _isPlaying.value) {
                val currentMs = _playbackPositionMs.value
                if (currentMs >= song.durationMs) {
                    _playbackPositionMs.value = 0L
                    break
                }

                // Locate active melody note for current time
                val currentTargetNote = song.pitchNotes.find { currentMs in it.startMs..it.endMs }

                // Vocal trigger smoothing: either continuous full song replacement or real-time mic trigger
                val shouldSing = if (singingMode == com.example.model.SingingMode.FULL_VOICE_REPLACEMENT) {
                    currentTargetNote != null
                } else {
                    isMouthOpenSinging && currentTargetNote != null
                }
                val targetEnvelope = if (shouldSing) 1.0f else 0.0f
                vocalEnvelope += (targetEnvelope - vocalEnvelope) * 0.15f

                // Musical pitch calculation
                var targetFreq = currentTargetNote?.frequencyHz ?: 0f
                if (targetFreq > 0 && userSingingPitchHz > 50f && activeProfile.autoTuneStrength < 1.0f) {
                    // Blend user pitch inflection with auto-tune
                    val autoSnap = activeProfile.autoTuneStrength
                    targetFreq = targetFreq * autoSnap + userSingingPitchHz * (1f - autoSnap)
                }

                // Synthesis loop per audio sample
                for (i in 0 until chunkSamples) {
                    val sampleTime = currentMs / 1000.0 + (i.toDouble() / sampleRate)

                    // 1. Synthesize Accompaniment (BPM rhythm & chord progression)
                    val beatFreq = song.bpm / 60.0
                    val beatPhase = (sampleTime * beatFreq) % 1.0
                    // Kick drum pulse
                    val kick = if (beatPhase < 0.15) {
                        sin(2 * PI * (120.0 * (1.0 - beatPhase / 0.15))) * (1.0 - beatPhase / 0.15)
                    } else 0.0

                    // Bassline note
                    val chordIdx = ((sampleTime * beatFreq) / 4).toInt() % 4
                    val bassMidi = when (chordIdx) {
                        0 -> 36 // C2
                        1 -> 41 // F2
                        2 -> 43 // G2
                        else -> 40 // E2
                    }
                    val bassFreq = 440.0 * Math.pow(2.0, (bassMidi - 69) / 12.0)
                    phaseBass += (2 * PI * bassFreq) / sampleRate
                    val bass = (sin(phaseBass) + 0.3 * sin(phaseBass * 2)) * 0.45

                    // Soft chord pad
                    val chordFreq = bassFreq * 4.0
                    phaseChord += (2 * PI * chordFreq) / sampleRate
                    val pad = sin(phaseChord) * 0.2

                    val bgmSample = (kick * 0.5 + bass + pad) * bgmVolume

                    // 2. Synthesize AI Cloned Vocal (Shaped by user timbre profile)
                    var vocalSample = 0.0
                    if (targetFreq > 0 && vocalEnvelope > 0.01f) {
                        // Formant & Timbre harmonics
                        val fShift = activeProfile.formantShift.toDouble()
                        val vibrato = 1.0 + (0.015 * activeProfile.vibratoDepth * sin(2 * PI * 5.5 * sampleTime))
                        val vocalPitch = targetFreq * vibrato

                        phaseVocal += (2 * PI * vocalPitch) / sampleRate
                        phaseHarmonic += (2 * PI * vocalPitch * (2.0 * fShift)) / sampleRate

                        // Rich glottal pulse + formant resonance
                        val fundamental = sin(phaseVocal)
                        val secondHarmonic = sin(phaseHarmonic) * activeProfile.resonance.toDouble()
                        val breathNoise = (Math.random() - 0.5) * activeProfile.breathiness.toDouble()

                        val rawVocal = (fundamental * 0.7 + secondHarmonic * 0.4 + breathNoise * 0.2)
                        vocalSample = rawVocal * vocalEnvelope * aiVoiceVolume
                    }

                    // 3. Mix stems
                    var mixL = bgmSample + vocalSample
                    var mixR = bgmSample + vocalSample

                    // 4. Studio Reverb DSP
                    if (reverbType != ReverbType.OFF) {
                        val decay = reverbType.decay
                        val wet = reverbType.wetMix

                        val revL = reverbBufferL[reverbIndex]
                        val revR = reverbBufferR[reverbIndex]

                        reverbBufferL[reverbIndex] = ((vocalSample + revL * decay)).toFloat()
                        reverbBufferR[reverbIndex] = ((vocalSample + revR * decay)).toFloat()

                        mixL += revL * wet
                        mixR += revR * wet

                        reverbIndex = (reverbIndex + 1) % reverbBufferL.size
                    }

                    // Clamp to 16-bit PCM
                    val sampleShortL = (mixL.coerceIn(-1.0, 1.0) * 32767).toInt().toShort()
                    val sampleShortR = (mixR.coerceIn(-1.0, 1.0) * 32767).toInt().toShort()

                    pcmBuffer[i * 2] = sampleShortL
                    pcmBuffer[i * 2 + 1] = sampleShortR
                }

                audioTrack?.write(pcmBuffer, 0, pcmBuffer.size)
                _playbackPositionMs.value = (currentMs + msPerChunk).coerceAtMost(song.durationMs)
            }

            _isPlaying.value = false
        }
    }

    fun release() {
        pause()
    }
}
