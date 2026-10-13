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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.RecordVoiceOver
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
import androidx.compose.ui.graphics.Brush
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

@Composable
fun VoiceCloneScreen(
    viewModel: SingingViewModel,
    modifier: Modifier = Modifier
) {
    val profiles by viewModel.voiceProfiles.collectAsState()
    val activeProfile by viewModel.activeProfile.collectAsState()
    val captureState by viewModel.voiceCaptureState.collectAsState()
    val scrollState = rememberScrollState()

    var isAuditioning by remember { mutableStateOf(false) }

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
                    text = "录入或上传你的干声样本，用你的声音实时跟唱与替换原曲",
                    fontSize = 12.sp,
                    color = TextMuted
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // AI Model Architecture & Principle Card
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
                        Text(text = "🧠", fontSize = 16.sp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = "声音转换模型架构：RVC / DDSP 神经声码器",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = NeonCyan
                            )
                            Text(
                                text = "原曲音高保持 + 本地声纹共振峰矩阵替换",
                                fontSize = 11.sp,
                                color = TextMuted
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
                        text = "本应用采用 RVC (Retrieval-based Voice Conversion) 与端侧 DDSP (可微数字信号处理) 声码器架构：\n\n" +
                                "1. 歌曲音高提取：通过 F0 跟踪器提取原歌曲人声的连续音高曲线与转音颤音；\n" +
                                "2. 本地声音建模：从你上传或录制的干声样本中，提取喉位共振峰（F1/F2）、泛音谐波包络与气息质感，构建专属本地声纹向量；\n" +
                                "3. 实时声线替代：声码器将原歌曲的旋律与歌词内容，用你的本地声线重新合成发音，从而实现【完全替代原唱】或【张嘴实时跟唱】！",
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
                            text = "用户干声样本采集",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Text(
                            text = "只需唱一句或长音'啊~'，提取共鸣峰与声纹",
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

                val context = androidx.compose.ui.platform.LocalContext.current
                val dryAudioPickerLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
                    contract = androidx.activity.result.contract.ActivityResultContracts.GetContent()
                ) { uri: android.net.Uri? ->
                    uri?.let {
                        viewModel.importDryVocalFromUri(context, it)
                    }
                }

                // Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Record dry sample button
                    Button(
                        onClick = { viewModel.recordDryVocalSample() },
                        enabled = !captureState.isRecordingSample && !captureState.isAnalyzing,
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
                            text = if (captureState.isRecordingSample) "录音中 (${captureState.recordedSeconds}s)" else "录制干声样本",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
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

        Spacer(modifier = Modifier.height(20.dp))

        // Voice Profiles Grid
        Text(
            text = "音色选择 (当前用于实时跟唱)",
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
                Row(
                    modifier = Modifier.padding(14.dp),
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
                            if (profile.isUserCustom) {
                                Spacer(modifier = Modifier.width(6.dp))
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(NeonGreen.copy(alpha = 0.2f))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(text = "我的原声", fontSize = 9.sp, color = NeonGreen)
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

                // Formant Shift (喉位/音色年龄)
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

                // Breathiness (气息质感)
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

                // Auto-tune strength (自动修音吸附)
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

                // Warmth (厚度与共鸣)
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(text = "声音厚度与胸腔共鸣 (Warmth)", fontSize = 12.sp, color = TextPrimary)
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
