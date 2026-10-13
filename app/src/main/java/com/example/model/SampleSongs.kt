package com.example.model

object SampleSongs {

    // 自带歌曲全部移除，仅使用用户自己导入的真实本地 MP3
    val songs: List<Song> = emptyList()

    val defaultProfiles: List<VoiceProfile> = listOf(
        VoiceProfile(
            id = "profile_custom_1",
            name = "我的干声克隆 (默认)",
            subtitle = "已通过麦克风干声样本分析建模",
            isUserCustom = true,
            basePitchHz = 240f,
            formantShift = 1.0f,
            breathiness = 0.25f,
            resonance = 0.60f,
            warmth = 0.70f,
            autoTuneStrength = 0.90f,
            vibratoDepth = 0.35f,
            tags = listOf("我的原声", "AI实时跟唱", "自适应音高"),
            sampleDurationSec = 12
        ),
        VoiceProfile(
            id = "profile_pop_tenor",
            name = "清澈流行少年",
            subtitle = "通透自然，现代华语流行质感",
            isUserCustom = false,
            basePitchHz = 260f,
            formantShift = 1.08f,
            breathiness = 0.18f,
            resonance = 0.55f,
            warmth = 0.50f,
            autoTuneStrength = 0.85f,
            vibratoDepth = 0.30f,
            tags = listOf("清澈", "流行男声", "高位置共鸣")
        ),
        VoiceProfile(
            id = "profile_anime_sweet",
            name = "甜美元气少女",
            subtitle = "高亮明快，二次元ACG元气音色",
            isUserCustom = false,
            basePitchHz = 330f,
            formantShift = 1.25f,
            breathiness = 0.35f,
            resonance = 0.75f,
            warmth = 0.40f,
            autoTuneStrength = 0.95f,
            vibratoDepth = 0.45f,
            tags = listOf("甜美", "ACG", "高频明亮")
        ),
        VoiceProfile(
            id = "profile_raspy_rock",
            name = "磁性烟嗓摇滚",
            subtitle = "厚实沧桑，带有微颗粒感摇滚腔",
            isUserCustom = false,
            basePitchHz = 165f,
            formantShift = 0.88f,
            breathiness = 0.40f,
            resonance = 0.80f,
            warmth = 0.85f,
            autoTuneStrength = 0.70f,
            vibratoDepth = 0.50f,
            tags = listOf("烟嗓", "低沉磁性", "摇滚力量")
        ),
        VoiceProfile(
            id = "profile_ethereal_diva",
            name = "空灵美声歌姬",
            subtitle = "开阔深远，柔和抒情空灵混响",
            isUserCustom = false,
            basePitchHz = 310f,
            formantShift = 1.12f,
            breathiness = 0.45f,
            resonance = 0.90f,
            warmth = 0.65f,
            autoTuneStrength = 0.88f,
            vibratoDepth = 0.60f,
            tags = listOf("空灵", "美声抒情", "大空间共鸣")
        )
    )
}
