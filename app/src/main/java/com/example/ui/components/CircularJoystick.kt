package com.example.ui.components

import androidx.compose.animation.core.animateOffsetAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.ui.games.GameInputState
import com.example.utils.SoundHelper
import kotlin.math.hypot

@Composable
fun CircularJoystick(
    input: GameInputState,
    accentColor: Color = Color(0xFF38BDF8),
    joystickSize: Dp = 128.dp,
    onDirectionHint: ((String) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    var dragOffset by remember { mutableStateOf(Offset.Zero) }
    var isActive by remember { mutableStateOf(false) }

    val animatedKnob by animateOffsetAsState(
        targetValue = if (isActive) dragOffset else Offset.Zero,
        animationSpec = spring(dampingRatio = 0.65f, stiffness = 480f),
        label = "joystickKnob"
    )

    Canvas(
        modifier = modifier
            .size(joystickSize)
            .testTag("circular_joystick")
            .pointerInput(joystickSize) {
                val maxRadius = (size.width.coerceAtMost(size.height) / 2f) * 0.62f
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    down.consume()
                    isActive = true
                    SoundHelper.playPop()

                    fun updateFromPosition(pos: Offset) {
                        val center = Offset(size.width / 2f, size.height / 2f)
                        val delta = pos - center
                        val dist = hypot(delta.x, delta.y)
                        val clamped = if (dist > maxRadius && dist > 0.001f) {
                            Offset((delta.x / dist) * maxRadius, (delta.y / dist) * maxRadius)
                        } else {
                            delta
                        }
                        dragOffset = clamped
                        val normX = (clamped.x / maxRadius.coerceAtLeast(1f)).coerceIn(-1f, 1f)
                        val normY = (clamped.y / maxRadius.coerceAtLeast(1f)).coerceIn(-1f, 1f)
                        // Deadzone 0.08f for jitter-free precision
                        val deadX = if (kotlin.math.abs(normX) < 0.08f) 0f else normX
                        val deadY = if (kotlin.math.abs(normY) < 0.08f) 0f else normY
                        input.setAnalog(deadX, deadY)
                    }

                    updateFromPosition(down.position)
                    onDirectionHint?.invoke("Joystick 360° Aktif")

                    while (true) {
                        val event = awaitPointerEvent()
                        val change = event.changes.firstOrNull { it.id == down.id } ?: event.changes.firstOrNull()
                        if (change == null || !change.pressed) {
                            break
                        }
                        change.consume()
                        updateFromPosition(change.position)
                    }

                    isActive = false
                    dragOffset = Offset.Zero
                    input.setAnalog(0f, 0f)
                }
            }
    ) {
        val center = Offset(size.width / 2f, size.height / 2f)
        val outerR = size.minDimension / 2f - 4f
        val knobR = outerR * 0.42f

        // Outer translucent base disc
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    accentColor.copy(alpha = if (isActive) 0.24f else 0.14f),
                    Color(0xFF0F172A).copy(alpha = 0.72f)
                ),
                center = center,
                radius = outerR
            ),
            radius = outerR,
            center = center
        )

        // Outer glowing ring
        drawCircle(
            color = accentColor.copy(alpha = if (isActive) 0.95f else 0.55f),
            radius = outerR,
            center = center,
            style = Stroke(width = if (isActive) 5f else 3f)
        )

        // Subtle 360 inner guide ring & crosshairs
        drawCircle(
            color = Color.White.copy(alpha = 0.14f),
            radius = outerR * 0.55f,
            center = center,
            style = Stroke(width = 1.5f)
        )
        drawLine(
            color = Color.White.copy(alpha = 0.12f),
            start = Offset(center.x - outerR * 0.75f, center.y),
            end = Offset(center.x + outerR * 0.75f, center.y),
            strokeWidth = 1.5f
        )
        drawLine(
            color = Color.White.copy(alpha = 0.12f),
            start = Offset(center.x, center.y - outerR * 0.75f),
            end = Offset(center.x, center.y + outerR * 0.75f),
            strokeWidth = 1.5f
        )

        // Draggable 360 spherical thumb knob
        val knobCenter = center + animatedKnob
        // Soft shadow
        drawCircle(
            color = Color.Black.copy(alpha = 0.45f),
            radius = knobR * 1.04f,
            center = knobCenter + Offset(2f, 4f)
        )
        // Spherical gradient
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(Color.White, accentColor, accentColor.copy(alpha = 0.7f)),
                center = knobCenter - Offset(knobR * 0.25f, knobR * 0.28f),
                radius = knobR * 1.3f
            ),
            radius = knobR,
            center = knobCenter
        )
        drawCircle(
            color = Color.White.copy(alpha = 0.8f),
            radius = knobR,
            center = knobCenter,
            style = Stroke(width = 2.5f)
        )
    }
}
