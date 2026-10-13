package com.example.audio

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import android.media.MediaPlayer
import android.media.audiofx.Equalizer
import com.example.model.PitchNote
import com.example.model.ReverbType
import com.example.model.SingingMode
import com.example.model.Song
import com.example.model.VoiceProfile
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.File
import kotlin.math.PI
import kotlin.math.sin

class StemAudioEngine {

    private val sampleRate = 44100
    private val bufferSize = AudioTrack.getMinBufferSize(
        sampleRate,
        AudioFormat.CHANNEL_OUT_STEREO,
        AudioFormat.ENCODING_PCM_16BIT
    ) * 2

    // Real MP3 Player
    private var mediaPlayer: MediaPlayer? = null
    private var equalizer: Equalizer? = null

    // Real-time AI Voice Synthesizer
    private var audioTrack: AudioTrack? = null
    private var playbackJob: Job? = null
    private var positionSyncJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.Default)

    private val _playbackPositionMs = MutableStateFlow(0L)
    val playbackPositionMs: StateFlow<Long> = _playbackPositionMs.asStateFlow()

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    // Stem Mixer settings
    var bgmVolume: Float = 0.85f
        set(value) {
            field = value
            mediaPlayer?.setVolume(value, value)
        }
    var aiVoiceVolume: Float = 0.95f
    var guideVocalVolume: Float = 0.0f
    var reverbType: ReverbType = ReverbType.KARAOKE
    var singingMode: SingingMode = SingingMode.REALTIME_MIC_TRIGGER

    // Stem solo mode: "BOTH", "BGM", "VOCAL"
    var stemSoloMode: String = "BOTH"
        set(value) {
            field = value
            applyStemFilter()
        }

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

        // Initialize real MP3 playback if an audio file path exists
        releaseMediaPlayer()
        val filePath = song.audioFilePath
        if (filePath != null && File(filePath).exists()) {
            try {
                mediaPlayer = MediaPlayer().apply {
                    setDataSource(filePath)
                    setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_MEDIA)
                            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                            .build()
                    )
                    prepare()
                    setVolume(bgmVolume, bgmVolume)
                    setOnCompletionListener {
                        _isPlaying.value = false
                        _playbackPositionMs.value = 0L
                    }
                }
                initEqualizer()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun initEqualizer() {
        try {
            val mp = mediaPlayer ?: return
            equalizer = Equalizer(0, mp.audioSessionId).apply {
                enabled = true
            }
            applyStemFilter()
        } catch (_: Exception) {}
    }

    private fun applyStemFilter() {
        val eq = equalizer ?: return
        try {
            val numBands = eq.numberOfBands
            val minBandLevel = eq.bandLevelRange[0]
            val maxBandLevel = eq.bandLevelRange[1]

            when (stemSoloMode) {
                "BGM" -> {
                    // Attenuate mid vocal frequencies (~1kHz to 3kHz) to eliminate singer's dry vocal
                    for (i in 0 until numBands) {
                        val centerFreq = eq.getCenterFreq(i.toShort()) // in mHz
                        if (centerFreq in 800000..3500000) {
                            eq.setBandLevel(i.toShort(), (minBandLevel * 0.8f).toInt().toShort())
                        } else {
                            eq.setBandLevel(i.toShort(), 0)
                        }
                    }
                }
                "VOCAL" -> {
                    // Attenuate bass drums & high frequencies, isolate mid vocal
                    for (i in 0 until numBands) {
                        val centerFreq = eq.getCenterFreq(i.toShort())
                        if (centerFreq in 800000..3500000) {
                            eq.setBandLevel(i.toShort(), (maxBandLevel * 0.6f).toInt().toShort())
                        } else {
                            eq.setBandLevel(i.toShort(), (minBandLevel * 0.7f).toInt().toShort())
                        }
                    }
                }
                else -> {
                    // Normal flat response
                    for (i in 0 until numBands) {
                        eq.setBandLevel(i.toShort(), 0)
                    }
                }
            }
        } catch (_: Exception) {}
    }

    fun play() {
        if (_isPlaying.value || currentSong == null) return

        try {
            // 1. Play real MP3 if available
            val mp = mediaPlayer
            if (mp != null) {
                mp.start()
                startPositionSync()
            }

            // 2. Start AI vocal follow-along synthesizer
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

    private fun startPositionSync() {
        positionSyncJob?.cancel()
        positionSyncJob = scope.launch {
            while (isActive && _isPlaying.value) {
                val mp = mediaPlayer
                if (mp != null && mp.isPlaying) {
                    _playbackPositionMs.value = mp.currentPosition.toLong()
                }
                delay(30)
            }
        }
    }

    fun pause() {
        _isPlaying.value = false
        positionSyncJob?.cancel()
        positionSyncJob = null
        playbackJob?.cancel()
        playbackJob = null

        try {
            mediaPlayer?.pause()
        } catch (_: Exception) {}

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
        try {
            mediaPlayer?.seekTo(clamped.toInt())
        } catch (_: Exception) {}
    }

    private fun startAudioSynthesisLoop() {
        playbackJob = scope.launch {
            val song = currentSong ?: return@launch
            val chunkSamples = 1024
            val pcmBuffer = ShortArray(chunkSamples * 2)

            var phaseVocal = 0.0
            var phaseHarmonic = 0.0
            var vocalEnvelope = 0.0f

            val msPerChunk = (chunkSamples * 1000L) / sampleRate

            while (isActive && _isPlaying.value) {
                val currentMs = _playbackPositionMs.value
                if (currentMs >= song.durationMs && mediaPlayer == null) {
                    _playbackPositionMs.value = 0L
                    break
                }

                // Locate active melody note for current time
                val currentTargetNote = song.pitchNotes.find { currentMs in it.startMs..it.endMs }

                // Determine whether AI cloned vocal should sound
                val shouldSing = if (singingMode == SingingMode.FULL_VOICE_REPLACEMENT) {
                    currentTargetNote != null
                } else {
                    isMouthOpenSinging && currentTargetNote != null
                }
                val targetEnvelope = if (shouldSing) 1.0f else 0.0f
                vocalEnvelope += (targetEnvelope - vocalEnvelope) * 0.15f

                // Musical pitch calculation
                var targetFreq = currentTargetNote?.frequencyHz ?: 0f
                if (targetFreq > 0 && userSingingPitchHz > 50f && activeProfile.autoTuneStrength < 1.0f) {
                    val autoSnap = activeProfile.autoTuneStrength
                    targetFreq = targetFreq * autoSnap + userSingingPitchHz * (1f - autoSnap)
                }

                // AI Voice synthesis loop
                for (i in 0 until chunkSamples) {
                    val sampleTime = currentMs / 1000.0 + (i.toDouble() / sampleRate)
                    var vocalSample = 0.0

                    if (targetFreq > 0 && vocalEnvelope > 0.01f) {
                        val fShift = activeProfile.formantShift.toDouble()
                        val vibrato = 1.0 + (0.015 * activeProfile.vibratoDepth * sin(2 * PI * 5.5 * sampleTime))
                        val vocalPitch = targetFreq * vibrato

                        phaseVocal += (2 * PI * vocalPitch) / sampleRate
                        phaseHarmonic += (2 * PI * vocalPitch * (2.0 * fShift)) / sampleRate

                        val fundamental = sin(phaseVocal)
                        val secondHarmonic = sin(phaseHarmonic) * activeProfile.resonance.toDouble()
                        val breathNoise = (Math.random() - 0.5) * activeProfile.breathiness.toDouble()

                        val rawVocal = (fundamental * 0.7 + secondHarmonic * 0.4 + breathNoise * 0.2)
                        vocalSample = rawVocal * vocalEnvelope * aiVoiceVolume
                    }

                    // Studio Reverb DSP
                    var mixL = vocalSample
                    var mixR = vocalSample

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

                    pcmBuffer[i * 2] = (mixL.coerceIn(-1.0, 1.0) * 32767).toInt().toShort()
                    pcmBuffer[i * 2 + 1] = (mixR.coerceIn(-1.0, 1.0) * 32767).toInt().toShort()
                }

                audioTrack?.write(pcmBuffer, 0, pcmBuffer.size)

                // If no real MP3 MediaPlayer is playing, advance timeline manually
                if (mediaPlayer == null) {
                    _playbackPositionMs.value = (currentMs + msPerChunk).coerceAtMost(song.durationMs)
                }
            }

            _isPlaying.value = false
        }
    }

    private fun releaseMediaPlayer() {
        try {
            equalizer?.release()
            equalizer = null
            mediaPlayer?.stop()
            mediaPlayer?.release()
            mediaPlayer = null
        } catch (_: Exception) {}
    }

    fun release() {
        pause()
        releaseMediaPlayer()
    }
}
