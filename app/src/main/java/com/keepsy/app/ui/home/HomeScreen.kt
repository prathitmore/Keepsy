package com.keepsy.app.ui.home

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.keepsy.app.navigation.SubScreen
import com.keepsy.app.navigation.TabScreen
import com.keepsy.app.ui.components.*
import com.keepsy.app.ui.theme.*
import com.keepsy.app.viewmodel.KeepsyViewModel

import com.keepsy.app.ui.tutorial.TutorialViewModel
import com.keepsy.app.ui.tutorial.tutorialSpotlight

@Composable
fun HomeScreen(
    viewModel: KeepsyViewModel,
    onTabSelected: (TabScreen) -> Unit,
    onNavigateToSub: (SubScreen) -> Unit,
    tutorialViewModel: TutorialViewModel? = null
) {
    val stats by viewModel.appStatistics.collectAsStateWithLifecycle()
    val favoritesList by viewModel.favoriteSpaces.collectAsStateWithLifecycle()
    val recentItemsList by viewModel.recentItems.collectAsStateWithLifecycle()

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 100.dp)
    ) {
        // Quick Stats Area
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .then(if (tutorialViewModel != null) Modifier.tutorialSpotlight("stats_area", tutorialViewModel) else Modifier)
                    .padding(horizontal = 24.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                StatCard(
                    value = "${stats.totalItems}",
                    label = "Total Items",
                    icon = Icons.Default.Inventory2,
                    modifier = Modifier.weight(1f)
                )
                StatCard(
                    value = "${stats.totalSpaces}",
                    label = "Storage Spaces",
                    icon = Icons.Default.Layers,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // Pinned / Favorite Spaces
        if (favoritesList.isNotEmpty()) {
            item {
                SectionHeader(
                    title = "Favorite Spaces",
                    subtitle = "Quick access to your main hubs",
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp)
                )
            }
            item {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 24.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.padding(bottom = 8.dp)
                ) {
                    items(favoritesList, key = { "fav_${it.spaceId}" }) { space ->
                        SpaceHorizontalCard(
                            space = space, 
                            onClick = { onNavigateToSub(SubScreen.SpaceDetails(space.spaceId)) }
                        )
                    }
                }
            }
        }

        // Recently Added
        item {
            SectionHeader(
                title = "Recently Added",
                subtitle = "New memories in your inventory",
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp)
            )
        }

        if (recentItemsList.isEmpty()) {
            item {
                EmptyState(
                    icon = {
                        Icon(
                            imageVector = Icons.Default.Inventory2,
                            contentDescription = null,
                            tint = PrimaryAccent,
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
                    },
                    modifier = Modifier.padding(top = 24.dp)
                )
            }
        } else {
            items(recentItemsList.take(1), key = { "recent_tut_${it.item.itemId}" }) { itemDetails ->
                Box(modifier = if (tutorialViewModel != null) Modifier.tutorialSpotlight("item_card_0", tutorialViewModel) else Modifier) {
                    ItemRowCard(
                        itemDetails = itemDetails, 
                        onClick = { onNavigateToSub(SubScreen.ItemDetails(itemDetails.item.itemId)) }
                    )
                }
            }
            items(recentItemsList.drop(1), key = { "recent_${it.item.itemId}" }) { itemDetails ->
                Box(modifier = Modifier.padding(horizontal = 24.dp)) {
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
    value: String,
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        color = SurfaceSecondary,
        shape = RoundedCornerShape(24.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.05f))
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(PrimaryPurple.copy(alpha = 0.1f), RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = PrimaryPurple,
                    modifier = Modifier.size(20.dp)
                )
            }
            Column {
                Text(
                    text = value,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.ExtraBold,
                    color = TextPrimary
                )
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    color = TextSecondary,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}
