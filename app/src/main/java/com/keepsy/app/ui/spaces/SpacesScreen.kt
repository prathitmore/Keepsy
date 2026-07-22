package com.keepsy.app.ui.spaces

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddHome
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.keepsy.app.navigation.SubScreen
import com.keepsy.app.ui.components.*
import com.keepsy.app.ui.theme.*
import com.keepsy.app.viewmodel.KeepsyViewModel

@Composable
fun SpacesScreen(
    viewModel: KeepsyViewModel, 
    onNavigateToSub: (SubScreen) -> Unit
) {
    val spacesList by viewModel.spaces.collectAsStateWithLifecycle(emptyList())
    val isRestoring by viewModel.isRestoringData.collectAsStateWithLifecycle()
    val expandedSpaceIds = remember { mutableStateListOf<Long>() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp)
    ) {
        if (isRestoring) {
            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(top = 8.dp, bottom = 100.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(8) {
                    SpaceCardShimmer()
                }
            }
        } else if (spacesList.isEmpty()) {
            EmptyState(
                icon = {
                    Icon(
                        imageVector = Icons.Default.Layers,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(32.dp)
                    )
                },
                title = "No storage map built",
                description = "Build your physical structure. Rooms, closets, drawers, and boxes work best here.",
                action = {
                    PrimaryGradientButton(
                        text = "Create First Space",
                        onClick = { onNavigateToSub(SubScreen.AddEditSpace()) },
                        modifier = Modifier.width(220.dp)
                    )
                },
                modifier = Modifier.weight(1f)
            )
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(top = 8.dp, bottom = 100.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val rootSpaces = spacesList.filter { it.parentSpaceId == null || it.parentSpaceId == 0L }
                
                items(rootSpaces, key = { "root_${it.spaceId}" }) { rootSpace ->
                    SpaceTreeNode(
                        space = rootSpace,
                        allSpaces = spacesList,
                        depth = 0,
                        expandedIds = expandedSpaceIds,
                        onNodeClick = { spaceId ->
                            if (expandedSpaceIds.contains(spaceId)) {
                                expandedSpaceIds.remove(spaceId)
                            } else {
                                expandedSpaceIds.add(spaceId)
                            }
                        },
                        onNodeDetails = { spaceId -> onNavigateToSub(SubScreen.SpaceDetails(spaceId)) }
                    )
                }
            }
        }
    }
}
