package com.example.ui.anim

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.ui.AppViewModel
import com.example.ui.AuraHaptics
import com.example.ui.Section
import com.example.ui.theme.*

data class NavItem(
    val section: Section,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val description: String,
    val testTag: String? = null
)

@Composable
fun AuraAnimatedNavBar(
    viewModel: AppViewModel,
    active: Section,
    modifier: Modifier = Modifier
) {
    val navItems = remember {
        listOf(
            NavItem(Section.Dashboard, Icons.Default.Home, "Home", "nav_home"),
            NavItem(Section.Notes, Icons.Default.StickyNote2, "Notes", "nav_notes"),
            NavItem(Section.Tasks, Icons.Default.CheckCircle, "Tasks", "nav_tasks"),
            NavItem(Section.Money, Icons.Default.AccountBalanceWallet, "Money", "nav_money"),
            NavItem(Section.Day, Icons.Default.MoreHoriz, "More", "nav_more")
        )
    }

    val activeIndex = remember(active) {
        val idx = navItems.indexOfFirst { it.section == active }
        if (idx != -1) idx else 0
    }

    Surface(
        color = AuraTheme.colors.bottomNavBackground,
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding(),
        tonalElevation = 8.dp
    ) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp, horizontal = 12.dp)
        ) {
            val totalWidth = maxWidth
            val tabWidth = totalWidth / navItems.size
            val view = LocalView.current

            // Animated sliding capsule background indicator
            val targetX = tabWidth * activeIndex
            val animatedX by animateDpAsState(
                targetValue = targetX,
                animationSpec = spring(
                    dampingRatio = 0.72f,
                    stiffness = 350f
                ),
                label = "nav_capsule_x"
            )

            // Outer capsule highlighting active tab
            Box(
                modifier = Modifier
                    .offset(x = animatedX)
                    .width(tabWidth)
                    .height(44.dp)
                    .padding(horizontal = 4.dp, vertical = 2.dp)
                    .shadow(
                        elevation = 6.dp,
                        shape = RoundedCornerShape(AuraCornerRadius.Card),
                        ambientColor = AuraTheme.colors.accentBrand.copy(alpha = 0.2f),
                        spotColor = AuraTheme.colors.accentBrand.copy(alpha = 0.35f)
                    )
                    .clip(RoundedCornerShape(AuraCornerRadius.Card))
                    .background(AuraTheme.colors.accentBrand.copy(alpha = 0.15f))
            )

            // Interactive Tabs Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                navItems.forEachIndexed { index, item ->
                    val isSelected = index == activeIndex

                    val iconScale by animateFloatAsState(
                        targetValue = if (isSelected) 1.15f else 1.0f,
                        animationSpec = spring(
                            dampingRatio = 0.6f,
                            stiffness = 500f
                        ),
                        label = "nav_icon_scale"
                    )

                    val tint = if (isSelected) AuraTheme.colors.accentBrand else AuraTheme.colors.textMuted

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp)
                            .clip(RoundedCornerShape(AuraCornerRadius.Card))
                            .clickable(
                                onClick = {
                                    if (!isSelected) {
                                        AuraHaptics.triggerSubtleTick(view)
                                        viewModel.navigateTo(item.section)
                                    }
                                }
                            )
                            .then(
                                if (item.testTag != null) Modifier.testTag(item.testTag) else Modifier
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = item.icon,
                            contentDescription = item.description,
                            tint = tint,
                            modifier = Modifier
                                .size(22.dp)
                                .graphicsLayer {
                                    scaleX = iconScale
                                    scaleY = iconScale
                                }
                        )
                    }
                }
            }
        }
    }
}
