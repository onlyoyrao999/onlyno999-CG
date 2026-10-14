package com.example.viewmodel

import android.app.Application
import android.os.VibrationEffect
import android.os.Vibrator
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.audio.LiveMicDetector
import com.example.audio.MicStatus
import com.example.model.ReverbType
import com.example.audio.StemAudioEngine
import com.example.audio.StemSeparator
import com.example.audio.VoiceCloneManager
import com.example.model.PitchNote
import com.example.model.SingingSessionRecord
import com.example.model.Song
import com.example.model.VoiceProfile
import com.example.util.AppDataStorage
import com.example.util.AudioFileUtils
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID
import kotlin.math.abs

enum class AppScreen {
    STUDIO,          // 实时跟唱舞台
    STEM_SEPARATION, // 伴奏与干声分离
    VOICE_CLONE,     // 声音克隆与音色库
    RECORDINGS       // 我的演唱录音
}

data class ScoreState(
    val currentScore: Int = 0,
    val comboCount: Int = 0,
    val maxCombo: Int = 0,
    val lastHitGrade: String = "", // "PERFECT", "GREAT", "GOOD", "MISSED"
    val accuracyPercent: Int = 100,
    val totalNotesEvaluated: Int = 0,
    val hitNotesCount: Int = 0
)

class SingingViewModel(application: Application) : AndroidViewModel(application) {

    private val vibrator = application.getSystemService(Vibrator::class.java)

    // Engines initialized with Application context
    val audioEngine = StemAudioEngine(application)
    val micDetector = LiveMicDetector()
    val voiceCloneManager = VoiceCloneManager(application)
    val stemSeparator = StemSeparator(application)
    val voiceConversionEngine = audioEngine.conversionEngine
    val conversionState = voiceConversionEngine.conversionState

    // Screen navigation
    private val _currentScreen = MutableStateFlow(AppScreen.STUDIO)
    val currentScreen: StateFlow<AppScreen> = _currentScreen.asStateFlow()

    // Persistent Songs
    private val _songs = MutableStateFlow<List<Song>>(AppDataStorage.loadSongs(application))
    val songs: StateFlow<List<Song>> = _songs.asStateFlow()

    private val initialSong: Song? = run {
        val savedSongId = AppDataStorage.loadActiveSongId(application)
        val loaded = _songs.value
        loaded.find { it.id == savedSongId } ?: loaded.firstOrNull()
    }

    private val _selectedSong = MutableStateFlow<Song?>(initialSong)
    val selectedSong: StateFlow<Song?> = _selectedSong.asStateFlow()

    // Recorded sessions with persistence
    private val _recordedSessions = MutableStateFlow<List<SingingSessionRecord>>(AppDataStorage.loadSessions(application))
    val recordedSessions: StateFlow<List<SingingSessionRecord>> = _recordedSessions.asStateFlow()

    // Singing Mode: FULL_VOICE_REPLACEMENT vs REALTIME_MIC_TRIGGER
    private val _singingMode = MutableStateFlow(com.example.model.SingingMode.FULL_VOICE_REPLACEMENT)
    val singingMode: StateFlow<com.example.model.SingingMode> = _singingMode.asStateFlow()

    fun setSingingMode(mode: com.example.model.SingingMode) {
        _singingMode.value = mode
        audioEngine.singingMode = mode
    }

    fun setStemSoloMode(mode: String) {
        audioEngine.stemSoloMode = mode
    }

    // Recording session state
    private val _isRecordingSession = MutableStateFlow(false)
    val isRecordingSession: StateFlow<Boolean> = _isRecordingSession.asStateFlow()

    // Score state
    private val _scoreState = MutableStateFlow(ScoreState())
    val scoreState: StateFlow<ScoreState> = _scoreState.asStateFlow()

    // Expose engine flows
    val playbackPositionMs = audioEngine.playbackPositionMs
    val isPlaying = audioEngine.isPlaying
    val isMp3Ready = audioEngine.isMp3Ready
    val playbackErrorMessage = audioEngine.errorMessage
    val micStatus = micDetector.status
    val voiceProfiles = voiceCloneManager.profiles
    val activeProfile = voiceCloneManager.activeProfile
    val voiceCaptureState = voiceCloneManager.captureState
    val isAuditionPlaying = voiceCloneManager.isAuditionPlaying
    val separationState = stemSeparator.processState

    // Track evaluated note IDs to avoid double-scoring same note
    private val evaluatedNotes = mutableSetOf<Long>()

    init {
        _selectedSong.value?.let { audioEngine.loadSong(it) }
        audioEngine.setVoiceProfile(activeProfile.value)

        // 1. Observe voice profile changes ONLY when profile actually updates
        viewModelScope.launch {
            activeProfile.collect { profile ->
                audioEngine.setVoiceProfile(profile)
            }
        }

        // 2. Observe mic changes for voice trigger
        viewModelScope.launch {
            micStatus.collect { mic ->
                audioEngine.updateSingingTrigger(mic.isMouthOpen, mic.pitchHz)
            }
        }

        // 3. Evaluate real-time scoring on playback progress
        viewModelScope.launch {
            combine(playbackPositionMs, micStatus) { pos, mic ->
                Pair(pos, mic)
            }.collect { (pos, mic) ->
                evaluateSingingScore(pos, mic)
            }
        }
    }

    fun navigateTo(screen: AppScreen) {
        _currentScreen.value = screen
    }

    fun selectSong(song: Song) {
        _selectedSong.value = song
        AppDataStorage.saveActiveSongId(getApplication(), song.id)
        audioEngine.loadSong(song)
        resetScore()
    }

    fun togglePlayPause() {
        if (isPlaying.value) {
            audioEngine.pause()
        } else {
            audioEngine.play()
        }
    }

    fun seekTo(posMs: Long) {
        audioEngine.seekTo(posMs)
    }

    fun restartSong() {
        audioEngine.seekTo(0L)
        audioEngine.play()
        resetScore()
    }

    fun startMic(hasPermission: Boolean) {
        micDetector.startListening(hasPermission)
    }

    fun stopMic() {
        micDetector.stopListening()
    }

    fun setMicSensitivity(threshold: Float) {
        micDetector.sensitivityThreshold = threshold
    }

    // Stem Mixer controls
    fun setBgmVolume(volume: Float) {
        audioEngine.bgmVolume = volume
    }

    fun setAiVoiceVolume(volume: Float) {
        audioEngine.aiVoiceVolume = volume
    }

    fun setGuideVocalVolume(volume: Float) {
        audioEngine.guideVocalVolume = volume
    }

    fun setReverbType(reverb: ReverbType) {
        audioEngine.reverbType = reverb
    }

    // Voice Profile Actions
    fun selectVoiceProfile(profile: VoiceProfile) {
        voiceCloneManager.selectProfile(profile)
        audioEngine.setVoiceProfile(profile)
        synthesizeAiCover(forceRebuild = false)
    }

    fun updateProfileParams(
        formant: Float,
        breath: Float,
        resonance: Float,
        warmth: Float,
        autoTune: Float,
        vibrato: Float
    ) {
        voiceCloneManager.updateActiveProfileParameters(
            formantShift = formant,
            breathiness = breath,
            resonance = resonance,
            warmth = warmth,
            autoTuneStrength = autoTune,
            vibratoDepth = vibrato
        )
        synthesizeAiCover(forceRebuild = true)
    }

    // Real Dry Vocal Recording
    fun startRealDryRecording() {
        voiceCloneManager.startRealRecordingSample { newProfile ->
            audioEngine.setVoiceProfile(newProfile)
            synthesizeAiCover(forceRebuild = true)
        }
    }

    fun stopRealDryRecording() {
        voiceCloneManager.stopRealRecordingSample { newProfile ->
            audioEngine.setVoiceProfile(newProfile)
            synthesizeAiCover(forceRebuild = true)
        }
    }

    // Dry Vocal Audio File Import
    fun importDryVocalFromUri(uri: android.net.Uri) {
        voiceCloneManager.importDryVocalFromUri(uri) { newProfile ->
            audioEngine.setVoiceProfile(newProfile)
            synthesizeAiCover(forceRebuild = true)
        }
    }

    // Dry Vocal Audition
    fun toggleAudition(filePath: String? = null) {
        voiceCloneManager.toggleAudition(filePath)
    }

    fun stopAudition() {
        voiceCloneManager.stopAudition()
    }

    // AI Singing Voice Conversion: transforms song vocal into the user's uploaded voice timbre
    fun synthesizeAiCover(forceRebuild: Boolean = true) {
        val song = _selectedSong.value ?: return
        val profile = voiceCloneManager.activeProfile.value
        audioEngine.setVoiceProfile(profile)
        viewModelScope.launch {
            voiceConversionEngine.convertSongVoice(song, profile, forceRebuild)
            audioEngine.reloadAiCoverPlayer()
        }
    }

    // Stem Separation action: imports MP3, executes center cancellation, persists
    fun importAndSeparateLocalAudio(context: android.content.Context, uri: android.net.Uri) {
        viewModelScope.launch {
            val fileName = AudioFileUtils.getFileName(context, uri)
            val artist = AudioFileUtils.getAudioArtist(context, uri)
            val durationMs = AudioFileUtils.getAudioDurationMs(context, uri)

            // Copy file to permanent storage
            val localFile = AudioFileUtils.copyUriToLocalFile(context, uri, "songs")
            val filePath = localFile?.absolutePath

            val newSong = stemSeparator.separateSong(
                songTitle = fileName,
                artistName = artist,
                durationMs = durationMs,
                audioFilePath = filePath,
                audioUriString = uri.toString()
            )

            val updated = listOf(newSong) + _songs.value.filter { it.id != newSong.id }
            _songs.value = updated
            AppDataStorage.saveSongs(getApplication(), updated)
            selectSong(newSong)
        }
    }

    fun separateSong(title: String, artist: String = "本地提取", durationMs: Long = 45000L) {
        viewModelScope.launch {
            val newSong = stemSeparator.separateSong(
                songTitle = title,
                artistName = artist,
                durationMs = durationMs
            )
            val updated = listOf(newSong) + _songs.value.filter { it.id != newSong.id }
            _songs.value = updated
            AppDataStorage.saveSongs(getApplication(), updated)
            selectSong(newSong)
        }
    }

    fun deleteSong(song: Song) {
        val updated = _songs.value.filter { it.id != song.id }
        _songs.value = updated
        AppDataStorage.saveSongs(getApplication(), updated)
        if (_selectedSong.value?.id == song.id) {
            val next = updated.firstOrNull()
            _selectedSong.value = next
            AppDataStorage.saveActiveSongId(getApplication(), next?.id)
            if (next != null) audioEngine.loadSong(next) else audioEngine.pause()
        }
    }

    // Session recording
    fun toggleSessionRecording() {
        if (_isRecordingSession.value) {
            _isRecordingSession.value = false
            saveCurrentSession()
        } else {
            _isRecordingSession.value = true
            resetScore()
            if (!isPlaying.value) {
                audioEngine.play()
            }
        }
    }

    private fun saveCurrentSession() {
        val song = _selectedSong.value ?: return
        val score = _scoreState.value
        val grade = when {
            score.currentScore >= 2000 -> "SSS"
            score.currentScore >= 1200 -> "SS"
            score.currentScore >= 600 -> "S"
            else -> "A"
        }
        val dateFormat = SimpleDateFormat("MM-dd HH:mm", Locale.getDefault())
        val record = SingingSessionRecord(
            id = UUID.randomUUID().toString(),
            songTitle = song.title,
            artist = song.artist,
            dateFormatted = dateFormat.format(Date()),
            score = score.currentScore,
            accuracyPercent = score.accuracyPercent,
            ratingGrade = grade,
            durationSec = (audioEngine.playbackPositionMs.value / 1000).toInt(),
            voiceProfileName = activeProfile.value.name
        )
        val updated = listOf(record) + _recordedSessions.value
        _recordedSessions.value = updated
        AppDataStorage.saveSessions(getApplication(), updated)
    }

    private fun resetScore() {
        evaluatedNotes.clear()
        _scoreState.value = ScoreState()
    }

    private fun evaluateSingingScore(posMs: Long, mic: MicStatus) {
        if (!isPlaying.value) return

        val song = _selectedSong.value ?: return
        val activeNote = song.pitchNotes.find { posMs in it.startMs..it.endMs } ?: return

        val midNoteTime = (activeNote.startMs + activeNote.endMs) / 2
        if (abs(posMs - midNoteTime) < 180 && !evaluatedNotes.contains(activeNote.startMs)) {
            evaluatedNotes.add(activeNote.startMs)

            val isSinging = mic.isMouthOpen
            val pitchDelta = if (mic.pitchHz > 50f) abs(mic.detectedMidi - activeNote.midiNote) else 99f

            val grade: String
            val points: Int
            val isHit: Boolean

            when {
                isSinging && pitchDelta <= 1.5f -> {
                    grade = "PERFECT"
                    points = 100
                    isHit = true
                    vibrateFeedback(40)
                }
                isSinging && pitchDelta <= 3.0f -> {
                    grade = "GREAT"
                    points = 70
                    isHit = true
                    vibrateFeedback(25)
                }
                isSinging -> {
                    grade = "GOOD"
                    points = 40
                    isHit = true
                }
                else -> {
                    grade = "MISSED"
                    points = 0
                    isHit = false
                }
            }

            val cur = _scoreState.value
            val newScore = cur.currentScore + points
            val newCombo = if (isHit) cur.comboCount + 1 else 0
            val maxCombo = maxOf(cur.maxCombo, newCombo)
            val newTotalEvaluated = cur.totalNotesEvaluated + 1
            val newHitCount = if (isHit) cur.hitNotesCount + 1 else cur.hitNotesCount
            val accuracy = if (newTotalEvaluated > 0) ((newHitCount.toDouble() / newTotalEvaluated) * 100).toInt() else 100

            _scoreState.value = ScoreState(
                currentScore = newScore,
                comboCount = newCombo,
                maxCombo = maxCombo,
                lastHitGrade = grade,
                accuracyPercent = accuracy,
                totalNotesEvaluated = newTotalEvaluated,
                hitNotesCount = newHitCount
            )
        }
    }

    private fun vibrateFeedback(durationMs: Long) {
        try {
            vibrator?.vibrate(VibrationEffect.createOneShot(durationMs, VibrationEffect.DEFAULT_AMPLITUDE))
        } catch (_: Exception) {}
    }

    override fun onCleared() {
        super.onCleared()
        audioEngine.release()
        micDetector.stopListening()
        voiceCloneManager.release()
    }
}
