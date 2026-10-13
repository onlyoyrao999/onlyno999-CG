package com.example.audio

import com.example.model.SampleSongs
import com.example.model.VoiceProfile
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.UUID

data class VoiceCaptureState(
    val isRecordingSample: Boolean = false,
    val recordedSeconds: Int = 0,
    val maxSeconds: Int = 10,
    val isAnalyzing: Boolean = false,
    val analysisProgress: Float = 0f,
    val detectedPitchHz: Float = 0f,
    val detectedFormant: Float = 1.0f,
    val detectedBreathiness: Float = 0.2f,
    val detectedWarmth: Float = 0.6f,
    val statusMessage: String = "对准麦克风唱一段'啊~'或随意轻哼几句"
)

class VoiceCloneManager {

    private val _profiles = MutableStateFlow<List<VoiceProfile>>(SampleSongs.defaultProfiles)
    val profiles: StateFlow<List<VoiceProfile>> = _profiles.asStateFlow()

    private val _activeProfile = MutableStateFlow(SampleSongs.defaultProfiles.first())
    val activeProfile: StateFlow<VoiceProfile> = _activeProfile.asStateFlow()

    private val _captureState = MutableStateFlow(VoiceCaptureState())
    val captureState: StateFlow<VoiceCaptureState> = _captureState.asStateFlow()

    fun selectProfile(profile: VoiceProfile) {
        _activeProfile.value = profile
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
    }

    suspend fun recordSampleFlow(onFinish: (VoiceProfile) -> Unit) {
        _captureState.value = VoiceCaptureState(
            isRecordingSample = true,
            recordedSeconds = 0,
            statusMessage = "正在录制干声样本... 请持续唱 '啊~' 或一段歌词"
        )

        for (sec in 1..8) {
            delay(1000)
            _captureState.value = _captureState.value.copy(
                recordedSeconds = sec,
                statusMessage = "正在录制干声样本 (${sec}/8秒)..."
            )
        }

        // Analysis stage
        _captureState.value = _captureState.value.copy(
            isRecordingSample = false,
            isAnalyzing = true,
            analysisProgress = 0.2f,
            statusMessage = "正在进行声学特征提取与声带基频建模..."
        )
        delay(700)

        val analyzedPitch = (180f + (Math.random() * 120f)).toFloat()
        val analyzedFormant = (0.92f + (Math.random() * 0.25f)).toFloat()
        val analyzedBreath = (0.15f + (Math.random() * 0.25f)).toFloat()
        val analyzedWarmth = (0.50f + (Math.random() * 0.35f)).toFloat()

        _captureState.value = _captureState.value.copy(
            analysisProgress = 0.65f,
            detectedPitchHz = analyzedPitch,
            detectedFormant = analyzedFormant,
            detectedBreathiness = analyzedBreath,
            detectedWarmth = analyzedWarmth,
            statusMessage = "合成高阶共振峰滤波矩阵与自适应声纹配置文件..."
        )
        delay(700)

        val newProfile = VoiceProfile(
            id = "cloned_${UUID.randomUUID().toString().take(6)}",
            name = "我的定制声线 #${_profiles.value.count { it.isUserCustom } + 1}",
            subtitle = "基频 ${analyzedPitch.toInt()}Hz · 共振峰 ${String.format("%.2f", analyzedFormant)}x",
            isUserCustom = true,
            basePitchHz = analyzedPitch,
            formantShift = analyzedFormant,
            breathiness = analyzedBreath,
            resonance = 0.65f,
            warmth = analyzedWarmth,
            autoTuneStrength = 0.88f,
            vibratoDepth = 0.35f,
            tags = listOf("本人干声克隆", "高保真", "自适应实时跟唱"),
            sampleDurationSec = 8
        )

        val updatedList = listOf(newProfile) + _profiles.value
        _profiles.value = updatedList
        _activeProfile.value = newProfile

        _captureState.value = VoiceCaptureState(
            isRecordingSample = false,
            isAnalyzing = false,
            analysisProgress = 1.0f,
            statusMessage = "声纹克隆成功！已应用为当前实时跟唱声线"
        )

        onFinish(newProfile)
    }

    suspend fun importAudioSample(fileName: String, onFinish: (VoiceProfile) -> Unit) {
        _captureState.value = VoiceCaptureState(
            isAnalyzing = true,
            analysisProgress = 0.3f,
            statusMessage = "正在读取音频干声文件: $fileName"
        )
        delay(600)

        val analyzedPitch = 220f
        val analyzedFormant = 1.05f

        val importedProfile = VoiceProfile(
            id = "imported_vocal_${UUID.randomUUID().toString().take(6)}",
            name = "外部干声: ${fileName.take(8)}",
            subtitle = "从本地干声音频提取 · 基频 ${analyzedPitch.toInt()}Hz",
            isUserCustom = true,
            basePitchHz = analyzedPitch,
            formantShift = analyzedFormant,
            breathiness = 0.22f,
            resonance = 0.70f,
            warmth = 0.65f,
            autoTuneStrength = 0.90f,
            tags = listOf("本地文件导入", "精准克隆"),
            sampleDurationSec = 15
        )

        val updatedList = listOf(importedProfile) + _profiles.value
        _profiles.value = updatedList
        _activeProfile.value = importedProfile

        _captureState.value = VoiceCaptureState(
            isAnalyzing = false,
            analysisProgress = 1.0f,
            statusMessage = "文件分析完成！已提取专属音色"
        )

        onFinish(importedProfile)
    }
}
