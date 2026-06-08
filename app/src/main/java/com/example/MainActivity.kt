package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.KeepsyApp
import com.example.ui.theme.MyApplicationTheme
import com.example.viewmodel.KeepsyViewModel

class MainActivity : ComponentActivity() {
    private val viewModel: KeepsyViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
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

            MyApplicationTheme(darkTheme = useDarkTheme) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = androidx.compose.material3.MaterialTheme.colorScheme.background
                ) {
                    KeepsyApp(viewModel = viewModel)
                }
            }
        }
    }
}
