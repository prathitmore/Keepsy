package com.keepsy.app.ui.items

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.keepsy.app.ui.components.*
import com.keepsy.app.ui.theme.*
import com.keepsy.app.viewmodel.KeepsyViewModel
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditItemScreen(
    itemId: Long?,
    initialSpaceId: Long?,
    viewModel: KeepsyViewModel,
    onPop: () -> Unit
) {
    val context = LocalContext.current
    val spacesList by viewModel.spaces.collectAsStateWithLifecycle(emptyList())
    val categoriesList by viewModel.categories.collectAsStateWithLifecycle(emptyList())

    var name by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    var tagsInput by remember { mutableStateOf("") }
    var isFavorite by remember { mutableStateOf(false) }

    var selectedSpaceId by remember { mutableStateOf(initialSpaceId ?: 0L) }
    var selectedCategoryId by remember { mutableStateOf(0L) }
    var photoUri by remember { mutableStateOf<Uri?>(null) }

    var showSpacesDrop by remember { mutableStateOf(false) }
    var showCategoriesDrop by remember { mutableStateOf(false) }

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            photoUri = uri
        }
    }

    var tempCameraUri by remember { mutableStateOf<Uri?>(null) }
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (success && tempCameraUri != null) {
            photoUri = tempCameraUri
        }
    }

    LaunchedEffect(itemId) {
        if (itemId != null && itemId != 0L) {
            val itemDetails = viewModel.getItemWithDetails(itemId)
            if (itemDetails != null) {
                val itObj = itemDetails.item
                name = itObj.name
                description = itObj.description
                notes = itObj.notes
                isFavorite = itObj.isFavorite
                selectedSpaceId = itObj.spaceId
                selectedCategoryId = itObj.categoryId
                tagsInput = itemDetails.tags.joinToString(", ") { tag -> tag.name }
            }
        }
    }

    LaunchedEffect(spacesList, categoriesList) {
        if (itemId == null || itemId == 0L) {
            if (selectedSpaceId == 0L && spacesList.isNotEmpty()) {
                selectedSpaceId = spacesList.first().spaceId
            }
            if (selectedCategoryId == 0L && categoriesList.isNotEmpty()) {
                selectedCategoryId = categoriesList.first().categoryId
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        text = if (itemId == null || itemId == 0L) "New Item" else "Edit Item",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    ) 
                },
                navigationIcon = {
                    IconButton(onClick = onPop) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack, 
                            contentDescription = "Back",
                            tint = TextPrimary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Background)
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(Background)
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // Photo Picker Card
            Card(
                colors = CardDefaults.cardColors(containerColor = CardBackground),
                shape = RoundedCornerShape(24.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, BorderColor.copy(alpha = 0.5f))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(100.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(PrimaryAccent.copy(alpha = 0.05f)),
                        contentAlignment = Alignment.Center
                    ) {
                        if (photoUri != null) {
                            AsyncImage(
                                model = photoUri, 
                                contentDescription = null, 
                                contentScale = ContentScale.Crop, 
                                modifier = Modifier.fillMaxSize()
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.AddAPhoto, 
                                contentDescription = null, 
                                tint = PrimaryAccent, 
                                modifier = Modifier.size(32.dp)
                            )
                        }
                    }

                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "Item Photo", 
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            SecondaryButton(
                                text = "Gallery",
                                onClick = { galleryLauncher.launch("image/*") },
                                modifier = Modifier.weight(1f).height(40.dp)
                            )
                            SecondaryButton(
                                text = "Camera",
                                onClick = {
                                    try {
                                        val tempFile = File(context.cacheDir, "cap_${System.currentTimeMillis()}.jpg")
                                        val authority = "${context.packageName}.fileprovider"
                                        val uri = FileProvider.getUriForFile(context, authority, tempFile)
                                        tempCameraUri = uri
                                        cameraLauncher.launch(uri)
                                    } catch (e: Exception) {
                                        Toast.makeText(context, "Cannot open camera", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                modifier = Modifier.weight(1f).height(40.dp)
                            )
                        }
                        if (photoUri != null) {
                            TextButton(
                                onClick = { photoUri = null },
                                colors = ButtonDefaults.textButtonColors(contentColor = ErrorRed)
                            ) {
                                Text("Remove Photo", style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }
                }
            }

            // Form Fields
            KeepsyTextField(
                value = name,
                onValueChange = { name = it },
                label = "Item Name *",
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
                modifier = Modifier.testTag("item_form_name_input")
            )

            // Location Selector
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showSpacesDrop = true }
            ) {
                val currentSpace = spacesList.find { it.spaceId == selectedSpaceId }
                KeepsyTextField(
                    value = currentSpace?.name ?: "Select Space *",
                    onValueChange = {},
                    label = "Storage Space",
                    enabled = false,
                    trailingIcon = { 
                        Icon(imageVector = Icons.Default.ArrowDropDown, contentDescription = null, tint = TextSecondary) 
                    }
                )
                DropdownMenu(
                    expanded = showSpacesDrop,
                    onDismissRequest = { showSpacesDrop = false },
                    modifier = Modifier.fillMaxWidth(0.85f).background(CardBackground)
                ) {
                    spacesList.forEach { space ->
                        DropdownMenuItem(
                            text = { Text(space.name, color = TextPrimary) },
                            onClick = {
                                selectedSpaceId = space.spaceId
                                showSpacesDrop = false
                            }
                        )
                    }
                }
            }

            // Category Selector
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showCategoriesDrop = true }
            ) {
                val currentCategory = categoriesList.find { it.categoryId == selectedCategoryId }
                KeepsyTextField(
                    value = currentCategory?.name ?: "Select Category *",
                    onValueChange = {},
                    label = "Category",
                    enabled = false,
                    trailingIcon = { 
                        Icon(imageVector = Icons.Default.ArrowDropDown, contentDescription = null, tint = TextSecondary) 
                    }
                )
                DropdownMenu(
                    expanded = showCategoriesDrop,
                    onDismissRequest = { showCategoriesDrop = false },
                    modifier = Modifier.fillMaxWidth(0.85f).background(CardBackground)
                ) {
                    categoriesList.forEach { cat ->
                        DropdownMenuItem(
                            text = { Text(cat.name, color = TextPrimary) },
                            onClick = {
                                selectedCategoryId = cat.categoryId
                                showCategoriesDrop = false
                            }
                        )
                    }
                }
            }

            KeepsyTextField(
                value = description,
                onValueChange = { description = it },
                label = "Description"
            )

            KeepsyTextField(
                value = notes,
                onValueChange = { notes = it },
                label = "Retrieval Notes",
                placeholder = "e.g. Hidden inside the red pocket..."
            )

            KeepsyTextField(
                value = tagsInput,
                onValueChange = { tagsInput = it },
                label = "Tags (comma separated)",
                placeholder = "travel, urgent, personal"
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Checkbox(
                    checked = isFavorite,
                    onCheckedChange = { isFavorite = it },
                    colors = CheckboxDefaults.colors(checkedColor = PrimaryAccent, uncheckedColor = BorderColor)
                )
                Text(
                    text = "Pin to Favorites", 
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextPrimary
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            PrimaryButton(
                text = "Save Item",
                onClick = {
                    if (name != "" && selectedSpaceId != 0L) {
                        val tagList = tagsInput.split(",")
                            .map { tag: String -> tag.trim() }
                            .filter { tag: String -> tag != "" }

                        viewModel.saveItem(
                            itemId = itemId ?: 0L,
                            name = name.trim(),
                            description = description.trim(),
                            spaceId = selectedSpaceId,
                            categoryId = selectedCategoryId,
                            notes = notes.trim(),
                            photoUri = photoUri,
                            tagList = tagList,
                            isFavorite = isFavorite
                        ) {
                            onPop()
                        }
                    } else {
                        Toast.makeText(context, "Please fill required fields", Toast.LENGTH_SHORT).show()
                    }
                },
                modifier = Modifier.testTag("submit_item_form_btn")
            )
            
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}
