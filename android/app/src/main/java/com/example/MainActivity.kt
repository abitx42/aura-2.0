package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.MainAppContainer
import com.example.ui.AppViewModel
import com.example.ui.theme.MyApplicationTheme
import com.example.data.AuraErrorHandler
import com.example.data.AuraImageLoader
import coil.Coil

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    
    // Install global error handler
    AuraErrorHandler.install(this)
    // Configure optimized image loader
    Coil.setImageLoader(AuraImageLoader.getInstance(this))
    // Initialize Firebase if not already initialized
    AuraApplication.ensureFirebaseInitialized(this)

    enableEdgeToEdge()
    setContent {
      val viewModel: AppViewModel = viewModel()
      val themeMode by viewModel.themeMode.collectAsState()
      val themePalette by viewModel.themePalette.collectAsState()
      val hasSeenOnboarding by viewModel.hasSeenOnboarding.collectAsState()

      MyApplicationTheme(themeMode = themeMode, themePalette = themePalette) {
        if (!hasSeenOnboarding) {
          com.example.ui.OnboardingScreen(
            viewModel = viewModel,
            onFinished = {
              viewModel.setHasSeenOnboarding(true)
            }
          )
        } else {
          MainAppContainer(
            viewModel = viewModel,
            modifier = Modifier.fillMaxSize()
          )
        }
      }
    }
  }
}
