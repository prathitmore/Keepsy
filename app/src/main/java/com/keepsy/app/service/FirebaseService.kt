package com.keepsy.app.service

import android.net.Uri
import android.os.Bundle
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.auth.AuthCredential
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageMetadata
import com.keepsy.app.model.User
import com.keepsy.app.utils.KeepsyLogger
import kotlinx.coroutines.delay
import kotlinx.coroutines.tasks.await
import java.util.HashMap

class FirebaseService(private val analytics: FirebaseAnalytics) {

    private val auth = FirebaseAuth.getInstance()
    private val firestore = FirebaseFirestore.getInstance()
    private val storage = FirebaseStorage.getInstance("gs://keepsy-project.firebasestorage.app")
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
                isAnonymous = it.isAnonymous,
                isEmailVerified = it.isEmailVerified,
                createdAt = it.metadata?.creationTimestamp
            )
        }
    }

    suspend fun updateProfile(name: String?, photoUrl: String?) {
        val user = auth.currentUser ?: throw Exception("No user authenticated")
        KeepsyLogger.i("Updating profile state: name=$name, hasPhoto=${photoUrl != null}")
        
        val profileUpdates = UserProfileChangeRequest.Builder()
        if (name != null) profileUpdates.setDisplayName(name)
        
        if (photoUrl == "") {
            KeepsyLogger.d("Clearing profile photo URL in Auth")
            profileUpdates.setPhotoUri(null)
        } else if (photoUrl != null) {
            KeepsyLogger.d("Setting new photo URL in Auth: $photoUrl")
            profileUpdates.setPhotoUri(Uri.parse(photoUrl))
        }
        
        try {
            user.updateProfile(profileUpdates.build()).await()
            user.reload().await()
            KeepsyLogger.d("Firebase Auth profile updated successfully")
        } catch (e: Exception) {
            KeepsyLogger.e("Auth update failed", e)
        }
        
        val updates = HashMap<String, Any?>()
        if (name != null) updates["displayName"] = name
        if (photoUrl != null) {
            updates["photoUrl"] = if (photoUrl == "") null else photoUrl
        }
        
        if (updates.isNotEmpty()) {
            firestore.collection("users").document(user.uid).set(updates, SetOptions.merge()).await()
            KeepsyLogger.i("Firestore profile document updated successfully")
        }
    }

    suspend fun uploadProfilePicture(uri: Uri): String {
        val uid = auth.currentUser?.uid ?: throw Exception("No user authenticated")
        KeepsyLogger.i("Starting upload to Firebase Storage for user: $uid")
        
        // Using a timestamped path to prevent 404s caused by eventual consistency or CDN caching
        val timestamp = System.currentTimeMillis()
        val storagePath = "users/$uid/profile/avatar_$timestamp.jpg"
        val ref = storage.reference.child(storagePath)
        
        try {
            val metadata = StorageMetadata.Builder()
                .setContentType("image/jpeg")
                .setCustomMetadata("uid", uid)
                .build()

            KeepsyLogger.d("Executing putFile to $storagePath")
            // Uploading file directly with metadata
            ref.putFile(uri, metadata).await()
            KeepsyLogger.i("Physical file written to Storage: $storagePath")
            
            // Wait briefly to ensure indexing is complete
            delay(1000)
            
            var downloadUrl: String? = null
            var retryCount = 0
            while (downloadUrl == null && retryCount < 3) {
                try {
                    downloadUrl = ref.downloadUrl.await().toString()
                    KeepsyLogger.d("Download URL retrieved: $downloadUrl")
                } catch (e: Exception) {
                    retryCount++
                    KeepsyLogger.w("Download URL retrieval failed, retrying ($retryCount)...")
                    delay(1500)
                }
            }
            
            return downloadUrl ?: throw Exception("Failed to retrieve download URL after multiple attempts")
        } catch (e: Exception) {
            KeepsyLogger.e("Firebase Storage pipeline failed", e)
            throw e
        }
    }

    suspend fun deleteProfilePicture() {
        val uid = auth.currentUser?.uid ?: throw Exception("No user authenticated")
        KeepsyLogger.i("Requested deletion of profile picture for user: $uid")
        
        // We update the pointers first to immediately refresh UI fallbacks
        updateProfile(null, "")
        
        // Cleaning up the storage folder is best-effort and happens in background
        try {
            val folderRef = storage.reference.child("users/$uid/profile/")
            folderRef.listAll().addOnSuccessListener { listResult ->
                for (item in listResult.items) {
                    item.delete()
                }
            }
        } catch (e: Exception) {
            KeepsyLogger.d("Storage cleanup skipped: ${e.message}")
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
                isAnonymous = firebaseUser.isAnonymous,
                isEmailVerified = firebaseUser.isEmailVerified
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
            
            val profileUpdates = UserProfileChangeRequest.Builder()
                .setDisplayName(name)
                .build()
            firebaseUser.updateProfile(profileUpdates).await()

            firebaseUser.sendEmailVerification().await()
            KeepsyLogger.i("Verification email sent to: $email")

            val user = User(
                uid = firebaseUser.uid,
                name = name,
                email = firebaseUser.email,
                photoUrl = firebaseUser.photoUrl?.toString(),
                isAnonymous = firebaseUser.isAnonymous,
                createdAt = System.currentTimeMillis(),
                isEmailVerified = firebaseUser.isEmailVerified
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

    suspend fun signInWithCredential(credential: AuthCredential): User {
        KeepsyLogger.i("signInWithCredential: start")
        try {
            val result = auth.signInWithCredential(credential).await()
            val firebaseUser = result.user ?: throw Exception("Credential sign in failed: no user")
            val isNewUser = result.additionalUserInfo?.isNewUser ?: false
            
            KeepsyLogger.i("signInWithCredential: success for ${firebaseUser.uid}, isNewUser: $isNewUser")
            val user = User(
                uid = firebaseUser.uid,
                name = firebaseUser.displayName,
                email = firebaseUser.email,
                photoUrl = firebaseUser.photoUrl?.toString(),
                isAnonymous = firebaseUser.isAnonymous,
                isEmailVerified = firebaseUser.isEmailVerified,
                createdAt = if (isNewUser) System.currentTimeMillis() else null
            )
            updateUserProfile(user, isNewUser = isNewUser)
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
                    isAnonymous = it.isAnonymous,
                    isEmailVerified = it.isEmailVerified
                )
            }
            KeepsyLogger.d("authStateListener: ${user?.uid ?: "no user"}")
            listener(user)
        }
    }

    private fun logEvent(name: String, method: String?) {
        val bundle = Bundle()
        method?.let { bundle.putString(com.google.firebase.analytics.FirebaseAnalytics.Param.METHOD, it) }
        analytics.logEvent(name, bundle)
    }

    suspend fun changePassword(current: String, new: String) {
        val user = auth.currentUser ?: throw Exception("No user authenticated")
        val email = user.email ?: throw Exception("No email associated")
        
        val credential = com.google.firebase.auth.EmailAuthProvider.getCredential(email, current)
        user.reauthenticate(credential).await()
        
        user.updatePassword(new).await()
    }
}
