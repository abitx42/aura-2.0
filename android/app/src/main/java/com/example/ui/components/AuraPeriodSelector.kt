package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.AuraHaptics
import com.example.ui.anim.auraSpringPress
import com.example.ui.theme.AuraTheme

/**
 * Horizontal scrollable pill selector with smooth animated state transitions
 * and tactile haptic feedback.
 */
@Composable
fun AuraPeriodSelector(
    items: List<String>,
    selectedIndex: Int,
    onItemSelected: (Int) -> Unit,
    modifier: Modifier = Modifier,
    accentColor: Color = AuraTheme.colors.accentBrand
) {
    val scrollState = rememberScrollState()
    val view = LocalView.current

    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(scrollState)
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        items.forEachIndexed { index, item ->
            val isSelected = index == selectedIndex
            val shape = RoundedCornerShape(50.dp)

            val backgroundColor by animateColorAsState(
                targetValue = if (isSelected) accentColor.copy(alpha = 0.18f) else AuraTheme.colors.cardBackground,
                animationSpec = tween(250, easing = FastOutSlowInEasing),
                label = "period_pill_bg"
            )

            val borderColor by animateColorAsState(
                targetValue = if (isSelected) accentColor.copy(alpha = 0.6f) else AuraTheme.colors.cardBorder,
                animationSpec = tween(250, easing = FastOutSlowInEasing),
                label = "period_pill_border"
            )

            val textColor by animateColorAsState(
                targetValue = if (isSelected) accentColor else AuraTheme.colors.textMuted,
                animationSpec = tween(250, easing = FastOutSlowInEasing),
                label = "period_pill_text"
            )

            Box(
                modifier = Modifier
                    .clip(shape)
                    .auraSpringPress(
                        cornerRadius = 50.dp,
                        onClick = {
                            if (!isSelected) {
                                AuraHaptics.triggerSubtleTick(view)
                                onItemSelected(index)
                            }
                        }
                    )
                    .background(backgroundColor)
                    .border(width = 1.dp, color = borderColor, shape = shape)
                    .padding(horizontal = 16.dp, vertical = 9.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = item.uppercase(),
                    style = MaterialTheme.typography.labelMedium,
                    color = textColor,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                    letterSpacing = 0.8.sp,
                    fontSize = 11.sp
                )
            }
        }
    }
}
