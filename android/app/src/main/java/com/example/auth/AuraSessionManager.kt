package com.example.auth

import android.content.Context
import android.content.SharedPreferences

class AuraSessionManager(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences("aura_session_prefs", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_ACCESS_TOKEN = "access_token"
        private const val KEY_USER_ID = "user_id"
        private const val KEY_USER_EMAIL = "user_email"
        private const val KEY_DISPLAY_NAME = "display_name"
        private const val KEY_ONBOARDING_DONE = "onboarding_done"

        @Volatile
        private var instance: AuraSessionManager? = null

        fun getInstance(context: Context): AuraSessionManager {
            return instance ?: synchronized(this) {
                instance ?: AuraSessionManager(context.applicationContext).also { instance = it }
            }
        }
    }

    val accessToken: String? get() = prefs.getString(KEY_ACCESS_TOKEN, null)
    val userId: String? get() = prefs.getString(KEY_USER_ID, null)
    val userEmail: String? get() = prefs.getString(KEY_USER_EMAIL, null)
    val displayName: String? get() = prefs.getString(KEY_DISPLAY_NAME, null)
    val isSignedIn: Boolean get() = !accessToken.isNullOrBlank()
    val isOnboardingComplete: Boolean get() = prefs.getBoolean(KEY_ONBOARDING_DONE, false)

    fun saveSession(token: String, id: String, email: String, name: String? = null) {
        prefs.edit()
            .putString(KEY_ACCESS_TOKEN, token)
            .putString(KEY_USER_ID, id)
            .putString(KEY_USER_EMAIL, email)
            .putString(KEY_DISPLAY_NAME, name)
            .apply()
    }

    fun setOnboardingComplete(completed: Boolean = true) {
        prefs.edit().putBoolean(KEY_ONBOARDING_DONE, completed).apply()
    }

    fun clearSession() {
        prefs.edit().clear().apply()
    }
}
