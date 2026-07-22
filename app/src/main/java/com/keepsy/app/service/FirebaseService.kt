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
    private val storage = FirebaseStorage.getInstance("gs://keepsy-project.firebasestorage.app")
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

    suspend fun getProfileDocument(): Map<String, Any?>? {
        val uid = auth.currentUser?.uid ?: return null
        return try {
            firestore.collection("users").document(uid).get(com.google.firebase.firestore.Source.SERVER).await().data
        } catch (e: Exception) {
            firestore.collection("users").document(uid).get().await().data
        }
    }

    suspend fun updateProfile(name: String?, displayName: String?, photoUrl: String?) {
        val user = auth.currentUser ?: throw Exception("No userauthenticated")
        
        val profileUpdates = UserProfileChangeRequest.Builder()
        if (name != null) profileUpdates.setDisplayName(name)
        if (photoUrl != null && photoUrl != "") {
            profileUpdates.setPhotoUri(Uri.parse(photoUrl))
        }
        
        try {
            user.updateProfile(profileUpdates.build()).await()
            user.reload().await()
        } catch (e: Exception) {
            KeepsyLogger.e("FirebaseService: Auth sync failed", e)
        }
        
        val updates = HashMap<String, Any?>()
        if (name != null) updates["profile_name"] = name
        if (displayName != null) updates["profile_display_name"] = displayName
        if (photoUrl != null) updates["profile_photo_url"] = photoUrl
        
        if (updates.isNotEmpty()) {
            firestore.collection("users").document(user.uid).set(updates, SetOptions.merge()).await()
        }
    }

    suspend fun uploadProfilePicture(uri: Uri): String {
        return uploadImageInternal(uri, "profile", "avatar")
    }

    suspend fun uploadEntityImage(uri: Uri, collection: String, entityName: String): String {
        return uploadImageInternal(uri, collection, entityName)
    }

    private suspend fun uploadImageInternal(uri: Uri, folder: String, prefix: String): String {
        val uid = auth.currentUser?.uid ?: throw Exception("No session")
        val timestamp = System.currentTimeMillis()
        val storagePath = "users/$uid/$folder/${prefix}_$timestamp.jpg"
        val ref = storage.reference.child(storagePath)
        
        try {
            val context = com.keepsy.app.KeepsyApplication.instance
            val outputStream = java.io.ByteArrayOutputStream()
            val buffer = ByteArray(8192)
            
            val inputStream = if (uri.scheme == "content") {
                context.contentResolver.openInputStream(uri)
            } else {
                java.io.FileInputStream(java.io.File(uri.path ?: ""))
            } ?: throw Exception("Stream null")
            
            var bytesRead: Int
            while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                outputStream.write(buffer, 0, bytesRead)
            }
            val bytes = outputStream.toByteArray()
            inputStream.close()

            val metadata = StorageMetadata.Builder()
                .setContentType("image/jpeg")
                .setCustomMetadata("uid", uid)
                .build()

            KeepsyLogger.i("FirebaseService: Pushing ${bytes.size} bytes.")
            ref.putBytes(bytes, metadata).await()
            
            var downloadUrl: String? = null
            for (attempt in 1..10) {
                try {
                    delay(1200L * attempt)
                    val url = ref.downloadUrl.await().toString()
                    if (url != "") {
                        downloadUrl = url
                        break
                    }
                } catch (e: Exception) { }
            }
            
            return downloadUrl ?: throw Exception("Indexing delay.")
        } catch (e: Exception) {
            KeepsyLogger.e("FirebaseService: Image pipeline fail", e)
            throw e
        }
    }

    suspend fun deleteProfilePicture() {
        val uid = auth.currentUser?.uid ?: throw Exception("No user")
        updateProfile(null, null, "")
        try {
            val res = storage.reference.child("users/$uid/profile/").listAll().await()
            for (i in 0..res.items.size - 1) {
                res.items[i].delete().await()
            }
        } catch (e: Exception) { }
    }

    suspend fun reloadUser(): Boolean {
        auth.currentUser?.reload()?.await()
        return auth.currentUser?.isEmailVerified ?: false
    }

    fun isEmailVerified(): Boolean = auth.currentUser?.isEmailVerified ?: false

    suspend fun signInWithEmail(email: String, password: String): User {
        val res = auth.signInWithEmailAndPassword(email, password).await()
        val u = res.user ?: throw Exception("Login fail")
        val user = User(u.uid, u.displayName, u.email, u.photoUrl?.toString(), isAnonymous = u.isAnonymous, isEmailVerified = u.isEmailVerified)
        updateUserProfile(user)
        return user
    }

    suspend fun signUpWithEmail(email: String, password: String, name: String): User {
        val res = auth.createUserWithEmailAndPassword(email, password).await()
        val u = res.user ?: throw Exception("Reg fail")
        val profileUpdates = UserProfileChangeRequest.Builder().setDisplayName(name).build()
        u.updateProfile(profileUpdates).await()
        u.sendEmailVerification().await()
        val user = User(u.uid, name, u.email, isAnonymous = u.isAnonymous, isEmailVerified = u.isEmailVerified, createdAt = System.currentTimeMillis())
        updateUserProfile(user, isNewUser = true)
        return user
    }

    suspend fun signInWithCredential(credential: AuthCredential): User {
        val res = auth.signInWithCredential(credential).await()
        val u = res.user ?: throw Exception("Login fail")
        val isNew = res.additionalUserInfo?.isNewUser ?: false
        val user = User(u.uid, u.displayName, u.email, u.photoUrl?.toString(), isAnonymous = u.isAnonymous, isEmailVerified = u.isEmailVerified, createdAt = if (isNew) System.currentTimeMillis() else null)
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
            if (isNewUser) {
                updates["createdAt"] = user.createdAt ?: System.currentTimeMillis()
                updates["profile_name"] = user.name
                updates["profile_display_name"] = user.name
                updates["profile_photo_url"] = user.photoUrl
            }
            userRef.set(updates, SetOptions.merge()).await()
            crashlytics.setUserId(user.uid)
        } catch (e: Exception) { }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    fun getProfileFlow(): Flow<Map<String, Any?>?> {
        return authStateFlow().flatMapLatest { u ->
            if (u == null) flowOf(null)
            else callbackFlow {
                val listener = firestore.collection("users").document(u.uid)
                    .addSnapshotListener { snapshot, error ->
                        if (error != null) {
                            trySend(null)
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

    suspend fun changePassword(current: String, new: String) {
        val user = auth.currentUser ?: throw Exception("No user")
        val cred = com.google.firebase.auth.EmailAuthProvider.getCredential(user.email!!, current)
        user.reauthenticate(cred).await()
        user.updatePassword(new).await()
    }

    fun addAuthStateListener(listener: (User?) -> Unit) {
        auth.addAuthStateListener { firebaseAuth ->
            val u = firebaseAuth.currentUser
            listener(u?.let { User(it.uid, it.displayName, it.email, it.photoUrl?.toString(), isAnonymous = it.isAnonymous, isEmailVerified = it.isEmailVerified) })
        }
    }
}
