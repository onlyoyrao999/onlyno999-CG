package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AudioFile
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.FiberManualRecord
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.LyricLine
import com.example.model.Song
import com.example.ui.components.MouthActivityIndicator
import com.example.ui.components.PitchVisualizer
import com.example.ui.components.StemMixerBar
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
import com.example.ui.theme.TextSecondary
import com.example.viewmodel.AppScreen
import com.example.viewmodel.SingingViewModel

@Composable
fun KaraokeStudioScreen(
    viewModel: SingingViewModel,
    hasRecordAudioPermission: Boolean,
    onRequestPermission: () -> Unit,
    modifier: Modifier = Modifier
) {
    val songs by viewModel.songs.collectAsState()
    val selectedSong by viewModel.selectedSong.collectAsState()
    val playbackPosMs by viewModel.playbackPositionMs.collectAsState()
    val isPlaying by viewModel.isPlaying.collectAsState()
    val micStatus by viewModel.micStatus.collectAsState()
    val activeProfile by viewModel.activeProfile.collectAsState()
    val scoreState by viewModel.scoreState.collectAsState()
    val isRecordingSession by viewModel.isRecordingSession.collectAsState()
    val profiles by viewModel.voiceCloneManager.profiles.collectAsState()
    val conversionState by viewModel.conversionState.collectAsState()

    var showSongPicker by remember { mutableStateOf(false) }
    var showVoicePicker by remember { mutableStateOf(false) }
    var showMixerPanel by remember { mutableStateOf(false) }
    var stemSoloMode by remember { mutableStateOf(viewModel.audioEngine.stemSoloMode) }

    val context = androidx.compose.ui.platform.LocalContext.current
    val studioAudioPickerLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.GetContent()
    ) { uri: android.net.Uri? ->
        uri?.let {
            viewModel.importAndSeparateLocalAudio(context, it)
            showSongPicker = false
        }
    }

    // Start mic monitoring when on studio screen
    LaunchedEffect(hasRecordAudioPermission) {
        viewModel.startMic(hasRecordAudioPermission)
    }

    // Empty state if no song is imported yet
    if (selectedSong == null) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(StudioBackground)
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(80.dp)
                        .clip(CircleShape)
                        .background(NeonPurple.copy(alpha = 0.2f))
                ) {
                    Icon(
                        imageVector = Icons.Default.AudioFile,
                        contentDescription = null,
                        tint = NeonCyan,
                        modifier = Modifier.size(40.dp)
                    )
                }

                Spacer(modifier = Modifier.height(18.dp))

                Text(
                    text = "暂未导入本地 MP3 歌曲",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "点击下方按钮选择你的手机本地 MP3，系统将自动分离背景伴奏与人声干声，开始实时跟唱！",
                    fontSize = 13.sp,
                    color = TextMuted,
                    textAlign = TextAlign.Center,
                    lineHeight = 20.sp
                )

                Spacer(modifier = Modifier.height(24.dp))

                androidx.compose.material3.Button(
                    onClick = { studioAudioPickerLauncher.launch("audio/*") },
                    colors = androidx.compose.material3.ButtonDefaults.buttonColors(containerColor = NeonCyan),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("empty_state_upload_mp3_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.AudioFile,
                        contentDescription = null,
                        tint = Color.Black,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "上传我的本地 MP3 歌曲",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black
                    )
                }
            }
        }
        return
    }

    val activeSong = selectedSong!!

    // Auto-scroll lyrics to current line
    val lyricsListState = rememberLazyListState()
    val currentLineIndex = remember(playbackPosMs, activeSong) {
        activeSong.lyrics.indexOfFirst { playbackPosMs in it.startMs..it.endMs }
            .takeIf { it >= 0 } ?: 0
    }

    LaunchedEffect(currentLineIndex) {
        if (currentLineIndex >= 0 && activeSong.lyrics.isNotEmpty()) {
            lyricsListState.animateScrollToItem(currentLineIndex.coerceAtLeast(0))
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(StudioBackground)
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        // 1. Header Bar: Song Title, Local MP3 Upload Button & Current Voice Profile Pill
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .clickable { showSongPicker = true }
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = activeSong.title,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "▼ 换歌",
                        fontSize = 11.sp,
                        color = NeonCyan,
                        fontWeight = FontWeight.Medium
                    )
                }
                Text(
                    text = "${activeSong.artist} · ${activeSong.genre} · v${com.example.BuildConfig.VERSION_NAME}",
                    fontSize = 11.sp,
                    color = TextMuted
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                // Direct Local MP3 Upload Button
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(NeonPurple.copy(alpha = 0.25f))
                        .clickable { studioAudioPickerLauncher.launch("audio/*") }
                        .padding(horizontal = 9.dp, vertical = 6.dp)
                        .testTag("top_bar_import_mp3_button")
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        androidx.compose.material3.Icon(
                            imageVector = androidx.compose.material.icons.Icons.Default.GraphicEq,
                            contentDescription = "传MP3",
                            tint = NeonPurple,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "传MP3",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = NeonPurple
                        )
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                // Voice Profile Tag (Click to navigate to voice clone screen)
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(StudioSurfaceVariant)
                        .clickable { viewModel.navigateTo(AppScreen.VOICE_CLONE) }
                        .padding(horizontal = 9.dp, vertical = 6.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(7.dp)
                                .clip(CircleShape)
                                .background(NeonCyan)
                        )
                        Spacer(modifier = Modifier.width(5.dp))
                        Text(
                            text = activeProfile.name,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = NeonCyan
                        )
                    }
                }
            }
        }

        // AI Voice Conversion & Profile Control Card
        Spacer(modifier = Modifier.height(6.dp))
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = StudioSurfaceVariant.copy(alpha = 0.85f))
        ) {
            Column(modifier = Modifier.padding(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .clickable { showVoicePicker = true },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = NeonGreen,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "主唱声线: ${activeProfile.name}",
                                    fontSize = 12.sp,
                                    color = NeonGreen,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "▼ 换音色",
                                    fontSize = 10.sp,
                                    color = NeonCyan,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                            Text(
                                text = if (activeProfile.dryAudioFilePath != null) {
                                    "✨ 已绑定本人真实干声：已用你的音色迁移翻唱全曲旋律与歌词"
                                } else {
                                    "正在用当前音色模型演唱全曲旋律与歌词"
                                },
                                fontSize = 10.sp,
                                color = if (activeProfile.dryAudioFilePath != null) NeonCyan else TextMuted
                            )
                        }
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        // Re-synthesize AI Cover button
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(NeonPurple.copy(alpha = 0.3f))
                                .clickable { viewModel.synthesizeAiCover(forceRebuild = true) }
                                .padding(horizontal = 7.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = "⚡ 重新合成",
                                color = NeonPurple,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        if (activeProfile.dryAudioFilePath != null) {
                            val isAuditionPlaying by viewModel.isAuditionPlaying.collectAsState()
                            Spacer(modifier = Modifier.width(6.dp))
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isAuditionPlaying) NeonMagenta else NeonGreen.copy(alpha = 0.25f))
                                    .clickable { viewModel.toggleAudition(activeProfile.dryAudioFilePath) }
                                    .padding(horizontal = 7.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = if (isAuditionPlaying) "⏸ 暂停" else "▶ 试听干声",
                                    color = if (isAuditionPlaying) Color.Black else NeonGreen,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }

                // If currently synthesizing voice conversion, show live progress bar
                if (conversionState.isConverting) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = conversionState.statusMessage,
                                fontSize = 10.sp,
                                color = NeonYellow
                            )
                            Text(
                                text = "${(conversionState.progress * 100).toInt()}%",
                                fontSize = 10.sp,
                                color = NeonYellow,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        LinearProgressIndicator(
                            progress = { conversionState.progress },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(4.dp)
                                .clip(RoundedCornerShape(2.dp)),
                            color = NeonYellow,
                            trackColor = StudioSurface
                        )
                    }
                }

                // Stem Solo Listening Selectors: Listen to BGM only, AI Vocal only, or Full Song
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    val isBoth = stemSoloMode == "BOTH"
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(6.dp))
                            .background(if (isBoth) NeonCyan.copy(alpha = 0.25f) else StudioSurface)
                            .clickable {
                                stemSoloMode = "BOTH"
                                viewModel.audioEngine.stemSoloMode = "BOTH"
                            }
                            .padding(vertical = 4.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "伴奏+AI人声",
                            fontSize = 10.sp,
                            fontWeight = if (isBoth) FontWeight.Bold else FontWeight.Normal,
                            color = if (isBoth) NeonCyan else TextMuted
                        )
                    }

                    val isVocal = stemSoloMode == "VOCAL"
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(6.dp))
                            .background(if (isVocal) NeonPurple.copy(alpha = 0.25f) else StudioSurface)
                            .clickable {
                                stemSoloMode = "VOCAL"
                                viewModel.audioEngine.stemSoloMode = "VOCAL"
                            }
                            .padding(vertical = 4.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "单听AI人声",
                            fontSize = 10.sp,
                            fontWeight = if (isVocal) FontWeight.Bold else FontWeight.Normal,
                            color = if (isVocal) NeonPurple else TextMuted
                        )
                    }

                    val isBgm = stemSoloMode == "BGM"
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(6.dp))
                            .background(if (isBgm) NeonGreen.copy(alpha = 0.25f) else StudioSurface)
                            .clickable {
                                stemSoloMode = "BGM"
                                viewModel.audioEngine.stemSoloMode = "BGM"
                            }
                            .padding(vertical = 4.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "单听伴奏",
                            fontSize = 10.sp,
                            fontWeight = if (isBgm) FontWeight.Bold else FontWeight.Normal,
                            color = if (isBgm) NeonGreen else TextMuted
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // 2. Score & Combo Board
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(StudioSurface)
                .padding(horizontal = 14.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "得分",
                    fontSize = 10.sp,
                    color = TextMuted
                )
                Text(
                    text = "${scoreState.currentScore}",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = NeonYellow
                )
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = if (scoreState.comboCount > 1) "🔥 COMBO x${scoreState.comboCount}" else "连击",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (scoreState.comboCount > 1) NeonMagenta else TextMuted
                )
                Text(
                    text = scoreState.lastHitGrade.ifEmpty { "跟唱评分" },
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = when (scoreState.lastHitGrade) {
                        "PERFECT" -> NeonCyan
                        "GREAT" -> NeonGreen
                        "GOOD" -> NeonYellow
                        else -> TextMuted
                    }
                )
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "音准吻合率",
                    fontSize = 10.sp,
                    color = TextMuted
                )
                Text(
                    text = "${scoreState.accuracyPercent}%",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = NeonCyan
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // 3. Dynamic Pitch Visualizer (Melody note waterfall & user pitch tracker)
        PitchVisualizer(
            notes = activeSong.pitchNotes,
            currentPosMs = playbackPosMs,
            userMidiNote = micStatus.detectedMidi,
            isMouthOpen = micStatus.isMouthOpen,
            modifier = Modifier.testTag("pitch_visualizer")
        )

        Spacer(modifier = Modifier.height(10.dp))

        // 4. Synchronized Scrolling Lyrics
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .height(110.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = StudioSurface)
        ) {
            LazyColumn(
                state = lyricsListState,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(vertical = 8.dp, horizontal = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                items(activeSong.lyrics) { line ->
                    val isCurrent = playbackPosMs in line.startMs..line.endMs
                    Text(
                        text = line.text,
                        fontSize = if (isCurrent) 16.sp else 13.sp,
                        fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal,
                        color = if (isCurrent) NeonCyan else TextMuted,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // 5. Singing Mode Selector: 张嘴跟唱 VS 全曲声线替代(AI翻唱)
        val singingMode by viewModel.singingMode.collectAsState()
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(StudioSurface)
                .padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            val isMode1 = singingMode == com.example.model.SingingMode.REALTIME_MIC_TRIGGER
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(9.dp))
                    .background(if (isMode1) NeonPurple else Color.Transparent)
                    .clickable { viewModel.setSingingMode(com.example.model.SingingMode.REALTIME_MIC_TRIGGER) }
                    .padding(vertical = 7.dp)
                    .testTag("mode_mic_trigger_tab"),
                contentAlignment = Alignment.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Mic,
                        contentDescription = null,
                        tint = if (isMode1) Color.White else TextMuted,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "张嘴实时跟唱",
                        fontSize = 12.sp,
                        fontWeight = if (isMode1) FontWeight.Bold else FontWeight.Normal,
                        color = if (isMode1) Color.White else TextMuted
                    )
                }
            }

            val isMode2 = singingMode == com.example.model.SingingMode.FULL_VOICE_REPLACEMENT
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(9.dp))
                    .background(if (isMode2) NeonCyan else Color.Transparent)
                    .clickable { viewModel.setSingingMode(com.example.model.SingingMode.FULL_VOICE_REPLACEMENT) }
                    .padding(vertical = 7.dp)
                    .testTag("mode_full_replacement_tab"),
                contentAlignment = Alignment.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.GraphicEq,
                        contentDescription = null,
                        tint = if (isMode2) Color.Black else TextMuted,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "全曲声线替代(AI翻唱)",
                        fontSize = 12.sp,
                        fontWeight = if (isMode2) FontWeight.Bold else FontWeight.Normal,
                        color = if (isMode2) Color.Black else TextMuted
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // 6. Mouth Activity / Voice Detection Radar
        MouthActivityIndicator(
            isMouthOpen = micStatus.isMouthOpen,
            amplitude = micStatus.amplitude,
            activeProfile = activeProfile,
            isFullReplacementMode = singingMode == com.example.model.SingingMode.FULL_VOICE_REPLACEMENT,
            modifier = Modifier.testTag("mouth_activity_indicator")
        )

        Spacer(modifier = Modifier.height(10.dp))

        // Optional permission request reminder if mic not granted
        if (!hasRecordAudioPermission) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onRequestPermission() },
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = StudioSurfaceVariant)
            ) {
                Row(
                    modifier = Modifier.padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = null,
                        tint = NeonYellow,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "点击开启麦克风权限以获得真实张嘴声音跟唱体验",
                        fontSize = 11.sp,
                        color = TextPrimary
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
        }

        // 6. Collapsible Mixer Panel
        AnimatedVisibility(
            visible = showMixerPanel,
            enter = fadeIn(tween(200)),
            exit = fadeOut(tween(200))
        ) {
            StemMixerBar(
                bgmVolume = viewModel.audioEngine.bgmVolume,
                aiVoiceVolume = viewModel.audioEngine.aiVoiceVolume,
                micSensitivity = viewModel.micDetector.sensitivityThreshold,
                currentReverb = viewModel.audioEngine.reverbType,
                onBgmVolumeChange = { viewModel.setBgmVolume(it) },
                onAiVoiceVolumeChange = { viewModel.setAiVoiceVolume(it) },
                onMicSensitivityChange = { viewModel.setMicSensitivity(it) },
                onReverbChange = { viewModel.setReverbType(it) },
                modifier = Modifier.padding(bottom = 8.dp)
            )
        }

        Spacer(modifier = Modifier.weight(1f))

        // 7. Playback & Action Controls
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Replay from start
            IconButton(
                onClick = { viewModel.restartSong() },
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(StudioSurface)
            ) {
                Icon(
                    imageVector = Icons.Default.Replay,
                    contentDescription = "重新开始",
                    tint = TextPrimary
                )
            }

            // Session Record button
            IconButton(
                onClick = { viewModel.toggleSessionRecording() },
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(if (isRecordingSession) NeonMagenta else StudioSurface)
                    .testTag("session_record_button")
            ) {
                Icon(
                    imageVector = Icons.Default.FiberManualRecord,
                    contentDescription = "录制作品",
                    tint = if (isRecordingSession) Color.White else NeonMagenta
                )
            }

            // Main Play/Pause Button
            FloatingActionButton(
                onClick = { viewModel.togglePlayPause() },
                containerColor = NeonCyan,
                contentColor = Color.Black,
                modifier = Modifier
                    .size(64.dp)
                    .testTag("play_pause_button")
            ) {
                Icon(
                    imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = if (isPlaying) "暂停" else "播放",
                    modifier = Modifier.size(32.dp)
                )
            }

            // Stem Separation Studio Shortcut
            IconButton(
                onClick = { viewModel.navigateTo(AppScreen.STEM_SEPARATION) },
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(StudioSurface)
            ) {
                Icon(
                    imageVector = Icons.Default.GraphicEq,
                    contentDescription = "伴奏与干声分离",
                    tint = NeonPurple
                )
            }

            // Mixer Toggle
            IconButton(
                onClick = { showMixerPanel = !showMixerPanel },
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(if (showMixerPanel) NeonPurple else StudioSurface)
            ) {
                Icon(
                    imageVector = Icons.Default.Tune,
                    contentDescription = "混音台",
                    tint = if (showMixerPanel) Color.White else TextPrimary
                )
            }
        }
    }

    // Song Selection Dialog
    if (showSongPicker) {
        AlertDialog(
            onDismissRequest = { showSongPicker = false },
            title = {
                Text(text = "选择跟唱歌曲", color = TextPrimary, fontWeight = FontWeight.Bold)
            },
            text = {
                LazyColumn(modifier = Modifier.fillMaxWidth()) {
                    item {
                        androidx.compose.material3.Button(
                            onClick = { studioAudioPickerLauncher.launch("audio/*") },
                            colors = androidx.compose.material3.ButtonDefaults.buttonColors(containerColor = NeonPurple),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 12.dp)
                                .testTag("dialog_upload_local_mp3_button")
                        ) {
                            androidx.compose.material3.Icon(
                                imageVector = androidx.compose.material.icons.Icons.Default.AudioFile,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "从手机选择本地MP3并分离",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                        }
                    }

                    items(songs) { song ->
                        val isSel = song.id == selectedSong?.id
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (isSel) StudioSurfaceVariant else Color.Transparent)
                                .clickable {
                                    viewModel.selectSong(song)
                                    showSongPicker = false
                                }
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = song.title,
                                    color = if (isSel) NeonCyan else TextPrimary,
                                    fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal
                                )
                                Text(
                                    text = "${song.artist} · ${song.genre}",
                                    fontSize = 11.sp,
                                    color = TextMuted
                                )
                            }
                            if (song.isCustomImported) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(NeonGreen.copy(alpha = 0.2f))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(text = "自选分轨", fontSize = 10.sp, color = NeonGreen)
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showSongPicker = false }) {
                    Text("关闭", color = NeonCyan)
                }
            },
            containerColor = StudioSurface
        )
    }

    // Voice Profile Picker Dialog
    if (showVoicePicker) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { showVoicePicker = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.RecordVoiceOver,
                        contentDescription = null,
                        tint = NeonCyan,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "选择AI主唱声线 (替换原唱)",
                        color = TextPrimary,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            text = {
                LazyColumn(modifier = Modifier.fillMaxWidth()) {
                    item {
                        androidx.compose.material3.Button(
                            onClick = {
                                showVoicePicker = false
                                viewModel.navigateTo(AppScreen.VOICE_CLONE)
                            },
                            colors = androidx.compose.material3.ButtonDefaults.buttonColors(containerColor = NeonPurple),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Mic,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "采集录制 / 上传新干声样本",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                        }
                    }

                    items(profiles) { profile ->
                        val isSel = profile.id == activeProfile.id
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (isSel) StudioSurfaceVariant else Color.Transparent)
                                .clickable {
                                    viewModel.selectVoiceProfile(profile)
                                    showVoicePicker = false
                                    viewModel.synthesizeAiCover(forceRebuild = false)
                                }
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(if (profile.isUserCustom) NeonPurple else Color(0xFF24304E))
                            ) {
                                Icon(
                                    imageVector = Icons.Default.RecordVoiceOver,
                                    contentDescription = null,
                                    tint = if (isSel) NeonCyan else Color.White,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = profile.name,
                                        color = if (isSel) NeonCyan else TextPrimary,
                                        fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal,
                                        fontSize = 13.sp
                                    )
                                    if (profile.dryAudioFilePath != null) {
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(4.dp))
                                                .background(NeonGreen.copy(alpha = 0.2f))
                                                .padding(horizontal = 5.dp, vertical = 2.dp)
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
                            if (isSel) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = "当前使用",
                                    tint = NeonCyan,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showVoicePicker = false }) {
                    Text("关闭", color = NeonCyan)
                }
            },
            containerColor = StudioSurface
        )
    }
}
