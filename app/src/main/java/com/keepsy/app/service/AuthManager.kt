package com.keepsy.app.service

import android.content.Context
import android.util.Log
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialException
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.auth.GoogleAuthProvider
import com.keepsy.app.R
import com.keepsy.app.viewmodel.KeepsyViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

class AuthManager(private val context: Context) {

    private val credentialManager = CredentialManager.create(context.applicationContext)
    private val TAG = "KeepsyAuth"

    fun signInWithGoogle(
        coroutineScope: CoroutineScope,
        viewModel: KeepsyViewModel
    ) {
        val webClientId: String = context.getString(R.string.default_web_client_id)
        
        Log.d(TAG, "signInWithGoogle: Using Web Client ID: $webClientId")

        val googleIdOption = GetGoogleIdOption.Builder()
            .setFilterByAuthorizedAccounts(false)
            .setServerClientId(webClientId)
            .setAutoSelectEnabled(false)
            .build()

        val request = GetCredentialRequest.Builder()
            .addCredentialOption(googleIdOption)
            .build()

        viewModel.viewModelScope.launch {
            try {
                Log.i(TAG, "signInWithGoogle: Requesting Google Credentials...")
                val result = credentialManager.getCredential(
                    context = context,
                    request = request
                )
                
                val credential = result.credential
                Log.d(TAG, "Credential Type received: ${credential.type}")
                
                // FIX: Use hardcoded string comparison to bypass potential SDK class-casting issues
                if (credential.type == "com.google.android.libraries.identity.googleid.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL") {
                    val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
                    val firebaseCredential = GoogleAuthProvider.getCredential(googleIdTokenCredential.idToken, null)
                    
                    Log.i(TAG, "Google ID Token successfully parsed. Proceeding to Firebase login.")
                    viewModel.signInWithCredential(firebaseCredential)
                } else {
                    Log.e(TAG, "Unsupported credential type: ${credential.type}")
                    viewModel.handleExternalError(Exception("Authentication failed: Unrecognized login type (${credential.type})"))
                }
            } catch (e: GetCredentialException) {
                Log.e(TAG, "Credential Manager Error: ${e.message}")
                val userMsg = when {
                    e.message?.contains("7:") == true -> "Google Play Services error. Please check your internet and Google account."
                    e.message?.contains("10:") == true -> "Developer error: SHA-1 or Web Client ID mismatch. Please verify Firebase Console."
                    e.message?.contains("cancel") == true -> "Sign-in was cancelled."
                    else -> e.message ?: "Authentication failed"
                }
                viewModel.handleExternalError(Exception(userMsg))
            } catch (e: Exception) {
                Log.e(TAG, "Unexpected login error", e)
                viewModel.handleExternalError(e)
            }
        }
    }
}
