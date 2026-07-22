package com.keepsy.app.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.keepsy.app.model.SyncState
import com.keepsy.app.model.UserProfile
import com.keepsy.app.navigation.SubScreen
import com.keepsy.app.navigation.TabScreen
import com.keepsy.app.ui.components.AccountCenterScreen
import com.keepsy.app.ui.components.KeepsyBackgroundEffects
import com.keepsy.app.ui.home.HomeScreen
import com.keepsy.app.ui.spaces.*
import com.keepsy.app.ui.items.*
import com.keepsy.app.ui.search.SearchScreen
import com.keepsy.app.ui.activity.ActivityScreen
import com.keepsy.app.ui.settings.*
import com.keepsy.app.ui.trash.TrashBinScreen
import com.keepsy.app.ui.theme.*
import com.keepsy.app.viewmodel.KeepsyViewModel

data class NavItem(val tab: TabScreen, val icon: ImageVector)

@Composable
fun DashboardScaffold(
    viewModel: KeepsyViewModel,
    currentTab: TabScreen,
    onTabSelected: (TabScreen) -> Unit,
    currentSubScreen: SubScreen,
    onNavigateToSub: (SubScreen) -> Unit,
    onPopSub: () -> Unit
) {
    val syncStatus by viewModel.syncState.collectAsStateWithLifecycle()
    val spacesList by viewModel.spaces.collectAsStateWithLifecycle(emptyList())
    val categoriesList by viewModel.categories.collectAsStateWithLifecycle(emptyList())
    val profile by viewModel.userProfile.collectAsStateWithLifecycle()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        KeepsyBackgroundEffects()

        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                if (currentSubScreen == SubScreen.None) {
                    Column {
                        SyncIndicator(state = syncStatus)
                        PremiumTopBar(
                            currentTab = currentTab,
                            profile = profile,
                            onAccountClick = { onNavigateToSub(SubScreen.AccountCenter) }
                        )
                    }
                }
            },
            bottomBar = {
                // FIXED: Removed the bottomBar slot completely to prevent double navbars
                // We use the Floating overlay below instead.
            },
            floatingActionButton = {
                if (currentSubScreen == SubScreen.None) {
                    val showFab = when (currentTab) {
                        TabScreen.Home -> spacesList.isNotEmpty() && categoriesList.isNotEmpty()
                        TabScreen.Spaces -> true
                        else -> false
                    }
                    if (showFab) {
                        PremiumFAB(currentTab = currentTab, onNavigateToSub = onNavigateToSub)
                    }
                }
            }
        ) { padding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
            ) {
                Crossfade(
                    targetState = currentSubScreen,
                    label = "ScreenTransition",
                    animationSpec = tween(150)
                ) { sub ->
                    when (sub) {
                        SubScreen.None -> {
                            AnimatedContent(
                                targetState = currentTab,
                                transitionSpec = { fadeIn(animationSpec = tween(100)) togetherWith fadeOut(animationSpec = tween(100)) },
                                label = "TabTransition"
                            ) { tab ->
                                when (tab) {
                                    TabScreen.Home -> HomeScreen(viewModel, onTabSelected, onNavigateToSub)
                                    TabScreen.Spaces -> SpacesScreen(viewModel, onNavigateToSub)
                                    TabScreen.Search -> SearchScreen(viewModel, onNavigateToSub)
                                    TabScreen.Activity -> ActivityScreen(viewModel, onNavigateToSub)
                                    TabScreen.Settings -> SettingsScreen(viewModel, onNavigateToSub)
                                }
                            }
                        }
                        SubScreen.AccountCenter -> AccountCenterScreen(
                            profile = profile,
                            onPop = onPopSub,
                            onNavigateToSub = onNavigateToSub,
                            onSignOut = { viewModel.signOut() }
                        )
                        is SubScreen.ItemDetails -> ItemDetailsScreen(sub.itemId, viewModel, onPopSub, onNavigateToSub)
                        is SubScreen.SpaceDetails -> SpaceDetailsScreen(sub.spaceId, viewModel, onPopSub, onNavigateToSub)
                        is SubScreen.AddEditItem -> AddEditItemScreen(sub.itemId, sub.spaceId, viewModel, onPopSub)
                        is SubScreen.AddEditSpace -> AddEditSpaceScreen(sub.spaceId, sub.parentSpaceId, viewModel, onPopSub)
                        is SubScreen.MoveItem -> MoveItemScreen(sub.itemId, viewModel, onPopSub)
                        SubScreen.TrashBin -> TrashBinScreen(viewModel, onPopSub)
                        SubScreen.Profile -> ProfileScreen(viewModel, onPopSub, onNavigateToSub)
                        SubScreen.EditProfile -> EditProfileScreen(viewModel, onPopSub)
                        SubScreen.BackupSync -> BackupSyncScreen(viewModel, onPopSub)
                        SubScreen.Subscription -> SubscriptionScreen(viewModel, onPopSub)
                        SubScreen.SecurityCenter -> SecurityScreen(viewModel, onPopSub)
                        else -> {}
                    }
                }
            }
        }

        // NAVIGATION BAR - TRUE FLOATING OVERLAY (Keep this one)
        if (currentSubScreen == SubScreen.None) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .navigationBarsPadding(),
                contentAlignment = Alignment.BottomCenter
            ) {
                FloatingBottomNavigation(
                    currentTab = currentTab,
                    onTabSelected = onTabSelected
                )
            }
        }
    }
}

@Composable
fun PremiumTopBar(
    currentTab: TabScreen,
    profile: UserProfile?,
    onAccountClick: () -> Unit
) {
    val title = when (currentTab) {
        TabScreen.Home -> "Dashboard"
        TabScreen.Spaces -> "Inventory Map"
        TabScreen.Search -> "Search"
        TabScreen.Activity -> "Activity"
        TabScreen.Settings -> "Settings"
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = 24.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.ExtraBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (currentTab == TabScreen.Home) {
                Text(
                    text = if (profile != null) "Welcome, ${profile.displayName}" else "Welcome back",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.secondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        
        Surface(
            modifier = Modifier
                .size(40.dp)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onAccountClick
                ),
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(
                    text = com.keepsy.app.utils.AvatarUtils.getInitials(profile?.name),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                
                if (profile?.photoUrl != null && profile?.photoUrl != "") {
                    AsyncImage(
                        model = profile?.photoUrl,
                        contentDescription = "Account",
                        modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surface),
                        contentScale = ContentScale.Crop
                    )
                }
            }
        }
    }
}

@Composable
fun FloatingBottomNavigation(
    currentTab: TabScreen,
    onTabSelected: (TabScreen) -> Unit
) {
    val items = remember {
        listOf(
            NavItem(TabScreen.Home, Icons.Default.Home),
            NavItem(TabScreen.Spaces, Icons.Default.Layers),
            NavItem(TabScreen.Search, Icons.Default.Search),
            NavItem(TabScreen.Activity, Icons.Default.History),
            NavItem(TabScreen.Settings, Icons.Default.Settings)
        )
    }

    Box(
        modifier = Modifier
            .padding(horizontal = 24.dp, vertical = 2.dp)
            .fillMaxWidth(),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            modifier = Modifier
                .height(60.dp)
                .fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.05f)),
            shadowElevation = 8.dp
        ) {
            Row(
                modifier = Modifier.fillMaxSize(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                items.forEach { item ->
                    val selected = currentTab == item.tab
                    val color by animateColorAsState(
                        targetValue = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary.copy(alpha = 0.6f),
                        label = "icon_color"
                    )
                    
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                                onClick = { onTabSelected(item.tab) }
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = item.icon,
                            contentDescription = null,
                            tint = color,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun PremiumFAB(
    currentTab: TabScreen,
    onNavigateToSub: (SubScreen) -> Unit
) {
    FloatingActionButton(
        onClick = { 
            if (currentTab == TabScreen.Spaces) {
                onNavigateToSub(SubScreen.AddEditSpace())
            } else {
                onNavigateToSub(SubScreen.AddEditItem())
            }
        },
        containerColor = MaterialTheme.colorScheme.primary,
        contentColor = Color.Black,
        shape = CircleShape,
        modifier = Modifier.padding(bottom = 0.dp)
    ) {
        Icon(
            imageVector = if (currentTab == TabScreen.Spaces) Icons.Default.AddHomeWork else Icons.Default.Add,
            contentDescription = "Add",
            modifier = Modifier.size(24.dp)
        )
    }
}

@Composable
fun SyncIndicator(state: SyncState) {
    val visible = state != SyncState.IDLE && state != SyncState.SYNCED && state != SyncState.COMPLETED
    AnimatedVisibility(
        visible = visible,
        enter = expandVertically() + fadeIn(),
        exit = shrinkVertically() + fadeOut()
    ) {
        val text = when (state) {
            SyncState.SYNCING -> "Refreshing..."
            SyncState.UPLOADING -> "Saving..."
            SyncState.DOWNLOADING -> "Fetching..."
            SyncState.FAILED -> "Sync Error"
            else -> ""
        }
        
        val color = when (state) {
            SyncState.UPLOADING -> PrimaryAccent
            SyncState.DOWNLOADING -> PrimaryPurple
            SyncState.FAILED -> ErrorRed
            else -> MaterialTheme.colorScheme.primary
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(color.copy(alpha = 0.1f))
                .padding(vertical = 2.dp),
            contentAlignment = Alignment.Center
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (state != SyncState.FAILED) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(10.dp),
                        strokeWidth = 2.dp,
                        color = color
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                }
                Text(
                    text = text,
                    style = MaterialTheme.typography.labelSmall,
                    color = color,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
