package com.aura.personalos

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
import com.aura.personalos.ui.MainAppContainer
import com.aura.personalos.ui.AppViewModel
import com.aura.personalos.ui.theme.MyApplicationTheme
import com.aura.personalos.data.AuraErrorHandler
import com.aura.personalos.data.AuraImageLoader
import coil.Coil

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    
    // Install global error handler
    AuraErrorHandler.install(this)
    // Configure optimized image loader
    Coil.setImageLoader(AuraImageLoader.getInstance(this))

    enableEdgeToEdge()
    setContent {
      val viewModel: AppViewModel = viewModel()
      val themeMode by viewModel.themeMode.collectAsState()
      val themePalette by viewModel.themePalette.collectAsState()
      val hasSeenOnboarding by viewModel.hasSeenOnboarding.collectAsState()

      MyApplicationTheme(themeMode = themeMode, themePalette = themePalette) {
        if (!hasSeenOnboarding) {
          com.aura.personalos.ui.OnboardingScreen(
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
