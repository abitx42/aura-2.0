package com.example.ui.anim

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.Spring
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.example.ui.AuraHaptics
import com.example.ui.theme.AuraTheme
import kotlinx.coroutines.launch
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * Dismissible insight card with soft gradient-glow edge treatment and smooth swipe physics.
 */
@Composable
fun AuraDismissibleCard(
    visible: Boolean,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    glowColor: Color = AuraTheme.colors.accentBrand,
    content: @Composable BoxScope.() -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    val view = LocalView.current
    val offsetX = remember { Animatable(0f) }

    AnimatedVisibility(
        visible = visible,
        exit = fadeOut() + shrinkVertically(),
        modifier = modifier
    ) {
        val shape = RoundedCornerShape(AuraCornerRadius.Card)

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .offset { IntOffset(offsetX.value.roundToInt(), 0) }
                .graphicsLayer {
                    val progress = (kotlin.math.abs(offsetX.value) / 400f).coerceIn(0f, 1f)
                    alpha = 1f - progress * 0.7f
                }
                .draggable(
                    state = rememberDraggableState { delta ->
                        coroutineScope.launch {
                            offsetX.snapTo(offsetX.value + delta)
                        }
                    },
                    orientation = Orientation.Horizontal,
                    onDragStopped = {
                        if (kotlin.math.abs(offsetX.value) > 250f) {
                            AuraHaptics.triggerConfirm(view)
                            onDismiss()
                        } else {
                            offsetX.animateTo(
                                targetValue = 0f,
                                animationSpec = spring(
                                    dampingRatio = Spring.DampingRatioMediumBouncy,
                                    stiffness = Spring.StiffnessMediumLow
                                )
                            )
                        }
                    }
                )
                .shadow(
                    elevation = 8.dp,
                    shape = shape,
                    ambientColor = glowColor.copy(alpha = 0.15f),
                    spotColor = glowColor.copy(alpha = 0.35f)
                )
                .clip(shape)
                .background(AuraTheme.colors.cardBackground)
                .border(
                    width = 1.dp,
                    color = AuraTheme.colors.cardBorder,
                    shape = shape
                )
                .padding(16.dp)
        ) {
            content()
        }
    }
}

@Composable
fun AuraDismissibleOverlay(
    visible: Boolean,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    if (!visible) return

    val coroutineScope = rememberCoroutineScope()
    val view = LocalView.current
    
    // Vertical offset tracking
    val offsetY = remember { Animatable(0f) }
    
    // Dismiss threshold (in pixels or dp) - 140dp translated roughly
    val dismissThreshold = 350f

    Box(
        modifier = modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectVerticalDragGestures(
                    onDragEnd = {
                        coroutineScope.launch {
                            if (offsetY.value > dismissThreshold) {
                                AuraHaptics.triggerConfirm(view)
                                onDismiss()
                                offsetY.snapTo(0f)
                            } else {
                                offsetY.animateTo(
                                    targetValue = 0f,
                                    animationSpec = spring(
                                        dampingRatio = Spring.DampingRatioMediumBouncy,
                                        stiffness = Spring.StiffnessMediumLow
                                    )
                                )
                            }
                        }
                    },
                    onDragCancel = {
                        coroutineScope.launch {
                            offsetY.animateTo(0f)
                        }
                    },
                    onVerticalDrag = { change, dragAmount ->
                        change.consume()
                        val newOffset = max(0f, offsetY.value + dragAmount)
                        coroutineScope.launch {
                            offsetY.snapTo(newOffset)
                        }
                    }
                )
            }
    ) {
        val currentOffset = offsetY.value
        val dragFraction = min(1f, currentOffset / 600f)
        val scrimAlpha = max(0f, min(0.6f, 0.6f * (1f - dragFraction)))
        val currentScale = 1f - (dragFraction * 0.08f)
        val currentRadius = (dragFraction * 24f).dp

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = scrimAlpha))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .offset { IntOffset(0, currentOffset.roundToInt()) }
                    .graphicsLayer {
                        scaleX = currentScale
                        scaleY = currentScale
                    }
                    .clip(RoundedCornerShape(currentRadius))
            ) {
                content()
            }
        }
    }
}

