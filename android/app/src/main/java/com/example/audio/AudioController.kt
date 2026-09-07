package com.example.audio

import android.content.Context
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.os.Build
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.io.File
import java.io.IOException

class AudioController(private val context: Context) {

    private var mediaRecorder: MediaRecorder? = null
    private var mediaPlayer: MediaPlayer? = null
    private var trackingJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.Main + Job())

    // Recording state flows
    private val _isRecording = MutableStateFlow(false)
    val isRecording: StateFlow<Boolean> = _isRecording

    private val _recordedDuration = MutableStateFlow(0L) // milliseconds
    val recordedDuration: StateFlow<Long> = _recordedDuration

    // Playback state flows
    private val _playbackState = MutableStateFlow<PlaybackState>(PlaybackState.Idle)
    val playbackState: StateFlow<PlaybackState> = _playbackState

    private val _playbackProgress = MutableStateFlow(0f) // 0.0f to 1.0f
    val playbackProgress: StateFlow<Float> = _playbackProgress

    private val _playbackHeader = MutableStateFlow("")
    val playbackHeader: StateFlow<String> = _playbackHeader

    var currentFilePath: String? = null

    // ==========================================
    // RECORDING CONTROL
    // ==========================================
    fun startRecording(): String? {
        stopPlaying()
        val filename = "recording_${System.currentTimeMillis()}.aac"
        val outputFile = File(context.cacheDir, filename)
        currentFilePath = outputFile.absolutePath

        try {
            _recordedDuration.value = 0L
            mediaRecorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                MediaRecorder(context)
            } else {
                @Suppress("DEPRECATION")
                MediaRecorder()
            }.apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setOutputFile(outputFile.absolutePath)
                prepare()
                start()
            }
            _isRecording.value = true
            startRecordingTimer()
            Log.d("AudioController", "Started recording into $currentFilePath")
            return currentFilePath
        } catch (e: Exception) {
            Log.e("AudioController", "Failed starting recording", e)
            _isRecording.value = false
            currentFilePath = null
            return null
        }
    }

    fun stopRecording() {
        if (!_isRecording.value) return
        try {
            mediaRecorder?.apply {
                stop()
                release()
            }
        } catch (e: Exception) {
            Log.e("AudioController", "Error stopping recorder", e)
            // If recording was stopped too quickly, delete invalid zero-byte file
            try {
                currentFilePath?.let { File(it).delete() }
                currentFilePath = null
            } catch (_: Exception) {}
        } finally {
            mediaRecorder = null
            _isRecording.value = false
            stopRecordingTimer()
            Log.d("AudioController", "Stopped recording. Saved file: $currentFilePath")
        }
    }

    private var recordingTimerJob: Job? = null
    private fun startRecordingTimer() {
        recordingTimerJob?.cancel()
        recordingTimerJob = scope.launch(Dispatchers.Main) {
            val startTime = System.currentTimeMillis()
            while (_isRecording.value) {
                _recordedDuration.value = System.currentTimeMillis() - startTime
                delay(100)
            }
        }
    }

    private fun stopRecordingTimer() {
        recordingTimerJob?.cancel()
    }

    // ==========================================
    // PLAYBACK CONTROL
    // ==========================================
    fun startPlaying(filePath: String, headerTitle: String = "Voice Note Recording") {
        if (filePath.isBlank() || !File(filePath).exists()) {
            _playbackState.value = PlaybackState.Error("File not found.")
            return
        }

        stopPlaying()
        _playbackHeader.value = headerTitle

        try {
            mediaPlayer = MediaPlayer().apply {
                setDataSource(filePath)
                prepare()
                setOnCompletionListener {
                    _playbackState.value = PlaybackState.Completed
                    _playbackProgress.value = 1.0f
                    stopTrackingProgress()
                }
                start()
            }
            _playbackState.value = PlaybackState.Playing
            startTrackingProgress()
            Log.d("AudioController", "Started playing: $filePath")
        } catch (e: Exception) {
            Log.e("AudioController", "Failed starting player", e)
            _playbackState.value = PlaybackState.Error("Playing error")
        }
    }

    fun resumePlaying() {
        val player = mediaPlayer ?: return
        if (_playbackState.value is PlaybackState.Paused) {
            player.start()
            _playbackState.value = PlaybackState.Playing
            startTrackingProgress()
        }
    }

    fun pausePlaying() {
        val player = mediaPlayer ?: return
        if (player.isPlaying) {
            player.pause()
            _playbackState.value = PlaybackState.Paused
            stopTrackingProgress()
        }
    }

    fun seekTo(progress: Float) {
        val player = mediaPlayer ?: return
        val duration = player.duration
        if (duration > 0) {
            val targetPosition = (progress.coerceIn(0f, 1f) * duration).toInt()
            player.seekTo(targetPosition)
            _playbackProgress.value = progress.coerceIn(0f, 1f)
        }
    }

    fun stopPlaying() {
        stopTrackingProgress()
        try {
            mediaPlayer?.apply {
                if (isPlaying) {
                    stop()
                }
                release()
            }
        } catch (e: Exception) {
            Log.e("AudioController", "Error releasing player", e)
        } finally {
            mediaPlayer = null
            _playbackState.value = PlaybackState.Idle
            _playbackProgress.value = 0f
        }
    }

    private fun startTrackingProgress() {
        trackingJob?.cancel()
        trackingJob = scope.launch(Dispatchers.Main) {
            while (mediaPlayer != null && (_playbackState.value is PlaybackState.Playing)) {
                try {
                    mediaPlayer?.let { player ->
                        val duration = player.duration
                        if (duration > 0) {
                            _playbackProgress.value = player.currentPosition.toFloat() / duration.toFloat()
                        }
                    }
                } catch (e: Exception) {
                    // media player can be in invalid state
                }
                delay(100)
            }
        }
    }

    private fun stopTrackingProgress() {
        trackingJob?.cancel()
    }
}

sealed class PlaybackState {
    object Idle : PlaybackState()
    object Playing : PlaybackState()
    object Paused : PlaybackState()
    object Completed : PlaybackState()
    data class Error(val message: String) : PlaybackState()
}
