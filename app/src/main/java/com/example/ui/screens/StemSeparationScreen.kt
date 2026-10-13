package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Canvas
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
import androidx.compose.material.icons.filled.AudioFile
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SettingsVoice
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
fun StemSeparationScreen(
    viewModel: SingingViewModel,
    modifier: Modifier = Modifier
) {
    val separationState by viewModel.separationState.collectAsState()
    val selectedSong by viewModel.selectedSong.collectAsState()
    val scrollState = rememberScrollState()

    var inputSongTitle by remember { mutableStateOf("夜空星辉 (本地MP3)") }
    var soloStem by remember { mutableStateOf<String?>("BOTH") } // "BGM", "VOCAL", "BOTH"

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(StudioBackground)
            .padding(16.dp)
            .verticalScroll(scrollState)
    ) {
        // Top Bar
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
                    text = "MP3伴奏与干声分离工坊",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                Text(
                    text = "自动提取歌曲背景声与人声干声，为跟唱建立分轨",
                    fontSize = 12.sp,
                    color = TextMuted
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Explanation Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = StudioSurface)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.GraphicEq,
                        contentDescription = null,
                        tint = NeonCyan,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "智能分轨原理",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = NeonCyan
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "系统采用中心人声反相消除与时频掩膜算法，自动将MP3分离为两路独立音轨：① 纯净背景伴奏 (BGM) ② 原唱人声干声 (用于音高引导与旋律对齐)。分离后即可张嘴实时跟唱！",
                    fontSize = 12.sp,
                    color = TextMuted,
                    lineHeight = 18.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Local MP3 / Audio File Upload Card
        val context = androidx.compose.ui.platform.LocalContext.current
        var selectedAudioUri by remember { mutableStateOf<android.net.Uri?>(null) }
        var selectedAudioName by remember { mutableStateOf<String?>(null) }
        var selectedAudioDurationMs by remember { mutableStateOf<Long?>(null) }

        val audioPickerLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
            contract = androidx.activity.result.contract.ActivityResultContracts.GetContent()
        ) { uri: android.net.Uri? ->
            uri?.let {
                selectedAudioUri = it
                val name = com.example.util.AudioFileUtils.getFileName(context, it)
                val duration = com.example.util.AudioFileUtils.getAudioDurationMs(context, it)
                selectedAudioName = name
                selectedAudioDurationMs = duration
                inputSongTitle = name
                viewModel.importAndSeparateLocalAudio(context, it)
            }
        }

        // 1. Prominent Local MP3 Upload Section
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .border(
                    width = 1.5.dp,
                    color = if (selectedAudioUri != null) NeonCyan else NeonPurple,
                    shape = RoundedCornerShape(18.dp)
                )
                .clickable {
                    audioPickerLauncher.launch("audio/*")
                }
                .testTag("upload_local_mp3_dropzone"),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (selectedAudioUri != null) StudioSurfaceVariant else StudioSurface
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.radialGradient(
                                listOf(NeonPurple.copy(alpha = 0.35f), StudioSurfaceVariant)
                            )
                        )
                ) {
                    Icon(
                        imageVector = Icons.Default.AudioFile,
                        contentDescription = "选择本地MP3",
                        tint = NeonCyan,
                        modifier = Modifier.size(28.dp)
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = if (selectedAudioName != null) "已选择音频: $selectedAudioName" else "点击上传手机本地 MP3 / 音频文件",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (selectedAudioName != null) NeonCyan else TextPrimary
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = if (selectedAudioDurationMs != null) "时长约 ${(selectedAudioDurationMs!! / 1000)} 秒 · 点击可重新选择" else "支持 MP3, WAV, FLAC, M4A, AAC · 自动分离干声与背景声",
                    fontSize = 11.sp,
                    color = TextMuted
                )

                Spacer(modifier = Modifier.height(12.dp))

                Button(
                    onClick = { audioPickerLauncher.launch("audio/*") },
                    colors = ButtonDefaults.buttonColors(containerColor = NeonPurple),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp)
                        .testTag("select_local_mp3_file_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.AudioFile,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (selectedAudioName != null) "更换其他本地音频" else "浏览本地文件上传 MP3",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 2. Custom Title / Manual input or Preset Separation
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = StudioSurface)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "快速分轨 / 自定义分离",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )

                Spacer(modifier = Modifier.height(10.dp))

                TextField(
                    value = inputSongTitle,
                    onValueChange = { inputSongTitle = it },
                    label = { Text("音频曲名 / 文件名", color = TextMuted) },
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = StudioSurfaceVariant,
                        unfocusedContainerColor = StudioSurfaceVariant,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        focusedIndicatorColor = NeonPurple,
                        unfocusedIndicatorColor = Color.Transparent
                    ),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.height(12.dp))

                Button(
                    onClick = { viewModel.separateSong(inputSongTitle) },
                    enabled = !separationState.isProcessing,
                    colors = ButtonDefaults.buttonColors(containerColor = NeonCyan),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(46.dp)
                        .testTag("start_separation_button")
                ) {
                    if (separationState.isProcessing) {
                        CircularProgressIndicator(
                            color = Color.Black,
                            strokeWidth = 2.dp,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = "正在智能分离分轨中...", color = Color.Black, fontWeight = FontWeight.Bold)
                    } else {
                        Icon(imageVector = Icons.Default.GraphicEq, contentDescription = null, tint = Color.Black)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = "开始一键分离伴奏与干声", fontWeight = FontWeight.Bold, color = Color.Black)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Separation Progress & Status
        if (separationState.isProcessing || separationState.progress > 0f) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = StudioSurface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (separationState.isProcessing) "分轨处理中" else "分轨完成",
                            fontWeight = FontWeight.Bold,
                            color = if (separationState.isProcessing) NeonYellow else NeonGreen,
                            fontSize = 14.sp
                        )
                        Text(
                            text = "${(separationState.progress * 100).toInt()}%",
                            fontWeight = FontWeight.Bold,
                            color = NeonCyan,
                            fontSize = 14.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    LinearProgressIndicator(
                        progress = { separationState.progress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp)),
                        color = NeonCyan,
                        trackColor = StudioSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = separationState.currentStage,
                        fontSize = 12.sp,
                        color = TextMuted
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }

        // Stem Tracks Display
        Text(
            text = "已分离独立音轨",
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            color = TextPrimary
        )

        Spacer(modifier = Modifier.height(10.dp))

        // Stem 1: Background Music (伴奏轨)
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .border(
                    width = 1.dp,
                    color = if (soloStem == "BGM") NeonPurple else StudioCardBorder,
                    shape = RoundedCornerShape(16.dp)
                ),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = StudioSurface)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.MusicNote,
                            contentDescription = null,
                            tint = NeonPurple,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = "音轨 1: 背景伴奏 (Instrumental BGM)",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = TextPrimary
                            )
                            Text(
                                text = "鼓组 · 贝斯 · 和弦乐器 (已消除原唱人声)",
                                fontSize = 11.sp,
                                color = TextMuted
                            )
                        }
                    }

                    OutlinedButton(
                        onClick = {
                            soloStem = if (soloStem == "BGM") "BOTH" else "BGM"
                            viewModel.setBgmVolume(1.0f)
                            viewModel.setAiVoiceVolume(if (soloStem == "BGM") 0.0f else 0.9f)
                        },
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = if (soloStem == "BGM") "独奏中" else "仅听伴奏",
                            fontSize = 11.sp,
                            color = if (soloStem == "BGM") NeonPurple else TextPrimary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Simulated Waveform for BGM
                Canvas(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(36.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(StudioSurfaceVariant)
                ) {
                    val w = size.width
                    val h = size.height
                    val barCount = 48
                    val barWidth = w / (barCount * 1.5f)
                    for (i in 0 until barCount) {
                        val barH = (h * (0.3f + 0.6f * kotlin.math.abs(kotlin.math.sin(i * 0.45f)))).coerceIn(4f, h)
                        drawRoundRect(
                            color = NeonPurple.copy(alpha = 0.8f),
                            topLeft = Offset(i * (barWidth * 1.5f), (h - barH) / 2),
                            size = androidx.compose.ui.geometry.Size(barWidth, barH)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Stem 2: Dry Vocal (人声干声轨)
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .border(
                    width = 1.dp,
                    color = if (soloStem == "VOCAL") NeonCyan else StudioCardBorder,
                    shape = RoundedCornerShape(16.dp)
                ),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = StudioSurface)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.SettingsVoice,
                            contentDescription = null,
                            tint = NeonCyan,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = "音轨 2: 人声干声 (Isolated Dry Vocal)",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = TextPrimary
                            )
                            Text(
                                text = "基频轮廓提取完成 · 旋律与节拍已标定",
                                fontSize = 11.sp,
                                color = TextMuted
                            )
                        }
                    }

                    OutlinedButton(
                        onClick = {
                            soloStem = if (soloStem == "VOCAL") "BOTH" else "VOCAL"
                            viewModel.setBgmVolume(if (soloStem == "VOCAL") 0.0f else 0.85f)
                            viewModel.setAiVoiceVolume(1.0f)
                        },
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = if (soloStem == "VOCAL") "独奏中" else "仅听干声",
                            fontSize = 11.sp,
                            color = if (soloStem == "VOCAL") NeonCyan else TextPrimary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Simulated Waveform for Vocal
                Canvas(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(36.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(StudioSurfaceVariant)
                ) {
                    val w = size.width
                    val h = size.height
                    val barCount = 48
                    val barWidth = w / (barCount * 1.5f)
                    for (i in 0 until barCount) {
                        val barH = (h * (0.2f + 0.7f * kotlin.math.abs(kotlin.math.cos(i * 0.35f)))).coerceIn(4f, h)
                        drawRoundRect(
                            color = NeonCyan.copy(alpha = 0.8f),
                            topLeft = Offset(i * (barWidth * 1.5f), (h - barH) / 2),
                            size = androidx.compose.ui.geometry.Size(barWidth, barH)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Navigate to Studio and Sing button
        Button(
            onClick = {
                viewModel.navigateTo(AppScreen.STUDIO)
                viewModel.restartSong()
            },
            colors = ButtonDefaults.buttonColors(containerColor = NeonGreen),
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .testTag("apply_stems_sing_button")
        ) {
            Icon(
                imageVector = Icons.Default.PlayArrow,
                contentDescription = null,
                tint = Color.Black
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "以此分轨立即开始AI实时跟唱",
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                color = Color.Black
            )
        }

        Spacer(modifier = Modifier.height(30.dp))
    }
}
