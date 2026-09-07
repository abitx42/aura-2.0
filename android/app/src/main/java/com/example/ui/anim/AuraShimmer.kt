package com.example.ui.anim

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.ui.theme.AuraTheme

fun Modifier.auraShimmer(
    baseColor: Color? = null,
    highlightColor: Color? = null
): Modifier = composed {
    val defaultBase = baseColor ?: AuraTheme.colors.cardBackground
    val defaultHighlight = highlightColor ?: AuraTheme.colors.cardBorder

    val transition = rememberInfiniteTransition(label = "shimmer_transition")
    val translateAnim = transition.animateFloat(
        initialValue = -1000f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = AuraAnimTiming.ShimmerCycle,
                easing = LinearEasing
            ),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmer_sweep"
    )

    val brush = Brush.linearGradient(
        colors = listOf(
            defaultBase,
            defaultHighlight,
            defaultBase
        ),
        start = Offset(translateAnim.value - 200f, translateAnim.value - 200f),
        end = Offset(translateAnim.value + 200f, translateAnim.value + 200f)
    )

    this.background(brush = brush)
}

@Composable
fun ShimmerNoteCard(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(150.dp)
            .clip(RoundedCornerShape(AuraCornerRadius.Card))
            .auraShimmer()
    ) {}
}

@Composable
fun ShimmerNoteListItem(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(72.dp)
            .clip(RoundedCornerShape(AuraCornerRadius.Row))
            .auraShimmer()
    ) {}
}

@Composable
fun ShimmerTaskRow(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(56.dp)
            .clip(RoundedCornerShape(AuraCornerRadius.Row))
            .auraShimmer()
    ) {}
}

@Composable
fun ShimmerKanbanCard(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(110.dp)
            .clip(RoundedCornerShape(AuraCornerRadius.Flow))
            .auraShimmer()
    ) {}
}

@Composable
fun ShimmerMoneyOverviewCard(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(180.dp)
            .clip(RoundedCornerShape(AuraCornerRadius.Hero))
            .auraShimmer()
    ) {}
}

@Composable
fun ShimmerTransactionRow(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(64.dp)
            .clip(RoundedCornerShape(AuraCornerRadius.Row))
            .auraShimmer()
    ) {}
}

@Composable
fun ShimmerHabitCard(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(120.dp)
            .clip(RoundedCornerShape(AuraCornerRadius.Flow))
            .auraShimmer()
    ) {}
}

@Composable
fun ShimmerDashboardGrid(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Hero Card
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(160.dp)
                .clip(RoundedCornerShape(AuraCornerRadius.Hero))
                .auraShimmer()
        ) {}
        // Two-column grids
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(100.dp)
                    .clip(RoundedCornerShape(AuraCornerRadius.Card))
                    .auraShimmer()
            ) {}
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(100.dp)
                    .clip(RoundedCornerShape(AuraCornerRadius.Card))
                    .auraShimmer()
            ) {}
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(100.dp)
                    .clip(RoundedCornerShape(AuraCornerRadius.Card))
                    .auraShimmer()
            ) {}
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(100.dp)
                    .clip(RoundedCornerShape(AuraCornerRadius.Card))
                    .auraShimmer()
            ) {}
        }
    }
}

@Composable
fun ShimmerTimelineRow(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(72.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(16.dp)
                .clip(androidx.compose.foundation.shape.CircleShape)
                .auraShimmer()
        ) {}
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .clip(RoundedCornerShape(AuraCornerRadius.Card))
                .auraShimmer()
        ) {}
    }
}

