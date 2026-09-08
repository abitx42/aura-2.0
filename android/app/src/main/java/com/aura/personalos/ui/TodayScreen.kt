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
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aura.personalos.data.Subtask
import com.aura.personalos.data.Task
import com.aura.personalos.ui.anim.AuraCornerRadius
import com.aura.personalos.ui.anim.auraSpringPress
import com.aura.personalos.ui.components.*
import com.aura.personalos.ui.theme.AuraTheme
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun TodayScreen(
    viewModel: AppViewModel,
    onOpenPlanTomorrow: () -> Unit,
    onOpenNightReview: () -> Unit,
    modifier: Modifier = Modifier
) {
    val currentFocusTask by viewModel.currentFocusTask.collectAsState()
    val isCurrentFocusMissed by viewModel.isCurrentFocusMissed.collectAsState()
    val nextUpTask by viewModel.nextUpTask.collectAsState()
    val subtasks by viewModel.currentFocusSubtasks.collectAsState()
    val todayPlan by viewModel.todayPlan.collectAsState()
    val todayTasks by viewModel.allTasks.collectAsState()
    val timerSeconds by viewModel.focusTimerSeconds.collectAsState()
    val isTimerRunning by viewModel.isFocusTimerRunning.collectAsState()

    val filteredTodayTasks = remember(todayTasks, viewModel.todayString) {
        todayTasks.filter { it.date == viewModel.todayString }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(AuraTheme.colors.screenBackground)
            .padding(horizontal = 20.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 100.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        // 1. Dynamic Greeting Header
        item {
            TodayHeader(
                todayString = viewModel.todayString,
                onSettingsClick = { viewModel.navigateTo(Section.Settings) }
            )
        }

        // 2. Plan Status Banner (Locked, Active, Evening Review)
        item {
            PlanStatusBanner(
                planStatus = todayPlan?.status ?: "NONE",
                accuracy = todayPlan?.planAccuracyPercent,
                onStartDay = { viewModel.startMyDay() },
                onOpenReview = onOpenNightReview,
                onOpenPlan = onOpenPlanTomorrow
            )
        }

        // 3. Current Focus Hero Card
        item {
            if (currentFocusTask != null) {
                CurrentFocusHeroCard(
                    task = currentFocusTask!!,
                    isMissed = isCurrentFocusMissed,
                    subtasks = subtasks,
                    timerSeconds = timerSeconds,
                    isTimerRunning = isTimerRunning,
                    onToggleTimer = { viewModel.toggleFocusTimer() },
                    onComplete = { viewModel.completeCurrentFocus(currentFocusTask!!) },
                    onSkip = { viewModel.skipCurrentFocus(currentFocusTask!!) },
                    onOpenFocusOverlay = { viewModel.showFocusOverlay() },
                    onToggleSubtask = { viewModel.toggleSubtaskCompleted(it) }
                )
            } else {
                AuraEmptyState(
                    title = "All Planned Focus Completed",
                    description = "You have finished all commitments for today. Prepare tomorrow deliberately.",
                    icon = Icons.Default.DoneAll,
                    modifier = Modifier.padding(vertical = 12.dp),
                    actionButton = {
                        AuraPrimaryAction(
                            text = "PLAN TOMORROW 🌙",
                            onClick = onOpenPlanTomorrow
                        )
                    }
                )
            }
        }

        // 4. Next Up Card
        if (nextUpTask != null) {
            item {
                NextUpPreviewCard(task = nextUpTask!!)
            }
        }

        // 5. Today's Task Backlog
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "TODAY'S COMMITMENTS (\${filteredTodayTasks.count { it.isCompleted }}/\${filteredTodayTasks.size})",
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.2.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                )
                TextButton(
                    onClick = onOpenPlanTomorrow,
                    modifier = Modifier.auraSpringPress()
                ) {
                    Text(
                        text = "MANAGE PLAN",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = AuraTheme.colors.accentBrand
                        )
                    )
                }
            }
        }

        items(filteredTodayTasks, key = { it.id }) { task ->
            TodayTaskRow(
                task = task,
                isCurrentFocus = currentFocusTask?.id == task.id,
                onToggle = { viewModel.toggleTaskCompleted(task) }
            )
        }
    }
}

@Composable
fun TodayHeader(
    todayString: String,
    onSettingsClick: () -> Unit
) {
    val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
    val greeting = when {
        hour < 12 -> "Good Morning"
        hour < 17 -> "Good Afternoon"
        else -> "Good Evening"
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(AuraTheme.colors.positiveGreen)
                )
                Text(
                    text = todayString.uppercase(),
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.5.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = greeting,
                style = MaterialTheme.typography.headlineLarge.copy(
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.onSurface
                )
            )
        }

        IconButton(
            onClick = onSettingsClick,
            modifier = Modifier
                .size(44.dp)
                .auraSpringPress()
        ) {
            Icon(
                imageVector = Icons.Default.Settings,
                contentDescription = "Settings",
                tint = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
fun PlanStatusBanner(
    planStatus: String,
    accuracy: Int?,
    onStartDay: () -> Unit,
    onOpenReview: () -> Unit,
    onOpenPlan: () -> Unit
) {
    val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
    val isEvening = hour >= 18

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(AuraCornerRadius.Card),
        colors = CardDefaults.cardColors(
            containerColor = when (planStatus) {
                "LOCKED" -> AuraTheme.colors.badgeGold.copy(alpha = 0.15f)
                "ACTIVE" -> if (isEvening) AuraTheme.colors.accentBrand.copy(alpha = 0.15f) else AuraTheme.colors.cardBackground
                "REVIEWED" -> AuraTheme.colors.positiveGreen.copy(alpha = 0.15f)
                else -> AuraTheme.colors.cardBackground
            }
        ),
        border = BorderStroke(
            1.dp,
            when (planStatus) {
                "LOCKED" -> AuraTheme.colors.badgeGold.copy(alpha = 0.4f)
                "REVIEWED" -> AuraTheme.colors.positiveGreen.copy(alpha = 0.4f)
                else -> AuraTheme.colors.cardBorder
            }
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.weight(1f)
            ) {
                Icon(
                    imageVector = when (planStatus) {
                        "LOCKED" -> Icons.Default.Lock
                        "ACTIVE" -> if (isEvening) Icons.Default.NightsStay else Icons.Default.WbSunny
                        "REVIEWED" -> Icons.Default.CheckCircle
                        else -> Icons.Default.EditCalendar
                    },
                    contentDescription = null,
                    tint = when (planStatus) {
                        "LOCKED" -> AuraTheme.colors.badgeGold
                        "REVIEWED" -> AuraTheme.colors.positiveGreen
                        else -> AuraTheme.colors.accentBrand
                    },
                    modifier = Modifier.size(24.dp)
                )

                Column {
                    Text(
                        text = when (planStatus) {
                            "LOCKED" -> "PLAN LOCKED 🔒"
                            "ACTIVE" -> if (isEvening) "EVENING REVIEW READY 🌙" else "PLAN ACTIVE ☀️"
                            "REVIEWED" -> "DAY REVIEWED (accuracy \${accuracy ?: 100}%)"
                            else -> "NO PLAN LOCKED"
                        },
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    )
                    Text(
                        text = when (planStatus) {
                            "LOCKED" -> "Ready to execute your intentional day"
                            "ACTIVE" -> if (isEvening) "Compare intention with reality" else "Focus on current priority"
                            "REVIEWED" -> "Review complete, bridge to tomorrow"
                            else -> "Plan tomorrow tonight to run your day deliberately"
                        },
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )
                }
            }

            when (planStatus) {
                "LOCKED" -> {
                    Button(
                        onClick = onStartDay,
                        modifier = Modifier.auraSpringPress(),
                        shape = RoundedCornerShape(AuraCornerRadius.Chip),
                        colors = ButtonDefaults.buttonColors(containerColor = AuraTheme.colors.badgeGold)
                    ) {
                        Text("START 🚀", color = Color.Black, fontWeight = FontWeight.Bold)
                    }
                }
                "ACTIVE" -> {
                    if (isEvening) {
                        Button(
                            onClick = onOpenReview,
                            modifier = Modifier.auraSpringPress(),
                            shape = RoundedCornerShape(AuraCornerRadius.Chip),
                            colors = ButtonDefaults.buttonColors(containerColor = AuraTheme.colors.accentBrand)
                        ) {
                            Text("REVIEW 🌙", color = Color.Black, fontWeight = FontWeight.Bold)
                        }
                    }
                }
                "NONE", "DRAFT" -> {
                    Button(
                        onClick = onOpenPlan,
                        modifier = Modifier.auraSpringPress(),
                        shape = RoundedCornerShape(AuraCornerRadius.Chip),
                        colors = ButtonDefaults.buttonColors(containerColor = AuraTheme.colors.accentBrand)
                    ) {
                        Text("PLAN 📅", color = Color.Black, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun CurrentFocusHeroCard(
    task: Task,
    isMissed: Boolean,
    subtasks: List<Subtask>,
    timerSeconds: Int,
    isTimerRunning: Boolean,
    onToggleTimer: () -> Unit,
    onComplete: () -> Unit,
    onSkip: () -> Unit,
    onOpenFocusOverlay: () -> Unit,
    onToggleSubtask: (Subtask) -> Unit
) {
    val minutes = timerSeconds / 60
    val seconds = timerSeconds % 60
    val timeFormatted = String.format(Locale.US, "%02d:%02d", minutes, seconds)

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(AuraCornerRadius.Hero),
        colors = CardDefaults.cardColors(containerColor = AuraTheme.colors.cardBackground),
        border = BorderStroke(
            1.5.dp,
            if (isMissed) AuraTheme.colors.negativeRed.copy(alpha = 0.6f) else AuraTheme.colors.accentBrand.copy(alpha = 0.5f)
        )
    ) {
        Column(modifier = Modifier.padding(22.dp)) {
            // Eyebrow
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(if (isMissed) AuraTheme.colors.negativeRed else AuraTheme.colors.accentBrand)
                    )
                    Text(
                        text = if (isMissed) "MISSED TIME BLOCK ⚠️" else "CURRENT FOCUS ⚡",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 1.2.sp,
                            color = if (isMissed) AuraTheme.colors.negativeRed else AuraTheme.colors.accentBrand
                        )
                    )
                }

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = AuraTheme.colors.screenBackground
                ) {
                    Text(
                        text = task.priority.uppercase(),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Task Title
            Text(
                text = task.title,
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.onSurface
                ),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            if (task.description.isNotBlank()) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = task.description,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    ),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Timer display row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.clickable { onOpenFocusOverlay() }
                ) {
                    Icon(
                        imageVector = Icons.Default.Timer,
                        contentDescription = null,
                        tint = AuraTheme.colors.accentBrand,
                        modifier = Modifier.size(28.dp)
                    )
                    Text(
                        text = timeFormatted,
                        style = MaterialTheme.typography.headlineLarge.copy(
                            fontWeight = FontWeight.Black,
                            letterSpacing = 2.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    )
                }

                IconButton(
                    onClick = onToggleTimer,
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(if (isTimerRunning) AuraTheme.colors.negativeRed.copy(alpha = 0.2f) else AuraTheme.colors.accentBrand.copy(alpha = 0.2f))
                        .auraSpringPress()
                ) {
                    Icon(
                        imageVector = if (isTimerRunning) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = if (isTimerRunning) "Pause" else "Play",
                        tint = if (isTimerRunning) AuraTheme.colors.negativeRed else AuraTheme.colors.accentBrand
                    )
                }
            }

            // Subtasks checklist if available
            if (subtasks.isNotEmpty()) {
                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider(color = AuraTheme.colors.cardBorder)
                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "CHECKLIST (\${subtasks.count { it.isCompleted }}/\${subtasks.size})",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                )
                Spacer(modifier = Modifier.height(8.dp))

                subtasks.take(3).forEach { sub ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onToggleSubtask(sub) }
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Checkbox(
                            checked = sub.isCompleted,
                            onCheckedChange = { onToggleSubtask(sub) },
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = sub.title,
                            style = MaterialTheme.typography.bodyMedium.copy(
                                color = if (sub.isCompleted) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
                                textDecoration = if (sub.isCompleted) TextDecoration.LineThrough else TextDecoration.None
                            )
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = onSkip,
                    modifier = Modifier
                        .weight(1f)
                        .auraSpringPress(),
                    shape = RoundedCornerShape(AuraCornerRadius.Chip),
                    border = BorderStroke(1.dp, AuraTheme.colors.cardBorder)
                ) {
                    Text("SKIP", color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Bold)
                }

                Button(
                    onClick = onComplete,
                    modifier = Modifier
                        .weight(1.5f)
                        .auraSpringPress(),
                    shape = RoundedCornerShape(AuraCornerRadius.Chip),
                    colors = ButtonDefaults.buttonColors(containerColor = AuraTheme.colors.positiveGreen)
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        tint = Color.Black,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("COMPLETE ⚡", color = Color.Black, fontWeight = FontWeight.ExtraBold)
                }
            }
        }
    }
}

@Composable
fun NextUpPreviewCard(task: Task) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(AuraCornerRadius.Card),
        colors = CardDefaults.cardColors(containerColor = AuraTheme.colors.cardBackground),
        border = BorderStroke(1.dp, AuraTheme.colors.cardBorder)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.weight(1f)
            ) {
                Icon(
                    imageVector = Icons.Default.SkipNext,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp)
                )
                Column {
                    Text(
                        text = "NEXT UP",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )
                    Text(
                        text = task.title,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Surface(
                shape = RoundedCornerShape(8.dp),
                color = AuraTheme.colors.screenBackground
            ) {
                Text(
                    text = task.priority,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                )
            }
        }
    }
}

@Composable
fun TodayTaskRow(
    task: Task,
    isCurrentFocus: Boolean,
    onToggle: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(AuraCornerRadius.Chip),
        colors = CardDefaults.cardColors(containerColor = AuraTheme.colors.cardBackground),
        border = BorderStroke(
            1.dp,
            if (isCurrentFocus) AuraTheme.colors.accentBrand.copy(alpha = 0.5f) else AuraTheme.colors.cardBorder
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onToggle() }
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.weight(1f)
            ) {
                Checkbox(
                    checked = task.isCompleted,
                    onCheckedChange = { onToggle() },
                    modifier = Modifier.size(20.dp)
                )
                Column {
                    Text(
                        text = task.title,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.SemiBold,
                            color = if (task.isCompleted) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
                            textDecoration = if (task.isCompleted) TextDecoration.LineThrough else TextDecoration.None
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (task.time != null) {
                        Text(
                            text = "Scheduled: \${task.time}",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        )
                    }
                }
            }

            if (isCurrentFocus) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = AuraTheme.colors.accentBrand.copy(alpha = 0.15f)
                ) {
                    Text(
                        text = "FOCUS",
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = 9.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = AuraTheme.colors.accentBrand
                        )
                    )
                }
            }
        }
    }
}
