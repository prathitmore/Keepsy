package com.keepsy.app.sync

import com.keepsy.app.model.*
import java.util.HashMap

fun Space.toMap(): Map<String, Any?> {
    val map = HashMap<String, Any?>()
    map["parentRemoteId"] = parentRemoteId
    map["name"] = name
    map["description"] = description
    map["icon"] = icon
    map["photoPath"] = photoPath
    map["createdAt"] = createdAt
    map["updatedAt"] = updatedAt
    map["isFavorite"] = isFavorite
    map["version"] = version
    map["isDeleted"] = isDeleted
    return map
}

fun Item.toMap(): Map<String, Any?> {
    val map = HashMap<String, Any?>()
    map["name"] = name
    map["description"] = description
    map["spaceRemoteId"] = spaceRemoteId
    map["categoryRemoteId"] = categoryRemoteId
    map["photoPath"] = photoPath
    map["createdAt"] = createdAt
    map["updatedAt"] = updatedAt
    map["isFavorite"] = isFavorite
    map["isDeleted"] = isDeleted
    map["deletedAt"] = deletedAt
    map["lastViewed"] = lastViewed
    map["notes"] = notes
    map["version"] = version
    return map
}

fun Category.toMap(): Map<String, Any?> {
    val map = HashMap<String, Any?>()
    map["name"] = name
    map["icon"] = icon
    map["color"] = color
    map["createdAt"] = createdAt
    map["updatedAt"] = updatedAt
    map["version"] = version
    map["isDeleted"] = isDeleted
    return map
}

fun Tag.toMap(): Map<String, Any?> {
    val map = HashMap<String, Any?>()
    map["name"] = name
    map["createdAt"] = createdAt
    map["updatedAt"] = updatedAt
    map["version"] = version
    map["isDeleted"] = isDeleted
    return map
}

fun ActivityLog.toMap(): Map<String, Any?> {
    val map = HashMap<String, Any?>()
    map["itemId"] = itemId
    map["itemName"] = itemName
    map["actionType"] = actionType
    map["timestamp"] = timestamp
    map["details"] = details
    map["createdAt"] = createdAt
    map["updatedAt"] = updatedAt
    map["version"] = version
    map["isDeleted"] = isDeleted
    return map
}

fun Map<String, Any?>.toSpace(remoteId: String): Space = Space(
    name = this["name"]?.toString() ?: "Unnamed Space",
    description = this["description"]?.toString() ?: "",
    icon = this["icon"]?.toString(),
    photoPath = this["photoPath"]?.toString(),
    createdAt = (this["createdAt"] as? Number)?.toLong() ?: System.currentTimeMillis(),
    updatedAt = (this["updatedAt"] as? Number)?.toLong() ?: System.currentTimeMillis(),
    isFavorite = this["isFavorite"] as? Boolean ?: false,
    version = (this["version"] as? Number)?.toInt() ?: 1,
    isDeleted = this["isDeleted"] as? Boolean ?: false,
    syncState = "SYNCED",
    lastSynced = System.currentTimeMillis(),
    remoteId = remoteId,
    parentRemoteId = this["parentRemoteId"]?.toString()
)

fun Map<String, Any?>.toItem(remoteId: String): Item = Item(
    name = this["name"]?.toString() ?: "Unnamed Item",
    description = this["description"]?.toString() ?: "",
    spaceId = 0L, // Will be resolved during merge
    categoryId = 0L, // Will be resolved during merge
    photoPath = this["photoPath"]?.toString(),
    createdAt = (this["createdAt"] as? Number)?.toLong() ?: System.currentTimeMillis(),
    updatedAt = (this["updatedAt"] as? Number)?.toLong() ?: System.currentTimeMillis(),
    isFavorite = this["isFavorite"] as? Boolean ?: false,
    isDeleted = this["isDeleted"] as? Boolean ?: false,
    deletedAt = (this["deletedAt"] as? Number)?.toLong(),
    lastViewed = (this["lastViewed"] as? Number)?.toLong(),
    notes = this["notes"]?.toString() ?: "",
    version = (this["version"] as? Number)?.toInt() ?: 1,
    syncState = "SYNCED",
    lastSynced = System.currentTimeMillis(),
    remoteId = remoteId,
    spaceRemoteId = this["spaceRemoteId"]?.toString(),
    categoryRemoteId = this["categoryRemoteId"]?.toString()
)

fun Map<String, Any?>.toCategory(remoteId: String): Category = Category(
    name = this["name"]?.toString() ?: "General",
    icon = this["icon"]?.toString(),
    color = this["color"]?.toString(),
    createdAt = (this["createdAt"] as? Number)?.toLong() ?: System.currentTimeMillis(),
    updatedAt = (this["updatedAt"] as? Number)?.toLong() ?: System.currentTimeMillis(),
    version = (this["version"] as? Number)?.toInt() ?: 1,
    syncState = "SYNCED",
    isDeleted = this["isDeleted"] as? Boolean ?: false,
    lastSynced = System.currentTimeMillis(),
    remoteId = remoteId
)

fun Map<String, Any?>.toTag(remoteId: String): Tag = Tag(
    name = this["name"]?.toString() ?: "tag",
    createdAt = (this["createdAt"] as? Number)?.toLong() ?: System.currentTimeMillis(),
    updatedAt = (this["updatedAt"] as? Number)?.toLong() ?: System.currentTimeMillis(),
    version = (this["version"] as? Number)?.toInt() ?: 1,
    syncState = "SYNCED",
    isDeleted = this["isDeleted"] as? Boolean ?: false,
    lastSynced = System.currentTimeMillis(),
    remoteId = remoteId
)

fun Map<String, Any?>.toActivityLog(remoteId: String): ActivityLog = ActivityLog(
    itemId = 0L, // Will be resolved if needed
    itemName = this["itemName"]?.toString() ?: "Activity",
    actionType = this["actionType"]?.toString() ?: "VIEWED",
    timestamp = (this["timestamp"] as? Number)?.toLong() ?: System.currentTimeMillis(),
    details = this["details"]?.toString() ?: "",
    createdAt = (this["createdAt"] as? Number)?.toLong() ?: System.currentTimeMillis(),
    updatedAt = (this["updatedAt"] as? Number)?.toLong() ?: System.currentTimeMillis(),
    version = (this["version"] as? Number)?.toInt() ?: 1,
    syncState = "SYNCED",
    isDeleted = this["isDeleted"] as? Boolean ?: false,
    lastSynced = System.currentTimeMillis(),
    remoteId = remoteId
)
