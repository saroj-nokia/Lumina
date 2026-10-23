package com.example.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

fun Modifier.shimmerBackground(baseColor: Color = Color(0xFF22252D)): Modifier = composed {
    val transition = rememberInfiniteTransition(label = "shimmer_transition")
    // Keep the State and only read .value while drawing, so each frame redraws
    // instead of recomposing every loading card.
    val translate = transition.animateFloat(
        initialValue = 0f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmer_anim"
    )

    val highlightColor = baseColor.copy(alpha = 0.5f)
    val shimmerColor = Color.White.copy(alpha = 0.15f)

    drawBehind {
        val t = translate.value
        drawRect(
            Brush.linearGradient(
                colors = listOf(highlightColor, shimmerColor, highlightColor),
                start = Offset(t - 400f, t - 400f),
                end = Offset(t, t)
            )
        )
    }
}
