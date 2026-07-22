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
import androidx.compose.ui.graphics.Brush
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
import com.keepsy.app.ui.components.AccountBottomSheet
import com.keepsy.app.ui.components.KeepsyBackgroundEffects
import com.keepsy.app.ui.home.HomeScreen
import com.keepsy.app.ui.spaces.SpacesScreen
import com.keepsy.app.ui.search.SearchScreen
import com.keepsy.app.ui.activity.ActivityScreen
import com.keepsy.app.ui.settings.SettingsScreen
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

    if (currentSubScreen == SubScreen.AccountCenter) {
        AccountBottomSheet(
            profile = profile,
            onClose = onPopSub,
            onNavigateToSub = onNavigateToSub,
            onSignOut = { viewModel.signOut() }
        )
    }

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
                    }
                }
            }
        }

        // NAVIGATION BAR - TRUE FLOATING OVERLAY
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
                    text = if (profile != null) "Welcome back, ${profile.displayName}" else "Welcome back to Keepsy",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.secondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        
        Spacer(modifier = Modifier.width(16.dp))

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
                Text(
                    text = com.keepsy.app.utils.AvatarUtils.getInitials(profile?.name),
                    style = MaterialTheme.typography.titleMedium,
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
            .padding(bottom = 20.dp, start = 24.dp, end = 24.dp)
            .fillMaxWidth(),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            modifier = Modifier
                .height(64.dp)
                .fillMaxWidth(0.95f), // Slightly narrower for floating feel
            shape = RoundedCornerShape(32.dp),
            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.1f)),
            shadowElevation = 16.dp
        ) {
            Row(
                modifier = Modifier.fillMaxSize(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                items.forEach { item ->
                    val selected = currentTab == item.tab
                    val color by animateColorAsState(
                        targetValue = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary.copy(alpha = 0.7f),
                        label = "icon_color"
                    )
                    val scale by animateFloatAsState(
                        targetValue = if (selected) 1.25f else 1f,
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
                                    .size(26.dp)
                                    .graphicsLayer(scaleX = scale, scaleY = scale)
                            )
                            if (selected) {
                                Box(
                                    modifier = Modifier
                                        .padding(top = 2.dp)
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
    LargeFloatingActionButton(
        onClick = { 
            if (currentTab == TabScreen.Spaces) {
                onNavigateToSub(SubScreen.AddEditSpace())
            } else {
                onNavigateToSub(SubScreen.AddEditItem())
            }
        },
        containerColor = MaterialTheme.colorScheme.primary,
        contentColor = Color.Black,
        shape = RoundedCornerShape(24.dp),
        modifier = Modifier.padding(bottom = 80.dp, end = 8.dp) // Move FAB up to clear the Navbar
    ) {
        Icon(
            imageVector = if (currentTab == TabScreen.Spaces) Icons.Default.AddHomeWork else Icons.Default.Add,
            contentDescription = "Add",
            modifier = Modifier.size(32.dp)
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
            SyncState.SYNCING -> "Refreshing memory..."
            SyncState.UPLOADING -> "Saving to cloud..."
            SyncState.DOWNLOADING -> "Fetching updates..."
            SyncState.FAILED -> "Sync paused"
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
                .padding(vertical = 4.dp),
            contentAlignment = Alignment.Center
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (state != SyncState.FAILED) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(12.dp),
                        strokeWidth = 2.dp,
                        color = color
                    )
                    Spacer(modifier = Modifier.width(8.dp))
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
