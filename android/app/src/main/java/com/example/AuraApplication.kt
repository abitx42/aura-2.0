package com.example

import android.app.Application
import com.example.data.AuraErrorHandler

class AuraApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        AuraErrorHandler.install(this)
    }
}

