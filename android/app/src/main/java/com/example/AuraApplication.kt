package com.example

import android.app.Application
import android.content.Context
import com.example.data.AuraErrorHandler
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions

class AuraApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        AuraErrorHandler.install(this)
        ensureFirebaseInitialized(this)
    }

    companion object {
        fun ensureFirebaseInitialized(context: Context): FirebaseApp? {
            return try {
                val apps = FirebaseApp.getApps(context)
                if (apps.isNotEmpty()) {
                    FirebaseApp.getInstance()
                } else {
                    var app: FirebaseApp? = null
                    try {
                        app = FirebaseApp.initializeApp(context)
                    } catch (e: Exception) {
                        // ignore and fall back to programmatic initialization
                    }
                    if (app == null) {
                        val options = FirebaseOptions.Builder()
                            .setApiKey("AIzaSyDummyKeyForAuraNotesInitOnly")
                            .setApplicationId("1:1234567890:android:abcdef123456")
                            .setProjectId("aura-notes-placeholder")
                            .build()
                        app = FirebaseApp.initializeApp(context, options)
                    }
                    app
                }
            } catch (e: Exception) {
                AuraErrorHandler.report("AuraApplication.ensureFirebaseInitialized", e)
                null
            }
        }
    }
}

