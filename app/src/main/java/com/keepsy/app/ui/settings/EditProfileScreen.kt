package com.keepsy.app.ui.settings

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.keepsy.app.ui.components.KeepsyTextField
import com.keepsy.app.ui.theme.*
import com.keepsy.app.utils.AvatarUtils
import com.keepsy.app.utils.KeepsyLogger
import com.keepsy.app.viewmodel.KeepsyViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditProfileScreen(
    viewModel: KeepsyViewModel,
    onPop: () -> Unit
) {
    val profile by viewModel.userProfile.collectAsStateWithLifecycle()
    val isUpdating by viewModel.isUpdatingProfile.collectAsStateWithLifecycle()
    
    var name by remember(profile) { mutableStateOf(profile?.name ?: "") }
    var displayName by remember(profile) { mutableStateOf(profile?.displayName ?: "") }
    var localPhotoUri by remember { mutableStateOf<Uri?>(null) }

    var showPhotoOptions by remember { mutableStateOf(false) }

    val context = LocalContext.current
    var tempCameraUri by remember { mutableStateOf<Uri?>(null) }

    val photoLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { 
            localPhotoUri = it
            viewModel.updateProfilePicture(it) 
        }
    }

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success: Boolean ->
        if (success) {
            tempCameraUri?.let { 
                localPhotoUri = it
                viewModel.updateProfilePicture(it) 
            }
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            val uri = com.keepsy.app.utils.ImageUtils.createTempImageUri(context)
            tempCameraUri = uri
            cameraLauncher.launch(uri)
        } else {
            Toast.makeText(context, "Camera permission required to take photos", Toast.LENGTH_SHORT).show()
        }
    }

    if (showPhotoOptions) {
        ModalBottomSheet(
            onDismissRequest = { showPhotoOptions = false },
            containerColor = SurfaceSecondary,
            dragHandle = { BottomSheetDefaults.DragHandle(color = BorderColor) }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .padding(bottom = 40.dp)
            ) {
                Text("Profile Photo", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = TextPrimary)
                Spacer(modifier = Modifier.height(24.dp))
                
                PhotoOptionItem(Icons.Default.Camera, "Take Photo") {
                    showPhotoOptions = false
                    val permissionCheck = ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA)
                    if (permissionCheck == PackageManager.PERMISSION_GRANTED) {
                        val uri = com.keepsy.app.utils.ImageUtils.createTempImageUri(context)
                        tempCameraUri = uri
                        cameraLauncher.launch(uri)
                    } else {
                        permissionLauncher.launch(Manifest.permission.CAMERA)
                    }
                }

                PhotoOptionItem(Icons.Default.PhotoLibrary, "Choose from Gallery") {
                    showPhotoOptions = false
                    photoLauncher.launch("image/*")
                }
                
                // Show remove option if there is a photo (either URL or local preview)
                if ((profile?.photoUrl != null && profile?.photoUrl != "") || localPhotoUri != null) {
                    PhotoOptionItem(Icons.Default.Delete, "Remove Photo", color = ErrorRed) {
                        showPhotoOptions = false
                        localPhotoUri = null
                        viewModel.removeProfilePicture()
                    }
                }
                
                PhotoOptionItem(Icons.Default.Close, "Cancel") {
                    showPhotoOptions = false
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Edit Profile", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onPop, enabled = !isUpdating) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (isUpdating) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = PrimaryAccent,
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                    } else {
                        TextButton(onClick = {
                            viewModel.updateProfile(name, displayName)
                            onPop()
                        }) {
                            Text("Save", color = PrimaryAccent, fontWeight = FontWeight.Bold)
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Background)
            )
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Background)
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp, vertical = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                // Avatar Edit
                Box(contentAlignment = Alignment.BottomEnd) {
                    Box(
                        modifier = Modifier
                            .size(120.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.linearGradient(
                                    colors = listOf(PrimaryPurple, PrimaryAccent)
                                )
                            )
                            .clickable(enabled = !isUpdating) { showPhotoOptions = true },
                        contentAlignment = Alignment.Center
                    ) {
                        if (localPhotoUri != null && isUpdating) {
                            AsyncImage(
                                model = ImageRequest.Builder(LocalContext.current)
                                    .data(localPhotoUri)
                                    .crossfade(true)
                                    .build(),
                                contentDescription = "Preview",
                                modifier = Modifier.fillMaxSize().alpha(0.5f),
                                contentScale = ContentScale.Crop
                            )
                            CircularProgressIndicator(color = Color.White)
                        } else if (profile?.photoUrl != null && profile?.photoUrl != "") {
                            AsyncImage(
                                model = ImageRequest.Builder(LocalContext.current)
                                    .data(profile?.photoUrl)
                                    .crossfade(true)
                                    .build(),
                                contentDescription = "Profile Picture",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            val initials = if (name != "") AvatarUtils.getInitials(name) else "?"
                            Text(
                                text = initials,
                                color = Color.White,
                                style = MaterialTheme.typography.displaySmall,
                                fontWeight = FontWeight.ExtraBold
                            )
                        }
                    }
                    
                    IconButton(
                        onClick = { showPhotoOptions = true },
                        enabled = !isUpdating,
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(if (isUpdating) Color.Gray else PrimaryAccent)
                    ) {
                        Icon(Icons.Default.CameraAlt, contentDescription = "Change Photo", tint = Color.Black, modifier = Modifier.size(20.dp))
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                KeepsyTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = "Full Name",
                    leadingIcon = Icons.Default.Person,
                    enabled = !isUpdating
                )

                KeepsyTextField(
                    value = displayName,
                    onValueChange = { displayName = it },
                    label = "Display Name (Public)",
                    leadingIcon = Icons.Default.Badge,
                    enabled = !isUpdating
                )

                KeepsyTextField(
                    value = profile?.email ?: "",
                    onValueChange = { },
                    label = "Email Address (Read-only)",
                    leadingIcon = Icons.Default.Email,
                    enabled = false
                )
                
                Text(
                    text = "Email can only be changed from the Security Center for your protection.",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
            
            if (isUpdating) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = Color.Black.copy(alpha = 0.4f)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = SurfaceSecondary),
                            shape = RoundedCornerShape(20.dp),
                            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 32.dp, vertical = 24.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(20.dp)
                            ) {
                                CircularProgressIndicator(color = PrimaryAccent, modifier = Modifier.size(24.dp), strokeWidth = 3.dp)
                                Text("Syncing...", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PhotoOptionItem(
    icon: ImageVector,
    text: String,
    color: Color = TextPrimary,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(imageVector = icon, contentDescription = null, tint = color.copy(alpha = 0.8f), modifier = Modifier.size(24.dp))
        Spacer(modifier = Modifier.width(20.dp))
        Text(text = text, style = MaterialTheme.typography.bodyLarge, color = color, fontWeight = FontWeight.Medium)
    }
}
