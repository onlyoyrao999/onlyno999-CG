package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.PitchNote
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonGreen
import com.example.ui.theme.NeonPurple
import com.example.ui.theme.NeonYellow
import com.example.ui.theme.StudioSurface
import com.example.ui.theme.StudioSurfaceVariant
import kotlin.math.abs

@Composable
fun PitchVisualizer(
    notes: List<PitchNote>,
    currentPosMs: Long,
    userMidiNote: Float,
    isMouthOpen: Boolean,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(140.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(StudioSurface)
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val canvasWidth = size.width
            val canvasHeight = size.height

            // Time window: view shows from (currentPosMs - 1500ms) to (currentPosMs + 3500ms)
            val windowStartMs = currentPosMs - 1500L
            val windowDurationMs = 5000L
            val playheadX = canvasWidth * 0.30f // Fixed playhead at 30% width

            // Pitch range: MIDI notes 55 (G3) to 75 (D#5)
            val minMidi = 55f
            val maxMidi = 75f
            val midiRange = maxMidi - minMidi

            fun midiToY(midi: Float): Float {
                val normalized = (midi - minMidi) / midiRange
                return canvasHeight * (1.0f - normalized.coerceIn(0f, 1f))
            }

            fun timeToX(timeMs: Long): Float {
                val deltaMs = timeMs - currentPosMs
                return playheadX + (deltaMs.toFloat() / windowDurationMs) * canvasWidth
            }

            // 1. Draw subtle horizontal pitch grid lines
            for (m in minMidi.toInt()..maxMidi.toInt() step 3) {
                val y = midiToY(m.toFloat())
                drawLine(
                    color = StudioSurfaceVariant.copy(alpha = 0.6f),
                    start = Offset(0f, y),
                    end = Offset(canvasWidth, y),
                    strokeWidth = 1f
                )
            }

            // 2. Draw Target Song Notes (Rectangles moving from right to left)
            for (note in notes) {
                if (note.endMs < windowStartMs || note.startMs > currentPosMs + windowDurationMs) {
                    continue
                }

                val xStart = timeToX(note.startMs)
                val xEnd = timeToX(note.endMs)
                val barWidth = (xEnd - xStart).coerceAtLeast(16f)
                val y = midiToY(note.midiNote.toFloat()) - 10f
                val barHeight = 20f

                val isCurrentlyPlaying = currentPosMs in note.startMs..note.endMs
                val isHit = isCurrentlyPlaying && isMouthOpen && abs(userMidiNote - note.midiNote) <= 2.0f

                val noteBrush = if (isHit) {
                    Brush.horizontalGradient(
                        listOf(NeonCyan, NeonGreen)
                    )
                } else if (isCurrentlyPlaying) {
                    Brush.horizontalGradient(
                        listOf(NeonPurple, NeonYellow)
                    )
                } else {
                    Brush.horizontalGradient(
                        listOf(Color(0xFF4C358A), Color(0xFF6B4FB8))
                    )
                }

                // Draw rounded pitch note bar
                drawRoundRect(
                    brush = noteBrush,
                    topLeft = Offset(xStart, y),
                    size = Size(barWidth, barHeight),
                    cornerRadius = CornerRadius(10f, 10f)
                )

                // Glow outline if hit
                if (isHit) {
                    drawRoundRect(
                        color = Color.White.copy(alpha = 0.8f),
                        topLeft = Offset(xStart, y),
                        size = Size(barWidth, barHeight),
                        cornerRadius = CornerRadius(10f, 10f),
                        style = Stroke(width = 2.5f)
                    )
                }
            }

            // 3. Draw Vertical Playhead line
            drawLine(
                brush = Brush.verticalGradient(listOf(NeonCyan, NeonPurple)),
                start = Offset(playheadX, 0f),
                end = Offset(playheadX, canvasHeight),
                strokeWidth = 3f
            )

            // 4. Draw User Live Singing Pitch Marker
            if (isMouthOpen && userMidiNote in minMidi..maxMidi) {
                val userY = midiToY(userMidiNote)

                // Glowing outer halo
                drawCircle(
                    color = NeonCyan.copy(alpha = 0.35f),
                    radius = 22f,
                    center = Offset(playheadX, userY)
                )

                // Glowing core dot
                drawCircle(
                    color = NeonCyan,
                    radius = 9f,
                    center = Offset(playheadX, userY)
                )

                // White bright spark
                drawCircle(
                    color = Color.White,
                    radius = 4f,
                    center = Offset(playheadX, userY)
                )
            }
        }

        // Overlay status badge
        if (isMouthOpen) {
            Text(
                text = "● 音准捕捉中",
                color = NeonCyan,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.align(Alignment.TopEnd)
            )
        }
    }
}
