package com.example.ui

import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.PendingOperation
import com.example.ui.theme.AuraTheme
import com.example.util.AuraCrashHandler
import java.text.SimpleDateFormat
import java.util.*

/**
 * DebugScreen — ADR-015
 *
 * Founder Testing & Diagnostic Console for Aura 2.0 Phase 2.
 * Provides sync queue inspection, table telemetry, daily loop time-travel shortcuts,
 * simulated offline mode, live error forensics, and quick friction note capture.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DebugScreen(
    viewModel: AppViewModel,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val pendingOps by viewModel.allPendingOperations.collectAsState()
    val isSimulatedOffline by viewModel.isSimulatedOffline.collectAsState()
    val tasks by viewModel.allTasks.collectAsState()
    val notes by viewModel.activeNotes.collectAsState()
    val habits by viewModel.habits.collectAsState()
    val transactions by viewModel.allTransactions.collectAsState()
    val timelineEvents by com.example.util.AuraSessionTimeline.events.collectAsState()

    var frictionNoteInput by remember { mutableStateOf("") }
    var noteSavedFeedback by remember { mutableStateOf(false) }
    var showCrashLogDialog by remember { mutableStateOf(false) }
    var crashLogContent by remember { mutableStateOf("") }

    val recentLogs = remember { mutableStateOf(AuraCrashHandler.getRecentLogs()) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "FOUNDER DIAGNOSTICS 🛠️",
                            color = AuraTheme.colors.textPrimary,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                        Text(
                            text = "Phase 2 Founder Testing Mode (Day 1-14)",
                            color = AuraTheme.colors.textMuted,
                            fontSize = 11.sp
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = AuraTheme.colors.textPrimary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = AuraTheme.colors.screenBackground
                )
            )
        },
        containerColor = AuraTheme.colors.screenBackground
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            contentPadding = PaddingValues(vertical = 12.dp)
        ) {
            // 1. Device & System Telemetry
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = AuraTheme.colors.cardBackground),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "📱 DEVICE & SYSTEM TELEMETRY",
                            color = AuraTheme.colors.accentBrand,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                        HorizontalDivider(color = AuraTheme.colors.cardBorder, modifier = Modifier.padding(vertical = 4.dp))
                        TelemetryRow("Device", "${Build.MANUFACTURER} ${Build.MODEL} (${Build.DEVICE})")
                        TelemetryRow("Android Version", "Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})")
                        TelemetryRow("App Version", "2.0.0 (Debug APK)")
                        TelemetryRow("Room Database", "Version 12 (SQLite)")
                        TelemetryRow("Local Time", SimpleDateFormat("yyyy-MM-dd HH:mm:ss z", Locale.getDefault()).format(Date()))
                    }
                }
            }

            // 2. Database Table Counts
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = AuraTheme.colors.cardBackground),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "🗄️ LOCAL DATABASE TABLE COUNTS",
                            color = AuraTheme.colors.accentBrand,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                        HorizontalDivider(color = AuraTheme.colors.cardBorder, modifier = Modifier.padding(vertical = 4.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            CountPill("Tasks", tasks.size.toString())
                            CountPill("Notes", notes.size.toString())
                            CountPill("Habits", habits.size.toString())
                            CountPill("Trans.", transactions.size.toString())
                            CountPill("Pending", pendingOps.size.toString())
                        }
                    }
                }
            }

            // 3. Daily Loop Testing Shortcuts (Time Travel)
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = AuraTheme.colors.cardBackground),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = "⚡ DAILY LOOP TESTING SHORTCUTS",
                            color = AuraTheme.colors.accentBrand,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                        Text(
                            text = "Test loop transitions on-demand without waiting for scheduled hours:",
                            color = AuraTheme.colors.textSecondary,
                            fontSize = 11.sp
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = { viewModel.startNightReview() },
                                colors = ButtonDefaults.buttonColors(containerColor = AuraTheme.colors.accentBrand),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("🌙 NIGHT REVIEW", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }

                            Button(
                                onClick = { viewModel.startMyDay() },
                                colors = ButtonDefaults.buttonColors(containerColor = AuraTheme.colors.bottomNavBackground),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("☀️ START DAY", color = AuraTheme.colors.textPrimary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = { viewModel.populateFounderSampleDay() },
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("🧪 POPULATE DAY", fontSize = 10.sp)
                            }

                            OutlinedButton(
                                onClick = { viewModel.resetTodayPlan() },
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("🔄 RESET PLAN", fontSize = 10.sp, color = AuraTheme.colors.badgeGold)
                            }
                        }
                    }
                }
            }

            // 4. Simulated Offline Mode
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = AuraTheme.colors.cardBackground),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
                            Text(
                                text = "📴 SIMULATED OFFLINE MODE",
                                color = if (isSimulatedOffline) AuraTheme.colors.badgeGold else AuraTheme.colors.textPrimary,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                            Text(
                                text = "Simulate cellular network drops inside Aura without switching device to Airplane Mode.",
                                color = AuraTheme.colors.textMuted,
                                fontSize = 10.sp,
                                lineHeight = 14.sp
                            )
                        }
                        Switch(
                            checked = isSimulatedOffline,
                            onCheckedChange = { viewModel.toggleSimulatedOffline() },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = AuraTheme.colors.badgeGold
                            )
                        )
                    }
                }
            }

            // 5. Sync Queue Inspector
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = AuraTheme.colors.cardBackground),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "🔄 SYNC QUEUE (${pendingOps.size})",
                                color = AuraTheme.colors.accentBrand,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                TextButton(onClick = { viewModel.triggerSyncNow() }) {
                                    Text("FORCE SYNC 🔄", fontSize = 11.sp, color = AuraTheme.colors.accentBrand)
                                }
                                TextButton(onClick = { viewModel.clearPendingOperationsQueue() }) {
                                    Text("PURGE 🧹", fontSize = 11.sp, color = AuraTheme.colors.negativeRed)
                                }
                            }
                        }

                        if (pendingOps.isEmpty()) {
                            Text(
                                text = "✓ Sync queue is empty. All local mutations have synced to cloud.",
                                color = AuraTheme.colors.textMuted,
                                fontSize = 11.sp
                            )
                        } else {
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                pendingOps.take(5).forEach { op ->
                                    PendingOpItem(op)
                                }
                                if (pendingOps.size > 5) {
                                    Text(
                                        text = "+ ${pendingOps.size - 5} more pending operations in SQLite",
                                        color = AuraTheme.colors.textMuted,
                                        fontSize = 10.sp,
                                        modifier = Modifier.padding(top = 4.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // 6. In-App Founder Friction Recorder
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = AuraTheme.colors.cardBackground),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = "📝 RECORD FOUNDER FRICTION NOTE",
                            color = AuraTheme.colors.accentBrand,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                        Text(
                            text = "Found an awkward interaction, lag, or bug? Log it right here to disk:",
                            color = AuraTheme.colors.textSecondary,
                            fontSize = 11.sp
                        )

                        OutlinedTextField(
                            value = frictionNoteInput,
                            onValueChange = {
                                frictionNoteInput = it
                                noteSavedFeedback = false
                            },
                            placeholder = { Text("e.g. Focus timer sound didn't ring on lock screen...", color = AuraTheme.colors.textMuted, fontSize = 11.sp) },
                            modifier = Modifier.fillMaxWidth(),
                            minLines = 2,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = AuraTheme.colors.accentBrand,
                                unfocusedBorderColor = AuraTheme.colors.cardBorder,
                                focusedTextColor = AuraTheme.colors.textPrimary,
                                unfocusedTextColor = AuraTheme.colors.textPrimary
                            )
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (noteSavedFeedback) {
                                Text("✓ Saved to founder_notes.txt", color = AuraTheme.colors.positiveGreen, fontSize = 11.sp)
                            } else {
                                Spacer(modifier = Modifier.width(1.dp))
                            }

                            Button(
                                onClick = {
                                    viewModel.recordFounderFrictionNote(frictionNoteInput)
                                    frictionNoteInput = ""
                                    noteSavedFeedback = true
                                },
                                enabled = frictionNoteInput.isNotBlank(),
                                colors = ButtonDefaults.buttonColors(containerColor = AuraTheme.colors.accentBrand),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text("SAVE NOTE 💾", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            // 7. Session Event Timeline
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = AuraTheme.colors.cardBackground),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "📜 SESSION EVENT TIMELINE (${timelineEvents.size})",
                                color = AuraTheme.colors.accentBrand,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                            TextButton(onClick = { com.example.util.AuraSessionTimeline.clear() }) {
                                Text("CLEAR", fontSize = 10.sp, color = AuraTheme.colors.textMuted)
                            }
                        }
                        Text(
                            text = "Redacted chronological log of user interactions and sync events:",
                            color = AuraTheme.colors.textSecondary,
                            fontSize = 11.sp
                        )

                        Surface(
                            color = AuraTheme.colors.screenBackground,
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 180.dp)
                        ) {
                            LazyColumn(
                                modifier = Modifier.padding(8.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                if (timelineEvents.isEmpty()) {
                                    item {
                                        Text("No session events recorded yet.", color = AuraTheme.colors.textMuted, fontSize = 10.sp)
                                    }
                                } else {
                                    items(timelineEvents) { event ->
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Text(
                                                text = event.timestamp,
                                                color = AuraTheme.colors.textMuted,
                                                fontSize = 9.sp,
                                                fontFamily = FontFamily.Monospace
                                            )
                                            Text(
                                                text = event.type,
                                                color = when {
                                                    event.type.contains("ERROR") || event.type.contains("FAILED") -> AuraTheme.colors.negativeRed
                                                    event.type.contains("LOCKED") || event.type.contains("COMPLETED") || event.type.contains("SUCCESS") -> AuraTheme.colors.positiveGreen
                                                    event.type.contains("START") || event.type.contains("OPENED") -> AuraTheme.colors.accentBrand
                                                    else -> AuraTheme.colors.textPrimary
                                                },
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Bold,
                                                fontFamily = FontFamily.Monospace
                                            )
                                            if (!event.details.isNullOrBlank()) {
                                                Text(
                                                    text = event.details,
                                                    color = AuraTheme.colors.textSecondary,
                                                    fontSize = 9.sp,
                                                    fontFamily = FontFamily.Monospace,
                                                    maxLines = 1
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

            // 8. Live Events & Crash Forensics
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = AuraTheme.colors.cardBackground),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "📋 RECENT EVENTS & LOGS",
                                color = AuraTheme.colors.accentBrand,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                            Row {
                                TextButton(
                                    onClick = {
                                        crashLogContent = AuraCrashHandler.readCrashLogFile(context)
                                        showCrashLogDialog = true
                                    }
                                ) {
                                    Text("CRASH FILE", fontSize = 10.sp, color = AuraTheme.colors.badgeGold)
                                }
                                TextButton(
                                    onClick = {
                                        recentLogs.value = AuraCrashHandler.getRecentLogs()
                                    }
                                ) {
                                    Text("REFRESH", fontSize = 10.sp, color = AuraTheme.colors.accentBrand)
                                }
                            }
                        }

                        Surface(
                            color = AuraTheme.colors.screenBackground,
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 160.dp)
                        ) {
                            LazyColumn(
                                modifier = Modifier.padding(8.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                if (recentLogs.value.isEmpty()) {
                                    item {
                                        Text("No diagnostic events recorded yet.", color = AuraTheme.colors.textMuted, fontSize = 10.sp)
                                    }
                                } else {
                                    items(recentLogs.value.take(20)) { entry ->
                                        Text(
                                            text = entry,
                                            color = AuraTheme.colors.textSecondary,
                                            fontSize = 9.sp,
                                            fontFamily = FontFamily.Monospace,
                                            lineHeight = 12.sp
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

    if (showCrashLogDialog) {
        AlertDialog(
            onDismissRequest = { showCrashLogDialog = false },
            title = {
                Text(
                    text = "AURA CRASH FORENSICS",
                    color = AuraTheme.colors.textPrimary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
            },
            text = {
                Surface(
                    color = AuraTheme.colors.screenBackground,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(300.dp)
                ) {
                    LazyColumn(modifier = Modifier.padding(10.dp)) {
                        item {
                            Text(
                                text = crashLogContent,
                                color = AuraTheme.colors.textPrimary,
                                fontSize = 10.sp,
                                fontFamily = FontFamily.Monospace,
                                lineHeight = 13.sp
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showCrashLogDialog = false }) {
                    Text("CLOSE", color = AuraTheme.colors.accentBrand)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        AuraCrashHandler.clearCrashLogFile(context)
                        crashLogContent = "Crash log cleared."
                    }
                ) {
                    Text("CLEAR FILE 🗑️", color = AuraTheme.colors.negativeRed)
                }
            }
        )
    }
}

@Composable
private fun TelemetryRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, color = AuraTheme.colors.textMuted, fontSize = 11.sp)
        Text(text = value, color = AuraTheme.colors.textPrimary, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
    }
}

@Composable
private fun CountPill(label: String, count: String) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(AuraTheme.colors.screenBackground)
            .padding(horizontal = 10.dp, vertical = 6.dp)
    ) {
        Text(text = count, color = AuraTheme.colors.textPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
        Text(text = label, color = AuraTheme.colors.textMuted, fontSize = 9.sp)
    }
}

@Composable
private fun PendingOpItem(op: PendingOperation) {
    Surface(
        color = AuraTheme.colors.screenBackground,
        shape = RoundedCornerShape(6.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = op.operationType,
                        color = when (op.operationType) {
                            "DELETE" -> AuraTheme.colors.negativeRed
                            "INSERT", "CREATE" -> AuraTheme.colors.positiveGreen
                            else -> AuraTheme.colors.accentBrand
                        },
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                    Text(
                        text = op.entityType,
                        color = AuraTheme.colors.textPrimary,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
                Text(
                    text = "ID: ${op.entitySyncId.take(12)}... • retries: ${op.retryCount}",
                    color = AuraTheme.colors.textMuted,
                    fontSize = 9.sp,
                    fontFamily = FontFamily.Monospace
                )
            }
        }
    }
}
