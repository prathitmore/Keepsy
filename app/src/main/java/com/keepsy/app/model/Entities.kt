package com.keepsy.app.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.ForeignKey
import androidx.room.Index

@Entity(
    tableName = "spaces",
    indices = [Index(value = ["parentSpaceId"]), Index(value = ["name"]), Index(value = ["remoteId"])]
)
data class Space(
    @PrimaryKey(autoGenerate = true) val spaceId: Long = 0L,
    val parentSpaceId: Long? = null,
    val name: String,
    val description: String = "",
    val icon: String? = null,
    val photoPath: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val isFavorite: Boolean = false,
    val version: Int = 1,
    val syncState: String = "DIRTY",
    val isDeleted: Boolean = false,
    val lastSynced: Long? = null,
    val remoteId: String? = null,
    val parentRemoteId: String? = null
)

@Entity(
    tableName = "items",
    indices = [
        Index(value = ["name"]),
        Index(value = ["spaceId"]),
        Index(value = ["categoryId"]),
        Index(value = ["isFavorite"]),
        Index(value = ["lastViewed"]),
        Index(value = ["remoteId"])
    ]
)
data class Item(
    @PrimaryKey(autoGenerate = true) val itemId: Long = 0L,
    val name: String,
    val description: String = "",
    val spaceId: Long,
    val categoryId: Long,
    val photoPath: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val isFavorite: Boolean = false,
    val isDeleted: Boolean = false,
    val deletedAt: Long? = null,
    val lastViewed: Long? = null,
    val notes: String = "",
    val version: Int = 1,
    val syncState: String = "DIRTY",
    val lastSynced: Long? = null,
    val remoteId: String? = null,
    val spaceRemoteId: String? = null,
    val categoryRemoteId: String? = null
)

@Entity(
    tableName = "categories",
    indices = [Index(value = ["name"]), Index(value = ["remoteId"])]
)
data class Category(
    @PrimaryKey(autoGenerate = true) val categoryId: Long = 0L,
    val name: String,
    val icon: String? = null,
    val color: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val version: Int = 1,
    val syncState: String = "DIRTY",
    val isDeleted: Boolean = false,
    val lastSynced: Long? = null,
    val remoteId: String? = null
)

@Entity(
    tableName = "tags",
    indices = [Index(value = ["name"], unique = true), Index(value = ["remoteId"])]
)
data class Tag(
    @PrimaryKey(autoGenerate = true) val tagId: Long = 0L,
    val name: String,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val version: Int = 1,
    val syncState: String = "DIRTY",
    val isDeleted: Boolean = false,
    val lastSynced: Long? = null,
    val remoteId: String? = null
)

@Entity(
    tableName = "item_tag_cross_ref",
    primaryKeys = ["itemId", "tagId"],
    indices = [Index(value = ["tagId"])]
)
data class ItemTagCrossRef(
    val itemId: Long,
    val tagId: Long
)

@Entity(
    tableName = "activity_logs",
    indices = [Index(value = ["itemId"]), Index(value = ["timestamp"]), Index(value = ["remoteId"])]
)
data class ActivityLog(
    @PrimaryKey(autoGenerate = true) val activityId: Long = 0L,
    val itemId: Long,
    val itemName: String,
    val actionType: String, // "CREATED", "UPDATED", "MOVED", "DELETED", "RESTORED", "VIEWED"
    val timestamp: Long = System.currentTimeMillis(),
    val details: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val version: Int = 1,
    val syncState: String = "DIRTY",
    val isDeleted: Boolean = false,
    val lastSynced: Long? = null,
    val remoteId: String? = null
)
