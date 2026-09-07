package com.example.ui

import androidx.compose.animation.*
import androidx.compose.foundation.*
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.*
import com.example.ui.theme.*
import com.example.ui.anim.auraSpringPress
import com.example.ui.anim.ShimmerTimelineRow
import com.example.ui.components.*
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun JournalAndCalendarScreen(
    viewModel: AppViewModel,
    onOpenDrawingWorkspace: (String?) -> Unit
) {
    // Collect Activities flowchart
    val activities by viewModel.todayActivitiesFlow.collectAsState(initial = emptyList())
    val selectedDate by viewModel.selectedJournalDate.collectAsState()
    val currentJournal by viewModel.currentJournalEntry.collectAsState()
    val isJournalLoading by viewModel.isHabitsLoading.collectAsState()

    var journalText by remember(currentJournal) { mutableStateOf(currentJournal?.content ?: "") }
    var selectedMood by remember(currentJournal) { mutableStateOf(currentJournal?.mood ?: "") }
    var attachedVoicePath by remember(currentJournal) { mutableStateOf(currentJournal?.voicePath) }
    var attachedDrawingData by remember(currentJournal) { mutableStateOf(currentJournal?.drawingData) }
    var isJournalExpanded by remember { mutableStateOf(false) }

    val moodCategories = listOf(
        "HAPPY" to MoodHappy,
        "CALM" to MoodCalm,
        "CONTENT" to MoodContent,
        "NEUTRAL" to MoodNeutral,
        "CREATIVE" to MoodCreative,
        "TIRED" to MoodTired,
        "SAD" to MoodSad
    )

    // Formatter for calendar
    val todayDateObj = remember { Date() }
    val dayFormat = SimpleDateFormat("dd", Locale.getDefault())
    val monthFormat = SimpleDateFormat("MMMM yyyy", Locale.getDefault())
    val weekDayFormat = SimpleDateFormat("EEEE", Locale.getDefault())

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(AuraTheme.colors.screenBackground)
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // --- 1. TITLE ---
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(top = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = "Day Timeline",
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.ExtraBold,
                        color = AuraTheme.colors.textPrimary
                    )
                    Text(
                        text = "Real-time pipeline: What you did today",
                        style = MaterialTheme.typography.bodySmall,
                        color = AuraTheme.colors.textSecondary
                    )
                }

                AuraHeaderActions(
                    onProClick = { viewModel.navigateTo(Section.SecuritySettings) },
                    onProfileClick = { viewModel.navigateTo(Section.SecuritySettings) }
                )
            }
        }

        // --- 2. COMPACT DYNAMIC CALENDAR (TODAY'S DATE & WEEK STRIP) ---
        item {
            val cardShape = RoundedCornerShape(20.dp)
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(cardShape)
                    .border(1.dp, AuraTheme.colors.cardBorder, cardShape),
                colors = CardDefaults.cardColors(containerColor = AuraTheme.colors.cardBackground),
                shape = cardShape
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    // Left Part: Primary Today big display
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(52.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(AuraTheme.colors.accentBrand),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = dayFormat.format(todayDateObj).uppercase(),
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Black,
                                    color = Color.White
                                )
                                Text(
                                    text = weekDayFormat.format(todayDateObj).take(3).uppercase(),
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White.copy(alpha = 0.9f)
                                )
                            }
                        }

                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(
                                text = weekDayFormat.format(todayDateObj).uppercase(),
                                style = MaterialTheme.typography.titleMedium,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = AuraTheme.colors.textPrimary
                            )
                            Text(
                                text = monthFormat.format(todayDateObj).uppercase(),
                                style = MaterialTheme.typography.labelSmall,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = AuraTheme.colors.accentBrand,
                                letterSpacing = 1.sp
                            )
                        }
                    }

                    // Right Part: Compact Calendar strip
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val cal = Calendar.getInstance()
                        val daysToShow = mutableListOf<Triple<String, String, String>>()
                        
                        cal.add(Calendar.DAY_OF_YEAR, -2)
                        val sdfKey = SimpleDateFormat("yyyy-MM-dd", Locale.US)
                        for (i in 0 until 5) {
                            val key = sdfKey.format(cal.time)
                            val dNum = cal.get(Calendar.DAY_OF_MONTH).toString()
                            val dName = when (cal.get(Calendar.DAY_OF_WEEK)) {
                                Calendar.SUNDAY -> "Su"
                                Calendar.MONDAY -> "Mo"
                                Calendar.TUESDAY -> "Tu"
                                Calendar.WEDNESDAY -> "We"
                                Calendar.THURSDAY -> "Th"
                                Calendar.FRIDAY -> "Fr"
                                Calendar.SATURDAY -> "Sa"
                                else -> ""
                            }
                            daysToShow.add(Triple(dNum, dName, key))
                            cal.add(Calendar.DAY_OF_YEAR, 1)
                        }

                        daysToShow.forEach { (num, name, dateKey) ->
                            val chosen = selectedDate == dateKey
                            val dayShape = RoundedCornerShape(10.dp)
                            Box(
                                modifier = Modifier
                                    .size(width = 34.dp, height = 46.dp)
                                    .clip(dayShape)
                                    .background(
                                        color = if (chosen) AuraTheme.colors.accentBrand.copy(alpha = 0.2f) else AuraTheme.colors.bottomNavBackground
                                    )
                                    .border(
                                        width = 1.dp,
                                        color = if (chosen) AuraTheme.colors.accentBrand else AuraTheme.colors.cardBorder.copy(alpha = 0.5f),
                                        shape = dayShape
                                    )
                                    .auraSpringPress(
                                        cornerRadius = 10.dp,
                                        onClick = {
                                            viewModel.selectJournalDate(dateKey)
                                            viewModel.selectTaskDate(dateKey)
                                        }
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        text = name,
                                        fontSize = 8.sp,
                                        color = if (chosen) AuraTheme.colors.accentBrand else AuraTheme.colors.textMuted,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = num,
                                        fontSize = 12.sp,
                                        color = if (chosen) AuraTheme.colors.accentBrand else AuraTheme.colors.textPrimary,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // --- 3. ACCORDION: QUICK DAILY MENTAL REFLECTIONS & JOURNALING ---
        item {
            val accordionShape = RoundedCornerShape(16.dp)
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(accordionShape)
                    .auraSpringPress(
                        cornerRadius = 16.dp,
                        onClick = { isJournalExpanded = !isJournalExpanded }
                    )
                    .border(1.dp, AuraTheme.colors.cardBorder, accordionShape),
                shape = accordionShape,
                colors = CardDefaults.cardColors(containerColor = AuraTheme.colors.cardBackground)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(AuraTheme.colors.accentBrand.copy(alpha = 0.14f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.MenuBook,
                                    contentDescription = "Journal",
                                    tint = AuraTheme.colors.accentBrand,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                            Text(
                                text = "DAILY MENTAL REFLECTIONS",
                                style = MaterialTheme.typography.titleMedium,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = AuraTheme.colors.textPrimary,
                                letterSpacing = 1.sp
                            )
                        }
                        Icon(
                            imageVector = if (isJournalExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                            contentDescription = "Expand",
                            tint = AuraTheme.colors.textMuted,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    if (isJournalExpanded) {
                        Spacer(modifier = Modifier.height(14.dp))
                        
                        // Mood selector
                        Text(
                            text = "CURRENT COGNITIVE STATE",
                            style = MaterialTheme.typography.labelSmall,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = AuraTheme.colors.textMuted,
                            letterSpacing = 1.sp
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            moodCategories.forEach { (moodName, colorsVal) ->
                                val isSelected = selectedMood == moodName
                                val moodShape = RoundedCornerShape(8.dp)
                                Box(
                                    modifier = Modifier
                                        .clip(moodShape)
                                        .background(
                                            color = if (isSelected) colorsVal.copy(alpha = 0.2f) else AuraTheme.colors.bottomNavBackground
                                        )
                                        .border(
                                            width = 1.dp,
                                            color = if (isSelected) colorsVal else AuraTheme.colors.cardBorder.copy(alpha = 0.4f),
                                            shape = moodShape
                                        )
                                        .auraSpringPress(
                                            cornerRadius = 8.dp,
                                            onClick = { selectedMood = moodName }
                                        )
                                        .padding(horizontal = 10.dp, vertical = 6.dp)
                                ) {
                                    Text(
                                        text = moodName,
                                        style = MaterialTheme.typography.labelSmall,
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isSelected) colorsVal else AuraTheme.colors.textSecondary
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Reflection input
                        OutlinedTextField(
                            value = journalText,
                            onValueChange = { journalText = it },
                            placeholder = { Text("Log cognitive notes, workouts, ideas, mental health patterns...", color = AuraTheme.colors.textMuted, fontSize = 12.sp) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 80.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = AuraTheme.colors.accentBrand,
                                unfocusedBorderColor = AuraTheme.colors.cardBorder,
                                focusedTextColor = AuraTheme.colors.textPrimary,
                                unfocusedTextColor = AuraTheme.colors.textPrimary
                            ),
                            shape = RoundedCornerShape(12.dp)
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        // Action Chips
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            AssistChip(
                                onClick = { onOpenDrawingWorkspace(attachedDrawingData) },
                                label = { Text("Canvas Paint", color = AuraTheme.colors.textPrimary, fontSize = 10.sp) },
                                leadingIcon = {
                                    Icon(
                                        Icons.Default.Gesture,
                                        contentDescription = "Sketches",
                                        tint = if (attachedDrawingData != null) AuraTheme.colors.accentBrand else AuraTheme.colors.textMuted,
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                            )

                            val isRecordingGlobally by viewModel.isRecording.collectAsState()
                            AssistChip(
                                onClick = {
                                    if (isRecordingGlobally) {
                                        viewModel.stopAudioNoteRecording()
                                        attachedVoicePath = viewModel.getVoiceNoteFile()
                                    } else {
                                        val path = viewModel.startAudioNoteRecording()
                                        if (path != null) {
                                            attachedVoicePath = path
                                        }
                                    }
                                },
                                label = {
                                    Text(
                                        text = if (isRecordingGlobally) "Rec..." else "Voice",
                                        color = AuraTheme.colors.textPrimary,
                                        fontSize = 10.sp
                                    )
                                },
                                leadingIcon = {
                                    Icon(
                                        imageVector = if (isRecordingGlobally) Icons.Default.FiberManualRecord else Icons.Default.Mic,
                                        contentDescription = "Mic",
                                        tint = if (isRecordingGlobally) AuraTheme.colors.negativeRed else if (attachedVoicePath != null) AuraTheme.colors.positiveGreen else AuraTheme.colors.textMuted,
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                            )

                            Spacer(modifier = Modifier.weight(1f))

                            AuraPrimaryAction(
                                text = "SAVE LOG",
                                onClick = {
                                    viewModel.saveJournal(
                                        content = journalText,
                                        mood = selectedMood,
                                        voicePath = attachedVoicePath,
                                        drawingData = attachedDrawingData
                                    )
                                    isJournalExpanded = false
                                },
                                containerColor = AuraTheme.colors.accentBrand
                            )
                        }
                    }
                }
            }
        }

        // --- 4. FLOWCHART TITLE ---
        item {
            Text(
                text = "DAY FLOW CHART PIPELINE",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = AuraTheme.colors.accentBrand,
                letterSpacing = 1.sp,
                modifier = Modifier.padding(top = 8.dp)
            )
        }

        // --- 5. ACTIVITY LIST TIMELINE PIPELINE ---
        if (isJournalLoading) {
            items(4) {
                ShimmerTimelineRow()
            }
        } else if (activities.isEmpty()) {
            item {
                AuraEmptyState(
                    title = "No Activities Logged Today",
                    description = "Complete tasks, write notes, or log transactions to see your real-time activity timeline here.",
                    icon = Icons.Default.TrendingFlat,
                    iconTint = AuraTheme.colors.textMuted
                )
            }
        } else {
            // Flowchart timeline renderer
            items(activities) { act ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(IntrinsicSize.Min)
                ) {
                    // LEFT COLUMN: FLOW CHART NODE & CONNECTOR LINE
                    Box(
                        modifier = Modifier
                            .width(42.dp)
                            .fillMaxHeight(),
                        contentAlignment = Alignment.TopCenter
                    ) {
                        // Vertical Flow connector
                        Box(
                            modifier = Modifier
                                .width(2.dp)
                                .fillMaxHeight()
                                .background(AuraTheme.colors.cardBorder)
                        )

                        // Circular Node symbol
                        val nodeColor = when (act.type) {
                            "NOTE" -> AuraTheme.colors.accentBrand
                            "TASK" -> AuraTheme.colors.gold
                            "TRANSACTION" -> AuraTheme.colors.positiveGreen
                            else -> AuraTheme.colors.textPrimary
                        }
                        val nodeIcon = when (act.type) {
                            "NOTE" -> Icons.Default.Notes
                            "TASK" -> if (act.isDone) Icons.Default.TaskAlt else Icons.Default.PendingActions
                            "TRANSACTION" -> Icons.Default.Payments
                            else -> Icons.Default.Grain
                        }

                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .background(AuraTheme.colors.screenBackground, CircleShape)
                                .border(2.dp, nodeColor, CircleShape)
                                .padding(2.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = nodeIcon,
                                contentDescription = act.type,
                                tint = nodeColor,
                                modifier = Modifier.size(13.dp)
                            )
                        }
                    }

                    // RIGHT COLUMN: DETAILED FLOW CARD WITH THEMATIC CORNERS
                    Card(
                        modifier = Modifier
                            .weight(1f)
                            .padding(bottom = 12.dp)
                            .border(1.dp, AuraTheme.colors.cardBorder, RoundedCornerShape(14.dp)),
                        colors = CardDefaults.cardColors(containerColor = AuraTheme.colors.cardBackground),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.Top
                            ) {
                                Text(
                                    text = act.title,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Black,
                                    color = AuraTheme.colors.textPrimary,
                                    modifier = Modifier.weight(1f),
                                    textDecoration = if (act.isDone) TextDecoration.LineThrough else null
                                )
                                Text(
                                    text = act.time.uppercase(),
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = AuraTheme.colors.accentBrand,
                                    modifier = Modifier.padding(start = 10.dp)
                                )
                            }

                            Spacer(modifier = Modifier.height(4.dp))

                            Text(
                                text = act.description,
                                fontSize = 11.sp,
                                color = AuraTheme.colors.textSecondary
                            )

                            if (!act.extraInfo.isNullOrBlank()) {
                                Spacer(modifier = Modifier.height(6.dp))
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .background(AuraTheme.colors.bottomNavBackground, RoundedCornerShape(6.dp))
                                            .border(1.dp, AuraTheme.colors.cardBorder.copy(alpha = 0.5f), RoundedCornerShape(6.dp))
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = act.extraInfo,
                                            fontSize = 8.sp,
                                            color = AuraTheme.colors.textMuted,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Safety Bottom spacing
        item {
            Spacer(modifier = Modifier.height(100.dp))
        }
    }
}

// Fluent Layout Modifier Utilities
private fun Modifier.fillModifierCompact(): Modifier = this.fillMaxWidth()
