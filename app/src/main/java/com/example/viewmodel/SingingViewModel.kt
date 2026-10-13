package com.example.viewmodel

import android.app.Application
import android.os.VibrationEffect
import android.os.Vibrator
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.audio.LiveMicDetector
import com.example.audio.MicStatus
import com.example.model.ReverbType
import com.example.audio.SeparationProcessState
import com.example.audio.StemAudioEngine
import com.example.audio.StemSeparator
import com.example.audio.VoiceCaptureState
import com.example.audio.VoiceCloneManager
import com.example.model.PitchNote
import com.example.model.SampleSongs
import com.example.model.SingingSessionRecord
import com.example.model.Song
import com.example.model.VoiceProfile
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
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

    // Engines
    val audioEngine = StemAudioEngine()
    val micDetector = LiveMicDetector()
    val voiceCloneManager = VoiceCloneManager()
    val stemSeparator = StemSeparator()

    // Screen navigation
    private val _currentScreen = MutableStateFlow(AppScreen.STUDIO)
    val currentScreen: StateFlow<AppScreen> = _currentScreen.asStateFlow()

    // Songs
    private val _songs = MutableStateFlow<List<Song>>(SampleSongs.songs)
    val songs: StateFlow<List<Song>> = _songs.asStateFlow()

    private val _selectedSong = MutableStateFlow<Song>(SampleSongs.songs.first())
    val selectedSong: StateFlow<Song> = _selectedSong.asStateFlow()

    // Recorded sessions
    private val _recordedSessions = MutableStateFlow<List<SingingSessionRecord>>(emptyList())
    val recordedSessions: StateFlow<List<SingingSessionRecord>> = _recordedSessions.asStateFlow()

    // Singing Mode: REALTIME_MIC_TRIGGER vs FULL_VOICE_REPLACEMENT
    private val _singingMode = MutableStateFlow(com.example.model.SingingMode.REALTIME_MIC_TRIGGER)
    val singingMode: StateFlow<com.example.model.SingingMode> = _singingMode.asStateFlow()

    fun setSingingMode(mode: com.example.model.SingingMode) {
        _singingMode.value = mode
        audioEngine.singingMode = mode
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
    val micStatus = micDetector.status
    val voiceProfiles = voiceCloneManager.profiles
    val activeProfile = voiceCloneManager.activeProfile
    val voiceCaptureState = voiceCloneManager.captureState
    val separationState = stemSeparator.processState

    // Track evaluated note IDs to avoid double-scoring same note
    private val evaluatedNotes = mutableSetOf<Long>()

    init {
        audioEngine.loadSong(_selectedSong.value)
        audioEngine.setVoiceProfile(activeProfile.value)

        // Observe mic changes and forward to audio synthesis engine in real-time
        viewModelScope.launch {
            combine(micStatus, activeProfile, playbackPositionMs) { mic, profile, pos ->
                Triple(mic, profile, pos)
            }.collect { (mic, profile, pos) ->
                audioEngine.updateSingingTrigger(mic.isMouthOpen, mic.pitchHz)
                audioEngine.setVoiceProfile(profile)

                // Evaluate real-time pitch accuracy against current target note
                evaluateSingingScore(pos, mic)
            }
        }
    }

    fun navigateTo(screen: AppScreen) {
        _currentScreen.value = screen
    }

    fun selectSong(song: Song) {
        _selectedSong.value = song
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

    // Voice Clone actions
    fun selectVoiceProfile(profile: VoiceProfile) {
        voiceCloneManager.selectProfile(profile)
        audioEngine.setVoiceProfile(profile)
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
    }

    fun recordDryVocalSample() {
        viewModelScope.launch {
            voiceCloneManager.recordSampleFlow { newProfile ->
                audioEngine.setVoiceProfile(newProfile)
            }
        }
    }

    fun importDryVocalFile(fileName: String) {
        viewModelScope.launch {
            voiceCloneManager.importAudioSample(fileName) { newProfile ->
                audioEngine.setVoiceProfile(newProfile)
            }
        }
    }

    fun importDryVocalFromUri(context: android.content.Context, uri: android.net.Uri) {
        val fileName = com.example.util.AudioFileUtils.getFileName(context, uri)
        importDryVocalFile(fileName)
    }

    // Stem Separation actions
    fun separateSong(title: String, artist: String = "本地提取", durationMs: Long = 40000L) {
        viewModelScope.launch {
            val newSong = stemSeparator.separateSong(title, artist, durationMs)
            val updated = listOf(newSong) + _songs.value
            _songs.value = updated
            selectSong(newSong)
        }
    }

    fun importAndSeparateLocalAudio(context: android.content.Context, uri: android.net.Uri) {
        val fileName = com.example.util.AudioFileUtils.getFileName(context, uri)
        val artist = com.example.util.AudioFileUtils.getAudioArtist(context, uri)
        val durationMs = com.example.util.AudioFileUtils.getAudioDurationMs(context, uri)
        separateSong(fileName, artist, durationMs)
    }

    // Session recording
    fun toggleSessionRecording() {
        if (_isRecordingSession.value) {
            // Stop and save
            _isRecordingSession.value = false
            saveCurrentSession()
        } else {
            // Start recording
            _isRecordingSession.value = true
            resetScore()
            if (!isPlaying.value) {
                audioEngine.play()
            }
        }
    }

    private fun saveCurrentSession() {
        val song = _selectedSong.value
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
        _recordedSessions.value = listOf(record) + _recordedSessions.value
    }

    private fun resetScore() {
        evaluatedNotes.clear()
        _scoreState.value = ScoreState()
    }

    private fun evaluateSingingScore(posMs: Long, mic: MicStatus) {
        if (!isPlaying.value) return

        val song = _selectedSong.value
        val activeNote = song.pitchNotes.find { posMs in it.startMs..it.endMs } ?: return

        // Evaluate around middle of note
        val midNoteTime = (activeNote.startMs + activeNote.endMs) / 2
        if (abs(posMs - midNoteTime) < 150 && !evaluatedNotes.contains(activeNote.startMs)) {
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
                    triggerHaptic()
                }
                isSinging && pitchDelta <= 3.0f -> {
                    grade = "GREAT"
                    points = 70
                    isHit = true
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

            val current = _scoreState.value
            val newHits = if (isHit) current.hitNotesCount + 1 else current.hitNotesCount
            val newTotal = current.totalNotesEvaluated + 1
            val newCombo = if (isHit) current.comboCount + 1 else 0
            val maxCombo = maxOf(current.maxCombo, newCombo)
            val accuracy = if (newTotal > 0) (newHits * 100) / newTotal else 100

            _scoreState.value = current.copy(
                currentScore = current.currentScore + points + (newCombo * 2),
                comboCount = newCombo,
                maxCombo = maxCombo,
                lastHitGrade = grade,
                accuracyPercent = accuracy,
                totalNotesEvaluated = newTotal,
                hitNotesCount = newHits
            )
        }
    }

    private fun triggerHaptic() {
        try {
            vibrator?.vibrate(VibrationEffect.createOneShot(30, VibrationEffect.DEFAULT_AMPLITUDE))
        } catch (_: Exception) {}
    }

    override fun onCleared() {
        super.onCleared()
        audioEngine.release()
        micDetector.stopListening()
    }
}
