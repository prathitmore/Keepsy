package com.keepsy.app.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.keepsy.app.navigation.SubScreen
import com.keepsy.app.navigation.TabScreen
import com.keepsy.app.ui.activity.ActivityScreen
import com.keepsy.app.ui.home.HomeScreen
import com.keepsy.app.ui.items.AddEditItemScreen
import com.keepsy.app.ui.items.ItemDetailsScreen
import com.keepsy.app.ui.items.MoveItemScreen
import com.keepsy.app.ui.search.SearchScreen
import com.keepsy.app.ui.settings.ProfileScreen
import com.keepsy.app.ui.settings.SettingsScreen
import com.keepsy.app.ui.spaces.AddEditSpaceScreen
import com.keepsy.app.ui.spaces.SpaceDetailsScreen
import com.keepsy.app.ui.spaces.SpacesScreen
import com.keepsy.app.ui.theme.*
import com.keepsy.app.ui.trash.TrashBinScreen
import com.keepsy.app.model.SyncState
import com.keepsy.app.viewmodel.KeepsyViewModel
import com.keepsy.app.ui.components.*

data class NavItem(val tab: TabScreen, val icon: androidx.compose.ui.graphics.vector.ImageVector)

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

    var showAccountMenu by remember { mutableStateOf(false) }
    val profile by viewModel.userProfile.collectAsStateWithLifecycle()

    if (showAccountMenu) {
        AccountBottomSheet(
            profile = profile,
            onClose = { showAccountMenu = false },
            onNavigateToProfile = { onNavigateToSub(SubScreen.Profile) },
            onSignOut = { 
                showAccountMenu = false
                viewModel.signOut() 
            },
            onNavigateToSettings = { onTabSelected(TabScreen.Settings) }
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Shared Background Effects
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
                            onAccountClick = { showAccountMenu = true }
                        )
                    }
                }
            },
            bottomBar = {
                if (currentSubScreen == SubScreen.None) {
                    FloatingBottomNavigation(
                        currentTab = currentTab,
                        onTabSelected = onTabSelected
                    )
                }
            },
            floatingActionButton = {
                if (currentSubScreen == SubScreen.None) {
                    val showFab = when (currentTab) {
                        TabScreen.Home -> spacesList.isNotEmpty() && categoriesList.isNotEmpty()
                        TabScreen.Spaces -> true
                        else -> false
                    }
                    
                    if (showFab) {
                        PremiumFAB(
                            currentTab = currentTab,
                            onNavigateToSub = onNavigateToSub
                        )
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
                    animationSpec = tween(400)
                ) { sub ->
                    if (sub == SubScreen.None) {
                        AnimatedContent(
                            targetState = currentTab,
                            transitionSpec = {
                                fadeIn(animationSpec = tween(300)) togetherWith 
                                fadeOut(animationSpec = tween(300))
                            },
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
                    } else {
                        Box(modifier = Modifier.fillMaxSize()) {
                            when (sub) {
                                is SubScreen.ItemDetails -> ItemDetailsScreen(
                                    itemId = sub.itemId,
                                    viewModel = viewModel,
                                    onPop = onPopSub,
                                    onNavigateToSub = onNavigateToSub
                                )
                                is SubScreen.SpaceDetails -> SpaceDetailsScreen(
                                    spaceId = sub.spaceId,
                                    viewModel = viewModel,
                                    onPop = onPopSub,
                                    onNavigateToSub = onNavigateToSub
                                )
                                is SubScreen.AddEditItem -> AddEditItemScreen(
                                    itemId = sub.itemId,
                                    initialSpaceId = sub.spaceId,
                                    viewModel = viewModel,
                                    onPop = onPopSub
                                )
                                is SubScreen.AddEditSpace -> AddEditSpaceScreen(
                                    spaceId = sub.spaceId,
                                    parentSpaceId = sub.parentSpaceId,
                                    viewModel = viewModel,
                                    onPop = onPopSub
                                )
                                is SubScreen.MoveItem -> MoveItemScreen(
                                    itemId = sub.itemId,
                                    viewModel = viewModel,
                                    onPop = onPopSub
                                )
                                SubScreen.TrashBin -> TrashBinScreen(
                                    viewModel = viewModel,
                                    onPop = onPopSub
                                )
                                is SubScreen.Profile -> ProfileScreen(
                                    viewModel = viewModel,
                                    onPop = onPopSub
                                )
                                else -> {}
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PremiumTopBar(
    currentTab: TabScreen,
    profile: com.keepsy.app.model.UserProfile?,
    onAccountClick: () -> Unit
) {
    val title = when (currentTab) {
        TabScreen.Home -> "Dashboard"
        TabScreen.Spaces -> "Inventory Map"
        TabScreen.Search -> "Search Everything"
        TabScreen.Activity -> "Memory Trail"
        TabScreen.Settings -> "System Settings"
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = 24.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column {
            Text(
                text = title,
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.ExtraBold
            )
            if (currentTab == TabScreen.Home) {
                Text(
                    text = if (profile != null) "Welcome back, ${profile.name}" else "Welcome back to Keepsy",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.secondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        
        Surface(
            modifier = Modifier
                .size(44.dp)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onAccountClick
                ),
            shape = RoundedCornerShape(14.dp),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
        ) {
            Box(contentAlignment = Alignment.Center) {
                if (profile?.photoUrl != null) {
                    // Profile image if available
                } else {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = "Account",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
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
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(bottom = 16.dp, start = 24.dp, end = 24.dp),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            modifier = Modifier
                .height(64.dp)
                .fillMaxWidth(),
            shape = RoundedCornerShape(32.dp),
            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.1f)),
            shadowElevation = 12.dp
        ) {
            Row(
                modifier = Modifier.fillMaxSize(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                items.forEach { item ->
                    val selected = currentTab == item.tab
                    val color by animateColorAsState(
                        targetValue = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary,
                        label = "icon_color"
                    )
                    val scale by animateFloatAsState(
                        targetValue = if (selected) 1.2f else 1f,
                        label = "icon_scale"
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
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = item.icon,
                                contentDescription = null,
                                tint = color,
                                modifier = Modifier
                                    .size(24.dp)
                                    .graphicsLayer(scaleX = scale, scaleY = scale)
                            )
                            AnimatedVisibility(visible = selected) {
                                Box(
                                    modifier = Modifier
                                        .padding(top = 4.dp)
                                        .size(4.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.primary)
                                )
                            }
                        }
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
    val icon = if (currentTab == TabScreen.Home) Icons.Default.Add else Icons.Default.AddHome
    val text = if (currentTab == TabScreen.Home) "Item" else "Space"
    
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(if (isPressed) 0.9f else 1f, label = "fab_scale")

    FloatingActionButton(
        onClick = {
            if (currentTab == TabScreen.Home) {
                onNavigateToSub(SubScreen.AddEditItem())
            } else {
                onNavigateToSub(SubScreen.AddEditSpace())
            }
        },
        containerColor = Color.Transparent,
        contentColor = Color.White,
        modifier = Modifier
            .navigationBarsPadding()
            .padding(bottom = 80.dp)
            .graphicsLayer(scaleX = scale, scaleY = scale)
            .shadow(12.dp, CircleShape, spotColor = PrimaryAccent.copy(alpha = 0.5f))
    ) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .background(
                    Brush.linearGradient(
                        colors = listOf(PrimaryPurple, PrimaryAccent)
                    ),
                    CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = "Add $text",
                modifier = Modifier.size(28.dp)
            )
        }
    }
}

@Composable
fun SyncIndicator(state: SyncState) {
    if (state == SyncState.IDLE || state == SyncState.SYNCED || state == SyncState.COMPLETED) return
    
    val text = when (state) {
        SyncState.SYNCING -> "Syncing..."
        SyncState.UPLOADING -> "Uploading changes..."
        SyncState.DOWNLOADING -> "Downloading data..."
        SyncState.WAITING_FOR_INTERNET -> "Waiting for internet..."
        SyncState.DIRTY -> "Changes queued"
        SyncState.FAILED -> "Sync failed"
        SyncState.DELETED_PENDING_SYNC -> "Syncing deletions"
        else -> ""
    }
    
    if (text == "") return

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(28.dp),
        color = PrimaryPurple.copy(alpha = 0.9f)
    ) {
        Row(
            modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            KeepsyGradientLoader(size = 14.dp, strokeWidth = 2.dp)
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = text, 
                fontSize = 11.sp, 
                fontWeight = FontWeight.Bold,
                color = Color.White,
                letterSpacing = 0.5.sp
            )
        }
    }
}
