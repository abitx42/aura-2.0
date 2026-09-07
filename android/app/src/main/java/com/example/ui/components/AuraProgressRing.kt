package com.example.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.anim.AuraAnimTiming
import com.example.ui.theme.AuraTheme

/**
 * Animated circular progress ring with centered prominent metric and subtle track.
 * Used for "Spent This Period", budget fulfillment, habit streaks, etc.
 */
@Composable
fun AuraProgressRing(
    progress: Float, // 0.0f to 1.0f (can exceed 1.0f if overbudget)
    mainText: String,
    modifier: Modifier = Modifier,
    subText: String? = null,
    badgeText: String? = null,
    size: Dp = 160.dp,
    strokeWidth: Dp = 10.dp,
    progressColor: Color = AuraTheme.colors.accentBrand,
    trackColor: Color = AuraTheme.colors.cardBorder
) {
    val animatedProgress by animateFloatAsState(
        targetValue = progress.coerceIn(0f, 1f),
        animationSpec = tween(
            durationMillis = AuraAnimTiming.ScreenTransition * 2,
            easing = androidx.compose.animation.core.FastOutSlowInEasing
        ),
        label = "progress_ring_arc"
    )

    Box(
        modifier = modifier.size(size),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize().padding(strokeWidth / 2)) {
            val sweep = animatedProgress * 360f

            // Background Track
            drawArc(
                color = trackColor,
                startAngle = -90f,
                sweepAngle = 360f,
                useCenter = false,
                style = Stroke(width = strokeWidth.toPx(), cap = StrokeCap.Round)
            )

            // Animated Foreground Arc
            if (animatedProgress > 0f) {
                drawArc(
                    color = progressColor,
                    startAngle = -90f,
                    sweepAngle = sweep,
                    useCenter = false,
                    style = Stroke(width = strokeWidth.toPx(), cap = StrokeCap.Round)
                )
            }
        }

        // Center Content
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(horizontal = strokeWidth * 2)
        ) {
            if (!badgeText.isNullOrBlank()) {
                Text(
                    text = badgeText.uppercase(),
                    style = MaterialTheme.typography.labelSmall,
                    color = progressColor,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(2.dp))
            }

            Text(
                text = mainText,
                style = MaterialTheme.typography.headlineMedium,
                color = AuraTheme.colors.textPrimary,
                fontWeight = FontWeight.Black,
                textAlign = TextAlign.Center,
                maxLines = 1
            )

            if (!subText.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subText,
                    style = MaterialTheme.typography.bodySmall,
                    color = AuraTheme.colors.textSecondary,
                    textAlign = TextAlign.Center,
                    maxLines = 1
                )
            }
        }
    }
}
