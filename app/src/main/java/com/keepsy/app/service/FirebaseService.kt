package com.keepsy.app.service

import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.auth.AuthCredential
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.keepsy.app.model.User
import kotlinx.coroutines.tasks.await
import android.os.Bundle
import com.keepsy.app.utils.KeepsyLogger

class FirebaseService(private val analytics: FirebaseAnalytics) {

    private val auth = FirebaseAuth.getInstance()
    private val firestore = FirebaseFirestore.getInstance()
    private val crashlytics = FirebaseCrashlytics.getInstance()

    fun getCurrentUser(): User? {
        val firebaseUser = auth.currentUser
        KeepsyLogger.d("getCurrentUser: ${firebaseUser?.uid ?: "no user"}")
        return firebaseUser?.let {
            User(
                uid = it.uid,
                name = it.displayName,
                email = it.email,
                photoUrl = it.photoUrl?.toString(),
                isAnonymous = it.isAnonymous
            )
        }
    }

    suspend fun signInWithEmail(email: String, password: String): User {
        KeepsyLogger.i("signInWithEmail: attempt for $email")
        try {
            val result = auth.signInWithEmailAndPassword(email, password).await()
            val firebaseUser = result.user ?: throw Exception("Sign in failed: no user")
            KeepsyLogger.i("signInWithEmail: success for ${firebaseUser.uid}")
            val user = User(
                uid = firebaseUser.uid,
                name = firebaseUser.displayName,
                email = firebaseUser.email,
                photoUrl = firebaseUser.photoUrl?.toString(),
                isAnonymous = firebaseUser.isAnonymous
            )
            updateUserProfile(user)
            logEvent("login", "email")
            return user
        } catch (e: Exception) {
            KeepsyLogger.e("signInWithEmail: failed for $email", e)
            throw e
        }
    }

    suspend fun signUpWithEmail(email: String, password: String, name: String): User {
        KeepsyLogger.i("Starting email sign up for: $email")
        try {
            val result = auth.createUserWithEmailAndPassword(email, password).await()
            val firebaseUser = result.user ?: throw Exception("Registration failed - no user returned")
            
            val profileUpdates = com.google.firebase.auth.UserProfileChangeRequest.Builder()
                .setDisplayName(name)
                .build()
            firebaseUser.updateProfile(profileUpdates).await()

            // Send verification email automatically
            firebaseUser.sendEmailVerification().await()
            KeepsyLogger.i("Verification email sent to: $email")

            val user = User(
                uid = firebaseUser.uid,
                name = name,
                email = firebaseUser.email,
                photoUrl = firebaseUser.photoUrl?.toString(),
                isAnonymous = firebaseUser.isAnonymous,
                createdAt = System.currentTimeMillis()
            )
            updateUserProfile(user, isNewUser = true)
            logEvent("sign_up", "email")
            KeepsyLogger.i("Email sign up successful: ${user.uid}")
            return user
        } catch (e: Exception) {
            KeepsyLogger.e("Email sign up failed for $email", e)
            throw e
        }
    }

    suspend fun reloadUser(): Boolean {
        auth.currentUser?.reload()?.await()
        val isVerified = auth.currentUser?.isEmailVerified ?: false
        KeepsyLogger.i("User reloaded. Email verified: $isVerified")
        return isVerified
    }

    fun isEmailVerified(): Boolean {
        return auth.currentUser?.isEmailVerified ?: false
    }

    suspend fun signInWithCredential(credential: AuthCredential): User {
        KeepsyLogger.i("signInWithCredential: start")
        try {
            val result = auth.signInWithCredential(credential).await()
            val firebaseUser = result.user ?: throw Exception("Credential sign in failed: no user")
            KeepsyLogger.i("signInWithCredential: success for ${firebaseUser.uid}")
            val user = User(
                uid = firebaseUser.uid,
                name = firebaseUser.displayName,
                email = firebaseUser.email,
                photoUrl = firebaseUser.photoUrl?.toString(),
                isAnonymous = firebaseUser.isAnonymous
            )
            updateUserProfile(user)
            logEvent("login", "google")
            return user
        } catch (e: Exception) {
            KeepsyLogger.e("signInWithCredential: failed", e)
            throw e
        }
    }

    private suspend fun updateUserProfile(user: User, isNewUser: Boolean = false) {
        KeepsyLogger.d("updateUserProfile: updating firestore for ${user.uid}")
        try {
            val userRef = firestore.collection("users").document(user.uid)
            
            val updates = HashMap<String, Any?>()
            updates["uid"] = user.uid
            updates["displayName"] = user.name
            updates["email"] = user.email
            updates["photoUrl"] = user.photoUrl
            updates["lastLogin"] = System.currentTimeMillis()
            updates["platform"] = "Android"
            updates["appVersion"] = "1.0.0"

            if (isNewUser) {
                updates["createdAt"] = user.createdAt ?: System.currentTimeMillis()
            }

            userRef.set(updates, SetOptions.merge()).await()
            KeepsyLogger.d("updateUserProfile: firestore update success")
            
            // Set user ID for Crashlytics
            crashlytics.setUserId(user.uid)
            user.email?.let { crashlytics.setCustomKey("email", it) }
        } catch (e: Exception) {
            KeepsyLogger.w("updateUserProfile: firestore update failed (non-fatal): ${e.message}")
        }
    }

    fun signOut() {
        KeepsyLogger.i("signOut: user ${auth.currentUser?.uid}")
        auth.signOut()
        logEvent("logout", null)
    }

    suspend fun sendPasswordResetEmail(email: String) {
        KeepsyLogger.i("Password reset started for: $email")
        try {
            auth.sendPasswordResetEmail(email).await()
            KeepsyLogger.i("Password reset success for: $email")
        } catch (e: Exception) {
            KeepsyLogger.e("Password reset failure for: $email", e)
            throw e
        }
    }

    suspend fun sendEmailVerification() {
        val uid = auth.currentUser?.uid
        KeepsyLogger.i("Verification email sent attempt for: $uid")
        try {
            auth.currentUser?.sendEmailVerification()?.await()
            KeepsyLogger.i("Verification email sent success for: $uid")
        } catch (e: Exception) {
            KeepsyLogger.e("Verification email sent failure for: $uid", e)
            throw e
        }
    }

    fun addAuthStateListener(listener: (User?) -> Unit) {
        auth.addAuthStateListener { firebaseAuth ->
            val user = firebaseAuth.currentUser?.let {
                User(
                    uid = it.uid,
                    name = it.displayName,
                    email = it.email,
                    photoUrl = it.photoUrl?.toString(),
                    isAnonymous = it.isAnonymous
                )
            }
            KeepsyLogger.d("authStateListener: ${user?.uid ?: "no user"}")
            listener(user)
        }
    }

    private fun logEvent(name: String, method: String?) {
        val bundle = Bundle()
        method?.let { bundle.putString(FirebaseAnalytics.Param.METHOD, it) }
        analytics.logEvent(name, bundle)
    }
}
