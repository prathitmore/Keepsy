package com.keepsy.app.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.keepsy.app.navigation.SubScreen
import com.keepsy.app.navigation.TabScreen
import com.keepsy.app.ui.components.*
import com.keepsy.app.ui.theme.*
import com.keepsy.app.viewmodel.KeepsyViewModel

@Composable
fun HomeScreen(
    viewModel: KeepsyViewModel,
    onTabSelected: (TabScreen) -> Unit,
    onNavigateToSub: (SubScreen) -> Unit
) {
    val stats by viewModel.appStatistics.collectAsStateWithLifecycle()
    val recentlyAdded by viewModel.recentItems.collectAsStateWithLifecycle()
    val favorites by viewModel.favoriteItems.collectAsStateWithLifecycle()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(Background),
        contentPadding = PaddingValues(bottom = 100.dp)
    ) {
        // Statistics Area
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
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

        // Favorites Section
        if (favorites.isNotEmpty()) {
            item {
                SectionHeader(title = "Pinned Favorites")
            }
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp)
                        .padding(bottom = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    favorites.take(3).forEach { itemDetails ->
                        PremiumItemCard(
                            itemDetails = itemDetails,
                            onClick = { onNavigateToSub(SubScreen.ItemDetails(itemDetails.item.itemId)) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }

        // Recently Added Section
        item {
            SectionHeader(title = "Recently Added")
        }

        if (recentlyAdded.isEmpty()) {
            item {
                EmptyState(
                    icon = {
                        Icon(
                            imageVector = Icons.Default.Inbox,
                            contentDescription = null,
                            tint = PrimaryPurple,
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
        colors = CardDefaults.cardColors(containerColor = SurfaceSecondary),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.05f))
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
                tint = PrimaryAccent,
                modifier = Modifier.size(24.dp)
            )
            Column {
                Text(
                    text = value,
                    style = MaterialTheme.typography.headlineSmall,
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelMedium,
                    color = TextSecondary
                )
            }
        }
    }
}
