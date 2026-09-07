package com.example.ui

import androidx.compose.animation.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AvTimer
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun LiveClockWidget(
    modifier: Modifier = Modifier,
    initialIsAnalog: Boolean = false
) {
    // Persistent real-time seconds ticking produceState
    val calendarState = produceState(initialValue = Calendar.getInstance()) {
        while (true) {
            delay(1000)
            value = Calendar.getInstance()
        }
    }

    var isAnalog by remember { mutableStateOf(initialIsAnalog) }
    var isCompact by remember { mutableStateOf(false) }

    val cal = calendarState.value
    val timeFormat = SimpleDateFormat("HH:mm", Locale.US)
    val secondFormat = SimpleDateFormat("ss", Locale.US)
    val dateFormat = SimpleDateFormat("EEEE, dd MMMM yyyy", Locale.US)

    Card(
        modifier = modifier
            .fillMaxWidth()
            .border(
                1.dp,
                AuraTheme.colors.cardBorder,
                RoundedCornerShape(20.dp)
            ),
        colors = CardDefaults.cardColors(containerColor = AuraTheme.colors.cardBackground),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header Switch Bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (isAnalog) "ANALOG RADIAL" else "TIME MATRIX",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = AuraTheme.colors.textMuted,
                    letterSpacing = 1.sp
                )

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = { isCompact = !isCompact },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Schedule,
                            contentDescription = "Compact Toggle",
                            tint = if (isCompact) AuraTheme.colors.accentBrand else AuraTheme.colors.textMuted,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                    IconButton(
                        onClick = { isAnalog = !isAnalog },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.AvTimer,
                            contentDescription = "Clock Mode",
                            tint = if (isAnalog) AuraTheme.colors.accentBrand else AuraTheme.colors.textMuted,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            if (isAnalog) {
                // Draw analog clock canvas
                Box(
                    modifier = Modifier
                        .size(if (isCompact) 110.dp else 160.dp)
                        .background(AuraTheme.colors.cardBackground, CircleShape)
                        .border(1.5.dp, AuraTheme.colors.cardBorder, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    val accentColor = AuraTheme.colors.accentBrand
                    val textColor = AuraTheme.colors.textPrimary
                    val mutedColor = AuraTheme.colors.textMuted
                    val markerBorderColor = AuraTheme.colors.cardBorder

                    AnalogClockCanvas(
                        calendar = cal,
                        accentColor = accentColor,
                        textColor = textColor,
                        mutedColor = mutedColor,
                        markerBorderColor = markerBorderColor,
                        modifier = Modifier.fillMaxSize()
                    )
                    // Core point
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .background(AuraTheme.colors.accentBrand, CircleShape)
                    )
                }
            } else {
                // Digital modern glow LCD format
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.animateContentSize()
                ) {
                    Row(
                        verticalAlignment = Alignment.Bottom,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = timeFormat.format(cal.time),
                            fontSize = if (isCompact) 32.sp else 46.sp,
                            fontWeight = FontWeight.Black,
                            fontFamily = FontFamily.Monospace,
                            color = AuraTheme.colors.textPrimary,
                            letterSpacing = 1.sp
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = secondFormat.format(cal.time),
                            fontSize = if (isCompact) 16.sp else 22.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            color = AuraTheme.colors.accentBrand,
                            modifier = Modifier.padding(bottom = if (isCompact) 4.dp else 8.dp)
                        )
                    }
                }
            }

            if (!isCompact) {
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = dateFormat.format(cal.time).uppercase(),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = AuraTheme.colors.textSecondary,
                    letterSpacing = 1.sp
                )
            }
        }
    }
}

@Composable
fun AnalogClockCanvas(
    calendar: Calendar,
    accentColor: Color = Color(0xFFFF5B32),
    textColor: Color = Color.White,
    mutedColor: Color = Color.Gray,
    markerBorderColor: Color = Color.DarkGray,
    modifier: Modifier = Modifier
) {
    val hrs = calendar.get(Calendar.HOUR)
    val mins = calendar.get(Calendar.MINUTE)
    val secs = calendar.get(Calendar.SECOND)

    Canvas(modifier = modifier.padding(8.dp)) {
        val width = size.width
        val height = size.height
        val radius = width / 2f
        val center = Offset(width / 2f, height / 2f)

        // Draw markers around outer edge
        for (i in 0 until 12) {
            val angle = i * 30 * Math.PI / 180
            val startX = center.x + (radius - 10f) * sin(angle).toFloat()
            val startY = center.y - (radius - 10f) * cos(angle).toFloat()
            val endX = center.x + radius * sin(angle).toFloat()
            val endY = center.y - radius * cos(angle).toFloat()

            drawLine(
                color = if (i % 3 == 0) accentColor.copy(alpha = 0.8f) else markerBorderColor,
                start = Offset(startX, startY),
                end = Offset(endX, endY),
                strokeWidth = if (i % 3 == 0) 2.5.dp.toPx() else 1.dp.toPx()
            )
        }

        // 1. Seconds hand
        val secAngle = secs * 6 * Math.PI / 180
        val secLength = radius - 12f
        val secX = center.x + secLength * sin(secAngle).toFloat()
        val secY = center.y - secLength * cos(secAngle).toFloat()
        drawLine(
            color = accentColor,
            start = center,
            end = Offset(secX, secY),
            strokeWidth = 1.25.dp.toPx(),
            cap = StrokeCap.Round
        )

        // 2. Minutes hand
        val minAngle = (mins + secs / 60f) * 6 * Math.PI / 180
        val minLength = radius - 20f
        val minX = center.x + minLength * sin(minAngle).toFloat()
        val minY = center.y - minLength * cos(minAngle).toFloat()
        drawLine(
            color = textColor,
            start = center,
            end = Offset(minX, minY),
            strokeWidth = 2.5.dp.toPx(),
            cap = StrokeCap.Round
        )

        // 3. Hour hand
        val hrAngle = (hrs % 12 + mins / 60f) * 30 * Math.PI / 180
        val hrLength = radius - 36f
        val hrX = center.x + hrLength * sin(hrAngle).toFloat()
        val hrY = center.y - hrLength * cos(hrAngle).toFloat()
        drawLine(
            color = accentColor,
            start = center,
            end = Offset(hrX, hrY),
            strokeWidth = 4.0.dp.toPx(),
            cap = StrokeCap.Round
        )
    }
}
