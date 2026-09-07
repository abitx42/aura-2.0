package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
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
 * 2-column grid hub card for modules (Split, Links, Categories, Budgets, Loans, Savings)
 * directly styled after the Ecosystem Toolbox reference design.
 */
@Composable
fun AuraHubCard(
    title: String,
    statText: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    accentColor: Color = AuraTheme.colors.accentBrand
) {
    val shape = RoundedCornerShape(20.dp)

    Box(
        modifier = modifier
            .clip(shape)
            .auraSpringPress(cornerRadius = 20.dp, onClick = onClick)
            .background(AuraTheme.colors.cardBackground)
            .border(
                width = 1.dp,
                color = AuraTheme.colors.cardBorder,
                shape = shape
            )
    ) {
        // Watermark decorative icon in background
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = accentColor.copy(alpha = 0.05f),
            modifier = Modifier
                .size(86.dp)
                .align(Alignment.BottomEnd)
                .offset(x = 12.dp, y = 12.dp)
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Squircle Icon Container
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(accentColor.copy(alpha = 0.14f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = AuraTheme.colors.textPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp,
                    maxLines = 1
                )
                Text(
                    text = statText.uppercase(),
                    style = MaterialTheme.typography.labelSmall,
                    color = AuraTheme.colors.textMuted,
                    letterSpacing = 1.sp,
                    fontSize = 10.sp,
                    maxLines = 1
                )
            }
        }
    }
}
