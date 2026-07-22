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
import com.keepsy.app.repository.AccountRepository
import com.keepsy.app.service.FirebaseService
import com.keepsy.app.sync.FirestoreService
import com.keepsy.app.sync.SyncManager
import com.keepsy.app.sync.SyncRepository
import com.keepsy.app.monetization.MonetizationProvider
import com.keepsy.app.monetization.MonetizationRepository
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import com.keepsy.app.service.SecurityService
import com.keepsy.app.utils.ErrorHandler
import com.keepsy.app.utils.KeepsyError
import com.keepsy.app.utils.KeepsyLogger
import java.io.File
import java.io.FileOutputStream

class KeepsyViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getDatabase(application)
    private val analytics = FirebaseAnalytics.getInstance(application)
    private val securityService = SecurityService(application)
    private val firebaseService = FirebaseService(analytics)
    private val repository = KeepsyRepository(db.appDao(), firebaseService)
    
    private val firestoreService = FirestoreService()
    private val syncRepository = SyncRepository(db.appDao(), firestoreService, firebaseService)
    val syncManager = SyncManager(application, syncRepository)
    val syncState = syncManager.syncState

    private val settingsManager = SettingsManager(application)
    private val accountRepository = AccountRepository(firebaseService, db.appDao())
    
    private val backupManager = BackupManager(application, db.appDao())
    val monetizationRepository: MonetizationRepository = MonetizationProvider.getRepository(application)

    private val _errorState = MutableStateFlow<KeepsyError?>(null)
    val errorState: StateFlow<KeepsyError?> = _errorState.asStateFlow()

    fun clearError() { _errorState.value = null }
    private fun handleError(throwable: Throwable) { _errorState.value = ErrorHandler.handleError(throwable) }

    val isOnboardingCompleted = settingsManager.isOnboardingCompleted
    private val _isStatusChecked = MutableStateFlow(false)
    val isStatusChecked: StateFlow<Boolean> = _isStatusChecked.asStateFlow()

    val spaces = repository.spaces
    val categories = repository.categories
    val tags = repository.tags
    val activityLogs = repository.activityLogs
    val activeItems = repository.activeItemsWithDetails
    val trashItems = repository.trashItemsWithDetails
    val authState = repository.authState
    
    private val _isRefreshingVerification = MutableStateFlow(false)
    val isRefreshingVerification: StateFlow<Boolean> = _isRefreshingVerification.asStateFlow()

    private suspend fun purgeLocalData() {
        _isStatusChecked.value = false
        withContext(Dispatchers.IO) {
            try {
                settingsManager.resetSettings()
                securityService.clearSecureData()
                db.clearAllTables()
                repository.seedDefaultCategoriesIfEmpty()
            } catch (e: Exception) { KeepsyLogger.e("Purge failed", e) }
        }
    }

    fun signIn(email: String, password: String) {
        viewModelScope.launch { try { purgeLocalData(); repository.signInWithEmail(email, password) } catch (e: Exception) { handleError(e) } }
    }

    fun signUp(email: String, password: String, name: String) {
        viewModelScope.launch { try { purgeLocalData(); repository.signUpWithEmail(email, password, name) } catch (e: Exception) { handleError(e) } }
    }

    fun signInWithCredential(credential: com.google.firebase.auth.AuthCredential) {
        viewModelScope.launch { try { purgeLocalData(); repository.signInWithCredential(credential) } catch (e: Exception) { handleError(e) } }
    }

    fun signOut(onComplete: () -> Unit = {}) {
        viewModelScope.launch {
            try {
                KeepsyLogger.i("Signing out...")
                try { syncManager.performSync() } catch (e: Exception) { }
                repository.signOut(); purgeLocalData(); onComplete()
            } catch (e: Exception) { handleError(e) }
        }
    }

    fun sendPasswordResetEmail(email: String, onSuccess: () -> Unit) {
        viewModelScope.launch { try { repository.sendPasswordResetEmail(email); onSuccess() } catch (e: Exception) { handleError(e) } }
    }

    fun refreshVerificationStatus() {
        viewModelScope.launch {
            _isRefreshingVerification.value = true
            try {
                if (repository.reloadUserVerification()) Toast.makeText(getApplication(), "Email verified!", Toast.LENGTH_SHORT).show()
                else Toast.makeText(getApplication(), "Check your inbox.", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) { handleError(e) } finally { _isRefreshingVerification.value = false }
        }
    }

    fun resendVerificationEmail() {
        viewModelScope.launch { try { repository.sendEmailVerification(); Toast.makeText(getApplication(), "Sent.", Toast.LENGTH_SHORT).show() } catch (e: Exception) { handleError(e) } }
    }

    fun isEmailVerified(): Boolean = repository.isEmailVerified()
    fun handleExternalError(throwable: Throwable) { handleError(throwable) }

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery

    val recentItems = activeItems.map { it.sortedByDescending { it.item.createdAt }.take(10) }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val favoriteSpaces = spaces.map { it.filter { it.isFavorite } }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val favoriteItems = activeItems.map { it.filter { it.item.isFavorite } }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val appStatistics = combine(activeItems, spaces, categories, trashItems, tags, activityLogs) { args ->
        val items = args[0] as List<ItemWithDetails>
        val spacesList = args[1] as List<Space>
        val categoriesList = args[2] as List<Category>
        val trashItemsList = args[3] as List<ItemWithDetails>
        val tagsList = args[4] as List<Tag>
        val logsList = args[5] as List<ActivityLog>
        Stats(items.size, spacesList.size, categoriesList.size, items.count { it.item.isFavorite }, trashItemsList.size, tagsList.size, logsList.size)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), Stats(0, 0, 0, 0, 0, 0, 0))

    val userProfile: StateFlow<UserProfile?> = combine(authState, appStatistics, isStatusChecked, firebaseService.getProfileFlow(), settingsManager.localProfileCache) { auth, stats, checked, doc, local ->
        if (auth is AuthState.Authenticated) {
            val user = auth.user
            val nameValue = local.name ?: (doc?.get("profile_name") as? String) ?: user.name ?: "Friend"
            val displayNameValue = local.displayName ?: (doc?.get("profile_display_name") as? String) ?: nameValue
            val photoValue = local.photoPath ?: (doc?.get("profile_photo_url") as? String) ?: user.photoUrl
            UserProfile(
                uid = user.uid, name = nameValue, displayName = displayNameValue, email = user.email ?: "", photoUrl = photoValue,
                planType = "Free", memberSince = (doc?.get("createdAt") as? Long) ?: user.createdAt ?: System.currentTimeMillis(),
                lastSyncAt = System.currentTimeMillis(), totalItems = stats.totalItems, totalSpaces = stats.totalSpaces,
                totalCategories = stats.totalCategories, totalTags = stats.tagsCount, totalActivity = stats.activityCount,
                totalTrash = stats.trashItemsCount, totalFavorites = stats.favoriteItemsCount, storageUsed = "1.2 MB",
                syncEnabled = true, theme = "System", language = "en", notificationSettings = emptyMap(), backupFrequency = "Daily"
            )
        } else null
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val searchResults: StateFlow<SearchResult> = combine(searchQuery, activeItems, spaces) { query, items, spacesList ->
        if (query.trim().isBlank()) SearchResult(emptyList(), emptyList())
        else {
            val q = query.trim().lowercase()
            SearchResult(items.filter { it.item.name.lowercase().contains(q) }, spacesList.filter { it.name.lowercase().contains(q) })
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), SearchResult(emptyList(), emptyList()))

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
            try {
                repository.seedDefaultCategoriesIfEmpty()
                syncManager.schedulePeriodicSync()
                syncManager.startNetworkObservation(this)
                authState.collect { auth ->
                    if (auth is AuthState.Authenticated) {
                        val currentUid = auth.user.uid
                        val lastCheckedUid = settingsManager.lastUserId.value
                        if (currentUid != lastCheckedUid || !isStatusChecked.value) { checkOnboardingStatus() }
                        manualSync()
                    } else if (auth is AuthState.Unauthenticated) { _isStatusChecked.value = false }
                }
            } catch (e: Exception) { KeepsyLogger.e("Init fail", e) }
        }
    }

    fun updateSearchQuery(query: String) { _searchQuery.value = query }
    suspend fun getItemWithDetails(itemId: Long): ItemWithDetails? { return repository.getItemWithDetails(itemId) }

    private val _isRestoringData = MutableStateFlow(false)
    val isRestoringData: StateFlow<Boolean> = _isRestoringData.asStateFlow()

    suspend fun checkOnboardingStatus() {
        if (_isStatusChecked.value || _isRestoringData.value) return 
        val currentUid = firebaseService.getCurrentUser()?.uid
        if (currentUid != settingsManager.lastUserId.value) purgeLocalData()
        settingsManager.setLastUserId(currentUid)
        _isRestoringData.value = true
        try {
            withContext(Dispatchers.IO) {
                val syncJob = launch { syncRepository.syncOnLogin() }
                val cloudDoc = withTimeoutOrNull(6000) {
                    var doc: Map<String, Any?>? = null
                    for (i in 1..4) {
                        doc = firebaseService.getProfileDocument()
                        if (doc?.get("profile_photo_url") != null) break
                        delay(1000)
                    }
                    doc
                }
                if (cloudDoc != null) {
                    settingsManager.updateLocalProfile(
                        name = (cloudDoc["profile_name"] as? String) ?: (cloudDoc["name"] as? String),
                        displayName = (cloudDoc["profile_display_name"] as? String) ?: (cloudDoc["displayName"] as? String),
                        photoPath = cloudDoc["profile_photo_url"] as? String
                    )
                }
                syncJob.join() 
                val spaceCount = db.appDao().getSpaceCount()
                if (spaceCount > 0) settingsManager.setOnboardingCompleted(true)
                else settingsManager.setOnboardingCompleted(syncRepository.isUserAlreadyExistsOnCloud())
            }
        } catch (e: Exception) { KeepsyLogger.e("Check fail", e) } 
        finally { _isStatusChecked.value = true; _isRestoringData.value = false }
    }

    fun setOnboardingCompleted(completed: Boolean = true) { settingsManager.setOnboardingCompleted(completed) }
    fun completeOnboarding(spaceName: String, itemName: String?) {
        viewModelScope.launch {
            try {
                settingsManager.setOnboardingCompleted(true)
                firestoreService.updateProfile(mapOf("onboardingCompleted" to true, "lastOnboarded" to System.currentTimeMillis()))
                saveSpace(0L, spaceName.trim(), "Primary", null, "home", null, true) {}
                if (itemName != null && itemName.trim().isNotEmpty()) {
                    val sp = spaces.first(); val sid = sp.find { it.name == spaceName.trim() }?.spaceId ?: sp.firstOrNull()?.spaceId ?: 1L
                    saveItem(0L, itemName.trim(), "First item", sid, 1L, "", null, emptyList(), true) {}
                }
                manualSync()
            } catch (e: Exception) { KeepsyLogger.e("Onboarding complete fail", e) }
        }
    }

    fun manualSync() { viewModelScope.launch { syncManager.performSync() } }

    fun selectItem(itemId: Long) {
        viewModelScope.launch {
            val d = repository.getItemWithDetails(itemId); _selectedItem.value = d
            if (d != null) { repository.trackItemViewed(itemId); syncManager.performSync() }
        }
    }

    fun saveItem(itemId: Long, name: String, description: String, spaceId: Long, categoryId: Long, notes: String, photoUri: Uri?, tagList: List<String>, isFavorite: Boolean = false, onSuccess: () -> Unit) {
        viewModelScope.launch {
            val path = photoUri?.let { repository.copyImageToAppStorage(getApplication(), it) }
            val ex = if (itemId != 0L) repository.getItemById(itemId) else null
            val item = Item(
                itemId = itemId, name = name, description = description, spaceId = spaceId, categoryId = categoryId,
                photoPath = path ?: ex?.photoPath, photoUrl = ex?.photoUrl,
                createdAt = ex?.createdAt ?: System.currentTimeMillis(), updatedAt = System.currentTimeMillis(),
                isFavorite = isFavorite, isDeleted = false, deletedAt = null, lastViewed = ex?.lastViewed,
                notes = notes, version = (ex?.version ?: 0) + 1, syncState = "DIRTY",
                lastSynced = ex?.lastSynced, remoteId = ex?.remoteId, spaceRemoteId = ex?.spaceRemoteId, categoryRemoteId = ex?.categoryRemoteId
            )
            repository.saveItem(item, tagList); onSuccess(); syncManager.performSync()
        }
    }

    fun toggleItemFavorite(itemId: Long) {
        viewModelScope.launch {
            val i = repository.getItemById(itemId)
            if (i != null) {
                db.appDao().updateItem(i.copy(isFavorite = !i.isFavorite, updatedAt = System.currentTimeMillis(), syncState = "DIRTY"))
                _selectedItem.value = repository.getItemWithDetails(itemId); syncManager.performSync()
            }
        }
    }

    fun selectSpace(spaceId: Long) {
        viewModelScope.launch {
            val s = repository.getSpaceById(spaceId)
            if (s != null) {
                _selectedSpace.value = SpaceWithParent(s, s.parentSpaceId?.let { repository.getSpaceById(it) })
                _nestedSubspaces.value = db.appDao().getSubspaces(spaceId)
                val its = db.appDao().getItemsInSpace(spaceId); val sps = db.appDao().getLiveSpaces().first(); val cats = db.appDao().getLiveCategories().first()
                _itemsInSpace.value = its.map { ItemWithDetails(it, sps.find { sp -> sp.spaceId == it.spaceId }, cats.find { c -> c.categoryId == it.categoryId }, emptyList()) }
            }
        }
    }

    fun saveSpace(spaceId: Long, name: String, description: String, parentSpaceId: Long?, icon: String?, photoUri: Uri?, isFavorite: Boolean = false, onSuccess: () -> Unit) {
        viewModelScope.launch {
            val path = photoUri?.let { repository.copyImageToAppStorage(getApplication(), it) }
            val ex = if (spaceId != 0L) repository.getSpaceById(spaceId) else null
            val space = Space(
                spaceId = spaceId, parentSpaceId = parentSpaceId, name = name, description = description,
                icon = icon, photoPath = path ?: ex?.photoPath, photoUrl = ex?.photoUrl,
                createdAt = ex?.createdAt ?: System.currentTimeMillis(), updatedAt = System.currentTimeMillis(),
                isFavorite = isFavorite, version = (ex?.version ?: 0) + 1, syncState = "DIRTY",
                isDeleted = false, lastSynced = ex?.lastSynced, remoteId = ex?.remoteId, parentRemoteId = ex?.parentRemoteId
            )
            if (spaceId == 0L) repository.insertSpace(space) else repository.updateSpace(space); onSuccess(); syncManager.performSync()
        }
    }

    fun moveItem(itemId: Long, newSpaceId: Long, reason: String, onSuccess: () -> Unit) {
        viewModelScope.launch { repository.moveItem(itemId, newSpaceId, reason); _selectedItem.value = repository.getItemWithDetails(itemId); onSuccess(); syncManager.performSync() }
    }

    fun softDeleteSelectedItem(onSuccess: () -> Unit) {
        val id = _selectedItem.value?.item?.itemId ?: return
        viewModelScope.launch { repository.softDeleteItem(id); _selectedItem.value = null; onSuccess(); syncManager.performSync() }
    }

    fun restoreItem(itemId: Long) { viewModelScope.launch { repository.restoreItem(itemId); syncManager.performSync() } }
    fun permanentlyDeleteItem(itemId: Long) { viewModelScope.launch { repository.permanentlyDeleteItem(itemId); syncManager.performSync() } }
    fun deleteSpace(spaceId: Long, onSuccess: () -> Unit) { viewModelScope.launch { repository.deleteSpace(spaceId); _selectedSpace.value = null; onSuccess(); syncManager.performSync() } }
    fun toggleSpaceFavorite(spaceId: Long) {
        viewModelScope.launch {
            val s = repository.getSpaceById(spaceId)
            if (s != null) {
                val upd = s.copy(isFavorite = !s.isFavorite, updatedAt = System.currentTimeMillis(), syncState = "DIRTY")
                repository.updateSpace(upd); _selectedSpace.value = SpaceWithParent(upd, upd.parentSpaceId?.let { repository.getSpaceById(it) }); syncManager.performSync()
            }
        }
    }

    fun exportBackup(onExported: (String) -> Unit) {
        viewModelScope.launch { try { val json = backupManager.exportBackupJson(); val file = File(getApplication<Application>().cacheDir, "keepsy_backup.json"); FileOutputStream(file).use { it.write(json.toByteArray()) }; onExported(json) } catch (e: Exception) { } }
    }

    fun importBackup(json: String, onCompleted: (Boolean) -> Unit) {
        viewModelScope.launch { val ok = backupManager.importBackupJson(json); if (ok) repository.seedDefaultCategoriesIfEmpty(); onCompleted(ok); syncManager.performSync() }
    }

    fun resetApp() { viewModelScope.launch(Dispatchers.IO) { settingsManager.resetSettings(); db.clearAllTables(); repository.seedDefaultCategoriesIfEmpty() } }
    fun getActivityTrailForItem(itemId: Long): Flow<List<ActivityLog>> = repository.getActivityTrailForItem(itemId)
    suspend fun getFullSpaceTrail(spaceId: Long): List<Space> {
        val trail = mutableListOf<Space>(); var cid: Long? = spaceId
        while (cid != null && cid != 0L) { val s = repository.getSpaceById(cid); if (s != null) { trail.add(0, s); cid = s.parentSpaceId } else cid = null }
        return trail
    }
    suspend fun getFullSpacePath(spaceId: Long): String {
        val trail = getFullSpaceTrail(spaceId)
        return if (trail.isEmpty()) "Unknown Location" else trail.joinToString(" • ") { it.name }
    }

    private val _isUpdatingProfile = MutableStateFlow(false)
    val isUpdatingProfile: StateFlow<Boolean> = _isUpdatingProfile.asStateFlow()

    fun updateProfile(name: String, displayName: String?) {
        viewModelScope.launch {
            settingsManager.updateLocalProfile(name = name, displayName = displayName); _isUpdatingProfile.value = true
            try { val dn = displayName ?: name; accountRepository.updateProfile(name, dn, null); withTimeoutOrNull(10000) { userProfile.filter { it?.name == name && it?.displayName == dn }.first() }; repository.refreshAuthState() }
            catch (e: Exception) { handleError(e) } finally { _isUpdatingProfile.value = false }
        }
    }

    fun updateProfilePicture(uri: Uri) {
        viewModelScope.launch {
            val lp = repository.copyImageToAppStorage(getApplication(), uri); settingsManager.updateLocalProfile(photoPath = lp); _isUpdatingProfile.value = true
            try { val url = accountRepository.uploadProfilePhoto(com.keepsy.app.utils.ImageUtils.compressImage(getApplication(), uri) ?: uri); accountRepository.updateProfile(null, null, url); withTimeoutOrNull(15000) { userProfile.filter { it?.photoUrl == url }.first() }; repository.refreshAuthState() }
            catch (e: Exception) { handleError(e) } finally { _isUpdatingProfile.value = false }
        }
    }

    fun removeProfilePicture() { viewModelScope.launch { try { accountRepository.deleteProfilePhoto(); repository.refreshAuthState() } catch (e: Exception) { handleError(e) } } }
    fun changePassword(current: String, new: String, onSuccess: () -> Unit) { viewModelScope.launch { try { accountRepository.changePassword(current, new); onSuccess() } catch (e: Exception) { handleError(e) } } }

    data class Stats(val totalItems: Int, val totalSpaces: Int, val totalCategories: Int, val favoriteItemsCount: Int, val trashItemsCount: Int, val tagsCount: Int, val activityCount: Int)
}
