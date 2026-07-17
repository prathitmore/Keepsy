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
        KeepsyLogger.d("FirebaseService: getCurrentUser uid=${firebaseUser?.uid}")
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
        KeepsyLogger.i("FirebaseService: updateProfile name=$name, photoUrl=$photoUrl")
        
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
            KeepsyLogger.d("FirebaseService: Auth profile updated and reloaded")
        } catch (e: Exception) {
            KeepsyLogger.e("FirebaseService: Auth update failed", e)
        }
        
        val updates = HashMap<String, Any?>()
        if (name != null) updates["name"] = name
        if (displayName != null) updates["displayName"] = displayName
        if (photoUrl != null) {
            updates["photoUrl"] = if (photoUrl == "") null else photoUrl
        }
        
        if (updates.isNotEmpty()) {
            try {
                firestore.collection("users").document(user.uid).set(updates, SetOptions.merge()).await()
                KeepsyLogger.i("FirebaseService: Firestore profile updated")
            } catch (e: Exception) {
                KeepsyLogger.e("FirebaseService: Firestore update failed", e)
            }
        }
    }

    suspend fun uploadProfilePicture(uri: Uri): String {
        val uid = auth.currentUser?.uid ?: throw Exception("No user authenticated")
        val timestamp = System.currentTimeMillis()
        val storagePath = "users/$uid/profile/avatar_$timestamp.jpg"
        val ref = storage.reference.child(storagePath)
        
        KeepsyLogger.i("FirebaseService: uploadProfilePicture path=$storagePath")
        
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

            KeepsyLogger.d("FirebaseService: Uploading ${bytes.size} bytes")
            ref.putBytes(bytes, metadata).await()
            KeepsyLogger.i("FirebaseService: Upload successful")
            
            delay(1500)
            
            var downloadUrl: String? = null
            try {
                downloadUrl = ref.downloadUrl.await().toString()
            } catch (e: Exception) {
                KeepsyLogger.w("FirebaseService: Download URL retrieval failed, retrying...")
                delay(2000)
                downloadUrl = ref.downloadUrl.await().toString()
            }
            
            return downloadUrl ?: throw Exception("Could not retrieve public URL from Storage")
        } catch (e: Exception) {
            KeepsyLogger.e("FirebaseService: uploadProfilePicture pipeline failed", e)
            throw e
        }
    }

    suspend fun deleteProfilePicture() {
        val uid = auth.currentUser?.uid ?: throw Exception("No user authenticated")
        KeepsyLogger.i("FirebaseService: deleteProfilePicture uid=$uid")
        updateProfile(null, null, "")
        try {
            val folderRef = storage.reference.child("users/$uid/profile/")
            val listResult = folderRef.listAll().await()
            for (item in listResult.items) {
                item.delete().await()
            }
            KeepsyLogger.d("FirebaseService: Old profile pictures deleted")
        } catch (e: Exception) {
            KeepsyLogger.w("FirebaseService: Storage cleanup failed (non-fatal)")
        }
    }

    suspend fun reloadUser(): Boolean {
        auth.currentUser?.reload()?.await()
        val isVerified = auth.currentUser?.isEmailVerified ?: false
        KeepsyLogger.i("FirebaseService: User reloaded, verified=$isVerified")
        return isVerified
    }

    fun isEmailVerified(): Boolean {
        return auth.currentUser?.isEmailVerified ?: false
    }

    suspend fun signInWithEmail(email: String, password: String): User {
        KeepsyLogger.i("FirebaseService: signInWithEmail $email")
        try {
            val result = auth.signInWithEmailAndPassword(email, password).await()
            val u = result.user ?: throw Exception("Firebase Auth returned null user")
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
        } catch (e: Exception) {
            KeepsyLogger.e("FirebaseService: signInWithEmail failed", e)
            throw e
        }
    }

    suspend fun signUpWithEmail(email: String, password: String, name: String): User {
        KeepsyLogger.i("FirebaseService: signUpWithEmail $email")
        try {
            val result = auth.createUserWithEmailAndPassword(email, password).await()
            val u = result.user ?: throw Exception("Firebase Auth registration failed")
            
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
        } catch (e: Exception) {
            KeepsyLogger.e("FirebaseService: signUpWithEmail failed", e)
            throw e
        }
    }

    suspend fun signInWithCredential(credential: AuthCredential): User {
        KeepsyLogger.i("FirebaseService: signInWithCredential")
        try {
            val result = auth.signInWithCredential(credential).await()
            val u = result.user ?: throw Exception("Firebase Credential sign in failed")
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
        } catch (e: Exception) {
            KeepsyLogger.e("FirebaseService: signInWithCredential failed", e)
            throw e
        }
    }

    private suspend fun updateUserProfile(user: User, isNewUser: Boolean = false) {
        try {
            val userRef = firestore.collection("users").document(user.uid)
            
            val updates = HashMap<String, Any?>()
            updates["uid"] = user.uid
            updates["email"] = user.email
            updates["lastLogin"] = System.currentTimeMillis()
            updates["platform"] = "Android"
            updates["appVersion"] = "1.2.6"

            if (isNewUser) {
                updates["createdAt"] = user.createdAt ?: System.currentTimeMillis()
                updates["name"] = user.name
                updates["displayName"] = user.name
                updates["photoUrl"] = user.photoUrl
            }

            userRef.set(updates, SetOptions.merge()).await()
            crashlytics.setUserId(user.uid)
            user.email?.let { crashlytics.setCustomKey("email", it) }
            KeepsyLogger.d("FirebaseService: Firestore session metadata updated")
        } catch (e: Exception) {
            KeepsyLogger.w("FirebaseService: Firestore user metadata update failed (non-fatal): ${e.message}")
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    fun getProfileFlow(): Flow<Map<String, Any?>?> {
        return authStateFlow().flatMapLatest { u ->
            if (u == null) {
                KeepsyLogger.d("FirebaseService: getProfileFlow u=null")
                flowOf(null)
            } else callbackFlow<Map<String, Any?>?> {
                KeepsyLogger.d("FirebaseService: Starting Profile snapshot listener for ${u.uid}")
                val listener = firestore.collection("users").document(u.uid)
                    .addSnapshotListener { snapshot, error ->
                        if (error != null) {
                            KeepsyLogger.e("FirebaseService: Snapshot listener error", error)
                            trySend(null)
                            return@addSnapshotListener
                        }
                        KeepsyLogger.d("FirebaseService: Snapshot received for ${u.uid}")
                        trySend(snapshot?.data)
                    }
                awaitClose { 
                    KeepsyLogger.d("FirebaseService: Removing snapshot listener")
                    listener.remove() 
                }
            }
        }.catch { e ->
            KeepsyLogger.e("FirebaseService: getProfileFlow global catch", e)
            emit(null)
        }
    }
    
    @OptIn(ExperimentalCoroutinesApi::class)
    private fun authStateFlow(): Flow<com.google.firebase.auth.FirebaseUser?> = callbackFlow {
        val listener = FirebaseAuth.AuthStateListener { trySend(it.currentUser) }
        auth.addAuthStateListener(listener)
        awaitClose { auth.removeAuthStateListener(listener) }
    }

    fun signOut() {
        KeepsyLogger.i("FirebaseService: signOut uid=${auth.currentUser?.uid}")
        auth.signOut()
    }

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
