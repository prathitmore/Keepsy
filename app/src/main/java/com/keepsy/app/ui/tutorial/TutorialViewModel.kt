package com.keepsy.app.ui.tutorial

import androidx.compose.ui.geometry.Rect
import com.keepsy.app.repository.SettingsManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import com.keepsy.app.utils.KeepsyLogger

/**
 * TutorialViewModel manages the state of the interactive tour.
 */
class TutorialViewModel(private val settingsManager: SettingsManager) {

    private val _currentStep = MutableStateFlow(TutorialStep.WELCOME)
    val currentStep: StateFlow<TutorialStep> = _currentStep.asStateFlow()

    private val _spotlights = MutableStateFlow<Map<String, Rect>>(emptyMap())
    val spotlights: StateFlow<Map<String, Rect>> = _spotlights.asStateFlow()

    private val _isVisible = MutableStateFlow(false)
    val isVisible: StateFlow<Boolean> = _isVisible.asStateFlow()

    init {
        try {
            val steps = TutorialStep.values()
            val savedStep = settingsManager.tutorialStep.value
            if (savedStep >= 0 && savedStep < steps.size) {
                _currentStep.value = steps[savedStep]
            }
        } catch (e: Exception) {
            KeepsyLogger.w("Tutorial initialization fallback")
        }
    }

    fun nextStep() {
        try {
            val steps = TutorialStep.values()
            val nextIndex = _currentStep.value.ordinal + 1
            if (nextIndex < steps.size) {
                val nextStep = steps[nextIndex]
                _currentStep.value = nextStep
                settingsManager.setTutorialStep(nextIndex)
            } else {
                completeTutorial()
            }
        } catch (e: Exception) {
            completeTutorial()
        }
    }

    fun previousStep() {
        try {
            val steps = TutorialStep.values()
            val prevIndex = _currentStep.value.ordinal - 1
            if (prevIndex >= 0) {
                val prevStep = steps[prevIndex]
                _currentStep.value = prevStep
                settingsManager.setTutorialStep(prevIndex)
            }
        } catch (e: Exception) {
            // No-op
        }
    }

    fun skipTutorial() {
        completeTutorial()
    }

    private fun completeTutorial() {
        _isVisible.value = false
        settingsManager.setTutorialCompleted(true)
        settingsManager.setTutorialStep(0)
        KeepsyLogger.i("Tutorial completed")
    }

    fun startTutorial() {
        _isVisible.value = true
        _currentStep.value = TutorialStep.WELCOME
        settingsManager.setTutorialStep(0)
        settingsManager.setTutorialCompleted(false)
        KeepsyLogger.i("Tutorial started")
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
