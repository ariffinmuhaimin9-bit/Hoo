package com.example.ui.games

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlin.math.atan2
import kotlin.math.hypot

@Stable
class GameInputState {
    var moveX by mutableFloatStateOf(0f) // -1f..1f smooth 360 analog
    var moveY by mutableFloatStateOf(0f) // -1f..1f smooth 360 analog
    var actionCount by mutableIntStateOf(0)

    // Remote WiFi opponent telemetry
    var remotePlayerName by mutableStateOf<String?>(null)
    var remoteScore by mutableIntStateOf(0)
    var remoteMoveX by mutableFloatStateOf(0f)
    var remoteMoveY by mutableFloatStateOf(0f)

    val magnitude: Float
        get() = hypot(moveX, moveY).coerceIn(0f, 1f)

    val angleDegrees: Float
        get() = Math.toDegrees(atan2(moveY.toDouble(), moveX.toDouble())).toFloat()

    fun setAnalog(x: Float, y: Float) {
        moveX = x.coerceIn(-1f, 1f)
        moveY = y.coerceIn(-1f, 1f)
    }

    fun triggerAction() {
        actionCount++
    }

    fun reset() {
        moveX = 0f
        moveY = 0f
    }
}
