package com.example.model

object SampleSongs {

    val songs: List<Song> = listOf(
        Song(
            id = "song_1",
            title = "晴空之约",
            artist = "晨曦乐队",
            durationMs = 45000L,
            bpm = 92,
            musicalKey = "C Major",
            genre = "治愈流行",
            primaryColor = 0xFF7952EC,
            secondaryColor = 0xFF00F5D4,
            lyrics = listOf(
                LyricLine(
                    startMs = 0L,
                    endMs = 4000L,
                    text = "微风轻轻吹过指尖的温度",
                    words = listOf(
                        LyricWord(0L, 800L, "微风", 60),
                        LyricWord(800L, 1600L, "轻轻", 62),
                        LyricWord(1600L, 2400L, "吹过", 64),
                        LyricWord(2400L, 3200L, "指尖", 65),
                        LyricWord(3200L, 4000L, "温度", 64)
                    )
                ),
                LyricLine(
                    startMs = 4500L,
                    endMs = 9000L,
                    text = "天空倒映着你清澈的眼眸",
                    words = listOf(
                        LyricWord(4500L, 5400L, "天空", 62),
                        LyricWord(5400L, 6300L, "倒映", 64),
                        LyricWord(6300L, 7200L, "着你", 65),
                        LyricWord(7200L, 8100L, "清澈", 67),
                        LyricWord(8100L, 9000L, "眼眸", 65)
                    )
                ),
                LyricLine(
                    startMs = 9500L,
                    endMs = 14500L,
                    text = "把所有梦想写成星光的歌",
                    words = listOf(
                        LyricWord(9500L, 10500L, "把所有", 64),
                        LyricWord(10500L, 11500L, "梦想", 65),
                        LyricWord(11500L, 12500L, "写成", 67),
                        LyricWord(12500L, 13500L, "星光", 69),
                        LyricWord(13500L, 14500L, "的歌", 67)
                    )
                ),
                LyricLine(
                    startMs = 15000L,
                    endMs = 21000L,
                    text = "陪你奔向那无尽的辽阔",
                    words = listOf(
                        LyricWord(15000L, 16200L, "陪你", 65),
                        LyricWord(16200L, 17400L, "奔向", 67),
                        LyricWord(17400L, 18600L, "那无尽", 69),
                        LyricWord(18600L, 21000L, "辽阔", 72)
                    )
                ),
                LyricLine(
                    startMs = 22000L,
                    endMs = 27000L,
                    text = "只要张开口唱出你的声音",
                    words = listOf(
                        LyricWord(22000L, 23000L, "只要", 67),
                        LyricWord(23000L, 24000L, "张开", 69),
                        LyricWord(24000L, 25000L, "口唱出", 71),
                        LyricWord(25000L, 27000L, "声音", 72)
                    )
                ),
                LyricLine(
                    startMs = 28000L,
                    endMs = 35000L,
                    text = "世界就会为你点亮黎明",
                    words = listOf(
                        LyricWord(28000L, 29200L, "世界", 71),
                        LyricWord(29200L, 30500L, "就会", 69),
                        LyricWord(30500L, 32000L, "为你", 67),
                        LyricWord(32000L, 35000L, "黎明", 65)
                    )
                ),
                LyricLine(
                    startMs = 36000L,
                    endMs = 44000L,
                    text = "啦啦啦~ 我们相约在晴空",
                    words = listOf(
                        LyricWord(36000L, 38000L, "啦啦啦", 64),
                        LyricWord(38000L, 40000L, "我们相约", 65),
                        LyricWord(40000L, 44000L, "晴空", 60)
                    )
                )
            ),
            pitchNotes = listOf(
                PitchNote(0L, 800L, 60, "C4", "微"),
                PitchNote(800L, 1600L, 62, "D4", "风"),
                PitchNote(1600L, 2400L, 64, "E4", "轻"),
                PitchNote(2400L, 3200L, 65, "F4", "指"),
                PitchNote(3200L, 4000L, 64, "E4", "温"),
                PitchNote(4500L, 5400L, 62, "D4", "天"),
                PitchNote(5400L, 6300L, 64, "E4", "倒"),
                PitchNote(6300L, 7200L, 65, "F4", "着"),
                PitchNote(7200L, 8100L, 67, "G4", "清"),
                PitchNote(8100L, 9000L, 65, "F4", "眼"),
                PitchNote(9500L, 10500L, 64, "E4", "把"),
                PitchNote(10500L, 11500L, 65, "F4", "梦"),
                PitchNote(11500L, 12500L, 67, "G4", "写"),
                PitchNote(12500L, 13500L, 69, "A4", "星"),
                PitchNote(13500L, 14500L, 67, "G4", "歌"),
                PitchNote(15000L, 16200L, 65, "F4", "陪"),
                PitchNote(16200L, 17400L, 67, "G4", "奔"),
                PitchNote(17400L, 18600L, 69, "A4", "无"),
                PitchNote(18600L, 21000L, 72, "C5", "阔"),
                PitchNote(22000L, 23000L, 67, "G4", "只"),
                PitchNote(23000L, 24000L, 69, "A4", "张"),
                PitchNote(24000L, 25000L, 71, "B4", "唱"),
                PitchNote(25000L, 27000L, 72, "C5", "声"),
                PitchNote(28000L, 29200L, 71, "B4", "世"),
                PitchNote(29200L, 30500L, 69, "A4", "就"),
                PitchNote(30500L, 32000L, 67, "G4", "为"),
                PitchNote(32000L, 35000L, 65, "F4", "明"),
                PitchNote(36000L, 38000L, 64, "E4", "啦"),
                PitchNote(38000L, 40000L, 65, "F4", "相"),
                PitchNote(40000L, 44000L, 60, "C4", "空")
            )
        ),
        Song(
            id = "song_2",
            title = "星际回声",
            artist = "赛博声波 (CyberEcho)",
            durationMs = 42000L,
            bpm = 118,
            musicalKey = "D Minor",
            genre = "电子合成流行",
            primaryColor = 0xFF00F5D4,
            secondaryColor = 0xFFFF007F,
            lyrics = listOf(
                LyricLine(
                    startMs = 0L,
                    endMs = 4000L,
                    text = "穿过霓虹闪烁的银河深处",
                    words = listOf(
                        LyricWord(0L, 1000L, "穿过", 62),
                        LyricWord(1000L, 2000L, "霓虹", 65),
                        LyricWord(2000L, 3000L, "闪烁", 67),
                        LyricWord(3000L, 4000L, "深处", 69)
                    )
                ),
                LyricLine(
                    startMs = 4500L,
                    endMs = 9500L,
                    text = "脉冲跳动着未来的信号",
                    words = listOf(
                        LyricWord(4500L, 5700L, "脉冲", 65),
                        LyricWord(5700L, 7000L, "跳动", 67),
                        LyricWord(7000L, 8200L, "未来", 69),
                        LyricWord(8200L, 9500L, "信号", 72)
                    )
                ),
                LyricLine(
                    startMs = 10000L,
                    endMs = 16000L,
                    text = "当频率共振，唤醒你我的心跳",
                    words = listOf(
                        LyricWord(10000L, 11500L, "当频率", 69),
                        LyricWord(11500L, 13000L, "共振", 72),
                        LyricWord(13000L, 14500L, "唤醒", 74),
                        LyricWord(14500L, 16000L, "心跳", 72)
                    )
                ),
                LyricLine(
                    startMs = 17000L,
                    endMs = 24000L,
                    text = "在光年之外回响，属于你的声音",
                    words = listOf(
                        LyricWord(17000L, 19000L, "在光年之外", 67),
                        LyricWord(19000L, 21000L, "回响", 69),
                        LyricWord(21000L, 24000L, "属于你声音", 62)
                    )
                )
            ),
            pitchNotes = listOf(
                PitchNote(0L, 1000L, 62, "D4", "穿"),
                PitchNote(1000L, 2000L, 65, "F4", "霓"),
                PitchNote(2000L, 3000L, 67, "G4", "闪"),
                PitchNote(3000L, 4000L, 69, "A4", "深"),
                PitchNote(4500L, 5700L, 65, "F4", "脉"),
                PitchNote(5700L, 7000L, 67, "G4", "跳"),
                PitchNote(7000L, 8200L, 69, "A4", "未"),
                PitchNote(8200L, 9500L, 72, "C5", "信"),
                PitchNote(10000L, 11500L, 69, "A4", "频"),
                PitchNote(11500L, 13000L, 72, "C5", "共"),
                PitchNote(13000L, 14500L, 74, "D5", "唤"),
                PitchNote(14500L, 16000L, 72, "C5", "心"),
                PitchNote(17000L, 19000L, 67, "G4", "光"),
                PitchNote(19000L, 21000L, 69, "A4", "回"),
                PitchNote(21000L, 24000L, 62, "D4", "声")
            )
        ),
        Song(
            id = "song_3",
            title = "江南烟雨",
            artist = "青石竹影",
            durationMs = 40000L,
            bpm = 78,
            musicalKey = "G Major",
            genre = "国风新韵",
            primaryColor = 0xFF06D6A0,
            secondaryColor = 0xFF7952EC,
            lyrics = listOf(
                LyricLine(
                    startMs = 0L,
                    endMs = 4500L,
                    text = "墨色染尽青石板上的水痕",
                    words = listOf(
                        LyricWord(0L, 1200L, "墨色", 59),
                        LyricWord(1200L, 2400L, "染尽", 62),
                        LyricWord(2400L, 3500L, "青石", 64),
                        LyricWord(3500L, 4500L, "水痕", 67)
                    )
                ),
                LyricLine(
                    startMs = 5000L,
                    endMs = 9500L,
                    text = "谁在一曲琵琶里轻叹红尘",
                    words = listOf(
                        LyricWord(5000L, 6200L, "谁在", 64),
                        LyricWord(6200L, 7400L, "一曲", 67),
                        LyricWord(7400L, 8500L, "琵琶", 69),
                        LyricWord(8500L, 9500L, "红尘", 67)
                    )
                ),
                LyricLine(
                    startMs = 10000L,
                    endMs = 15000L,
                    text = "春风拂袖，落花满身醉游人",
                    words = listOf(
                        LyricWord(10000L, 11500L, "春风", 64),
                        LyricWord(11500L, 13000L, "拂袖", 62),
                        LyricWord(13000L, 15000L, "醉游人", 59)
                    )
                )
            ),
            pitchNotes = listOf(
                PitchNote(0L, 1200L, 59, "B3", "墨"),
                PitchNote(1200L, 2400L, 62, "D4", "染"),
                PitchNote(2400L, 3500L, 64, "E4", "青"),
                PitchNote(3500L, 4500L, 67, "G4", "水"),
                PitchNote(5000L, 6200L, 64, "E4", "谁"),
                PitchNote(6200L, 7400L, 67, "G4", "一"),
                PitchNote(7400L, 8500L, 69, "A4", "琵"),
                PitchNote(8500L, 9500L, 67, "G4", "尘"),
                PitchNote(10000L, 11500L, 64, "E4", "春"),
                PitchNote(11500L, 13000L, 62, "D4", "拂"),
                PitchNote(13000L, 15000L, 59, "B3", "醉")
            )
        )
    )

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
