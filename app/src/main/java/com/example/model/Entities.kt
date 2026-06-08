package com.example.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.ForeignKey
import androidx.room.Index

@Entity(
    tableName = "spaces",
    foreignKeys = [
        ForeignKey(
            entity = Space::class,
            parentColumns = ["spaceId"],
            childColumns = ["parentSpaceId"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [Index(value = ["parentSpaceId"]), Index(value = ["name"])]
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
    val isFavorite: Boolean = false
)

@Entity(
    tableName = "items",
    indices = [
        Index(value = ["name"]),
        Index(value = ["spaceId"]),
        Index(value = ["categoryId"]),
        Index(value = ["isFavorite"]),
        Index(value = ["lastViewed"])
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
    val notes: String = ""
)

@Entity(
    tableName = "categories",
    indices = [Index(value = ["name"])]
)
data class Category(
    @PrimaryKey(autoGenerate = true) val categoryId: Long = 0L,
    val name: String,
    val icon: String? = null,
    val color: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "tags",
    indices = [Index(value = ["name"], unique = true)]
)
data class Tag(
    @PrimaryKey(autoGenerate = true) val tagId: Long = 0L,
    val name: String,
    val createdAt: Long = System.currentTimeMillis()
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
    indices = [Index(value = ["itemId"]), Index(value = ["timestamp"])]
)
data class ActivityLog(
    @PrimaryKey(autoGenerate = true) val activityId: Long = 0L,
    val itemId: Long,
    val itemName: String,
    val actionType: String, // "CREATED", "UPDATED", "MOVED", "DELETED", "RESTORED", "VIEWED"
    val timestamp: Long = System.currentTimeMillis(),
    val details: String = ""
)
