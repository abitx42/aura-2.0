package com.example.ui

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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.DialogProperties
import com.example.data.Task
import com.example.ui.anim.AuraCornerRadius
import com.example.ui.anim.auraSpringPress
import com.example.ui.theme.AuraTheme
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun NightReviewDialog(
    viewModel: AppViewModel,
    onDismiss: () -> Unit
) {
    val step by viewModel.nightReviewStep.collectAsState()
    val selectedMood by viewModel.reviewSelectedMood.collectAsState()
    val impactFactors by viewModel.reviewImpactFactors.collectAsState()
    val reviewNotes by viewModel.reviewNotes.collectAsState()
    val reconciliations by viewModel.itemReconciliations.collectAsState()

    val allTasks by viewModel.allTasks.collectAsState()
    val todayPlanItems by viewModel.todayPlanItems.collectAsState()
    val todayString = viewModel.todayString

    // Deterministic facts calculated via Room data
    val totalPlanned = todayPlanItems.size
    val completedItemsCount = todayPlanItems.count { it.executionState == "COMPLETED" }
    val plannedFocusSeconds = todayPlanItems.sumOf { it.durationMinutes * 60 }
    val actualFocusSeconds = todayPlanItems.sumOf { it.actualDurationSeconds }

    val planAccuracyPercent = if (totalPlanned > 0) {
        ((completedItemsCount.toDouble() / totalPlanned.toDouble()) * 100).toInt()
    } else {
        100
    }

    // Incomplete tasks scheduled for today
    val incompleteTasks = allTasks.filter { it.date == todayString && !it.isCompleted && !it.isDeleted }

    val todayFormatted = remember {
        SimpleDateFormat("EEEE, dd MMM yyyy", Locale.US).format(Date())
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
        modifier = Modifier
            .fillMaxWidth(0.95f)
            .fillMaxHeight(0.90f)
            .padding(vertical = 16.dp),
        content = {
            Card(
                modifier = Modifier
                    .fillMaxSize()
                    .border(1.dp, AuraTheme.colors.cardBorder, RoundedCornerShape(AuraCornerRadius.Hero)),
                colors = CardDefaults.cardColors(containerColor = AuraTheme.colors.screenBackground),
                shape = RoundedCornerShape(AuraCornerRadius.Hero)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(20.dp),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    // Header with Step Indicator
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text("🌙", fontSize = 22.sp)
                                Text(
                                    text = "Night Review",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = AuraTheme.colors.textPrimary
                                )
                            }
                            IconButton(onClick = onDismiss) {
                                Icon(Icons.Default.Close, contentDescription = "Close", tint = AuraTheme.colors.textMuted)
                            }
                        }

                        // Step Indicator Pills
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            val stepLabels = listOf("Reality", "Reconcile", "Reflect", "Summary")
                            stepLabels.forEachIndexed { index, label ->
                                val stepNumber = index + 1
                                val isActive = step == stepNumber
                                val isPast = step > stepNumber
                                val pillColor = when {
                                    isActive -> AuraTheme.colors.accentBrand
                                    isPast -> AuraTheme.colors.positiveGreen
                                    else -> AuraTheme.colors.cardBorder
                                }
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(28.dp)
                                        .clip(RoundedCornerShape(AuraCornerRadius.Chip))
                                        .background(pillColor.copy(alpha = if (isActive || isPast) 0.2f else 0.1f))
                                        .border(
                                            width = if (isActive) 1.5.dp else 1.dp,
                                            color = pillColor,
                                            shape = RoundedCornerShape(AuraCornerRadius.Chip)
                                        )
                                        .clickable { viewModel.setNightReviewStep(stepNumber) },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "$stepNumber. $label",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = if (isActive) FontWeight.ExtraBold else FontWeight.Medium,
                                        color = if (isActive || isPast) AuraTheme.colors.textPrimary else AuraTheme.colors.textMuted,
                                        fontSize = 11.sp
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Step Body Content (Scrollable)
                    Box(modifier = Modifier.weight(1f)) {
                        when (step) {
                            1 -> RealityStepContent(
                                date = todayFormatted,
                                planAccuracy = planAccuracyPercent,
                                completedCount = completedItemsCount,
                                totalCount = totalPlanned,
                                plannedFocusSec = plannedFocusSeconds,
                                actualFocusSec = actualFocusSeconds
                            )
                            2 -> ReconcileStepContent(
                                incompleteTasks = incompleteTasks,
                                reconciliations = reconciliations,
                                onSetAction = { taskId, action ->
                                    viewModel.setTaskReconciliation(taskId, action)
                                },
                                onSetReason = { taskId, reason ->
                                    viewModel.setTaskReconciliationReason(taskId, reason)
                                }
                            )
                            3 -> ReflectStepContent(
                                selectedMood = selectedMood,
                                impactFactors = impactFactors,
                                notes = reviewNotes,
                                onSelectMood = { viewModel.setReviewMood(it) },
                                onToggleFactor = { viewModel.toggleImpactFactor(it) },
                                onNotesChange = { viewModel.setReviewNotes(it) }
                            )
                            4 -> SummaryStepContent(
                                date = todayFormatted,
                                accuracy = planAccuracyPercent,
                                completedCount = completedItemsCount,
                                incompleteTasks = incompleteTasks,
                                reconciliations = reconciliations,
                                plannedSec = plannedFocusSeconds,
                                actualSec = actualFocusSeconds,
                                mood = selectedMood,
                                impactFactors = impactFactors,
                                notes = reviewNotes
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Bottom Navigation Buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (step > 1) {
                            OutlinedButton(
                                onClick = { viewModel.setNightReviewStep(step - 1) },
                                shape = RoundedCornerShape(AuraCornerRadius.Row),
                                border = BorderStroke(1.dp, AuraTheme.colors.cardBorder),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = AuraTheme.colors.textSecondary),
                                modifier = Modifier.height(44.dp)
                            ) {
                                Icon(Icons.Default.ArrowBack, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Back", fontWeight = FontWeight.SemiBold)
                            }
                        } else {
                            Spacer(modifier = Modifier.width(1.dp))
                        }

                        if (step < 4) {
                            Button(
                                onClick = { viewModel.setNightReviewStep(step + 1) },
                                shape = RoundedCornerShape(AuraCornerRadius.Row),
                                colors = ButtonDefaults.buttonColors(containerColor = AuraTheme.colors.accentBrand),
                                modifier = Modifier.height(44.dp)
                            ) {
                                Text(
                                    text = when (step) {
                                        1 -> "Next: Reconcile ➡️"
                                        2 -> "Next: Reflect ➡️"
                                        else -> "Next: Summary ➡️"
                                    },
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                        } else {
                            Button(
                                onClick = { viewModel.submitNightReview() },
                                shape = RoundedCornerShape(AuraCornerRadius.Row),
                                colors = ButtonDefaults.buttonColors(containerColor = AuraTheme.colors.positiveGreen),
                                modifier = Modifier.height(44.dp)
                            ) {
                                Icon(Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("COMPLETE & PLAN TOMORROW 🚀", fontWeight = FontWeight.Black, color = Color.White, fontSize = 13.sp)
                            }
                        }
                    }
                }
            }
        }
    )
}

// ==========================================
// STEP 1: THE REALITY (ADR-013)
// Deterministic accuracy & non-judgmental facts
// ==========================================
@Composable
private fun RealityStepContent(
    date: String,
    planAccuracy: Int,
    completedCount: Int,
    totalCount: Int,
    plannedFocusSec: Int,
    actualFocusSec: Int
) {
    val plannedHours = plannedFocusSec / 3600
    val plannedMins = (plannedFocusSec % 3600) / 60
    val actualHours = actualFocusSec / 3600
    val actualMins = (actualFocusSec % 3600) / 60

    val caption = remember(planAccuracy) {
        when {
            planAccuracy >= 80 -> "Exceptional execution today. You followed through on your core priorities."
            planAccuracy >= 50 -> "Solid progress made today. Key commitments moved forward."
            else -> "Valuable execution telemetry gathered. Tomorrow is a blank canvas to calibrate your workload."
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Text(
                text = date,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = AuraTheme.colors.textMuted
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "The Reality of Today",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.ExtraBold,
                color = AuraTheme.colors.textPrimary
            )
        }

        // Accuracy Gauge Card
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.5.dp, AuraTheme.colors.accentBrand.copy(alpha = 0.5f), RoundedCornerShape(AuraCornerRadius.Card)),
                colors = CardDefaults.cardColors(containerColor = AuraTheme.colors.cardBackground),
                shape = RoundedCornerShape(AuraCornerRadius.Card)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(100.dp)
                            .clip(CircleShape)
                            .background(AuraTheme.colors.accentBrand.copy(alpha = 0.12f))
                            .border(3.dp, AuraTheme.colors.accentBrand, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "$planAccuracy%",
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Black,
                                color = AuraTheme.colors.textPrimary
                            )
                            Text(
                                text = "ACCURACY",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = AuraTheme.colors.accentBrand,
                                fontSize = 9.sp
                            )
                        }
                    }

                    Text(
                        text = caption,
                        style = MaterialTheme.typography.bodyMedium,
                        color = AuraTheme.colors.textSecondary,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 8.dp)
                    )
                }
            }
        }

        // Focus & Task Stats
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Card(
                    modifier = Modifier
                        .weight(1f)
                        .border(1.dp, AuraTheme.colors.cardBorder, RoundedCornerShape(AuraCornerRadius.Row)),
                    colors = CardDefaults.cardColors(containerColor = AuraTheme.colors.cardBackground),
                    shape = RoundedCornerShape(AuraCornerRadius.Row)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "TASKS DONE",
                            style = MaterialTheme.typography.labelSmall,
                            color = AuraTheme.colors.textMuted,
                            fontWeight = FontWeight.Bold,
                            fontSize = 10.sp
                        )
                        Text(
                            text = "$completedCount of $totalCount",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.ExtraBold,
                            color = AuraTheme.colors.positiveGreen
                        )
                        Text(
                            text = "completed commitments",
                            style = MaterialTheme.typography.bodySmall,
                            color = AuraTheme.colors.textSecondary,
                            fontSize = 11.sp
                        )
                    }
                }

                Card(
                    modifier = Modifier
                        .weight(1f)
                        .border(1.dp, AuraTheme.colors.cardBorder, RoundedCornerShape(AuraCornerRadius.Row)),
                    colors = CardDefaults.cardColors(containerColor = AuraTheme.colors.cardBackground),
                    shape = RoundedCornerShape(AuraCornerRadius.Row)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "FOCUS TIME",
                            style = MaterialTheme.typography.labelSmall,
                            color = AuraTheme.colors.textMuted,
                            fontWeight = FontWeight.Bold,
                            fontSize = 10.sp
                        )
                        Text(
                            text = "${actualHours}h ${actualMins}m",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.ExtraBold,
                            color = AuraTheme.colors.accentBrand
                        )
                        Text(
                            text = "of ${plannedHours}h ${plannedMins}m planned",
                            style = MaterialTheme.typography.bodySmall,
                            color = AuraTheme.colors.textSecondary,
                            fontSize = 11.sp
                        )
                    }
                }
            }
        }

        // Philosophical Callout
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = AuraTheme.colors.accentBrand.copy(alpha = 0.05f)),
                shape = RoundedCornerShape(AuraCornerRadius.Row)
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("💡", fontSize = 18.sp)
                    Text(
                        text = "Night Review is where Aura compares intention with reality — not where it judges you. Honest tracking calibrates your capacity.",
                        style = MaterialTheme.typography.bodySmall,
                        color = AuraTheme.colors.textSecondary,
                        fontSize = 11.sp,
                        lineHeight = 16.sp
                    )
                }
            }
        }
    }
}

// ==========================================
// STEP 2: RECONCILE COMMITMENTS (ADR-013)
// 4 Resolutions + Optional non-punitive reason
// ==========================================
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ReconcileStepContent(
    incompleteTasks: List<Task>,
    reconciliations: Map<Int, Pair<String, String?>>,
    onSetAction: (taskId: Int, action: String) -> Unit,
    onSetReason: (taskId: Int, reason: String?) -> Unit
) {
    val resolutionOptions = listOf(
        Triple("MOVE_TOMORROW", "Tomorrow ➡️", "Bridge into tomorrow's plan"),
        Triple("RESCHEDULE", "Reschedule 📅", "Keep in backlog without a fixed time"),
        Triple("CANCEL", "Drop ❌", "Mark as no longer necessary"),
        Triple("KEEP_OPEN", "Keep Open ⏳", "Leave on today's list for late evening")
    )

    val reasonOptions = listOf(
        Pair("TIME_UNDER_ESTIMATED", "⏱️ Underestimated"),
        Pair("LOW_ENERGY", "⚡ Low energy"),
        Pair("UNEXPECTED_EVENT", "🚨 Interruption"),
        Pair("PROCRASTINATION", "🧘 Distraction"),
        Pair("PRIORITY_CHANGED", "🔄 Shift in priority"),
        Pair("NO_LONGER_RELEVANT", "🚫 No longer needed"),
        Pair("OTHER", "📝 Other")
    )

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Text(
                text = "Incomplete Commitments",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.ExtraBold,
                color = AuraTheme.colors.textPrimary
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "Decide the next step for unfinished items. Moving them carries them directly into tomorrow's plan.",
                style = MaterialTheme.typography.bodySmall,
                color = AuraTheme.colors.textSecondary
            )
        }

        if (incompleteTasks.isEmpty()) {
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, AuraTheme.colors.positiveGreen.copy(alpha = 0.5f), RoundedCornerShape(AuraCornerRadius.Card)),
                    colors = CardDefaults.cardColors(containerColor = AuraTheme.colors.positiveGreen.copy(alpha = 0.08f)),
                    shape = RoundedCornerShape(AuraCornerRadius.Card)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text("✨", fontSize = 36.sp)
                        Text(
                            text = "All Commitments Finished!",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = AuraTheme.colors.textPrimary
                        )
                        Text(
                            text = "You cleared every planned item today. Proceed straight to reflection.",
                            style = MaterialTheme.typography.bodySmall,
                            color = AuraTheme.colors.textSecondary,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        } else {
            items(incompleteTasks, key = { it.id }) { task ->
                val currentPair = reconciliations[task.id]
                val currentAction = currentPair?.first ?: "MOVE_TOMORROW"
                val currentReason = currentPair?.second

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, AuraTheme.colors.cardBorder, RoundedCornerShape(AuraCornerRadius.Card)),
                    colors = CardDefaults.cardColors(containerColor = AuraTheme.colors.cardBackground),
                    shape = RoundedCornerShape(AuraCornerRadius.Card)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Task title & priority
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = task.title,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = AuraTheme.colors.textPrimary,
                                modifier = Modifier.weight(1f)
                            )
                            if (!task.time.isNullOrBlank()) {
                                Text(
                                    text = task.time,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = AuraTheme.colors.textMuted
                                )
                            }
                        }

                        // 4 Resolution Chips
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            resolutionOptions.forEach { (actionKey, actionTitle, _) ->
                                val isSelected = currentAction == actionKey
                                val activeColor = when (actionKey) {
                                    "MOVE_TOMORROW" -> AuraTheme.colors.accentBrand
                                    "RESCHEDULE" -> Color(0xFF6366F1)
                                    "CANCEL" -> AuraTheme.colors.negativeRed
                                    else -> AuraTheme.colors.badgeGold
                                }
                                Box(
                                    modifier = Modifier
                                        .auraSpringPress(
                                            cornerRadius = AuraCornerRadius.Chip,
                                            onClick = { onSetAction(task.id, actionKey) }
                                        )
                                        .clip(RoundedCornerShape(AuraCornerRadius.Chip))
                                        .background(if (isSelected) activeColor else activeColor.copy(alpha = 0.1f))
                                        .border(
                                            1.dp,
                                            if (isSelected) activeColor else activeColor.copy(alpha = 0.3f),
                                            RoundedCornerShape(AuraCornerRadius.Chip)
                                        )
                                        .padding(horizontal = 10.dp, vertical = 6.dp)
                                ) {
                                    Text(
                                        text = actionTitle,
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        color = if (isSelected) Color.White else AuraTheme.colors.textPrimary,
                                        fontSize = 11.sp
                                    )
                                }
                            }
                        }

                        // Optional Reason Selector
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                text = "Why did this slip? (Optional, helps calibrate future estimates)",
                                style = MaterialTheme.typography.labelSmall,
                                color = AuraTheme.colors.textMuted,
                                fontSize = 10.sp
                            )
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                reasonOptions.forEach { (reasonKey, reasonTitle) ->
                                    val isReasonSelected = currentReason == reasonKey
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(AuraCornerRadius.Chip))
                                            .background(
                                                if (isReasonSelected) AuraTheme.colors.accentBrand.copy(alpha = 0.2f)
                                                else AuraTheme.colors.screenBackground
                                            )
                                            .border(
                                                1.dp,
                                                if (isReasonSelected) AuraTheme.colors.accentBrand else AuraTheme.colors.cardBorder,
                                                RoundedCornerShape(AuraCornerRadius.Chip)
                                            )
                                            .clickable {
                                                onSetReason(task.id, if (isReasonSelected) null else reasonKey)
                                            }
                                            .padding(horizontal = 8.dp, vertical = 4.dp)
                                    ) {
                                        Text(
                                            text = reasonTitle,
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = if (isReasonSelected) FontWeight.Bold else FontWeight.Normal,
                                            color = if (isReasonSelected) AuraTheme.colors.accentBrand else AuraTheme.colors.textSecondary,
                                            fontSize = 10.sp
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// ==========================================
// STEP 3: LIGHTWEIGHT REFLECTION (ADR-013)
// 5 Mood Emojis + Impact Chips + Gratitude Notes
// ==========================================
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ReflectStepContent(
    selectedMood: Int?,
    impactFactors: Set<String>,
    notes: String,
    onSelectMood: (Int) -> Unit,
    onToggleFactor: (String) -> Unit,
    onNotesChange: (String) -> Unit
) {
    val moodOptions = listOf(
        Pair(1, "😫 Drained"),
        Pair(2, "😕 Rough"),
        Pair(3, "😐 Neutral"),
        Pair(4, "🙂 Good"),
        Pair(5, "🔥 In Flow")
    )

    val factorOptions = listOf(
        "Low energy",
        "Unexpected work",
        "Procrastination",
        "Social plans",
        "Feeling sick",
        "Poor planning",
        "High focus",
        "Great sleep"
    )

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                text = "Lightweight Reflection",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.ExtraBold,
                color = AuraTheme.colors.textPrimary
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "Fast, effortless pulse check on how the day unfolded.",
                style = MaterialTheme.typography.bodySmall,
                color = AuraTheme.colors.textSecondary
            )
        }

        // Mood Selector
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, AuraTheme.colors.cardBorder, RoundedCornerShape(AuraCornerRadius.Card)),
                colors = CardDefaults.cardColors(containerColor = AuraTheme.colors.cardBackground),
                shape = RoundedCornerShape(AuraCornerRadius.Card)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "How did today feel overall?",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = AuraTheme.colors.textPrimary
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        moodOptions.forEach { (moodVal, label) ->
                            val isSelected = selectedMood == moodVal
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(4.dp),
                                modifier = Modifier
                                    .auraSpringPress(
                                        cornerRadius = AuraCornerRadius.Row,
                                        onClick = { onSelectMood(moodVal) }
                                    )
                                    .clip(RoundedCornerShape(AuraCornerRadius.Row))
                                    .background(
                                        if (isSelected) AuraTheme.colors.accentBrand.copy(alpha = 0.2f)
                                        else AuraTheme.colors.screenBackground
                                    )
                                    .border(
                                        1.5.dp,
                                        if (isSelected) AuraTheme.colors.accentBrand else AuraTheme.colors.cardBorder,
                                        RoundedCornerShape(AuraCornerRadius.Row)
                                    )
                                    .padding(horizontal = 10.dp, vertical = 8.dp)
                            ) {
                                Text(label.take(2), fontSize = 24.sp)
                                Text(
                                    text = label.drop(2).trim(),
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSelected) AuraTheme.colors.accentBrand else AuraTheme.colors.textSecondary,
                                    fontSize = 10.sp
                                )
                            }
                        }
                    }
                }
            }
        }

        // Impact Factors
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, AuraTheme.colors.cardBorder, RoundedCornerShape(AuraCornerRadius.Card)),
                colors = CardDefaults.cardColors(containerColor = AuraTheme.colors.cardBackground),
                shape = RoundedCornerShape(AuraCornerRadius.Card)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "What influenced your execution today? (Multi-select)",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = AuraTheme.colors.textPrimary
                    )
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        factorOptions.forEach { factor ->
                            val isSelected = impactFactors.contains(factor)
                            Box(
                                modifier = Modifier
                                    .auraSpringPress(
                                        cornerRadius = AuraCornerRadius.Chip,
                                        onClick = { onToggleFactor(factor) }
                                    )
                                    .clip(RoundedCornerShape(AuraCornerRadius.Chip))
                                    .background(
                                        if (isSelected) AuraTheme.colors.accentBrand.copy(alpha = 0.2f)
                                        else AuraTheme.colors.screenBackground
                                    )
                                    .border(
                                        1.dp,
                                        if (isSelected) AuraTheme.colors.accentBrand else AuraTheme.colors.cardBorder,
                                        RoundedCornerShape(AuraCornerRadius.Chip)
                                    )
                                    .padding(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = factor,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSelected) AuraTheme.colors.accentBrand else AuraTheme.colors.textSecondary,
                                    fontSize = 11.sp
                                )
                            }
                        }
                    }
                }
            }
        }

        // Reflection Notes / Gratitude
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, AuraTheme.colors.cardBorder, RoundedCornerShape(AuraCornerRadius.Card)),
                colors = CardDefaults.cardColors(containerColor = AuraTheme.colors.cardBackground),
                shape = RoundedCornerShape(AuraCornerRadius.Card)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "One win or thought from today (Optional)",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = AuraTheme.colors.textPrimary
                    )
                    OutlinedTextField(
                        value = notes,
                        onValueChange = onNotesChange,
                        placeholder = { Text("Grateful for finishing the project proposal early...", color = AuraTheme.colors.textMuted, fontSize = 12.sp) },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 2,
                        maxLines = 4,
                        shape = RoundedCornerShape(AuraCornerRadius.Row),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = AuraTheme.colors.accentBrand,
                            unfocusedBorderColor = AuraTheme.colors.cardBorder,
                            focusedTextColor = AuraTheme.colors.textPrimary,
                            unfocusedTextColor = AuraTheme.colors.textPrimary
                        )
                    )
                }
            }
        }
    }
}

// ==========================================
// STEP 4: DAY SUMMARY & BRIDGE TO TOMORROW
// ==========================================
@Composable
private fun SummaryStepContent(
    date: String,
    accuracy: Int,
    completedCount: Int,
    incompleteTasks: List<Task>,
    reconciliations: Map<Int, Pair<String, String?>>,
    plannedSec: Int,
    actualSec: Int,
    mood: Int?,
    impactFactors: Set<String>,
    notes: String
) {
    val movedCount = reconciliations.count { it.value.first == "MOVE_TOMORROW" }
    val cancelledCount = reconciliations.count { it.value.first == "CANCEL" }
    val rescheduledCount = reconciliations.count { it.value.first == "RESCHEDULE" }

    val moodEmoji = when (mood) {
        1 -> "😫 Drained"
        2 -> "😕 Rough"
        3 -> "😐 Neutral"
        4 -> "🙂 Good"
        5 -> "🔥 In Flow"
        else -> "✨ Not specified"
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Text(
                text = "Day Summary 🌙",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.ExtraBold,
                color = AuraTheme.colors.textPrimary
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "Your truth for today. Completing this will update your daily plan and bridge carried tasks into tomorrow's plan.",
                style = MaterialTheme.typography.bodySmall,
                color = AuraTheme.colors.textSecondary
            )
        }

        // Summary Card
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.5.dp, AuraTheme.colors.positiveGreen.copy(alpha = 0.5f), RoundedCornerShape(AuraCornerRadius.Card)),
                colors = CardDefaults.cardColors(containerColor = AuraTheme.colors.cardBackground),
                shape = RoundedCornerShape(AuraCornerRadius.Card)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = date,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = AuraTheme.colors.textPrimary
                        )
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(AuraCornerRadius.Chip))
                                .background(AuraTheme.colors.positiveGreen.copy(alpha = 0.15f))
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = "$accuracy% ACCURACY",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.ExtraBold,
                                color = AuraTheme.colors.positiveGreen,
                                fontSize = 10.sp
                            )
                        }
                    }

                    Divider(color = AuraTheme.colors.cardBorder)

                    // Key facts grid
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("Execution", style = MaterialTheme.typography.labelSmall, color = AuraTheme.colors.textMuted)
                            Text("$completedCount finished", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = AuraTheme.colors.textPrimary)
                        }
                        Column {
                            Text("Focus Time", style = MaterialTheme.typography.labelSmall, color = AuraTheme.colors.textMuted)
                            Text("${actualSec / 60}m recorded", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = AuraTheme.colors.textPrimary)
                        }
                        Column {
                            Text("Mood", style = MaterialTheme.typography.labelSmall, color = AuraTheme.colors.textMuted)
                            Text(moodEmoji, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = AuraTheme.colors.textPrimary)
                        }
                    }

                    if (impactFactors.isNotEmpty()) {
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text("Factors", style = MaterialTheme.typography.labelSmall, color = AuraTheme.colors.textMuted)
                            Text(
                                text = impactFactors.joinToString(" • "),
                                style = MaterialTheme.typography.bodySmall,
                                color = AuraTheme.colors.textSecondary
                            )
                        }
                    }

                    if (notes.isNotBlank()) {
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text("Reflection", style = MaterialTheme.typography.labelSmall, color = AuraTheme.colors.textMuted)
                            Text(
                                text = "\"$notes\"",
                                style = MaterialTheme.typography.bodySmall,
                                color = AuraTheme.colors.accentBrand,
                                fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                            )
                        }
                    }
                }
            }
        }

        // Bridge Callout Card
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, Color(0xFF6366F1).copy(alpha = 0.5f), RoundedCornerShape(AuraCornerRadius.Card)),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF6366F1).copy(alpha = 0.08f)),
                shape = RoundedCornerShape(AuraCornerRadius.Card)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text("🌉", fontSize = 20.sp)
                        Text(
                            text = "Tomorrow's Bridge",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = AuraTheme.colors.textPrimary
                        )
                    }
                    Text(
                        text = when {
                            movedCount > 0 -> "$movedCount task(s) will be automatically rescheduled into tomorrow's plan."
                            cancelledCount > 0 -> "$cancelledCount task(s) cancelled, clean slate for tomorrow."
                            else -> "All tasks completed! Tomorrow begins with a blank canvas."
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = AuraTheme.colors.textSecondary
                    )
                }
            }
        }
    }
}
