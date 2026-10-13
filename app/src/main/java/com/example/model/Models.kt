package com.example.model

import androidx.compose.ui.graphics.Color

data class PitchNote(
    val startMs: Long,
    val endMs: Long,
    val midiNote: Int, // e.g. 60 = C4, 62 = D4, etc.
    val noteName: String,
    val lyric: String
) {
    val durationMs: Long get() = endMs - startMs
    val frequencyHz: Float get() = 440f * Math.pow(2.0, (midiNote - 69) / 12.0).toFloat()
}

data class LyricWord(
    val startMs: Long,
    val endMs: Long,
    val text: String,
    val midiNote: Int
)

data class LyricLine(
    val startMs: Long,
    val endMs: Long,
    val text: String,
    val words: List<LyricWord> = emptyList()
)

data class Song(
    val id: String,
    val title: String,
    val artist: String,
    val durationMs: Long,
    val bpm: Int = 100,
    val musicalKey: String = "C Major",
    val genre: String = "本地音乐",
    val primaryColor: Long = 0xFF7952EC,
    val secondaryColor: Long = 0xFF00F5D4,
    val lyrics: List<LyricLine> = emptyList(),
    val pitchNotes: List<PitchNote> = emptyList(),
    val isCustomImported: Boolean = true,
    val separationStatus: SeparationStatus = SeparationStatus.READY,
    val audioFilePath: String? = null,
    val audioUriString: String? = null,
    val bgmFilePath: String? = null,        // 纯净伴奏文件路径 (已去除原唱)
    val vocalFilePath: String? = null       // 提取的原唱人声文件路径
)

enum class SeparationStatus {
    NOT_SEPARATED,
    SEPARATING,
    READY
}

data class VoiceProfile(
    val id: String,
    val name: String,
    val subtitle: String,
    val isUserCustom: Boolean = false,
    val dryAudioFilePath: String? = null,   // 用户真实录制或上传的干声音频文件路径
    val basePitchHz: Float = 220f,
    val formantShift: Float = 1.0f,     // 0.7 (deeper) to 1.4 (brighter/sweeter)
    val breathiness: Float = 0.2f,      // 0.0 to 1.0
    val resonance: Float = 0.5f,        // 0.0 to 1.0
    val warmth: Float = 0.6f,           // 0.0 to 1.0
    val autoTuneStrength: Float = 0.85f,// 0.0 to 1.0
    val vibratoDepth: Float = 0.3f,     // 0.0 to 1.0
    val tags: List<String> = emptyList(),
    val sampleDurationSec: Int = 0
)

data class SingingSessionRecord(
    val id: String,
    val songTitle: String,
    val artist: String,
    val dateFormatted: String,
    val score: Int,
    val accuracyPercent: Int,
    val ratingGrade: String, // "SSS", "SS", "S", "A"
    val durationSec: Int,
    val voiceProfileName: String
)

enum class ReverbType(val displayName: String, val decay: Float, val wetMix: Float) {
    OFF("原声干音", 0.0f, 0.0f),
    STUDIO("录音棚", 0.35f, 0.25f),
    KARAOKE("KTV包厢", 0.65f, 0.45f),
    CONCERT("宏大演唱会", 0.90f, 0.65f)
}

enum class SingingMode(val title: String, val subtitle: String) {
    REALTIME_MIC_TRIGGER("张嘴实时跟唱", "张嘴发声时触发，用你的声音跟着唱"),
    FULL_VOICE_REPLACEMENT("全曲声线替代", "直接用你的本地声音模型替换原唱整首歌")
}
