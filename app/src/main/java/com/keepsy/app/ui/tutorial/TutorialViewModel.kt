package com.keepsy.app.ui.tutorial

import androidx.compose.ui.geometry.Rect
import androidx.lifecycle.ViewModel
import com.keepsy.app.repository.SettingsManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class TutorialViewModel(private val settingsManager: SettingsManager) : ViewModel() {

    private val _currentStep = MutableStateFlow(TutorialStep.values()[settingsManager.tutorialStep.value])
    val currentStep: StateFlow<TutorialStep> = _currentStep.asStateFlow()

    private val _spotlights = MutableStateFlow<Map<String, Rect>>(emptyMap())
    val spotlights: StateFlow<Map<String, Rect>> = _spotlights.asStateFlow()

    private val _isVisible = MutableStateFlow(false)
    val isVisible: StateFlow<Boolean> = _isVisible.asStateFlow()

    fun nextStep() {
        val nextIndex = _currentStep.value.ordinal + 1
        if (nextIndex < TutorialStep.values().size) {
            val nextStep = TutorialStep.values()[nextIndex]
            _currentStep.value = nextStep
            settingsManager.setTutorialStep(nextIndex)
        } else {
            completeTutorial()
        }
    }

    fun previousStep() {
        val prevIndex = _currentStep.value.ordinal - 1
        if (prevIndex >= 0) {
            val prevStep = TutorialStep.values()[prevIndex]
            _currentStep.value = prevStep
            settingsManager.setTutorialStep(prevIndex)
        }
    }

    fun skipTutorial() {
        completeTutorial()
    }

    private fun completeTutorial() {
        _isVisible.value = false
        settingsManager.setTutorialCompleted(true)
        settingsManager.setTutorialStep(0)
    }

    fun startTutorial() {
        _isVisible.value = true
        _currentStep.value = TutorialStep.WELCOME
        settingsManager.setTutorialStep(0)
        settingsManager.setTutorialCompleted(false)
    }

    fun updateSpotlight(key: String, rect: Rect) {
        val current = _spotlights.value.toMutableMap()
        current[key] = rect
        _spotlights.value = current
    }
    
    fun setVisibility(visible: Boolean) {
        _isVisible.value = visible
    }
}
