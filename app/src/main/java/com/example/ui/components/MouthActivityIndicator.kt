package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.VoiceProfile
import com.example.ui.theme.MicActiveGlow
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonGreen
import com.example.ui.theme.NeonPurple
import com.example.ui.theme.StudioSurface
import com.example.ui.theme.StudioSurfaceVariant
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary

@Composable
fun MouthActivityIndicator(
    isMouthOpen: Boolean,
    amplitude: Float,
    activeProfile: VoiceProfile,
    isFullReplacementMode: Boolean = false,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.9f,
        targetValue = 1.25f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )

    val ringColor by animateColorAsState(
        targetValue = if (isMouthOpen) NeonGreen else Color(0xFF334155),
        animationSpec = tween(150),
        label = "ringColor"
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(StudioSurface)
            .border(
                width = 1.dp,
                color = if (isMouthOpen) NeonCyan.copy(alpha = 0.5f) else StudioSurfaceVariant,
                shape = RoundedCornerShape(20.dp)
            )
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Animated pulsating mic radar
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(56.dp)
            ) {
                // Outer ripple ring when singing
                if (isMouthOpen) {
                    Box(
                        modifier = Modifier
                            .size(56.dp * pulseScale)
                            .clip(CircleShape)
                            .background(NeonCyan.copy(alpha = 0.20f))
                    )
                }

                // Core indicator circle
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(
                            if (isMouthOpen) {
                                Brush.radialGradient(listOf(NeonCyan, NeonPurple))
                            } else {
                                Brush.radialGradient(listOf(StudioSurfaceVariant, Color(0xFF1E293B)))
                            }
                        )
                ) {
                    Icon(
                        imageVector = if (isMouthOpen) Icons.Default.RecordVoiceOver else Icons.Default.Mic,
                        contentDescription = "张嘴检测",
                        tint = if (isMouthOpen) Color.White else TextMuted,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(14.dp))

            // Text description & amplitude bar
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    val statusText = when {
                        isFullReplacementMode -> "全曲声线替代生效中 · 原唱已无缝替换为你的声线"
                        isMouthOpen -> "张嘴识别中 · 正在用你的声线跟唱"
                        else -> "只要张嘴发声 · 即可触发AI跟唱"
                    }
                    Text(
                        text = statusText,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        color = if (isFullReplacementMode) NeonCyan else if (isMouthOpen) NeonGreen else TextPrimary
                    )

                    // Small status pill
                    val pillBg = if (isFullReplacementMode) NeonCyan.copy(alpha = 0.2f) else if (isMouthOpen) NeonGreen.copy(alpha = 0.2f) else StudioSurfaceVariant
                    val pillText = if (isFullReplacementMode) "⚡ 声线全替换" else if (isMouthOpen) "跟唱触发 ON" else "待机侦听中"
                    val pillColor = if (isFullReplacementMode) NeonCyan else if (isMouthOpen) NeonGreen else TextMuted
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(pillBg)
                            .padding(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = pillText,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Medium,
                            color = pillColor
                        )
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Amplitude visualizer meter
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(StudioSurfaceVariant)
                ) {
                    val progress = amplitude.coerceIn(0f, 1f)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(progress)
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(
                                Brush.horizontalGradient(
                                    listOf(NeonPurple, NeonCyan, if (progress > 0.8f) NeonGreen else NeonCyan)
                                )
                            )
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "克隆声线: ${activeProfile.name}",
                        fontSize = 11.sp,
                        color = TextMuted
                    )

                    Text(
                        text = if (isMouthOpen) "AI发音合成率: 100%" else "音量门限: 灵敏",
                        fontSize = 11.sp,
                        color = if (isMouthOpen) NeonGreen else TextMuted
                    )
                }
            }
        }
    }
}
