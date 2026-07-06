package com.keepsy.app.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.keepsy.app.model.AuthState
import com.keepsy.app.navigation.AuthSubScreen
import com.keepsy.app.navigation.Screen
import com.keepsy.app.navigation.SubScreen
import com.keepsy.app.navigation.TabScreen
import com.keepsy.app.ui.auth.AuthFlow
import com.keepsy.app.ui.auth.AuthSuccessScreen
import com.keepsy.app.ui.auth.EmailVerificationScreen
import com.keepsy.app.ui.components.SplashScreenView
import com.keepsy.app.ui.onboarding.OnboardingScreenView
import com.keepsy.app.ui.tutorial.TutorialScreen
import com.keepsy.app.ui.tutorial.TutorialViewModel
import com.keepsy.app.viewmodel.KeepsyViewModel
import com.keepsy.app.utils.KeepsyLogger
import kotlinx.coroutines.delay
import androidx.compose.runtime.snapshotFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first

@Composable
fun KeepsyApp(viewModel: KeepsyViewModel, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val authState by viewModel.authState.collectAsStateWithLifecycle()
    val isRestoringData by viewModel.isRestoringData.collectAsStateWithLifecycle()
    val isTutorialCompleted by viewModel.isTutorialCompleted.collectAsStateWithLifecycle()
    val isTutorialRequested by viewModel.isTutorialRequested.collectAsStateWithLifecycle()
    
    var appScreen by remember { mutableStateOf<Screen>(Screen.Splash) }
    var authSubScreen by remember { mutableStateOf<AuthSubScreen>(AuthSubScreen.Welcome) }
    var currentTab by remember { mutableStateOf<TabScreen>(TabScreen.Home) }
    
    // To ensure animations have time to play
    var isSplashAnimationComplete by remember { mutableStateOf(false) }
    
    val subScreenHistory = remember { mutableStateListOf<SubScreen>() }
    val currentSubScreen = subScreenHistory.lastOrNull() ?: SubScreen.None

    fun navigateToSub(sub: SubScreen) {
        subScreenHistory.add(sub)
    }

    fun popSub(): Boolean {
        if (subScreenHistory.isNotEmpty()) {
            subScreenHistory.removeAt(subScreenHistory.size - 1)
            return true
        }
        return false
    }

    LaunchedEffect(isTutorialRequested) {
        if (isTutorialRequested) {
            appScreen = Screen.Tutorial
            viewModel.requestTutorial(false)
        }
    }

    LaunchedEffect(authState, isSplashAnimationComplete) {
        if (!isSplashAnimationComplete) return@LaunchedEffect
        
        delay(300) // Small buffer for stability
        KeepsyLogger.d("KeepsyApp: Navigating with AuthState $authState")
        try {
            when (authState) {
                is AuthState.Authenticated -> {
                    val user = (authState as AuthState.Authenticated).user
                    KeepsyLogger.i("KeepsyApp: User authenticated: ${user.uid}, Verified: ${user.isEmailVerified}")
                    
                    // 1. Check if email is verified
                    if (!user.isEmailVerified) {
                        KeepsyLogger.i("KeepsyApp: Email not verified, showing verification screen")
                        appScreen = Screen.VerifyEmail
                    } else {
                        // 2. Show Success screen for a brief moment to feel premium
                        appScreen = Screen.AuthSuccess(user.name ?: user.email ?: "Friend")
                        delay(2000) // Increased to 2 seconds for a calmer experience
                        
                        // 3. Check cloud data and onboarding status
                        try {
                            viewModel.checkOnboardingStatus()
                        } catch (e: Exception) {
                            KeepsyLogger.e("KeepsyApp: Onboarding check failed", e)
                        }

                        val onboardingDone = viewModel.isOnboardingCompleted.value
                        val tutorialDone = viewModel.isTutorialCompleted.value
                        
                        KeepsyLogger.i("KeepsyApp: Navigation check - Onboarding: $onboardingDone, Tutorial: $tutorialDone")

                        if (onboardingDone) {
                            if (tutorialDone) {
                                appScreen = Screen.Dashboard
                            } else {
                                KeepsyLogger.i("KeepsyApp: Starting Tutorial for new user")
                                appScreen = Screen.Tutorial
                            }
                        } else {
                            appScreen = Screen.Onboarding
                        }
                    }
                }
                is AuthState.Unauthenticated, is AuthState.Error -> {
                    KeepsyLogger.i("KeepsyApp: User unauthenticated or error, going to Auth")
                    appScreen = Screen.Auth
                }
                else -> {
                    // Stay on Splash or Idle
                }
            }
        } catch (e: Exception) {
            KeepsyLogger.e("CRITICAL ERROR in KeepsyApp Navigation", e)
        }
    }

    BackHandler(enabled = appScreen == Screen.Dashboard || (appScreen == Screen.Auth && authSubScreen != AuthSubScreen.Welcome)) {
        if (appScreen == Screen.Dashboard) {
            if (!popSub()) {
                if (currentTab != TabScreen.Home) {
                    currentTab = TabScreen.Home
                } else {
                    (context as? android.app.Activity)?.finish()
                }
            }
        } else if (appScreen == Screen.Auth) {
            authSubScreen = AuthSubScreen.Welcome
        }
    }

    val snackbarHostState = remember { SnackbarHostState() }
    val errorState by viewModel.errorState.collectAsStateWithLifecycle()

    LaunchedEffect(errorState) {
        errorState?.let { error ->
            snackbarHostState.showSnackbar(error.message)
            viewModel.clearError()
        }
    }

    LaunchedEffect(authState) {
        if (authState is AuthState.Error) {
            snackbarHostState.showSnackbar((authState as AuthState.Error).message)
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { padding ->
        Box(modifier = Modifier.padding(padding)) {
            Crossfade(targetState = appScreen, label = "AppTransition") { screen ->
                when (screen) {
                    Screen.Splash -> SplashScreenView(onAnimationFinished = { isSplashAnimationComplete = true })
                    Screen.Auth -> {
                        AuthFlow(
                            viewModel = viewModel,
                            currentSub = authSubScreen,
                            onNavigate = { authSubScreen = it }
                        )
                    }
                    is Screen.AuthSuccess -> {
                        AuthSuccessScreen(userName = screen.name, isRestoring = isRestoringData)
                    }
                    Screen.Onboarding -> OnboardingScreenView(
                        viewModel = viewModel,
                        onFinished = {
                            KeepsyLogger.i("KeepsyApp: Onboarding finished")
                            viewModel.setOnboardingCompleted()
                            viewModel.manualSync()
                            appScreen = Screen.Tutorial
                        }
                    )
                    Screen.Tutorial -> TutorialScreen(
                        viewModel = viewModel,
                        tutorialViewModel = viewModel.tutorialViewModel,
                        onFinished = {
                            appScreen = Screen.Dashboard
                        }
                    )
                    Screen.VerifyEmail -> EmailVerificationScreen(viewModel = viewModel)
                    Screen.Dashboard -> {
                        DashboardScaffold(
                            viewModel = viewModel,
                            currentTab = currentTab,
                            onTabSelected = { tab ->
                                subScreenHistory.clear()
                                currentTab = tab
                            },
                            currentSubScreen = currentSubScreen,
                            onNavigateToSub = ::navigateToSub,
                            onPopSub = { popSub() },
                            tutorialViewModel = viewModel.tutorialViewModel
                        )
                    }
                }
            }
        }
    }
}
