package com.keepsy.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.keepsy.app.ui.KeepsyApp
import com.keepsy.app.ui.theme.KeepsyTheme
import com.keepsy.app.viewmodel.KeepsyViewModel

class MainActivity : ComponentActivity() {
    private val viewModel: KeepsyViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        
        // Mandatory edge-to-edge full bleed rendering
        enableEdgeToEdge()

        setContent {
            // Observe the user's reactive theme preference
            val darkModePref by viewModel.darkModePreference.collectAsStateWithLifecycle()
            val useDarkTheme = when (darkModePref) {
                null -> isSystemInDarkTheme()
                true -> true
                false -> false
            }

            KeepsyTheme(darkTheme = useDarkTheme) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    KeepsyApp(viewModel = viewModel)
                }
            }
        }
    }
}
