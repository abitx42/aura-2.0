package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.anim.AuraCornerRadius
import com.example.ui.anim.auraSpringPress
import com.example.ui.theme.AuraTheme

/**
 * Modern numbered stat card matching reference pattern:
 * e.g., '01 · SPENT' tracked eyebrow tag, large bold metric, and caption.
 */
@Composable
fun AuraNumberedStat(
    index: String,
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    caption: String? = null,
    accentColor: Color = MaterialTheme.colorScheme.primary,
    onClick: (() -> Unit)? = null
) {
    val shape = RoundedCornerShape(AuraCornerRadius.Card)
    val pressModifier = if (onClick != null) {
        Modifier.auraSpringPress(cornerRadius = AuraCornerRadius.Card, onClick = onClick)
    } else {
        Modifier
    }

    Column(
        modifier = modifier
            .then(pressModifier)
            .clip(shape)
            .background(AuraTheme.colors.cardBackground)
            .border(
                width = 1.dp,
                color = AuraTheme.colors.cardBorder,
                shape = shape
            )
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        // Tracked Eyebrow Badge (e.g., '01 · SPENT')
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(AuraCornerRadius.Chip))
                    .background(accentColor.copy(alpha = 0.15f))
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
                Text(
                    text = index,
                    style = MaterialTheme.typography.labelSmall,
                    color = accentColor,
                    fontWeight = FontWeight.Black
                )
            }

            Text(
                text = label.uppercase(),
                style = MaterialTheme.typography.labelSmall,
                color = AuraTheme.colors.textMuted,
                letterSpacing = 1.2.sp
            )
        }

        // Big Primary Value
        Text(
            text = value,
            style = MaterialTheme.typography.titleLarge,
            color = AuraTheme.colors.textPrimary,
            fontWeight = FontWeight.Bold,
            maxLines = 1
        )

        // Optional Caption / Subtitle
        if (!caption.isNullOrBlank()) {
            Text(
                text = caption,
                style = MaterialTheme.typography.bodySmall,
                color = AuraTheme.colors.textSecondary,
                maxLines = 1
            )
        }
    }
}
