package com.keepsy.app.ui.settings

import android.os.Build
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.keepsy.app.BuildConfig
import com.keepsy.app.ui.components.*
import com.keepsy.app.ui.theme.*
import com.keepsy.app.viewmodel.KeepsyViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SecurityScreen(
    viewModel: KeepsyViewModel,
    onPop: () -> Unit
) {
    val context = LocalContext.current
    var showPasswordDialog by remember { mutableStateOf(false) }

    if (showPasswordDialog) {
        ChangePasswordDialog(
            onDismiss = { showPasswordDialog = false },
            onConfirm = { current, new ->
                viewModel.changePassword(current, new) {
                    Toast.makeText(context, "Password updated successfully", Toast.LENGTH_SHORT).show()
                    showPasswordDialog = false
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Security Center", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onPop) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
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
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text("Authentication", style = MaterialTheme.typography.titleSmall, color = TextSecondary, fontWeight = FontWeight.Bold)

            SecurityItem(
                icon = Icons.Default.VerifiedUser,
                title = "Email Verification",
                subtitle = if (viewModel.isEmailVerified()) "Verified" else "Action required",
                color = if (viewModel.isEmailVerified()) SuccessGreen else ErrorRed
            )

            SecurityItem(
                icon = Icons.Default.LockReset,
                title = "Change Password",
                subtitle = "Last changed: Unknown",
                onClick = { showPasswordDialog = true }
            )

            Spacer(modifier = Modifier.height(8.dp))
            Text("Current Device Info", style = MaterialTheme.typography.titleSmall, color = TextSecondary, fontWeight = FontWeight.Bold)

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = SurfaceSecondary),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.03f))
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    DeviceInfoRow("Model", Build.MODEL)
                    DeviceInfoRow("Manufacturer", Build.MANUFACTURER)
                    DeviceInfoRow("OS Version", "Android ${Build.VERSION.RELEASE}")
                    DeviceInfoRow("App Version", "v${BuildConfig.VERSION_NAME}")
                    DeviceInfoRow("Platform", "Keepsy for Android")
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = { viewModel.signOut() },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                colors = ButtonDefaults.buttonColors(containerColor = ErrorRed.copy(alpha = 0.1f)),
                shape = RoundedCornerShape(16.dp)
            ) {
                Icon(Icons.Default.Logout, contentDescription = null, tint = ErrorRed)
                Spacer(modifier = Modifier.width(12.dp))
                Text("Sign out from this device", color = ErrorRed, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun DeviceInfoRow(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
        Text(value, style = MaterialTheme.typography.bodyMedium, color = TextPrimary, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun SecurityItem(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String, subtitle: String, color: Color = PrimaryAccent, onClick: () -> Unit = {}) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceSecondary),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.03f))
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, contentDescription = null, tint = color)
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold, color = TextPrimary)
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
            }
        }
    }
}

@Composable
fun ChangePasswordDialog(onDismiss: () -> Unit, onConfirm: (String, String) -> Unit) {
    var current by remember { mutableStateOf("") }
    var new by remember { mutableStateOf("") }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Change Password", color = TextPrimary) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                KeepsyTextField(value = current, onValueChange = { current = it }, label = "Current Password", isPassword = true)
                KeepsyTextField(value = new, onValueChange = { new = it }, label = "New Password", isPassword = true)
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(current, new) }) {
                Text("Update", color = PrimaryAccent)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = TextSecondary)
            }
        },
        containerColor = SurfaceSecondary
    )
}
