package com.example.smartstudent.ui.components

import androidx.compose.animation.core.EaseInOutSine
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.smartstudent.theme.StudentBlack
import com.example.smartstudent.theme.StudentGold
import com.example.smartstudent.theme.StudentGreen
import com.example.smartstudent.theme.StudentRed
import com.example.smartstudent.util.SoundPlayer
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * SmartStudent's mascot: a small round bird drawn with Canvas paths (no image assets
 * needed). Idles with a gentle bob + wing flap; tapping it triggers a quick excited
 * hop, giving the app a bit of personality without pulling in a Lottie dependency.
 */
enum class MascotMood { NEUTRAL, HAPPY, CONCERNED }

@Composable
fun MascotBird(
    modifier: Modifier = Modifier,
    size: Dp = 96.dp,
    mood: MascotMood = MascotMood.NEUTRAL,
    onTap: (() -> Unit)? = null
) {
    val infiniteTransition = rememberInfiniteTransition(label = "mascot_idle")

    val bob by infiniteTransition.animateFloat(
        initialValue = -4f,
        targetValue = 4f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "bob"
    )

    val wingFlap by infiniteTransition.animateFloat(
        initialValue = -8f,
        targetValue = 10f,
        animationSpec = infiniteRepeatable(
            animation = tween(700, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "wing"
    )

    var hopBoost by remember { mutableFloatStateOf(0f) }
    val scope = rememberCoroutineScope()

    val bodyColor = when (mood) {
        MascotMood.NEUTRAL -> StudentGold
        MascotMood.HAPPY -> StudentGreen
        MascotMood.CONCERNED -> StudentRed
    }

    Canvas(
        modifier = modifier
            .size(size)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) {
                SoundPlayer.playChirp()
                onTap?.invoke()
                scope.launch {
                    hopBoost = -18f
                    delay(120)
                    hopBoost = 0f
                }
            }
    ) {
        val w = this.size.width
        val h = this.size.height
        val centerX = w / 2f
        val bodyCenterY = h * 0.55f + bob + hopBoost
        val bodyRadius = w * 0.32f

        // Body
        drawCircle(color = bodyColor, radius = bodyRadius, center = Offset(centerX, bodyCenterY))

        // Belly (lighter patch, beige)
        drawCircle(
            color = Color(0xFFFAF6EC),
            radius = bodyRadius * 0.55f,
            center = Offset(centerX, bodyCenterY + bodyRadius * 0.25f)
        )

        // Wing (rotates for a flap effect)
        rotate(degrees = wingFlap, pivot = Offset(centerX - bodyRadius * 0.2f, bodyCenterY)) {
            drawOval(
                color = StudentBlack.copy(alpha = 0.85f),
                topLeft = Offset(centerX - bodyRadius * 0.95f, bodyCenterY - bodyRadius * 0.15f),
                size = Size(bodyRadius * 0.85f, bodyRadius * 0.5f)
            )
        }

        // Eye
        drawCircle(
            color = StudentBlack,
            radius = bodyRadius * 0.09f,
            center = Offset(centerX + bodyRadius * 0.32f, bodyCenterY - bodyRadius * 0.25f)
        )

        // Beak (small triangle)
        val beakPath = Path().apply {
            moveTo(centerX + bodyRadius * 0.85f, bodyCenterY - bodyRadius * 0.05f)
            lineTo(centerX + bodyRadius * 1.25f, bodyCenterY + bodyRadius * 0.05f)
            lineTo(centerX + bodyRadius * 0.85f, bodyCenterY + bodyRadius * 0.2f)
            close()
        }
        drawPath(beakPath, color = Color(0xFFD9A441))

        // Feet
        val footY = bodyCenterY + bodyRadius * 0.95f
        drawLine(
            color = StudentBlack,
            start = Offset(centerX - bodyRadius * 0.2f, footY),
            end = Offset(centerX - bodyRadius * 0.35f, footY + bodyRadius * 0.25f),
            strokeWidth = 4f
        )
        drawLine(
            color = StudentBlack,
            start = Offset(centerX + bodyRadius * 0.2f, footY),
            end = Offset(centerX + bodyRadius * 0.35f, footY + bodyRadius * 0.25f),
            strokeWidth = 4f
        )
    }
}
