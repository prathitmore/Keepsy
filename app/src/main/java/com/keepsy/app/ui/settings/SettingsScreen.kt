package com.keepsy.app.ui.settings

import android.content.Context
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.keepsy.app.navigation.SubScreen
import com.keepsy.app.ui.components.*
import com.keepsy.app.ui.theme.*
import com.keepsy.app.viewmodel.KeepsyViewModel
import kotlinx.coroutines.launch
import androidx.compose.foundation.Image
import androidx.compose.ui.res.painterResource
import com.keepsy.app.R
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun SettingsScreen(viewModel: KeepsyViewModel, onNavigateToSub: (SubScreen) -> Unit) {
    val darkModePreferred by viewModel.darkModePreference.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    val saveBackupLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri != null) {
            viewModel.exportBackup { json ->
                scope.launch {
                    try {
                        context.contentResolver.openOutputStream(uri)?.use { os ->
                            os.write(json.toByteArray())
                        }
                        Toast.makeText(context, "Backup saved to device.", Toast.LENGTH_SHORT).show()
                    } catch (e: Exception) {
                        Toast.makeText(context, "Export failed.", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    val openBackupLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            scope.launch {
                try {
                    val content = context.contentResolver.openInputStream(uri)?.use { inputStream ->
                        inputStream.readBytes().toString(Charsets.UTF_8)
                    }
                    if (!content.isNullOrBlank()) {
                        viewModel.importBackup(content) { success ->
                            if (success) {
                                Toast.makeText(context, "Backup restored successfully.", Toast.LENGTH_SHORT).show()
                            } else {
                                Toast.makeText(context, "Invalid backup file.", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                } catch (e: Exception) {
                    Toast.makeText(context, "Restore failed.", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 24.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            PremiumSettingsCard(title = "Appearance") {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "System Theme",
                        style = MaterialTheme.typography.labelMedium,
                        color = TextSecondary
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        ThemeOption(
                            label = "System",
                            selected = darkModePreferred == null,
                            onClick = { viewModel.setDarkModePreference(null) },
                            modifier = Modifier.weight(1f)
                        )
                        ThemeOption(
                            label = "Light",
                            selected = darkModePreferred == false,
                            onClick = { viewModel.setDarkModePreference(false) },
                            modifier = Modifier.weight(1f)
                        )
                        ThemeOption(
                            label = "Dark",
                            selected = darkModePreferred == true,
                            onClick = { viewModel.setDarkModePreference(true) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }

        item {
            PremiumSettingsCard(title = "Data Management") {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    SettingsActionRow(
                        icon = Icons.Default.Backup,
                        title = "Export Local Backup",
                        subtitle = "Save a JSON file of your memories",
                        onClick = {
                            val time = SimpleDateFormat("yyyyMMdd_HHmm", Locale.getDefault()).format(Date())
                            saveBackupLauncher.launch("keepsy_backup_$time.json")
                        }
                    )
                    HorizontalDivider(color = Color.White.copy(alpha = 0.05f))
                    SettingsActionRow(
                        icon = Icons.Default.Restore,
                        title = "Restore from File",
                        subtitle = "Import data from a previous export",
                        onClick = {
                            openBackupLauncher.launch(arrayOf("application/json", "*/*"))
                        }
                    )
                }
            }
        }

        item {
            PremiumSettingsCard(title = "Account & System") {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    HorizontalDivider(color = Color.White.copy(alpha = 0.05f))
                    SettingsActionRow(
                        icon = Icons.AutoMirrored.Filled.Logout,
                        title = "Sign Out",
                        subtitle = "Safely end your session",
                        iconColor = ErrorRed,
                        onClick = {
                            viewModel.signOut {
                                Toast.makeText(context, "Signed out.", Toast.LENGTH_SHORT).show()
                            }
                        }
                    )
                    HorizontalDivider(color = Color.White.copy(alpha = 0.05f))
                    SettingsActionRow(
                        icon = Icons.Default.DeleteForever,
                        title = "Wipe All Data",
                        subtitle = "Clear everything on this device",
                        iconColor = ErrorRed,
                        onClick = {
                            viewModel.resetApp()
                            Toast.makeText(context, "All data wiped.", Toast.LENGTH_SHORT).show()
                        }
                    )
                }
            }
        }

        item {
            Column(
                modifier = Modifier.fillMaxWidth().padding(vertical = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Surface(
                    modifier = Modifier.size(48.dp),
                    shape = RoundedCornerShape(14.dp),
                    color = SurfaceSecondary,
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.05f))
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Image(
                            painter = painterResource(id = R.drawable.ic_keepsy_logo),
                            contentDescription = null,
                            modifier = Modifier.size(32.dp).clip(RoundedCornerShape(8.dp))
                        )
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Keepsy Founders Edition",
                    style = MaterialTheme.typography.labelLarge,
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Version 1.0.0",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextSecondary.copy(alpha = 0.6f)
                )
                Spacer(modifier = Modifier.height(100.dp))
            }
        }
    }
}

@Composable
fun PremiumSettingsCard(title: String, content: @Composable () -> Unit) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            color = PrimaryAccent,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(start = 4.dp, bottom = 12.dp)
        )
        Surface(
            color = SurfaceSecondary,
            shape = RoundedCornerShape(24.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.03f))
        ) {
            Box(modifier = Modifier.padding(20.dp)) {
                content()
            }
        }
    }
}

@Composable
fun ThemeOption(label: String, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val backgroundColor by animateColorAsState(if (selected) PrimaryPurple else SurfaceTertiary)
    val contentColor by animateColorAsState(if (selected) Color.White else TextSecondary)
    
    Box(
        modifier = modifier
            .height(44.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(backgroundColor)
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            color = contentColor
        )
    }
}

@Composable
fun SettingsActionRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    iconColor: Color = PrimaryPurple,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(iconColor.copy(alpha = 0.1f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconColor,
                modifier = Modifier.size(20.dp)
            )
        }
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                color = TextPrimary,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelSmall,
                color = TextSecondary
            )
        }
        Icon(
            imageVector = Icons.Default.ChevronRight,
            contentDescription = null,
            tint = TextSecondary.copy(alpha = 0.3f),
            modifier = Modifier.size(20.dp)
        )
    }
}
