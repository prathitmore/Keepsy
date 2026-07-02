package com.keepsy.app.sync

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.tasks.await
import com.keepsy.app.utils.KeepsyLogger

class FirestoreService {
    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private val TAG = "KeepsySync"

    private val userDoc get() = auth.currentUser?.uid?.let { 
        firestore.collection("users").document(it) 
    }

    suspend fun uploadEntity(collection: String, remoteId: String, data: Map<String, Any?>) {
        KeepsyLogger.d("Firestore: Uploading $remoteId to $collection")
        val doc = userDoc?.collection(collection)?.document(remoteId) ?: return
        doc.set(data, SetOptions.merge()).await()
    }

    suspend fun downloadEntities(collection: String, lastSynced: Long): List<Map<String, Any?>> {
        val coll = userDoc?.collection(collection) ?: return emptyList()
        val snapshot = coll.whereGreaterThan("updatedAt", lastSynced).get().await()
        return snapshot.documents.mapNotNull { doc ->
            val data = doc.data
            if (data != null) {
                val mutableData = java.util.HashMap(data)
                mutableData["remoteId"] = doc.id
                mutableData
            } else null
        }
    }

    suspend fun deleteEntity(collection: String, remoteId: String) {
        val doc = userDoc?.collection(collection)?.document(remoteId) ?: return
        doc.delete().await()
    }
    
    suspend fun getAllEntities(collection: String): List<Map<String, Any?>> {
        val coll = userDoc?.collection(collection) ?: return emptyList()
        val snapshot = coll.get().await()
        return snapshot.documents.mapNotNull { doc ->
            val data = doc.data
            if (data != null) {
                val mutableData = java.util.HashMap(data)
                mutableData["remoteId"] = doc.id
                mutableData
            } else null
        }
    }

    suspend fun getProfile(): Map<String, Any?>? {
        val doc = userDoc?.get()?.await() ?: return null
        return doc.data
    }

    suspend fun updateProfile(data: Map<String, Any?>) {
        userDoc?.set(data, SetOptions.merge())?.await()
    }
}
