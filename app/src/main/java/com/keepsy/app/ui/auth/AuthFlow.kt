package com.keepsy.app.ui.auth

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import com.keepsy.app.navigation.AuthSubScreen
import com.keepsy.app.service.AuthManager
import com.keepsy.app.viewmodel.KeepsyViewModel

@Composable
fun AuthFlow(
    viewModel: KeepsyViewModel,
    currentSub: AuthSubScreen,
    onNavigate: (AuthSubScreen) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val authManager = remember { AuthManager(context) }

    val onGoogleSignIn = {
        authManager.signInWithGoogle(scope, viewModel)
    }

    AnimatedContent(
        targetState = currentSub,
        transitionSpec = {
            fadeIn() togetherWith fadeOut()
        },
        label = "AuthTransition"
    ) { sub ->
        when (sub) {
            AuthSubScreen.Welcome -> WelcomeScreen(
                onSignInClick = { onNavigate(AuthSubScreen.SignIn) },
                onSignUpClick = { onNavigate(AuthSubScreen.SignUp) },
                onGoogleSignInClick = onGoogleSignIn
            )
            AuthSubScreen.SignIn -> SignInScreen(
                viewModel = viewModel,
                onBack = { onNavigate(AuthSubScreen.Welcome) },
                onSignUpClick = { onNavigate(AuthSubScreen.SignUp) },
                onGoogleSignInClick = onGoogleSignIn
            )
            AuthSubScreen.SignUp -> SignUpScreen(
                viewModel = viewModel,
                onBack = { onNavigate(AuthSubScreen.Welcome) },
                onSignInClick = { onNavigate(AuthSubScreen.SignIn) },
                onGoogleSignInClick = onGoogleSignIn
            )
        }
    }
}
