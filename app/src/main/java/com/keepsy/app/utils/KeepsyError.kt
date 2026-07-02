package com.keepsy.app.utils

import java.io.IOException
import java.sql.SQLException

/**
 * KeepsyError represents centralized application errors.
 */
sealed class KeepsyError(val message: String, val cause: Throwable? = null) {
    class DatabaseError(message: String, cause: Throwable? = null) : KeepsyError(message, cause)
    class AuthError(message: String, cause: Throwable? = null) : KeepsyError(message, cause)
    class NetworkError(message: String, cause: Throwable? = null) : KeepsyError(message, cause)
    class JsonError(message: String, cause: Throwable? = null) : KeepsyError(message, cause)
    class UnknownError(message: String, cause: Throwable? = null) : KeepsyError(message, cause)
}

/**
 * Centralized error handler to log and wrap exceptions.
 */
object ErrorHandler {
    fun handleError(throwable: Throwable): KeepsyError {
        KeepsyLogger.e("Handling error: ${throwable.message}", throwable)
        
        return when (throwable) {
            is SQLException -> {
                KeepsyError.DatabaseError("Storage access failure", throwable)
            }
            is com.google.firebase.auth.FirebaseAuthException -> {
                val msg = when (throwable.errorCode) {
                    "ERROR_INVALID_EMAIL" -> "The email address is badly formatted."
                    "ERROR_WRONG_PASSWORD" -> "Incorrect password. Please try again."
                    "ERROR_USER_NOT_FOUND" -> "No account found with this email."
                    "ERROR_USER_DISABLED" -> "This account has been disabled."
                    "ERROR_TOO_MANY_REQUESTS" -> "Too many attempts. Please try again later."
                    "ERROR_EMAIL_ALREADY_IN_USE" -> "An account already exists with this email."
                    "ERROR_NETWORK_REQUEST_FAILED" -> "Network error. Please check your connection."
                    else -> throwable.localizedMessage ?: "Authentication service error"
                }
                KeepsyError.AuthError(msg, throwable)
            }
            is androidx.credentials.exceptions.GetCredentialException -> {
                KeepsyError.AuthError("Sign-in cancelled or failed: ${throwable.localizedMessage}", throwable)
            }
            is IOException -> {
                KeepsyError.NetworkError("Connectivity problem. Please check your internet connection.", throwable)
            }
            else -> {
                KeepsyError.UnknownError(throwable.localizedMessage ?: "Something went wrong", throwable)
            }
        }
    }
}
