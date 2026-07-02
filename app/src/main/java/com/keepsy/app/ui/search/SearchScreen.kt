package com.keepsy.app.ui.search

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.keepsy.app.navigation.SubScreen
import com.keepsy.app.ui.components.*
import com.keepsy.app.ui.theme.*
import com.keepsy.app.ui.tutorial.TutorialViewModel
import com.keepsy.app.ui.tutorial.tutorialSpotlight
import com.keepsy.app.viewmodel.KeepsyViewModel

@Composable
fun SearchScreen(
    viewModel: KeepsyViewModel, 
    onNavigateToSub: (SubScreen) -> Unit,
    tutorialViewModel: TutorialViewModel? = null
) {
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val searchResults by viewModel.searchResults.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp)
    ) {
        KeepsySearchBar(
            query = searchQuery,
            onQueryChange = { viewModel.updateSearchQuery(it) },
            placeholder = "Search items, spaces, or notes...",
            modifier = Modifier
                .padding(bottom = 16.dp)
                .then(if (tutorialViewModel != null) Modifier.tutorialSpotlight("search_bar", tutorialViewModel) else Modifier)
        )

        if (searchQuery == "") {
            EmptyState(
                icon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = null,
                        tint = PrimaryAccent,
                        modifier = Modifier.size(32.dp)
                    )
                },
                title = "Search Memories",
                description = "Type anything to find your physical items instantly. We'll search names, notes, and locations.",
                modifier = Modifier.weight(1f)
            )
        } else if (searchResults.items.isEmpty() && searchResults.spaces.isEmpty()) {
            EmptyState(
                icon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = null,
                        tint = ErrorRed,
                        modifier = Modifier.size(32.dp)
                    )
                },
                title = "No results found",
                description = "We couldn't find anything matching '$searchQuery'. Try different keywords.",
                modifier = Modifier.weight(1f)
            )
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(bottom = 100.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (searchResults.spaces.isNotEmpty()) {
                    item {
                        SectionHeader(
                            title = "Spaces",
                            modifier = Modifier.padding(vertical = 12.dp)
                        )
                    }
                    items(searchResults.spaces, key = { "search_space_${it.spaceId}" }) { space ->
                        SpaceHorizontalCard(
                            space = space, 
                            onClick = { onNavigateToSub(SubScreen.SpaceDetails(space.spaceId)) }
                        )
                    }
                }

                if (searchResults.items.isNotEmpty()) {
                    item {
                        SectionHeader(
                            title = "Items",
                            modifier = Modifier.padding(vertical = 12.dp)
                        )
                    }
                    items(searchResults.items, key = { "search_item_${it.item.itemId}" }) { itemDetails ->
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
