package com.example

import android.app.Application
import com.example.data.AuraErrorHandler
import com.example.util.AuraCrashHandler

class AuraApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        AuraErrorHandler.install(this)
        AuraCrashHandler.install(this)
    }
}

