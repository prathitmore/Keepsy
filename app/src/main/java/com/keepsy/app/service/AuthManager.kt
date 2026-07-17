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
        
        if (webClientId.contains("YOUR_WEB_CLIENT_ID", ignoreCase = true)) {
            Log.e(TAG, "signInWithGoogle: Web Client ID not configured in strings.xml")
            viewModel.handleExternalError(Exception("Google Sign-In is not configured yet. Please check the documentation."))
            return
        }

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
                Log.i(TAG, "signInWithGoogle: Launching Credential Manager")
                val result = credentialManager.getCredential(
                    context = context,
                    request = request
                )
                
                val credential = result.credential
                Log.d(TAG, "Received credential type: ${credential.type}")
                
                if (credential is GoogleIdTokenCredential) {
                    Log.i(TAG, "signInWithGoogle: ID Token received")
                    val firebaseCredential = GoogleAuthProvider.getCredential(credential.idToken, null)
                    viewModel.signInWithCredential(firebaseCredential)
                } else {
                    Log.e(TAG, "signInWithGoogle: Unexpected credential type: ${credential.type}")
                    // Improved error message to help diagnose configuration issues
                    viewModel.handleExternalError(Exception("Google Auth mismatch (type: ${credential.type}). Please verify SHA-1 and Web Client ID in Firebase Console."))
                }
            } catch (e: GetCredentialException) {
                Log.e(TAG, "signInWithGoogle: Credential Manager Error: ${e.message}")
                // Often 'invalid response' comes from here if SHA-1 is missing
                val msg = e.message ?: "Unknown credential error"
                viewModel.handleExternalError(Exception("Authentication failed: $msg"))
            } catch (e: Exception) {
                Log.e(TAG, "signInWithGoogle: Unexpected error", e)
                viewModel.handleExternalError(e)
            }
        }
    }
}
