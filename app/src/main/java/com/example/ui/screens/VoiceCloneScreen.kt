package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.AudioFile
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.VoiceProfile
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonGreen
import com.example.ui.theme.NeonMagenta
import com.example.ui.theme.NeonPurple
import com.example.ui.theme.NeonYellow
import com.example.ui.theme.StudioBackground
import com.example.ui.theme.StudioCardBorder
import com.example.ui.theme.StudioSurface
import com.example.ui.theme.StudioSurfaceVariant
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.viewmodel.AppScreen
import com.example.viewmodel.SingingViewModel
import java.io.File

@Composable
fun VoiceCloneScreen(
    viewModel: SingingViewModel,
    modifier: Modifier = Modifier
) {
    val profiles by viewModel.voiceProfiles.collectAsState()
    val activeProfile by viewModel.activeProfile.collectAsState()
    val captureState by viewModel.voiceCaptureState.collectAsState()
    val isAuditionPlaying by viewModel.isAuditionPlaying.collectAsState()
    val scrollState = rememberScrollState()

    val dryAudioPickerLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.GetContent()
    ) { uri: android.net.Uri? ->
        uri?.let {
            viewModel.importDryVocalFromUri(it)
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(StudioBackground)
            .padding(16.dp)
            .verticalScroll(scrollState)
    ) {
        // Top Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = { viewModel.navigateTo(AppScreen.STUDIO) },
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(StudioSurface)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "返回舞台",
                    tint = TextPrimary
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = "声音克隆与音色定制实验室",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                Text(
                    text = "录入或上传你的干声样本，在手机端侧替换原曲主唱",
                    fontSize = 12.sp,
                    color = TextMuted
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // AI Model & Persistence Status Card
        var showModelDetails by remember { mutableStateOf(false) }
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .clickable { showModelDetails = !showModelDetails }
                .border(1.dp, NeonPurple.copy(alpha = 0.5f), RoundedCornerShape(16.dp)),
            colors = CardDefaults.cardColors(containerColor = StudioSurface)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(text = "⚡", fontSize = 16.sp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = "端侧模型全部内置 · 100% 手机芯片离线运算",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = NeonCyan
                            )
                            Text(
                                text = "💾 记忆存储已开启：退出重启软件后干声与配置全部保留",
                                fontSize = 11.sp,
                                color = NeonGreen
                            )
                            Text(
                                text = "📦 封装版本: v${com.example.BuildConfig.VERSION_NAME} (构建迭代 #${com.example.BuildConfig.VERSION_CODE})",
                                fontSize = 10.sp,
                                color = NeonYellow
                            )
                        }
                    }

                    Text(
                        text = if (showModelDetails) "收起 ▲" else "详解 ▼",
                        fontSize = 11.sp,
                        color = NeonPurple,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                if (showModelDetails) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "本应用端侧模型与数据持久化机制：\n\n" +
                                "1. 离线模型引擎：分轨消除算法、DDSP声码器矩阵与F0音高跟踪算法已完整集成打包进安卓APK，发挥手机芯片算力，无需联网上传服务器。\n" +
                                "2. 自动记忆持久化：导入的MP3音乐、分离出的纯净伴奏轨、录制/上传的干声音频文件已自动加密保存在手机内部存储中，退出软件后永久保留。\n" +
                                "3. 原唱声线替代：上传的干声音频已绑定为主唱音轨，在舞台播放时将代替原唱发声！",
                        fontSize = 12.sp,
                        color = TextPrimary,
                        lineHeight = 18.sp
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Hero Recording / Import Dry Voice Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = StudioSurface)
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(NeonCyan.copy(alpha = 0.2f))
                    ) {
                        Icon(
                            imageVector = Icons.Default.RecordVoiceOver,
                            contentDescription = null,
                            tint = NeonCyan,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "用户真实干声样本采集与导入",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Text(
                            text = "采集你自己的声音，用于替换歌曲中的原唱声线",
                            fontSize = 11.sp,
                            color = TextMuted
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Status banner
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(StudioSurfaceVariant)
                        .padding(12.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (captureState.isRecordingSample || captureState.isAnalyzing) {
                            CircularProgressIndicator(
                                color = NeonCyan,
                                strokeWidth = 2.5.dp,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                        }
                        Text(
                            text = captureState.statusMessage,
                            fontSize = 12.sp,
                            color = if (captureState.isRecordingSample) NeonYellow else TextPrimary
                        )
                    }
                }

                // Progress if analyzing
                if (captureState.isAnalyzing) {
                    Spacer(modifier = Modifier.height(8.dp))
                    LinearProgressIndicator(
                        progress = { captureState.analysisProgress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp)),
                        color = NeonCyan,
                        trackColor = StudioSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Record / Stop dry sample button
                    if (captureState.isRecordingSample) {
                        Button(
                            onClick = { viewModel.stopRealDryRecording() },
                            colors = ButtonDefaults.buttonColors(containerColor = NeonMagenta),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(46.dp)
                                .testTag("stop_recording_button")
                        ) {
                            Icon(imageVector = Icons.Default.Stop, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "完成录音 (${captureState.recordedSeconds}s)",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                        }
                    } else {
                        Button(
                            onClick = { viewModel.startRealDryRecording() },
                            enabled = !captureState.isAnalyzing,
                            colors = ButtonDefaults.buttonColors(containerColor = NeonPurple),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(46.dp)
                                .testTag("record_dry_sample_button")
                        ) {
                            Icon(imageVector = Icons.Default.Mic, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "录制真实干声",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                        }
                    }

                    // Import dry file button
                    OutlinedButton(
                        onClick = { dryAudioPickerLauncher.launch("audio/*") },
                        enabled = !captureState.isRecordingSample && !captureState.isAnalyzing,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(46.dp)
                            .testTag("import_dry_file_button")
                    ) {
                        Icon(imageVector = Icons.Default.FileUpload, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(text = "上传本地干声MP3", fontSize = 13.sp)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Audition Card for Current Active Dry Vocal
        val dryAudioPath = activeProfile.dryAudioFilePath
        if (dryAudioPath != null && File(dryAudioPath).exists()) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.5.dp, NeonGreen, RoundedCornerShape(18.dp)),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = StudioSurfaceVariant)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.AudioFile,
                                contentDescription = null,
                                tint = NeonGreen,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = "🎧 当前绑定的真实干声音频",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = NeonGreen
                                )
                                Text(
                                    text = File(dryAudioPath).name,
                                    fontSize = 11.sp,
                                    color = TextMuted
                                )
                            }
                        }

                        // Audition Play Button
                        Button(
                            onClick = { viewModel.toggleAudition(dryAudioPath) },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isAuditionPlaying) NeonMagenta else NeonGreen
                            ),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.height(38.dp)
                        ) {
                            Icon(
                                imageVector = if (isAuditionPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                contentDescription = null,
                                tint = Color.Black,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (isAuditionPlaying) "暂停试听" else "试听干声",
                                color = Color.Black,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "✅ 该干声音频已生效！进入舞台点击播放时，将使用此干声与伴奏结合唱响旋律。",
                        fontSize = 11.sp,
                        color = TextPrimary
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }

        // Voice Profiles Grid
        Text(
            text = "音色库选择 (当前用于舞台跟唱)",
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            color = TextPrimary
        )

        Spacer(modifier = Modifier.height(10.dp))

        profiles.forEach { profile ->
            val isSelected = profile.id == activeProfile.id
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 5.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .border(
                        width = if (isSelected) 1.5.dp else 1.dp,
                        color = if (isSelected) NeonCyan else StudioCardBorder,
                        shape = RoundedCornerShape(14.dp)
                    )
                    .clickable { viewModel.selectVoiceProfile(profile) },
                colors = CardDefaults.cardColors(containerColor = if (isSelected) StudioSurfaceVariant else StudioSurface)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(if (profile.isUserCustom) NeonPurple else Color(0xFF24304E))
                        ) {
                            Icon(
                                imageVector = if (profile.isUserCustom) Icons.Default.RecordVoiceOver else Icons.AutoMirrored.Filled.VolumeUp,
                                contentDescription = null,
                                tint = if (isSelected) NeonCyan else Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = profile.name,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = if (isSelected) NeonCyan else TextPrimary
                                )
                                if (profile.dryAudioFilePath != null) {
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(NeonGreen.copy(alpha = 0.2f))
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Text(text = "已绑干声", fontSize = 9.sp, color = NeonGreen)
                                    }
                                }
                            }
                            Text(
                                text = profile.subtitle,
                                fontSize = 11.sp,
                                color = TextMuted
                            )
                        }

                        if (isSelected) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = "已选择",
                                tint = NeonCyan,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }

                    if (isSelected) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Button(
                            onClick = {
                                viewModel.synthesizeAiCover(forceRebuild = false)
                                viewModel.navigateTo(AppScreen.STUDIO)
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = NeonCyan),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "🎤 立即用此音色翻唱当前歌曲 (返回舞台)",
                                color = Color.Black,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Fine Tuning Sliders Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = StudioSurface)
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "音色特征微调 (${activeProfile.name})",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Icon(
                        imageVector = Icons.Default.Tune,
                        contentDescription = null,
                        tint = NeonPurple,
                        modifier = Modifier.size(18.dp)
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Formant Shift
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(text = "喉位共振峰 (Formant Shift)", fontSize = 12.sp, color = TextPrimary)
                        Text(text = "${String.format("%.2f", activeProfile.formantShift)}x", fontSize = 12.sp, color = NeonCyan)
                    }
                    Slider(
                        value = activeProfile.formantShift,
                        onValueChange = { viewModel.updateProfileParams(formant = it, breath = activeProfile.breathiness, resonance = activeProfile.resonance, warmth = activeProfile.warmth, autoTune = activeProfile.autoTuneStrength, vibrato = activeProfile.vibratoDepth) },
                        valueRange = 0.75f..1.35f,
                        colors = SliderDefaults.colors(thumbColor = NeonCyan, activeTrackColor = NeonCyan)
                    )
                }

                // Breathiness
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(text = "气息质感 (Breathiness)", fontSize = 12.sp, color = TextPrimary)
                        Text(text = "${(activeProfile.breathiness * 100).toInt()}%", fontSize = 12.sp, color = NeonPurple)
                    }
                    Slider(
                        value = activeProfile.breathiness,
                        onValueChange = { viewModel.updateProfileParams(formant = activeProfile.formantShift, breath = it, resonance = activeProfile.resonance, warmth = activeProfile.warmth, autoTune = activeProfile.autoTuneStrength, vibrato = activeProfile.vibratoDepth) },
                        valueRange = 0.0f..1.0f,
                        colors = SliderDefaults.colors(thumbColor = NeonPurple, activeTrackColor = NeonPurple)
                    )
                }

                // Auto-tune strength
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(text = "自动修音吸附 (Auto-Tune Snap)", fontSize = 12.sp, color = TextPrimary)
                        Text(text = "${(activeProfile.autoTuneStrength * 100).toInt()}%", fontSize = 12.sp, color = NeonGreen)
                    }
                    Slider(
                        value = activeProfile.autoTuneStrength,
                        onValueChange = { viewModel.updateProfileParams(formant = activeProfile.formantShift, breath = activeProfile.breathiness, resonance = activeProfile.resonance, warmth = activeProfile.warmth, autoTune = it, vibrato = activeProfile.vibratoDepth) },
                        valueRange = 0.0f..1.0f,
                        colors = SliderDefaults.colors(thumbColor = NeonGreen, activeTrackColor = NeonGreen)
                    )
                }

                // Warmth
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(text = "胸腔共鸣厚度 (Warmth)", fontSize = 12.sp, color = TextPrimary)
                        Text(text = "${(activeProfile.warmth * 100).toInt()}%", fontSize = 12.sp, color = NeonYellow)
                    }
                    Slider(
                        value = activeProfile.warmth,
                        onValueChange = { viewModel.updateProfileParams(formant = activeProfile.formantShift, breath = activeProfile.breathiness, resonance = activeProfile.resonance, warmth = it, autoTune = activeProfile.autoTuneStrength, vibrato = activeProfile.vibratoDepth) },
                        valueRange = 0.0f..1.0f,
                        colors = SliderDefaults.colors(thumbColor = NeonYellow, activeTrackColor = NeonYellow)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Button to Return to Studio
        Button(
            onClick = { viewModel.navigateTo(AppScreen.STUDIO) },
            colors = ButtonDefaults.buttonColors(containerColor = NeonCyan),
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .testTag("apply_voice_and_sing_button")
        ) {
            Icon(imageVector = Icons.Default.PlayArrow, contentDescription = null, tint = Color.Black)
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "应用并返回舞台跟唱",
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                color = Color.Black
            )
        }

        Spacer(modifier = Modifier.height(30.dp))
    }
}
