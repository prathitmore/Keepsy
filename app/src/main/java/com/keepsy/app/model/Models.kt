package com.keepsy.app.model

data class ItemWithDetails(
    val item: Item,
    val space: Space?,
    val category: Category?,
    val tags: List<Tag> = emptyList()
)

data class SpaceWithParent(
    val space: Space,
    val parentSpace: Space?
)

data class SearchResult(
    val items: List<ItemWithDetails>,
    val spaces: List<Space>
)

data class User(
    val uid: String,
    val name: String?,
    val email: String?,
    val photoUrl: String? = null,
    val createdAt: Long? = null,
    val lastLogin: Long? = null,
    val appVersion: String? = null,
    val platform: String = "Android",
    val isAnonymous: Boolean = false,
    val isEmailVerified: Boolean = false
)

data class UserProfile(
    val uid: String,
    val name: String,
    val email: String,
    val photoUrl: String?,
    val planType: String = "Free Plan",
    val memberSince: Long,
    val lastSyncAt: Long?,
    val totalItems: Int,
    val totalSpaces: Int,
    val storageUsed: String = "0.0 MB",
    val syncEnabled: Boolean = true
)

sealed class AuthState {
    object Idle : AuthState()
    object Loading : AuthState()
    data class Authenticated(val user: User) : AuthState()
    data class Error(val message: String) : AuthState()
    object Unauthenticated : AuthState()
}

enum class SyncState {
    IDLE,
    SYNCING,
    UPLOADING,
    DOWNLOADING,
    COMPLETED,
    FAILED,
    WAITING_FOR_INTERNET,
    DIRTY,
    DELETED_PENDING_SYNC,
    SYNCED
}
