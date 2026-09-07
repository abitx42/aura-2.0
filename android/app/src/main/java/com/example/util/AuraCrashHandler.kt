package com.example.util

import android.content.Context
import android.os.Build
import android.util.Log
import java.io.File
import java.io.PrintWriter
import java.io.StringWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.CopyOnWriteArrayList

/**
 * AuraCrashHandler — ADR-015
 *
 * Persistent, file-backed crash and diagnostic event recorder for Founder Testing Mode.
 * Ensures that unhandled lifecycle crashes, coroutine panics, or SQLite errors during
 * 14-day daily testing leave a complete forensic trace on disk without requiring adb.
 */
class AuraCrashHandler private constructor(
    private val context: Context,
    private val defaultHandler: Thread.UncaughtExceptionHandler?
) : Thread.UncaughtExceptionHandler {

    override fun uncaughtException(thread: Thread, throwable: Throwable) {
        try {
            val sw = StringWriter()
            val pw = PrintWriter(sw)
            throwable.printStackTrace(pw)
            val stackTraceString = sw.toString()

            val timestamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.US).format(Date())
            val crashReport = buildString {
                appendLine("================ CRASH REPORT ================")
                appendLine("Timestamp: $timestamp")
                appendLine("Device: ${Build.MANUFACTURER} ${Build.MODEL} (${Build.DEVICE})")
                appendLine("Android SDK: ${Build.VERSION.SDK_INT} (OS: ${Build.VERSION.RELEASE})")
                appendLine("Thread: ${thread.name} (id: ${thread.id})")
                appendLine("Exception: ${throwable.javaClass.name}: ${throwable.message}")
                appendLine("Stack Trace:")
                appendLine(stackTraceString)
                appendLine("==============================================")
                appendLine()
            }

            // 1. Append to disk
            val crashFile = File(context.filesDir, CRASH_LOG_FILENAME)
            crashFile.appendText(crashReport)

            // 2. Add to in-memory ring buffer
            recordMemoryLog("CRASH [${thread.name}]: ${throwable.message ?: throwable.javaClass.simpleName}")

            Log.e(TAG, "Uncaught exception recorded to ${crashFile.absolutePath}", throwable)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to write crash log to disk", e)
        } finally {
            // Forward to Android's default handler for standard OS behavior
            defaultHandler?.uncaughtException(thread, throwable)
        }
    }

    companion object {
        private const val TAG = "AuraCrashHandler"
        const val CRASH_LOG_FILENAME = "aura_crash_log.txt"
        private const val MAX_IN_MEMORY_LOGS = 100

        private val inMemoryLogs = CopyOnWriteArrayList<String>()

        @JvmStatic
        fun install(context: Context) {
            val currentHandler = Thread.getDefaultUncaughtExceptionHandler()
            if (currentHandler !is AuraCrashHandler) {
                val customHandler = AuraCrashHandler(context.applicationContext, currentHandler)
                Thread.setDefaultUncaughtExceptionHandler(customHandler)
                logEvent("SYSTEM", "AuraCrashHandler installed. Device: ${Build.MODEL} (API ${Build.VERSION.SDK_INT})")
            }
        }

        fun logEvent(tag: String, message: String) {
            val timestamp = SimpleDateFormat("HH:mm:ss.SSS", Locale.US).format(Date())
            val entry = "[$timestamp] [$tag] $message"
            recordMemoryLog(entry)
            Log.d(TAG, entry)
        }

        private fun recordMemoryLog(entry: String) {
            inMemoryLogs.add(0, entry)
            while (inMemoryLogs.size > MAX_IN_MEMORY_LOGS) {
                inMemoryLogs.removeAt(inMemoryLogs.size - 1)
            }
        }

        fun getRecentLogs(): List<String> = inMemoryLogs.toList()

        fun readCrashLogFile(context: Context): String {
            val crashFile = File(context.filesDir, CRASH_LOG_FILENAME)
            return if (crashFile.exists()) crashFile.readText() else "No fatal crashes recorded on this device."
        }

        fun clearCrashLogFile(context: Context): Boolean {
            val crashFile = File(context.filesDir, CRASH_LOG_FILENAME)
            inMemoryLogs.clear()
            return if (crashFile.exists()) crashFile.delete() else true
        }
    }
}
