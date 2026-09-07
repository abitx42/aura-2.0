package com.example.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.audio.PlaybackState
import com.example.data.*
import com.example.ui.theme.*
import com.example.ui.anim.auraSpringPress
import com.example.ui.anim.AuraCornerRadius
import com.example.ui.anim.ShimmerDashboardGrid
import com.example.ui.components.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import java.io.File
import android.net.Uri
import android.graphics.Bitmap
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.PickVisualMediaRequest
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.zIndex
import kotlinx.coroutines.flow.firstOrNull

@Composable
fun MainAppContainer(
    viewModel: AppViewModel,
    modifier: Modifier = Modifier
) {
    val activeTab by viewModel.currentSection.collectAsState()
    val isAppUnlocked by viewModel.isAppUnlocked.collectAsState()
    val coroutineScope = rememberCoroutineScope()

    // Trigger initial PIN security lock assessment
    LaunchedEffect(Unit) {
        viewModel.checkSecurityLock()
    }

    Box(modifier = modifier.fillMaxSize().background(AuraTheme.colors.screenBackground)) {
        if (!isAppUnlocked) {
            // High-contrast Glassmorphic security lock screen
            SecurityPinKeypadGate(viewModel = viewModel)
        } else {
            // Main Scaffold with bottom bar routing
            var editNoteItem by remember { mutableStateOf<Note?>(null) }
            var editTaskItem by remember { mutableStateOf<Task?>(null) }
            
            var showAudioRecordDialog by remember { mutableStateOf(false) }
            var showImageCaptureDialog by remember { mutableStateOf(false) }
            var showCustomIconDialog by remember { mutableStateOf(false) }
            var showGlobalQuickTransactionType by remember { mutableStateOf<String?>(null) }
            
            var showOverlayMenu by remember { mutableStateOf(false) }
            val captureColorId by viewModel.captureButtonColor.collectAsState()
            val captureAnimId by viewModel.captureButtonAnimationType.collectAsState()

            val accounts by viewModel.allAccounts.collectAsState()
            val friends by viewModel.allFriends.collectAsState()
            
            // Sub workspace triggers
            var workspaceDrawingData by remember { mutableStateOf<String?>(null) }
            var isDrawingWorkspaceOpen by remember { mutableStateOf(false) }

            // Systematic back button interception
            val isBackHandlingRequired = showOverlayMenu || showCustomIconDialog || showImageCaptureDialog || 
                showAudioRecordDialog || showGlobalQuickTransactionType != null || 
                isDrawingWorkspaceOpen || editNoteItem != null || editTaskItem != null || 
                activeTab != Section.Dashboard

            BackHandler(enabled = isBackHandlingRequired) {
                when {
                    showOverlayMenu -> showOverlayMenu = false
                    showCustomIconDialog -> showCustomIconDialog = false
                    showImageCaptureDialog -> showImageCaptureDialog = false
                    showAudioRecordDialog -> showAudioRecordDialog = false
                    showGlobalQuickTransactionType != null -> showGlobalQuickTransactionType = null
                    isDrawingWorkspaceOpen -> isDrawingWorkspaceOpen = false
                    editNoteItem != null -> editNoteItem = null
                    editTaskItem != null -> editTaskItem = null
                    activeTab != Section.Dashboard -> viewModel.navigateTo(Section.Dashboard)
                }
            }

            var selectedMediaUri by remember { mutableStateOf<Uri?>(null) }
            val context = LocalContext.current

            val iconPickerLauncher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.GetContent(),
                onResult = { uri ->
                    if (uri != null) {
                        viewModel.setQuickCaptureIconUri(uri.toString())
                    }
                }
            )

            val mediaPickerLauncher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.GetContent(),
                onResult = { uri ->
                    if (uri != null) {
                        selectedMediaUri = uri
                    }
                }
            )

            val cameraLauncher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.TakePicturePreview(),
                onResult = { bitmap ->
                    if (bitmap != null) {
                        try {
                            val file = File(context.cacheDir, "camera_capture_${System.currentTimeMillis()}.png")
                            file.outputStream().use { outStream ->
                                bitmap.compress(Bitmap.CompressFormat.PNG, 100, outStream)
                            }
                            selectedMediaUri = Uri.fromFile(file)
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                }
            )

            // Active internal navigation switches
            val contentModifier = Modifier
                .fillMaxSize()
                .navigationBarsPadding()

            Scaffold(
                modifier = Modifier.fillMaxSize(),
                bottomBar = {
                    if (activeTab != Section.RichNoteEditor && activeTab != Section.DrawingWorkspace && !isDrawingWorkspaceOpen) {
                        AuraBottomNavRow(viewModel = viewModel, active = activeTab)
                    }
                },
                floatingActionButton = {
                    val isRobotCompanionEnabled by viewModel.isRobotCompanionEnabled.collectAsState()
                    if (!isRobotCompanionEnabled && activeTab != Section.RichNoteEditor && activeTab != Section.DrawingWorkspace && !isDrawingWorkspaceOpen) {
                        val quickCaptureIconUri by viewModel.quickCaptureIconUri.collectAsState()
                        val haptic = LocalHapticFeedback.current

                        val infiniteTransition = rememberInfiniteTransition()

                        // Pulse Outer Ring Animation for PULSE mode
                        val pulseScale by infiniteTransition.animateFloat(
                            initialValue = 1.0f,
                            targetValue = 1.5f,
                            animationSpec = infiniteRepeatable(
                                animation = tween(1400, easing = LinearOutSlowInEasing),
                                repeatMode = RepeatMode.Restart
                            )
                        )
                        val pulseAlpha by infiniteTransition.animateFloat(
                            initialValue = 0.5f,
                            targetValue = 0.0f,
                            animationSpec = infiniteRepeatable(
                                animation = tween(1400, easing = LinearOutSlowInEasing),
                                repeatMode = RepeatMode.Restart
                            )
                        )

                        // Breathe Animation for BREATHE mode
                        val breatheScale by infiniteTransition.animateFloat(
                            initialValue = 1.0f,
                            targetValue = if (captureAnimId == "BREATHE") 1.12f else 1.0f,
                            animationSpec = infiniteRepeatable(
                                animation = tween(1200, easing = FastOutSlowInEasing),
                                repeatMode = RepeatMode.Reverse
                            )
                        )

                        // Spin Rotation for SPIN mode
                        val spinRotation by infiniteTransition.animateFloat(
                            initialValue = 0f,
                            targetValue = if (captureAnimId == "SPIN") 360f else 0f,
                            animationSpec = infiniteRepeatable(
                                animation = tween(3500, easing = LinearEasing),
                                repeatMode = RepeatMode.Restart
                            )
                        )

                        val buttonBrush = when (captureColorId) {
                            "CYAN" -> Brush.radialGradient(listOf(Color(0xFF00E5FF), Color(0xFF00B0FF)))
                            "PURPLE" -> Brush.radialGradient(listOf(Color(0xFFBB86FC), Color(0xFFD500F9)))
                            "EMERALD" -> Brush.radialGradient(listOf(Color(0xFF00D287), Color(0xFF00C853)))
                            "PINK" -> Brush.radialGradient(listOf(Color(0xFFFF2A85), Color(0xFFFF1744)))
                            "RED" -> Brush.radialGradient(listOf(Color(0xFFFF3E3E), Color(0xFFD50000)))
                            "AMBER" -> Brush.radialGradient(listOf(Color(0xFFFFC107), Color(0xFFFF6D00)))
                            "LIME" -> Brush.radialGradient(listOf(AxioElectricLime, Color(0xFF00D287)))
                            else -> Brush.radialGradient(listOf(AuraTheme.colors.accentBrand, Color(0xFFFF7A50)))
                        }

                        Column(
                            horizontalAlignment = Alignment.End,
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.padding(bottom = 12.dp)
                        ) {
                            // Rich & Clean Frosted Card speed dial overlay
                            AnimatedVisibility(
                                visible = showOverlayMenu,
                                enter = scaleIn(transformOrigin = androidx.compose.ui.graphics.TransformOrigin(1f, 1f), initialScale = 0.7f) + fadeIn(),
                                exit = scaleOut(transformOrigin = androidx.compose.ui.graphics.TransformOrigin(1f, 1f), targetScale = 0.7f) + fadeOut()
                            ) {
                                val menuShape = RoundedCornerShape(24.dp)
                                Card(
                                    shape = menuShape,
                                    colors = CardDefaults.cardColors(containerColor = AuraTheme.colors.cardBackground.copy(alpha = 0.98f)),
                                    border = BorderStroke(1.dp, AuraTheme.colors.cardBorder),
                                    modifier = Modifier
                                        .width(200.dp)
                                        .padding(bottom = 8.dp)
                                        .shadow(16.dp, menuShape, spotColor = AuraTheme.colors.accentBrand.copy(alpha = 0.25f))
                                ) {
                                    Column(
                                        modifier = Modifier.padding(10.dp),
                                        verticalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        // 1. Write Note
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clip(RoundedCornerShape(12.dp))
                                                .auraSpringPress(
                                                    cornerRadius = 12.dp,
                                                    onClick = {
                                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                        showOverlayMenu = false
                                                        viewModel.selectNote(null)
                                                        viewModel.navigateTo(Section.RichNoteEditor)
                                                    }
                                                )
                                                .padding(horizontal = 12.dp, vertical = 8.dp),
                                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(32.dp)
                                                    .clip(RoundedCornerShape(8.dp))
                                                    .background(AuraTheme.colors.accentBrand.copy(alpha = 0.15f)),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(Icons.Default.Edit, contentDescription = null, tint = AuraTheme.colors.accentBrand, modifier = Modifier.size(16.dp))
                                            }
                                            Text("Quick Note", color = AuraTheme.colors.textPrimary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                                        }

                                        // 2. AudioMemo
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clip(RoundedCornerShape(12.dp))
                                                .auraSpringPress(
                                                    cornerRadius = 12.dp,
                                                    onClick = {
                                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                        showOverlayMenu = false
                                                        showAudioRecordDialog = true
                                                    }
                                                )
                                                .padding(horizontal = 12.dp, vertical = 8.dp),
                                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(32.dp)
                                                    .clip(RoundedCornerShape(8.dp))
                                                    .background(AuraTheme.colors.positiveGreen.copy(alpha = 0.15f)),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(Icons.Default.Mic, contentDescription = null, tint = AuraTheme.colors.positiveGreen, modifier = Modifier.size(16.dp))
                                            }
                                            Text("Voice Memo", color = AuraTheme.colors.textPrimary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                                        }

                                        // 3. Take Image
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clip(RoundedCornerShape(12.dp))
                                                .auraSpringPress(
                                                    cornerRadius = 12.dp,
                                                    onClick = {
                                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                        showOverlayMenu = false
                                                        showImageCaptureDialog = true
                                                    }
                                                )
                                                .padding(horizontal = 12.dp, vertical = 8.dp),
                                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(32.dp)
                                                    .clip(RoundedCornerShape(8.dp))
                                                    .background(Color(0xFF00FF87).copy(alpha = 0.15f)),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(Icons.Default.PhotoCamera, contentDescription = null, tint = Color(0xFF00FF87), modifier = Modifier.size(16.dp))
                                            }
                                            Text("Snap Image", color = AuraTheme.colors.textPrimary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                                        }

                                        // 4. Add Payment (Income)
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clip(RoundedCornerShape(12.dp))
                                                .auraSpringPress(
                                                    cornerRadius = 12.dp,
                                                    onClick = {
                                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                        showOverlayMenu = false
                                                        showGlobalQuickTransactionType = "RECEIVED"
                                                    }
                                                )
                                                .padding(horizontal = 12.dp, vertical = 8.dp),
                                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(32.dp)
                                                    .clip(RoundedCornerShape(8.dp))
                                                    .background(AuraTheme.colors.positiveGreen.copy(alpha = 0.15f)),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(Icons.Default.CallReceived, contentDescription = null, tint = AuraTheme.colors.positiveGreen, modifier = Modifier.size(16.dp))
                                            }
                                            Text("Received ₹", color = AuraTheme.colors.positiveGreen, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                                        }

                                        // 5. Deduct Payment (Expense)
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clip(RoundedCornerShape(12.dp))
                                                .auraSpringPress(
                                                    cornerRadius = 12.dp,
                                                    onClick = {
                                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                        showOverlayMenu = false
                                                        showGlobalQuickTransactionType = "SENT"
                                                    }
                                                )
                                                .padding(horizontal = 12.dp, vertical = 8.dp),
                                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(32.dp)
                                                    .clip(RoundedCornerShape(8.dp))
                                                    .background(AuraTheme.colors.negativeRed.copy(alpha = 0.15f)),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(Icons.Default.ArrowOutward, contentDescription = null, tint = AuraTheme.colors.negativeRed, modifier = Modifier.size(16.dp))
                                            }
                                            Text("Spent ₹", color = AuraTheme.colors.negativeRed, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                                        }

                                        // 6. Customize Icon
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clip(RoundedCornerShape(12.dp))
                                                .auraSpringPress(
                                                    cornerRadius = 12.dp,
                                                    onClick = {
                                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                        showOverlayMenu = false
                                                        showCustomIconDialog = true
                                                    }
                                                )
                                                .padding(horizontal = 12.dp, vertical = 8.dp),
                                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(32.dp)
                                                    .clip(RoundedCornerShape(8.dp))
                                                    .background(AuraTheme.colors.badgeGold.copy(alpha = 0.15f)),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = AuraTheme.colors.badgeGold, modifier = Modifier.size(16.dp))
                                            }
                                            Text("Style FAB", color = AuraTheme.colors.textMuted, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                                        }
                                    }
                                }
                            }

                            // Dynamic interactive button
                            Box(
                                modifier = Modifier
                                    .size(58.dp)
                                    .scale(breatheScale),
                                contentAlignment = Alignment.Center
                            ) {
                                // Pulsing halo ring animation behind button if enabled
                                if (captureAnimId == "PULSE") {
                                    Box(
                                        modifier = Modifier
                                            .size(58.dp)
                                            .scale(pulseScale)
                                            .background(buttonBrush, CircleShape)
                                            .graphicsLayer { alpha = pulseAlpha }
                                    )
                                }

                                Box(
                                    modifier = Modifier
                                        .size(56.dp)
                                        .clip(CircleShape)
                                        .background(buttonBrush)
                                        .clickable {
                                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                            showOverlayMenu = !showOverlayMenu
                                        }
                                        .border(2.dp, Color.White, CircleShape)
                                        .testTag("global_fab"),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (quickCaptureIconUri.isNotEmpty()) {
                                        androidx.compose.foundation.Image(
                                            painter = coil.compose.rememberAsyncImagePainter(
                                                 model = coil.request.ImageRequest.Builder(androidx.compose.ui.platform.LocalContext.current)
                                                     .data(quickCaptureIconUri)
                                                     .size(coil.size.Size(120, 120))
                                                     .crossfade(true)
                                                     .memoryCacheKey("quick_capture_fab")
                                                     .build()
                                             ),
                                            contentDescription = "Custom Quick Capture Graphic",
                                            modifier = Modifier.fillMaxSize().clip(CircleShape),
                                            contentScale = ContentScale.Crop
                                        )
                                    } else {
                                        Icon(
                                            imageVector = Icons.Default.Bolt,
                                            contentDescription = "Default Quick Capture Mode",
                                            tint = Color.Black,
                                            modifier = Modifier
                                                .size(28.dp)
                                                .graphicsLayer(rotationZ = spinRotation)
                                        )
                                    }
                                }
                            }
                        }
                    }
                },
                containerColor = AuraTheme.colors.screenBackground
            ) { innerPadding ->
                val isOnline by viewModel.isOnline.collectAsState()
                val pendingOpsCount by viewModel.pendingOpsCount.collectAsState()

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .padding(bottom = if (activeTab != Section.RichNoteEditor && activeTab != Section.DrawingWorkspace) 76.dp else 0.dp)
                ) {
                    // Non-intrusive offline status banner
                    AnimatedVisibility(
                        visible = !isOnline,
                        enter = slideInVertically() + fadeIn(),
                        exit = slideOutVertically() + fadeOut(),
                        modifier = Modifier.align(Alignment.TopCenter).zIndex(10f)
                    ) {
                        Surface(
                            modifier = Modifier.fillMaxWidth().padding(8.dp),
                            color = MaterialTheme.colorScheme.errorContainer,
                            shape = RoundedCornerShape(12.dp),
                            tonalElevation = 6.dp
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.CloudOff,
                                    contentDescription = "Offline",
                                    tint = MaterialTheme.colorScheme.onErrorContainer
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    "Offline mode active",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onErrorContainer,
                                    modifier = Modifier.weight(1f)
                                )
                                if (pendingOpsCount > 0) {
                                    Spacer(Modifier.width(8.dp))
                                    Badge(
                                        containerColor = MaterialTheme.colorScheme.error,
                                        contentColor = MaterialTheme.colorScheme.onError
                                    ) { Text("$pendingOpsCount pending sync") }
                                }
                            }
                        }
                    }

                    // Elegant semi-transparent backdrop overlay when Quick Capture menu is active
                    AnimatedVisibility(
                        visible = showOverlayMenu,
                        enter = fadeIn(animationSpec = tween(250)),
                        exit = fadeOut(animationSpec = tween(250))
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color.Black.copy(alpha = 0.6f))
                                .clickable(
                                    onClick = { showOverlayMenu = false },
                                    interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                                    indication = null
                                )
                        )
                    }

                    if (isDrawingWorkspaceOpen) {
                        DrawingCanvas(
                            initialData = workspaceDrawingData,
                            onSaveDrawing = { savedStr ->
                                workspaceDrawingData = savedStr
                                coroutineScope.launch {
                                    // Save drawing directly back to the active note if currently editing
                                    val currentEditingNote = viewModel.selectedNote.value
                                    if (currentEditingNote != null) {
                                        viewModel.saveDraftNote(
                                            title = currentEditingNote.title,
                                            content = currentEditingNote.content,
                                            category = currentEditingNote.category,
                                            tags = currentEditingNote.tags,
                                            voicePath = currentEditingNote.voicePath,
                                            drawingData = savedStr,
                                            isBookmarked = currentEditingNote.isBookmarked
                                        )
                                    }
                                }
                                isDrawingWorkspaceOpen = false
                            },
                            onBack = { isDrawingWorkspaceOpen = false },
                            viewModel = viewModel
                        )
                    } else {
                        val mainTabs = remember {
                            listOf(
                                Section.Dashboard,
                                Section.Notes,
                                Section.Tasks,
                                Section.Money,
                                Section.Day
                            )
                        }
                        val pagerState = androidx.compose.foundation.pager.rememberPagerState(
                            initialPage = 0,
                            pageCount = { mainTabs.size }
                        )

                        // Keep pager in sync with programmatic state updates
                        val targetPage = mainTabs.indexOf(activeTab)
                        LaunchedEffect(targetPage) {
                            if (targetPage != -1 && pagerState.currentPage != targetPage) {
                                pagerState.animateScrollToPage(targetPage)
                            }
                        }

                        // Sync swiping back to ViewModel state once page settles
                        LaunchedEffect(pagerState.settledPage) {
                            val currentSwipeSection = mainTabs[pagerState.settledPage]
                            if (viewModel.currentSection.value != currentSwipeSection && targetPage != -1) {
                                viewModel.navigateTo(currentSwipeSection)
                            }
                        }

                        // Trigger tactile tick when a page change successfully commits/snaps
                        val view = androidx.compose.ui.platform.LocalView.current
                        LaunchedEffect(pagerState.settledPage) {
                            com.example.ui.AuraHaptics.triggerSubtleTick(view)
                        }

                        if (activeTab !in mainTabs) {
                            when (activeTab) {
                                Section.RichNoteEditor -> {
                                    val selectedNoteVal by viewModel.selectedNote.collectAsState()
                                    NoteEditorScreen(
                                        note = selectedNoteVal,
                                        viewModel = viewModel,
                                        onBack = { viewModel.navigateTo(Section.Notes) },
                                        onOpenDrawingBoard = { data ->
                                            workspaceDrawingData = data
                                            isDrawingWorkspaceOpen = true
                                        }
                                    )
                                }
                                Section.DrawingWorkspace -> {
                                    val selectedTaskVal by viewModel.selectedEditTask.collectAsState()
                                    TaskComposerScreen(
                                        task = selectedTaskVal,
                                        viewModel = viewModel,
                                        onBack = { viewModel.navigateTo(Section.Tasks) }
                                    )
                                }
                                Section.SecuritySettings -> {
                                    AppSecuritySettingsScreen(viewModel = viewModel)
                                }
                                Section.Habits -> {
                                    HabitsTabScreen(viewModel = viewModel)
                                }
                                else -> { /* Safety fallback */ }
                            }
                        } else {
                            androidx.compose.foundation.pager.HorizontalPager(
                                state = pagerState,
                                modifier = Modifier.fillMaxSize(),
                                userScrollEnabled = true
                            ) { pageIndex ->
                                when (mainTabs[pageIndex]) {
                                    Section.Dashboard -> {
                                        DashboardScreen(
                                            viewModel = viewModel,
                                            onOpenNote = { note ->
                                                viewModel.selectNote(note)
                                                viewModel.navigateTo(Section.RichNoteEditor)
                                            },
                                            onOpenTask = { task ->
                                                viewModel.selectEditTask(task)
                                                viewModel.navigateTo(Section.DrawingWorkspace)
                                            }
                                        )
                                    }
                                    Section.Notes -> {
                                        NotesScreen(
                                            viewModel = viewModel,
                                            onOpenNoteEditor = { note ->
                                                viewModel.selectNote(note)
                                                viewModel.navigateTo(Section.RichNoteEditor)
                                            },
                                            onOpenDrawingWorkspace = { data ->
                                                workspaceDrawingData = data
                                                isDrawingWorkspaceOpen = true
                                            }
                                        )
                                    }
                                    Section.Tasks -> {
                                        TasksScreen(
                                            viewModel = viewModel,
                                            onOpenTaskComposer = { task ->
                                                viewModel.selectEditTask(task)
                                                viewModel.navigateTo(Section.DrawingWorkspace)
                                            }
                                        )
                                    }
                                    Section.Money -> {
                                        MoneyTrackerScreen(viewModel = viewModel)
                                    }
                                    Section.Day -> {
                                        MoreHubScreen(viewModel = viewModel)
                                    }
                                    else -> {}
                                }
                            }
                        }
                    }

                    // Global floating voice playback controller player
                    val playState by viewModel.playbackState.collectAsState()
                    val playProgress by viewModel.playbackProgress.collectAsState()
                    val headerTitle by viewModel.playbackHeader.collectAsState()

                    if (playState is PlaybackState.Playing || playState is PlaybackState.Paused) {
                        AnimatedVisibility(
                            visible = true,
                            enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                            exit = slideOutVertically(targetOffsetY = { it }) + fadeOut()
                        ) {
                            GlobalMiniFloatingPlayer(
                                header = headerTitle,
                                isPlaying = playState is PlaybackState.Playing,
                                progress = playProgress,
                                onTogglePlayPause = {
                                    if (playState is PlaybackState.Playing) viewModel.pauseVoiceNote()
                                    else viewModel.resumeVoiceNote()
                                },
                                onSeek = { viewModel.seekVoiceNote(it) },
                                onClose = { viewModel.stopVoiceNote() },
                                modifier = Modifier
                                    .align(Alignment.BottomCenter)
                                    .padding(horizontal = 16.dp, vertical = 8.dp)
                            )
                        }
                    }

                    // Slide up premium Rounded Bottom Sheet for Info Architecture
                    val infoTitle by viewModel.infoSheetTitle.collectAsState()
                    val infoContent by viewModel.infoSheetContent.collectAsState()

                    AnimatedVisibility(
                        visible = infoTitle != null,
                        enter = slideInVertically(
                            initialOffsetY = { it },
                            animationSpec = tween(350, easing = androidx.compose.animation.core.LinearOutSlowInEasing)
                        ) + fadeIn(),
                        exit = slideOutVertically(
                            targetOffsetY = { it },
                            animationSpec = tween(300, easing = androidx.compose.animation.core.FastOutLinearInEasing)
                        ) + fadeOut()
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color.Black.copy(alpha = 0.5f))
                                .clickable { viewModel.dismissInfoSheet() },
                            contentAlignment = Alignment.BottomCenter
                        ) {
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable(enabled = false) {} // Disable click propagation to dismiss
                                    .padding(16.dp)
                                    .shadow(16.dp, RoundedCornerShape(28.dp))
                                    .border(1.dp, AuraTheme.colors.cardBorder, RoundedCornerShape(28.dp)),
                                colors = CardDefaults.cardColors(containerColor = AuraTheme.colors.cardBackground),
                                shape = RoundedCornerShape(28.dp)
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(24.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(16.dp)
                                ) {
                                    // Handle drag indicator bar
                                    Box(
                                        modifier = Modifier
                                            .width(40.dp)
                                            .height(4.dp)
                                            .clip(CircleShape)
                                            .background(AuraTheme.colors.cardBorder)
                                    )

                                    Text(
                                        text = infoTitle ?: "About this Section",
                                        color = AuraTheme.colors.textPrimary,
                                        fontWeight = FontWeight.Black,
                                        fontSize = 18.sp,
                                        letterSpacing = 1.sp,
                                        textAlign = TextAlign.Center
                                    )

                                    Text(
                                        text = infoContent ?: "",
                                        color = AuraTheme.colors.textSecondary,
                                        fontSize = 13.sp,
                                        lineHeight = 20.sp,
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier.padding(horizontal = 8.dp)
                                    )

                                    Spacer(modifier = Modifier.height(4.dp))

                                    Button(
                                        onClick = { viewModel.dismissInfoSheet() },
                                        colors = ButtonDefaults.buttonColors(containerColor = AuraTheme.colors.accentBrand),
                                        shape = RoundedCornerShape(12.dp),
                                        modifier = Modifier.fillMaxWidth().height(44.dp)
                                    ) {
                                        Text(
                                            "UNDERSTOOD",
                                            color = Color.White,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.sp,
                                            letterSpacing = 1.sp
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Interactive Autonomous AI Companion Robot ("Aura Bot")
                    val isRobotCompanionEnabled by viewModel.isRobotCompanionEnabled.collectAsState()
                    AuraRobotCompanion(
                        isVisible = isRobotCompanionEnabled && activeTab != Section.RichNoteEditor && activeTab != Section.DrawingWorkspace && !isDrawingWorkspaceOpen,
                        activeTab = activeTab,
                        onNavigateTab = { section -> viewModel.navigateTo(section) },
                        onQuickNote = {
                            viewModel.selectNote(null)
                            viewModel.navigateTo(Section.RichNoteEditor)
                        },
                        onVoiceMemo = {
                            showAudioRecordDialog = true
                        },
                        onSnapImage = {
                            showImageCaptureDialog = true
                        },
                        onIncome = {
                            showGlobalQuickTransactionType = "RECEIVED"
                        },
                        onExpense = {
                            showGlobalQuickTransactionType = "SENT"
                        },
                        onStyleFab = {
                            showCustomIconDialog = true
                        }
                    )
                }

                // --- AUDIO RECORDING DIALOG BEGIN ---
                if (showAudioRecordDialog) {
                    val recordedDuration by viewModel.audioController.recordedDuration.collectAsState()
                    val isRecordingGlobally by viewModel.audioController.isRecording.collectAsState()

                    // Formatted time: mm:ss
                    val secs = (recordedDuration / 1000) % 60
                    val mins = (recordedDuration / 1000) / 60
                    val timeString = String.format("%02d:%02d", mins, secs)

                    // Wave animations: pulsing neon circles using animatable scale
                    val pulseScale1 = remember { Animatable(1f) }
                    val pulseScale2 = remember { Animatable(1f) }
                    
                    LaunchedEffect(isRecordingGlobally) {
                        if (isRecordingGlobally) {
                            // Animating wave ripples
                            launch {
                                while (true) {
                                    pulseScale1.animateTo(1.5f, animationSpec = infiniteRepeatable(tween(1000, easing = LinearEasing), RepeatMode.Reverse))
                                }
                            }
                            launch {
                                while (true) {
                                    pulseScale2.animateTo(1.8f, animationSpec = infiniteRepeatable(tween(800, easing = LinearEasing), RepeatMode.Reverse))
                                }
                            }
                        }
                    }

                    LaunchedEffect(showAudioRecordDialog) {
                        if (showAudioRecordDialog) {
                            viewModel.startAudioNoteRecording()
                        }
                    }

                    AlertDialog(
                        onDismissRequest = {
                            viewModel.stopAudioNoteRecording()
                            showAudioRecordDialog = false
                        },
                        title = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Mic, contentDescription = null, tint = AuraTheme.colors.negativeRed, modifier = Modifier.size(24.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("AUDIO QUICK CAPTURE", color = AuraTheme.colors.textPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                            }
                        },
                        text = {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 12.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Box(
                                    modifier = Modifier.size(100.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(60.dp)
                                            .scale(pulseScale2.value)
                                            .background(AuraTheme.colors.negativeRed.copy(alpha = 0.15f), CircleShape)
                                    )
                                    Box(
                                        modifier = Modifier
                                            .size(65.dp)
                                            .scale(pulseScale1.value)
                                            .background(AuraTheme.colors.negativeRed.copy(alpha = 0.25f), CircleShape)
                                    )
                                    Box(
                                        modifier = Modifier
                                            .size(50.dp)
                                            .background(AuraTheme.colors.negativeRed, CircleShape),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            Icons.Default.FiberManualRecord,
                                            contentDescription = "Recording mic active",
                                            tint = Color.White,
                                            modifier = Modifier.size(28.dp)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(16.dp))

                                Text(
                                    text = timeString,
                                    fontSize = 28.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = AuraTheme.colors.textPrimary
                                )

                                Spacer(modifier = Modifier.height(6.dp))

                                Text(
                                    "Speak clearly. The audio is automatically saved.",
                                    color = AuraTheme.colors.textSecondary,
                                    fontSize = 11.sp,
                                    textAlign = TextAlign.Center
                                )
                            }
                        },
                        confirmButton = {
                            TextButton(
                                onClick = {
                                    viewModel.stopAudioNoteRecording()
                                    val voicePathForNote = viewModel.getVoiceNoteFile()
                                    
                                    if (voicePathForNote != null) {
                                        viewModel.saveDraftNote(
                                            title = "Voice Capture Note",
                                            content = "Captured via instant quick recorder on " + SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date()),
                                            category = "Voice Log",
                                            tags = "QuickCapture, Voice",
                                            voicePath = voicePathForNote,
                                            drawingData = null,
                                            photoPath = null
                                        )
                                        viewModel.navigateTo(Section.Notes)
                                    }
                                    showAudioRecordDialog = false
                                }
                            ) {
                                Text("SAVE & STOP", color = AuraTheme.colors.accentBrand, fontWeight = FontWeight.Bold)
                            }
                        },
                        dismissButton = {
                            TextButton(
                                onClick = {
                                    viewModel.stopAudioNoteRecording()
                                    showAudioRecordDialog = false
                                }
                            ) {
                                Text("DISCARD & CANCEL", color = AuraTheme.colors.textSecondary)
                            }
                        },
                        containerColor = AuraTheme.colors.cardBackground
                    )
                }

                // --- IMAGE/MEDIA CAPTURE DIALOG BEGIN ---
                if (showImageCaptureDialog) {
                    var noteTitleState by remember { mutableStateOf("Captured Media Note") }
                    var noteContentState by remember { mutableStateOf("") }

                    AlertDialog(
                        onDismissRequest = {
                            showImageCaptureDialog = false
                            selectedMediaUri = null
                        },
                        title = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.PhotoCamera, contentDescription = null, tint = AuraTheme.colors.accentBrand, modifier = Modifier.size(24.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("CAPTURE MEDIA NOTE", color = AuraTheme.colors.textPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                            }
                        },
                        text = {
                            LazyColumn(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                item {
                                    Text("Select how you want to capture or import your image/video:", color = AuraTheme.colors.textSecondary, fontSize = 11.sp)
                                }

                                item {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Button(
                                            onClick = {
                                                try {
                                                    cameraLauncher.launch(null)
                                                } catch (e: Exception) {
                                                    mediaPickerLauncher.launch("image/* video/*")
                                                }
                                            },
                                            colors = ButtonDefaults.buttonColors(containerColor = AuraTheme.colors.accentBrand),
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Icon(Icons.Default.PhotoCamera, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("Shoot Photo", color = Color.White, fontSize = 11.sp)
                                        }

                                        Button(
                                            onClick = {
                                                mediaPickerLauncher.launch("image/* video/*")
                                            },
                                            colors = ButtonDefaults.buttonColors(containerColor = AuraTheme.colors.bottomNavBackground),
                                            border = BorderStroke(1.dp, AuraTheme.colors.cardBorder),
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Icon(Icons.Default.Image, contentDescription = null, tint = AuraTheme.colors.textPrimary, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("Choose Gallery", color = AuraTheme.colors.textPrimary, fontSize = 11.sp)
                                        }
                                    }
                                }

                                if (selectedMediaUri != null) {
                                    item {
                                        Card(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(150.dp)
                                                .clip(RoundedCornerShape(12.dp))
                                                .border(1.dp, AuraTheme.colors.accentBrand, RoundedCornerShape(12.dp)),
                                            colors = CardDefaults.cardColors(containerColor = AuraTheme.colors.cardBackground)
                                        ) {
                                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                                androidx.compose.foundation.Image(
                                                    painter = coil.compose.rememberAsyncImagePainter(
                                                     model = coil.request.ImageRequest.Builder(androidx.compose.ui.platform.LocalContext.current)
                                                         .data(selectedMediaUri)
                                                         .size(coil.size.Size(400, 300))
                                                         .crossfade(true)
                                                         .memoryCacheKey("selected_media_preview_${selectedMediaUri.hashCode()}")
                                                         .build()
                                                 ),
                                                    contentDescription = "Preview of captured image/video",
                                                    modifier = Modifier.fillMaxSize(),
                                                    contentScale = ContentScale.Crop
                                                )
                                                Box(
                                                    modifier = Modifier
                                                        .align(Alignment.BottomEnd)
                                                        .padding(6.dp)
                                                        .background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(4.dp))
                                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                                ) {
                                                    Text("Captured/Chosen", color = AuraTheme.colors.accentBrand, fontSize = 9.sp)
                                                }
                                            }
                                        }
                                    }
                                }

                                item {
                                    OutlinedTextField(
                                        value = noteTitleState,
                                        onValueChange = { noteTitleState = it },
                                        placeholder = { Text("Note Title", color = AuraTheme.colors.textMuted) },
                                        label = { Text("Title", color = AuraTheme.colors.accentBrand) },
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedBorderColor = AuraTheme.colors.accentBrand,
                                            unfocusedBorderColor = AuraTheme.colors.cardBorder,
                                            focusedTextColor = AuraTheme.colors.textPrimary,
                                            unfocusedTextColor = AuraTheme.colors.textPrimary
                                        )
                                    )
                                }

                                item {
                                    OutlinedTextField(
                                        value = noteContentState,
                                        onValueChange = { noteContentState = it },
                                        placeholder = { Text("Add any details or transcription here...", color = AuraTheme.colors.textMuted) },
                                        label = { Text("Memos / Details", color = AuraTheme.colors.accentBrand) },
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedBorderColor = AuraTheme.colors.accentBrand,
                                            unfocusedBorderColor = AuraTheme.colors.cardBorder,
                                            focusedTextColor = AuraTheme.colors.textPrimary,
                                            unfocusedTextColor = AuraTheme.colors.textPrimary
                                        )
                                    )
                                }
                            }
                        },
                        confirmButton = {
                            TextButton(
                                onClick = {
                                    val mediaPath = selectedMediaUri?.toString() ?: ""
                                    viewModel.saveDraftNote(
                                        title = noteTitleState.ifBlank { "Captured Media" },
                                        content = noteContentState,
                                        category = "Photos",
                                        tags = "QuickCapture, Media",
                                        voicePath = null,
                                        drawingData = null,
                                        photoPath = mediaPath.ifBlank { null }
                                    )
                                    selectedMediaUri = null
                                    showImageCaptureDialog = false
                                    viewModel.navigateTo(Section.Notes)
                                },
                                enabled = selectedMediaUri != null
                            ) {
                                Text(
                                    "SAVE NOTE",
                                    color = if (selectedMediaUri != null) AuraTheme.colors.accentBrand else AuraTheme.colors.textMuted,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        },
                        dismissButton = {
                            TextButton(
                                onClick = {
                                    selectedMediaUri = null
                                    showImageCaptureDialog = false
                                }
                            ) {
                                Text("CANCEL", color = AuraTheme.colors.textSecondary)
                            }
                        },
                        containerColor = AuraTheme.colors.cardBackground
                    )
                }

                // --- CUSTOM QUICK CAPTURE ICON DIALOG BEGIN ---
                if (showCustomIconDialog) {
                    val quickCaptureIconUri by viewModel.quickCaptureIconUri.collectAsState()
                    var rawUrlInput by remember { mutableStateOf("") }
                    val presetIcons = listOf(
                        Pair("Neon Star 🌌", "https://i.giphy.com/media/v1.Y2lkPTc5MGI3NjExNG11aDdkczkza3ZodmN2MDY5bTAxMXhhbWVwZ2Z3ZHkxaHA0azBoOSZlcD12MV9pbnRlcm5hbF9naWZfYnlfaWQmY3Q9cw/gT9SOnHj3S6P1810iC/giphy.gif"),
                        Pair("Neon Bolt ⚡", "https://i.giphy.com/media/v1.Y2lkPTc5MGI3NjExM3ZpcTcyN3F3MXZncDV1MGF1eHR4cnE5N2F4cjA1MG0zampreDNzOSZlcD12MV9pbnRlcm5hbF9naWZfYnlfaWQmY3Q9cw/WfNfPwO6LidD86v6x9/giphy.gif"),
                        Pair("Cosmic Orbit 🌀", "https://i.giphy.com/media/v1.Y2lkPTc5MGI3NjExbnZnbWptbjRhOG9obmI5N2d3czgzdHYwMzF6dW4wZXk1cnRocjAycSZlcD12MV9pbnRlcm5hbF9naWZfYnlfaWQmY3Q9cw/Y8S6gBqg3N68N0iH3L/giphy.gif"),
                        Pair("Galaxy Vortex 🌀", "https://i.giphy.com/media/v1.Y2lkPTc5MGI3NjExbDVnbXQ3bTN0Y3kyNWlhNDJ5MTBicm1vMzI2YWZ3dzdtcm91djhpNCZlcD12MV9pbnRlcm5hbF9naWZfYnlfaWQmY3Q9cw/JsiL1g5xO76yKqO41S/giphy.gif"),
                        Pair("Party Cat 🐱", "https://media.giphy.com/media/v1.Y2lkPTc5MGI3NjExM3Zpc3g0N3l2cW9xczh4amx6dGZ0NDF0bnY1MXQyNHp3MG8xeWFwYyZlcD12MV9pbnRlcm5hbF9naWZfYnlfaWQmY3Q9cw/vFKqnCdLPNOKc/giphy.gif"),
                        Pair("Banana Cat 😸", "https://media.giphy.com/media/ND6xk9vIfV69O/giphy.gif"),
                        Pair("Wink Doggo 🐶", "https://media.giphy.com/media/12p3g23Ysrvw9W/giphy.gif")
                    )

                    AlertDialog(
                        onDismissRequest = { showCustomIconDialog = false },
                        title = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.AddPhotoAlternate, contentDescription = null, tint = AuraTheme.colors.accentBrand, modifier = Modifier.size(24.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("CUSTOMIZE CAPTURE ICON", color = AuraTheme.colors.textPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                            }
                        },
                        text = {
                            LazyColumn(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                item {
                                    Text("Transform your quick capture button with funny GIFs of cats, dogs, customized circles, animations or photographs!", color = AuraTheme.colors.textSecondary, fontSize = 11.sp)
                                }

                                item {
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Button(
                                            onClick = {
                                                iconPickerLauncher.launch("image/*")
                                            },
                                            colors = ButtonDefaults.buttonColors(containerColor = AuraTheme.colors.accentBrand),
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Icon(Icons.Default.FolderOpen, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text("Pick from Device", color = Color.White, fontSize = 11.sp)
                                        }
                                    }
                                }

                                item {
                                    HorizontalDivider(color = AuraTheme.colors.cardBorder.copy(alpha = 0.5f))
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text("Amusing Preset GIFs & Circles (tap to apply):", color = AuraTheme.colors.textPrimary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }

                                item {
                                    LazyRow(
                                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                                        modifier = Modifier.fillMaxWidth().height(115.dp)
                                    ) {
                                        items(presetIcons) { preset ->
                                            val isSelected = quickCaptureIconUri == preset.second
                                            Box(
                                                modifier = Modifier
                                                    .width(105.dp)
                                                    .fillMaxHeight()
                                                    .clip(RoundedCornerShape(12.dp))
                                                    .background(AuraTheme.colors.cardBackground)
                                                    .border(2.dp, if (isSelected) AuraTheme.colors.accentBrand else AuraTheme.colors.cardBorder, RoundedCornerShape(12.dp))
                                                    .clickable {
                                                        viewModel.setQuickCaptureIconUri(preset.second)
                                                    }
                                                    .padding(6.dp),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Column(
                                                    horizontalAlignment = Alignment.CenterHorizontally,
                                                    verticalArrangement = Arrangement.Center
                                                ) {
                                                    Box(
                                                        modifier = Modifier
                                                            .size(54.dp)
                                                            .clip(CircleShape)
                                                            .background(AuraTheme.colors.bottomNavBackground),
                                                        contentAlignment = Alignment.Center
                                                    ) {
                                                        androidx.compose.foundation.Image(
                                                            painter = coil.compose.rememberAsyncImagePainter(
                                                             model = coil.request.ImageRequest.Builder(androidx.compose.ui.platform.LocalContext.current)
                                                                 .data(preset.second)
                                                                 .size(coil.size.Size(120, 120))
                                                                 .crossfade(true)
                                                                 .memoryCacheKey("preset_fab_${preset.first}")
                                                                 .build()
                                                         ),
                                                            contentDescription = preset.first,
                                                            modifier = Modifier.fillMaxSize().clip(CircleShape),
                                                            contentScale = ContentScale.Crop
                                                        )
                                                    }
                                                    Spacer(modifier = Modifier.height(6.dp))
                                                    Text(preset.first, color = AuraTheme.colors.textPrimary, fontSize = 9.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, fontWeight = FontWeight.SemiBold)
                                                }
                                            }
                                        }
                                    }
                                }

                                item {
                                    OutlinedTextField(
                                        value = rawUrlInput,
                                        onValueChange = { rawUrlInput = it },
                                        placeholder = { Text("https://example.com/item.gif", color = AuraTheme.colors.textMuted) },
                                        label = { Text("Or paste image URL", color = AuraTheme.colors.accentBrand) },
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedBorderColor = AuraTheme.colors.accentBrand,
                                            unfocusedBorderColor = AuraTheme.colors.cardBorder,
                                            focusedTextColor = AuraTheme.colors.textPrimary,
                                            unfocusedTextColor = AuraTheme.colors.textPrimary
                                        )
                                    )
                                }
                            }
                        },
                        confirmButton = {
                            TextButton(
                                onClick = {
                                    if (rawUrlInput.isNotBlank()) {
                                        viewModel.setQuickCaptureIconUri(rawUrlInput.trim())
                                    }
                                    showCustomIconDialog = false
                                }
                            ) {
                                Text("DONE", color = AuraTheme.colors.accentBrand, fontWeight = FontWeight.Bold)
                            }
                        },
                        dismissButton = {
                            TextButton(
                                onClick = {
                                    viewModel.setQuickCaptureIconUri("")
                                    showCustomIconDialog = false
                                }
                            ) {
                                Text("RESET DEFAULT", color = AuraTheme.colors.textSecondary)
                            }
                        },
                        containerColor = AuraTheme.colors.cardBackground
                    )
                }

                // --- GLOBAL QUICK TRANSACTION DIALOG (ADD & DEDUCT PAYMENT) ---
                if (showGlobalQuickTransactionType != null) {
                    val txType = showGlobalQuickTransactionType!!
                    val sentOptions by viewModel.sentOptions.collectAsState()
                    QuickTransactionBottomSheet(
                        type = txType,
                        accounts = accounts,
                        friends = friends,
                        sentOptions = sentOptions,
                        onDismiss = { showGlobalQuickTransactionType = null },
                        onSubmit = { amount, recipient, category, note, loc, method, acctId, tickedFriends, includeMe ->
                            viewModel.addTransaction(
                                type = txType,
                                amount = amount,
                                recipientOrSender = recipient,
                                category = category,
                                note = note,
                                location = loc,
                                paymentMethod = method,
                                accountId = acctId
                            )
                            if (category == "Friend" && tickedFriends.isNotEmpty()) {
                                val shareCount = tickedFriends.size + (if (includeMe) 1 else 0)
                                val splitShare = amount / shareCount
                                tickedFriends.forEach { fri ->
                                    viewModel.addDebt(
                                        friendId = fri.id,
                                        friendName = fri.name,
                                        title = note.ifBlank { "Shared Quick Expense" },
                                        totalAmount = splitShare,
                                        amount = splitShare,
                                        isYouOwe = (txType == "RECEIVED")
                                    )
                                }
                            }
                            showGlobalQuickTransactionType = null
                        }
                    )
                }
            }
        }
    }
}

fun getNoteCategoryColor(category: String): Color {
    return when (category.trim().lowercase()) {
        "work", "business", "project" -> Color(0xFF38BDF8)
        "ideas", "brainstorm", "creative" -> Color(0xFFA78BFA)
        "personal", "life" -> RadiantOrange
        "voice", "audio", "recording" -> Color(0xFF34D399)
        "finance", "money" -> SemanticGreen
        "study", "education" -> Color(0xFFFBBF24)
        else -> RadiantOrange
    }
}

fun formatNoteRelativeDate(timestamp: Long): String {
    val now = System.currentTimeMillis()
    val diff = now - timestamp
    val seconds = diff / 1000
    val minutes = seconds / 60
    val hours = minutes / 60
    val days = hours / 24

    return when {
        diff < 60_000 -> "Just now"
        minutes < 60 -> "${minutes}m ago"
        hours < 24 -> "${hours}h ago"
        days == 1L -> "Yesterday"
        days < 7 -> "${days}d ago"
        else -> SimpleDateFormat("MMM dd", Locale.getDefault()).format(Date(timestamp))
    }
}

@Composable
fun AuraQuickActionStarterChip(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(AuraTheme.colors.cardBackground)
            .border(1.dp, AuraTheme.colors.cardBorder, RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            color = AuraTheme.colors.textPrimary
        )
    }
}

@Composable
fun AuraHomeScreenNoteCard(
    note: Note,
    isPlayingThis: Boolean,
    onOpen: () -> Unit,
    onToggleBookmark: () -> Unit,
    onTogglePin: () -> Unit,
    onPlayVoice: () -> Unit,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    val shape = RoundedCornerShape(16.dp)
    val catColor = getNoteCategoryColor(note.category)
    val wordCount = remember(note.content) {
        note.content.trim().split(Regex("\\s+")).filter { it.isNotBlank() }.size
    }
    val timeAgo = remember(note.lastModified) {
        formatNoteRelativeDate(note.lastModified)
    }

    Card(
        modifier = modifier
            .auraSpringPress(
                cornerRadius = 16.dp,
                onClick = onOpen,
                onLongClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onTogglePin()
                }
            )
            .border(
                width = if (note.isPinned) 1.5.dp else 1.dp,
                color = if (note.isPinned) AuraTheme.colors.accentBrand.copy(alpha = 0.6f) else AuraTheme.colors.cardBorder,
                shape = shape
            ),
        colors = CardDefaults.cardColors(
            containerColor = if (note.isPinned) AuraTheme.colors.cardBackground.copy(alpha = 0.95f) else AuraTheme.colors.cardBackground
        ),
        shape = shape
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                // Top Row: Category Squircle Badge + Action Badges (Pin, Star)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(catColor.copy(alpha = 0.15f))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = note.category.uppercase(),
                            fontSize = 8.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = catColor,
                            letterSpacing = 0.5.sp
                        )
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        if (note.isPinned) {
                            Icon(
                                Icons.Default.PushPin,
                                contentDescription = "Pinned",
                                tint = AuraTheme.colors.badgeGold,
                                modifier = Modifier.size(12.dp)
                            )
                        }
                        IconButton(
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                onToggleBookmark()
                            },
                            modifier = Modifier.size(20.dp)
                        ) {
                            Icon(
                                imageVector = if (note.isBookmarked || note.isFavorite) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                                contentDescription = "Bookmark",
                                tint = if (note.isBookmarked || note.isFavorite) AuraTheme.colors.gold else AuraTheme.colors.textMuted,
                                modifier = Modifier.size(13.dp)
                            )
                        }
                    }
                }

                // Note Title
                Text(
                    text = note.title.ifBlank { "Untitled Note" },
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = AuraTheme.colors.textPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                // Note Content Excerpt
                Text(
                    text = note.content.ifBlank { "Tap to add rich notes..." },
                    fontSize = 11.sp,
                    color = AuraTheme.colors.textSecondary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    lineHeight = 15.sp
                )

                // Media Attachment Previews if any
                if (!note.voicePath.isNullOrBlank() || !note.drawingData.isNullOrBlank() || !note.photoPath.isNullOrBlank()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (!note.voicePath.isNullOrBlank()) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(AuraTheme.colors.positiveGreen.copy(alpha = 0.14f))
                                    .clickable { onPlayVoice() }
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        imageVector = if (isPlayingThis) Icons.Default.Pause else Icons.Default.PlayArrow,
                                        contentDescription = "Play Voice Note",
                                        tint = AuraTheme.colors.positiveGreen,
                                        modifier = Modifier.size(11.dp)
                                    )
                                    Text(
                                        text = if (isPlayingThis) "PLAYING" else "VOICE",
                                        fontSize = 8.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = AuraTheme.colors.positiveGreen
                                    )
                                }
                            }
                        }

                        if (!note.drawingData.isNullOrBlank()) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(AuraTheme.colors.accentBrand.copy(alpha = 0.12f))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Draw,
                                        contentDescription = "Canvas",
                                        tint = AuraTheme.colors.accentBrand,
                                        modifier = Modifier.size(10.dp)
                                    )
                                    Text(
                                        text = "SKETCH",
                                        fontSize = 8.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = AuraTheme.colors.accentBrand
                                    )
                                }
                            }
                        }

                        if (!note.photoPath.isNullOrBlank()) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color(0xFF38BDF8).copy(alpha = 0.12f))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Image,
                                        contentDescription = "Photo",
                                        tint = Color(0xFF38BDF8),
                                        modifier = Modifier.size(10.dp)
                                    )
                                    Text(
                                        text = "IMAGE",
                                        fontSize = 8.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF38BDF8)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Bottom metadata row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = timeAgo,
                    fontSize = 9.sp,
                    color = AuraTheme.colors.textMuted
                )
                if (wordCount > 0) {
                    Text(
                        text = "$wordCount words",
                        fontSize = 9.sp,
                        color = AuraTheme.colors.textMuted
                    )
                }
            }
        }
    }
}

@Composable
fun AuraHomeScreenNoteRowItem(
    note: Note,
    isPlayingThis: Boolean,
    onOpen: () -> Unit,
    onToggleBookmark: () -> Unit,
    onTogglePin: () -> Unit,
    onPlayVoice: () -> Unit,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    val shape = RoundedCornerShape(14.dp)
    val catColor = getNoteCategoryColor(note.category)
    val timeAgo = remember(note.lastModified) {
        formatNoteRelativeDate(note.lastModified)
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .auraSpringPress(
                cornerRadius = 14.dp,
                onClick = onOpen,
                onLongClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onTogglePin()
                }
            )
            .border(
                width = if (note.isPinned) 1.5.dp else 1.dp,
                color = if (note.isPinned) AuraTheme.colors.accentBrand.copy(alpha = 0.5f) else AuraTheme.colors.cardBorder,
                shape = shape
            ),
        colors = CardDefaults.cardColors(containerColor = AuraTheme.colors.cardBackground),
        shape = shape
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(catColor.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = when {
                        !note.voicePath.isNullOrBlank() -> Icons.Default.Mic
                        !note.drawingData.isNullOrBlank() -> Icons.Default.Draw
                        !note.photoPath.isNullOrBlank() -> Icons.Default.Image
                        else -> Icons.Default.Description
                    },
                    contentDescription = null,
                    tint = catColor,
                    modifier = Modifier.size(18.dp)
                )
            }

            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = note.title.ifBlank { "Untitled Note" },
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = AuraTheme.colors.textPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    if (note.isPinned) {
                        Icon(
                            Icons.Default.PushPin,
                            contentDescription = "Pinned",
                            tint = AuraTheme.colors.badgeGold,
                            modifier = Modifier.size(11.dp)
                        )
                    }
                }

                Text(
                    text = note.content.ifBlank { "No content description." },
                    fontSize = 11.sp,
                    color = AuraTheme.colors.textSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = note.category.uppercase(),
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Bold,
                        color = catColor
                    )
                    Text(
                        text = "•",
                        fontSize = 8.sp,
                        color = AuraTheme.colors.textMuted
                    )
                    Text(
                        text = timeAgo,
                        fontSize = 9.sp,
                        color = AuraTheme.colors.textMuted
                    )
                }
            }

            // Quick Play Voice or Bookmark
            if (!note.voicePath.isNullOrBlank()) {
                IconButton(
                    onClick = onPlayVoice,
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        imageVector = if (isPlayingThis) Icons.Default.PauseCircleFilled else Icons.Default.PlayCircleFilled,
                        contentDescription = "Play Voice Note",
                        tint = AuraTheme.colors.positiveGreen,
                        modifier = Modifier.size(22.dp)
                    )
                }
            } else {
                IconButton(
                    onClick = onToggleBookmark,
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        imageVector = if (note.isBookmarked || note.isFavorite) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                        contentDescription = "Bookmark",
                        tint = if (note.isBookmarked || note.isFavorite) AuraTheme.colors.gold else AuraTheme.colors.textMuted,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun HomeScreenAuraNotesPreview(
    viewModel: AppViewModel,
    notesList: List<Note>,
    onOpenNote: (Note) -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedFilterTag by remember { mutableStateOf("All") }
    var previewLayoutMode by remember { mutableStateOf("CAROUSEL") } // "CAROUSEL", "GRID", "LIST"
    val playbackState by viewModel.playbackState.collectAsState()
    val isPlaying = playbackState is PlaybackState.Playing
    val currentHeader by viewModel.playbackHeader.collectAsState()
    val haptic = LocalHapticFeedback.current

    // Extract dynamic categories and filter options
    val filterOptions = remember(notesList) {
        val baseFilters = mutableListOf("All", "📌 Pinned", "🔖 Starred", "🎙️ Voice", "🎨 Canvas")
        val uniqueCats = notesList.map { it.category.trim() }.filter { it.isNotBlank() && it !in listOf("General", "All") }.distinct()
        baseFilters.addAll(uniqueCats)
        baseFilters
    }

    val filteredNotes = remember(notesList, selectedFilterTag) {
        when (selectedFilterTag) {
            "All" -> notesList
            "📌 Pinned" -> notesList.filter { it.isPinned }
            "🔖 Starred" -> notesList.filter { it.isBookmarked || it.isFavorite }
            "🎙️ Voice" -> notesList.filter { !it.voicePath.isNullOrBlank() }
            "🎨 Canvas" -> notesList.filter { !it.drawingData.isNullOrBlank() }
            else -> notesList.filter { it.category.equals(selectedFilterTag, ignoreCase = true) }
        }
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // 1. SECTION HEADER WITH ACTIONS & LAYOUT TOGGLE
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.clickable { viewModel.navigateTo(Section.Notes) }
            ) {
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(AuraTheme.colors.accentBrand.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Feed,
                        contentDescription = null,
                        tint = AuraTheme.colors.accentBrand,
                        modifier = Modifier.size(14.dp)
                    )
                }

                Text(
                    "AURA NOTES",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = AuraTheme.colors.textPrimary,
                    letterSpacing = 1.2.sp
                )

                if (notesList.isNotEmpty()) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(AuraTheme.colors.accentBrand.copy(alpha = 0.15f))
                            .padding(horizontal = 7.dp, vertical = 2.dp)
                    ) {
                        Text(
                            "${notesList.size}",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = AuraTheme.colors.accentBrand
                        )
                    }
                }
            }

            // Right side: Layout Switcher & Action Buttons
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // Layout Mode Switcher Box
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(14.dp))
                        .background(AuraTheme.colors.cardBackground)
                        .border(1.dp, AuraTheme.colors.cardBorder, RoundedCornerShape(14.dp))
                        .padding(horizontal = 4.dp, vertical = 2.dp)
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = { previewLayoutMode = "CAROUSEL" },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.ViewCarousel,
                                contentDescription = "Carousel View",
                                tint = if (previewLayoutMode == "CAROUSEL") AuraTheme.colors.accentBrand else AuraTheme.colors.textMuted,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                        IconButton(
                            onClick = { previewLayoutMode = "GRID" },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.GridView,
                                contentDescription = "Grid View",
                                tint = if (previewLayoutMode == "GRID") AuraTheme.colors.accentBrand else AuraTheme.colors.textMuted,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                        IconButton(
                            onClick = { previewLayoutMode = "LIST" },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.FormatListBulleted,
                                contentDescription = "List View",
                                tint = if (previewLayoutMode == "LIST") AuraTheme.colors.accentBrand else AuraTheme.colors.textMuted,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                }

                // Quick Add Note Button
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(14.dp))
                        .background(AuraTheme.colors.accentBrand.copy(alpha = 0.14f))
                        .clickable {
                            viewModel.selectNote(null)
                            viewModel.navigateTo(Section.RichNoteEditor)
                        }
                        .padding(horizontal = 8.dp, vertical = 5.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            Icons.Default.Add,
                            contentDescription = "New Note",
                            tint = AuraTheme.colors.accentBrand,
                            modifier = Modifier.size(12.dp)
                        )
                        Text(
                            "NEW",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = AuraTheme.colors.accentBrand,
                            letterSpacing = 0.5.sp
                        )
                    }
                }

                // View All Button
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(2.dp),
                    modifier = Modifier
                        .clip(RoundedCornerShape(14.dp))
                        .clickable { viewModel.navigateTo(Section.Notes) }
                        .padding(horizontal = 6.dp, vertical = 5.dp)
                ) {
                    Text(
                        "ALL",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = AuraTheme.colors.textSecondary
                    )
                    Icon(
                        Icons.Default.ArrowForward,
                        contentDescription = "View all notes",
                        tint = AuraTheme.colors.textSecondary,
                        modifier = Modifier.size(10.dp)
                    )
                }
            }
        }

        // 2. QUICK FILTER CHIPS (Scrollable)
        if (notesList.isNotEmpty()) {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(filterOptions) { filter ->
                    val isSelected = selectedFilterTag == filter
                    val chipCount = when (filter) {
                        "All" -> notesList.size
                        "📌 Pinned" -> notesList.count { it.isPinned }
                        "🔖 Starred" -> notesList.count { it.isBookmarked || it.isFavorite }
                        "🎙️ Voice" -> notesList.count { !it.voicePath.isNullOrBlank() }
                        "🎨 Canvas" -> notesList.count { !it.drawingData.isNullOrBlank() }
                        else -> notesList.count { it.category.equals(filter, ignoreCase = true) }
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(
                                if (isSelected) AuraTheme.colors.accentBrand
                                else AuraTheme.colors.cardBackground
                            )
                            .border(
                                1.dp,
                                if (isSelected) AuraTheme.colors.accentBrand
                                else AuraTheme.colors.cardBorder,
                                RoundedCornerShape(20.dp)
                            )
                            .clickable {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                selectedFilterTag = filter
                            }
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = filter,
                                fontSize = 10.sp,
                                fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Medium,
                                color = if (isSelected) Color.White else AuraTheme.colors.textPrimary
                            )
                            if (chipCount > 0) {
                                Text(
                                    text = "($chipCount)",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSelected) Color.White.copy(alpha = 0.8f) else AuraTheme.colors.textMuted
                                )
                            }
                        }
                    }
                }
            }
        }

        // 3. NOTES PREVIEW CONTENT
        if (notesList.isEmpty() || filteredNotes.isEmpty()) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, AuraTheme.colors.cardBorder, RoundedCornerShape(18.dp)),
                colors = CardDefaults.cardColors(containerColor = AuraTheme.colors.cardBackground),
                shape = RoundedCornerShape(18.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .clip(CircleShape)
                                .background(AuraTheme.colors.accentBrand.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.DriveFileRenameOutline,
                                contentDescription = null,
                                tint = AuraTheme.colors.accentBrand,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (notesList.isEmpty()) "Aura Notebook is Ready" else "No matching notes found",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = AuraTheme.colors.textPrimary
                            )
                            Text(
                                text = if (notesList.isEmpty()) "Capture thoughts, voice recordings, and sketches." else "Try choosing another filter tab above.",
                                fontSize = 11.sp,
                                color = AuraTheme.colors.textSecondary
                            )
                        }
                    }

                    // 3 Quick Action Starter Buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        AuraQuickActionStarterChip(
                            label = "💡 Note",
                            onClick = {
                                viewModel.selectNote(null)
                                viewModel.navigateTo(Section.RichNoteEditor)
                            },
                            modifier = Modifier.weight(1f)
                        )
                        AuraQuickActionStarterChip(
                            label = "🎙️ Voice",
                            onClick = {
                                viewModel.selectNote(null)
                                viewModel.navigateTo(Section.RichNoteEditor)
                            },
                            modifier = Modifier.weight(1f)
                        )
                        AuraQuickActionStarterChip(
                            label = "🎨 Sketch",
                            onClick = {
                                viewModel.navigateTo(Section.DrawingWorkspace)
                            },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        } else {
            when (previewLayoutMode) {
                "CAROUSEL" -> {
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(filteredNotes.take(10), key = { it.id }) { note ->
                            AuraHomeScreenNoteCard(
                                note = note,
                                isPlayingThis = (isPlaying && currentHeader == note.title),
                                onOpen = { onOpenNote(note) },
                                onToggleBookmark = { viewModel.toggleNoteBookmark(note) },
                                onTogglePin = { viewModel.toggleNotePinned(note) },
                                onPlayVoice = {
                                    if (!note.voicePath.isNullOrBlank()) {
                                        if (isPlaying && currentHeader == note.title) {
                                            viewModel.pauseVoiceNote()
                                        } else {
                                            viewModel.startPlayingVoiceNote(note.voicePath, note.title)
                                        }
                                    }
                                },
                                modifier = Modifier.width(200.dp)
                            )
                        }
                    }
                }
                "GRID" -> {
                    // 2-column stacked rows for Home Screen
                    val chunkedNotes = filteredNotes.take(6).chunked(2)
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        chunkedNotes.forEach { pair ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                pair.forEach { note ->
                                    AuraHomeScreenNoteCard(
                                        note = note,
                                        isPlayingThis = (isPlaying && currentHeader == note.title),
                                        onOpen = { onOpenNote(note) },
                                        onToggleBookmark = { viewModel.toggleNoteBookmark(note) },
                                        onTogglePin = { viewModel.toggleNotePinned(note) },
                                        onPlayVoice = {
                                            if (!note.voicePath.isNullOrBlank()) {
                                                if (isPlaying && currentHeader == note.title) {
                                                    viewModel.pauseVoiceNote()
                                                } else {
                                                    viewModel.startPlayingVoiceNote(note.voicePath, note.title)
                                                }
                                            }
                                        },
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                                if (pair.size == 1) {
                                    Spacer(modifier = Modifier.weight(1f))
                                }
                            }
                        }
                    }
                }
                "LIST" -> {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        filteredNotes.take(5).forEach { note ->
                            AuraHomeScreenNoteRowItem(
                                note = note,
                                isPlayingThis = (isPlaying && currentHeader == note.title),
                                onOpen = { onOpenNote(note) },
                                onToggleBookmark = { viewModel.toggleNoteBookmark(note) },
                                onTogglePin = { viewModel.toggleNotePinned(note) },
                                onPlayVoice = {
                                    if (!note.voicePath.isNullOrBlank()) {
                                        if (isPlaying && currentHeader == note.title) {
                                            viewModel.pauseVoiceNote()
                                        } else {
                                            viewModel.startPlayingVoiceNote(note.voicePath, note.title)
                                        }
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

// ==========================================
// CENTRAL IMMERSIVE DASHBOARD SCREEN
// ==========================================
@Composable
fun DashboardScreen(
    viewModel: AppViewModel,
    onOpenNote: (Note) -> Unit,
    onOpenTask: (Task) -> Unit
) {
    val stats by viewModel.dashboardStats.collectAsState()
    val notesList by viewModel.activeNotes.collectAsState()
    val tasksList by viewModel.allTasks.collectAsState()
    val accounts by viewModel.allAccounts.collectAsState()
    val investments by viewModel.allInvestments.collectAsState()
    val debts by viewModel.allDebts.collectAsState()
    val journals by viewModel.journalEntries.collectAsState()
    val isDashboardLoading by viewModel.isDashboardLoading.collectAsState()

    val todayDate = SimpleDateFormat("EEEE, dd MMM yyyy", Locale.US).format(Date())
    val todayString = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())

    val totalAvailableBalance = accounts.sumOf { it.balance }
    val totalInvested = investments.sumOf { it.amount }
    val totalToReceive = debts.filter { !it.isYouOwe && it.status == "PENDING" }.sumOf { it.remainingAmount }
    val totalYouOwe = debts.filter { it.isYouOwe && it.status == "PENDING" }.sumOf { it.remainingAmount }
    val netWorth = totalAvailableBalance + totalInvested + totalToReceive - totalYouOwe

    val todayJournal = journals.find { it.date == todayString }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(AuraTheme.colors.screenBackground)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Welcome Header info matching reference layout
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(top = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                val greeting = remember {
                    val hour = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)
                    when {
                        hour in 5..11 -> "Good Morning"
                        hour in 12..16 -> "Good Afternoon"
                        hour in 17..21 -> "Good Evening"
                        else -> "Good Night"
                    }
                }
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = greeting,
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.ExtraBold,
                        color = AuraTheme.colors.textPrimary
                    )
                    Text(
                        text = "Manage budgets, notes, objectives and focus",
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

        if (isDashboardLoading) {
            item {
                AuraLoadingState.HubGrid()
            }
        } else {
            // COGNITIVE STATUS PROGRESS HERO CARD
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .auraSpringPress(
                            cornerRadius = 24.dp,
                            onClick = { viewModel.navigateTo(Section.Tasks) }
                        )
                        .border(
                            1.dp,
                            AuraTheme.colors.cardBorder,
                            RoundedCornerShape(24.dp)
                        ),
                    colors = CardDefaults.cardColors(containerColor = AuraTheme.colors.cardBackground),
                    shape = RoundedCornerShape(24.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                "DAILY OBJECTIVES PROGRESS",
                                style = MaterialTheme.typography.labelSmall,
                                color = AuraTheme.colors.textMuted,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "${stats.productivityPercentage}% ACHIEVED",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Black,
                                color = AuraTheme.colors.accentBrand
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "${stats.todayCompletedTasksCount} objectives finished of ${stats.todayTasksCount} scheduled today",
                                style = MaterialTheme.typography.bodySmall,
                                color = AuraTheme.colors.textSecondary
                            )
                        }

                        // Progress Ring
                        Spacer(modifier = Modifier.width(16.dp))
                        AuraProgressRing(
                            progress = stats.productivityPercentage.toFloat() / 100f,
                            mainText = "${stats.productivityPercentage}%",
                            size = 80.dp,
                            strokeWidth = 6.dp,
                            progressColor = AuraTheme.colors.accentBrand
                        )
                    }
                }
            }

            // TOOLBOX 2-COLUMN HUB GRID
            item {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        AuraHubCard(
                            title = "Notes",
                            statText = "${stats.totalNotesCount} notes logged",
                            icon = Icons.Default.Feed,
                            accentColor = AuraTheme.colors.accentBrand,
                            onClick = { viewModel.navigateTo(Section.Notes) },
                            modifier = Modifier.weight(1f)
                        )
                        AuraHubCard(
                            title = "Tasks",
                            statText = "${stats.todayCompletedTasksCount}/${stats.todayTasksCount} completed",
                            icon = Icons.Default.FormatListBulleted,
                            accentColor = AuraTheme.colors.accentBrand,
                            onClick = { viewModel.navigateTo(Section.Tasks) },
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        AuraHubCard(
                            title = "Habits",
                            statText = "${stats.allTimeStreakValue} 🔥 day streak",
                            icon = Icons.Default.LocalFireDepartment,
                            accentColor = AuraTheme.colors.gold,
                            onClick = { viewModel.navigateTo(Section.Habits) },
                            modifier = Modifier.weight(1f)
                        )
                        AuraHubCard(
                            title = "Money",
                            statText = "₹${"%,.0f".format(netWorth)} net worth",
                            icon = Icons.Default.Payments,
                            accentColor = AuraTheme.colors.positiveGreen,
                            onClick = { viewModel.navigateTo(Section.Money) },
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        AuraHubCard(
                            title = "Journal",
                            statText = if (todayJournal != null) "Mood: ${todayJournal.mood}" else "Not logged today",
                            icon = Icons.Default.CalendarToday,
                            accentColor = AuraTheme.colors.accentBrand,
                            onClick = { viewModel.navigateTo(Section.Day) },
                            modifier = Modifier.weight(1f)
                        )
                        AuraHubCard(
                            title = "Canvas",
                            statText = "Vector sketching",
                            icon = Icons.Default.Draw,
                            accentColor = AuraTheme.colors.accentBrand,
                            onClick = {
                                viewModel.navigateTo(Section.DrawingWorkspace)
                            },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }

        // ======================================================================
        // AURA NOTES PREVIEW SECTION (INTERACTIVE & MULTI-VIEW HOME COMPONENT)
        // ======================================================================
        item {
            HomeScreenAuraNotesPreview(
                viewModel = viewModel,
                notesList = notesList,
                onOpenNote = onOpenNote
            )
        }

        // TODAY'S TASKS LIST QUICK PREVIEW (Header clickable to navigate)
        item {
            Row(
                modifier = Modifier.fillMaxWidth().clickable { viewModel.navigateTo(Section.Tasks) },
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "TODAY OBJECTIVES PREVIEW",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = AuraTheme.colors.textMuted,
                    letterSpacing = 1.sp
                )
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("MANAGE", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = AuraTheme.colors.accentBrand)
                    Icon(Icons.Default.ArrowForward, contentDescription = "Tasks", tint = AuraTheme.colors.accentBrand, modifier = Modifier.size(10.dp))
                }
            }
        }

        val todayString = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
        val todayTasks = tasksList.filter { it.date == todayString }

        if (todayTasks.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth().border(1.dp, AuraTheme.colors.cardBorder, RoundedCornerShape(14.dp)),
                    colors = CardDefaults.cardColors(containerColor = AuraTheme.colors.cardBackground),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.DoneAll, contentDescription = "All done", tint = AuraTheme.colors.textMuted)
                        Spacer(modifier = Modifier.height(6.dp))
                        Text("No objectives planned for today", fontSize = 12.sp, color = AuraTheme.colors.textMuted)
                    }
                }
            }
        } else {
            items(todayTasks, key = { it.id }) { task ->
                TaskRowItem(
                    task = task,
                    onClicked = { onOpenTask(task) },
                    onToggleCompleted = { viewModel.toggleTaskCompleted(task) },
                    onStartTimer = { viewModel.startTaskTimer(task.id, 25) }
                )
            }
        }
    }
}

// ==========================================
// MORE SCREEN (ECOSYSTEM TOOLBOX + GROUPED SETTINGS)
// Directly matching Reference Screenshots 2 & 3
// ==========================================
@Composable
fun MoreHubScreen(
    viewModel: AppViewModel
) {
    val debts by viewModel.allDebts.collectAsState()
    val savingsGoals by viewModel.allSavingsGoals.collectAsState()

    var isGridView by remember { mutableStateOf(true) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(AuraTheme.colors.screenBackground)
            .statusBarsPadding()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. Header: "More" + PRO + Profile
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "More",
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.ExtraBold,
                    color = AuraTheme.colors.textPrimary
                )
                AuraHeaderActions(
                    onProClick = { viewModel.navigateTo(Section.SecuritySettings) },
                    onProfileClick = { viewModel.navigateTo(Section.SecuritySettings) }
                )
            }
        }

        // 2. ECOSYSTEM TOOLBOX Eyebrow + View Switcher + Swap
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "ECOSYSTEM TOOLBOX",
                    style = MaterialTheme.typography.labelSmall,
                    color = AuraTheme.colors.textMuted,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.2.sp
                )

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // View Toggle (List vs Grid)
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(AuraTheme.colors.cardBackground)
                            .border(1.dp, AuraTheme.colors.cardBorder, RoundedCornerShape(20.dp))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.FormatListBulleted,
                                contentDescription = "List",
                                tint = if (!isGridView) AuraTheme.colors.accentBrand else AuraTheme.colors.textMuted,
                                modifier = Modifier
                                    .size(15.dp)
                                    .clickable { isGridView = false }
                            )
                            Icon(
                                imageVector = Icons.Default.GridView,
                                contentDescription = "Grid",
                                tint = if (isGridView) AuraTheme.colors.accentBrand else AuraTheme.colors.textMuted,
                                modifier = Modifier
                                    .size(15.dp)
                                    .clickable { isGridView = true }
                            )
                        }
                    }

                    // SWAP Button
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(AuraTheme.colors.cardBackground)
                            .border(1.dp, AuraTheme.colors.cardBorder, RoundedCornerShape(20.dp))
                            .clickable { /* Swap view */ }
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.SwapHoriz,
                                contentDescription = "Swap",
                                tint = AuraTheme.colors.textSecondary,
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = "SWAP",
                                style = MaterialTheme.typography.labelSmall,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = AuraTheme.colors.textSecondary,
                                letterSpacing = 0.8.sp
                            )
                        }
                    }
                }
            }
        }

        // 3. 2-COLUMN GRID (Matching Reference Screenshot 2)
        item {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                // Row 1: Split & Links
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    AuraHubCard(
                        title = "Split",
                        statText = "${debts.size} TRANSACTIONS",
                        icon = Icons.Default.PeopleOutline,
                        accentColor = AuraTheme.colors.accentBrand,
                        onClick = { viewModel.navigateTo(Section.Money) },
                        modifier = Modifier.weight(1f)
                    )
                    AuraHubCard(
                        title = "Links",
                        statText = "0 SAVED",
                        icon = Icons.Default.OpenInNew,
                        accentColor = AuraTheme.colors.accentBrand,
                        onClick = { viewModel.navigateTo(Section.Notes) },
                        modifier = Modifier.weight(1f)
                    )
                }

                // Row 2: Categories & Budgets
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    AuraHubCard(
                        title = "Categories",
                        statText = "1 TRACKED",
                        icon = Icons.Default.PieChart,
                        accentColor = AuraTheme.colors.accentBrand,
                        onClick = { viewModel.navigateTo(Section.Money) },
                        modifier = Modifier.weight(1f)
                    )
                    AuraHubCard(
                        title = "Budgets",
                        statText = "SET PLAN",
                        icon = Icons.Default.AccountBalanceWallet,
                        accentColor = AuraTheme.colors.accentBrand,
                        onClick = { viewModel.navigateTo(Section.Money) },
                        modifier = Modifier.weight(1f)
                    )
                }

                // Row 3: Loans & Savings
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    AuraHubCard(
                        title = "Loans",
                        statText = "${debts.filter { it.isYouOwe }.size} LOANS",
                        icon = Icons.Default.AccountBalance,
                        accentColor = AuraTheme.colors.accentBrand,
                        onClick = { viewModel.navigateTo(Section.Money) },
                        modifier = Modifier.weight(1f)
                    )
                    AuraHubCard(
                        title = "Savings",
                        statText = "${savingsGoals.size} SAVINGS TARGETS",
                        icon = Icons.Default.TrackChanges,
                        accentColor = AuraTheme.colors.accentBrand,
                        onClick = { viewModel.navigateTo(Section.Money) },
                        modifier = Modifier.weight(1f)
                    )
                }

                // Row 4: Habits & Daily Reflections
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    AuraHubCard(
                        title = "Habits",
                        statText = "DAILY STREAKS",
                        icon = Icons.Default.LocalFireDepartment,
                        accentColor = AuraTheme.colors.accentBrand,
                        onClick = { viewModel.navigateTo(Section.Habits) },
                        modifier = Modifier.weight(1f)
                    )
                    AuraHubCard(
                        title = "Journal",
                        statText = "DAILY LOG",
                        icon = Icons.Default.CalendarToday,
                        accentColor = AuraTheme.colors.accentBrand,
                        onClick = { viewModel.navigateTo(Section.Habits) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        // 4. GROUPED SECTIONS (Matching Reference Screenshot 3)
        item {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                // Top Highlight Card: Ad-free Experience & Financial Freedom
                val topShape = RoundedCornerShape(20.dp)
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(topShape)
                        .background(AuraTheme.colors.cardBackground)
                        .border(1.dp, AuraTheme.colors.cardBorder, topShape)
                        .padding(vertical = 4.dp)
                ) {
                    AuraSectionRow(
                        title = "Ad-free Experience",
                        description = "No more interruptions while managing your money.",
                        icon = Icons.Default.StarOutline,
                        iconTint = AuraTheme.colors.accentBrand,
                        onClick = {}
                    )
                    HorizontalDivider(
                        modifier = Modifier.padding(start = 56.dp, end = 16.dp),
                        color = AuraTheme.colors.cardBorder.copy(alpha = 0.5f),
                        thickness = 0.8.dp
                    )
                    AuraSectionRow(
                        title = "Financial Freedom",
                        description = "Manage more than 1 personal goal and loan.",
                        icon = Icons.Default.TrackChanges,
                        iconTint = AuraTheme.colors.accentBrand,
                        onClick = {}
                    )
                }

                // Group 1: SHARING & COLLABORATION
                AuraSectionGroup(
                    eyebrow = "SHARING & COLLABORATION",
                    items = listOf(
                        SectionRowItem(
                            title = "Unlimited Shared Members",
                            description = "Add more than 3 persons in individual shared history.",
                            icon = Icons.Default.PeopleOutline,
                            iconTint = AuraTheme.colors.accentBrand
                        ),
                        SectionRowItem(
                            title = "Multiple Groups",
                            description = "Create more than 1 sharing group in the Shared tab.",
                            icon = Icons.Default.GridView,
                            iconTint = AuraTheme.colors.accentBrand
                        ),
                        SectionRowItem(
                            title = "Friend Collaboration",
                            description = "Real-time cloud sync with friends and group members.",
                            icon = Icons.Default.Sync,
                            iconTint = AuraTheme.colors.accentBrand
                        )
                    )
                )

                // Group 2: ORGANIZATION & TOOLS
                AuraSectionGroup(
                    eyebrow = "ORGANIZATION & TOOLS",
                    items = listOf(
                        SectionRowItem(
                            title = "Unlimited Links",
                            description = "Save and organize more than 4 useful links.",
                            icon = Icons.Default.Link,
                            iconTint = AuraTheme.colors.accentBrand
                        ),
                        SectionRowItem(
                            title = "Grouped Links",
                            description = "Organize your links into multiple categories.",
                            icon = Icons.Default.GridView,
                            iconTint = AuraTheme.colors.accentBrand
                        ),
                        SectionRowItem(
                            title = "Subscription Mastery",
                            description = "Track more than 2 recurring subscriptions.",
                            icon = Icons.Default.CreditCard,
                            iconTint = AuraTheme.colors.accentBrand
                        )
                    )
                )

                // Group 3: SECURITY & SUPPORT
                AuraSectionGroup(
                    eyebrow = "SECURITY & SUPPORT",
                    items = listOf(
                        SectionRowItem(
                            title = "Advanced Data Security",
                            description = "Unlimited daily backups and automated cloud sync.",
                            icon = Icons.Default.Shield,
                            iconTint = AuraTheme.colors.accentBrand,
                            onClick = { viewModel.navigateTo(Section.SecuritySettings) }
                        ),
                        SectionRowItem(
                            title = "Cloud Backup & Restore",
                            description = "Securely backup and load your data across devices.",
                            icon = Icons.Default.CloudUpload,
                            iconTint = AuraTheme.colors.accentBrand,
                            onClick = { viewModel.navigateTo(Section.SecuritySettings) }
                        )
                    )
                )
            }
        }

        // Safety Bottom Space
        item {
            Spacer(modifier = Modifier.height(100.dp))
        }
    }
}

// ==========================================
// FLOATING PILL NAVIGATION DOCK (5 TABS)
// Directly matching all Reference Screenshots
// ==========================================
private data class NavigationTabItem(
    val section: Section,
    val label: String,
    val activeIcon: androidx.compose.ui.graphics.vector.ImageVector,
    val inactiveIcon: androidx.compose.ui.graphics.vector.ImageVector
)

@Composable
fun AuraBottomNavRow(
    viewModel: AppViewModel,
    active: Section
) {
    val navItems = listOf(
        NavigationTabItem(Section.Dashboard, "Home", Icons.Filled.Home, Icons.Outlined.Home),
        NavigationTabItem(Section.Notes, "Notes", Icons.Filled.StickyNote2, Icons.Outlined.StickyNote2),
        NavigationTabItem(Section.Tasks, "Tasks", Icons.Filled.CheckCircle, Icons.Outlined.CheckCircleOutline),
        NavigationTabItem(Section.Money, "Money", Icons.Filled.AccountBalanceWallet, Icons.Outlined.AccountBalanceWallet),
        NavigationTabItem(Section.Day, "More", Icons.Filled.MoreHoriz, Icons.Outlined.MoreHoriz)
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            color = AuraTheme.colors.bottomNavBackground,
            shape = RoundedCornerShape(32.dp),
            border = BorderStroke(1.dp, AuraTheme.colors.cardBorder),
            shadowElevation = 12.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 6.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceAround,
                verticalAlignment = Alignment.CenterVertically
            ) {
                navItems.forEach { item ->
                    val isSelected = active == item.section
                    val shape = RoundedCornerShape(22.dp)

                    Box(
                        modifier = Modifier
                            .clip(shape)
                            .auraSpringPress(
                                cornerRadius = 22.dp,
                                onClick = { viewModel.navigateTo(item.section) }
                            )
                            .background(if (isSelected) AuraTheme.colors.bottomNavActivePill else Color.Transparent)
                            .padding(horizontal = if (isSelected) 16.dp else 12.dp, vertical = 7.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            Icon(
                                imageVector = if (isSelected) item.activeIcon else item.inactiveIcon,
                                contentDescription = item.label,
                                tint = if (isSelected) AuraTheme.colors.accentBrand else AuraTheme.colors.textMuted,
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = item.label,
                                style = MaterialTheme.typography.labelSmall,
                                color = if (isSelected) AuraTheme.colors.accentBrand else AuraTheme.colors.textMuted,
                                fontSize = 10.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                            )
                        }
                    }
                }
            }
        }
    }
}

// ==========================================
// SECURITY FROSTED PASS KEYPAD LOCK SCREEN
// ==========================================
@Composable
fun SecurityPinKeypadGate(
    viewModel: AppViewModel
) {
    var pinText by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AuraTheme.colors.screenBackground)
            .statusBarsPadding()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(68.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(AuraTheme.colors.accentBrand.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Lock,
                contentDescription = "Security Required",
                tint = AuraTheme.colors.accentBrand,
                modifier = Modifier.size(36.dp)
            )
        }
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "AURA SAFEGUARD LOCK",
            style = MaterialTheme.typography.titleMedium,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = AuraTheme.colors.textPrimary,
            letterSpacing = 2.sp
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "Enter your 4-digit PIN passcode",
            style = MaterialTheme.typography.bodySmall,
            fontSize = 12.sp,
            color = AuraTheme.colors.textSecondary
        )

        Spacer(modifier = Modifier.height(28.dp))

        // Circular dot placeholders
        Row(horizontalArrangement = Arrangement.spacedBy(18.dp)) {
            for (i in 1..4) {
                val isActive = pinText.length >= i
                Box(
                    modifier = Modifier
                        .size(16.dp)
                        .background(
                            color = if (isActive) AuraTheme.colors.accentBrand else AuraTheme.colors.bottomNavBackground,
                            shape = CircleShape
                        )
                        .border(
                            width = 1.dp,
                            color = if (isActive) AuraTheme.colors.accentBrand else AuraTheme.colors.cardBorder,
                            shape = CircleShape
                        )
                )
            }
        }

        if (errorMessage.isNotEmpty()) {
            Spacer(modifier = Modifier.height(14.dp))
            Text(
                text = errorMessage,
                style = MaterialTheme.typography.labelSmall,
                fontSize = 12.sp,
                color = AuraTheme.colors.negativeRed,
                fontWeight = FontWeight.SemiBold
            )
        }

        Spacer(modifier = Modifier.height(36.dp))

        // Matrix keypad layout
        val buttonsList = listOf("1", "2", "3", "4", "5", "6", "7", "8", "9", "C", "0", "OK")
        LazyVerticalGrid(
            columns = GridCells.Fixed(3),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.width(280.dp)
        ) {
            items(buttonsList) { digit ->
                val isAction = digit == "C" || digit == "OK"
                val shape = CircleShape
                Box(
                    modifier = Modifier
                        .aspectRatio(1f)
                        .clip(shape)
                        .background(
                            if (digit == "OK") AuraTheme.colors.accentBrand.copy(alpha = 0.2f)
                            else AuraTheme.colors.cardBackground
                        )
                        .border(
                            1.dp,
                            if (digit == "OK") AuraTheme.colors.accentBrand
                            else AuraTheme.colors.cardBorder,
                            shape
                        )
                        .auraSpringPress(
                            cornerRadius = 35.dp,
                            onClick = {
                                if (digit == "C") {
                                    pinText = ""
                                    errorMessage = ""
                                } else if (digit == "OK") {
                                    if (pinText.length == 4) {
                                        val success = viewModel.verifyPin(pinText)
                                        if (!success) {
                                            errorMessage = "Invalid security PIN passcode"
                                            pinText = ""
                                        }
                                    }
                                } else {
                                    if (pinText.length < 4) {
                                        pinText += digit
                                    }
                                }
                            }
                        )
                        .testTag("pin_btn_$digit"),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = digit,
                        style = MaterialTheme.typography.titleMedium,
                        fontSize = if (isAction) 14.sp else 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (digit == "OK") AuraTheme.colors.accentBrand
                                else if (digit == "C") AuraTheme.colors.negativeRed
                                else AuraTheme.colors.textPrimary
                    )
                }
            }
        }
    }
}

// ==========================================
// PREFERENCES / PIN CONFIGURATIONS
// ==========================================
@Composable
fun AppSecuritySettingsScreen(
    viewModel: AppViewModel
) {
    val securitySec by viewModel.securitySettings.collectAsState()
    val friends by viewModel.allFriends.collectAsState()
    val sentOptions by viewModel.sentOptions.collectAsState()

    // Collect Google Cloud Sync states
    val isSyncEnabled by viewModel.isCloudSyncEnabled.collectAsState()
    val userEmail by viewModel.cloudUserEmail.collectAsState()
    val isSyncing by viewModel.isCurrentlySyncing.collectAsState()
    val lastSync by viewModel.lastSyncedTime.collectAsState()
    val backups by viewModel.mockCloudBackups.collectAsState()
    val profileName by viewModel.profileDisplayName.collectAsState()
    val connectedDevices by viewModel.connectedDevices.collectAsState()

    // Collect DB sizes for the data summary
    val notesCount = viewModel.activeNotes.collectAsState().value.size
    val tasksCount = viewModel.allTasks.collectAsState().value.size
    val habitsCount = viewModel.habits.collectAsState().value.size
    val transactionCount = viewModel.allTransactions.collectAsState().value.size

    var isEnabled by remember(securitySec) { mutableStateOf(securitySec?.isLockEnabled ?: false) }
    var pinValue by remember(securitySec) { mutableStateOf(securitySec?.pinCode ?: "") }

    var newOptionText by remember { mutableStateOf("") }
    var newFriendName by remember { mutableStateOf("") }
    var newFriendPhone by remember { mutableStateOf("") }
    var newFriendNotes by remember { mutableStateOf("") }

    // Settings Profile Sync states
    var showGoogleSignDialogInSettings by remember { mutableStateOf(false) }
    var tempProfileNameInput by remember(profileName) { mutableStateOf(profileName) }
    var isEditingProfile by remember { mutableStateOf(false) }
    val context = LocalContext.current

    val googleSignInLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult(),
        onResult = { result ->
            val intent = result.data
            if (intent != null) {
                try {
                    val task = com.google.android.gms.auth.api.signin.GoogleSignIn.getSignedInAccountFromIntent(intent)
                    val account = task.getResult(com.google.android.gms.common.api.ApiException::class.java)
                    val idToken = account?.idToken
                    if (idToken != null) {
                        viewModel.signInWithGoogleReal(idToken) { success ->
                            // real Google Sign-In succeeded
                        }
                    } else {
                        account?.email?.let { email ->
                            viewModel.signInWithGoogle(email)
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    )

    if (showGoogleSignDialogInSettings) {
        var tempEmailInput by remember { mutableStateOf(userEmail ?: "moreaboutastram@gmail.com") }
        AlertDialog(
            onDismissRequest = { showGoogleSignDialogInSettings = false },
            title = { Text("CONNECT GOOGLE WORKSPACE ACCOUNT", color = AuraTheme.colors.textPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Select your authorization pathway for secure Cloud database synchronization & multi-device backup indexing:", color = AuraTheme.colors.textSecondary, fontSize = 11.sp, lineHeight = 15.sp)
                    
                    // Real Connection Button
                    Button(
                        onClick = {
                            try {
                                val intent = viewModel.authManager.getSignInIntent()
                                googleSignInLauncher.launch(intent)
                                showGoogleSignDialogInSettings = false
                            } catch (e: Exception) {
                                // fall back to standard text field input if services not loaded
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = AuraTheme.colors.accentBrand),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.CloudSync, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("SIGN IN WITH GOOGLE", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }

                    Text("Or configure simulated email identification below:", color = AuraTheme.colors.textSecondary, fontSize = 10.sp)

                    OutlinedTextField(
                        value = tempEmailInput,
                        onValueChange = { tempEmailInput = it },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = AuraTheme.colors.accentBrand,
                            unfocusedBorderColor = AuraTheme.colors.cardBorder,
                            focusedTextColor = AuraTheme.colors.textPrimary,
                            unfocusedTextColor = AuraTheme.colors.textPrimary
                        ),
                        placeholder = { Text("example@gmail.com", color = AuraTheme.colors.textMuted) }
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.signInWithGoogle(tempEmailInput)
                        showGoogleSignDialogInSettings = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AuraTheme.colors.accentBrand)
                ) {
                    Text("IDENTIFY & CONNECT", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                Button(
                    onClick = { showGoogleSignDialogInSettings = false },
                    colors = ButtonDefaults.buttonColors(containerColor = AuraTheme.colors.bottomNavBackground),
                    border = BorderStroke(1.dp, AuraTheme.colors.cardBorder)
                ) {
                    Text("CANCEL", color = AuraTheme.colors.textSecondary, fontSize = 11.sp)
                }
            },
            containerColor = AuraTheme.colors.cardBackground
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AuraTheme.colors.screenBackground)
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = { viewModel.navigateTo(Section.Dashboard) }) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Dashboard", tint = AuraTheme.colors.textPrimary)
            }
            Spacer(modifier = Modifier.width(6.dp))
            Text("SETTINGS & SYSTEM CONFIG", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = AuraTheme.colors.textPrimary)
        }

        // ======================================================================
        // NEW CARD: GOOGLE CLOUD SYNC & PROFILE MANAGEMENT (USER REQUEST)
        // ======================================================================
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, AuraTheme.colors.cardBorder, RoundedCornerShape(20.dp)),
            colors = CardDefaults.cardColors(containerColor = AuraTheme.colors.cardBackground),
            shape = RoundedCornerShape(20.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                // Header row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(
                            imageVector = Icons.Default.CloudQueue,
                            contentDescription = "Cloud logo",
                            tint = AuraTheme.colors.accentBrand,
                            modifier = Modifier.size(22.dp)
                        )
                        Text(
                            "AURA PROFILE & GOOGLE DRIVE CLOUD SYNC",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Black,
                            color = AuraTheme.colors.textPrimary,
                            letterSpacing = 0.5.sp
                        )
                    }

                    if (isSyncing) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), color = AuraTheme.colors.accentBrand, strokeWidth = 2.dp)
                    } else {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .background(if (isSyncEnabled) AuraTheme.colors.positiveGreen else AuraTheme.colors.gold, CircleShape)
                        )
                    }
                }

                HorizontalDivider(color = AuraTheme.colors.cardBorder.copy(alpha = 0.5f))

                // Profile Display Name Config
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Active Sync Profile Label", fontSize = 9.sp, color = AuraTheme.colors.textMuted, fontWeight = FontWeight.Bold)
                        if (isEditingProfile) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                OutlinedTextField(
                                    value = tempProfileNameInput,
                                    onValueChange = { tempProfileNameInput = it },
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = AuraTheme.colors.accentBrand,
                                        unfocusedBorderColor = AuraTheme.colors.cardBorder,
                                        focusedTextColor = AuraTheme.colors.textPrimary,
                                        unfocusedTextColor = AuraTheme.colors.textPrimary
                                    ),
                                    textStyle = androidx.compose.ui.text.TextStyle(fontSize = 12.sp),
                                    modifier = Modifier.weight(1f).height(46.dp),
                                    shape = RoundedCornerShape(8.dp),
                                    singleLine = true
                                )
                                Button(
                                    onClick = {
                                        if (tempProfileNameInput.isNotBlank()) {
                                            viewModel.updateProfileName(tempProfileNameInput)
                                        }
                                        isEditingProfile = false
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = AuraTheme.colors.accentBrand),
                                    shape = RoundedCornerShape(8.dp),
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp)
                                ) {
                                    Text("SAVE", color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Black)
                                }
                            }
                        } else {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text(
                                    text = profileName,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = AuraTheme.colors.textPrimary
                                )
                                Icon(
                                    imageVector = Icons.Default.Edit,
                                    contentDescription = "Edit Profile Name",
                                    tint = AuraTheme.colors.accentBrand,
                                    modifier = Modifier
                                        .size(14.dp)
                                        .clickable { isEditingProfile = true }
                                )
                            }
                        }
                    }
                }

                // Cloud Status
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(AuraTheme.colors.bottomNavBackground, RoundedCornerShape(12.dp))
                        .border(1.dp, AuraTheme.colors.cardBorder.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Google Cloud Connection Status", fontSize = 9.sp, color = AuraTheme.colors.textMuted, fontWeight = FontWeight.Bold)
                        Text(
                            text = if (userEmail != null) "Connected: $userEmail" else "Offline Local Only (Unlinked)",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (userEmail != null) AuraTheme.colors.accentBrand else AuraTheme.colors.textSecondary
                        )
                        Text(
                            text = "Mirror Latency Status: $lastSync",
                            fontSize = 8.sp,
                            color = AuraTheme.colors.textMuted
                        )
                    }

                    if (userEmail == null) {
                        Button(
                            onClick = { showGoogleSignDialogInSettings = true },
                            colors = ButtonDefaults.buttonColors(containerColor = AuraTheme.colors.bottomNavBackground),
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(1.dp, AuraTheme.colors.accentBrand.copy(alpha = 0.5f)),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp)
                        ) {
                            Text("LINK DRIVE", color = AuraTheme.colors.accentBrand, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                        }
                    } else {
                        Text(
                            text = "Sign Out",
                            color = AuraTheme.colors.negativeRed,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier
                                .clickable {
                                    viewModel.signOut()
                                    android.widget.Toast.makeText(context, "Unlinked Google account successfully.", android.widget.Toast.LENGTH_SHORT).show()
                                }
                                .padding(4.dp)
                        )
                    }
                }

                // Switch option
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1.0f)) {
                        Text("Enable Google Cloud Real-Time Auto-Sync", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = AuraTheme.colors.textPrimary)
                        Text("Triggers background drive updates on any database frames changed.", fontSize = 9.sp, color = AuraTheme.colors.textMuted)
                    }
                    Switch(
                        checked = isSyncEnabled,
                        onCheckedChange = { viewModel.setCloudSyncEnabled(it) },
                        colors = SwitchDefaults.colors(checkedThumbColor = AuraTheme.colors.accentBrand, checkedTrackColor = AuraTheme.colors.accentBrand.copy(alpha = 0.5f))
                    )
                }

                // Database Frames summary
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(AuraTheme.colors.bottomNavBackground, RoundedCornerShape(10.dp))
                        .border(1.dp, AuraTheme.colors.cardBorder.copy(alpha = 0.5f), RoundedCornerShape(10.dp))
                        .padding(10.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("COMPRESSED DATA SUMMARY FOR DRIVE:", fontSize = 8.sp, fontWeight = FontWeight.Black, color = AuraTheme.colors.gold, letterSpacing = 0.5.sp)
                        Text(
                            text = "⚡ Real Database Payload: $notesCount Notes | $tasksCount Tasks | $habitsCount Habits | $transactionCount Ledger Records",
                            fontSize = 10.sp,
                            color = AuraTheme.colors.textSecondary
                        )
                    }
                }

                // Export/Import buttons simulating save and other device
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = { 
                            if (userEmail == null) {
                                android.widget.Toast.makeText(context, "Please connect Google Drive profile first to export payload!", android.widget.Toast.LENGTH_LONG).show()
                            } else {
                                viewModel.createGoogleDriveBackup()
                                android.widget.Toast.makeText(context, "Compressed $notesCount notes, $tasksCount tasks, and $habitsCount habits exported safely to Drive!", android.widget.Toast.LENGTH_LONG).show()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = AuraTheme.colors.accentBrand),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(vertical = 4.dp)
                    ) {
                        Icon(Icons.Default.CloudUpload, contentDescription = "Export icon", tint = Color.White, modifier = Modifier.size(13.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("EXPORT & SAVE", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = { 
                            if (userEmail == null) {
                                android.widget.Toast.makeText(context, "Please connect Google Drive profile first to import payload!", android.widget.Toast.LENGTH_LONG).show()
                            } else {
                                viewModel.triggerSyncNow()
                                android.widget.Toast.makeText(context, "Retrieving database payload indexes; device indices refreshed successfully!", android.widget.Toast.LENGTH_SHORT).show()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = AuraTheme.colors.bottomNavBackground),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(vertical = 4.dp),
                        border = BorderStroke(1.dp, AuraTheme.colors.cardBorder)
                    ) {
                        Icon(Icons.Default.CloudDownload, contentDescription = "Import icon", tint = AuraTheme.colors.accentBrand, modifier = Modifier.size(13.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("IMPORT / RESTORE", color = AuraTheme.colors.textPrimary, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                }

                // Simulating Cross-device
                Button(
                    onClick = {
                        if (userEmail == null) {
                            android.widget.Toast.makeText(context, "Google Account required to simulate cross-device syncing!", android.widget.Toast.LENGTH_LONG).show()
                        } else {
                            viewModel.simulateDeviceImportFromDrive()
                            android.widget.Toast.makeText(context, "Synchronizing current database with external workspace indices!", android.widget.Toast.LENGTH_LONG).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AuraTheme.colors.bottomNavBackground),
                    border = BorderStroke(1.dp, AuraTheme.colors.cardBorder),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(vertical = 4.dp)
                ) {
                    Icon(Icons.Default.Sync, contentDescription = "Simulate pull", tint = AuraTheme.colors.accentBrand, modifier = Modifier.size(13.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("SIMULATE EXTERNAL DEVICE SYNC & PULL", color = AuraTheme.colors.textPrimary, fontSize = 9.sp, fontWeight = FontWeight.Black, letterSpacing = 0.2.sp)
                }

                // Connected Devices List
                Spacer(modifier = Modifier.height(4.dp))
                Text("WORKSPACE CLOUD PARTICIPANT DEVICES:", fontSize = 8.sp, fontWeight = FontWeight.Black, color = AuraTheme.colors.textMuted, letterSpacing = 0.5.sp)
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    connectedDevices.forEach { device ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(AuraTheme.colors.bottomNavBackground, RoundedCornerShape(8.dp))
                                .border(1.dp, AuraTheme.colors.cardBorder.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                                .padding(horizontal = 8.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Icon(
                                    imageVector = if (device.contains("This Device")) Icons.Default.Smartphone else Icons.Default.Tv,
                                    contentDescription = "Device icon",
                                    tint = if (device.contains("This Device")) AuraTheme.colors.accentBrand else AuraTheme.colors.textMuted,
                                    modifier = Modifier.size(14.dp)
                                )
                                Text(device, fontSize = 10.sp, color = AuraTheme.colors.textPrimary, fontWeight = FontWeight.Medium)
                            }
                            Text(
                                text = if (device.contains("This Device")) "Active Now" else "Idle State Sync",
                                fontSize = 8.sp,
                                color = if (device.contains("This Device")) AuraTheme.colors.positiveGreen else AuraTheme.colors.textMuted
                            )
                        }
                    }
                }
            }
        }

        // ======================================================================
        // NEW CARD: CAPTURE BUTTON AESTHETICS & FEEL (USER REQUEST)
        // ======================================================================
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, AuraTheme.colors.cardBorder, RoundedCornerShape(20.dp)),
            colors = CardDefaults.cardColors(containerColor = AuraTheme.colors.cardBackground),
            shape = RoundedCornerShape(20.dp)
        ) {
            val currentCaptureColor by viewModel.captureButtonColor.collectAsState()
            val currentCaptureAnim by viewModel.captureButtonAnimationType.collectAsState()

            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(
                        imageVector = Icons.Default.Palette,
                        contentDescription = "Aesthetics logo",
                        tint = AuraTheme.colors.accentBrand,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        "CAPTURE BUTTON AESTHETICS",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = AuraTheme.colors.textPrimary,
                        letterSpacing = 0.5.sp
                    )
                }

                Text(
                    "Personalize the interactive glowing aura, haptic trigger states, and physics dynamics of the floating Quick Capture overlay.",
                    color = AuraTheme.colors.textSecondary,
                    fontSize = 11.sp,
                    lineHeight = 15.sp
                )

                HorizontalDivider(color = AuraTheme.colors.cardBorder.copy(alpha = 0.5f))

                // 1. Color Selector
                Text("INTELLIGENT TINT GLOW COLOR:", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = AuraTheme.colors.accentBrand)

                class LocalCaptureColorSetting(val id: String, val label: String, val brush: Brush)

                val colorOptions = listOf(
                    LocalCaptureColorSetting("DEFAULT", "Gradient", Brush.linearGradient(listOf(AuraTheme.colors.accentBrand, AuraTheme.colors.gold))),
                    LocalCaptureColorSetting("CYAN", "Cyber Cyan", Brush.linearGradient(listOf(AuraTheme.colors.accentBrand, AuraTheme.colors.accentBrand))),
                    LocalCaptureColorSetting("PURPLE", "Aura Purple", Brush.linearGradient(listOf(Color(0xFF7C4DFF), Color(0xFF7C4DFF)))),
                    LocalCaptureColorSetting("EMERALD", "Emerald Mint", Brush.linearGradient(listOf(AuraTheme.colors.positiveGreen, AuraTheme.colors.positiveGreen))),
                    LocalCaptureColorSetting("PINK", "Hot Pink", Brush.linearGradient(listOf(Color(0xFFFF2A85), Color(0xFFFF2A85)))),
                    LocalCaptureColorSetting("RED", "Volcano", Brush.linearGradient(listOf(AuraTheme.colors.negativeRed, AuraTheme.colors.negativeRed))),
                    LocalCaptureColorSetting("AMBER", "Amber Gold", Brush.linearGradient(listOf(AuraTheme.colors.gold, AuraTheme.colors.gold)))
                )

                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(colorOptions) { option ->
                        val isSelected = currentCaptureColor == option.id
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(AuraTheme.colors.bottomNavBackground)
                                .border(1.5.dp, if (isSelected) AuraTheme.colors.accentBrand else AuraTheme.colors.cardBorder, RoundedCornerShape(8.dp))
                                .clickable {
                                    viewModel.setCaptureButtonColor(option.id)
                                }
                                .padding(horizontal = 10.dp, vertical = 6.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Box(
                                    modifier = Modifier
                                        .size(12.dp)
                                        .clip(CircleShape)
                                        .background(option.brush)
                                )
                                Text(option.label, color = AuraTheme.colors.textPrimary, fontSize = 9.sp, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                }

                HorizontalDivider(color = AuraTheme.colors.cardBorder.copy(alpha = 0.5f))

                // 2. Animation Selector
                Text("PULSE & ANIMATION DYNAMICS:", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = AuraTheme.colors.gold)

                val animOptions = listOf(
                    Pair("SPRING", "Classic Spring"),
                    Pair("BREATHE", "Aura Breathing"),
                    Pair("PULSE", "Holographic Pulse"),
                    Pair("SPIN", "Subtle Spin")
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    animOptions.forEach { anim ->
                        val isSelected = currentCaptureAnim == anim.first
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSelected) AuraTheme.colors.accentBrand.copy(alpha = 0.2f) else AuraTheme.colors.bottomNavBackground)
                                .border(1.5.dp, if (isSelected) AuraTheme.colors.accentBrand else AuraTheme.colors.cardBorder, RoundedCornerShape(8.dp))
                                .clickable {
                                    viewModel.setCaptureButtonAnimationType(anim.first)
                                }
                                .padding(vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = anim.second,
                                color = if (isSelected) Color.White else AuraTheme.colors.textMuted,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }
        }

        // 1. PIN LOCK CARD
        Card(
            modifier = Modifier.fillMaxWidth().border(1.dp, AuraTheme.colors.cardBorder, RoundedCornerShape(16.dp)),
            colors = CardDefaults.cardColors(containerColor = AuraTheme.colors.cardBackground)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    "SECURITY CONFIRMATION",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = AuraTheme.colors.accentBrand,
                    letterSpacing = 1.sp
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Enable PIN Lock Passcode", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = AuraTheme.colors.textPrimary)
                        Text("Prompts keypad gate upon launching Aura safeguard", fontSize = 11.sp, color = AuraTheme.colors.textSecondary)
                    }

                    Switch(
                        checked = isEnabled,
                        onCheckedChange = { isEnabled = it },
                        colors = SwitchDefaults.colors(checkedThumbColor = AuraTheme.colors.accentBrand, checkedTrackColor = AuraTheme.colors.accentBrand.copy(alpha = 0.5f))
                    )
                }

                if (isEnabled) {
                    Spacer(modifier = Modifier.height(16.dp))
                    OutlinedTextField(
                        value = pinValue,
                        onValueChange = { input ->
                            if (input.length <= 4 && input.all { it.isDigit() }) {
                                pinValue = input
                            }
                        },
                        placeholder = { Text("Enter 4-digit PIN", color = AuraTheme.colors.textMuted) },
                        label = { Text("Security passcode", color = AuraTheme.colors.accentBrand) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = AuraTheme.colors.accentBrand,
                            unfocusedBorderColor = AuraTheme.colors.cardBorder,
                            focusedTextColor = AuraTheme.colors.textPrimary,
                            unfocusedTextColor = AuraTheme.colors.textPrimary
                        ),
                        shape = RoundedCornerShape(10.dp),
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                            keyboardType = androidx.compose.ui.text.input.KeyboardType.NumberPassword
                        )
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = {
                        if (isEnabled && pinValue.length < 4) {
                            android.widget.Toast.makeText(context, "PIN code must be exactly 4 digits.", android.widget.Toast.LENGTH_SHORT).show()
                        } else {
                            viewModel.configurePin(
                                pin = if (isEnabled) pinValue else null,
                                enabled = isEnabled
                            )
                            android.widget.Toast.makeText(context, "Security policies updated successfully.", android.widget.Toast.LENGTH_SHORT).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AuraTheme.colors.accentBrand),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Apply Security Policies", color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
        }

        // 1.5 VISUAL SYSTEM THEMING CARD
        val activeMode by viewModel.themeMode.collectAsState()
        val activePalette by viewModel.themePalette.collectAsState()

        Card(
            modifier = Modifier.fillMaxWidth().border(1.dp, AuraTheme.colors.cardBorder, RoundedCornerShape(16.dp)),
            colors = CardDefaults.cardColors(containerColor = AuraTheme.colors.cardBackground)
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    "VISUAL COLOR WORKSPACE",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = AuraTheme.colors.accentBrand,
                    letterSpacing = 1.sp
                )
                Text(
                    "Customize workspace environments, background theme, and overall color accent palettes.",
                    fontSize = 11.sp,
                    color = AuraTheme.colors.textSecondary
                )

                // Theme Mode Selector
                Text("Background Mode", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = AuraTheme.colors.textPrimary)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val modes = listOf("DARK" to "Deep Slate", "AMOLED" to "Black AMOLED", "LIGHT" to "Premium Bone")
                    modes.forEach { (m, label) ->
                        val isSelected = activeMode == m
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(40.dp)
                                .background(
                                    if (isSelected) AuraTheme.colors.accentBrand else AuraTheme.colors.bottomNavBackground,
                                    RoundedCornerShape(10.dp)
                                )
                                .border(1.dp, if (isSelected) AuraTheme.colors.accentBrand else AuraTheme.colors.cardBorder, RoundedCornerShape(10.dp))
                                .clickable { viewModel.setThemeMode(m) },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(label, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = if (isSelected) Color.White else AuraTheme.colors.textSecondary)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                // Color accent palettes
                Text("Color Accent Palette", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = AuraTheme.colors.textPrimary)
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    val palettes = listOf(
                        "RADIANT_SUNSET" to Pair("Fintech Sunset Coral (Default)", RadiantOrange),
                        "AXIO_LIME" to Pair("Axio Electric Lime", AxioElectricLime),
                        "CYAN_GLOW" to Pair("Aura Neon Cyan", Color(0xFF00E5FF)),
                        "EMERALD_GARDEN" to Pair("Mint Emerald", SemanticGreen),
                        "ROYAL_AMETHYST" to Pair("Royal Amethyst", Color(0xFFBB86FC)),
                        "OCEAN_BREEZE" to Pair("Ocean Breeze", Color(0xFF00B0FF))
                    )
                    palettes.forEach { (p, pair) ->
                        val (pLabel, pColor) = pair
                        val isSelected = activePalette == p
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(if (isSelected) AuraTheme.colors.accentBrand.copy(alpha = 0.2f) else AuraTheme.colors.bottomNavBackground, RoundedCornerShape(10.dp))
                                .border(1.dp, if (isSelected) AuraTheme.colors.accentBrand else AuraTheme.colors.cardBorder, RoundedCornerShape(10.dp))
                                .clickable { viewModel.setThemePalette(p) }
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(16.dp)
                                        .background(pColor, CircleShape)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(pLabel, fontSize = 12.sp, color = AuraTheme.colors.textPrimary, fontWeight = FontWeight.SemiBold)
                            }
                            if (isSelected) {
                                Icon(Icons.Default.Check, contentDescription = "Selected", tint = AuraTheme.colors.accentBrand, modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                }
            }
        }

        // 2. AURA ROBOT COMPANION CARD
        val isRobotCompanionEnabled by viewModel.isRobotCompanionEnabled.collectAsState()
        Card(
            modifier = Modifier.fillMaxWidth().border(1.dp, AuraTheme.colors.cardBorder, RoundedCornerShape(16.dp)),
            colors = CardDefaults.cardColors(containerColor = AuraTheme.colors.cardBackground)
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            "🤖 AURA ROBOT COMPANION",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = AuraTheme.colors.accentBrand,
                            letterSpacing = 1.sp
                        )
                        Text(
                            "Interactive walking mini buddy with physics & speech",
                            fontSize = 11.sp,
                            color = AuraTheme.colors.textSecondary
                        )
                    }
                    Switch(
                        checked = isRobotCompanionEnabled,
                        onCheckedChange = { viewModel.setRobotCompanionEnabled(it) },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = AuraTheme.colors.accentBrand
                        )
                    )
                }

                HorizontalDivider(color = AuraTheme.colors.cardBorder, thickness = 0.8.dp)

                Text(
                    "Interactive Gestures & Physics:",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = AuraTheme.colors.textPrimary
                )

                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(AuraTheme.colors.bottomNavBackground, RoundedCornerShape(10.dp))
                            .border(1.dp, AuraTheme.colors.cardBorder, RoundedCornerShape(10.dp))
                            .padding(horizontal = 10.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text("👆", fontSize = 16.sp)
                        Column {
                            Text("Push & Stumble", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = AuraTheme.colors.textPrimary)
                            Text("Single tap him to push him back with cute dizzy eyes and wobble animation", fontSize = 10.sp, color = AuraTheme.colors.textMuted)
                        }
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(AuraTheme.colors.bottomNavBackground, RoundedCornerShape(10.dp))
                            .border(1.dp, AuraTheme.colors.cardBorder, RoundedCornerShape(10.dp))
                            .padding(horizontal = 10.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text("⚡", fontSize = 16.sp)
                        Column {
                            Text("Sturdy Power Shield", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = AuraTheme.colors.badgeGold)
                            Text("Double-tap him to activate super strong stance with glowing energy shield", fontSize = 10.sp, color = AuraTheme.colors.textMuted)
                        }
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(AuraTheme.colors.bottomNavBackground, RoundedCornerShape(10.dp))
                            .border(1.dp, AuraTheme.colors.cardBorder, RoundedCornerShape(10.dp))
                            .padding(horizontal = 10.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text("🪂", fontSize = 16.sp)
                        Column {
                            Text("Drag, Fling & Bouncing Gravity", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF00E5FF))
                            Text("Drag anywhere on screen, throw him, and watch him bounce and squash elastically", fontSize = 10.sp, color = AuraTheme.colors.textMuted)
                        }
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(AuraTheme.colors.bottomNavBackground, RoundedCornerShape(10.dp))
                            .border(1.dp, AuraTheme.colors.cardBorder, RoundedCornerShape(10.dp))
                            .padding(horizontal = 10.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text("💬", fontSize = 16.sp)
                        Column {
                            Text("Friendly Speech & Quick Hub", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = AuraTheme.colors.positiveGreen)
                            Text("He speaks to you periodically. Tap his speech bubble or long-press him to open Quick Hub", fontSize = 10.sp, color = AuraTheme.colors.textMuted)
                        }
                    }
                }
            }
        }

        // 3. QUICK SENT TARGET CARD
        Card(
            modifier = Modifier.fillMaxWidth().border(1.dp, AuraTheme.colors.cardBorder, RoundedCornerShape(16.dp)),
            colors = CardDefaults.cardColors(containerColor = AuraTheme.colors.cardBackground)
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    "QUICK TRANSACTION TARGETS",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = AuraTheme.colors.accentBrand,
                    letterSpacing = 1.sp
                )
                Text(
                    "Customize destination options available under Quick Transaction 'Money Sent'. Default categories include Food, Friend, Merchant.",
                    fontSize = 11.sp,
                    color = AuraTheme.colors.textSecondary
                )

                // Grid layout of target options
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    sentOptions.chunked(3).forEach { rowOpts ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            rowOpts.forEach { opt ->
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .background(AuraTheme.colors.bottomNavBackground, RoundedCornerShape(8.dp))
                                        .border(1.dp, AuraTheme.colors.cardBorder, RoundedCornerShape(8.dp))
                                        .padding(horizontal = 8.dp, vertical = 6.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(opt, fontSize = 10.sp, color = AuraTheme.colors.textPrimary, fontWeight = FontWeight.Bold)
                                        IconButton(
                                            onClick = { viewModel.removeSentOption(opt) },
                                            modifier = Modifier.size(16.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Close,
                                                contentDescription = "Remove option",
                                                tint = AuraTheme.colors.negativeRed,
                                                modifier = Modifier.size(10.dp)
                                            )
                                        }
                                    }
                                }
                            }
                            // Fill remaining space if chunks < 3
                            repeat(3 - rowOpts.size) {
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                    }
                }

                HorizontalDivider(color = AuraTheme.colors.cardBorder.copy(alpha = 0.5f))

                // Inline form to add option
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = newOptionText,
                        onValueChange = { newOptionText = it },
                        placeholder = { Text("e.g. Rent, Gift, Travel", color = AuraTheme.colors.textMuted) },
                        label = { Text("Add Custom Option", color = AuraTheme.colors.accentBrand) },
                        modifier = Modifier.weight(1f),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = AuraTheme.colors.accentBrand,
                            unfocusedBorderColor = AuraTheme.colors.cardBorder,
                            focusedTextColor = AuraTheme.colors.textPrimary,
                            unfocusedTextColor = AuraTheme.colors.textPrimary
                        ),
                        shape = RoundedCornerShape(10.dp),
                        singleLine = true
                    )
                    
                    Button(
                        onClick = {
                            if (newOptionText.isNotBlank()) {
                                viewModel.addSentOption(newOptionText)
                                newOptionText = ""
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = AuraTheme.colors.accentBrand),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("Add", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }
            }
        }

        // 3. FRIENDS REGISTER CARD
        Card(
            modifier = Modifier.fillMaxWidth().border(1.dp, AuraTheme.colors.cardBorder, RoundedCornerShape(16.dp)),
            colors = CardDefaults.cardColors(containerColor = AuraTheme.colors.cardBackground)
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    "FRIENDS DIRECTORY",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = AuraTheme.colors.accentBrand,
                    letterSpacing = 1.sp
                )
                Text(
                    "Manage your friends registry used for splitting bills. Your list is stored locally on this device.",
                    fontSize = 11.sp,
                    color = AuraTheme.colors.textSecondary
                )

                // Scrollable list of friends directly inside the card in Settings with delete triggers
                if (friends.isEmpty()) {
                    Text(
                        "No friends registered in directory yet.",
                        color = AuraTheme.colors.textMuted,
                        fontSize = 11.sp,
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                } else {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        friends.forEach { frnd ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(AuraTheme.colors.bottomNavBackground, RoundedCornerShape(10.dp))
                                    .border(1.dp, AuraTheme.colors.cardBorder, RoundedCornerShape(10.dp))
                                    .padding(horizontal = 10.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(frnd.name, fontSize = 12.sp, color = AuraTheme.colors.textPrimary, fontWeight = FontWeight.Bold)
                                    if (frnd.notes.isNotBlank() || frnd.phone.isNotBlank()) {
                                        Text(
                                            listOfNotNull(
                                                frnd.phone.takeIf { it.isNotBlank() },
                                                frnd.notes.takeIf { it.isNotBlank() }
                                            ).joinToString(" | "),
                                            fontSize = 9.sp,
                                            color = AuraTheme.colors.textMuted
                                        )
                                    }
                                }
                                IconButton(
                                    onClick = { viewModel.deleteFriend(frnd) },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = "Remove friend",
                                        tint = AuraTheme.colors.negativeRed,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                HorizontalDivider(color = AuraTheme.colors.cardBorder.copy(alpha = 0.5f))

                // Inline form to add friend
                Text("Register New Friend Entry", fontSize = 11.sp, color = AuraTheme.colors.textSecondary, fontWeight = FontWeight.Bold)
                
                OutlinedTextField(
                    value = newFriendName,
                    onValueChange = { newFriendName = it },
                    placeholder = { Text("Friend Name", color = AuraTheme.colors.textMuted) },
                    label = { Text("Full Name", color = AuraTheme.colors.accentBrand) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = AuraTheme.colors.accentBrand,
                        unfocusedBorderColor = AuraTheme.colors.cardBorder,
                        focusedTextColor = AuraTheme.colors.textPrimary,
                        unfocusedTextColor = AuraTheme.colors.textPrimary
                    ),
                    shape = RoundedCornerShape(10.dp),
                    singleLine = true
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = newFriendPhone,
                        onValueChange = { newFriendPhone = it },
                        placeholder = { Text("Phone (optional)", color = AuraTheme.colors.textMuted) },
                        label = { Text("Phone", color = AuraTheme.colors.accentBrand) },
                        modifier = Modifier.weight(1f),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = AuraTheme.colors.accentBrand,
                            unfocusedBorderColor = AuraTheme.colors.cardBorder,
                            focusedTextColor = AuraTheme.colors.textPrimary,
                            unfocusedTextColor = AuraTheme.colors.textPrimary
                        ),
                        shape = RoundedCornerShape(10.dp),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = newFriendNotes,
                        onValueChange = { newFriendNotes = it },
                        placeholder = { Text("e.g. Roommate", color = AuraTheme.colors.textMuted) },
                        label = { Text("Notes", color = AuraTheme.colors.accentBrand) },
                        modifier = Modifier.weight(1f),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = AuraTheme.colors.accentBrand,
                            unfocusedBorderColor = AuraTheme.colors.cardBorder,
                            focusedTextColor = AuraTheme.colors.textPrimary,
                            unfocusedTextColor = AuraTheme.colors.textPrimary
                        ),
                        shape = RoundedCornerShape(10.dp),
                        singleLine = true
                    )
                }

                Button(
                    onClick = {
                        if (newFriendName.isNotBlank()) {
                            viewModel.addFriend(newFriendName, newFriendPhone, newFriendNotes)
                            newFriendName = ""
                            newFriendPhone = ""
                            newFriendNotes = ""
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AuraTheme.colors.accentBrand),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("+ Add Friend to Ledger", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
            }
        }

        // ======================================================================
        // ABOUT US CARD (USER REQUEST)
        // ======================================================================
        val currentAboutUsText by viewModel.aboutUsText.collectAsState()
        val isOwner = (userEmail?.trim()?.lowercase() == "moreaboutastram@gmail.com")
        var isEditingAboutUs by remember { mutableStateOf(false) }
        var tempAboutUsInput by remember { mutableStateOf("") }

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, AuraTheme.colors.cardBorder, RoundedCornerShape(16.dp)),
            colors = CardDefaults.cardColors(containerColor = AuraTheme.colors.cardBackground),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = "About us logo",
                            tint = AuraTheme.colors.accentBrand,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            "ABOUT US & CREATOR PANEL",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = AuraTheme.colors.textPrimary,
                            letterSpacing = 1.sp
                        )
                    }

                    // Only show "EDIT" button if logged in as moreaboutastram@gmail.com
                    if (isOwner) {
                        if (isEditingAboutUs) {
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                TextButton(
                                    onClick = { isEditingAboutUs = false },
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                                ) {
                                    Text("CANCEL", color = AuraTheme.colors.negativeRed, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                }
                                Button(
                                    onClick = {
                                        viewModel.updateAboutUsText(tempAboutUsInput)
                                        isEditingAboutUs = false
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = AuraTheme.colors.accentBrand),
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                                ) {
                                    Text("SAVE", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        } else {
                            Button(
                                onClick = {
                                    tempAboutUsInput = currentAboutUsText
                                    isEditingAboutUs = true
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = AuraTheme.colors.accentBrand),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                            ) {
                                Text("EDIT (OWNER)", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    } else {
                        // Helpful reminder for other emails
                        Box(
                            modifier = Modifier
                                .background(AuraTheme.colors.bottomNavBackground, RoundedCornerShape(6.dp))
                                .border(1.dp, AuraTheme.colors.cardBorder, RoundedCornerShape(6.dp))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text("READ ONLY", color = AuraTheme.colors.textMuted, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                if (isEditingAboutUs && isOwner) {
                    OutlinedTextField(
                        value = tempAboutUsInput,
                        onValueChange = { tempAboutUsInput = it },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = AuraTheme.colors.accentBrand,
                            unfocusedBorderColor = AuraTheme.colors.cardBorder,
                            focusedTextColor = AuraTheme.colors.textPrimary,
                            unfocusedTextColor = AuraTheme.colors.textPrimary
                        ),
                        textStyle = androidx.compose.ui.text.TextStyle(fontSize = 12.sp, lineHeight = 16.sp)
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(AuraTheme.colors.bottomNavBackground, RoundedCornerShape(10.dp))
                            .border(1.dp, AuraTheme.colors.cardBorder, RoundedCornerShape(10.dp))
                            .padding(12.dp)
                    ) {
                        SelectionContainer {
                            Text(
                                text = currentAboutUsText,
                                color = AuraTheme.colors.textPrimary,
                                fontSize = 12.sp,
                                lineHeight = 16.sp
                            )
                        }
                    }
                }

                if (!isOwner) {
                    Text(
                        "* Log in with Gmail ‘moreaboutastram@gmail.com’ via Sync to authenticate as owner and edit this content.",
                        color = AuraTheme.colors.textMuted,
                        fontSize = 9.sp,
                        lineHeight = 12.sp
                    )
                }
            }
        }

        // Elegant minimal footer section
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = "your support matters".uppercase(),
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = AuraTheme.colors.accentBrand,
                letterSpacing = 2.sp
            )
            
            Text(
                text = "created by aadii_xy",
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                color = AuraTheme.colors.textMuted,
                letterSpacing = 1.sp
            )
        }
    }
}

// ==========================================
// FLOATING PLAYER POPUP CARD
// ==========================================
@Composable
fun GlobalMiniFloatingPlayer(
    header: String,
    isPlaying: Boolean,
    progress: Float,
    onTogglePlayPause: () -> Unit,
    onSeek: (Float) -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .border(
                1.dp,
                AuraTheme.colors.cardBorder,
                RoundedCornerShape(16.dp)
            ),
        colors = CardDefaults.cardColors(containerColor = AuraTheme.colors.cardBackground),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    Icon(
                        imageVector = Icons.Default.MusicNote,
                        contentDescription = "Voice playing note",
                        tint = AuraTheme.colors.accentBrand,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = header,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = AuraTheme.colors.textPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                IconButton(onClick = onClose, modifier = Modifier.size(24.dp)) {
                    Icon(Icons.Default.Close, contentDescription = "Close Player", tint = AuraTheme.colors.negativeRed, modifier = Modifier.size(14.dp))
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Wave progress slider bar
            Slider(
                value = progress,
                onValueChange = onSeek,
                colors = SliderDefaults.colors(
                    thumbColor = AuraTheme.colors.accentBrand,
                    activeTrackColor = AuraTheme.colors.accentBrand,
                    inactiveTrackColor = AuraTheme.colors.cardBorder
                ),
                modifier = Modifier.height(18.dp)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onTogglePlayPause, modifier = Modifier.size(36.dp)) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Default.PauseCircleFilled else Icons.Default.PlayCircleFilled,
                        contentDescription = "Playback toggle button",
                        tint = AuraTheme.colors.accentBrand,
                        modifier = Modifier.size(30.dp)
                    )
                }
            }
        }
    }
}

// ==========================================
// HABITS SCREEN TAB DETAILS
// ==========================================
@Composable
fun HabitsTabScreen(
    viewModel: AppViewModel
) {
    val habitsList by viewModel.habits.collectAsState()
    val rawLogs by viewModel.habitLogs.collectAsState()

    var showCreateDialog by remember { mutableStateOf(false) }
    var selectedTabFreq by remember { mutableStateOf("Daily") } // Daily vs Weekly

    // Habit Timer & Configuration Customization States
    var habitToConfigure by remember { mutableStateOf<Habit?>(null) }
    var configReminderTime by remember { mutableStateOf("") }
    var configTargetMins by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AuraTheme.colors.screenBackground)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text("HABIT MATRIX", fontSize = 18.sp, fontWeight = FontWeight.Black, color = AuraTheme.colors.textPrimary, letterSpacing = 1.sp)
                Text("Repetitive neurological feedback structures", fontSize = 11.sp, color = AuraTheme.colors.textSecondary)
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                AuraSectionInfoButton(
                    viewModel = viewModel,
                    title = "Habit Matrix",
                    description = "Form atomic habits through repetitive feedback loops. Log completions, set dynamic goals with timed focus logs, and visualize completion metrics."
                )
                Button(
                    onClick = { showCreateDialog = true },
                    colors = ButtonDefaults.buttonColors(containerColor = AuraTheme.colors.accentBrand),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Add Habit", tint = Color.White, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("INSERT", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        // ACTIVE FLOATING HABIT Practice Timer BANNER
        val activeHabitId by viewModel.activeTimerHabitId.collectAsState()
        val habitSecondsLeft by viewModel.habitSecondsLeft.collectAsState()
        val isHabitTimerRunning by viewModel.isHabitTimerRunning.collectAsState()

        if (activeHabitId != null) {
            val timedHabit = habitsList.find { it.id == activeHabitId }
            if (timedHabit != null) {
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
                                    contentDescription = "Active Habit Timer",
                                    tint = AuraTheme.colors.accentBrand,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "ACTIVE HABIT FOCUS PRACTICE",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = AuraTheme.colors.accentBrand,
                                    letterSpacing = 1.sp
                                )
                            }
                            IconButton(
                                onClick = { viewModel.resetHabitTimer() },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(Icons.Default.Close, contentDescription = "Close Timer", tint = AuraTheme.colors.negativeRed, modifier = Modifier.size(14.dp))
                            }
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        Text(
                            text = timedHabit.name,
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
                            val mins = habitSecondsLeft / 60
                            val secs = habitSecondsLeft % 60
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
                                        if (isHabitTimerRunning) {
                                            viewModel.pauseHabitTimer()
                                        } else {
                                            viewModel.resumeHabitTimer()
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = if (isHabitTimerRunning) AuraTheme.colors.gold else AuraTheme.colors.accentBrand),
                                    shape = RoundedCornerShape(8.dp),
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                                ) {
                                    Icon(
                                        imageVector = if (isHabitTimerRunning) Icons.Default.Pause else Icons.Default.PlayArrow,
                                        contentDescription = "Play/pause button",
                                        tint = Color.White,
                                        modifier = Modifier.size(12.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = if (isHabitTimerRunning) "PAUSE" else "RESUME",
                                        fontSize = 9.sp,
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold
                                    )
                                }

                                OutlinedButton(
                                    onClick = { viewModel.resetHabitTimer() },
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

        TabRow(
            selectedTabIndex = if (selectedTabFreq == "Daily") 0 else 1,
            containerColor = AuraTheme.colors.cardBackground,
            contentColor = AuraTheme.colors.accentBrand
        ) {
            Tab(selected = selectedTabFreq == "Daily", onClick = { selectedTabFreq = "Daily" }, text = { Text("DAILY LOOPS", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = if (selectedTabFreq == "Daily") AuraTheme.colors.accentBrand else AuraTheme.colors.textMuted) })
            Tab(selected = selectedTabFreq == "Weekly", onClick = { selectedTabFreq = "Weekly" }, text = { Text("WEEKLY LOOPS", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = if (selectedTabFreq == "Weekly") AuraTheme.colors.accentBrand else AuraTheme.colors.textMuted) })
        }

        Spacer(modifier = Modifier.height(12.dp))

        val filteredHabits = habitsList.filter { it.frequency == selectedTabFreq }

        if (filteredHabits.isEmpty()) {
            Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                AuraEmptyState(
                    title = "No ${selectedTabFreq} Habits",
                    description = "Build consistency with atomic daily or weekly routine habits.",
                    icon = Icons.Default.LocalFireDepartment,
                    iconTint = AuraTheme.colors.gold
                )
            }
        } else {
            val todayString = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(filteredHabits, key = { it.id }) { habit ->
                    // Gather completion state for today
                    val isDoneToday = rawLogs.any { it.habitId == habit.id && it.completionDate == todayString }
                    
                    val habitsStreakFlow = remember(habit.id) { viewModel.getStreakForHabit(habit.id) }.collectAsState(initial = HabitStreak(0,0))
                    val completePctFlow = remember(habit.id) { viewModel.getCompletionPercentageForHabit(habit.id) }.collectAsState(initial = 0f)

                    val alarmTime = viewModel.getHabitReminderTime(habit.id)
                    val limitMins = viewModel.getHabitTargetMinutes(habit.id)

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, AuraTheme.colors.cardBorder, RoundedCornerShape(14.dp)),
                        colors = CardDefaults.cardColors(containerColor = AuraTheme.colors.cardBackground),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                IconButton(
                                    onClick = { viewModel.toggleHabitToday(habit.id, !isDoneToday) },
                                    modifier = Modifier.size(28.dp).testTag("habit_toggle_${habit.id}")
                                ) {
                                    Icon(
                                        imageVector = if (isDoneToday) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                                        contentDescription = "Complete Habit for today",
                                        tint = if (isDoneToday) AuraTheme.colors.positiveGreen else AuraTheme.colors.textMuted,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }

                                Spacer(modifier = Modifier.width(10.dp))

                                Column {
                                    Text(
                                        text = habit.name,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isDoneToday) AuraTheme.colors.textMuted else AuraTheme.colors.textPrimary
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(Icons.Default.ChevronRight, contentDescription = "Active streak", tint = AuraTheme.colors.gold, modifier = Modifier.size(10.dp))
                                            Spacer(modifier = Modifier.width(2.dp))
                                            Text(
                                                "STREAK: ${habitsStreakFlow.value.currentStreak} d",
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                color = AuraTheme.colors.textSecondary
                                            )
                                        }

                                        if (!alarmTime.isNullOrBlank()) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Icon(Icons.Default.NotificationsActive, contentDescription = "Alarm active", tint = AuraTheme.colors.accentBrand, modifier = Modifier.size(10.dp))
                                                Spacer(modifier = Modifier.width(2.dp))
                                                Text(alarmTime, fontSize = 9.sp, color = AuraTheme.colors.accentBrand, fontWeight = FontWeight.Bold)
                                            }
                                        }

                                        if (limitMins > 0) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Icon(Icons.Default.Timer, contentDescription = "Target limit", tint = AuraTheme.colors.gold, modifier = Modifier.size(10.dp))
                                                Spacer(modifier = Modifier.width(2.dp))
                                                Text("${limitMins}m Goal", fontSize = 9.sp, color = AuraTheme.colors.gold, fontWeight = FontWeight.Bold)
                                            }
                                        }
                                    }
                                }
                            }

                            // Streaks indicator & play controls
                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = "${(completePctFlow.value * 100).toInt()}% COMPLETED",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = AuraTheme.colors.positiveGreen
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    // Start habit timer loop
                                    IconButton(
                                        onClick = {
                                            val minutesToRun = if (limitMins > 0) limitMins else 15
                                            viewModel.startHabitTimer(habit.id, minutesToRun)
                                        },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(Icons.Default.PlayCircle, contentDescription = "Start Habit Timer", tint = AuraTheme.colors.accentBrand, modifier = Modifier.size(16.dp))
                                    }

                                    // Config details
                                    IconButton(
                                        onClick = { habitToConfigure = habit },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(Icons.Default.Settings, contentDescription = "Habit Alarm Settings", tint = AuraTheme.colors.textMuted, modifier = Modifier.size(14.dp))
                                    }

                                    IconButton(onClick = { viewModel.deleteHabit(habit) }, modifier = Modifier.size(24.dp)) {
                                        Icon(Icons.Default.Delete, contentDescription = "Delete Habit", tint = AuraTheme.colors.negativeRed, modifier = Modifier.size(14.dp))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showCreateDialog) {
        var inlineHabitName by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showCreateDialog = false },
            title = { Text("CREATE HABIT", color = AuraTheme.colors.textPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold) },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = inlineHabitName,
                        onValueChange = { inlineHabitName = it },
                        placeholder = { Text("Meditate, water intake...", color = AuraTheme.colors.textMuted) },
                        label = { Text("Habit name loop", color = AuraTheme.colors.accentBrand) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = AuraTheme.colors.accentBrand,
                            unfocusedBorderColor = AuraTheme.colors.cardBorder,
                            focusedTextColor = AuraTheme.colors.textPrimary,
                            unfocusedTextColor = AuraTheme.colors.textPrimary
                        )
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("Select tracking frequency", fontSize = 11.sp, color = AuraTheme.colors.textSecondary)
                    Row(modifier = Modifier.padding(top = 4.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("Daily", "Weekly").forEach { freq ->
                            val isSel = selectedTabFreq == freq
                            FilterChip(
                                selected = isSel,
                                onClick = { selectedTabFreq = freq },
                                label = { Text(freq) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = AuraTheme.colors.accentBrand,
                                    selectedLabelColor = Color.White
                                )
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (inlineHabitName.isNotBlank()) {
                            viewModel.createNewHabit(inlineHabitName, selectedTabFreq)
                            showCreateDialog = false
                            inlineHabitName = ""
                        }
                    }
                ) {
                    Text("CREATE", color = AuraTheme.colors.accentBrand, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showCreateDialog = false }) {
                    Text("CANCEL", color = AuraTheme.colors.textSecondary)
                }
            },
            containerColor = AuraTheme.colors.cardBackground
        )
    }

    if (habitToConfigure != null) {
        val editingHabit = habitToConfigure!!
        
        LaunchedEffect(editingHabit.id) {
            configReminderTime = viewModel.getHabitReminderTime(editingHabit.id) ?: ""
            configTargetMins = viewModel.getHabitTargetMinutes(editingHabit.id).let { if (it > 0) it.toString() else "" }
        }
        
        AlertDialog(
            onDismissRequest = { habitToConfigure = null },
            title = { Text("HABIT SETTINGS", color = AuraTheme.colors.textPrimary, fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Structure your habit loop with precise target guidelines and daily reminder alarm triggers.", color = AuraTheme.colors.textSecondary, fontSize = 11.sp)
                    
                    OutlinedTextField(
                        value = configReminderTime,
                        onValueChange = { configReminderTime = it },
                        placeholder = { Text("e.g. 08:30 or 21:00", color = AuraTheme.colors.textMuted) },
                        label = { Text("Daily Reminder (HH:MM)", color = AuraTheme.colors.accentBrand) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = AuraTheme.colors.accentBrand,
                            unfocusedBorderColor = AuraTheme.colors.cardBorder,
                            focusedTextColor = AuraTheme.colors.textPrimary,
                            unfocusedTextColor = AuraTheme.colors.textPrimary
                        ),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                    
                    OutlinedTextField(
                        value = configTargetMins,
                        onValueChange = { configTargetMins = it.filter { c -> c.isDigit() } },
                        placeholder = { Text("e.g. 15 or 30 mins", color = AuraTheme.colors.textMuted) },
                        label = { Text("Practice Goals (Minutes)", color = AuraTheme.colors.accentBrand) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = AuraTheme.colors.accentBrand,
                            unfocusedBorderColor = AuraTheme.colors.cardBorder,
                            focusedTextColor = AuraTheme.colors.textPrimary,
                            unfocusedTextColor = AuraTheme.colors.textPrimary
                        ),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val mins = configTargetMins.toIntOrNull() ?: 0
                        viewModel.setHabitReminderTime(editingHabit.id, configReminderTime.ifBlank { null })
                        viewModel.setHabitTargetMinutes(editingHabit.id, mins)
                        habitToConfigure = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AuraTheme.colors.accentBrand)
                ) {
                    Text("SAVE CONFIG", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { habitToConfigure = null }) {
                    Text("CANCEL", color = AuraTheme.colors.textSecondary)
                }
            },
            containerColor = AuraTheme.colors.cardBackground
        )
    }
}

