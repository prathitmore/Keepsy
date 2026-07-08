package com.keepsy.app.ui.home

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.keepsy.app.model.SyncState
import com.keepsy.app.navigation.SubScreen
import com.keepsy.app.navigation.TabScreen
import com.keepsy.app.ui.components.*
import com.keepsy.app.ui.theme.*
import com.keepsy.app.viewmodel.KeepsyViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: KeepsyViewModel,
    onTabSelected: (TabScreen) -> Unit,
    onNavigateToSub: (SubScreen) -> Unit
) {
    val stats by viewModel.appStatistics.collectAsStateWithLifecycle()
    val recentlyAdded by viewModel.recentItems.collectAsStateWithLifecycle()
    val favorites by viewModel.favoriteItems.collectAsStateWithLifecycle()
    val isRestoring by viewModel.isRestoringData.collectAsStateWithLifecycle()
    val syncStatus by viewModel.syncState.collectAsStateWithLifecycle()

    val isRefreshing = remember(syncStatus) {
        syncStatus == SyncState.SYNCING || 
        syncStatus == SyncState.UPLOADING || 
        syncStatus == SyncState.DOWNLOADING
    }

    val pullToRefreshState = rememberPullToRefreshState()

    PullToRefreshBox(
        isRefreshing = isRefreshing,
        onRefresh = { viewModel.manualSync() },
        state = pullToRefreshState,
        indicator = {
            PullToRefreshDefaults.Indicator(
                state = pullToRefreshState,
                isRefreshing = isRefreshing,
                containerColor = MaterialTheme.colorScheme.surface,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.align(Alignment.TopCenter)
            )
        },
        modifier = Modifier.fillMaxSize()
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background),
            contentPadding = PaddingValues(bottom = 100.dp)
        ) {
            // 1. Search Shortcut
            item {
                Box(
                    modifier = Modifier
                        .padding(horizontal = 24.dp, vertical = 8.dp)
                        .fillMaxWidth()
                        .height(60.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.4f))
                        .border(BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.1f)), RoundedCornerShape(20.dp))
                        .clickable { 
                            viewModel.updateSearchQuery("")
                            onTabSelected(TabScreen.Search) 
                        }
                        .padding(horizontal = 20.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Search, 
                            contentDescription = null, 
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Text(
                            text = "Search items, spaces, or notes...",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                        )
                    }
                }
            }

            // 2. Statistics Overview
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    if (isRestoring) {
                        repeat(2) {
                            Box(modifier = Modifier.weight(1f).height(110.dp).clip(RoundedCornerShape(28.dp)).shimmerLoadingAnimation())
                        }
                    } else {
                        StatCard(
                            title = "Total Items",
                            value = stats.totalItems.toString(),
                            icon = Icons.Default.Inventory2,
                            modifier = Modifier.weight(1f)
                        )
                        StatCard(
                            title = "Spaces",
                            value = stats.totalSpaces.toString(),
                            icon = Icons.Default.Layers,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            // 3. Favorites Section (Pinned)
            if (favorites.isNotEmpty() || isRestoring) {
                item {
                    SectionHeader(
                        title = "Pinned Favorites",
                        modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp),
                        action = {
                            TextButton(onClick = { onTabSelected(TabScreen.Search) }) {
                                Text("See All", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                            }
                        }
                    )
                }
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp)
                            .padding(bottom = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        if (isRestoring) {
                            repeat(3) {
                                Box(modifier = Modifier.weight(1f).height(180.dp).clip(RoundedCornerShape(24.dp)).shimmerLoadingAnimation())
                            }
                        } else {
                            favorites.take(3).forEach { itemDetails ->
                                PremiumItemCard(
                                    itemDetails = itemDetails,
                                    onClick = { onNavigateToSub(SubScreen.ItemDetails(itemDetails.item.itemId)) },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                            val remaining = 3 - favorites.take(3).size
                            if (remaining > 0) {
                                repeat(remaining) {
                                    Spacer(modifier = Modifier.weight(1f))
                                }
                            }
                        }
                    }
                }
            }

            // 4. Recently Added Section
            item {
                SectionHeader(
                    title = "Recently Added",
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp)
                )
            }

            if (isRestoring) {
                items(5) {
                    Box(modifier = Modifier.padding(horizontal = 24.dp, vertical = 6.dp)) {
                        ItemCardShimmer()
                    }
                }
            } else if (recentlyAdded.isEmpty()) {
                item {
                    EmptyState(
                        icon = {
                            Icon(
                                imageVector = Icons.Default.Inbox,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(32.dp)
                            )
                        },
                        title = "No memories yet",
                        description = "Start by remembering something important in your first space.",
                        action = {
                            PrimaryGradientButton(
                                text = "Add First Item",
                                onClick = { onNavigateToSub(SubScreen.AddEditItem()) },
                                modifier = Modifier.width(200.dp)
                            )
                        }
                    )
                }
            } else {
                items(recentlyAdded) { itemDetails ->
                    Box(modifier = Modifier.padding(horizontal = 24.dp, vertical = 6.dp)) {
                        ItemRowCard(
                            itemDetails = itemDetails,
                            onClick = { onNavigateToSub(SubScreen.ItemDetails(itemDetails.item.itemId)) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun StatCard(
    title: String,
    value: String,
    icon: ImageVector,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.height(110.dp),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.05f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp)
            )
            Column {
                Text(
                    text = value,
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.secondary
                )
            }
        }
    }
}
