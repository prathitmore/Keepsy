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

    val displayRecentlyAdded = remember(recentlyAdded) { recentlyAdded.take(4) }

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
            contentPadding = PaddingValues(bottom = 120.dp) // Bottom padding for navbar
        ) {
            // 1. Search Shortcut
            item {
                Box(
                    modifier = Modifier
                        .padding(horizontal = 24.dp, vertical = 8.dp)
                        .fillMaxWidth()
                        .height(54.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.4f))
                        .border(BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.1f)), RoundedCornerShape(16.dp))
                        .clickable { 
                            viewModel.updateSearchQuery("")
                            onTabSelected(TabScreen.Search) 
                        }
                        .padding(horizontal = 16.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Default.Search, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(text = "Search everything...", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f))
                    }
                }
            }

            // 2. Statistics
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    StatCard(title = "Total Items", value = stats.totalItems.toString(), icon = Icons.Default.Inventory2, modifier = Modifier.weight(1f))
                    StatCard(title = "Spaces", value = stats.totalSpaces.toString(), icon = Icons.Default.Layers, modifier = Modifier.weight(1f))
                }
            }

            // 3. Favorites Section
            if (favorites.isNotEmpty()) {
                item {
                    SectionHeader(
                        title = "Pinned Favorites",
                        modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp),
                        action = {
                            TextButton(onClick = { onTabSelected(TabScreen.Search) }) {
                                Text("See All", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            }
                        }
                    )
                }
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp).padding(bottom = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        favorites.take(3).forEach { item ->
                            PremiumItemCard(
                                itemDetails = item,
                                onClick = { onNavigateToSub(SubScreen.ItemDetails(item.item.itemId)) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                        repeat(3 - favorites.take(3).size) { Spacer(modifier = Modifier.weight(1f)) }
                    }
                }
            }

            // 4. Recently Added (Small Rows)
            item {
                SectionHeader(title = "Recently Added", modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp))
            }

            if (isRestoring) {
                items(5) { Box(modifier = Modifier.padding(horizontal = 24.dp, vertical = 4.dp)) { ItemCardShimmer() } }
            } else if (recentlyAdded.isEmpty()) {
                item { EmptyState(icon = { Icon(Icons.Default.Inbox, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(32.dp)) }, title = "No items yet", description = "Add your first item to start organizing.", action = { PrimaryGradientButton("Add First Item", { onNavigateToSub(SubScreen.AddEditItem()) }, Modifier.width(200.dp)) }) }
            } else {
                items(displayRecentlyAdded) { item ->
                    Box(modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)) {
                        ItemRowCard(itemDetails = item, onClick = { onNavigateToSub(SubScreen.ItemDetails(item.item.itemId)) })
                    }
                }
            }
        }
    }
}

@Composable
fun StatCard(title: String, value: String, icon: ImageVector, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.height(110.dp),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.05f))
    ) {
        Column(modifier = Modifier.fillMaxSize().padding(18.dp), verticalArrangement = Arrangement.SpaceBetween) {
            Icon(imageVector = icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(26.dp))
            Column {
                Text(text = value, style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.ExtraBold)
                Text(text = title, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary)
            }
        }
    }
}
