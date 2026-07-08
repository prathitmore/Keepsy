package com.keepsy.app.ui.items

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.keepsy.app.R
import com.keepsy.app.navigation.SubScreen
import com.keepsy.app.ui.components.*
import com.keepsy.app.ui.theme.*
import com.keepsy.app.utils.getSpaceIconVector
import com.keepsy.app.viewmodel.KeepsyViewModel
import java.io.File

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ItemDetailsScreen(
    itemId: Long,
    viewModel: KeepsyViewModel,
    onPop: () -> Unit,
    onNavigateToSub: (SubScreen) -> Unit
) {
    val itemDetails by viewModel.selectedItem.collectAsStateWithLifecycle()
    val activityTrail by viewModel.getActivityTrailForItem(itemId).collectAsStateWithLifecycle(emptyList())

    var spacePath by remember { mutableStateOf("") }
    var showDeleteDialog by remember { mutableStateOf(false) }

    LaunchedEffect(itemId) {
        viewModel.selectItem(itemId)
    }

    LaunchedEffect(itemDetails) {
        itemDetails?.space?.let {
            spacePath = viewModel.getFullSpacePath(it.spaceId)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Item Details", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onPop) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    itemDetails?.let { details ->
                        IconButton(onClick = { viewModel.toggleItemFavorite(details.item.itemId) }) {
                            Icon(
                                imageVector = if (details.item.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                tint = if (details.item.isFavorite) ErrorRed else TextSecondary,
                                contentDescription = "Favorite"
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Background)
            )
        }
    ) { innerPadding ->
        itemDetails?.let { details ->
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .background(Background),
                contentPadding = PaddingValues(24.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                // Photo Section
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(280.dp),
                        shape = RoundedCornerShape(32.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
                    ) {
                        Box(modifier = Modifier.fillMaxSize()) {
                            if (details.item.photoPath != null && File(details.item.photoPath).exists()) {
                                AsyncImage(
                                    model = File(details.item.photoPath),
                                    contentDescription = details.item.name,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                            } else {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(
                                            Brush.verticalGradient(
                                                colors = listOf(PrimaryPurple.copy(alpha = 0.2f), CardBackground)
                                            )
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Inventory2,
                                        contentDescription = null,
                                        tint = PrimaryPurple.copy(alpha = 0.4f),
                                        modifier = Modifier.size(80.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                // Title & Description
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = details.item.name,
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = TextPrimary
                        )
                        if (details.item.description != "") {
                            Text(
                                text = details.item.description,
                                style = MaterialTheme.typography.bodyLarge,
                                color = TextSecondary,
                                lineHeight = 24.sp
                            )
                        }
                    }
                }

                // Tags
                if (details.tags.isNotEmpty()) {
                    item {
                        FlowRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            details.tags.forEach { tag ->
                                Surface(
                                    color = PrimaryAccent.copy(alpha = 0.1f),
                                    shape = RoundedCornerShape(8.dp),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, PrimaryAccent.copy(alpha = 0.2f))
                                ) {
                                    Text(
                                        text = "#${tag.name}",
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                        style = MaterialTheme.typography.labelMedium,
                                        color = PrimaryAccent,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }

                // Location Card
                item {
                    SectionHeader(title = "Stored In")
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(containerColor = SurfaceSecondary),
                        onClick = {
                            details.space?.let { onNavigateToSub(SubScreen.SpaceDetails(it.spaceId)) }
                        }
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(PrimaryAccent.copy(alpha = 0.1f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = getSpaceIconVector(details.space?.icon ?: "home"),
                                    contentDescription = null,
                                    tint = PrimaryAccent,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(16.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = if (spacePath != "") spacePath else (details.space?.name ?: "Unknown Location"),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary
                                )
                                Text(
                                    text = "Physical Map Skeleton",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = PrimaryAccent,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Icon(imageVector = Icons.Default.ChevronRight, contentDescription = null, tint = TextSecondary)
                        }
                    }
                }

                // Notes Section
                if (details.item.notes != "") {
                    item {
                        SectionHeader(title = "Retrieval Notes")
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(24.dp),
                            color = CardBackground,
                            border = androidx.compose.foundation.BorderStroke(1.dp, BorderColor.copy(alpha = 0.5f))
                        ) {
                            Text(
                                text = details.item.notes,
                                modifier = Modifier.padding(20.dp),
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextPrimary,
                                lineHeight = 22.sp
                            )
                        }
                    }
                }

                // Action Buttons
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        SecondaryButton(
                            text = "Move Item",
                            onClick = { onNavigateToSub(SubScreen.MoveItem(itemId)) },
                            modifier = Modifier.weight(1f)
                        )
                        SecondaryButton(
                            text = "Edit Info",
                            onClick = { onNavigateToSub(SubScreen.AddEditItem(itemId)) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    TextButton(
                        onClick = { showDeleteDialog = true },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.textButtonColors(contentColor = ErrorRed)
                    ) {
                        Icon(imageVector = Icons.Default.DeleteOutline, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Move to Trash")
                    }
                }

                // Activity Trail
                if (activityTrail.isNotEmpty()) {
                    item {
                        SectionHeader(title = "Memory Trail")
                    }
                    items(activityTrail) { log ->
                        ActivityLogCard(log = log)
                    }
                }
                
                item { Spacer(modifier = Modifier.height(40.dp)) }
            }
        }
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Move to Trash?") },
            text = { Text("The item will be moved to your Trash Bin. You can restore it anytime or delete it permanently from there.") },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteDialog = false
                        viewModel.softDeleteSelectedItem {
                            onPop()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ErrorRed)
                ) {
                    Text("Move to Trash", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Cancel", color = TextSecondary)
                }
            },
            containerColor = CardBackground,
            titleContentColor = TextPrimary,
            textContentColor = TextSecondary
        )
    }
}
