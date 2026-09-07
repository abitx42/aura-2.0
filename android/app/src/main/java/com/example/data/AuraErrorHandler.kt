package com.example.data

import android.content.Context
import android.os.Build
import android.util.Log
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.io.PrintWriter
import java.io.StringWriter
import java.text.SimpleDateFormat
import java.util.*

/**
 * Centralized error handler for Aura.
 * Captures all unhandled exceptions, formats diagnostic payloads,
 * and exposes error state for UI consumption.
 */
object AuraErrorHandler {

    private val _lastError = MutableStateFlow<ErrorPayload?>(null)
    val lastError: StateFlow<ErrorPayload?> = _lastError

    private val _errorHistory = MutableStateFlow<List<ErrorPayload>>(emptyList())
    val errorHistory: StateFlow<List<ErrorPayload>> = _errorHistory

    /**
     * CoroutineExceptionHandler for viewModelScope.
     * Catches unhandled coroutine exceptions silently.
     */
    val coroutineHandler = CoroutineExceptionHandler { _, throwable ->
        handleException("CoroutineScope", throwable)
    }

    /**
     * Install as the global uncaught exception handler.
     * Call this in Application.onCreate() or MainActivity.
     */
    fun install(context: Context) {
        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            handleException("UncaughtException[${thread.name}]", throwable)
            // Optionally delegate to default handler for crash reporting
            defaultHandler?.uncaughtException(thread, throwable)
        }
    }

    /**
     * Manually report an error from a try/catch block.
     */
    fun report(tag: String, throwable: Throwable, silent: Boolean = true) {
        handleException(tag, throwable, silent)
    }

    /**
     * Manually report an error message without an exception.
     */
    fun reportMessage(tag: String, message: String) {
        val payload = ErrorPayload(
            tag = tag,
            message = message,
            stackTrace = "",
            deviceInfo = buildDeviceInfo(),
            timestamp = System.currentTimeMillis()
        )
        pushPayload(payload)
    }

    private fun handleException(
        tag: String,
        throwable: Throwable,
        silent: Boolean = true
    ) {
        val sw = StringWriter()
        throwable.printStackTrace(PrintWriter(sw))

        val payload = ErrorPayload(
            tag = tag,
            message = throwable.message ?: "Unknown error",
            stackTrace = sw.toString().take(2000),
            deviceInfo = buildDeviceInfo(),
            timestamp = System.currentTimeMillis()
        )

        pushPayload(payload)

        // Always log locally
        Log.e("AuraErrorHandler", "[$tag] ${throwable.message}", throwable)
    }

    private fun pushPayload(payload: ErrorPayload) {
        _lastError.value = payload
        _errorHistory.value = (listOf(payload) + _errorHistory.value).take(50)
    }

    private fun buildDeviceInfo(): String {
        return buildString {
            append("Device: ${Build.MANUFACTURER} ${Build.MODEL}")
            append(" | Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})")
            append(" | Time: ${SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Date())}")
            val runtime = Runtime.getRuntime()
            val usedMem = (runtime.totalMemory() - runtime.freeMemory()) / 1024 / 1024
            val maxMem = runtime.maxMemory() / 1024 / 1024
            append(" | Memory: ${usedMem}MB/${maxMem}MB")
        }
    }
}

data class ErrorPayload(
    val tag: String,
    val message: String,
    val stackTrace: String,
    val deviceInfo: String,
    val timestamp: Long
)
