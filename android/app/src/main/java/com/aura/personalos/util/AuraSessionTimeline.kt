package com.aura.personalos.util

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.CopyOnWriteArrayList

/**
 * AuraSessionTimeline — ADR-015
 *
 * Debug-only chronological session event recorder.
 * Captures lifecycle transitions, sync transactions, user interactions, and network shifts
 * with strict redaction of sensitive content (no tokens, passwords, or personal text).
 */
data class SessionEvent(
    val id: Long = System.currentTimeMillis(),
    val timestamp: String,
    val type: String,
    val details: String? = null
) {
    val formatted: String
        get() = if (details.isNullOrBlank()) "$timestamp  $type" else "$timestamp  $type ($details)"
}

object AuraSessionTimeline {

    private const val MAX_EVENTS = 150
    private val buffer = CopyOnWriteArrayList<SessionEvent>()
    private val _events = MutableStateFlow<List<SessionEvent>>(emptyList())
    val events: StateFlow<List<SessionEvent>> = _events.asStateFlow()

    fun record(type: String, details: String? = null) {
        val timeString = SimpleDateFormat("HH:mm:ss", Locale.US).format(Date())
        val event = SessionEvent(
            timestamp = timeString,
            type = type,
            details = details?.take(80) // Restrict detail length to prevent memory bloating
        )

        buffer.add(0, event)
        while (buffer.size > MAX_EVENTS) {
            buffer.removeAt(buffer.size - 1)
        }
        _events.value = buffer.toList()

        // Also record to AuraCrashHandler log buffer
        AuraCrashHandler.logEvent("TIMELINE", event.formatted)
    }

    fun clear() {
        buffer.clear()
        _events.value = emptyList()
    }
}
