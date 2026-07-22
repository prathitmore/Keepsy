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
import com.keepsy.app.model.User
import com.keepsy.app.utils.KeepsyLogger
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeout
import java.util.HashMap
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.io.OutputStream

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

    suspend fun getProfileDocument(): Map<String, Any?>? {
        val uid = auth.currentUser?.uid ?: return null
        return try {
            firestore.collection("users").document(uid).get(com.google.firebase.firestore.Source.SERVER).await().data
        } catch (e: Exception) {
            firestore.collection("users").document(uid).get().await().data
        }
    }

    suspend fun updateProfile(name: String?, displayName: String?, photoUrl: String?) {
        val user = auth.currentUser ?: throw Exception("No authenticated session")
        val profileUpdates = UserProfileChangeRequest.Builder()
        if (name != null) profileUpdates.setDisplayName(name)
        if (photoUrl != null && photoUrl != "") { profileUpdates.setPhotoUri(Uri.parse(photoUrl)) }
        try {
            user.updateProfile(profileUpdates.build()).await()
            user.reload().await()
        } catch (e: Exception) { KeepsyLogger.e("Auth sync failed", e) }
        val updates = HashMap<String, Any?>()
        if (name != null) updates["profile_name"] = name
        if (displayName != null) updates["profile_display_name"] = displayName
        if (photoUrl != null) updates["profile_photo_url"] = photoUrl
        if (updates.isNotEmpty()) { firestore.collection("users").document(user.uid).set(updates, SetOptions.merge()).await() }
    }

    suspend fun uploadProfilePicture(uri: Uri): String { return uploadImageInternal(uri, "profile", "avatar") }
    suspend fun uploadEntityImage(uri: Uri, collection: String, entityName: String): String { return uploadImageInternal(uri, collection, entityName) }

    private suspend fun uploadImageInternal(uri: Uri, folder: String, prefix: String): String {
        val uid = auth.currentUser?.uid ?: throw Exception("Not logged in")
        val timestamp = System.currentTimeMillis()
        val fileName = prefix + "_" + timestamp.toString() + ".jpg"
        
        val context = com.keepsy.app.KeepsyApplication.instance
        val tempFile = File(context.cacheDir, "staging.jpg")
        
        try {
            val input = context.contentResolver.openInputStream(uri) ?: throw Exception("No input")
            val output = FileOutputStream(tempFile)
            val buffer = ByteArray(16384)
            var bytesRead: Int
            while (input.read(buffer).also { bytesRead = it } != -1) {
                output.write(buffer, 0, bytesRead)
            }
            output.flush()
            output.close()
            input.close()
        } catch (e: Exception) {
            throw Exception("Local copy failed: " + e.message)
        }

        // NO LEADING SLASHES. ONLY RELATIVE.
        val ref = storage.reference.child("users").child(uid).child(folder).child(fileName)

        KeepsyLogger.i("FirebaseService: V9.2 - Final Standard Path Logic")
        KeepsyLogger.i("Target Bucket: " + ref.bucket)
        KeepsyLogger.i("Target Path: " + ref.path)

        try {
            // Use putFile which is the most stable for large objects and resumable sessions
            withTimeout(60000L) {
                ref.putFile(Uri.fromFile(tempFile)).await()
            }
            
            KeepsyLogger.i("FirebaseService: PutFile successful. Polling...")
            
            var downloadUrl: String? = null
            for (i in 1..20) {
                try {
                    delay(1500L)
                    val url = ref.downloadUrl.await().toString()
                    if (url != "") {
                        downloadUrl = url
                        break
                    }
                } catch (e: Exception) { }
            }
            
            if (tempFile.exists()) tempFile.delete()
            return downloadUrl ?: throw Exception("Timeout generating link")

        } catch (e: Exception) {
            KeepsyLogger.e("FirebaseService: PutFile Failed: " + e.message)
            if (tempFile.exists()) tempFile.delete()
            throw e
        }
    }

    suspend fun deleteProfilePicture() {
        val uid = auth.currentUser?.uid ?: throw Exception("No user")
        updateProfile(null, null, "")
        try {
            val res = storage.reference.child("users/$uid/profile/").listAll().await()
            val items = res.items
            for (i in 0..items.size - 1) { items[i].delete().await() }
        } catch (e: Exception) { }
    }

    suspend fun reloadUser(): Boolean { auth.currentUser?.reload()?.await(); return auth.currentUser?.isEmailVerified ?: false }
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
        u.updateProfile(UserProfileChangeRequest.Builder().setDisplayName(name).build()).await()
        u.sendEmailVerification().await()
        val user = User(u.uid, name, u.email, isAnonymous = u.isAnonymous, isEmailVerified = u.isEmailVerified, createdAt = System.currentTimeMillis())
        updateUserProfile(user, isNewUser = true)
        return user
    }

    suspend fun signUpWithCredential(credential: AuthCredential): User {
        val res = auth.signInWithCredential(credential).await()
        val u = res.user ?: throw Exception("Login fail")
        val isNew = res.additionalUserInfo?.isNewUser ?: false
        val user = User(u.uid, u.displayName, u.email, u.photoUrl?.toString(), isAnonymous = u.isAnonymous, isEmailVerified = u.isEmailVerified, createdAt = if (isNew) System.currentTimeMillis() else null)
        updateUserProfile(user, isNewUser = isNew)
        return user
    }
    
    suspend fun signInWithCredential(credential: AuthCredential): User {
        return signUpWithCredential(credential)
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
        } catch (e: Exception) { }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    fun getProfileFlow(): Flow<Map<String, Any?>?> {
        return authStateFlow().flatMapLatest { u ->
            if (u == null) flowOf(null)
            else callbackFlow {
                val listener = firestore.collection("users").document(u.uid)
                    .addSnapshotListener { snapshot, error ->
                        if (error != null) { trySend(null); return@addSnapshotListener }
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
        user.reauthenticate(com.google.firebase.auth.EmailAuthProvider.getCredential(user.email!!, current)).await()
        user.updatePassword(new).await()
    }

    fun addAuthStateListener(listener: (User?) -> Unit) {
        auth.addAuthStateListener { firebaseAuth ->
            val u = firebaseAuth.currentUser
            listener(u?.let { User(it.uid, it.displayName, it.email, it.photoUrl?.toString(), isAnonymous = it.isAnonymous, isEmailVerified = it.isEmailVerified) })
        }
    }
}
