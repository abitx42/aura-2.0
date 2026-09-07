package com.example.ui.components

import android.view.View
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.example.ui.AppViewModel
import com.example.ui.AuraHaptics
import com.example.ui.Section
import com.example.ui.anim.auraSpringPress
import com.example.ui.theme.AuraTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.*
import kotlin.random.Random

/**
 * Climbable UI Object Definition
 */
data class ClimbablePlatform(
    val id: String,
    val name: String,
    val section: Section?,
    val emoji: String,
    val relativeX: Float, // 0.0 .. 1.0 of screen width center
    val elevationDp: Float, // Height in DP above baseline ground
    val climbHint: String
)

/**
 * Robot Companion emotional states and physical poses
 */
enum class RobotState {
    IDLE_STAND,
    WALKING,
    CLIMBING_UP,
    PERCHED_ON_OBJECT,
    HOPPING_DOWN,
    DRAGGED,
    PUSHED_STUMBLE,
    SUPER_STRONG,
    CHEERING
}

enum class RobotMood {
    HAPPY,
    CURIOUS,
    DIZZY,
    STRONG_SHIELD,
    SURPRISED,
    COOL,
    TALKING,
    LOVE,
    CONFIDENT
}

/**
 * Interactive Ground & Object-Climbing AI Companion Robot ("Aura Bot")
 * Sees UI elements (Home, Notes, Tasks, Money, Habits) as real physical climbable objects!
 * Does NOT fly — rolls on heavy-duty caterpillar treads, extends articulated mechanical climbing arms,
 * hoists itself onto tabs/cards, perches proudly on elements, hops down, and provides quick actions.
 */
@Composable
fun BoxScope.AuraRobotCompanion(
    isVisible: Boolean,
    activeTab: Section = Section.Dashboard,
    onNavigateTab: (Section) -> Unit = {},
    onQuickNote: () -> Unit,
    onVoiceMemo: () -> Unit,
    onSnapImage: () -> Unit,
    onIncome: () -> Unit,
    onExpense: () -> Unit,
    onStyleFab: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (!isVisible) return

    val density = LocalDensity.current
    val view = LocalView.current
    val scope = rememberCoroutineScope()

    // Screen-relative positions
    var posX by remember { mutableFloatStateOf(0f) }
    var currentElevationPx by remember { mutableFloatStateOf(0f) }
    var targetElevationPx by remember { mutableFloatStateOf(0f) }
    var hasInitializedPos by remember { mutableStateOf(false) }

    // Currently mounted / climbed platform
    var currentPlatform by remember { mutableStateOf<ClimbablePlatform?>(null) }
    var targetedPlatform by remember { mutableStateOf<ClimbablePlatform?>(null) }

    // Robot Character States
    var robotState by remember { mutableStateOf(RobotState.IDLE_STAND) }
    var robotMood by remember { mutableStateOf(RobotMood.HAPPY) }
    var facingDirection by remember { mutableFloatStateOf(1f) } // 1f = right, -1f = left
    var pettingStreak by remember { mutableIntStateOf(0) }
    var lastPetTimestamp by remember { mutableLongStateOf(0L) }

    // Interactive & Climbing Animation Drivers
    val squashStretchX = remember { Animatable(1f) }
    val squashStretchY = remember { Animatable(1f) }
    val wobbleRotation = remember { Animatable(0f) }
    val climbArmProgress = remember { Animatable(0f) } // 0f = resting, 1f = fully reaching/pulling up
    val shieldGlowAlpha = remember { Animatable(0f) }

    // Ground & Perch animations
    val infiniteTransition = rememberInfiniteTransition(label = "robot_anim")
    val idleBobbing by infiniteTransition.animateFloat(
        initialValue = -3f,
        targetValue = 3f,
        animationSpec = infiniteRepeatable(
            animation = tween(1100, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "bobbing"
    )
    val earTwitch by infiniteTransition.animateFloat(
        initialValue = -3.5f,
        targetValue = 5.5f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "ear_twitch"
    )
    val treadRollPhase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(420, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "treads"
    )
    val antennaPulse by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "antenna"
    )
    val neonBreathe by infiniteTransition.animateFloat(
        initialValue = 0.7f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "neon_breathe"
    )

    // Speech Bubble State
    var speechMessage by remember { mutableStateOf<String?>(null) }
    var isSpeechVisible by remember { mutableStateOf(false) }
    var showQuickMenu by remember { mutableStateOf(false) }

    fun triggerSpeech(text: String, durationMs: Long = 4000L) {
        speechMessage = text
        isSpeechVisible = true
        AuraHaptics.triggerSubtleTick(view)
        scope.launch {
            delay(durationMs)
            if (speechMessage == text) {
                isSpeechVisible = false
            }
        }
    }

    // Define all tangible UI objects / platforms that the robot can see and climb
    val climbablePlatforms = remember {
        listOf(
            ClimbablePlatform("home", "Home Base", Section.Dashboard, "🏠", 0.12f, 44f, "Climbed onto Home! 🏠 Look at this dashboard!"),
            ClimbablePlatform("notes", "Notes Vault", Section.Notes, "📝", 0.31f, 44f, "Perched atop Notes! 📝 Ready to jot ideas!"),
            ClimbablePlatform("tasks", "Tasks Tower", Section.Tasks, "🛡️", 0.50f, 44f, "Standing tall on Tasks! 🛡️ Let's conquer goals!"),
            ClimbablePlatform("money", "Money Vault", Section.Money, "💰", 0.69f, 44f, "Climbed onto Money! 💰 Guarding your stash!"),
            ClimbablePlatform("habits", "Habit Matrix", Section.Day, "⚡", 0.88f, 44f, "Scaled the Habit Matrix! ⚡ Daily streak boost!"),
            ClimbablePlatform("deck", "Upper Deck", null, "🎴", 0.50f, 98f, "Scaled up to the Upper Deck! 🌟 What a view!")
        )
    }

    // Function to initiate climbing onto a specific platform
    fun climbToPlatform(platform: ClimbablePlatform, screenWidthPx: Float, robotWidthPx: Float, groundElevationPx: Float) {
        scope.launch {
            val targetX = (platform.relativeX * screenWidthPx - robotWidthPx / 2f).coerceIn(12f, screenWidthPx - robotWidthPx - 12f)
            val elevPx = with(density) { platform.elevationDp.dp.toPx() }

            // 1. Roll to the base of the object
            facingDirection = if (targetX > posX) 1f else -1f
            robotState = RobotState.WALKING
            robotMood = RobotMood.CURIOUS
            val startX = posX
            val dist = abs(targetX - startX)
            val walkDuration = (dist * 3.2f).coerceIn(400f, 1500f).toLong()
            val startTime = System.currentTimeMillis()

            while (System.currentTimeMillis() - startTime < walkDuration && robotState == RobotState.WALKING) {
                val progress = (System.currentTimeMillis() - startTime).toFloat() / walkDuration
                posX = startX + (targetX - startX) * progress
                delay(16)
            }
            posX = targetX

            // 2. Extend mechanical arms & hoist body up (Climbing physics)
            robotState = RobotState.CLIMBING_UP
            robotMood = RobotMood.STRONG_SHIELD
            AuraHaptics.triggerConfirm(view)

            // Arms reach up
            climbArmProgress.animateTo(1f, tween(320, easing = FastOutSlowInEasing))
            squashStretchY.animateTo(1.25f, tween(200))
            squashStretchX.animateTo(0.85f, tween(200))

            // Hoist body up to the object elevation
            val startElev = currentElevationPx
            val climbElevDuration = 450L
            val climbStart = System.currentTimeMillis()
            while (System.currentTimeMillis() - climbStart < climbElevDuration) {
                val p = (System.currentTimeMillis() - climbStart).toFloat() / climbElevDuration
                currentElevationPx = startElev + (elevPx - startElev) * p
                delay(16)
            }
            currentElevationPx = elevPx
            targetElevationPx = elevPx

            // Retract arms to rest on object ledge
            climbArmProgress.animateTo(0f, tween(200))
            squashStretchY.animateTo(0.9f, tween(100))
            squashStretchX.animateTo(1.15f, tween(100))
            squashStretchY.animateTo(1f, spring(dampingRatio = 0.6f))
            squashStretchX.animateTo(1f, spring(dampingRatio = 0.6f))

            // 3. Perched victory pose
            currentPlatform = platform
            robotState = RobotState.PERCHED_ON_OBJECT
            robotMood = RobotMood.CONFIDENT
            AuraHaptics.triggerConfirm(view)
            triggerSpeech(platform.climbHint, 4000)
        }
    }

    // Function to hop down from an object back to ground
    fun hopDownToGround(groundElevationPx: Float) {
        scope.launch {
            robotState = RobotState.HOPPING_DOWN
            robotMood = RobotMood.SURPRISED
            AuraHaptics.triggerSlideFeedback(view)

            // Hop arc down
            val startElev = currentElevationPx
            val hopDuration = 350L
            val hopStart = System.currentTimeMillis()
            while (System.currentTimeMillis() - hopStart < hopDuration) {
                val p = (System.currentTimeMillis() - hopStart).toFloat() / hopDuration
                currentElevationPx = startElev * (1f - p)
                delay(16)
            }
            currentElevationPx = 0f
            targetElevationPx = 0f
            currentPlatform = null

            // Impact squash & settle
            squashStretchY.animateTo(0.75f, tween(80))
            squashStretchX.animateTo(1.25f, tween(80))
            squashStretchY.animateTo(1f, spring(dampingRatio = 0.6f))
            squashStretchX.animateTo(1f, spring(dampingRatio = 0.6f))

            robotState = RobotState.IDLE_STAND
            robotMood = RobotMood.HAPPY
            triggerSpeech("Hopped back down to ground floor! 🚗💨", 2500)
        }
    }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .zIndex(100f)
    ) {
        val screenWidthPx = with(density) { maxWidth.toPx() }
        val screenHeightPx = with(density) { maxHeight.toPx() }
        val robotWidthPx = with(density) { 104.dp.toPx() }
        val robotHeightPx = with(density) { 110.dp.toPx() }
        val groundBottomMarginPx = with(density) { 72.dp.toPx() }
        val groundPosY = (screenHeightPx - groundBottomMarginPx - robotHeightPx).coerceAtLeast(50f)

        // Initialize position on ground
        LaunchedEffect(screenWidthPx, groundPosY) {
            if (!hasInitializedPos && screenWidthPx > 0f && groundPosY > 0f) {
                posX = (screenWidthPx - robotWidthPx - with(density) { 16.dp.toPx() }).coerceIn(12f, screenWidthPx - robotWidthPx - 12f)
                hasInitializedPos = true
                delay(500)
                triggerSpeech("Aura Rover online! Tap any tab to watch me climb! 🐾⚡", 4800)
            }
        }

        // Screen & Tab Reactive Personality System
        LaunchedEffect(activeTab) {
            if (!hasInitializedPos) return@LaunchedEffect
            AuraHaptics.triggerSubtleTick(view)
            when (activeTab) {
                Section.Dashboard -> {
                    robotMood = RobotMood.HAPPY
                    triggerSpeech("Headquarters overview! 📊 Everything at a glance!", 3500)
                }
                Section.Notes -> {
                    robotMood = RobotMood.CURIOUS
                    triggerSpeech("Offline Notebook! 📝 Ready for thoughts & sketches!", 3500)
                }
                Section.Tasks -> {
                    robotMood = RobotMood.STRONG_SHIELD
                    triggerSpeech("Objectives Tower! 🛡️ Let's crush those tasks!", 3500)
                }
                Section.Money -> {
                    robotMood = RobotMood.COOL
                    triggerSpeech("Fintech Vault! 💰 Budgets and splits under control!", 3500)
                }
                Section.Habits, Section.Day -> {
                    robotMood = RobotMood.CONFIDENT
                    triggerSpeech("Habit Matrix & Day Flow! ⚡ Streaks build masters!", 3500)
                }
                Section.SecuritySettings -> {
                    robotMood = RobotMood.STRONG_SHIELD
                    triggerSpeech("Security & Sync Vault! 🔐 Maximum protection!", 3500)
                }
                else -> {}
            }
        }

        // Autonomous Climbing & Ground Patrol Loop
        LaunchedEffect(hasInitializedPos) {
            if (!hasInitializedPos) return@LaunchedEffect
            while (isActive) {
                if (robotState == RobotState.DRAGGED || robotState == RobotState.PUSHED_STUMBLE || robotState == RobotState.SUPER_STRONG || robotState == RobotState.CLIMBING_UP || robotState == RobotState.HOPPING_DOWN) {
                    delay(500)
                    continue
                }

                // If currently perched on an object, chill for a while then either hop down or look around
                if (robotState == RobotState.PERCHED_ON_OBJECT) {
                    delay(Random.nextLong(4000, 7500))
                    if (Random.nextInt(100) < 45) {
                        hopDownToGround(0f)
                        delay(1000)
                    } else {
                        robotMood = if (Random.nextBoolean()) RobotMood.LOVE else RobotMood.COOL
                    }
                    continue
                }

                // On Ground: Either wander, talk, or pick a climbable tab to conquer!
                val roll = Random.nextInt(100)
                if (roll < 35) {
                    // Autonomous Decision: Pick a random UI tab object to climb!
                    val tabToClimb = climbablePlatforms.random()
                    climbToPlatform(tabToClimb, screenWidthPx, robotWidthPx, 0f)
                    delay(5000)
                } else if (roll < 65 && !isSpeechVisible && !showQuickMenu) {
                    // Friendly conversational line
                    robotMood = RobotMood.TALKING
                    triggerSpeech(
                        listOf(
                            "I can climb Home, Notes, Tasks & Habits! 🐾🧗",
                            "Look at my mechanical climbing arms! 🦾⚡",
                            "Drag me over any tab to climb onto it! ⛰️",
                            "Guarding the interface ground line! 🛡️🚗",
                            "Double-tap me to activate Power Shield! 💪⚡",
                            "Tappin' on tasks makes them shiny! ✨"
                        ).random(),
                        3800
                    )
                    delay(4000)
                    robotMood = RobotMood.HAPPY
                } else {
                    // Ground Patrol Rover: rolls left/right on treads
                    val walkDir = if (posX > screenWidthPx * 0.65f) -1f else if (posX < screenWidthPx * 0.25f) 1f else if (Random.nextBoolean()) 1f else -1f
                    facingDirection = walkDir
                    robotState = RobotState.WALKING
                    robotMood = if (Random.nextBoolean()) RobotMood.HAPPY else RobotMood.COOL

                    val rollDuration = Random.nextLong(2000, 3600)
                    val startTime = System.currentTimeMillis()
                    val rollSpeed = with(density) { 40.dp.toPx() }

                    while (System.currentTimeMillis() - startTime < rollDuration && robotState == RobotState.WALKING) {
                        val dt = 0.016f
                        posX = (posX + walkDir * rollSpeed * dt).coerceIn(12f, screenWidthPx - robotWidthPx - 12f)
                        delay(16)
                    }

                    robotState = RobotState.IDLE_STAND
                    robotMood = RobotMood.CURIOUS
                    delay(Random.nextLong(2000, 4500))
                    robotMood = RobotMood.HAPPY
                }
            }
        }

        // Render Ground Position with current object elevation & idle bobbing
        val currentBob = if (robotState == RobotState.IDLE_STAND || robotState == RobotState.WALKING || robotState == RobotState.PERCHED_ON_OBJECT) idleBobbing else 0f
        val renderX = posX
        val renderY = groundPosY - currentElevationPx + currentBob

        // Primary Robot Container
        Box(
            modifier = Modifier
                .offset { IntOffset(renderX.roundToInt(), renderY.roundToInt()) }
                .size(width = 108.dp, height = 114.dp)
                .testTag("aura_robot_companion")
        ) {
            // Speed Dial Quick Menu Overlay (anchored directly above robot head)
            AnimatedVisibility(
                visible = showQuickMenu,
                enter = scaleIn(transformOrigin = androidx.compose.ui.graphics.TransformOrigin(0.5f, 1f), initialScale = 0.6f) + fadeIn(),
                exit = scaleOut(transformOrigin = androidx.compose.ui.graphics.TransformOrigin(0.5f, 1f), targetScale = 0.6f) + fadeOut(),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .offset(y = (-118).dp)
            ) {
                RobotQuickActionsMenu(
                    onDismiss = { showQuickMenu = false },
                    onQuickNote = {
                        showQuickMenu = false
                        onQuickNote()
                    },
                    onVoiceMemo = {
                        showQuickMenu = false
                        onVoiceMemo()
                    },
                    onSnapImage = {
                        showQuickMenu = false
                        onSnapImage()
                    },
                    onIncome = {
                        showQuickMenu = false
                        onIncome()
                    },
                    onExpense = {
                        showQuickMenu = false
                        onExpense()
                    },
                    onStyleFab = {
                        showQuickMenu = false
                        onStyleFab()
                    }
                )
            }

            // Speech Bubble Popup (anchored above head)
            AnimatedVisibility(
                visible = isSpeechVisible && speechMessage != null && !showQuickMenu,
                enter = scaleIn(transformOrigin = androidx.compose.ui.graphics.TransformOrigin(0.5f, 1f), initialScale = 0.5f) + fadeIn(),
                exit = scaleOut(transformOrigin = androidx.compose.ui.graphics.TransformOrigin(0.5f, 1f), targetScale = 0.5f) + fadeOut(),
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .offset(y = (-78).dp)
                    .widthIn(max = 240.dp)
                    .clickable {
                        // If perched on a section, clicking bubble navigates to it!
                        if (currentPlatform?.section != null) {
                            onNavigateTab(currentPlatform!!.section!!)
                            AuraHaptics.triggerConfirm(view)
                        } else {
                            showQuickMenu = true
                            isSpeechVisible = false
                            AuraHaptics.triggerSelection(view)
                        }
                    }
            ) {
                Surface(
                    color = AuraTheme.colors.cardBackground.copy(alpha = 0.98f),
                    shape = RoundedCornerShape(18.dp),
                    border = BorderStroke(1.5.dp, AuraTheme.colors.accentBrand.copy(alpha = 0.7f)),
                    shadowElevation = 14.dp,
                    modifier = Modifier.padding(horizontal = 4.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = speechMessage ?: "",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = AuraTheme.colors.textPrimary,
                            textAlign = TextAlign.Center,
                            lineHeight = 15.sp
                        )
                        Text(
                            text = if (currentPlatform != null) "Perched on ${currentPlatform?.name} • Tap to enter" else "Drag to climb tabs • Tap to poke",
                            fontSize = 8.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = AuraTheme.colors.accentBrand,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(top = 3.dp)
                        )
                    }
                }
            }

            // Holographic platform badge floating under robot when perched
            if (currentPlatform != null && robotState == RobotState.PERCHED_ON_OBJECT) {
                Surface(
                    color = AuraTheme.colors.accentBrand.copy(alpha = 0.18f),
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, AuraTheme.colors.accentBrand.copy(alpha = 0.5f)),
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .offset(y = 18.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "${currentPlatform?.emoji} ${currentPlatform?.name}",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Black,
                            color = AuraTheme.colors.accentBrand
                        )
                    }
                }
            }

            // High-Tech Sturdy Shield Halo Ring when in SUPER_STRONG power mode
            if (robotState == RobotState.SUPER_STRONG || shieldGlowAlpha.value > 0.05f) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .scale(1.3f)
                        .border(
                            width = 3.dp,
                            brush = Brush.sweepGradient(
                                listOf(
                                    AuraTheme.colors.accentBrand,
                                    AuraTheme.colors.gold,
                                    Color(0xFF00E5FF),
                                    AuraTheme.colors.accentBrand
                                )
                            ),
                            shape = CircleShape
                        )
                )
            }

            // Interactive Gestures: Tap (Poke / Hop / Stumble), Double-Tap (Shield Overdrive), Drag (Snap & Climb)
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        scaleX = squashStretchX.value * facingDirection
                        scaleY = squashStretchY.value
                        rotationZ = wobbleRotation.value
                    }
                    .pointerInput(Unit) {
                        detectTapGestures(
                            onTap = {
                                AuraHaptics.triggerConfirm(view)
                                val now = System.currentTimeMillis()
                                if (now - lastPetTimestamp < 650L) {
                                    pettingStreak++
                                } else {
                                    pettingStreak = 1
                                }
                                lastPetTimestamp = now

                                if (pettingStreak >= 3) {
                                    // PETTING ACTIVATION: Heart Eyes, Joy Jump & Purr!
                                    robotMood = RobotMood.LOVE
                                    triggerSpeech("Aww! I love exploring Aura with you! ❤️🐾✨", 3200)
                                    scope.launch {
                                        squashStretchY.animateTo(1.3f, tween(120))
                                        squashStretchX.animateTo(0.85f, tween(120))
                                        squashStretchY.animateTo(0.9f, tween(100))
                                        squashStretchX.animateTo(1.15f, tween(100))
                                        squashStretchY.animateTo(1f, spring(dampingRatio = 0.5f))
                                        squashStretchX.animateTo(1f, spring(dampingRatio = 0.5f))
                                        delay(2500)
                                        if (robotMood == RobotMood.LOVE) {
                                            robotMood = RobotMood.HAPPY
                                        }
                                    }
                                } else if (robotState == RobotState.PERCHED_ON_OBJECT) {
                                    // When tapped while perched: prompt action or hop down
                                    if (currentPlatform?.section != null) {
                                        onNavigateTab(currentPlatform!!.section!!)
                                        triggerSpeech("Opening ${currentPlatform?.name}! 🚀", 2000)
                                    } else {
                                        hopDownToGround(0f)
                                    }
                                } else {
                                    // On Ground: Poke stumble
                                    robotState = RobotState.PUSHED_STUMBLE
                                    robotMood = RobotMood.DIZZY
                                    val pushDir = if (facingDirection > 0) -1f else 1f
                                    posX = (posX + pushDir * 35f).coerceIn(12f, screenWidthPx - robotWidthPx - 12f)

                                    triggerSpeech(
                                        listOf(
                                            "Hey! Claws fully locked! 🦾✨",
                                            "Whoa! Ground drift engaged! 🌀",
                                            "Hehe! Tap a tab to watch me climb it! 🧗",
                                            "Super sturdy! Nothing knocks me down! ⚡",
                                            "Tap rapidly to pet me! 🐾❤️"
                                        ).random(),
                                        2500
                                    )

                                    scope.launch {
                                        wobbleRotation.animateTo(pushDir * 18f, tween(90, easing = FastOutSlowInEasing))
                                        wobbleRotation.animateTo(-pushDir * 12f, tween(110, easing = FastOutSlowInEasing))
                                        wobbleRotation.animateTo(pushDir * 6f, tween(90, easing = FastOutSlowInEasing))
                                        wobbleRotation.animateTo(0f, spring(dampingRatio = 0.5f, stiffness = Spring.StiffnessLow))
                                        delay(350)
                                        if (robotState == RobotState.PUSHED_STUMBLE) {
                                            robotMood = RobotMood.HAPPY
                                            robotState = RobotState.IDLE_STAND
                                        }
                                    }
                                }
                            },
                            onDoubleTap = {
                                // DOUBLE TAP: SUPER STURDY OVERDRIVE SHIELD!
                                AuraHaptics.triggerConfirm(view)
                                robotState = RobotState.SUPER_STRONG
                                robotMood = RobotMood.STRONG_SHIELD
                                triggerSpeech("Sturdy Shield Overdrive Activated! 🛡️⚡💪", 3200)

                                scope.launch {
                                    shieldGlowAlpha.animateTo(1f, tween(120))
                                    squashStretchY.animateTo(0.88f, tween(100))
                                    squashStretchX.animateTo(1.18f, tween(100))
                                    delay(1800)
                                    squashStretchY.animateTo(1f, spring(dampingRatio = 0.6f))
                                    squashStretchX.animateTo(1f, spring(dampingRatio = 0.6f))
                                    shieldGlowAlpha.animateTo(0f, tween(350))
                                    robotState = if (currentPlatform != null) RobotState.PERCHED_ON_OBJECT else RobotState.IDLE_STAND
                                    robotMood = RobotMood.COOL
                                    delay(1000)
                                    robotMood = RobotMood.HAPPY
                                }
                            },
                            onLongPress = {
                                // LONG PRESS: Launch Quick Actions
                                AuraHaptics.triggerConfirm(view)
                                showQuickMenu = !showQuickMenu
                                triggerSpeech("Aura Quick Actions Ready! 🚀", 2000)
                            }
                        )
                    }
                    .pointerInput(Unit) {
                        detectDragGestures(
                            onDragStart = {
                                robotState = RobotState.DRAGGED
                                robotMood = RobotMood.SURPRISED
                                AuraHaptics.triggerSlideFeedback(view)
                            },
                            onDragEnd = {
                                // Release: Snap and climb onto nearest UI element/tab or ground!
                                val nearestPlatform = climbablePlatforms.minByOrNull { p ->
                                    val platX = p.relativeX * screenWidthPx
                                    val curCenterX = posX + robotWidthPx / 2f
                                    abs(platX - curCenterX)
                                }

                                if (nearestPlatform != null && currentElevationPx > 20f) {
                                    // Climbed and locked onto that platform!
                                    climbToPlatform(nearestPlatform, screenWidthPx, robotWidthPx, 0f)
                                } else {
                                    // Settle on ground
                                    hopDownToGround(0f)
                                }
                            },
                            onDragCancel = {
                                hopDownToGround(0f)
                            },
                            onDrag = { _, dragAmount ->
                                posX = (posX + dragAmount.x).coerceIn(10f, screenWidthPx - robotWidthPx - 10f)
                                currentElevationPx = (currentElevationPx - dragAmount.y).coerceIn(0f, with(density) { 140.dp.toPx() })
                                facingDirection = if (dragAmount.x < -1f) -1f else if (dragAmount.x > 1f) 1f else facingDirection
                            }
                        )
                    }
            ) {
                // Vector Canvas Rendering of the Cute & Cool Ground & Climbing Mech Rover
                val brandAccent = AuraTheme.colors.accentBrand
                val badgeGold = AuraTheme.colors.badgeGold
                val cardBg = AuraTheme.colors.cardBackground
                val textWhite = AuraTheme.colors.textPrimary

                Canvas(modifier = Modifier.fillMaxSize()) {
                    drawSuperCoolClimbingRobot(
                        state = robotState,
                        mood = robotMood,
                        brandAccent = brandAccent,
                        goldAccent = badgeGold,
                        surfaceColor = cardBg,
                        textColor = textWhite,
                        treadRollPhase = treadRollPhase,
                        climbArmProgress = climbArmProgress.value,
                        antennaPulse = antennaPulse,
                        earTwitch = earTwitch,
                        neonBreathe = neonBreathe,
                        isPerched = currentPlatform != null
                    )
                }
            }
        }
    }
}

/**
 * Procedural Vector Canvas Renderer for the Ground Rover & UI Object Climber
 */
private fun DrawScope.drawSuperCoolClimbingRobot(
    state: RobotState,
    mood: RobotMood,
    brandAccent: Color,
    goldAccent: Color,
    surfaceColor: Color,
    textColor: Color,
    treadRollPhase: Float,
    climbArmProgress: Float, // 0f = resting, 1f = reaching/pulling up
    antennaPulse: Float,
    earTwitch: Float,
    neonBreathe: Float,
    isPerched: Boolean
) {
    val canvasW = size.width
    val canvasH = size.height
    val centerX = canvasW / 2f

    // -------------------------------------------------------------
    // 1. GROUND CONTACT / PLATFORM CONTACT SHADOW & MAGNETIC CLAMPS
    // -------------------------------------------------------------
    drawOval(
        color = if (isPerched) brandAccent.copy(alpha = 0.35f * neonBreathe) else Color.Black.copy(alpha = 0.45f),
        topLeft = Offset(centerX - 36f, canvasH - 12f),
        size = Size(72f, 10f)
    )

    // -------------------------------------------------------------
    // 2. HEAVY-DUTY GROUND CATERPILLAR TREADS & CLIMBING GRIPPERS
    // -------------------------------------------------------------
    val treadW = 76f
    val treadH = 18f
    val treadTop = canvasH - 24f
    val treadLeft = centerX - (treadW / 2f)

    // Outer Rubber Tread Track (Dark Steel with Chamfer)
    drawRoundRect(
        color = Color(0xFF1B1D24),
        topLeft = Offset(treadLeft, treadTop),
        size = Size(treadW, treadH),
        cornerRadius = CornerRadius(9f, 9f)
    )
    drawRoundRect(
        color = if (isPerched) brandAccent.copy(alpha = 0.85f) else Color(0xFF2E3340),
        topLeft = Offset(treadLeft, treadTop),
        size = Size(treadW, treadH),
        cornerRadius = CornerRadius(9f, 9f),
        style = Stroke(width = 2.5f)
    )

    // Rotating Cog Wheels inside Tread Track
    val wheelRadius = 5.5f
    val wheelPositions = listOf(
        treadLeft + 12f,
        treadLeft + 28f,
        treadLeft + 48f,
        treadLeft + 64f
    )
    val treadAngleRad = Math.toRadians(treadRollPhase.toDouble())

    wheelPositions.forEach { wx ->
        val wy = treadTop + (treadH / 2f)
        drawCircle(
            color = Color(0xFF353A47),
            radius = wheelRadius,
            center = Offset(wx, wy)
        )
        drawCircle(
            color = if (mood == RobotMood.STRONG_SHIELD) goldAccent else brandAccent.copy(alpha = 0.8f),
            radius = 2.2f,
            center = Offset(wx, wy)
        )
        val spokeX = wx + cos(treadAngleRad).toFloat() * 3.5f
        val spokeY = wy + sin(treadAngleRad).toFloat() * 3.5f
        drawLine(
            color = Color.White.copy(alpha = 0.7f),
            start = Offset(wx, wy),
            end = Offset(spokeX, spokeY),
            strokeWidth = 1.8f,
            cap = StrokeCap.Round
        )
    }

    // Tread Grippers / Magnetic Locking Studs
    for (i in 0..7) {
        val toothBase = (treadLeft + 6f + (i * 9f) + (if (state == RobotState.WALKING) (treadRollPhase / 40f) % 9f else 0f))
        if (toothBase in (treadLeft + 4f)..(treadLeft + treadW - 6f)) {
            drawLine(
                color = if (isPerched) brandAccent else brandAccent.copy(alpha = 0.5f),
                start = Offset(toothBase, treadTop - 1f),
                end = Offset(toothBase, treadTop + 2.5f),
                strokeWidth = 2f,
                cap = StrokeCap.Round
            )
        }
    }

    // -------------------------------------------------------------
    // 3. ARTICULATED CYBER CLIMBING ARMS & MAGNETIC CLAWS (Reaching & Gripping!)
    // -------------------------------------------------------------
    val shoulderY = 46f
    val leftShoulderX = centerX - 36f
    val rightShoulderX = centerX + 36f

    // Arm Reach calculation based on climb progress
    val armAngleL = if (state == RobotState.CLIMBING_UP) -110f * climbArmProgress else if (isPerched) -40f else 25f
    val armAngleR = if (state == RobotState.CLIMBING_UP) -110f * climbArmProgress else if (isPerched) -40f else 25f

    // Draw Left Arm & Claws
    val armLength = 22f
    val leftHandX = leftShoulderX + cos(Math.toRadians(armAngleL.toDouble())).toFloat() * armLength
    val leftHandY = shoulderY + sin(Math.toRadians(armAngleL.toDouble())).toFloat() * armLength

    drawLine(
        color = Color(0xFF353A47),
        start = Offset(leftShoulderX, shoulderY),
        end = Offset(leftHandX, leftHandY),
        strokeWidth = 4.5f,
        cap = StrokeCap.Round
    )
    // Left Shoulder Joint
    drawCircle(
        color = brandAccent,
        radius = 3.5f,
        center = Offset(leftShoulderX, shoulderY)
    )
    // Left Magnetic Claw / Gripper Pad
    drawCircle(
        color = if (state == RobotState.CLIMBING_UP || isPerched) Color(0xFF00E5FF) else brandAccent,
        radius = 4f,
        center = Offset(leftHandX, leftHandY)
    )

    // Draw Right Arm & Claws
    val rightHandX = rightShoulderX + cos(Math.toRadians((180 - armAngleR).toDouble())).toFloat() * armLength
    val rightHandY = shoulderY + sin(Math.toRadians((180 - armAngleR).toDouble())).toFloat() * armLength

    drawLine(
        color = Color(0xFF353A47),
        start = Offset(rightShoulderX, shoulderY),
        end = Offset(rightHandX, rightHandY),
        strokeWidth = 4.5f,
        cap = StrokeCap.Round
    )
    // Right Shoulder Joint
    drawCircle(
        color = brandAccent,
        radius = 3.5f,
        center = Offset(rightShoulderX, shoulderY)
    )
    // Right Magnetic Claw / Gripper Pad
    drawCircle(
        color = if (state == RobotState.CLIMBING_UP || isPerched) Color(0xFF00E5FF) else brandAccent,
        radius = 4f,
        center = Offset(rightHandX, rightHandY)
    )

    // -------------------------------------------------------------
    // 4. CYBER MECH CAT-EARS / ANTENNA SENSORS (Ultra Cool & Cute!)
    // -------------------------------------------------------------
    val earBaseY = 24f
    val earLeftBaseX = centerX - 24f
    val earRightBaseX = centerX + 24f
    val earOffset = if (state == RobotState.WALKING) earTwitch else 0f

    // Left Cyber Ear
    val leftEarPath = Path().apply {
        moveTo(earLeftBaseX - 8f, earBaseY + 6f)
        lineTo(earLeftBaseX - 14f - earOffset, earBaseY - 14f)
        lineTo(earLeftBaseX + 2f, earBaseY)
        close()
    }
    drawPath(path = leftEarPath, color = Color(0xFF242731))
    drawPath(path = leftEarPath, color = brandAccent.copy(alpha = 0.8f), style = Stroke(width = 2f))
    val leftEarInner = Path().apply {
        moveTo(earLeftBaseX - 7f, earBaseY + 4f)
        lineTo(earLeftBaseX - 11f - earOffset, earBaseY - 8f)
        lineTo(earLeftBaseX, earBaseY + 1f)
        close()
    }
    drawPath(path = leftEarInner, color = if (mood == RobotMood.STRONG_SHIELD) goldAccent else brandAccent.copy(alpha = 0.85f * neonBreathe))

    // Right Cyber Ear
    val rightEarPath = Path().apply {
        moveTo(earRightBaseX + 8f, earBaseY + 6f)
        lineTo(earRightBaseX + 14f + earOffset, earBaseY - 14f)
        lineTo(earRightBaseX - 2f, earBaseY)
        close()
    }
    drawPath(path = rightEarPath, color = Color(0xFF242731))
    drawPath(path = rightEarPath, color = brandAccent.copy(alpha = 0.8f), style = Stroke(width = 2f))
    val rightEarInner = Path().apply {
        moveTo(earRightBaseX + 7f, earBaseY + 4f)
        lineTo(earRightBaseX + 11f + earOffset, earBaseY - 8f)
        lineTo(earRightBaseX, earBaseY + 1f)
        close()
    }
    drawPath(path = rightEarInner, color = if (mood == RobotMood.STRONG_SHIELD) goldAccent else brandAccent.copy(alpha = 0.85f * neonBreathe))

    // Center Mini Beacon Antenna with Pulsing Hologram Orb
    drawLine(
        color = Color(0xFF717786),
        start = Offset(centerX, 20f),
        end = Offset(centerX, 8f),
        strokeWidth = 3.5f,
        cap = StrokeCap.Round
    )
    drawCircle(
        color = brandAccent.copy(alpha = 0.4f * antennaPulse),
        radius = 11f * antennaPulse,
        center = Offset(centerX, 7f)
    )
    drawCircle(
        color = if (mood == RobotMood.STRONG_SHIELD) goldAccent else brandAccent,
        radius = 6f,
        center = Offset(centerX, 7f)
    )
    drawCircle(
        color = Color.White,
        radius = 2.2f,
        center = Offset(centerX - 1.5f, 5.5f)
    )

    // -------------------------------------------------------------
    // 5. MAIN CYBER ROBOT HEAD & CHASSIS BODY (Bigger & Rounded)
    // -------------------------------------------------------------
    val bodyW = 74f
    val bodyH = 58f
    val bodyTop = 18f
    val bodyLeft = centerX - (bodyW / 2f)

    val bodyBrush = Brush.verticalGradient(
        listOf(
            Color(0xFF343946),
            Color(0xFF222630),
            Color(0xFF191B22)
        )
    )
    drawRoundRect(
        brush = bodyBrush,
        topLeft = Offset(bodyLeft, bodyTop),
        size = Size(bodyW, bodyH),
        cornerRadius = CornerRadius(22f, 22f)
    )
    drawRoundRect(
        color = if (mood == RobotMood.STRONG_SHIELD) goldAccent else brandAccent.copy(alpha = 0.85f),
        topLeft = Offset(bodyLeft, bodyTop),
        size = Size(bodyW, bodyH),
        cornerRadius = CornerRadius(22f, 22f),
        style = Stroke(width = 3f)
    )

    // -------------------------------------------------------------
    // 6. GLOSSY OLED CYBER VISOR (Curved Screen Face)
    // -------------------------------------------------------------
    val visorW = bodyW - 14f
    val visorH = 30f
    val visorTop = bodyTop + 8f
    val visorLeft = centerX - (visorW / 2f)

    drawRoundRect(
        color = Color(0xFF090A0E),
        topLeft = Offset(visorLeft, visorTop),
        size = Size(visorW, visorH),
        cornerRadius = CornerRadius(14f, 14f)
    )
    drawLine(
        color = Color.White.copy(alpha = 0.22f),
        start = Offset(visorLeft + 6f, visorTop + 4f),
        end = Offset(visorLeft + visorW - 10f, visorTop + 4f),
        strokeWidth = 2f,
        cap = StrokeCap.Round
    )

    // -------------------------------------------------------------
    // 7. EXPRESSIVE CYBER EYES & CUTE FACIAL EMOTIONS
    // -------------------------------------------------------------
    val eyeColor = if (mood == RobotMood.STRONG_SHIELD) goldAccent else if (mood == RobotMood.COOL) Color(0xFF00E5FF) else brandAccent
    val eyeY = visorTop + (visorH / 2f) - 0.5f
    val eyeSpacing = 14f
    val leftEyeX = centerX - eyeSpacing
    val rightEyeX = centerX + eyeSpacing

    when (mood) {
        RobotMood.HAPPY -> {
            drawArc(
                color = eyeColor,
                startAngle = 180f,
                sweepAngle = 180f,
                useCenter = false,
                topLeft = Offset(leftEyeX - 6f, eyeY - 6f),
                size = Size(12f, 11f),
                style = Stroke(width = 3.5f, cap = StrokeCap.Round)
            )
            drawArc(
                color = eyeColor,
                startAngle = 180f,
                sweepAngle = 180f,
                useCenter = false,
                topLeft = Offset(rightEyeX - 6f, eyeY - 6f),
                size = Size(12f, 11f),
                style = Stroke(width = 3.5f, cap = StrokeCap.Round)
            )
            // Cute cat-mouth :3
            drawArc(
                color = eyeColor.copy(alpha = 0.9f),
                startAngle = 0f,
                sweepAngle = 180f,
                useCenter = false,
                topLeft = Offset(centerX - 4.5f, eyeY + 3f),
                size = Size(9f, 6f),
                style = Stroke(width = 2.2f, cap = StrokeCap.Round)
            )
            // Rosy Pink Blush on Cheeks (Ultra Cute!)
            drawCircle(
                color = Color(0xFFFF4081).copy(alpha = 0.45f * neonBreathe),
                radius = 3.5f,
                center = Offset(leftEyeX - 8f, eyeY + 4f)
            )
            drawCircle(
                color = Color(0xFFFF4081).copy(alpha = 0.45f * neonBreathe),
                radius = 3.5f,
                center = Offset(rightEyeX + 8f, eyeY + 4f)
            )
        }
        RobotMood.LOVE -> {
            // Heart-shaped glowing cyber eyes
            drawCircle(color = Color(0xFFFF4081), radius = 4f, center = Offset(leftEyeX - 2.5f, eyeY - 2f))
            drawCircle(color = Color(0xFFFF4081), radius = 4f, center = Offset(leftEyeX + 2.5f, eyeY - 2f))
            drawCircle(color = Color(0xFFFF4081), radius = 4f, center = Offset(rightEyeX - 2.5f, eyeY - 2f))
            drawCircle(color = Color(0xFFFF4081), radius = 4f, center = Offset(rightEyeX + 2.5f, eyeY - 2f))
            // Sweet smile
            drawArc(
                color = Color(0xFFFF4081),
                startAngle = 0f,
                sweepAngle = 180f,
                useCenter = false,
                topLeft = Offset(centerX - 4f, eyeY + 3f),
                size = Size(8f, 5f),
                style = Stroke(width = 2.2f, cap = StrokeCap.Round)
            )
        }
        RobotMood.COOL -> {
            // High-tech Cyberpunk Sunglasses Visor
            drawRoundRect(
                color = Color(0xFF00E5FF),
                topLeft = Offset(leftEyeX - 7f, eyeY - 5f),
                size = Size(28f, 10f),
                cornerRadius = CornerRadius(3f, 3f)
            )
            drawLine(
                color = Color.White,
                start = Offset(leftEyeX - 5f, eyeY - 3f),
                end = Offset(rightEyeX + 5f, eyeY - 3f),
                strokeWidth = 1.5f
            )
            // Confident smirk
            drawLine(
                color = Color(0xFF00E5FF),
                start = Offset(centerX - 3f, eyeY + 5f),
                end = Offset(centerX + 4f, eyeY + 3.5f),
                strokeWidth = 2.2f,
                cap = StrokeCap.Round
            )
        }
        RobotMood.CONFIDENT -> {
            // Determined wink / star eye
            drawCircle(color = eyeColor, radius = 5.5f, center = Offset(leftEyeX, eyeY))
            drawCircle(color = Color.White, radius = 2f, center = Offset(leftEyeX - 1.5f, eyeY - 1.5f))
            // Right eye cheeky wink
            drawLine(
                color = eyeColor,
                start = Offset(rightEyeX - 5f, eyeY),
                end = Offset(rightEyeX + 5f, eyeY),
                strokeWidth = 3f,
                cap = StrokeCap.Round
            )
            // Big smile
            drawArc(
                color = eyeColor,
                startAngle = 0f,
                sweepAngle = 180f,
                useCenter = false,
                topLeft = Offset(centerX - 4f, eyeY + 2f),
                size = Size(8f, 6f),
                style = Stroke(width = 2.2f, cap = StrokeCap.Round)
            )
        }
        RobotMood.CURIOUS -> {
            drawCircle(color = eyeColor, radius = 6f, center = Offset(leftEyeX, eyeY))
            drawCircle(color = Color.White, radius = 2f, center = Offset(leftEyeX - 2f, eyeY - 2f))
            drawCircle(color = eyeColor, radius = 4f, center = Offset(rightEyeX, eyeY))
            drawCircle(color = Color.White, radius = 1.5f, center = Offset(rightEyeX - 1f, eyeY - 1f))
            drawCircle(color = eyeColor, radius = 2f, center = Offset(centerX, eyeY + 4f))
        }
        RobotMood.SURPRISED -> {
            drawCircle(color = eyeColor, radius = 6.5f, center = Offset(leftEyeX, eyeY))
            drawCircle(color = Color.White, radius = 2.5f, center = Offset(leftEyeX - 1.5f, eyeY - 1.5f))
            drawCircle(color = eyeColor, radius = 6.5f, center = Offset(rightEyeX, eyeY))
            drawCircle(color = Color.White, radius = 2.5f, center = Offset(rightEyeX - 1.5f, eyeY - 1.5f))
            drawCircle(color = eyeColor, radius = 3.5f, center = Offset(centerX, eyeY + 4f))
        }
        RobotMood.DIZZY -> {
            // Crossed dizzy eyes X X
            drawLine(color = Color(0xFFFF5252), start = Offset(leftEyeX - 4f, eyeY - 4f), end = Offset(leftEyeX + 4f, eyeY + 4f), strokeWidth = 3f, cap = StrokeCap.Round)
            drawLine(color = Color(0xFFFF5252), start = Offset(leftEyeX + 4f, eyeY - 4f), end = Offset(leftEyeX - 4f, eyeY + 4f), strokeWidth = 3f, cap = StrokeCap.Round)
            drawLine(color = Color(0xFFFF5252), start = Offset(rightEyeX - 4f, eyeY - 4f), end = Offset(rightEyeX + 4f, eyeY + 4f), strokeWidth = 3f, cap = StrokeCap.Round)
            drawLine(color = Color(0xFFFF5252), start = Offset(rightEyeX + 4f, eyeY - 4f), end = Offset(rightEyeX - 4f, eyeY + 4f), strokeWidth = 3f, cap = StrokeCap.Round)
            // Wobbly mouth ~
            val dizzyMouth = Path().apply {
                moveTo(centerX - 5f, eyeY + 4f)
                quadraticTo(centerX - 2f, eyeY + 2f, centerX, eyeY + 4f)
                quadraticTo(centerX + 2f, eyeY + 6f, centerX + 5f, eyeY + 4f)
            }
            drawPath(path = dizzyMouth, color = Color(0xFFFF5252), style = Stroke(width = 2f, cap = StrokeCap.Round))
        }
        RobotMood.STRONG_SHIELD -> {
            // Resolute anime battle focus eyes > <
            val leftFocus = Path().apply {
                moveTo(leftEyeX - 5f, eyeY - 4f)
                lineTo(leftEyeX + 4f, eyeY)
                lineTo(leftFocusEndX(leftEyeX), eyeY + 4f)
            }
            drawPath(path = leftFocus, color = goldAccent, style = Stroke(width = 3.5f, cap = StrokeCap.Round))
            val rightFocus = Path().apply {
                moveTo(rightEyeX + 5f, eyeY - 4f)
                lineTo(rightEyeX - 4f, eyeY)
                lineTo(rightEyeX + 4f, eyeY + 4f)
            }
            drawPath(path = rightFocus, color = goldAccent, style = Stroke(width = 3.5f, cap = StrokeCap.Round))
        }
        RobotMood.TALKING -> {
            drawArc(color = eyeColor, startAngle = 180f, sweepAngle = 180f, useCenter = false, topLeft = Offset(leftEyeX - 5f, eyeY - 5f), size = Size(10f, 9f), style = Stroke(width = 3f, cap = StrokeCap.Round))
            drawArc(color = eyeColor, startAngle = 180f, sweepAngle = 180f, useCenter = false, topLeft = Offset(rightEyeX - 5f, eyeY - 5f), size = Size(10f, 9f), style = Stroke(width = 3f, cap = StrokeCap.Round))
            drawOval(color = eyeColor, topLeft = Offset(centerX - 3.5f, eyeY + 2f), size = Size(7f, 5f))
        }
    }
}

private fun leftFocusEndX(leftEyeX: Float): Float = leftEyeX - 5f

/**
 * Rich Frosted Quick Actions Dial Menu
 */
@Composable
private fun RobotQuickActionsMenu(
    onDismiss: () -> Unit,
    onQuickNote: () -> Unit,
    onVoiceMemo: () -> Unit,
    onSnapImage: () -> Unit,
    onIncome: () -> Unit,
    onExpense: () -> Unit,
    onStyleFab: () -> Unit
) {
    val view = LocalView.current
    val menuShape = RoundedCornerShape(24.dp)

    Card(
        shape = menuShape,
        colors = CardDefaults.cardColors(containerColor = AuraTheme.colors.cardBackground.copy(alpha = 0.98f)),
        border = BorderStroke(1.dp, AuraTheme.colors.cardBorder),
        modifier = Modifier
            .width(220.dp)
            .shadow(20.dp, menuShape, spotColor = AuraTheme.colors.accentBrand.copy(alpha = 0.35f))
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 2.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "AURA QUICK ACTIONS",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Black,
                    color = AuraTheme.colors.accentBrand,
                    letterSpacing = 1.sp
                )
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.size(20.dp)
                ) {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = "Close Menu",
                        tint = AuraTheme.colors.textMuted,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }

            HorizontalDivider(color = AuraTheme.colors.cardBorder.copy(alpha = 0.6f))

            // 1. Quick Note
            QuickMenuItem(
                icon = Icons.Default.Edit,
                iconColor = AuraTheme.colors.accentBrand,
                label = "Quick Note",
                onClick = {
                    AuraHaptics.triggerConfirm(view)
                    onQuickNote()
                }
            )

            // 2. Audio Memo
            QuickMenuItem(
                icon = Icons.Default.Mic,
                iconColor = AuraTheme.colors.positiveGreen,
                label = "Voice Memo",
                onClick = {
                    AuraHaptics.triggerConfirm(view)
                    onVoiceMemo()
                }
            )

            // 3. Take Image
            QuickMenuItem(
                icon = Icons.Default.PhotoCamera,
                iconColor = Color(0xFF00FF87),
                label = "Snap Image",
                onClick = {
                    AuraHaptics.triggerConfirm(view)
                    onSnapImage()
                }
            )

            // 4. Add Income
            QuickMenuItem(
                icon = Icons.Default.CallReceived,
                iconColor = AuraTheme.colors.positiveGreen,
                label = "Received ₹",
                onClick = {
                    AuraHaptics.triggerConfirm(view)
                    onIncome()
                }
            )

            // 5. Add Expense
            QuickMenuItem(
                icon = Icons.Default.ArrowOutward,
                iconColor = AuraTheme.colors.negativeRed,
                label = "Spent ₹",
                onClick = {
                    AuraHaptics.triggerConfirm(view)
                    onExpense()
                }
            )

            // 6. Style FAB
            QuickMenuItem(
                icon = Icons.Default.AutoAwesome,
                iconColor = AuraTheme.colors.badgeGold,
                label = "Style Companion",
                onClick = {
                    AuraHaptics.triggerConfirm(view)
                    onStyleFab()
                }
            )
        }
    }
}

@Composable
private fun QuickMenuItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconColor: Color,
    label: String,
    onClick: () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .auraSpringPress(
                cornerRadius = 12.dp,
                onClick = onClick
            )
            .padding(horizontal = 8.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Box(
            modifier = Modifier
                .size(30.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(iconColor.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconColor,
                modifier = Modifier.size(16.dp)
            )
        }
        Text(
            text = label,
            color = AuraTheme.colors.textPrimary,
            fontSize = 12.5.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}
