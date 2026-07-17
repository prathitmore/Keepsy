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
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.tasks.await
import java.util.HashMap

class FirebaseService(private val analytics: FirebaseAnalytics) {

    private val auth = FirebaseAuth.getInstance()
    private val firestore = FirebaseFirestore.getInstance()
    private val storage = FirebaseStorage.getInstance()
    private val crashlytics = FirebaseCrashlytics.getInstance()

    fun getCurrentUser(): User? {
        val firebaseUser = auth.currentUser
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

    suspend fun updateProfile(name: String?, displayName: String?, photoUrl: String?) {
        val user = auth.currentUser ?: throw Exception("No user authenticated")
        
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
        } catch (e: Exception) {
            KeepsyLogger.e("Auth update failed", e)
        }
        
        val updates = HashMap<String, Any?>()
        if (name != null) updates["name"] = name
        if (displayName != null) updates["displayName"] = displayName
        if (photoUrl != null) {
            updates["photoUrl"] = if (photoUrl == "") null else photoUrl
        }
        
        if (updates.isNotEmpty()) {
            firestore.collection("users").document(user.uid).set(updates, SetOptions.merge()).await()
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
            var bytesRead: Int
            while (true) {
                bytesRead = inputStream.read(buffer)
                if (bytesRead == -1) break
                outputStream.write(buffer, 0, bytesRead)
            }
            val bytes = outputStream.toByteArray()
            inputStream.close()

            val metadata = StorageMetadata.Builder()
                .setContentType("image/jpeg")
                .setCustomMetadata("uid", uid)
                .build()

            KeepsyLogger.i("Uploading image bytes: ${bytes.size}")
            ref.putBytes(bytes, metadata).await()
            
            delay(1500)
            
            var downloadUrl: String? = null
            try {
                downloadUrl = ref.downloadUrl.await().toString()
            } catch (e: Exception) {
                delay(2000)
                downloadUrl = ref.downloadUrl.await().toString()
            }
            
            return downloadUrl ?: throw Exception("Could not retrieve public URL")
        } catch (e: Exception) {
            KeepsyLogger.e("uploadProfilePicture pipeline failed", e)
            throw e
        }
    }

    suspend fun deleteProfilePicture() {
        val uid = auth.currentUser?.uid ?: throw Exception("No user authenticated")
        updateProfile(null, null, "")
        try {
            val folderRef = storage.reference.child("users/$uid/profile/")
            val listResult = folderRef.listAll().await()
            for (item in listResult.items) {
                item.delete().await()
            }
        } catch (e: Exception) {
            KeepsyLogger.w("Storage cleanup failed")
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
        val u = result.user ?: throw Exception("Login failed")
        val user = User(
            uid = u.uid,
            name = u.displayName,
            email = u.email,
            photoUrl = u.photoUrl?.toString(),
            isAnonymous = u.isAnonymous,
            isEmailVerified = u.isEmailVerified
        )
        updateUserProfile(user)
        return user
    }

    suspend fun signUpWithEmail(email: String, password: String, name: String): User {
        val result = auth.createUserWithEmailAndPassword(email, password).await()
        val u = result.user ?: throw Exception("Registration failed")
        
        val profileUpdates = UserProfileChangeRequest.Builder().setDisplayName(name).build()
        u.updateProfile(profileUpdates).await()
        u.sendEmailVerification().await()

        val user = User(
            uid = u.uid,
            name = name,
            email = u.email,
            photoUrl = u.photoUrl?.toString(),
            isAnonymous = u.isAnonymous,
            isEmailVerified = u.isEmailVerified,
            createdAt = System.currentTimeMillis()
        )
        updateUserProfile(user, isNewUser = true)
        return user
    }

    suspend fun signInWithCredential(credential: AuthCredential): User {
        val result = auth.signInWithCredential(credential).await()
        val u = result.user ?: throw Exception("Sign in failed")
        val isNew = result.additionalUserInfo?.isNewUser ?: false
        val user = User(
            uid = u.uid,
            name = u.displayName,
            email = u.email,
            photoUrl = u.photoUrl?.toString(),
            isAnonymous = u.isAnonymous,
            isEmailVerified = u.isEmailVerified,
            createdAt = if (isNew) System.currentTimeMillis() else null
        )
        updateUserProfile(user, isNewUser = isNew)
        return user
    }

    private suspend fun updateUserProfile(user: User, isNewUser: Boolean = false) {
        try {
            val userRef = firestore.collection("users").document(user.uid)
            val updates = HashMap<String, Any?>()
            updates["uid"] = user.uid
            updates["email"] = user.email
            updates["lastLogin"] = System.currentTimeMillis()
            updates["platform"] = "Android"
            updates["appVersion"] = "1.2.2"

            if (isNewUser) {
                updates["createdAt"] = user.createdAt ?: System.currentTimeMillis()
                updates["name"] = user.name
                updates["displayName"] = user.name
                updates["photoUrl"] = user.photoUrl
            }

            userRef.set(updates, SetOptions.merge()).await()
            crashlytics.setUserId(user.uid)
            user.email?.let { crashlytics.setCustomKey("email", it) }
        } catch (e: Exception) {
            KeepsyLogger.w("Firestore user update failed: ${e.message}")
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    fun getProfileFlow(): Flow<Map<String, Any?>?> {
        return authStateFlow().flatMapLatest { u ->
            if (u == null) flowOf(null)
            else callbackFlow {
                val listener = firestore.collection("users").document(u.uid)
                    .addSnapshotListener { snapshot, error ->
                        if (error != null) {
                            close(error)
                            return@addSnapshotListener
                        }
                        trySend(snapshot?.data)
                    }
                awaitClose { listener.remove() }
            }
        }
    }
    
    @OptIn(ExperimentalCoroutinesApi::class)
    private fun authStateFlow(): Flow<com.google.firebase.auth.FirebaseUser?> = callbackFlow {
        val listener = FirebaseAuth.AuthStateListener { trySend(it.currentUser) }
        auth.addAuthStateListener(listener)
        awaitClose { auth.removeAuthStateListener(listener) }
    }

    fun signOut() = auth.signOut()

    suspend fun sendPasswordResetEmail(email: String) = auth.sendPasswordResetEmail(email).await()
    suspend fun sendEmailVerification() = auth.currentUser?.sendEmailVerification()?.await()

    fun addAuthStateListener(listener: (User?) -> Unit) {
        auth.addAuthStateListener { firebaseAuth ->
            val u = firebaseAuth.currentUser
            val user = u?.let {
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
        val user = auth.currentUser ?: throw Exception("No user authenticated")
        val email = user.email ?: throw Exception("No email associated")
        val credential = com.google.firebase.auth.EmailAuthProvider.getCredential(email, current)
        user.reauthenticate(credential).await()
        user.updatePassword(new).await()
    }
}
