package com.keepsy.app.ui.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.keepsy.app.R
import com.keepsy.app.model.AuthState
import com.keepsy.app.ui.components.PrimaryButton
import com.keepsy.app.ui.components.SecondaryButton
import com.keepsy.app.ui.components.KeepsyTextField
import com.keepsy.app.ui.theme.Background
import com.keepsy.app.ui.theme.BorderColor
import com.keepsy.app.ui.theme.CardBackground
import com.keepsy.app.ui.theme.PrimaryAccent
import com.keepsy.app.ui.theme.TextPrimary
import com.keepsy.app.ui.theme.TextSecondary
import com.keepsy.app.viewmodel.KeepsyViewModel

@Composable
fun SignInScreen(
    viewModel: KeepsyViewModel,
    onBack: () -> Unit,
    onSignUpClick: () -> Unit,
    onGoogleSignInClick: () -> Unit
) {
    val authState by viewModel.authState.collectAsState()
    val focusManager = LocalFocusManager.current
    
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }

    val isLoading = authState is AuthState.Loading

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Background)
            .verticalScroll(rememberScrollState())
            .padding(24.dp)
    ) {
        IconButton(onClick = onBack, enabled = !isLoading) {
            Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = TextPrimary)
        }
        
        Spacer(modifier = Modifier.height(32.dp))
        
        Text(
            text = "Welcome Back",
            fontSize = 32.sp,
            fontWeight = FontWeight.Bold,
            color = TextPrimary
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = stringResource(id = R.string.sign_in_subtitle),
            fontSize = 14.sp,
            color = TextSecondary
        )

        Spacer(modifier = Modifier.height(48.dp))

        KeepsyTextField(
            value = email,
            onValueChange = { email = it },
            label = "Email",
            leadingIcon = Icons.Default.Email,
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Email,
                imeAction = ImeAction.Next
            ),
            keyboardActions = KeyboardActions(
                onNext = { focusManager.moveFocus(FocusDirection.Down) }
            ),
            enabled = !isLoading
        )
        
        Spacer(modifier = Modifier.height(16.dp))

        KeepsyTextField(
            value = password,
            onValueChange = { password = it },
            label = "Password",
            leadingIcon = Icons.Default.Lock,
            isPassword = true,
            passwordVisible = passwordVisible,
            onPasswordToggle = { passwordVisible = !passwordVisible },
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Password,
                imeAction = ImeAction.Done
            ),
            keyboardActions = KeyboardActions(
                onDone = { 
                    focusManager.clearFocus()
                    viewModel.signIn(email, password)
                }
            ),
            enabled = !isLoading
        )

        Spacer(modifier = Modifier.height(12.dp))
        
        var showResetDialog by remember { mutableStateOf(false) }
        
        Text(
            text = "Forgot Password?",
            color = PrimaryAccent,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.align(Alignment.End).clickable { showResetDialog = true }
        )

        if (showResetDialog) {
            AlertDialog(
                onDismissRequest = { showResetDialog = false },
                containerColor = CardBackground,
                title = { Text("Reset Password", fontWeight = FontWeight.Bold, color = TextPrimary) },
                text = {
                    Column {
                        Text("Enter your email address to receive a reset link.", color = TextSecondary)
                        Spacer(modifier = Modifier.height(16.dp))
                        KeepsyTextField(
                            value = email,
                            onValueChange = { email = it },
                            label = "Email",
                            leadingIcon = Icons.Default.Email
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            viewModel.sendPasswordResetEmail(email) {
                                showResetDialog = false
                                // Error handling is already managed by viewModel.errorState
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryAccent)
                    ) {
                        Text("Send Link")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showResetDialog = false }) {
                        Text("Cancel", color = TextSecondary)
                    }
                }
            )
        }

        Spacer(modifier = Modifier.height(32.dp))

        PrimaryButton(
            text = "Sign In",
            onClick = { 
                focusManager.clearFocus()
                viewModel.signIn(email, password) 
            },
            isLoading = isLoading
        )

        Spacer(modifier = Modifier.height(24.dp))

        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            HorizontalDivider(modifier = Modifier.weight(1f), color = BorderColor)
            Text(
                text = " OR ",
                color = TextSecondary,
                fontSize = 12.sp,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
            HorizontalDivider(modifier = Modifier.weight(1f), color = BorderColor)
        }

        Spacer(modifier = Modifier.height(24.dp))

        SecondaryButton(
            text = "Continue with Google",
            onClick = onGoogleSignInClick,
            enabled = !isLoading
        )

        Spacer(modifier = Modifier.weight(1f))

        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = "Don't have an account? ", color = TextSecondary, fontSize = 14.sp)
            Text(
                text = "Create Account",
                color = PrimaryAccent,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.clickable { onSignUpClick() }
            )
        }
    }
}
