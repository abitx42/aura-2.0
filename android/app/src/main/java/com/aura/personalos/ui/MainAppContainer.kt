package com.aura.personalos.ui

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.DialogProperties
import com.aura.personalos.BuildConfig
import com.aura.personalos.data.Subtask
import com.aura.personalos.data.Task
import com.aura.personalos.ui.anim.AuraCornerRadius
import com.aura.personalos.ui.anim.auraSpringPress
import com.aura.personalos.ui.components.AuraProgressRing
import com.aura.personalos.ui.theme.AuraTheme
import java.util.Locale

@Composable
fun MainAppContainer(
    viewModel: AppViewModel,
    modifier: Modifier = Modifier
) {
    val currentSection by viewModel.currentSection.collectAsState()
    val isFocusOverlayVisible by viewModel.isFocusOverlayVisible.collectAsState()
    val isNightReviewOpen by viewModel.isNightReviewOpen.collectAsState()
    val isTaskComposerOpen by viewModel.isTaskComposerOpen.collectAsState()
    val selectedEditTask by viewModel.selectedEditTask.collectAsState()

    AuraErrorBoundary {
        Scaffold(
            modifier = modifier.fillMaxSize(),
            containerColor = AuraTheme.colors.screenBackground,
            bottomBar = {
                if (currentSection != Section.Debug) {
                    PersonalOsBottomBar(
                        activeSection = currentSection,
                        onSelectSection = { viewModel.navigateTo(it) }
                    )
                }
            }
        ) { paddingValues ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                when (currentSection) {
                    Section.Today -> {
                        TodayScreen(
                            viewModel = viewModel,
                            onOpenPlanTomorrow = { viewModel.navigateTo(Section.Plan) },
                            onOpenNightReview = { viewModel.openNightReview() }
                        )
                    }
                    Section.Plan -> {
                        TasksScreen(
                            viewModel = viewModel,
                            onOpenTaskComposer = { viewModel.openTaskComposer(it) }
                        )
                    }
                    Section.Brain -> {
                        BrainScreen(viewModel = viewModel)
                    }
                    Section.Settings -> {
                        SettingsScreen(
                            viewModel = viewModel,
                            onNavigateToDebug = { viewModel.navigateTo(Section.Debug) }
                        )
                    }
                    Section.Debug -> {
                        if (BuildConfig.DEBUG) {
                            DebugScreen(
                                viewModel = viewModel,
                                onBack = { viewModel.navigateTo(Section.Settings) }
                            )
                        } else {
                            viewModel.navigateTo(Section.Today)
                        }
                    }
                }

                // Night Review Modal (ADR-013)
                if (isNightReviewOpen) {
                    NightReviewDialog(
                        viewModel = viewModel,
                        onDismiss = { viewModel.closeNightReview() }
                    )
                }

                // Focus Execution Modal (ADR-012)
                if (isFocusOverlayVisible) {
                    FocusExecutionModal(
                        viewModel = viewModel,
                        onDismiss = { viewModel.hideFocusOverlay() }
                    )
                }

                // Task Composer / Quick Add
                if (isTaskComposerOpen) {
                    TaskComposerScreen(
                        task = selectedEditTask,
                        viewModel = viewModel,
                        onBack = { viewModel.closeTaskComposer() }
                    )
                }
            }
        }
    }
}

data class NavigationTabItem(
    val section: Section,
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
)

@Composable
fun PersonalOsBottomBar(
    activeSection: Section,
    onSelectSection: (Section) -> Unit
) {
    val items = remember {
        listOf(
            NavigationTabItem(Section.Today, "Today", Icons.Filled.WbSunny, Icons.Outlined.WbSunny),
            NavigationTabItem(Section.Plan, "Plan", Icons.Filled.CalendarMonth, Icons.Outlined.CalendarMonth),
            NavigationTabItem(Section.Brain, "Brain", Icons.Filled.Psychology, Icons.Outlined.Psychology),
            NavigationTabItem(Section.Settings, "Settings", Icons.Filled.Settings, Icons.Outlined.Settings)
        )
    }

    NavigationBar(
        containerColor = AuraTheme.colors.screenBackground,
        contentColor = MaterialTheme.colorScheme.onSurface,
        tonalElevation = 8.dp,
        modifier = Modifier.border(
            BorderStroke(1.dp, AuraTheme.colors.cardBorder),
            RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
        )
    ) {
        items.forEach { item ->
            val isSelected = activeSection == item.section
            NavigationBarItem(
                selected = isSelected,
                onClick = { onSelectSection(item.section) },
                icon = {
                    Icon(
                        imageVector = if (isSelected) item.selectedIcon else item.unselectedIcon,
                        contentDescription = item.label,
                        tint = if (isSelected) AuraTheme.colors.accentBrand else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                },
                label = {
                    Text(
                        text = item.label,
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = if (isSelected) AuraTheme.colors.accentBrand else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )
                },
                colors = NavigationBarItemDefaults.colors(
                    indicatorColor = AuraTheme.colors.accentBrand.copy(alpha = 0.15f)
                )
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FocusExecutionModal(
    viewModel: AppViewModel,
    onDismiss: () -> Unit
) {
    val currentFocusTask by viewModel.currentFocusTask.collectAsState()
    val isTimerRunning by viewModel.isFocusTimerRunning.collectAsState()
    val timerSeconds by viewModel.focusTimerSeconds.collectAsState()
    val targetSeconds by viewModel.focusSessionTargetSeconds.collectAsState()
    val subtasks by viewModel.currentFocusSubtasks.collectAsState()
    var newSubtaskText by remember { mutableStateOf("") }

    if (currentFocusTask == null) {
        LaunchedEffect(Unit) { onDismiss() }
        return
    }
    val task = currentFocusTask!!

    val minutes = timerSeconds / 60
    val seconds = timerSeconds % 60
    val formattedTime = String.format(Locale.US, "%02d:%02d", minutes, seconds)
    val elapsed = (targetSeconds - timerSeconds).coerceAtLeast(0)
    val progress = if (targetSeconds > 0) (elapsed.toFloat() / targetSeconds.toFloat()).coerceIn(0f, 1f) else 0f

    AlertDialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
        modifier = Modifier
            .fillMaxWidth(0.94f)
            .padding(vertical = 16.dp),
        content = {
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = AuraTheme.colors.screenBackground),
                border = BorderStroke(1.5.dp, AuraTheme.colors.accentBrand.copy(alpha = 0.8f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Header
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .clip(CircleShape)
                                    .background(if (isTimerRunning) AuraTheme.colors.positiveGreen else AuraTheme.colors.accentBrand)
                            )
                            Text(
                                text = if (isTimerRunning) "FOCUS MODE ACTIVE" else "FOCUS PAUSED",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Black,
                                color = if (isTimerRunning) AuraTheme.colors.positiveGreen else AuraTheme.colors.accentBrand,
                                letterSpacing = 1.2.sp
                            )
                        }

                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Minimize",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    // Large circular progress & countdown
                    // Large circular progress & countdown
                    AuraProgressRing(
                        progress = progress,
                        mainText = formattedTime,
                        subText = if (isTimerRunning) "Target: ${targetSeconds / 60}m" else "Tap Play to Resume",
                        size = 200.dp,
                        strokeWidth = 12.dp,
                        progressColor = AuraTheme.colors.accentBrand,
                        trackColor = AuraTheme.colors.cardBorder
                    )

                    // Task details
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = task.title,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (task.description.isNotBlank()) {
                            Text(
                                text = task.description,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    // Subtasks checklist
                    if (subtasks.isNotEmpty()) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(AuraCornerRadius.Card))
                                .background(AuraTheme.colors.cardBackground)
                                .padding(12.dp)
                        ) {
                            Text(
                                text = "CHECKLIST (\${subtasks.count { it.isCompleted }}/\${subtasks.size})",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(6.dp))

                            subtasks.forEach { sub ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { viewModel.toggleSubtaskCompleted(sub) }
                                        .padding(vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Checkbox(
                                        checked = sub.isCompleted,
                                        onCheckedChange = { viewModel.toggleSubtaskCompleted(sub) },
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Text(
                                        text = sub.title,
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            color = if (sub.isCompleted) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
                                            textDecoration = if (sub.isCompleted) TextDecoration.LineThrough else TextDecoration.None
                                        )
                                    )
                                }
                            }
                        }
                    }

                    // Controls
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = { viewModel.extendFocusTimer(5) },
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(AuraTheme.colors.cardBackground)
                                .auraSpringPress()
                        ) {
                            Text("+5m", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = AuraTheme.colors.accentBrand)
                        }

                        IconButton(
                            onClick = { viewModel.toggleFocusTimer() },
                            modifier = Modifier
                                .size(60.dp)
                                .clip(CircleShape)
                                .background(AuraTheme.colors.accentBrand)
                                .auraSpringPress()
                        ) {
                            Icon(
                                imageVector = if (isTimerRunning) Icons.Default.Pause else Icons.Default.PlayArrow,
                                contentDescription = if (isTimerRunning) "Pause" else "Resume",
                                tint = Color.Black,
                                modifier = Modifier.size(32.dp)
                            )
                        }

                        IconButton(
                            onClick = { viewModel.skipCurrentFocus(task) },
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(AuraTheme.colors.cardBackground)
                                .auraSpringPress()
                        ) {
                            Icon(
                                imageVector = Icons.Default.SkipNext,
                                contentDescription = "Skip",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    // Complete Button
                    Button(
                        onClick = { viewModel.completeCurrentFocus(task) },
                        shape = RoundedCornerShape(AuraCornerRadius.Chip),
                        colors = ButtonDefaults.buttonColors(containerColor = AuraTheme.colors.positiveGreen),
                        modifier = Modifier
                            .fillMaxWidth()
                            .auraSpringPress()
                    ) {
                        Icon(Icons.Default.Check, contentDescription = null, tint = Color.Black)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "MARK AS COMPLETED",
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.Black
                        )
                    }
                }
            }
        }
    )
}
