package com.example.ui

import android.os.Build
import android.view.HapticFeedbackConstants
import android.view.View

object AuraHaptics {

    /**
     * For extremely subtle micro-ticks, like scrolling, calendar switches, or light toggles.
     */
    fun triggerSubtleTick(view: View?) {
        try {
            view?.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
        } catch (_: Exception) {}
    }

    /**
     * For soft sliding transitions or continuous adjustment feedback.
     */
    fun triggerSlideFeedback(view: View?) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
                view?.performHapticFeedback(HapticFeedbackConstants.TEXT_HANDLE_MOVE)
            } else {
                view?.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
            }
        } catch (_: Exception) {}
    }

    /**
     * For selection / item clicks.
     */
    fun triggerSelection(view: View?) {
        try {
            view?.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
        } catch (_: Exception) {}
    }

    /**
     * For major successful commitments (e.g., saving an expense, checking off a critical task).
     */
    fun triggerConfirm(view: View?) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                view?.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
            } else {
                view?.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
            }
        } catch (_: Exception) {}
    }

    /**
     * For input rejection, validation failures, or cancellations.
     */
    fun triggerReject(view: View?) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                view?.performHapticFeedback(HapticFeedbackConstants.REJECT)
            } else {
                view?.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
            }
        } catch (_: Exception) {}
    }

    /**
     * For gesture / drag drop snapping.
     */
    fun triggerGesture(view: View?) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                view?.performHapticFeedback(HapticFeedbackConstants.GESTURE_START)
            } else {
                view?.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
            }
        } catch (_: Exception) {}
    }
}
