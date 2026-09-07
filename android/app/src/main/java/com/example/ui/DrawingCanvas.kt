package com.example.ui

import android.view.MotionEvent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.pointerInteropFilter
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun DrawingCanvas(
    initialData: String?,
    onSaveDrawing: (String) -> Unit,
    onBack: () -> Unit,
    viewModel: AppViewModel
) {
    // Deserialize strokes
    val loadedStrokes = remember(initialData) {
        viewModel.deserializeDrawing(initialData).toMutableStateList()
    }

    var selectedColorHex by remember { mutableStateOf("#FF5B32") } // Aura Accent Brand Default
    var selectedWidth by remember { mutableStateOf(6f) }
    var isEraserActive by remember { mutableStateOf(false) }
    var isGridActive by remember { mutableStateOf(true) }

    // Track active points drawn in current motion cycle
    val currentPoints = remember { mutableStateListOf<FloatPair>() }
    val redoStrokes = remember { mutableStateListOf<SketchStroke>() }

    val colorsMap = listOf(
        "#FF5B32" to AuraTheme.colors.accentBrand,
        "#00D084" to AuraTheme.colors.positiveGreen,
        "#FF4D4D" to AuraTheme.colors.negativeRed,
        "#FFB800" to AuraTheme.colors.gold,
        "#7C4DFF" to Color(0xFF7C4DFF),
        "#FFFFFF" to Color.White
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AuraTheme.colors.screenBackground)
    ) {
        // Control Bar Header
        Surface(
            color = AuraTheme.colors.cardBackground,
            modifier = Modifier.fillMaxWidth(),
            tonalElevation = 4.dp
        ) {
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
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = AuraTheme.colors.textPrimary)
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "SKETCHPAD",
                        fontSize = 16.sp,
                        color = AuraTheme.colors.textPrimary,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { isGridActive = !isGridActive }) {
                        Icon(
                            imageVector = Icons.Default.GridOn,
                            contentDescription = "Grid lines",
                            tint = if (isGridActive) AuraTheme.colors.accentBrand else AuraTheme.colors.textMuted
                        )
                    }

                    // Undo
                    IconButton(
                        onClick = {
                            if (loadedStrokes.isNotEmpty()) {
                                val removed = loadedStrokes.removeAt(loadedStrokes.size - 1)
                                redoStrokes.add(removed)
                            }
                        },
                        enabled = loadedStrokes.isNotEmpty()
                    ) {
                        Icon(
                            Icons.Default.Undo,
                            contentDescription = "Undo",
                            tint = if (loadedStrokes.isNotEmpty()) AuraTheme.colors.textPrimary else AuraTheme.colors.textMuted
                        )
                    }

                    // Redo
                    IconButton(
                        onClick = {
                            if (redoStrokes.isNotEmpty()) {
                                val restored = redoStrokes.removeAt(redoStrokes.size - 1)
                                loadedStrokes.add(restored)
                            }
                        },
                        enabled = redoStrokes.isNotEmpty()
                    ) {
                        Icon(
                            Icons.Default.Redo,
                            contentDescription = "Redo",
                            tint = if (redoStrokes.isNotEmpty()) AuraTheme.colors.textPrimary else AuraTheme.colors.textMuted
                        )
                    }

                    IconButton(onClick = {
                        loadedStrokes.clear()
                        currentPoints.clear()
                        redoStrokes.clear()
                    }) {
                        Icon(Icons.Default.DeleteSweep, contentDescription = "Clear", tint = AuraTheme.colors.negativeRed)
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Button(
                        onClick = {
                            val serialized = viewModel.serializeDrawing(loadedStrokes)
                            onSaveDrawing(serialized)
                            onBack()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = AuraTheme.colors.accentBrand),
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp)
                    ) {
                        Text("Save", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Toolbar Controls
        Surface(
            color = AuraTheme.colors.cardBackground,
            modifier = Modifier.fillMaxWidth(),
            tonalElevation = 2.dp
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                // Brushes & Eraser Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    // Colors
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        colorsMap.forEach { (hexStr, colorVal) ->
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .clickable {
                                        selectedColorHex = hexStr
                                        isEraserActive = false
                                    }
                                    .padding(10.dp)
                                    .background(colorVal, CircleShape)
                                    .border(
                                        width = if (selectedColorHex == hexStr && !isEraserActive) 2.5.dp else 1.dp,
                                        color = if (selectedColorHex == hexStr && !isEraserActive) AuraTheme.colors.accentBrand else Color.Transparent,
                                        shape = CircleShape
                                    )
                            )
                        }
                    }

                    // Eraser Toggle
                    IconButton(
                        onClick = { isEraserActive = !isEraserActive },
                        modifier = Modifier
                            .background(
                                color = if (isEraserActive) AuraTheme.colors.accentBrand else Color.Transparent,
                                shape = CircleShape
                            )
                    ) {
                        Icon(
                            imageVector = Icons.Default.BorderColor,
                            contentDescription = "Eraser toggle",
                            tint = if (isEraserActive) Color.White else AuraTheme.colors.textMuted
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Brush Thickness slider
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Brush Width", fontSize = 11.sp, color = AuraTheme.colors.textMuted, modifier = Modifier.width(80.dp))
                    Slider(
                        value = selectedWidth,
                        onValueChange = { selectedWidth = it },
                        valueRange = 2f..40f,
                        colors = SliderDefaults.colors(
                            thumbColor = AuraTheme.colors.accentBrand,
                            activeTrackColor = AuraTheme.colors.accentBrand,
                            inactiveTrackColor = AuraTheme.colors.cardBorder
                        ),
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("${selectedWidth.toInt()}px", fontSize = 11.sp, color = AuraTheme.colors.textPrimary, modifier = Modifier.width(36.dp))
                }
            }
        }

        val gridBorderColor = AuraTheme.colors.cardBorder
        val marginRedColor = AuraTheme.colors.negativeRed
        val accentBrandColor = AuraTheme.colors.accentBrand
        val canvasBgColor = AuraTheme.colors.cardBackground

        // Draw Blackboard surface
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(16.dp)
                .border(1.dp, gridBorderColor, RoundedCornerShape(16.dp))
                .background(canvasBgColor, RoundedCornerShape(16.dp))
                .pointerInteropFilter { motionEvent ->
                    val x = motionEvent.x
                    val y = motionEvent.y
                    when (motionEvent.action) {
                        MotionEvent.ACTION_DOWN -> {
                            currentPoints.clear()
                            currentPoints.add(FloatPair(x, y))
                            true
                        }
                        MotionEvent.ACTION_MOVE -> {
                            currentPoints.add(FloatPair(x, y))
                            true
                        }
                        MotionEvent.ACTION_UP -> {
                            if (currentPoints.isNotEmpty()) {
                                loadedStrokes.add(
                                    SketchStroke(
                                        points = currentPoints.toList(),
                                        colorHex = if (isEraserActive) "#14171E" else selectedColorHex, // Matches chalkboard background
                                        strokeWidth = selectedWidth,
                                        isEraser = isEraserActive
                                    )
                                )
                                currentPoints.clear()
                            }
                            true
                        }
                        else -> false
                    }
                }
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val canvasWidth = size.width
                val canvasHeight = size.height

                // Draw ruled grid rows if requested
                if (isGridActive) {
                    val lineSpacing = 30.dp.toPx()
                    // Horizontal rows
                    var currentY = lineSpacing
                    while (currentY < canvasHeight) {
                        drawLine(
                            color = gridBorderColor.copy(alpha = 0.6f),
                            start = Offset(0f, currentY),
                            end = Offset(canvasWidth, currentY),
                            strokeWidth = 1f
                        )
                        currentY += lineSpacing
                    }
                    // Vertical margin rule list
                    drawLine(
                        color = marginRedColor.copy(alpha = 0.4f),
                        start = Offset(60.dp.toPx(), 0f),
                        end = Offset(60.dp.toPx(), canvasHeight),
                        strokeWidth = 1.5f
                    )
                }

                // Render permanent saved strokes
                for (stroke in loadedStrokes) {
                    val path = Path()
                    val startPoints = stroke.points
                    if (startPoints.isNotEmpty()) {
                        path.moveTo(startPoints[0].x, startPoints[0].y)
                        for (i in 1 until startPoints.size) {
                            path.lineTo(startPoints[i].x, startPoints[i].y)
                        }
                        
                        // Parse Hex and render
                        val strokeColor = try {
                            Color(android.graphics.Color.parseColor(stroke.colorHex))
                        } catch (e: Exception) {
                            accentBrandColor
                        }

                        drawPath(
                            path = path,
                            color = strokeColor,
                            style = Stroke(
                                width = stroke.strokeWidth,
                                cap = StrokeCap.Round,
                                join = StrokeJoin.Round
                            )
                        )
                    }
                }

                // Render current active line points dynamically
                if (currentPoints.isNotEmpty()) {
                    val activePath = Path()
                    activePath.moveTo(currentPoints[0].x, currentPoints[0].y)
                    for (i in 1 until currentPoints.size) {
                        activePath.lineTo(currentPoints[i].x, currentPoints[i].y)
                    }
                    
                    val activeColor = if (isEraserActive) {
                        canvasBgColor
                    } else {
                        try {
                            Color(android.graphics.Color.parseColor(selectedColorHex))
                        } catch (e: Exception) {
                            accentBrandColor
                        }
                    }

                    drawPath(
                        path = activePath,
                        color = activeColor,
                        style = Stroke(
                            width = selectedWidth,
                            cap = StrokeCap.Round,
                            join = StrokeJoin.Round
                        )
                    )
                }
            }
        }
    }
}
