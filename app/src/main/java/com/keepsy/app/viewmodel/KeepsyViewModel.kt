package com.keepsy.app.viewmodel

import android.app.Application
import android.net.Uri
import android.widget.Toast
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.keepsy.app.database.AppDatabase
import com.keepsy.app.model.*
import com.google.firebase.analytics.FirebaseAnalytics
import com.keepsy.app.repository.BackupManager
import com.keepsy.app.repository.KeepsyRepository
import com.keepsy.app.repository.SettingsManager
import com.keepsy.app.service.FirebaseService
import com.keepsy.app.sync.FirestoreService
import com.keepsy.app.sync.SyncManager
import com.keepsy.app.sync.SyncRepository
import com.keepsy.app.ui.tutorial.TutorialStep
import com.keepsy.app.ui.tutorial.TutorialViewModel
import com.keepsy.app.monetization.MonetizationProvider
import com.keepsy.app.monetization.MonetizationRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import com.keepsy.app.service.SecurityService
import com.keepsy.app.utils.ErrorHandler
import com.keepsy.app.utils.KeepsyError
import com.keepsy.app.utils.KeepsyLogger
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

class KeepsyViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getDatabase(application)
    private val analytics = FirebaseAnalytics.getInstance(application)
    private val securityService = SecurityService(application)
    private val firebaseService = FirebaseService(analytics)
    private val repository = KeepsyRepository(db.appDao(), firebaseService)
    
    // Sync components
    private val firestoreService = FirestoreService()
    private val syncRepository = SyncRepository(db.appDao(), firestoreService)
    val syncManager = SyncManager(application, syncRepository)
    val syncState = syncManager.syncState

    private val settingsManager = SettingsManager(application)
    
    val tutorialViewModel = TutorialViewModel(settingsManager) {
        // Sync tutorial completion to cloud
        viewModelScope.launch {
            try {
                val data = HashMap<String, Any>()
                data["tutorialCompleted"] = true
                firestoreService.updateProfile(data)
            } catch (e: Exception) { /* Non-fatal */ }
        }
    }
    private val backupManager = BackupManager(application, db.appDao())
    val monetizationRepository: MonetizationRepository = MonetizationProvider.getRepository(application)

    private val _errorState = MutableStateFlow<KeepsyError?>(null)
    val errorState: StateFlow<KeepsyError?> = _errorState.asStateFlow()

    fun clearError() {
        _errorState.value = null
    }

    private fun handleError(throwable: Throwable) {
        _errorState.value = ErrorHandler.handleError(throwable)
    }

    // App Preferences
    val isOnboardingCompleted = settingsManager.isOnboardingCompleted
    val isTutorialCompleted = settingsManager.isTutorialCompleted
    
    private val _isTutorialRequested = MutableStateFlow(false)
    val isTutorialRequested: StateFlow<Boolean> = _isTutorialRequested.asStateFlow()

    fun requestTutorial(requested: Boolean) {
        _isTutorialRequested.value = requested
    }
    val darkModePreference = settingsManager.darkModePreference

    // Database source streams
    val spaces = repository.spaces
    val categories = repository.categories
    val tags = repository.tags
    val activityLogs = repository.activityLogs
    val activeItems = repository.activeItemsWithDetails
    val trashItems = repository.trashItemsWithDetails

    // Auth state
    val authState = repository.authState
    
    private val _isRefreshingVerification = MutableStateFlow(false)
    val isRefreshingVerification: StateFlow<Boolean> = _isRefreshingVerification.asStateFlow()

    private suspend fun purgeLocalData() {
        KeepsyLogger.i("Purging all local data for security identity isolation...")
        withContext(Dispatchers.IO) {
            try {
                // Reset settings (onboarding, tutorial, etc)
                settingsManager.resetSettings()
                // Clear secure data
                securityService.clearSecureData()
                // Wipe DB
                db.clearAllTables()
                // Re-seed defaults
                repository.seedDefaultCategoriesIfEmpty()
                KeepsyLogger.i("Local data purge complete")
            } catch (e: Exception) {
                KeepsyLogger.e("Purge failed", e)
            }
        }
    }

    fun signIn(email: String, password: String) {
        if (email.trim().isEmpty()) {
            _errorState.value = KeepsyError.AuthError("Please enter your email address.")
            return
        }
        viewModelScope.launch {
            try {
                // Purge before sign in to ensure clean state
                purgeLocalData()
                repository.signInWithEmail(email, password)
            } catch (e: Exception) {
                handleError(e)
            }
        }
    }

    fun signUp(email: String, password: String, name: String) {
        if (email.trim().isEmpty()) {
            _errorState.value = KeepsyError.AuthError("Please enter your email address.")
            return
        }
        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            _errorState.value = KeepsyError.AuthError("Please enter a valid email address.")
            return
        }
        viewModelScope.launch {
            try {
                // Purge before sign up to ensure clean state
                purgeLocalData()
                repository.signUpWithEmail(email, password, name)
            } catch (e: Exception) {
                handleError(e)
            }
        }
    }

    fun signInWithCredential(credential: com.google.firebase.auth.AuthCredential) {
        viewModelScope.launch {
            try {
                // Purge before credential sign in to ensure clean state
                purgeLocalData()
                repository.signInWithCredential(credential)
            } catch (e: Exception) {
                handleError(e)
            }
        }
    }

    fun signOut(onComplete: () -> Unit = {}) {
        viewModelScope.launch {
            try {
                KeepsyLogger.i("Signing out...")
                // 1. Try a final sync
                try {
                    syncManager.performSync()
                } catch (e: Exception) {
                    KeepsyLogger.w("Final sync before logout failed: ${e.message}")
                }

                // 2. Sign out from Firebase
                repository.signOut()
                
                // 3. Mandatory Security Purge
                purgeLocalData()
                
                KeepsyLogger.i("Sign out complete")
                onComplete()
            } catch (e: Exception) {
                KeepsyLogger.e("Sign out failed", e)
                handleError(e)
            }
        }
    }

    fun sendPasswordResetEmail(email: String, onSuccess: () -> Unit) {
        if (email.trim().isEmpty()) {
            _errorState.value = KeepsyError.AuthError("Please enter your email address.")
            return
        }
        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            _errorState.value = KeepsyError.AuthError("Please enter a valid email address.")
            return
        }
        viewModelScope.launch {
            try {
                repository.sendPasswordResetEmail(email)
                onSuccess()
            } catch (e: Exception) {
                handleError(e)
            }
        }
    }

    fun refreshVerificationStatus() {
        viewModelScope.launch {
            _isRefreshingVerification.value = true
            try {
                val isVerified = repository.reloadUserVerification()
                if (isVerified) {
                    Toast.makeText(getApplication(), "Email verified successfully!", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(getApplication(), "Email not verified yet. Please check your inbox.", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                handleError(e)
            } finally {
                _isRefreshingVerification.value = false
            }
        }
    }

    fun resendVerificationEmail() {
        viewModelScope.launch {
            try {
                repository.sendEmailVerification()
                Toast.makeText(getApplication(), "Verification email resent.", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                handleError(e)
            }
        }
    }

    fun isEmailVerified(): Boolean {
        return repository.isEmailVerified()
    }

    fun handleExternalError(throwable: Throwable) {
        handleError(throwable)
    }

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
    val appStatistics = combine(
        activeItems, 
        spaces, 
        categories,
        trashItems
    ) { items, spacesList, categoriesList, trashItemsList ->
        val totalCount = items.size
        val spacesCount = spacesList.size
        val categoriesCount = categoriesList.size
        val favoritesCount = items.count { it.item.isFavorite }
        val trashCount = trashItemsList.size
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
        KeepsyLogger.i("KeepsyViewModel initializing...")
        viewModelScope.launch {
            try {
                repository.seedDefaultCategoriesIfEmpty()
                // Start periodic sync
                syncManager.schedulePeriodicSync()
                // Start network observer
                syncManager.startNetworkObservation(this)
                KeepsyLogger.i("KeepsyViewModel initialization complete")
            } catch (e: Exception) {
                KeepsyLogger.e("KeepsyViewModel initialization failed", e)
                handleError(e)
            }
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
    private val _isRestoringData = MutableStateFlow(false)
    val isRestoringData: StateFlow<Boolean> = _isRestoringData.asStateFlow()

    suspend fun checkOnboardingStatus() {
        if (_isRestoringData.value) return
        
        val currentUid = firebaseService.getCurrentUser()?.uid
        val lastUid = settingsManager.lastUserId.value
        
        // Identity Isolation Check
        if (currentUid != null && lastUid != null && currentUid != lastUid) {
            KeepsyLogger.w("Identity mismatch detected! Purging local data to prevent leakage.")
            purgeLocalData()
        }
        
        // Set the current identity
        settingsManager.setLastUserId(currentUid)

        _isRestoringData.value = true
        try {
            // 1. Always perform initial sync to see if there's remote data
            syncRepository.syncOnLogin()
            
            // 2. Check if the user has a Root Space in the newly restored Room DB
            val rootSpaces = db.appDao().getLiveSpaces().first()
            if (rootSpaces.isNotEmpty()) {
                settingsManager.setOnboardingCompleted(true)
                settingsManager.setTutorialCompleted(true) // Existing users with data skip tutorial
            } else {
                // Check cloud profile for tutorial status
                val profile = firestoreService.getProfile()
                val cloudOnboardingDone = profile?.get("onboardingCompleted") as? Boolean ?: false
                val cloudTutorialDone = profile?.get("tutorialCompleted") as? Boolean ?: false
                
                if (cloudOnboardingDone) {
                    settingsManager.setOnboardingCompleted(true)
                    if (cloudTutorialDone) {
                        settingsManager.setTutorialCompleted(true)
                    }
                } else {
                    // Check cloud entities one last time explicitly
                    val existsOnCloud = syncRepository.isUserAlreadyExistsOnCloud()
                    if (existsOnCloud) {
                        settingsManager.setOnboardingCompleted(true)
                        settingsManager.setTutorialCompleted(true)
                    } else {
                        settingsManager.setOnboardingCompleted(false)
                    }
                }
            }
        } catch (e: Exception) {
            handleError(e)
        } finally {
            _isRestoringData.value = false
        }
    }

    fun setOnboardingCompleted() {
        settingsManager.setOnboardingCompleted(true)
        viewModelScope.launch {
            try {
                val data = HashMap<String, Any>()
                data["onboardingCompleted"] = true
                firestoreService.updateProfile(data)
            } catch (e: Exception) { /* Non-fatal */ }
        }
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

    fun manualSync() {
        viewModelScope.launch {
            syncManager.performSync()
        }
    }

    // --- ITEM ACTIONS ---
    fun selectItem(itemId: Long) {
        viewModelScope.launch {
            val details = repository.getItemWithDetails(itemId)
            _selectedItem.value = details
            if (details != null) {
                repository.trackItemViewed(itemId)
                syncManager.performSync()
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
                updatedAt = System.currentTimeMillis(),
                syncState = "DIRTY"
            )

            repository.saveItem(itemToSave, tagList)
            onSuccess()
            syncManager.performSync()
        }
    }

    fun toggleItemFavorite(itemId: Long) {
        viewModelScope.launch {
            val item = repository.getItemById(itemId)
            if (item != null) {
                val updated = item.copy(isFavorite = !item.isFavorite, updatedAt = System.currentTimeMillis(), syncState = "DIRTY")
                db.appDao().updateItem(updated)
                // Refresh selection state
                _selectedItem.value = repository.getItemWithDetails(itemId)
                syncManager.performSync()
            }
        }
    }

    fun moveItem(itemId: Long, newSpaceId: Long, reason: String, onSuccess: () -> Unit) {
        viewModelScope.launch {
            repository.moveItem(itemId, newSpaceId, reason)
            // Refresh detailed states
            _selectedItem.value = repository.getItemWithDetails(itemId)
            onSuccess()
            syncManager.performSync()
        }
    }

    fun softDeleteSelectedItem(onSuccess: () -> Unit) {
        val currentId = _selectedItem.value?.item?.itemId ?: return
        viewModelScope.launch {
            repository.softDeleteItem(currentId)
            _selectedItem.value = null
            onSuccess()
            syncManager.performSync()
        }
    }

    fun restoreItem(itemId: Long) {
        viewModelScope.launch {
            repository.restoreItem(itemId)
            syncManager.performSync()
        }
    }

    fun permanentlyDeleteItem(itemId: Long) {
        viewModelScope.launch {
            repository.permanentlyDeleteItem(itemId)
            syncManager.performSync()
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
                updatedAt = System.currentTimeMillis(),
                syncState = "DIRTY"
            )

            if (spaceId == 0L) {
                repository.insertSpace(spaceToSave)
            } else {
                repository.updateSpace(spaceToSave)
            }
            onSuccess()
            syncManager.performSync()
        }
    }

    fun deleteSpace(spaceId: Long, onSuccess: () -> Unit) {
        viewModelScope.launch {
            repository.deleteSpace(spaceId)
            _selectedSpace.value = null
            onSuccess()
            syncManager.performSync()
        }
    }

    fun toggleSpaceFavorite(spaceId: Long) {
        viewModelScope.launch {
            val space = repository.getSpaceById(spaceId)
            if (space != null) {
                val updated = space.copy(isFavorite = !space.isFavorite, updatedAt = System.currentTimeMillis(), syncState = "DIRTY")
                repository.updateSpace(updated)
                // Refresh detail
                _selectedSpace.value = SpaceWithParent(updated, updated.parentSpaceId?.let { repository.getSpaceById(it) })
                syncManager.performSync()
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
            syncManager.performSync()
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
