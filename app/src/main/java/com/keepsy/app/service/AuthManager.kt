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
        
        Log.i(TAG, "signInWithGoogle: Initiating request with Client ID: $webClientId")

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
                Log.d(TAG, "signInWithGoogle: Launching UI...")
                val result = credentialManager.getCredential(
                    context = context,
                    request = request
                )
                
                val credential = result.credential
                Log.d(TAG, "Credential Type received: [${credential.type}]")
                
                // UNIVERSAL CHECK: Check both constant and raw string to ensure compatibility
                val isGoogleType = credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL || 
                                  credential.type.contains("googleid", ignoreCase = true)

                if (isGoogleType) {
                    val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
                    val idToken = googleIdTokenCredential.idToken
                    
                    if (idToken != null && idToken != "") {
                        Log.i(TAG, "Token successfully extracted")
                        val firebaseCredential = GoogleAuthProvider.getCredential(idToken, null)
                        viewModel.signInWithCredential(firebaseCredential)
                    } else {
                        throw Exception("Google ID Token is empty")
                    }
                } else {
                    Log.e(TAG, "Mismatched credential type: ${credential.type}")
                    viewModel.handleExternalError(Exception("Auth Mismatch: Unexpected response type from Google."))
                }
            } catch (e: GetCredentialException) {
                Log.e(TAG, "Credential Manager Error: ${e.message}")
                val msg = when {
                    e.message?.contains("10:") == true -> "Developer Error (10): Please verify that your SHA-1 is added to BOTH Firebase and Google Cloud Console."
                    e.message?.contains("7:") == true -> "Network Error (7): Please check your connection."
                    else -> e.message ?: "Authentication failed"
                }
                viewModel.handleExternalError(Exception(msg))
            } catch (e: Exception) {
                Log.e(TAG, "Fatal Auth Error", e)
                viewModel.handleExternalError(e)
            }
        }
    }
}
