package com.example.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.animation.*
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
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
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import com.example.data.*
import com.example.ui.AuraHaptics
import com.example.ui.theme.*
import com.example.ui.anim.auraSpringPress
import com.example.ui.anim.ShimmerTaskRow
import com.example.ui.anim.ShimmerKanbanCard
import com.example.ui.components.*
import java.text.SimpleDateFormat
import java.util.*
import kotlinx.coroutines.launch

@Composable
fun TasksScreen(
    viewModel: AppViewModel,
    onOpenTaskComposer: (Task?) -> Unit
) {
    val view = LocalView.current
    val tasksList by viewModel.allTasks.collectAsState()
    val filterCategory by viewModel.tasksFilterCategory.collectAsState()
    val filterPriority by viewModel.tasksFilterPriority.collectAsState()
    val isTasksLoading by viewModel.isTasksLoading.collectAsState()

    var showKanbanBoard by remember { mutableStateOf(true) }
    var taskToTime by remember { mutableStateOf<Task?>(null) }
    var selectedTimerMinutes by remember { mutableStateOf("25") }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(AuraTheme.colors.screenBackground)
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // App bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = "Objectives",
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.ExtraBold,
                        color = AuraTheme.colors.textPrimary
                    )
                    Text(
                        text = "${tasksList.filter { !it.isCompleted }.size} active objectives scheduled",
                        style = MaterialTheme.typography.bodySmall,
                        color = AuraTheme.colors.textSecondary
                    )
                }

                AuraHeaderActions(
                    onProClick = { viewModel.navigateTo(Section.SecuritySettings) },
                    onProfileClick = { viewModel.navigateTo(Section.SecuritySettings) }
                )
            }

            // View toggle switcher toolbar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier
                        .background(AuraTheme.colors.cardBackground, RoundedCornerShape(12.dp))
                        .border(1.dp, AuraTheme.colors.cardBorder, RoundedCornerShape(12.dp))
                        .padding(3.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = { showKanbanBoard = true },
                        modifier = Modifier
                            .size(36.dp)
                            .background(
                                if (showKanbanBoard) AuraTheme.colors.accentBrand else Color.Transparent,
                                RoundedCornerShape(8.dp)
                            )
                    ) {
                        Icon(
                            imageVector = Icons.Default.Dashboard,
                            contentDescription = "Kanbanboard View",
                            tint = if (showKanbanBoard) Color.White else AuraTheme.colors.textSecondary,
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    IconButton(
                        onClick = { showKanbanBoard = false },
                        modifier = Modifier
                            .size(36.dp)
                            .background(
                                if (!showKanbanBoard) AuraTheme.colors.accentBrand else Color.Transparent,
                                RoundedCornerShape(8.dp)
                            )
                    ) {
                        Icon(
                            imageVector = Icons.Default.FormatListBulleted,
                            contentDescription = "Standard List view",
                            tint = if (!showKanbanBoard) Color.White else AuraTheme.colors.textSecondary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            // Filters controls
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Category tag Selector
                var catMenuExpanded by remember { mutableStateOf(false) }
                Box {
                    AssistChip(
                        onClick = { catMenuExpanded = true },
                        label = { Text("Label: $filterCategory", color = AuraTheme.colors.textPrimary, fontSize = 11.sp, fontWeight = FontWeight.SemiBold) },
                        colors = AssistChipDefaults.assistChipColors(containerColor = AuraTheme.colors.cardBackground),
                        border = BorderStroke(1.dp, AuraTheme.colors.cardBorder)
                    )
                    DropdownMenu(
                        expanded = catMenuExpanded,
                        onDismissRequest = { catMenuExpanded = false },
                        modifier = Modifier.background(AuraTheme.colors.cardBackground)
                    ) {
                        listOf("All", "General", "Work", "Urgent Objectives", "Life").forEach { item ->
                            DropdownMenuItem(
                                text = { Text(item, color = AuraTheme.colors.textPrimary) },
                                onClick = {
                                    viewModel.setTaskFilterCategory(item)
                                    catMenuExpanded = false
                                }
                            )
                        }
                    }
                }

                // Priority filter Selector
                var priorityMenuExpanded by remember { mutableStateOf(false) }
                Box {
                    AssistChip(
                        onClick = { priorityMenuExpanded = true },
                        label = { Text("Priority: $filterPriority", color = AuraTheme.colors.textPrimary, fontSize = 11.sp, fontWeight = FontWeight.SemiBold) },
                        colors = AssistChipDefaults.assistChipColors(containerColor = AuraTheme.colors.cardBackground),
                        border = BorderStroke(1.dp, AuraTheme.colors.cardBorder)
                    )
                    DropdownMenu(
                        expanded = priorityMenuExpanded,
                        onDismissRequest = { priorityMenuExpanded = false },
                        modifier = Modifier.background(AuraTheme.colors.cardBackground)
                    ) {
                        listOf("All", "Low", "Medium", "High", "Urgent").forEach { item ->
                            DropdownMenuItem(
                                text = { Text(item, color = AuraTheme.colors.textPrimary) },
                                onClick = {
                                    viewModel.setTaskFilterPriority(item)
                                    priorityMenuExpanded = false
                                }
                            )
                        }
                    }
                }
            }

        Spacer(modifier = Modifier.height(12.dp))

        // PERSISTENT ACTIVE FOCUS TIMER STATUS BANNER
        val activeTimerId by viewModel.activeTimerTaskId.collectAsState()
        val timerSecondsLeft by viewModel.timerSecondsLeft.collectAsState()
        val isTimerRunning by viewModel.isTimerRunning.collectAsState()

        if (activeTimerId != null) {
            val timedTask = tasksList.find { it.id == activeTimerId }
            if (timedTask != null) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp)
                        .border(1.dp, AuraTheme.colors.accentBrand, RoundedCornerShape(14.dp)),
                    colors = CardDefaults.cardColors(containerColor = AuraTheme.colors.cardBackground),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.HourglassEmpty,
                                    contentDescription = "Active Timer",
                                    tint = AuraTheme.colors.accentBrand,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "ACTIVE POMODORO FOCUS SESSION",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = AuraTheme.colors.accentBrand,
                                    letterSpacing = 1.sp
                                )
                            }
                            IconButton(
                                onClick = { viewModel.resetTaskTimer() },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(Icons.Default.Close, contentDescription = "Close Timer", tint = AuraTheme.colors.negativeRed, modifier = Modifier.size(14.dp))
                            }
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        Text(
                            text = timedTask.title,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = AuraTheme.colors.textPrimary
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            val mins = timerSecondsLeft / 60
                            val secs = timerSecondsLeft % 60
                            val formattedTime = "%02d:%02d".format(mins, secs)

                            Text(
                                text = formattedTime,
                                fontSize = 28.sp,
                                fontWeight = FontWeight.Black,
                                color = AuraTheme.colors.accentBrand,
                                style = androidx.compose.ui.text.TextStyle(fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace)
                            )

                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Button(
                                    onClick = {
                                        AuraHaptics.triggerSelection(view)
                                        if (isTimerRunning) {
                                            viewModel.pauseTaskTimer()
                                        } else {
                                            viewModel.resumeTaskTimer()
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = if (isTimerRunning) AuraTheme.colors.gold else AuraTheme.colors.accentBrand),
                                    shape = RoundedCornerShape(8.dp),
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                                ) {
                                    Icon(
                                        imageVector = if (isTimerRunning) Icons.Default.Pause else Icons.Default.PlayArrow,
                                        contentDescription = "Play/pause button",
                                        tint = Color.Black,
                                        modifier = Modifier.size(12.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = if (isTimerRunning) "PAUSE" else "RESUME",
                                        fontSize = 9.sp,
                                        color = Color.Black,
                                        fontWeight = FontWeight.Bold
                                    )
                                }

                                OutlinedButton(
                                    onClick = {
                                        AuraHaptics.triggerSelection(view)
                                        viewModel.resetTaskTimer()
                                    },
                                    border = BorderStroke(1.dp, AuraTheme.colors.cardBorder),
                                    shape = RoundedCornerShape(8.dp),
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                                ) {
                                    Text("RESET", fontSize = 9.sp, color = AuraTheme.colors.textPrimary, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
        }

        if (isTasksLoading) {
            if (showKanbanBoard) {
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    listOf("TODO", "IN PROGRESS", "DONE").forEach { col ->
                        Column(
                            modifier = Modifier
                                .weight(1.0f)
                                .fillMaxHeight(),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text(col, color = AuraTheme.colors.textMuted, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            repeat(3) {
                                ShimmerKanbanCard()
                            }
                        }
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(6) {
                        ShimmerTaskRow()
                    }
                }
            }
        } else if (showKanbanBoard) {
            KanbanBoardLayout(viewModel = viewModel, onOpenComposer = onOpenTaskComposer, onStartTimer = { taskToTime = it })
        } else {
            // Main Standard List view
            tasksList.let { listState ->
                val filtered = listState.filter {
                    (filterCategory == "All" || it.category == filterCategory) &&
                    (filterPriority == "All" || it.priority == filterPriority)
                }

                if (filtered.isEmpty()) {
                    Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                        AuraEmptyState(
                            title = "No Objectives Found",
                            description = "No tasks match your selected filter criteria. Create a new objective to get started.",
                            icon = Icons.Default.HourglassDisabled,
                            iconTint = AuraTheme.colors.accentBrand
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(filtered, key = { it.id }) { task ->
                            TaskRowItem(
                                task = task,
                                onClicked = { onOpenTaskComposer(task) },
                                onToggleCompleted = { viewModel.toggleTaskCompleted(task) },
                                onStartTimer = { taskToTime = task }
                            )
                        }
                    }
                }
            }
        }
    }

    if (taskToTime != null) {
        AlertDialog(
            onDismissRequest = { taskToTime = null },
            title = { Text("Start Focus Timer", color = AuraTheme.colors.textPrimary, fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Select session duration in minutes for: \"${taskToTime?.title}\"", color = AuraTheme.colors.textSecondary, fontSize = 13.sp)
                    
                    OutlinedTextField(
                        value = selectedTimerMinutes,
                        onValueChange = { selectedTimerMinutes = it.filter { c -> c.isDigit() } },
                        label = { Text("Minutes", color = AuraTheme.colors.accentBrand) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = AuraTheme.colors.accentBrand,
                            unfocusedBorderColor = AuraTheme.colors.cardBorder,
                            focusedTextColor = AuraTheme.colors.textPrimary,
                            unfocusedTextColor = AuraTheme.colors.textPrimary
                        ),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf("10", "15", "25", "50", "90").forEach { preset ->
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .background(AuraTheme.colors.bottomNavBackground, RoundedCornerShape(8.dp))
                                    .border(1.dp, if (selectedTimerMinutes == preset) AuraTheme.colors.accentBrand else AuraTheme.colors.cardBorder, RoundedCornerShape(8.dp))
                                    .clickable { selectedTimerMinutes = preset }
                                    .padding(vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(preset, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = AuraTheme.colors.textPrimary)
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val mins = selectedTimerMinutes.toIntOrNull() ?: 25
                        taskToTime?.let { viewModel.startTaskTimer(it.id, mins) }
                        taskToTime = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AuraTheme.colors.accentBrand)
                ) {
                    Text("START TIMER", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { taskToTime = null }) {
                    Text("CANCEL", color = AuraTheme.colors.textSecondary)
                }
            },
            containerColor = AuraTheme.colors.cardBackground
        )
    }

    // Radiant Orange Floating Action Button (FAB)
    AuraFloatingActionButton(
        onClick = { onOpenTaskComposer(null) },
        contentDescription = "New Objective",
        modifier = Modifier
            .align(Alignment.BottomEnd)
            .padding(bottom = 80.dp, end = 20.dp)
    )
}
}

@Composable
fun KanbanBoardLayout(
    viewModel: AppViewModel,
    onOpenComposer: (Task?) -> Unit,
    onStartTimer: (Task) -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    val tasksList by viewModel.allTasks.collectAsState()
    val filterCategory by viewModel.tasksFilterCategory.collectAsState()
    val filterPriority by viewModel.tasksFilterPriority.collectAsState()

    // Kanban boards headers
    val columns = listOf("TO DO", "IN PROGRESS", "COMPLETED")
    var currentColumnSelected by remember { mutableStateOf(0) } // 0, 1, 2 for pagination on Compact screens

    Column(modifier = Modifier.fillMaxSize()) {
        // Tab controls for mobile screen sliding columns
        TabRow(
            selectedTabIndex = currentColumnSelected,
            containerColor = AuraTheme.colors.cardBackground,
            contentColor = AuraTheme.colors.accentBrand,
            indicator = { tabPositions ->
                TabRowDefaults.SecondaryIndicator(
                    modifier = Modifier.tabIndicatorOffset(tabPositions[currentColumnSelected]),
                    color = AuraTheme.colors.accentBrand
                )
            }
        ) {
            columns.forEachIndexed { idx, colTitle ->
                Tab(
                    selected = idx == currentColumnSelected,
                    onClick = { currentColumnSelected = idx },
                    text = { Text(colTitle, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = if (idx == currentColumnSelected) AuraTheme.colors.accentBrand else AuraTheme.colors.textMuted) }
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Pagination content
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
        ) {
            val columnTasks = remember(tasksList, currentColumnSelected, filterCategory, filterPriority) {
                tasksList.filter {
                    val matchingColumn = when (currentColumnSelected) {
                        0 -> !it.isCompleted && it.description != "In Progress"
                        1 -> !it.isCompleted && it.description == "In Progress"
                        else -> it.isCompleted
                    }
                    matchingColumn &&
                    (filterCategory == "All" || it.category == filterCategory) &&
                    (filterPriority == "All" || it.priority == filterPriority)
                }
            }

            if (columnTasks.isEmpty()) {
                val emptyTitle = when (currentColumnSelected) {
                    0 -> "No Pending Tasks"
                    1 -> "Nothing In Progress"
                    else -> "No Completed Tasks"
                }
                val emptyDesc = when (currentColumnSelected) {
                    0 -> "All planned tasks are moved or finished. Add a new objective above."
                    1 -> "Move tasks here from To Do to track your active focus workflow."
                    else -> "Complete tasks to see your finished achievements archived here."
                }
                AuraEmptyState(
                    title = emptyTitle,
                    description = emptyDesc,
                    icon = Icons.Outlined.CheckCircle,
                    iconTint = AuraTheme.colors.accentBrand
                )
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxSize()) {
                    items(columnTasks, key = { it.id }) { task ->
                        KanbanTaskCard(
                            task = task,
                            onClicked = { onOpenComposer(task) },
                            onAdvance = {
                                coroutineScope.launch {
                                    val nextDesc = if (currentColumnSelected == 0) "In Progress" else "Completed"
                                    viewModel.updateTask(task.copy(
                                        description = nextDesc,
                                        isCompleted = currentColumnSelected == 1
                                    ))
                                }
                            },
                            onRegress = {
                                coroutineScope.launch {
                                    val prevDesc = if (currentColumnSelected == 2) "In Progress" else ""
                                    viewModel.updateTask(task.copy(
                                        description = prevDesc,
                                        isCompleted = false
                                    ))
                                }
                            },
                            currentStage = currentColumnSelected,
                            onStartTimer = { onStartTimer(task) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun TaskRowItem(
    task: Task,
    onClicked: () -> Unit,
    onToggleCompleted: () -> Unit,
    onStartTimer: () -> Unit
) {
    val scaleVal by animateFloatAsState(targetValue = if (task.isCompleted) 0.96f else 1f, spring(0.5f), label = "haptic spring")

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .scale(scaleVal)
            .border(
                1.dp,
                if (task.isCompleted) AuraTheme.colors.cardBorder.copy(alpha = 0.5f) else AuraTheme.colors.cardBorder,
                RoundedCornerShape(12.dp)
            )
            .auraSpringPress(
                cornerRadius = 12.dp,
                onClick = onClicked
            )
            .testTag("task_row_card_${task.id}"),
        colors = CardDefaults.cardColors(
            containerColor = if (task.isCompleted) AuraTheme.colors.cardBackground.copy(alpha = 0.5f) else AuraTheme.colors.cardBackground
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                // Circular completion selector
                IconButton(onClick = onToggleCompleted, modifier = Modifier.size(28.dp)) {
                    Icon(
                        imageVector = if (task.isCompleted) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                        contentDescription = "Completed checkbox",
                        tint = if (task.isCompleted) AuraTheme.colors.positiveGreen else AuraTheme.colors.textMuted,
                        modifier = Modifier.size(22.dp)
                    )
                }

                Spacer(modifier = Modifier.width(10.dp))

                Column {
                    Text(
                        text = task.title,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (task.isCompleted) AuraTheme.colors.textMuted else AuraTheme.colors.textPrimary,
                        textDecoration = if (task.isCompleted) TextDecoration.LineThrough else TextDecoration.None,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        // Priority Badge
                        Box(
                            modifier = Modifier
                                .background(getPriorityColor(task.priority).copy(alpha = 0.2f), RoundedCornerShape(4.dp))
                                .padding(horizontal = 4.dp, vertical = 2.dp)
                        ) {
                            Text(task.priority, fontSize = 8.sp, fontWeight = FontWeight.Bold, color = getPriorityColor(task.priority))
                        }

                        // Energy level badge
                        Text(task.energy, fontSize = 9.sp, color = AuraTheme.colors.textSecondary)

                        // Alarm Badge
                        if (!task.time.isNullOrBlank()) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.NotificationsActive, contentDescription = "Reminder active", tint = AuraTheme.colors.accentBrand, modifier = Modifier.size(10.dp))
                                Spacer(modifier = Modifier.width(2.dp))
                                Text(task.time, fontSize = 9.sp, color = AuraTheme.colors.accentBrand, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (!task.isCompleted) {
                    IconButton(onClick = onStartTimer, modifier = Modifier.size(28.dp)) {
                        Icon(
                            imageVector = Icons.Default.PlayCircle,
                            contentDescription = "Start Timer",
                            tint = AuraTheme.colors.accentBrand,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
                Text(task.category, fontSize = 9.sp, color = AuraTheme.colors.textMuted, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
fun KanbanTaskCard(
    task: Task,
    onClicked: () -> Unit,
    onAdvance: () -> Unit,
    onRegress: () -> Unit,
    currentStage: Int,
    onStartTimer: () -> Unit
) {
    val shape = RoundedCornerShape(16.dp)
    val priorityColor = getPriorityColor(task.priority)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .border(1.dp, AuraTheme.colors.cardBorder, shape)
            .auraSpringPress(
                cornerRadius = 16.dp,
                onClick = onClicked
            ),
        colors = CardDefaults.cardColors(containerColor = AuraTheme.colors.cardBackground),
        shape = shape
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Top Row: Category + Priority Pill
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(AuraTheme.colors.accentBrand.copy(alpha = 0.14f))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = task.category.uppercase(),
                        style = MaterialTheme.typography.labelSmall,
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Bold,
                        color = AuraTheme.colors.accentBrand,
                        letterSpacing = 0.8.sp
                    )
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(priorityColor.copy(alpha = 0.15f))
                        .border(0.8.dp, priorityColor.copy(alpha = 0.4f), RoundedCornerShape(6.dp))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = task.priority.uppercase(),
                        style = MaterialTheme.typography.labelSmall,
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Bold,
                        color = priorityColor,
                        letterSpacing = 0.5.sp
                    )
                }
            }

            // Task Title
            Text(
                text = task.title,
                style = MaterialTheme.typography.titleMedium,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = AuraTheme.colors.textPrimary,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            // Energy & Reminder Metadata
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "⚡ ${task.energy}",
                    style = MaterialTheme.typography.bodySmall,
                    fontSize = 10.sp,
                    color = AuraTheme.colors.textSecondary
                )

                if (!task.time.isNullOrBlank()) {
                    Text(
                        text = "⏰ ${task.time}",
                        style = MaterialTheme.typography.bodySmall,
                        fontSize = 10.sp,
                        color = AuraTheme.colors.accentBrand,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Bottom action footer
            HorizontalDivider(color = AuraTheme.colors.cardBorder.copy(alpha = 0.6f), thickness = 0.8.dp)

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (currentStage < 2) {
                    IconButton(
                        onClick = onStartTimer,
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayCircle,
                            contentDescription = "Start Focus Timer",
                            tint = AuraTheme.colors.accentBrand,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                } else {
                    Spacer(modifier = Modifier.width(1.dp))
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    if (currentStage > 0) {
                        IconButton(
                            onClick = onRegress,
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.ArrowBackIosNew,
                                contentDescription = "Move Left",
                                tint = AuraTheme.colors.textSecondary,
                                modifier = Modifier.size(12.dp)
                            )
                        }
                    }
                    if (currentStage < 2) {
                        IconButton(
                            onClick = onAdvance,
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.ArrowForwardIos,
                                contentDescription = "Move Right",
                                tint = AuraTheme.colors.accentBrand,
                                modifier = Modifier.size(12.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun getPriorityColor(priority: String): Color {
    return when (priority.lowercase()) {
        "urgent" -> AuraTheme.colors.negativeRed
        "high" -> AuraTheme.colors.accentBrand
        "medium" -> AuraTheme.colors.gold
        "low" -> AuraTheme.colors.positiveGreen
        else -> AuraTheme.colors.textSecondary
    }
}

// ==========================================
// TASKS COMPOSER SCREEN
// ==========================================
@Composable
fun TaskComposerScreen(
    task: Task?,
    viewModel: AppViewModel,
    onBack: () -> Unit
) {
    var title by remember { mutableStateOf(task?.title ?: "") }
    var description by remember { mutableStateOf(task?.description ?: "") }
    var priority by remember { mutableStateOf(task?.priority ?: "Medium") }
    var energy by remember { mutableStateOf(task?.energy ?: "Medium Energy") }
    var date by remember { mutableStateOf(task?.date ?: SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())) }
    var time by remember { mutableStateOf(task?.time ?: "") }
    var category by remember { mutableStateOf(task?.category ?: "General") }
    var recurrence by remember { mutableStateOf(task?.recurrence ?: "None") }

    // Draft subtasks lists
    val availableSubtasks by viewModel.activeSubtasks.collectAsState()
    var draftSubtaskText by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AuraTheme.colors.screenBackground)
    ) {
        // App header bar
        Surface(color = AuraTheme.colors.cardBackground, modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 12.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        task?.let { "EDIT OBJECTIVE" } ?: "NEW OBJECTIVE",
                        fontSize = 15.sp,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                }

                Button(
                    onClick = {
                        if (title.isNotBlank()) {
                            viewModel.saveTask(
                                title = title,
                                description = description,
                                priority = priority,
                                energy = energy,
                                date = date,
                                time = time.ifBlank { null },
                                category = category,
                                tags = "",
                                recurrence = recurrence,
                                subtaskTitles = listOf(draftSubtaskText)
                            )
                            onBack()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AuraTheme.colors.accentBrand),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Save", color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
        }

        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Task Title inputs
            item {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    placeholder = { Text("What needs to be done?", color = AuraTheme.colors.textMuted) },
                    label = { Text("Objective Title", color = AuraTheme.colors.accentBrand) },
                    modifier = Modifier.fillMaxWidth().testTag("task_title_input"),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = AuraTheme.colors.accentBrand,
                        unfocusedBorderColor = AuraTheme.colors.cardBorder,
                        focusedTextColor = AuraTheme.colors.textPrimary,
                        unfocusedTextColor = AuraTheme.colors.textPrimary
                    ),
                    shape = RoundedCornerShape(12.dp)
                )
            }

            // Categories list chips selection
            item {
                Text("Select Workspace category", fontSize = 11.sp, color = AuraTheme.colors.textMuted)
                Row(modifier = Modifier.padding(top = 4.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("General", "Work", "Urgent Objectives", "Life").forEach { item ->
                        val isSelected = category == item
                        FilterChip(
                            selected = isSelected,
                            onClick = { category = item },
                            label = { Text(item, fontSize = 11.sp, color = if (isSelected) Color.White else AuraTheme.colors.textPrimary) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = AuraTheme.colors.accentBrand,
                                containerColor = AuraTheme.colors.cardBackground
                            )
                        )
                    }
                }
            }

            // Priorities chips selection
            item {
                Text("Priority Level", fontSize = 11.sp, color = AuraTheme.colors.textMuted)
                Row(modifier = Modifier.padding(top = 4.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("Low", "Medium", "High", "Urgent").forEach { item ->
                        val isSelected = priority == item
                        val col = getPriorityColor(item)
                        FilterChip(
                            selected = isSelected,
                            onClick = { priority = item },
                            label = { Text(item, fontSize = 11.sp, color = if (isSelected) Color.White else AuraTheme.colors.textPrimary) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = col,
                                containerColor = AuraTheme.colors.cardBackground
                            )
                        )
                    }
                }
            }

            // Focus Energy level attributes
            item {
                Text("Focus Energy intensity required", fontSize = 11.sp, color = AuraTheme.colors.textMuted)
                Row(modifier = Modifier.padding(top = 4.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("Low Energy", "Medium Energy", "High Energy").forEach { item ->
                        val isSelected = energy == item
                        FilterChip(
                            selected = isSelected,
                            onClick = { energy = item },
                            label = { Text(item, fontSize = 11.sp, color = if (isSelected) Color.White else AuraTheme.colors.textSecondary) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = AuraTheme.colors.gold,
                                containerColor = AuraTheme.colors.cardBackground
                            )
                        )
                    }
                }
            }

            // Date & Schedule Configurations
            item {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    // Due Date input
                    OutlinedTextField(
                        value = date,
                        onValueChange = { date = it },
                        label = { Text("Due Date (yyyy-MM-dd)", color = AuraTheme.colors.textSecondary) },
                        modifier = Modifier.weight(1f),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = AuraTheme.colors.accentBrand,
                            unfocusedBorderColor = AuraTheme.colors.cardBorder,
                            focusedTextColor = AuraTheme.colors.textPrimary,
                            unfocusedTextColor = AuraTheme.colors.textPrimary
                        ),
                        shape = RoundedCornerShape(10.dp)
                    )

                    // Optional Reminder alarm time input (HH:MM)
                    OutlinedTextField(
                        value = time,
                        onValueChange = { time = it },
                        placeholder = { Text("e.g. 09:30 or 18:00", color = AuraTheme.colors.textMuted) },
                        label = { Text("Reminder Alarm (HH:MM)", color = AuraTheme.colors.textSecondary) },
                        modifier = Modifier.weight(1f),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = AuraTheme.colors.accentBrand,
                            unfocusedBorderColor = AuraTheme.colors.cardBorder,
                            focusedTextColor = AuraTheme.colors.textPrimary,
                            unfocusedTextColor = AuraTheme.colors.textPrimary
                        ),
                        shape = RoundedCornerShape(10.dp)
                    )

                    // Optional recurrence
                    var expandRecur by remember { mutableStateOf(false) }
                    Box(modifier = Modifier.weight(1f)) {
                        OutlinedTextField(
                            value = recurrence,
                            onValueChange = { },
                            readOnly = true,
                            label = { Text("Repeat schedule", color = AuraTheme.colors.textSecondary) },
                            trailingIcon = {
                                IconButton(onClick = { expandRecur = true }) {
                                    Icon(Icons.Default.ArrowDropDown, contentDescription = "Dropdown", tint = AuraTheme.colors.textSecondary)
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = AuraTheme.colors.accentBrand,
                                unfocusedBorderColor = AuraTheme.colors.cardBorder,
                                focusedTextColor = AuraTheme.colors.textPrimary,
                                unfocusedTextColor = AuraTheme.colors.textPrimary
                            ),
                            shape = RoundedCornerShape(10.dp)
                        )

                        DropdownMenu(expanded = expandRecur, onDismissRequest = { expandRecur = false }, modifier = Modifier.background(AuraTheme.colors.cardBackground)) {
                            listOf("None", "Daily", "Weekly", "Monthly").forEach { opt ->
                                DropdownMenuItem(text = { Text(opt, color = AuraTheme.colors.textPrimary) }, onClick = {
                                    recurrence = opt
                                    expandRecur = false
                                })
                            }
                        }
                    }
                }
            }

            // RELATIONAL SUBTASKS CHECKLIST NODES
            if (task != null) {
                item {
                    Text("Hierarchical Subtasks checklists", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = AuraTheme.colors.textPrimary)
                    Spacer(modifier = Modifier.height(4.dp))
                }

                items(availableSubtasks) { sub ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(AuraTheme.colors.bottomNavBackground, RoundedCornerShape(8.dp))
                            .border(1.dp, AuraTheme.colors.cardBorder.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            IconButton(onClick = { viewModel.toggleSubtaskCompleted(sub) }, modifier = Modifier.size(24.dp)) {
                                Icon(
                                    imageVector = if (sub.isCompleted) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                                    contentDescription = "Check",
                                    tint = if (sub.isCompleted) AuraTheme.colors.positiveGreen else AuraTheme.colors.textMuted,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = sub.title,
                                fontSize = 12.sp,
                                color = if (sub.isCompleted) AuraTheme.colors.textMuted else AuraTheme.colors.textPrimary,
                                textDecoration = if (sub.isCompleted) TextDecoration.LineThrough else TextDecoration.None
                            )
                        }

                        IconButton(onClick = { viewModel.deleteSubtaskDirectly(sub) }, modifier = Modifier.size(24.dp)) {
                            Icon(Icons.Default.Close, contentDescription = "Delete sub", tint = AuraTheme.colors.negativeRed, modifier = Modifier.size(14.dp))
                        }
                    }
                }

                item {
                    // Fast insert subtask inline
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        var inlineSubTitle by remember { mutableStateOf("") }
                        OutlinedTextField(
                            value = inlineSubTitle,
                            onValueChange = { inlineSubTitle = it },
                            placeholder = { Text("Add secondary subtask node...", fontSize = 11.sp, color = AuraTheme.colors.textMuted) },
                            modifier = Modifier.weight(1f),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = AuraTheme.colors.accentBrand,
                                unfocusedBorderColor = AuraTheme.colors.cardBorder,
                                focusedTextColor = AuraTheme.colors.textPrimary,
                                unfocusedTextColor = AuraTheme.colors.textPrimary
                            ),
                            shape = RoundedCornerShape(8.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Button(
                            onClick = {
                                if (inlineSubTitle.isNotBlank()) {
                                    viewModel.addNewSubtaskDirectly(inlineSubTitle)
                                    inlineSubTitle = ""
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = AuraTheme.colors.accentBrand),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = "Add sub", tint = Color.White)
                        }
                    }
                }
            } else {
                // Draft single subtask text if creating new
                item {
                    OutlinedTextField(
                        value = draftSubtaskText,
                        onValueChange = { draftSubtaskText = it },
                        placeholder = { Text("E.g. Step 1: Research components", color = AuraTheme.colors.textMuted) },
                        label = { Text("Initial subtask checkpoint (optional)", color = AuraTheme.colors.accentBrand) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = AuraTheme.colors.accentBrand,
                            unfocusedBorderColor = AuraTheme.colors.cardBorder,
                            focusedTextColor = AuraTheme.colors.textPrimary,
                            unfocusedTextColor = AuraTheme.colors.textPrimary
                        ),
                        shape = RoundedCornerShape(10.dp)
                    )
                }
            }

            // Delete objective permanently
            if (task != null) {
                item {
                    Spacer(modifier = Modifier.height(24.dp))
                    Button(
                        onClick = {
                            viewModel.deleteTaskPermanently(task)
                            onBack()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = AuraTheme.colors.negativeRed),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color.White)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Delete Objective Permanently", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
