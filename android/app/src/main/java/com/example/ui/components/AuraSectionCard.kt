package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Divider
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.anim.AuraCornerRadius
import com.example.ui.anim.auraSpringPress
import com.example.ui.theme.AuraTheme

/**
 * Data item definition for grouped section cards.
 */
data class SectionRowItem(
    val title: String,
    val description: String,
    val icon: ImageVector,
    val onClick: () -> Unit = {},
    val badge: String? = null,
    val iconTint: Color? = null
)

/**
 * Grouped section container with tracked eyebrow header (inspired by reference design).
 * e.g., SHARING & COLLABORATION, ORGANIZATION & TOOLS, SECURITY & SUPPORT.
 */
@Composable
fun AuraSectionGroup(
    eyebrow: String,
    items: List<SectionRowItem>,
    modifier: Modifier = Modifier,
    accentColor: Color = AuraTheme.colors.accentBrand
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Section Eyebrow Header
        Text(
            text = eyebrow.uppercase(),
            style = MaterialTheme.typography.labelSmall,
            color = AuraTheme.colors.textMuted,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.2.sp,
            modifier = Modifier.padding(horizontal = 4.dp)
        )

        // Card Container holding items
        val shape = RoundedCornerShape(AuraCornerRadius.Card)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(shape)
                .background(AuraTheme.colors.cardBackground)
                .border(width = 1.dp, color = AuraTheme.colors.cardBorder, shape = shape)
                .padding(vertical = 4.dp)
        ) {
            items.forEachIndexed { index, item ->
                AuraSectionRow(
                    title = item.title,
                    description = item.description,
                    icon = item.icon,
                    onClick = item.onClick,
                    iconTint = item.iconTint ?: accentColor,
                    badge = item.badge
                )

                if (index < items.size - 1) {
                    HorizontalDivider(
                        modifier = Modifier.padding(start = 56.dp, end = 16.dp),
                        color = AuraTheme.colors.cardBorder.copy(alpha = 0.5f),
                        thickness = 0.8.dp
                    )
                }
            }
        }
    }
}

/**
 * Individual row item inside a section group.
 */
@Composable
fun AuraSectionRow(
    title: String,
    description: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    iconTint: Color = AuraTheme.colors.accentBrand,
    badge: String? = null,
    trailingContent: (@Composable () -> Unit)? = null
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .auraSpringPress(cornerRadius = 12.dp, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Squircle Icon
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(iconTint.copy(alpha = 0.14f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconTint,
                modifier = Modifier.size(20.dp)
            )
        }

        // Title & Description
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = AuraTheme.colors.textPrimary,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 15.sp
                )
                if (!badge.isNullOrBlank()) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(iconTint.copy(alpha = 0.18f))
                            .padding(horizontal = 5.dp, vertical = 1.dp)
                    ) {
                        Text(
                            text = badge.uppercase(),
                            style = MaterialTheme.typography.labelSmall,
                            color = iconTint,
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            if (description.isNotBlank()) {
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = AuraTheme.colors.textSecondary,
                    fontSize = 12.sp,
                    lineHeight = 16.sp
                )
            }
        }

        if (trailingContent != null) {
            trailingContent()
        }
    }
}
