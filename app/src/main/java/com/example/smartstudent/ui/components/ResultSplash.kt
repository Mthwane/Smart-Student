package com.example.smartstudent.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.EaseOutBack
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.PriorityHigh
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.smartstudent.theme.StudentGray600
import com.example.smartstudent.theme.StudentGreen
import com.example.smartstudent.theme.StudentGreenLight
import com.example.smartstudent.theme.StudentRed
import com.example.smartstudent.theme.StudentRedLight
import com.example.smartstudent.util.SoundPlayer
import kotlin.math.cos
import kotlin.math.sin
import androidx.compose.foundation.layout.systemBarsPadding

enum class ResultOutcome { SUCCESS, FAILURE }

/**
 * Full-bleed animated result screen. Use for the outcome of any task the user
 * initiated (a scan, a save, a sign-up) — SUCCESS gets a bouncy mascot + confetti,
 * FAILURE gets a gentle "shake" and a short, friendly message.
 *
 * IMPORTANT: [message] must always be a short, user-safe string (e.g. "Couldn't read
 * that file — try again"), never a raw exception message or API error body. Log the
 * technical detail separately via AppLogger before showing this.
 */
@Composable
fun ResultSplash(
    outcome: ResultOutcome,
    title: String,
    message: String,
    modifier: Modifier = Modifier,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
    applySystemBars: Boolean = false
) {
    val isSuccess = outcome == ResultOutcome.SUCCESS
    val bg = if (isSuccess) StudentGreenLight else StudentRedLight
    val accent = if (isSuccess) StudentGreen else StudentRed

    val scale = remember { Animatable(0.6f) }
    val shakeOffsetDp = remember { Animatable(0f) }

    LaunchedEffect(outcome) {
        if (isSuccess) SoundPlayer.playSuccess() else SoundPlayer.playError()
        scale.animateTo(1f, animationSpec = tween(420, easing = EaseOutBack))
        if (!isSuccess) {
            // Gentle shake: a few quick small oscillations, then settle.
            shakeOffsetDp.animateTo(
                targetValue = 0f,
                animationSpec = keyframes {
                    durationMillis = 380
                    0f at 0
                    -14f at 60
                    12f at 120
                    -8f at 190
                    6f at 260
                    0f at 380
                }
            )
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(bg)
            .then(if (applySystemBars) Modifier.systemBarsPadding() else Modifier)
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(contentAlignment = Alignment.Center) {
            if (isSuccess) {
                ConfettiBurst(color = accent)
            }
            Box(
                modifier = Modifier
                    .scale(scale.value)
                    .offset(x = shakeOffsetDp.value.dp),
                contentAlignment = Alignment.Center
            ) {
                MascotBird(
                    size = 120.dp,
                    mood = if (isSuccess) MascotMood.HAPPY else MascotMood.CONCERNED
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        Box(
            modifier = Modifier
                .size(40.dp)
                .background(if (isSuccess) StudentGreen else StudentRed, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                if (isSuccess) Icons.Filled.Check else Icons.Filled.PriorityHigh,
                contentDescription = null,
                tint = Color.White
            )
        }

        Spacer(modifier = Modifier.height(16.dp))
        Text(title, style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            message,
            style = MaterialTheme.typography.bodyLarge,
            color = StudentGray600
        )

        if (actionLabel != null && onAction != null) {
            Spacer(modifier = Modifier.height(28.dp))
            PrimaryPillButton(
                text = actionLabel,
                onClick = onAction,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

/** Small celebratory burst of dots radiating outward, drawn with Canvas — no image assets. */
@Composable
private fun ConfettiBurst(color: Color) {
    val infiniteTransition = rememberInfiniteTransition(label = "confetti")
    val progress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400, easing = LinearOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "confetti_progress"
    )

    Canvas(modifier = Modifier.size(220.dp)) {
        val center = Offset(this.size.width / 2f, this.size.height / 2f)
        val dots = 10
        for (i in 0 until dots) {
            val angle = (2 * Math.PI * i / dots).toFloat()
            val distance = 60f + progress * 40f
            val alpha = (1f - progress).coerceIn(0f, 1f)
            val x = center.x + cos(angle) * distance
            val y = center.y + sin(angle) * distance
            drawCircle(
                color = color.copy(alpha = alpha),
                radius = 5f,
                center = Offset(x, y)
            )
        }
    }
}
