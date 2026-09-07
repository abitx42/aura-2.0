package com.aura.personalos.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aura.personalos.ui.theme.AuraTheme
import com.aura.personalos.util.AuraCrashHandler

/**
 * AuraErrorBoundary — ADR-015
 *
 * Catches view-level exceptions, records forensics to AuraCrashHandler, and provides
 * a non-destructive recovery card so that unhandled UI composition or state errors
 * during Founder Testing never result in an abrupt force-close.
 */
val LocalErrorReporter = compositionLocalOf<(Throwable) -> Unit> { { _ -> } }

@Composable
fun AuraErrorBoundary(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    var caughtError by remember { mutableStateOf<Throwable?>(null) }

    if (caughtError != null) {
        val error = caughtError!!
        val clipboardManager = LocalClipboardManager.current
        var copiedToast by remember { mutableStateOf(false) }

        Box(
            modifier = modifier
                .fillMaxSize()
                .background(AuraTheme.colors.screenBackground)
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Card(
                colors = CardDefaults.cardColors(containerColor = AuraTheme.colors.cardBackground),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .padding(20.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Text(
                        text = "⚠️ UI RECOVERY BOUNDARY",
                        color = AuraTheme.colors.badgeGold,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )

                    Text(
                        text = "An unexpected error occurred in this view. Aura preserved your local Room database safely.",
                        color = AuraTheme.colors.textSecondary,
                        fontSize = 12.sp,
                        lineHeight = 16.sp
                    )

                    Surface(
                        color = AuraTheme.colors.screenBackground,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "${error.javaClass.simpleName}: ${error.message ?: "Unknown error"}",
                            color = AuraTheme.colors.textPrimary,
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                            modifier = Modifier.padding(12.dp)
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                clipboardManager.setText(AnnotatedString(error.stackTraceToString()))
                                copiedToast = true
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(if (copiedToast) "COPIED! ✓" else "COPY ERROR", fontSize = 11.sp)
                        }

                        Button(
                            onClick = { caughtError = null },
                            colors = ButtonDefaults.buttonColors(containerColor = AuraTheme.colors.accentBrand),
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("RETRY VIEW", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    } else {
        CompositionLocalProvider(
            LocalErrorReporter provides { err ->
                AuraCrashHandler.logEvent("UI_ERROR", "${err.javaClass.simpleName}: ${err.message}")
                com.aura.personalos.util.AuraSessionTimeline.record("UI_BOUNDARY_TRIGGERED", "${err.javaClass.simpleName}: ${err.message?.take(50)}")
                caughtError = err
            }
        ) {
            content()
        }
    }
}
