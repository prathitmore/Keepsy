package com.keepsy.app.ui.settings

import android.net.Uri
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.keepsy.app.ui.components.*
import com.keepsy.app.ui.theme.*
import com.keepsy.app.utils.AvatarUtils
import com.keepsy.app.viewmodel.KeepsyViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditProfileScreen(
    viewModel: KeepsyViewModel,
    onPop: () -> Unit
) {
    val profile by viewModel.userProfile.collectAsStateWithLifecycle()
    
    var name by remember(profile) { mutableStateOf(profile?.name ?: "") }
    var displayName by remember(profile) { mutableStateOf(profile?.displayName ?: "") }

    var showPhotoOptions by remember { mutableStateOf(false) }

    val context = androidx.compose.ui.platform.LocalContext.current
    var tempCameraUri by remember { mutableStateOf<Uri?>(null) }

    val photoLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { viewModel.updateProfilePicture(it) }
    }

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success: Boolean ->
        if (success) {
            tempCameraUri?.let { viewModel.updateProfilePicture(it) }
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
                    val uri = com.keepsy.app.utils.ImageUtils.createTempImageUri(context)
                    tempCameraUri = uri
                    cameraLauncher.launch(uri)
                }

                PhotoOptionItem(Icons.Default.PhotoLibrary, "Choose from Gallery") {
                    showPhotoOptions = false
                    photoLauncher.launch("image/*")
                }
                
                if (profile?.photoUrl != null && profile?.photoUrl != "") {
                    PhotoOptionItem(Icons.Default.Delete, "Remove Photo", color = ErrorRed) {
                        showPhotoOptions = false
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
                    IconButton(onClick = onPop) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    TextButton(onClick = {
                        viewModel.updateProfile(name, displayName)
                        onPop()
                    }) {
                        Text("Save", color = PrimaryAccent, fontWeight = FontWeight.Bold)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Background)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Background)
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // Avatar Edit
            Box(contentAlignment = Alignment.BottomEnd) {
                Box(
                    modifier = Modifier
                        .size(100.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.linearGradient(
                                colors = listOf(PrimaryPurple, PrimaryAccent)
                            )
                        )
                        .clickable { showPhotoOptions = true },
                    contentAlignment = Alignment.Center
                ) {
                    var showInitials by remember(profile?.photoUrl) { 
                        mutableStateOf(profile?.photoUrl == null || profile?.photoUrl == "") 
                    }
                    
                    if (!showInitials) {
                        AsyncImage(
                            model = profile?.photoUrl,
                            contentDescription = "Profile Picture",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop,
                            onError = { showInitials = true }
                        )
                    }
                    
                    if (showInitials) {
                        Text(
                            text = AvatarUtils.getInitials(name),
                            color = Color.White,
                            style = MaterialTheme.typography.displaySmall,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                }
                
                IconButton(
                    onClick = { showPhotoOptions = true },
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(PrimaryAccent)
                ) {
                    Icon(Icons.Default.CameraAlt, contentDescription = "Change Photo", tint = Color.Black, modifier = Modifier.size(16.dp))
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            KeepsyTextField(
                value = name,
                onValueChange = { name = it },
                label = "Full Name",
                leadingIcon = Icons.Default.Person
            )

            KeepsyTextField(
                value = displayName,
                onValueChange = { displayName = it },
                label = "Display Name (Public)",
                leadingIcon = Icons.Default.Badge
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
    }
}

@Composable
fun PhotoOptionItem(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, color: Color = TextPrimary, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = color.copy(alpha = 0.7f))
        Spacer(modifier = Modifier.width(16.dp))
        Text(label, style = MaterialTheme.typography.bodyLarge, color = color, fontWeight = FontWeight.Medium)
    }
}
