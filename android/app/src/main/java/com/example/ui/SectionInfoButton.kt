package com.example.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import com.example.ui.theme.AuraTheme

@Composable
fun AuraSectionInfoButton(
    viewModel: AppViewModel,
    title: String,
    description: String,
    modifier: Modifier = Modifier
) {
    val view = LocalView.current
    IconButton(
        onClick = {
            AuraHaptics.triggerSubtleTick(view)
            viewModel.showInfoSheet(title, description)
        },
        modifier = modifier
            .background(AuraTheme.colors.bottomNavBackground, CircleShape)
            .border(1.dp, AuraTheme.colors.cardBorder, CircleShape)
    ) {
        Icon(
            imageVector = Icons.Default.Info,
            contentDescription = "About $title Section",
            tint = AuraTheme.colors.accentBrand
        )
    }
}
