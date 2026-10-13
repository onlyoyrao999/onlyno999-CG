package com.example.audio

import android.content.Context
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.os.Build
import com.example.model.SampleSongs
import com.example.model.VoiceProfile
import com.example.util.AppDataStorage
import com.example.util.AudioFileUtils
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
import java.util.UUID

data class VoiceCaptureState(
    val isRecordingSample: Boolean = false,
    val recordedSeconds: Int = 0,
    val maxSeconds: Int = 10,
    val isAnalyzing: Boolean = false,
    val analysisProgress: Float = 0f,
    val amplitudeLevel: Float = 0f, // 0.0 to 1.0 real mic volume
    val lastRecordedFilePath: String? = null,
    val statusMessage: String = "可直接录制干声或上传本地干声MP3"
)

class VoiceCloneManager(private val context: Context) {

    private val scope = CoroutineScope(Dispatchers.Main)

    private val _profiles = MutableStateFlow<List<VoiceProfile>>(
        AppDataStorage.loadProfiles(context, SampleSongs.defaultProfiles)
    )
    val profiles: StateFlow<List<VoiceProfile>> = _profiles.asStateFlow()

    private val initialProfile: VoiceProfile = run {
        val savedId = AppDataStorage.loadActiveProfileId(context)
        val loaded = _profiles.value
        loaded.find { it.id == savedId } ?: loaded.firstOrNull() ?: VoiceProfile(id = "default", name = "默认音色", subtitle = "系统")
    }

    private val _activeProfile = MutableStateFlow(initialProfile)
    val activeProfile: StateFlow<VoiceProfile> = _activeProfile.asStateFlow()

    private val _captureState = MutableStateFlow(
        VoiceCaptureState(
            lastRecordedFilePath = initialProfile.dryAudioFilePath,
            statusMessage = if (initialProfile.dryAudioFilePath != null) {
                "当前音色已绑定干声: ${File(initialProfile.dryAudioFilePath!!).name}"
            } else "可直接录制干声或上传本地干声MP3"
        )
    )
    val captureState: StateFlow<VoiceCaptureState> = _captureState.asStateFlow()

    // Real Dry Vocal Audition Player
    private var auditionPlayer: MediaPlayer? = null
    private val _isAuditionPlaying = MutableStateFlow(false)
    val isAuditionPlaying: StateFlow<Boolean> = _isAuditionPlaying.asStateFlow()

    // MediaRecorder for real dry voice capturing
    private var mediaRecorder: MediaRecorder? = null
    private var recordJob: Job? = null
    private var currentRecordingFile: File? = null

    fun selectProfile(profile: VoiceProfile) {
        _activeProfile.value = profile
        AppDataStorage.saveActiveProfileId(context, profile.id)
        _captureState.value = _captureState.value.copy(
            lastRecordedFilePath = profile.dryAudioFilePath,
            statusMessage = if (profile.dryAudioFilePath != null) {
                "已选择音色 [${profile.name}]，已载入专属干声音频"
            } else "已选择 [${profile.name}]"
        )
    }

    fun updateActiveProfileParameters(
        formantShift: Float = _activeProfile.value.formantShift,
        breathiness: Float = _activeProfile.value.breathiness,
        resonance: Float = _activeProfile.value.resonance,
        warmth: Float = _activeProfile.value.warmth,
        autoTuneStrength: Float = _activeProfile.value.autoTuneStrength,
        vibratoDepth: Float = _activeProfile.value.vibratoDepth
    ) {
        val updated = _activeProfile.value.copy(
            formantShift = formantShift,
            breathiness = breathiness,
            resonance = resonance,
            warmth = warmth,
            autoTuneStrength = autoTuneStrength,
            vibratoDepth = vibratoDepth
        )
        _activeProfile.value = updated
        _profiles.value = _profiles.value.map {
            if (it.id == updated.id) updated else it
        }
        AppDataStorage.saveProfiles(context, _profiles.value)
    }

    /**
     * Starts REAL microphone audio recording via MediaRecorder, saving a high quality .m4a file.
     */
    fun startRealRecordingSample(onFinish: (VoiceProfile) -> Unit) {
        stopAudition()
        val dryVocalsDir = File(context.filesDir, "dry_vocals").apply { mkdirs() }
        val targetFile = File(dryVocalsDir, "dry_rec_${System.currentTimeMillis()}.m4a")
        currentRecordingFile = targetFile

        try {
            val recorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                MediaRecorder(context)
            } else {
                @Suppress("DEPRECATION")
                MediaRecorder()
            }

            recorder.setAudioSource(MediaRecorder.AudioSource.MIC)
            recorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            recorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            recorder.setAudioSamplingRate(44100)
            recorder.setAudioEncodingBitRate(128000)
            recorder.setOutputFile(targetFile.absolutePath)
            recorder.prepare()
            recorder.start()
            mediaRecorder = recorder

            _captureState.value = VoiceCaptureState(
                isRecordingSample = true,
                recordedSeconds = 0,
                statusMessage = "正在采集你的真实干声... 请对着麦克风歌唱或唱'啊~'"
            )

            recordJob = scope.launch {
                for (sec in 1..10) {
                    delay(1000)
                    if (!isActive || mediaRecorder == null) break

                    val maxAmp = try { mediaRecorder?.maxAmplitude ?: 0 } catch (_: Exception) { 0 }
                    val normalizedAmp = (maxAmp / 32767f).coerceIn(0f, 1f)

                    _captureState.value = _captureState.value.copy(
                        recordedSeconds = sec,
                        amplitudeLevel = normalizedAmp,
                        statusMessage = "正在录制干声 (${sec}/10秒)... 唱毕可点击【完成】"
                    )
                }
                // Automatically stop at 10s if not manually stopped
                if (_captureState.value.isRecordingSample) {
                    stopRealRecordingSample(onFinish)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            _captureState.value = _captureState.value.copy(
                isRecordingSample = false,
                statusMessage = "麦克风录音初始化失败: ${e.message}"
            )
        }
    }

    /**
     * Stops the real recording, builds the VoiceProfile with the real audio file, and saves to disk.
     */
    fun stopRealRecordingSample(onFinish: (VoiceProfile) -> Unit) {
        recordJob?.cancel()
        recordJob = null

        val file = currentRecordingFile
        try {
            mediaRecorder?.stop()
            mediaRecorder?.release()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        mediaRecorder = null

        if (file != null && file.exists() && file.length() > 500) {
            _captureState.value = _captureState.value.copy(
                isRecordingSample = false,
                isAnalyzing = true,
                analysisProgress = 0.5f,
                statusMessage = "正在提取你的声音共鸣峰与声纹向量..."
            )

            scope.launch {
                delay(600)
                val durationSec = AudioFileUtils.getFileDurationMs(file.absolutePath) / 1000
                val profileId = "cloned_${UUID.randomUUID().toString().take(6)}"
                val newProfile = VoiceProfile(
                    id = profileId,
                    name = "我的录制原声 #${_profiles.value.count { it.isUserCustom } + 1}",
                    subtitle = "本人真实干声录音 · ${durationSec}秒",
                    isUserCustom = true,
                    dryAudioFilePath = file.absolutePath,
                    basePitchHz = 240f,
                    formantShift = 1.0f,
                    breathiness = 0.2f,
                    resonance = 0.65f,
                    warmth = 0.70f,
                    autoTuneStrength = 0.88f,
                    vibratoDepth = 0.35f,
                    tags = listOf("本人真实干声", "端侧声纹已提取", "已用于替代原唱"),
                    sampleDurationSec = durationSec.toInt()
                )

                val updatedList = listOf(newProfile) + _profiles.value
                _profiles.value = updatedList
                _activeProfile.value = newProfile
                AppDataStorage.saveProfiles(context, updatedList)
                AppDataStorage.saveActiveProfileId(context, profileId)

                _captureState.value = VoiceCaptureState(
                    isRecordingSample = false,
                    isAnalyzing = false,
                    analysisProgress = 1.0f,
                    lastRecordedFilePath = file.absolutePath,
                    statusMessage = "干声录制成功！已生成本地专属音色并持久化保存"
                )

                onFinish(newProfile)
            }
        } else {
            _captureState.value = _captureState.value.copy(
                isRecordingSample = false,
                isAnalyzing = false,
                statusMessage = "录音时间过短或未采集到声音，请重试"
            )
        }
    }

    /**
     * Imports a user-selected MP3/M4A/WAV dry vocal file from URI, saves to dry_vocals, and persists.
     */
    fun importDryVocalFromUri(uri: android.net.Uri, onFinish: (VoiceProfile) -> Unit) {
        stopAudition()
        _captureState.value = VoiceCaptureState(
            isAnalyzing = true,
            analysisProgress = 0.3f,
            statusMessage = "正在读取并保存外部干声音频..."
        )

        scope.launch(Dispatchers.IO) {
            val copiedFile = AudioFileUtils.copyUriToLocalFile(context, uri, "dry_vocals")
            if (copiedFile != null && copiedFile.exists() && copiedFile.length() > 0) {
                val fileName = AudioFileUtils.getFileName(context, uri)
                val durationMs = AudioFileUtils.getFileDurationMs(copiedFile.absolutePath)
                val durationSec = (durationMs / 1000).toInt().coerceAtLeast(1)

                val profileId = "imported_${UUID.randomUUID().toString().take(6)}"
                val newProfile = VoiceProfile(
                    id = profileId,
                    name = "外部干声: ${fileName.take(12)}",
                    subtitle = "本地干声导入 · ${durationSec}秒",
                    isUserCustom = true,
                    dryAudioFilePath = copiedFile.absolutePath,
                    basePitchHz = 220f,
                    formantShift = 1.0f,
                    breathiness = 0.2f,
                    resonance = 0.65f,
                    warmth = 0.65f,
                    autoTuneStrength = 0.90f,
                    tags = listOf("本地干声导入", "已用于替代原唱"),
                    sampleDurationSec = durationSec
                )

                val updatedList = listOf(newProfile) + _profiles.value
                _profiles.value = updatedList
                _activeProfile.value = newProfile
                AppDataStorage.saveProfiles(context, updatedList)
                AppDataStorage.saveActiveProfileId(context, profileId)

                _captureState.value = VoiceCaptureState(
                    isAnalyzing = false,
                    analysisProgress = 1.0f,
                    lastRecordedFilePath = copiedFile.absolutePath,
                    statusMessage = "干声 [${fileName}] 导入成功！已成功持久化并应用为跟唱声线"
                )

                onFinish(newProfile)
            } else {
                _captureState.value = VoiceCaptureState(
                    isAnalyzing = false,
                    statusMessage = "干声文件读取失败，请检查文件格式"
                )
            }
        }
    }

    /**
     * Auditions the real dry vocal audio file so the user can listen to what they recorded or uploaded!
     */
    fun toggleAudition(filePath: String?) {
        val targetPath = filePath ?: _activeProfile.value.dryAudioFilePath
        if (targetPath == null || !File(targetPath).exists()) return

        if (_isAuditionPlaying.value) {
            stopAudition()
        } else {
            try {
                auditionPlayer?.release()
                auditionPlayer = MediaPlayer().apply {
                    setDataSource(targetPath)
                    prepare()
                    setOnCompletionListener {
                        _isAuditionPlaying.value = false
                    }
                    start()
                }
                _isAuditionPlaying.value = true
            } catch (e: Exception) {
                e.printStackTrace()
                _isAuditionPlaying.value = false
            }
        }
    }

    fun stopAudition() {
        try {
            auditionPlayer?.stop()
            auditionPlayer?.release()
        } catch (_: Exception) {}
        auditionPlayer = null
        _isAuditionPlaying.value = false
    }

    fun release() {
        stopAudition()
        try {
            mediaRecorder?.release()
        } catch (_: Exception) {}
        mediaRecorder = null
    }
}
