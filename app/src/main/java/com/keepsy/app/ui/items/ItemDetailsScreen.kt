package com.keepsy.app.ui.items

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.keepsy.app.model.Space
import com.keepsy.app.navigation.SubScreen
import com.keepsy.app.ui.components.TimelineCard
import com.keepsy.app.ui.theme.*
import com.keepsy.app.utils.getSmartItemIconVector
import com.keepsy.app.utils.getSpaceIconVector
import com.keepsy.app.utils.parseCategoryColor
import com.keepsy.app.viewmodel.KeepsyViewModel
import com.keepsy.app.ui.tutorial.TutorialViewModel
import com.keepsy.app.ui.tutorial.tutorialSpotlight
import java.io.File
import kotlin.ranges.coerceIn

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ItemDetailsScreen(
    itemId: Long,
    viewModel: KeepsyViewModel,
    onPop: () -> Unit,
    onNavigateToSub: (SubScreen) -> Unit,
    tutorialViewModel: TutorialViewModel? = null
) {
    val context = LocalContext.current
    val itemDetails by viewModel.selectedItem.collectAsStateWithLifecycle()
    val activityTrail by viewModel.getActivityTrailForItem(itemId).collectAsState(initial = emptyList())
    val allSpaces by viewModel.spaces.collectAsStateWithLifecycle(emptyList())

    val scrollState = rememberScrollState()
    val spacePath = remember(itemDetails?.space, allSpaces) {
        val details = itemDetails ?: return@remember emptyList<Space>()
        val path = mutableListOf<Space>()
        var currentSpace = details.space
        while (currentSpace != null) {
            path.add(0, currentSpace)
            val parentId = currentSpace.parentSpaceId
            currentSpace = if (parentId != null) {
                allSpaces.find { it.spaceId == parentId }
            } else {
                null
            }
        }
        path
    }

    var displayDeleteDialog by remember { mutableStateOf(false) }

    LaunchedEffect(itemId) {
        viewModel.selectItem(itemId)
    }

    Scaffold(
        topBar = {
            val alpha = (scrollState.value / 300f).coerceIn(0f, 1f)
            TopAppBar(
                title = { 
                    AnimatedVisibility(
                        visible = alpha > 0.5f,
                        enter = fadeIn() + slideInVertically(),
                        exit = fadeOut() + slideOutVertically()
                    ) {
                        Text(
                            text = itemDetails?.item?.name ?: "",
                            style = MaterialTheme.typography.titleMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = onPop,
                        colors = IconButtonDefaults.iconButtonColors(
                            containerColor = if (alpha < 0.5f) Color.Black.copy(alpha = 0.3f) else Color.Transparent
                        )
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack, 
                            contentDescription = "Back",
                            tint = TextPrimary
                        )
                    }
                },
                actions = {
                    itemDetails?.let { details ->
                        IconButton(
                            onClick = { viewModel.toggleItemFavorite(details.item.itemId) },
                            colors = IconButtonDefaults.iconButtonColors(
                                containerColor = if (alpha < 0.5f) Color.Black.copy(alpha = 0.3f) else Color.Transparent
                            )
                        ) {
                            Icon(
                                imageVector = if (details.item.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                contentDescription = "Favorite",
                                tint = if (details.item.isFavorite) ErrorRed else TextPrimary
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Background.copy(alpha = alpha),
                    scrolledContainerColor = Background
                )
            )
        }
    ) { innerPadding ->
        itemDetails?.let { details ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState)
                    .background(Background)
                    .padding(bottom = innerPadding.calculateBottomPadding())
            ) {
                // Large Hero Section
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(360.dp)
                ) {
                    if (details.item.photoPath != null && File(details.item.photoPath).exists()) {
                        AsyncImage(
                            model = File(details.item.photoPath),
                            contentDescription = details.item.name,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        val catColor = parseCategoryColor(details.category?.color)
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    Brush.verticalGradient(
                                        colors = listOf(catColor.copy(alpha = 0.8f), catColor.copy(alpha = 0.2f))
                                    )
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = getSmartItemIconVector(details.item.name, details.category?.icon),
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(100.dp)
                            )
                        }
                    }
                    
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(Color.Transparent, Background.copy(alpha = 0.8f), Background),
                                    startY = 600f
                                )
                            )
                    )
                }

                Column(
                    modifier = Modifier
                        .padding(horizontal = 24.dp)
                        .offset(y = (-24).dp)
                ) {
                    Surface(
                        color = parseCategoryColor(details.category?.color).copy(alpha = 0.15f),
                        contentColor = parseCategoryColor(details.category?.color),
                        shape = CircleShape,
                        border = BorderStroke(1.dp, parseCategoryColor(details.category?.color).copy(alpha = 0.3f))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = com.keepsy.app.utils.getCategoryIconVector(details.category?.icon),
                                contentDescription = null,
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = details.category?.name ?: "Other",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = details.item.name,
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    Card(
                        colors = CardDefaults.cardColors(containerColor = CardBackground),
                        shape = RoundedCornerShape(24.dp),
                        border = BorderStroke(1.dp, BorderColor.copy(alpha = 0.5f))
                    ) {
                        Column(modifier = Modifier.padding(20.dp)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(PrimaryAccent.copy(alpha = 0.1f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Layers,
                                        contentDescription = null,
                                        tint = PrimaryAccent,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                                Text(
                                    text = "Storage Path",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary
                                )
                            }
                            
                            Spacer(modifier = Modifier.height(16.dp))
                            
                            if (spacePath.isNotEmpty()) {
                                FlowRow(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    spacePath.forEachIndexed { index, space ->
                                        if (index > 0) {
                                            Icon(
                                                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                                contentDescription = null,
                                                tint = TextSecondary.copy(alpha = 0.5f),
                                                modifier = Modifier.size(16.dp).align(Alignment.CenterVertically)
                                            )
                                        }
                                        Surface(
                                            onClick = { onNavigateToSub(SubScreen.SpaceDetails(space.spaceId)) },
                                            color = if (space == details.space) PrimaryAccent else Background.copy(alpha = 0.4f),
                                            contentColor = if (space == details.space) Color.White else TextPrimary,
                                            shape = RoundedCornerShape(12.dp)
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                                            ) {
                                                Icon(
                                                    imageVector = getSpaceIconVector(space.icon),
                                                    contentDescription = null,
                                                    modifier = Modifier.size(14.dp)
                                                )
                                                Text(
                                                    text = space.name,
                                                    style = MaterialTheme.typography.labelLarge,
                                                    fontWeight = if (space == details.space) FontWeight.Bold else FontWeight.Medium
                                                )
                                            }
                                        }
                                    }
                                }
                            } else {
                                Text(
                                    text = "No location assigned",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = TextSecondary
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(32.dp))

                    if (details.item.description != "") {
                        DetailSection(title = "Description", content = details.item.description)
                        Spacer(modifier = Modifier.height(24.dp))
                    }

                    if (details.item.notes != "") {
                        DetailSection(
                            title = "Retrieval Notes", 
                            content = details.item.notes,
                            isItalic = true,
                            containerColor = CardBackground.copy(alpha = 0.5f)
                        )
                        Spacer(modifier = Modifier.height(24.dp))
                    }

                    if (details.tags.isNotEmpty()) {
                        Text(
                            text = "Labels",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = TextSecondary
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            details.tags.forEach { tag ->
                                SuggestionChip(
                                    onClick = { /* TODO: Search by tag */ },
                                    label = { Text("#${tag.name}") },
                                    colors = SuggestionChipDefaults.suggestionChipColors(
                                        containerColor = PrimaryAccent.copy(alpha = 0.05f),
                                        labelColor = PrimaryAccent
                                    ),
                                    border = null
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(32.dp))
                    }

                    Text(
                        text = "Memory Trail",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = TextSecondary
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    if (activityTrail.isEmpty()) {
                        Text(
                            text = "No recent activity recorded.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary.copy(alpha = 0.6f)
                        )
                    } else {
                        activityTrail.forEach { log ->
                            TimelineCard(log = log, onClickItem = {})
                        }
                    }

                    Spacer(modifier = Modifier.height(48.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Button(
                            onClick = { onNavigateToSub(SubScreen.MoveItem(details.item.itemId)) },
                            modifier = Modifier
                                .weight(1f)
                                .height(56.dp)
                                .then(if (tutorialViewModel != null) Modifier.tutorialSpotlight("move_item_btn", tutorialViewModel) else Modifier),
                            shape = RoundedCornerShape(20.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = PrimaryAccent)
                        ) {
                            Icon(imageVector = Icons.Default.MultipleStop, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Relocate", fontWeight = FontWeight.Bold)
                        }

                        OutlinedButton(
                            onClick = { onNavigateToSub(SubScreen.AddEditItem(itemId = details.item.itemId)) },
                            modifier = Modifier.weight(1f).height(56.dp),
                            shape = RoundedCornerShape(20.dp),
                            border = BorderStroke(1.dp, BorderColor)
                        ) {
                            Icon(imageVector = Icons.Default.Edit, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Edit", fontWeight = FontWeight.Bold)
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    TextButton(
                        onClick = { displayDeleteDialog = true },
                        modifier = Modifier.fillMaxWidth().height(56.dp),
                        colors = ButtonDefaults.textButtonColors(contentColor = ErrorRed)
                    ) {
                        Icon(imageVector = Icons.Default.DeleteOutline, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Move to Trash", fontWeight = FontWeight.Bold)
                    }
                    
                    Spacer(modifier = Modifier.height(48.dp))
                }
            }
        }
    }

    if (displayDeleteDialog) {
        AlertDialog(
            onDismissRequest = { displayDeleteDialog = false },
            containerColor = CardBackground,
            titleContentColor = TextPrimary,
            textContentColor = TextSecondary,
            title = { Text("Move to Trash?", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold) },
            text = { Text("This item will be hidden but can be restored from the Trash Bin later.") },
            confirmButton = {
                Button(
                    onClick = {
                        displayDeleteDialog = false
                        viewModel.softDeleteSelectedItem {
                            onPop()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ErrorRed)
                ) {
                    Text("Move to Trash", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { displayDeleteDialog = false }) {
                    Text("Cancel", color = TextSecondary)
                }
            }
        )
    }
}

@Composable
fun DetailSection(
    title: String,
    content: String,
    isItalic: Boolean = false,
    containerColor: Color = Color.Transparent
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(containerColor)
            .padding(if (containerColor != Color.Transparent) 16.dp else 0.dp)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = TextSecondary
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = content,
            style = MaterialTheme.typography.bodyLarge,
            color = TextPrimary,
            fontStyle = if (isItalic) androidx.compose.ui.text.font.FontStyle.Italic else androidx.compose.ui.text.font.FontStyle.Normal,
            lineHeight = 24.sp
        )
    }
}
