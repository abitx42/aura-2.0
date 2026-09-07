package com.example.ui.anim

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
fun AuraAnimatedSection(
    visible: Boolean,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(
            animationSpec = tween(
                durationMillis = AuraAnimTiming.ScreenTransition,
                easing = EaseOutCubic
            )
        ) + slideInVertically(
            animationSpec = tween(
                durationMillis = AuraAnimTiming.ScreenTransition,
                easing = EaseOutCubic
            ),
            initialOffsetY = { it / 12 }
        ) + scaleIn(
            animationSpec = tween(
                durationMillis = AuraAnimTiming.ScreenTransition,
                easing = EaseOutCubic
            ),
            initialScale = 0.96f
        ),
        exit = fadeOut(
            animationSpec = tween(
                durationMillis = 250,
                easing = EaseInCubic
            )
        ) + slideOutVertically(
            animationSpec = tween(
                durationMillis = 250,
                easing = EaseInCubic
            ),
            targetOffsetY = { it / 16 }
        ) + scaleOut(
            animationSpec = tween(
                durationMillis = 250,
                easing = EaseInCubic
            ),
            targetScale = 0.98f
        ),
        modifier = modifier
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            content()
        }
    }
}
