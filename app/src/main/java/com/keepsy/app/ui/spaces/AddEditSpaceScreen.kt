package com.keepsy.app.ui.spaces

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.graphics.vector.ImageVector
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
import com.keepsy.app.utils.getSpaceIconLabel
import com.keepsy.app.utils.getSpaceIconVector
import com.keepsy.app.viewmodel.KeepsyViewModel
import kotlinx.coroutines.flow.first
import java.io.File
import androidx.compose.foundation.shape.RoundedCornerShape

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditSpaceScreen(
    spaceId: Long?,
    parentSpaceId: Long?,
    viewModel: KeepsyViewModel,
    onPop: () -> Unit
) {
    val context = LocalContext.current
    val spacesList by viewModel.spaces.collectAsStateWithLifecycle(emptyList())

    var name by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var icon by remember { mutableStateOf("home") }
    var isFavorite by remember { mutableStateOf(false) }
    var selectedParentId by remember { mutableStateOf(parentSpaceId) }
    var photoUri by remember { mutableStateOf<Uri?>(null) }
    var existingPhotoPath by remember { mutableStateOf<String?>(null) }
    var existingPhotoUrl by remember { mutableStateOf<String?>(null) }

    var showParentDrop by remember { mutableStateOf(false) }

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

    LaunchedEffect(spaceId) {
        if (spaceId != null && spaceId != 0L) {
            val space = viewModel.spaces.first().find { it.spaceId == spaceId }
            if (space != null) {
                name = space.name
                description = space.description
                icon = space.icon ?: "home"
                isFavorite = space.isFavorite
                selectedParentId = space.parentSpaceId
                existingPhotoPath = space.photoPath
                existingPhotoUrl = space.photoUrl
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        text = if (spaceId == null || spaceId == 0L) "New Space" else "Edit Space",
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
                        val photoModel = remember(photoUri, existingPhotoPath, existingPhotoUrl) {
                            if (photoUri != null) {
                                photoUri
                            } else if (existingPhotoPath != null && File(existingPhotoPath!!).exists()) {
                                File(existingPhotoPath!!)
                            } else {
                                existingPhotoUrl
                            }
                        }

                        if (photoModel != null) {
                            AsyncImage(
                                model = photoModel,
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
                            text = "Space Photo", 
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
                        if (photoUri != null || existingPhotoPath != null || existingPhotoUrl != null) {
                            TextButton(
                                onClick = { 
                                    photoUri = null 
                                    existingPhotoPath = null
                                    existingPhotoUrl = null
                                },
                                colors = ButtonDefaults.textButtonColors(contentColor = ErrorRed)
                            ) {
                                Text("Remove Photo", style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }
                }
            }

            KeepsyTextField(
                value = name,
                onValueChange = { name = it },
                label = "Space Name *",
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
                modifier = Modifier.testTag("space_form_name_input")
            )

            KeepsyTextField(
                value = description,
                onValueChange = { description = it },
                label = "Description"
            )

            // Parent Space Selector
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showParentDrop = true }
            ) {
                val availableSpaces = spacesList.filter { it.spaceId != spaceId }
                val parentSpace = availableSpaces.find { it.spaceId == selectedParentId }
                KeepsyTextField(
                    value = parentSpace?.name ?: "No Parent (Root level)",
                    onValueChange = {},
                    label = "Parent Space (Nesting)",
                    enabled = false,
                    trailingIcon = { 
                        Icon(imageVector = Icons.Default.ArrowDropDown, contentDescription = null, tint = TextSecondary) 
                    }
                )
                DropdownMenu(
                    expanded = showParentDrop,
                    onDismissRequest = { showParentDrop = false },
                    modifier = Modifier.fillMaxWidth(0.85f).background(CardBackground)
                ) {
                    DropdownMenuItem(
                        text = { Text("No Parent (Root level)", color = TextPrimary) },
                        onClick = {
                            selectedParentId = null
                            showParentDrop = false
                        }
                    )
                    availableSpaces.forEach { space ->
                        DropdownMenuItem(
                            text = { Text(space.name, color = TextPrimary) },
                            onClick = {
                                selectedParentId = space.spaceId
                                showParentDrop = false
                            }
                        )
                    }
                }
            }

            // Icon Picker
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Representation Icon", 
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = TextSecondary
                )
                Surface(
                    color = PrimaryAccent.copy(alpha = 0.1f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = getSpaceIconLabel(icon),
                        style = MaterialTheme.typography.labelSmall,
                        color = PrimaryAccent,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            
            val iconIdList = listOf(
                "home", "bedroom", "kitchen", "bathroom", "garage",
                "office", "warehouse", "store", "workshop", "garden",
                "car", "inbox", "box", "drawer", "closet",
                "shelf", "cabinet", "backpack", "suitcase", "safe",
                "archive", "devices", "lock"
            )

            // Display icons in multiple rows
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                iconIdList.chunked(5).forEach { chunk ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        chunk.forEach { currentIconId ->
                            val iconVec = getSpaceIconVector(currentIconId)
                            val isSelected = icon == currentIconId
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(if (isSelected) PrimaryAccent else CardBackground)
                                    .border(1.dp, if (isSelected) PrimaryAccent else BorderColor, RoundedCornerShape(12.dp))
                                    .clickable { icon = currentIconId }
                                    .padding(8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = iconVec, 
                                    contentDescription = null, 
                                    tint = if (isSelected) Color.White else PrimaryAccent,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }
                    }
                }
            }

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
                text = "Save Space",
                onClick = {
                    if (name != "") {
                        viewModel.saveSpace(
                            spaceId = spaceId ?: 0L,
                            name = name.trim(),
                            description = description.trim(),
                            parentSpaceId = selectedParentId,
                            icon = icon,
                            photoUri = photoUri,
                            isFavorite = isFavorite
                        ) {
                            onPop()
                        }
                    } else {
                        Toast.makeText(context, "Space name is required", Toast.LENGTH_SHORT).show()
                    }
                },
                modifier = Modifier.testTag("submit_space_form_btn")
            )
            
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}
