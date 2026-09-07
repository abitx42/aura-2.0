package com.aura.personalos

import android.app.Application
import com.aura.personalos.data.AuraErrorHandler
import com.aura.personalos.util.AuraCrashHandler
import com.aura.personalos.util.AuraSessionTimeline

class AuraApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        AuraErrorHandler.install(this)
        AuraCrashHandler.install(this)
        AuraSessionTimeline.record("APP_OPENED", "${android.os.Build.MODEL} (API ${android.os.Build.VERSION.SDK_INT})")
    }
}

