package com.example.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Draw
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.KeyboardDoubleArrowRight
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Paid
import androidx.compose.material.icons.filled.TaskAlt
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.AuraPrimaryAction
import com.example.ui.theme.AuraTheme
import kotlinx.coroutines.launch

data class OnboardingPageData(
    val title: String,
    val subtitle: String,
    val description: String,
    val icon: ImageVector,
    val glowColor: Color,
    val visualContent: @Composable () -> Unit
)

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun OnboardingScreen(
    viewModel: AppViewModel,
    onFinished: () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    val pages = remember {
        listOf(
            OnboardingPageData(
                title = "ZEN SPACE",
                subtitle = "Mindful Journaling & Infinite Canvas",
                description = "Compose structured thoughts, customize with modern category tags, and sketch ideas directly onto an infinite pressure-sensitive vector canvas.",
                icon = Icons.Default.EditNote,
                glowColor = Color(0xFFFF5B32),
                visualContent = {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp)
                            .clip(RoundedCornerShape(24.dp))
                            .background(AuraTheme.colors.cardBackground)
                            .border(1.dp, AuraTheme.colors.cardBorder, RoundedCornerShape(24.dp))
                            .padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Draw,
                                contentDescription = null,
                                tint = AuraTheme.colors.accentBrand,
                                modifier = Modifier.size(64.dp)
                            )
                            Row(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(AuraTheme.colors.bottomNavBackground)
                                    .border(1.dp, AuraTheme.colors.cardBorder, RoundedCornerShape(12.dp))
                                    .padding(horizontal = 16.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(AuraTheme.colors.accentBrand)
                                )
                                Text(
                                    "Infinite Vector Sketching",
                                    color = AuraTheme.colors.textPrimary,
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            ),
            OnboardingPageData(
                title = "OBJECTIVES",
                subtitle = "Energy-Based Goal Planning",
                description = "Categorize plans based on your cognitive energy (High, Medium, or Low). Set recurring routines and complete objectives without clutter.",
                icon = Icons.Default.TaskAlt,
                glowColor = Color(0xFFFFB800),
                visualContent = {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp)
                            .clip(RoundedCornerShape(24.dp))
                            .background(AuraTheme.colors.cardBackground)
                            .border(1.dp, AuraTheme.colors.cardBorder, RoundedCornerShape(24.dp))
                            .padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            // High Energy item
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(AuraTheme.colors.bottomNavBackground)
                                    .border(1.dp, AuraTheme.colors.cardBorder, RoundedCornerShape(12.dp))
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = null,
                                        tint = AuraTheme.colors.positiveGreen,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Text("Refactor Aura core engine", color = AuraTheme.colors.textPrimary, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                                }
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(AuraTheme.colors.gold.copy(alpha = 0.2f))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text("HIGH ENERGY", color = AuraTheme.colors.gold, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                                }
                            }

                            // Low Energy item
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(AuraTheme.colors.bottomNavBackground)
                                    .border(1.dp, AuraTheme.colors.cardBorder, RoundedCornerShape(12.dp))
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = null,
                                        tint = AuraTheme.colors.textMuted,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Text("Review daily metrics log", color = AuraTheme.colors.textSecondary, style = MaterialTheme.typography.bodySmall)
                                }
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(AuraTheme.colors.accentBrand.copy(alpha = 0.15f))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text("LOW ENERGY", color = AuraTheme.colors.accentBrand, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            ),
            OnboardingPageData(
                title = "FLOW PASSBOOK",
                subtitle = "Unified Ledgers & Splitwise Rooms",
                description = "Track accounts dynamically, log multi-currency cash flows, and organize collaborative split rooms with zero cloud dependency.",
                icon = Icons.Default.Paid,
                glowColor = Color(0xFF00D084),
                visualContent = {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp)
                            .clip(RoundedCornerShape(24.dp))
                            .background(AuraTheme.colors.cardBackground)
                            .border(1.dp, AuraTheme.colors.cardBorder, RoundedCornerShape(24.dp))
                            .padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text("AURA PASSBOOK LEDGER", color = AuraTheme.colors.textMuted, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceEvenly
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("₹0.00", color = AuraTheme.colors.textPrimary, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Black)
                                    Text("Net Liquid Assets", color = AuraTheme.colors.accentBrand, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                                }
                            }
                            Row(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(AuraTheme.colors.bottomNavBackground)
                                    .border(1.dp, AuraTheme.colors.cardBorder, RoundedCornerShape(8.dp))
                                    .padding(horizontal = 12.dp, vertical = 4.dp),
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.LockOpen, contentDescription = null, tint = AuraTheme.colors.positiveGreen, modifier = Modifier.size(12.dp))
                                Text("Offline-First Vault", color = AuraTheme.colors.textSecondary, style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                }
            )
        )
    }

    val pagerState = rememberPagerState(pageCount = { pages.size })

    Scaffold(
        containerColor = AuraTheme.colors.screenBackground,
        modifier = Modifier.fillMaxSize()
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 24.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header: Skip button & Aura title
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "AURA",
                    color = AuraTheme.colors.textPrimary,
                    fontWeight = FontWeight.Black,
                    style = MaterialTheme.typography.titleLarge,
                    letterSpacing = 4.sp
                )

                if (pagerState.currentPage < pages.size - 1) {
                    Text(
                        text = "SKIP",
                        color = AuraTheme.colors.accentBrand,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp,
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable {
                                coroutineScope.launch {
                                    pagerState.scrollToPage(pages.size - 1)
                                }
                            }
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                } else {
                    Spacer(modifier = Modifier.size(1.dp))
                }
            }

            // Central pager
            HorizontalPager(
                state = pagerState,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) { pageIndex ->
                val page = pages[pageIndex]
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(vertical = 16.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Page central graphics container
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        page.visualContent()
                    }

                    // Page title and indicators
                    Text(
                        text = page.title,
                        color = AuraTheme.colors.accentBrand,
                        fontWeight = FontWeight.Black,
                        style = MaterialTheme.typography.headlineLarge,
                        letterSpacing = 2.sp,
                        textAlign = TextAlign.Center
                    )
                    
                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = page.subtitle,
                        color = AuraTheme.colors.textPrimary,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = page.description,
                        color = AuraTheme.colors.textSecondary,
                        style = MaterialTheme.typography.bodyMedium,
                        lineHeight = 20.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 12.dp)
                    )
                }
            }

            // Footer navigation row
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Page Indicator Dots (Axio pattern: active elongated pill, inactive circles)
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    pages.indices.forEach { idx ->
                        val isSelected = pagerState.currentPage == idx
                        val dotWidth by animateDpAsState(
                            targetValue = if (isSelected) 28.dp else 8.dp,
                            label = "dotWidth"
                        )
                        val dotColor = if (isSelected) AuraTheme.colors.accentBrand else AuraTheme.colors.cardBorder

                        Box(
                            modifier = Modifier
                                .height(8.dp)
                                .width(dotWidth)
                                .clip(CircleShape)
                                .background(dotColor)
                        )
                    }
                }

                // Interactive Primary Action CTA
                AnimatedContent(
                    targetState = pagerState.currentPage == pages.size - 1,
                    transitionSpec = {
                        fadeIn() togetherWith fadeOut()
                    },
                    label = "primaryButton"
                ) { isLastPage ->
                    if (isLastPage) {
                        AuraPrimaryAction(
                            text = "ENTER AURA SPACE",
                            icon = Icons.Default.KeyboardDoubleArrowRight,
                            onClick = {
                                viewModel.setHasSeenOnboarding(true)
                                onFinished()
                            },
                            modifier = Modifier.fillMaxWidth().height(52.dp),
                            containerColor = AuraTheme.colors.accentBrand
                        )
                    } else {
                        AuraPrimaryAction(
                            text = "NEXT CONFIGURATION",
                            icon = Icons.AutoMirrored.Filled.ArrowForward,
                            onClick = {
                                coroutineScope.launch {
                                    pagerState.animateScrollToPage(pagerState.currentPage + 1)
                                }
                            },
                            modifier = Modifier.fillMaxWidth().height(52.dp),
                            containerColor = AuraTheme.colors.accentBrand
                        )
                    }
                }
            }
        }
    }
}
