package com.aura.personalos.ui

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aura.personalos.data.LifeEventEntity
import com.aura.personalos.data.ProposedActionEntity
import com.aura.personalos.ui.anim.AuraCornerRadius
import com.aura.personalos.ui.anim.auraSpringPress
import com.aura.personalos.ui.components.*
import com.aura.personalos.ui.theme.AuraTheme
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun BrainScreen(
    viewModel: AppViewModel,
    modifier: Modifier = Modifier
) {
    val pendingActions by viewModel.pendingProposedActions.collectAsState()
    val lifeEvents by viewModel.recentLifeEvents.collectAsState()
    val todayPlan by viewModel.todayPlan.collectAsState()
    val currentFocus by viewModel.currentFocusTask.collectAsState()
    val tasks by viewModel.allTasks.collectAsState()

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(AuraTheme.colors.screenBackground)
            .padding(horizontal = 20.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 100.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        // 1. Header & AI Status
        item {
            BrainHeader(
                activePlanStatus = todayPlan?.status ?: "NO_PLAN",
                currentFocusTitle = currentFocus?.title
            )
        }

        // 2. Context Engine Card
        item {
            ContextEngineCard(
                todayPlan = todayPlan,
                completedTaskCount = tasks.count { it.isCompleted },
                totalTaskCount = tasks.size
            )
        }

        // 3. Proposed Actions Section (Invariant 1: AI Proposes, User Approves)
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "PROPOSED ACTIONS (\${pendingActions.size})",
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.2.sp,
                        color = AuraTheme.colors.accentBrand
                    )
                )

                TextButton(
                    onClick = { viewModel.requestAiOptimization() },
                    modifier = Modifier.auraSpringPress()
                ) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = AuraTheme.colors.accentBrand
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "ANALYZE NOW",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = AuraTheme.colors.accentBrand
                        )
                    )
                }
            }
        }

        if (pendingActions.isEmpty()) {
            item {
                AuraEmptyState(
                    title = "All Actions Reviewed",
                    description = "Aura Brain is continuously monitoring your execution telemetry. When optimizations or schedule adjustments are found, they will appear here for your approval.",
                    icon = Icons.Default.CheckCircle,
                    modifier = Modifier.padding(vertical = 12.dp)
                )
            }
        } else {
            items(pendingActions, key = { it.id }) { action ->
                ProposedActionCard(
                    action = action,
                    onApprove = { viewModel.approveProposedAction(action.id) },
                    onReject = { viewModel.rejectProposedAction(action.id) }
                )
            }
        }

        // 4. Canonical Life Events Stream (Invariant 2: Facts are immutable)
        item {
            Text(
                text = "CANONICAL LIFE EVENTS INDEX",
                style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.2.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                ),
                modifier = Modifier.padding(top = 8.dp)
            )
        }

        if (lifeEvents.isEmpty()) {
            item {
                AuraEmptyState(
                    title = "Event Stream Initializing",
                    description = "Your lock, kickoff, completion, and reflection actions are recorded here as immutable chronological telemetry.",
                    icon = Icons.Default.Timeline,
                    modifier = Modifier.padding(vertical = 12.dp)
                )
            }
        } else {
            items(lifeEvents, key = { it.id }) { event ->
                LifeEventRow(event = event)
            }
        }
    }
}

@Composable
fun BrainHeader(
    activePlanStatus: String,
    currentFocusTitle: String?
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(12.dp)
                    .clip(CircleShape)
                    .background(AuraTheme.colors.positiveGreen)
            )
            Text(
                text = "AURA BRAIN & AI ENGINE",
                style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 1.5.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            )
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = "Autonomous Intelligence",
            style = MaterialTheme.typography.headlineMedium.copy(
                fontWeight = FontWeight.Black,
                color = MaterialTheme.colorScheme.onSurface
            )
        )
        Text(
            text = "Invariant 1: AI proposes actions. Facts are computed. You hold final authority.",
            style = MaterialTheme.typography.bodySmall.copy(
                color = MaterialTheme.colorScheme.onSurfaceVariant
            ),
            modifier = Modifier.padding(top = 4.dp)
        )
    }
}

@Composable
fun ContextEngineCard(
    todayPlan: com.aura.personalos.data.DailyPlan?,
    completedTaskCount: Int,
    totalTaskCount: Int
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(AuraCornerRadius.Card),
        colors = CardDefaults.cardColors(containerColor = AuraTheme.colors.cardBackground),
        border = androidx.compose.foundation.BorderStroke(1.dp, AuraTheme.colors.cardBorder)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Psychology,
                    contentDescription = null,
                    tint = AuraTheme.colors.accentBrand,
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    text = "REAL-TIME CONTEXT ENGINE",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.2.sp,
                        color = AuraTheme.colors.accentBrand
                    )
                )
            }
            Spacer(modifier = Modifier.height(14.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                ContextMetricItem(
                    label = "PLAN STATE",
                    value = todayPlan?.status ?: "NONE",
                    color = when (todayPlan?.status) {
                        "LOCKED", "ACTIVE" -> AuraTheme.colors.badgeGold
                        "REVIEWED" -> AuraTheme.colors.positiveGreen
                        else -> MaterialTheme.colorScheme.onSurfaceVariant
                    }
                )
                ContextMetricItem(
                    label = "TASKS DONE",
                    value = "\$completedTaskCount / \$totalTaskCount",
                    color = MaterialTheme.colorScheme.onSurface
                )
                ContextMetricItem(
                    label = "ACCURACY",
                    value = if (todayPlan?.planAccuracyPercent != null) "\${todayPlan.planAccuracyPercent}%" else "--",
                    color = AuraTheme.colors.positiveGreen
                )
            }
        }
    }
}

@Composable
fun ContextMetricItem(
    label: String,
    value: String,
    color: Color
) {
    Column {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.ExtraBold,
                color = color
            )
        )
    }
}

@Composable
fun ProposedActionCard(
    action: ProposedActionEntity,
    onApprove: () -> Unit,
    onReject: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(AuraCornerRadius.Card),
        colors = CardDefaults.cardColors(containerColor = AuraTheme.colors.cardBackground),
        border = androidx.compose.foundation.BorderStroke(1.dp, AuraTheme.colors.badgeGold.copy(alpha = 0.5f))
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Lightbulb,
                        contentDescription = null,
                        tint = AuraTheme.colors.badgeGold,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = action.actionType.replace("_", " "),
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.ExtraBold,
                            color = AuraTheme.colors.badgeGold
                        )
                    )
                }

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = AuraTheme.colors.badgeGold.copy(alpha = 0.15f)
                ) {
                    Text(
                        text = "AI PROPOSAL",
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = AuraTheme.colors.badgeGold
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = action.reasoning,
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = MaterialTheme.colorScheme.onSurface,
                    lineHeight = 20.sp
                )
            )

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = onReject,
                    modifier = Modifier
                        .weight(1f)
                        .auraSpringPress(),
                    shape = RoundedCornerShape(AuraCornerRadius.Chip),
                    border = androidx.compose.foundation.BorderStroke(1.dp, AuraTheme.colors.cardBorder)
                ) {
                    Text(
                        text = "DISMISS",
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )
                }

                Button(
                    onClick = onApprove,
                    modifier = Modifier
                        .weight(1.3f)
                        .auraSpringPress(),
                    shape = RoundedCornerShape(AuraCornerRadius.Chip),
                    colors = ButtonDefaults.buttonColors(containerColor = AuraTheme.colors.accentBrand)
                ) {
                    Text(
                        text = "APPROVE & APPLY",
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.Black
                        )
                    )
                }
            }
        }
    }
}

@Composable
fun LifeEventRow(event: LifeEventEntity) {
    val sdf = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
    val timeFormatted = sdf.format(Date(event.createdAt))

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(AuraCornerRadius.Chip),
        colors = CardDefaults.cardColors(containerColor = AuraTheme.colors.cardBackground),
        border = androidx.compose.foundation.BorderStroke(1.dp, AuraTheme.colors.cardBorder)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(
                            when (event.domain) {
                                "PLANNING" -> AuraTheme.colors.badgeGold
                                "EXECUTION" -> AuraTheme.colors.positiveGreen
                                "REFLECTION" -> AuraTheme.colors.accentBrand
                                else -> MaterialTheme.colorScheme.onSurfaceVariant
                            }
                        )
                )
                Column {
                    Text(
                        text = event.eventType,
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "Domain: \${event.domain}",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )
                }
            }

            Text(
                text = timeFormatted,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            )
        }
    }
}
