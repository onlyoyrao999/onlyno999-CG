package com.example.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
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
import java.io.FileInputStream

class StemAudioEngine(private val context: Context) {

    private val scope = CoroutineScope(Dispatchers.Main)

    // Primary BGM Accompaniment Player
    private var bgmPlayer: MediaPlayer? = null

    // User Dry Vocal Player (Plays uploaded dry MP3 or recorded dry vocal)
    private var userVocalPlayer: MediaPlayer? = null

    // Vocal Guide / Extracted Vocal Player (if listening to original vocal stem)
    private var guideVocalPlayer: MediaPlayer? = null

    private var positionSyncJob: Job? = null

    private val _playbackPositionMs = MutableStateFlow(0L)
    val playbackPositionMs: StateFlow<Long> = _playbackPositionMs.asStateFlow()

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _isMp3Ready = MutableStateFlow(false)
    val isMp3Ready: StateFlow<Boolean> = _isMp3Ready.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    // Stem Mixer settings
    var bgmVolume: Float = 0.90f
        set(value) {
            field = value
            applyVolumes()
        }

    var aiVoiceVolume: Float = 1.0f
        set(value) {
            field = value
            applyVolumes()
        }

    var guideVocalVolume: Float = 0.0f
        set(value) {
            field = value
            applyVolumes()
        }

    var reverbType: ReverbType = ReverbType.KARAOKE

    var singingMode: SingingMode = SingingMode.FULL_VOICE_REPLACEMENT
        set(value) {
            field = value
            applyVolumes()
            if (_isPlaying.value) {
                ensureUserVocalPlaying()
            }
        }

    // Stem solo mode: "BOTH", "BGM", "VOCAL"
    var stemSoloMode: String = "BOTH"
        set(value) {
            field = value
            applyVolumes()
        }

    // Current state references
    private var currentSong: Song? = null
    private var activeProfile: VoiceProfile = VoiceProfile(id = "default", name = "默认音色", subtitle = "系统预设")
    private var isMouthOpenSinging: Boolean = false
    private var userSingingPitchHz: Float = 0f

    fun updateSingingTrigger(isMouthOpen: Boolean, livePitchHz: Float) {
        this.isMouthOpenSinging = isMouthOpen
        this.userSingingPitchHz = livePitchHz

        if (!_isPlaying.value) return

        val uvp = userVocalPlayer ?: return
        try {
            if (singingMode == SingingMode.REALTIME_MIC_TRIGGER) {
                if (isMouthOpen) {
                    uvp.setVolume(aiVoiceVolume, aiVoiceVolume)
                    if (!uvp.isPlaying) {
                        uvp.start()
                    }
                } else {
                    // Duck volume smoothly when mouth is not open
                    uvp.setVolume(0.15f * aiVoiceVolume, 0.15f * aiVoiceVolume)
                }
            } else {
                // In Full Voice Replacement mode, always full volume
                uvp.setVolume(aiVoiceVolume, aiVoiceVolume)
                if (!uvp.isPlaying) {
                    uvp.start()
                }
            }
        } catch (_: Exception) {}
    }

    /**
     * Updates active voice profile. Only re-initializes userVocalPlayer if the file path or ID actually changed!
     */
    fun setVoiceProfile(profile: VoiceProfile) {
        val pathChanged = this.activeProfile.dryAudioFilePath != profile.dryAudioFilePath
        val idChanged = this.activeProfile.id != profile.id
        this.activeProfile = profile

        if (pathChanged || idChanged || userVocalPlayer == null) {
            setupUserVocalPlayer()
        }
    }

    /**
     * Loads the song. Automatically selects the cleanest audio:
     * Prefers clean separated BGM file (with lead vocals eliminated via phase cancellation),
     * or falls back to original audioFilePath.
     */
    fun loadSong(song: Song) {
        pause()
        currentSong = song
        _playbackPositionMs.value = 0L
        _isMp3Ready.value = false
        _errorMessage.value = null

        releasePlayers()

        // 1. Determine Accompaniment (BGM) file
        val targetBgmPath = when {
            song.bgmFilePath != null && File(song.bgmFilePath).exists() -> song.bgmFilePath
            song.audioFilePath != null && File(song.audioFilePath).exists() -> song.audioFilePath
            else -> null
        }

        if (targetBgmPath != null) {
            try {
                val file = File(targetBgmPath)
                val fis = FileInputStream(file)
                bgmPlayer = MediaPlayer().apply {
                    setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_MEDIA)
                            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                            .build()
                    )
                    setDataSource(fis.fd)
                    fis.close()
                    prepare()
                    setOnCompletionListener {
                        _isPlaying.value = false
                        _playbackPositionMs.value = 0L
                    }
                }
                _isMp3Ready.value = true
            } catch (e: Exception) {
                e.printStackTrace()
                _errorMessage.value = "伴奏加载失败: ${e.message}"
            }
        }

        // 2. Guide vocal stem player (if isolated original vocal exists)
        val targetVocalPath = song.vocalFilePath
        if (targetVocalPath != null && File(targetVocalPath).exists()) {
            try {
                val file = File(targetVocalPath)
                val fis = FileInputStream(file)
                guideVocalPlayer = MediaPlayer().apply {
                    setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_MEDIA)
                            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                            .build()
                    )
                    setDataSource(fis.fd)
                    fis.close()
                    prepare()
                }
            } catch (_: Exception) {}
        }

        // 3. Setup User dry vocal player
        setupUserVocalPlayer()
        applyVolumes()
    }

    /**
     * Prepares user's uploaded or recorded dry vocal track to sing alongside BGM.
     */
    private fun setupUserVocalPlayer() {
        val dryPath = activeProfile.dryAudioFilePath
        if (dryPath != null && File(dryPath).exists()) {
            try {
                userVocalPlayer?.stop()
                userVocalPlayer?.release()
                val file = File(dryPath)
                val fis = FileInputStream(file)
                userVocalPlayer = MediaPlayer().apply {
                    setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_MEDIA)
                            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                            .build()
                    )
                    setDataSource(fis.fd)
                    fis.close()
                    isLooping = true // Loop dry vocal sample to continuously accompany the melody
                    prepare()
                    if (_isPlaying.value) {
                        ensureUserVocalPlaying()
                    }
                }
                applyVolumes()
            } catch (e: Exception) {
                e.printStackTrace()
                userVocalPlayer = null
            }
        } else {
            userVocalPlayer?.stop()
            userVocalPlayer?.release()
            userVocalPlayer = null
        }
    }

    private fun ensureUserVocalPlaying() {
        val uvp = userVocalPlayer ?: return
        val bp = bgmPlayer
        try {
            val currentPos = bp?.currentPosition ?: 0
            val dryDuration = uvp.duration.coerceAtLeast(1)
            uvp.seekTo(currentPos % dryDuration)
            uvp.start()
            applyVolumes()
        } catch (_: Exception) {}
    }

    private fun applyVolumes() {
        val effectiveBgmVol = when (stemSoloMode) {
            "VOCAL" -> 0f
            else -> bgmVolume
        }

        val effectiveAiVoiceVol = when (stemSoloMode) {
            "BGM" -> 0f
            else -> if (singingMode == SingingMode.REALTIME_MIC_TRIGGER && !isMouthOpenSinging) {
                0.15f * aiVoiceVolume
            } else {
                aiVoiceVolume
            }
        }

        val effectiveGuideVol = when (stemSoloMode) {
            "BGM" -> 0f
            else -> guideVocalVolume
        }

        try {
            bgmPlayer?.setVolume(effectiveBgmVol, effectiveBgmVol)
            userVocalPlayer?.setVolume(effectiveAiVoiceVol, effectiveAiVoiceVol)
            guideVocalPlayer?.setVolume(effectiveGuideVol, effectiveGuideVol)
        } catch (_: Exception) {}
    }

    fun play() {
        if (_isPlaying.value || currentSong == null) return

        try {
            val bp = bgmPlayer
            if (bp != null) {
                bp.start()
                startPositionSync()
            }

            // Sync and start user dry vocal player
            val uvp = userVocalPlayer
            if (uvp != null) {
                val currentPos = bp?.currentPosition ?: 0
                val dryDuration = uvp.duration.coerceAtLeast(1)
                uvp.seekTo(currentPos % dryDuration)
                uvp.start()
            }

            // Start guide vocal if active
            val gvp = guideVocalPlayer
            if (gvp != null && guideVocalVolume > 0.05f) {
                gvp.seekTo(bp?.currentPosition ?: 0)
                gvp.start()
            }

            _isPlaying.value = true
            applyVolumes()
        } catch (e: Exception) {
            e.printStackTrace()
            _errorMessage.value = "播放出错: ${e.message}"
        }
    }

    fun pause() {
        _isPlaying.value = false
        positionSyncJob?.cancel()
        positionSyncJob = null

        try {
            bgmPlayer?.pause()
        } catch (_: Exception) {}

        try {
            userVocalPlayer?.pause()
        } catch (_: Exception) {}

        try {
            guideVocalPlayer?.pause()
        } catch (_: Exception) {}
    }

    fun seekTo(positionMs: Long) {
        val song = currentSong ?: return
        val clamped = positionMs.coerceIn(0L, song.durationMs)
        _playbackPositionMs.value = clamped
        try {
            bgmPlayer?.seekTo(clamped.toInt())
            val uvp = userVocalPlayer
            if (uvp != null) {
                val dur = uvp.duration.coerceAtLeast(1)
                uvp.seekTo((clamped % dur).toInt())
            }
            guideVocalPlayer?.seekTo(clamped.toInt())
        } catch (_: Exception) {}
    }

    private fun startPositionSync() {
        positionSyncJob?.cancel()
        positionSyncJob = scope.launch {
            while (isActive && _isPlaying.value) {
                val bp = bgmPlayer
                if (bp != null && bp.isPlaying) {
                    _playbackPositionMs.value = bp.currentPosition.toLong()
                }
                delay(40)
            }
        }
    }

    private fun releasePlayers() {
        positionSyncJob?.cancel()
        positionSyncJob = null
        try {
            bgmPlayer?.stop()
            bgmPlayer?.release()
        } catch (_: Exception) {}
        bgmPlayer = null

        try {
            userVocalPlayer?.stop()
            userVocalPlayer?.release()
        } catch (_: Exception) {}
        userVocalPlayer = null

        try {
            guideVocalPlayer?.stop()
            guideVocalPlayer?.release()
        } catch (_: Exception) {}
        guideVocalPlayer = null
    }

    fun release() {
        pause()
        releasePlayers()
    }
}
