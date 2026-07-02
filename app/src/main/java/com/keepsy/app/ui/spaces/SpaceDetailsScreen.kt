package com.keepsy.app.ui.spaces

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.keepsy.app.navigation.SubScreen
import com.keepsy.app.ui.components.ItemRowCard
import com.keepsy.app.ui.theme.*
import com.keepsy.app.ui.tutorial.TutorialViewModel
import com.keepsy.app.ui.tutorial.tutorialSpotlight
import com.keepsy.app.utils.getSpaceIconVector
import com.keepsy.app.viewmodel.KeepsyViewModel
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SpaceDetailsScreen(
    spaceId: Long,
    viewModel: KeepsyViewModel,
    onPop: () -> Unit,
    onNavigateToSub: (SubScreen) -> Unit,
    tutorialViewModel: TutorialViewModel? = null
) {
    val context = LocalContext.current
    val spaceDetails by viewModel.selectedSpace.collectAsStateWithLifecycle()
    val nestedList by viewModel.nestedSubspaces.collectAsStateWithLifecycle()
    val itemsInThisSpace by viewModel.itemsInSpace.collectAsStateWithLifecycle()

    var showDeleteAlert by remember { mutableStateOf(false) }

    LaunchedEffect(spaceId) {
        viewModel.selectSpace(spaceId)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(spaceDetails?.space?.name ?: "Space details", maxLines = 1, overflow = TextOverflow.Ellipsis) },
                navigationIcon = {
                    IconButton(onClick = onPop) {
                        Icon(imageVector = Icons.AutoMirrored.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    spaceDetails?.let { details ->
                        IconButton(onClick = { viewModel.toggleSpaceFavorite(details.space.spaceId) }) {
                            Icon(
                                imageVector = if (details.space.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                tint = if (details.space.isFavorite) MutedRedDanger else Color.Gray,
                                contentDescription = "Fav"
                            )
                        }
                    }
                }
            )
        }
    ) { innerPadding ->
        spaceDetails?.let { details ->
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .background(MaterialTheme.colorScheme.background),
                contentPadding = PaddingValues(16.dp)
            ) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp)
                            .clip(RoundedCornerShape(12.dp))
                    ) {
                        if (details.space.photoPath != null && File(details.space.photoPath).exists()) {
                            AsyncImage(
                                model = File(details.space.photoPath),
                                contentDescription = details.space.name,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        } else {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(HighlightTeal.copy(alpha = 0.2f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = getSpaceIconVector(details.space.icon),
                                    contentDescription = "",
                                    tint = HighlightTeal,
                                    modifier = Modifier.size(64.dp)
                                )
                            }
                        }
                    }
                }

                item {
                    Column(modifier = Modifier.padding(vertical = 16.dp)) {
                        Text(text = details.space.name, fontSize = 26.sp, fontWeight = FontWeight.Bold)
                        if (details.space.description.isNotEmpty()) {
                            Text(text = details.space.description, fontSize = 14.sp, color = Color.Gray)
                        }
                        if (details.parentSpace != null) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(text = "Nested inside: ${details.parentSpace.name}", fontSize = 12.sp, color = HighlightTeal)
                        }
                    }
                }

                if (nestedList.isNotEmpty()) {
                    item {
                        Text(text = "Nested Sub-spaces", fontWeight = FontWeight.Bold, fontSize = 16.sp, modifier = Modifier.padding(bottom = 8.dp))
                    }
                    items(nestedList) { sub ->
                        Card(
                            onClick = { onNavigateToSub(SubScreen.SpaceDetails(sub.spaceId)) },
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 3.dp)
                        ) {
                            Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(imageVector = getSpaceIconVector(sub.icon), contentDescription = null, tint = HighlightTeal, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(text = sub.name, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            }
                        }
                    }
                    item { Spacer(modifier = Modifier.height(16.dp)) }
                }

                item {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = "Tracked Items Inside", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        IconButton(onClick = { onNavigateToSub(SubScreen.AddEditItem(spaceId = details.space.spaceId)) }) {
                            Icon(imageVector = Icons.Default.Add, contentDescription = "Add Item Here", tint = HighlightTeal)
                        }
                    }
                }

                if (itemsInThisSpace.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(text = "No items tracked inside this space.", color = Color.Gray, fontSize = 14.sp)
                        }
                    }
                } else {
                    items(itemsInThisSpace) { item ->
                        ItemRowCard(itemDetails = item, onClick = { onNavigateToSub(SubScreen.ItemDetails(item.item.itemId)) })
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(24.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { onNavigateToSub(SubScreen.AddEditSpace(spaceId = details.space.spaceId)) },
                            colors = ButtonDefaults.buttonColors(containerColor = HighlightTeal),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(imageVector = Icons.Default.Edit, contentDescription = "")
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Edit Space")
                        }

                        Button(
                            onClick = { onNavigateToSub(SubScreen.AddEditSpace(parentSpaceId = details.space.spaceId)) },
                            colors = ButtonDefaults.buttonColors(containerColor = DeepIndigoPrimary),
                            modifier = Modifier
                                .weight(1f)
                                .then(if (tutorialViewModel != null) Modifier.tutorialSpotlight("add_subspace_btn", tutorialViewModel) else Modifier)
                        ) {
                            Icon(imageVector = Icons.Default.Subtitles, contentDescription = "")
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Add Sub-Space")
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Button(
                        onClick = { showDeleteAlert = true },
                        colors = ButtonDefaults.buttonColors(containerColor = MutedRedDanger),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(imageVector = Icons.Default.Delete, contentDescription = "")
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Delete Space container")
                    }
                    Spacer(modifier = Modifier.height(24.dp))
                }
            }
        }
    }

    if (showDeleteAlert) {
        AlertDialog(
            onDismissRequest = { showDeleteAlert = false },
            title = { Text("Confirm Space Deletion?") },
            text = { Text("Deleting this space container will un-nest any child sub-spaces dynamically. Items currently mapped directly to this Space container will stay preserved.") },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteAlert = false
                        viewModel.deleteSpace(spaceId) {
                            Toast.makeText(context, "Space container deleted.", Toast.LENGTH_SHORT).show()
                            onPop()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MutedRedDanger)
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteAlert = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}
