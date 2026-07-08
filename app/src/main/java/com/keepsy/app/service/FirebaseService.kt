package com.keepsy.app.service

import android.net.Uri
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
        KeepsyLogger.d("getCurrentUser: uid=${firebaseUser?.uid}, name=${firebaseUser?.displayName}")
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
        KeepsyLogger.i("updateProfile request: name=$name, photoUrl=$photoUrl")
        
        val profileUpdates = UserProfileChangeRequest.Builder()
        if (name != null) profileUpdates.setDisplayName(name)
        
        if (photoUrl == "") {
            profileUpdates.setPhotoUri(null)
        } else if (photoUrl != null) {
            profileUpdates.setPhotoUri(Uri.parse(photoUrl))
        }
        
        try {
            user.updateProfile(profileUpdates.build()).await()
            user.reload().await()
            KeepsyLogger.i("Auth update success. New name: ${auth.currentUser?.displayName}")
        } catch (e: Exception) {
            KeepsyLogger.e("Auth profile update failed", e)
        }
        
        val updates = HashMap<String, Any?>()
        if (name != null) updates["displayName"] = name
        if (photoUrl != null) {
            updates["photoUrl"] = if (photoUrl == "") null else photoUrl
        }
        
        if (updates.isNotEmpty()) {
            firestore.collection("users").document(user.uid).set(updates, SetOptions.merge()).await()
            KeepsyLogger.i("Firestore update success")
        }
    }

    suspend fun uploadProfilePicture(uri: Uri): String {
        val uid = auth.currentUser?.uid ?: throw Exception("No user authenticated")
        val timestamp = System.currentTimeMillis()
        val storagePath = "users/$uid/profile/avatar_$timestamp.jpg"
        val ref = storage.reference.child(storagePath)
        
        try {
            val context = com.keepsy.app.KeepsyApplication.instance
            val inputStream = context.contentResolver.openInputStream(uri) ?: throw Exception("Failed to open image stream")
            
            val outputStream = java.io.ByteArrayOutputStream()
            val buffer = ByteArray(8192)
            var bytesRead = inputStream.read(buffer)
            while (bytesRead != -1) {
                outputStream.write(buffer, 0, bytesRead)
                bytesRead = inputStream.read(buffer)
            }
            val bytes = outputStream.toByteArray()
            inputStream.close()

            val metadata = StorageMetadata.Builder()
                .setContentType("image/jpeg")
                .setCustomMetadata("uid", uid)
                .build()

            KeepsyLogger.d("Executing putBytes to $storagePath")
            ref.putBytes(bytes, metadata).await()
            
            var downloadUrl: String? = null
            var lastError: Exception? = null
            
            for (attempt in 1..5) {
                try {
                    delay(1500L * attempt) 
                    downloadUrl = ref.downloadUrl.await().toString()
                    if (downloadUrl != null) break
                } catch (e: Exception) {
                    lastError = e
                    KeepsyLogger.w("Download URL attempt $attempt failed")
                }
            }
            
            val finalUrl = downloadUrl ?: throw lastError ?: Exception("Download URL retrieval timeout")
            
            // Clean up old avatars
            try {
                val folderRef = storage.reference.child("users/$uid/profile/")
                val listResult = folderRef.listAll().await()
                for (item in listResult.items) {
                    if (item.name != "avatar_$timestamp.jpg") {
                        item.delete()
                    }
                }
            } catch (e: Exception) { /* cleanup failure is non-fatal */ }
            
            return finalUrl
        } catch (e: Exception) {
            KeepsyLogger.e("uploadProfilePicture failure", e)
            throw e
        }
    }

    suspend fun deleteProfilePicture() {
        val uid = auth.currentUser?.uid ?: throw Exception("No user authenticated")
        updateProfile(null, "")
        try {
            val folderRef = storage.reference.child("users/$uid/profile/")
            val listResult = folderRef.listAll().await()
            for (item in listResult.items) {
                item.delete().await()
            }
        } catch (e: Exception) {
            KeepsyLogger.w("Storage cleanup failed: ${e.message}")
        }
    }

    suspend fun reloadUser(): Boolean {
        auth.currentUser?.reload()?.await()
        return auth.currentUser?.isEmailVerified ?: false
    }

    fun isEmailVerified(): Boolean {
        return auth.currentUser?.isEmailVerified ?: false
    }

    suspend fun signInWithEmail(email: String, password: String): User {
        val result = auth.signInWithEmailAndPassword(email, password).await()
        val firebaseUser = result.user ?: throw Exception("Login failed")
        val user = User(
            uid = firebaseUser.uid,
            name = firebaseUser.displayName,
            email = firebaseUser.email,
            photoUrl = firebaseUser.photoUrl?.toString(),
            isAnonymous = firebaseUser.isAnonymous,
            isEmailVerified = firebaseUser.isEmailVerified
        )
        updateUserProfile(user)
        return user
    }

    suspend fun signUpWithEmail(email: String, password: String, name: String): User {
        val result = auth.createUserWithEmailAndPassword(email, password).await()
        val firebaseUser = result.user ?: throw Exception("Registration failed")
        val profileUpdates = UserProfileChangeRequest.Builder().setDisplayName(name).build()
        firebaseUser.updateProfile(profileUpdates).await()
        firebaseUser.sendEmailVerification().await()
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
        return user
    }

    suspend fun signInWithCredential(credential: AuthCredential): User {
        val result = auth.signInWithCredential(credential).await()
        val firebaseUser = result.user ?: throw Exception("Credential login failed")
        val isNewUser = result.additionalUserInfo?.isNewUser ?: false
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
        return user
    }

    private suspend fun updateUserProfile(user: User, isNewUser: Boolean = false) {
        try {
            val updates = HashMap<String, Any?>()
            updates["uid"] = user.uid
            updates["displayName"] = user.name
            updates["email"] = user.email
            updates["photoUrl"] = user.photoUrl
            updates["lastLogin"] = System.currentTimeMillis()
            if (isNewUser) updates["createdAt"] = user.createdAt ?: System.currentTimeMillis()
            firestore.collection("users").document(user.uid).set(updates, SetOptions.merge()).await()
        } catch (e: Exception) {
            KeepsyLogger.w("Firestore user update failed: ${e.message}")
        }
    }

    fun signOut() = auth.signOut()

    suspend fun sendPasswordResetEmail(email: String) {
        auth.sendPasswordResetEmail(email).await()
    }

    suspend fun sendEmailVerification() {
        auth.currentUser?.sendEmailVerification()?.await()
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
            listener(user)
        }
    }

    suspend fun changePassword(current: String, new: String) {
        val user = auth.currentUser ?: throw Exception("No user")
        val email = user.email ?: throw Exception("No email")
        val credential = com.google.firebase.auth.EmailAuthProvider.getCredential(email, current)
        user.reauthenticate(credential).await()
        user.updatePassword(new).await()
    }
}
