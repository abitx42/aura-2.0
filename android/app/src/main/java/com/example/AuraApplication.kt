package com.example

import android.app.Application
import com.example.data.AuraErrorHandler
import com.example.util.AuraCrashHandler
import com.example.util.AuraSessionTimeline

class AuraApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        AuraErrorHandler.install(this)
        AuraCrashHandler.install(this)
        AuraSessionTimeline.record("APP_OPENED", "${android.os.Build.MODEL} (API ${android.os.Build.VERSION.SDK_INT})")
    }
}

