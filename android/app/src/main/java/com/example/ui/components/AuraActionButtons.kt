package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.anim.AuraCornerRadius
import com.example.ui.anim.auraSpringPress
import com.example.ui.theme.AuraTheme

/**
 * Primary filled pill action button with spring feedback and haptics.
 */
@Composable
fun AuraPrimaryAction(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    enabled: Boolean = true,
    containerColor: Color = AuraTheme.colors.accentBrand,
    contentColor: Color = Color.White
) {
    val shape = RoundedCornerShape(50.dp) // Pill shape

    Box(
        modifier = modifier
            .clip(shape)
            .then(
                if (enabled) {
                    Modifier.auraSpringPress(cornerRadius = 50.dp, onClick = onClick)
                } else {
                    Modifier
                }
            )
            .background(
                if (enabled) containerColor else containerColor.copy(alpha = 0.4f)
            )
            .padding(horizontal = 20.dp, vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = contentColor,
                    modifier = Modifier.size(18.dp)
                )
            }
            Text(
                text = text,
                style = MaterialTheme.typography.labelLarge,
                color = contentColor,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.5.sp
            )
        }
    }
}

/**
 * Secondary dashed-outline pill action button with spring feedback and haptics.
 * Used side-by-side with AuraPrimaryAction.
 */
@Composable
fun AuraSecondaryAction(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    enabled: Boolean = true,
    outlineColor: Color = AuraTheme.colors.cardBorder,
    contentColor: Color = AuraTheme.colors.textPrimary
) {
    val shape = RoundedCornerShape(50.dp) // Pill shape

    Box(
        modifier = modifier
            .clip(shape)
            .then(
                if (enabled) {
                    Modifier.auraSpringPress(cornerRadius = 50.dp, onClick = onClick)
                } else {
                    Modifier
                }
            )
            .background(AuraTheme.colors.bottomNavBackground)
            .border(
                width = 1.5.dp,
                color = if (enabled) outlineColor else outlineColor.copy(alpha = 0.3f),
                shape = shape
            )
            .padding(horizontal = 20.dp, vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = contentColor,
                    modifier = Modifier.size(18.dp)
                )
            }
            Text(
                text = text,
                style = MaterialTheme.typography.labelLarge,
                color = contentColor,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 0.5.sp
            )
        }
    }
}

/**
 * Standard paired action buttons (Primary + Secondary side by side).
 */
@Composable
fun AuraActionPair(
    primaryText: String,
    onPrimaryClick: () -> Unit,
    secondaryText: String,
    onSecondaryClick: () -> Unit,
    modifier: Modifier = Modifier,
    primaryIcon: ImageVector? = null,
    secondaryIcon: ImageVector? = null
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AuraSecondaryAction(
            text = secondaryText,
            onClick = onSecondaryClick,
            icon = secondaryIcon,
            modifier = Modifier.weight(1f)
        )
        AuraPrimaryAction(
            text = primaryText,
            onClick = onPrimaryClick,
            icon = primaryIcon,
            modifier = Modifier.weight(1f)
        )
    }
}

/**
 * Top Header PRO pill button inspired by reference design.
 */
@Composable
fun AuraProBadge(
    onClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(50.dp))
            .auraSpringPress(cornerRadius = 50.dp, onClick = onClick)
            .background(AuraTheme.colors.gold)
            .padding(horizontal = 14.dp, vertical = 7.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            Text(
                text = "✨",
                fontSize = 12.sp
            )
            Text(
                text = "PRO",
                style = MaterialTheme.typography.labelMedium,
                color = Color.Black,
                fontWeight = FontWeight.Black,
                letterSpacing = 1.sp
            )
        }
    }
}

/**
 * Top Header Profile Avatar button with accent outline.
 */
@Composable
fun AuraProfileAvatar(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 38.dp,
    accentColor: Color = AuraTheme.colors.accentBrand
) {
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .auraSpringPress(cornerRadius = 50.dp, onClick = onClick)
            .background(AuraTheme.colors.cardBackground)
            .border(width = 1.5.dp, color = accentColor.copy(alpha = 0.6f), shape = CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Outlined.Person,
            contentDescription = "User Profile",
            tint = accentColor,
            modifier = Modifier.size(size * 0.55f)
        )
    }
}

/**
 * Standard Header Action Cluster with PRO button + Profile Avatar.
 */
@Composable
fun AuraHeaderActions(
    onProClick: () -> Unit = {},
    onProfileClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        AuraProBadge(onClick = onProClick)
        AuraProfileAvatar(onClick = onProfileClick)
    }
}

/**
 * Floating Action Button (FAB) in Radiant Orange with spring physics.
 */
@Composable
fun AuraFloatingActionButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector = Icons.Default.Add,
    contentDescription: String = "Add Item",
    containerColor: Color = AuraTheme.colors.accentBrand,
    contentColor: Color = Color.White
) {
    Box(
        modifier = modifier
            .size(56.dp)
            .shadow(elevation = 12.dp, shape = CircleShape, spotColor = containerColor.copy(alpha = 0.5f))
            .clip(CircleShape)
            .auraSpringPress(cornerRadius = 50.dp, onClick = onClick)
            .background(containerColor),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = contentColor,
            modifier = Modifier.size(26.dp)
        )
    }
}

/**
 * Mini tag/badge pill (e.g. ★ DEFAULT in account cards).
 */
@Composable
fun AuraDefaultBadge(
    text: String = "DEFAULT",
    modifier: Modifier = Modifier,
    accentColor: Color = AuraTheme.colors.accentBrand
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(6.dp))
            .background(accentColor.copy(alpha = 0.15f))
            .border(width = 0.8.dp, color = accentColor.copy(alpha = 0.35f), shape = RoundedCornerShape(6.dp))
            .padding(horizontal = 6.dp, vertical = 2.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            Text(
                text = "★",
                fontSize = 9.sp,
                color = accentColor,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = text.uppercase(),
                style = MaterialTheme.typography.labelSmall,
                color = accentColor,
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.8.sp
            )
        }
    }
}
