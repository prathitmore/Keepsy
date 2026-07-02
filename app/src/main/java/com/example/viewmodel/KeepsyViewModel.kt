package com.example.viewmodel

import android.app.Application
import android.net.Uri
import android.widget.Toast
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.database.AppDatabase
import com.example.model.*
import com.example.repository.BackupManager
import com.example.repository.KeepsyRepository
import com.example.repository.SettingsManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

class KeepsyViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getDatabase(application)
    private val repository = KeepsyRepository(db.appDao())
    private val settingsManager = SettingsManager(application)
    private val backupManager = BackupManager(application, db.appDao())

    // App Preferences
    val isOnboardingCompleted = settingsManager.isOnboardingCompleted
    val darkModePreference = settingsManager.darkModePreference

    // Database source streams
    val spaces = repository.spaces
    val categories = repository.categories
    val tags = repository.tags
    val activityLogs = repository.activityLogs
    val activeItems = repository.activeItemsWithDetails
    val trashItems = repository.trashItemsWithDetails

    // Search query backing
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery

    // Home Screen Sections
    val recentItems = activeItems.map { list ->
        list.sortedByDescending { it.item.createdAt }.take(10)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val recentlyViewedItems = activeItems.map { list ->
        list.filter { it.item.lastViewed != null }
            .sortedByDescending { it.item.lastViewed }
            .take(15)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val favoriteSpaces = spaces.map { list ->
        list.filter { it.isFavorite }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val favoriteItems = activeItems.map { list ->
        list.filter { it.item.isFavorite }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Statistics state
    val appStatistics = combine(activeItems, spaces, categories) { items, spacesList, categoriesList ->
        val totalCount = items.size
        val spacesCount = spacesList.size
        val categoriesCount = categoriesList.size
        val favoritesCount = items.count { it.item.isFavorite }
        val trashCount = db.appDao().getLiveTrashItems().first().size // fallback query
        Stats(totalCount, spacesCount, categoriesCount, favoritesCount, trashCount)
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        Stats(0, 0, 0, 0, 0)
    )

    // Reactive Instant Search (sub-100ms)
    val searchResults: StateFlow<SearchResult> = combine(
        searchQuery,
        activeItems,
        spaces
    ) { query, items, spacesList ->
        if (query.trim().isBlank()) {
            SearchResult(emptyList(), emptyList())
        } else {
            val q = query.trim().lowercase()

            val filteredItems = items.filter { details ->
                details.item.name.lowercase().contains(q) ||
                details.item.description.lowercase().contains(q) ||
                details.item.notes.lowercase().contains(q) ||
                (details.space?.name?.lowercase()?.contains(q) ?: false) ||
                (details.category?.name?.lowercase()?.contains(q) ?: false)
            }

            val filteredSpaces = spacesList.filter { space ->
                space.name.lowercase().contains(q) ||
                space.description.lowercase().contains(q)
            }

            SearchResult(filteredItems, filteredSpaces)
        }
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        SearchResult(emptyList(), emptyList())
    )

    // Selected Detail States for reactive presentation
    private val _selectedItem = MutableStateFlow<ItemWithDetails?>(null)
    val selectedItem: StateFlow<ItemWithDetails?> = _selectedItem

    private val _selectedSpace = MutableStateFlow<SpaceWithParent?>(null)
    val selectedSpace: StateFlow<SpaceWithParent?> = _selectedSpace

    private val _nestedSubspaces = MutableStateFlow<List<Space>>(emptyList())
    val nestedSubspaces: StateFlow<List<Space>> = _nestedSubspaces

    private val _itemsInSpace = MutableStateFlow<List<ItemWithDetails>>(emptyList())
    val itemsInSpace: StateFlow<List<ItemWithDetails>> = _itemsInSpace

    init {
        viewModelScope.launch {
            repository.seedDefaultCategoriesIfEmpty()
        }
    }

    // --- SEARCH HELPERS ---
    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    suspend fun getItemWithDetails(itemId: Long): ItemWithDetails? {
        return repository.getItemWithDetails(itemId)
    }

    // --- APP PREFERENCE ACTIONS ---
    fun setOnboardingCompleted() {
        settingsManager.setOnboardingCompleted(true)
    }

    fun setDarkModePreference(dark: Boolean?) {
        settingsManager.setDarkModePreference(dark)
    }

    fun resetApp() {
        viewModelScope.launch(Dispatchers.IO) {
            settingsManager.resetSettings()
            db.clearAllTables()
            repository.seedDefaultCategoriesIfEmpty()
        }
    }

    // --- ITEM ACTIONS ---
    fun selectItem(itemId: Long) {
        viewModelScope.launch {
            val details = repository.getItemWithDetails(itemId)
            _selectedItem.value = details
            if (details != null) {
                repository.trackItemViewed(itemId)
            }
        }
    }

    fun saveItem(
        itemId: Long,
        name: String,
        description: String,
        spaceId: Long,
        categoryId: Long,
        notes: String,
        photoUri: Uri?,
        tagList: List<String>,
        isFavorite: Boolean = false,
        onSuccess: () -> Unit
    ) {
        viewModelScope.launch {
            val finalPhotoPath = if (photoUri != null) {
                repository.copyImageToAppStorage(getApplication(), photoUri)
            } else {
                null
            }

            val existingItem = if (itemId != 0L) repository.getItemById(itemId) else null
            val photoToSave = finalPhotoPath ?: existingItem?.photoPath

            val itemToSave = Item(
                itemId = itemId,
                name = name,
                description = description,
                spaceId = spaceId,
                categoryId = categoryId,
                photoPath = photoToSave,
                isFavorite = isFavorite,
                notes = notes,
                createdAt = existingItem?.createdAt ?: System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis()
            )

            repository.saveItem(itemToSave, tagList)
            onSuccess()
        }
    }

    fun toggleItemFavorite(itemId: Long) {
        viewModelScope.launch {
            val item = repository.getItemById(itemId)
            if (item != null) {
                val updated = item.copy(isFavorite = !item.isFavorite, updatedAt = System.currentTimeMillis())
                db.appDao().updateItem(updated)
                // Refresh selection state
                _selectedItem.value = repository.getItemWithDetails(itemId)
            }
        }
    }

    fun moveItem(itemId: Long, newSpaceId: Long, reason: String, onSuccess: () -> Unit) {
        viewModelScope.launch {
            repository.moveItem(itemId, newSpaceId, reason)
            // Refresh detailed states
            _selectedItem.value = repository.getItemWithDetails(itemId)
            onSuccess()
        }
    }

    fun softDeleteSelectedItem(onSuccess: () -> Unit) {
        val currentId = _selectedItem.value?.item?.itemId ?: return
        viewModelScope.launch {
            repository.softDeleteItem(currentId)
            _selectedItem.value = null
            onSuccess()
        }
    }

    fun restoreItem(itemId: Long) {
        viewModelScope.launch {
            repository.restoreItem(itemId)
        }
    }

    fun permanentlyDeleteItem(itemId: Long) {
        viewModelScope.launch {
            repository.permanentlyDeleteItem(itemId)
        }
    }

    // --- SPACE ACTIONS ---
    fun selectSpace(spaceId: Long) {
        viewModelScope.launch {
            val space = repository.getSpaceById(spaceId)
            if (space != null) {
                val parentSpace = space.parentSpaceId?.let { repository.getSpaceById(it) }
                _selectedSpace.value = SpaceWithParent(space, parentSpace)
                
                // Get subspaces
                val sub = db.appDao().getSubspaces(spaceId)
                _nestedSubspaces.value = sub

                // Get items in this space
                val items = db.appDao().getItemsInSpace(spaceId)
                val allSpaces = db.appDao().getLiveSpaces().first()
                val allCategories = db.appDao().getLiveCategories().first()
                
                _itemsInSpace.value = items.map { item ->
                    val spaceRef = allSpaces.find { it.spaceId == item.spaceId }
                    val catRef = allCategories.find { it.categoryId == item.categoryId }
                    ItemWithDetails(item, spaceRef, catRef, emptyList())
                }
            }
        }
    }

    fun saveSpace(
        spaceId: Long,
        name: String,
        description: String,
        parentSpaceId: Long?,
        icon: String?,
        photoUri: Uri?,
        isFavorite: Boolean = false,
        onSuccess: () -> Unit
    ) {
        viewModelScope.launch {
            val finalPhotoPath = if (photoUri != null) {
                repository.copyImageToAppStorage(getApplication(), photoUri)
            } else {
                null
            }

            val existingSpace = if (spaceId != 0L) repository.getSpaceById(spaceId) else null
            val photoToSave = finalPhotoPath ?: existingSpace?.photoPath

            val spaceToSave = Space(
                spaceId = spaceId,
                parentSpaceId = parentSpaceId,
                name = name,
                description = description,
                icon = icon,
                photoPath = photoToSave,
                isFavorite = isFavorite,
                createdAt = existingSpace?.createdAt ?: System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis()
            )

            if (spaceId == 0L) {
                repository.insertSpace(spaceToSave)
            } else {
                repository.updateSpace(spaceToSave)
            }
            onSuccess()
        }
    }

    fun deleteSpace(spaceId: Long, onSuccess: () -> Unit) {
        viewModelScope.launch {
            repository.deleteSpace(spaceId)
            _selectedSpace.value = null
            onSuccess()
        }
    }

    fun toggleSpaceFavorite(spaceId: Long) {
        viewModelScope.launch {
            val space = repository.getSpaceById(spaceId)
            if (space != null) {
                val updated = space.copy(isFavorite = !space.isFavorite, updatedAt = System.currentTimeMillis())
                repository.updateSpace(updated)
                // Refresh detail
                _selectedSpace.value = SpaceWithParent(updated, updated.parentSpaceId?.let { repository.getSpaceById(it) })
            }
        }
    }

    // --- LOCAL BACKUP ACTIONS ---
    fun exportBackup(onExported: (String) -> Unit) {
        viewModelScope.launch {
            try {
                val backupJson = backupManager.exportBackupJson()
                val backupFile = File(getApplication<Application>().cacheDir, "keepsy_backup.json")
                FileOutputStream(backupFile).use { it.write(backupJson.toByteArray()) }
                onExported(backupJson)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun importBackup(jsonString: String, onCompleted: (Boolean) -> Unit) {
        viewModelScope.launch {
            val success = backupManager.importBackupJson(jsonString)
            if (success) {
                // Seed category fallback just in case
                repository.seedDefaultCategoriesIfEmpty()
            }
            onCompleted(success)
        }
    }

    fun getActivityTrailForItem(itemId: Long): Flow<List<ActivityLog>> {
        return repository.getActivityTrailForItem(itemId)
    }

    data class Stats(
        val totalItems: Int,
        val totalSpaces: Int,
        val totalCategories: Int,
        val favoriteItemsCount: Int,
        val trashItemsCount: Int
    )
}
