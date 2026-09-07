package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.example.ui.anim.AuraCornerRadius
import com.example.ui.anim.auraShimmer
import com.example.ui.theme.AuraTheme

/**
 * Standard loading shimmer placeholder states for screens and components.
 */
object AuraLoadingState {

    @Composable
    fun Card(
        modifier: Modifier = Modifier,
        height: androidx.compose.ui.unit.Dp = 120.dp
    ) {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .height(height)
                .clip(RoundedCornerShape(AuraCornerRadius.Card))
                .auraShimmer(
                    baseColor = AuraTheme.colors.cardBackground,
                    highlightColor = AuraTheme.colors.cardBorder
                )
        )
    }

    @Composable
    fun List(
        itemCount: Int = 4,
        modifier: Modifier = Modifier
    ) {
        Column(
            modifier = modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            repeat(itemCount) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(68.dp)
                        .clip(RoundedCornerShape(AuraCornerRadius.Row))
                        .auraShimmer(
                            baseColor = AuraTheme.colors.cardBackground,
                            highlightColor = AuraTheme.colors.cardBorder
                        ),
                    verticalAlignment = Alignment.CenterVertically
                ) {}
            }
        }
    }

    @Composable
    fun HeroOverview(modifier: Modifier = Modifier) {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .height(180.dp)
                .clip(RoundedCornerShape(AuraCornerRadius.Hero))
                .auraShimmer(
                    baseColor = AuraTheme.colors.cardBackground,
                    highlightColor = AuraTheme.colors.cardBorder
                )
        )
    }

    @Composable
    fun HubGrid(modifier: Modifier = Modifier) {
        Column(
            modifier = modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            repeat(3) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(110.dp)
                            .clip(RoundedCornerShape(AuraCornerRadius.Card))
                            .auraShimmer(
                                baseColor = AuraTheme.colors.cardBackground,
                                highlightColor = AuraTheme.colors.cardBorder
                            )
                    )
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(110.dp)
                            .clip(RoundedCornerShape(AuraCornerRadius.Card))
                            .auraShimmer(
                                baseColor = AuraTheme.colors.cardBackground,
                                highlightColor = AuraTheme.colors.cardBorder
                            )
                    )
                }
            }
        }
    }
}
