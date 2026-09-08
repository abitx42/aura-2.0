package com.aura.personalos.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aura.personalos.BuildConfig
import com.aura.personalos.ui.anim.AuraCornerRadius
import com.aura.personalos.ui.anim.auraSpringPress
import com.aura.personalos.ui.theme.AuraTheme
import com.aura.personalos.ui.theme.ThemeMode
import com.aura.personalos.ui.theme.ThemePalette

@Composable
fun SettingsScreen(
    viewModel: AppViewModel,
    onNavigateToDebug: () -> Unit,
    modifier: Modifier = Modifier
) {
    val themeMode by viewModel.themeMode.collectAsState()
    val themePalette by viewModel.themePalette.collectAsState()
    val isOnline by viewModel.isOnline.collectAsState()
    val pendingOpsCount by viewModel.pendingOpsCount.collectAsState()

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(AuraTheme.colors.screenBackground)
            .padding(horizontal = 20.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 100.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        item {
            Column {
                Text(
                    text = "SYSTEM PREFERENCES",
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.5.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Settings & Identity",
                    style = MaterialTheme.typography.headlineLarge.copy(
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                )
            }
        }

        // Cloud Sync Status Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(AuraCornerRadius.Card),
                colors = CardDefaults.cardColors(containerColor = AuraTheme.colors.cardBackground),
                border = BorderStroke(1.dp, AuraTheme.colors.cardBorder)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(if (isOnline) AuraTheme.colors.positiveGreen else AuraTheme.colors.negativeRed)
                        )
                        Column {
                            Text(
                                text = if (isOnline) "CLOUD SYNC CONNECTED" else "OFFLINE MODE",
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            )
                            Text(
                                text = "Queue: \$pendingOpsCount pending operations",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            )
                        }
                    }

                    Button(
                        onClick = { viewModel.syncNow() },
                        modifier = Modifier.auraSpringPress(),
                        shape = RoundedCornerShape(AuraCornerRadius.Chip),
                        colors = ButtonDefaults.buttonColors(containerColor = AuraTheme.colors.accentBrand)
                    ) {
                        Text("SYNC 🔄", color = Color.Black, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Theme Display Mode
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(AuraCornerRadius.Card),
                colors = CardDefaults.cardColors(containerColor = AuraTheme.colors.cardBackground),
                border = BorderStroke(1.dp, AuraTheme.colors.cardBorder)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Text(
                        text = "DISPLAY MODE",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.2.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        ThemeModeOption("DARK 🌙", ThemeMode.DARK, themeMode) { viewModel.setThemeMode(it) }
                        ThemeModeOption("AMOLED 🖤", ThemeMode.AMOLED, themeMode) { viewModel.setThemeMode(it) }
                        ThemeModeOption("LIGHT ☀️", ThemeMode.LIGHT, themeMode) { viewModel.setThemeMode(it) }
                    }
                }
            }
        }

        // Theme Palette Selector
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(AuraCornerRadius.Card),
                colors = CardDefaults.cardColors(containerColor = AuraTheme.colors.cardBackground),
                border = BorderStroke(1.dp, AuraTheme.colors.cardBorder)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Text(
                        text = "ACCENT PALETTE",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.2.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        PaletteOption("CYAN", ThemePalette.CYAN_GLOW, themePalette) { viewModel.setThemePalette(it) }
                        PaletteOption("EMERALD", ThemePalette.EMERALD_GARDEN, themePalette) { viewModel.setThemePalette(it) }
                        PaletteOption("SUNSET", ThemePalette.RADIANT_SUNSET, themePalette) { viewModel.setThemePalette(it) }
                    }
                }
            }
        }

        // Founder Diagnostics Button (DEBUG ONLY)
        if (BuildConfig.DEBUG) {
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onNavigateToDebug() }
                        .auraSpringPress(),
                    shape = RoundedCornerShape(AuraCornerRadius.Card),
                    colors = CardDefaults.cardColors(containerColor = AuraTheme.colors.badgeGold.copy(alpha = 0.12f)),
                    border = BorderStroke(1.dp, AuraTheme.colors.badgeGold.copy(alpha = 0.4f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(18.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.BugReport,
                                contentDescription = null,
                                tint = AuraTheme.colors.badgeGold,
                                modifier = Modifier.size(24.dp)
                            )
                            Column {
                                Text(
                                    text = "FOUNDER DIAGNOSTICS & DEBUG 🛠️",
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        fontWeight = FontWeight.ExtraBold,
                                        color = AuraTheme.colors.badgeGold
                                    )
                                )
                                Text(
                                    text = "Inspect sync queue, db counts, and crash forensics",
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                )
                            }
                        }

                        Icon(
                            imageVector = Icons.Default.ChevronRight,
                            contentDescription = null,
                            tint = AuraTheme.colors.badgeGold
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun RowScope.ThemeModeOption(
    label: String,
    mode: ThemeMode,
    currentMode: ThemeMode,
    onSelect: (ThemeMode) -> Unit
) {
    val selected = mode == currentMode
    OutlinedButton(
        onClick = { onSelect(mode) },
        modifier = Modifier
            .weight(1f)
            .auraSpringPress(),
        shape = RoundedCornerShape(AuraCornerRadius.Chip),
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = if (selected) AuraTheme.colors.accentBrand.copy(alpha = 0.15f) else Color.Transparent
        ),
        border = BorderStroke(1.dp, if (selected) AuraTheme.colors.accentBrand else AuraTheme.colors.cardBorder)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = if (selected) FontWeight.ExtraBold else FontWeight.Medium,
                color = if (selected) AuraTheme.colors.accentBrand else MaterialTheme.colorScheme.onSurfaceVariant
            )
        )
    }
}

@Composable
fun RowScope.PaletteOption(
    label: String,
    palette: ThemePalette,
    currentPalette: ThemePalette,
    onSelect: (ThemePalette) -> Unit
) {
    val selected = palette == currentPalette
    OutlinedButton(
        onClick = { onSelect(palette) },
        modifier = Modifier
            .weight(1f)
            .auraSpringPress(),
        shape = RoundedCornerShape(AuraCornerRadius.Chip),
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = if (selected) AuraTheme.colors.accentBrand.copy(alpha = 0.15f) else Color.Transparent
        ),
        border = BorderStroke(1.dp, if (selected) AuraTheme.colors.accentBrand else AuraTheme.colors.cardBorder)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = if (selected) FontWeight.ExtraBold else FontWeight.Medium,
                color = if (selected) AuraTheme.colors.accentBrand else MaterialTheme.colorScheme.onSurfaceVariant
            )
        )
    }
}
