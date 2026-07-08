package com.keepsy.app.ui.settings

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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.keepsy.app.ui.components.*
import com.keepsy.app.ui.theme.*
import com.keepsy.app.viewmodel.KeepsyViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BackupSyncScreen(
    viewModel: KeepsyViewModel,
    onPop: () -> Unit
) {
    val syncState by viewModel.syncState.collectAsStateWithLifecycle()
    val profile by viewModel.userProfile.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Backup & Sync", fontWeight = FontWeight.Bold) },
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
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // Cloud Status Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = SurfaceSecondary),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.05f))
            ) {
                Row(
                    modifier = Modifier.padding(20.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(SuccessGreen.copy(alpha = 0.1f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.CloudDone, contentDescription = null, tint = SuccessGreen)
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text("Cloud Sync Active", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = TextPrimary)
                        Text("Last synced: Today, 10:45 PM", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                    }
                }
            }

            SectionHeader(title = "Synchronization Settings")

            SyncSettingRow(
                title = "Auto Sync",
                subtitle = "Keep data updated automatically",
                checked = true,
                onCheckedChange = {}
            )

            SyncSettingRow(
                title = "Sync over Wi-Fi only",
                subtitle = "Save mobile data",
                checked = false,
                onCheckedChange = {}
            )

            Spacer(modifier = Modifier.height(16.dp))

            PrimaryGradientButton(
                text = "Backup Now",
                onClick = { viewModel.manualSync() },
                trailingIcon = Icons.Default.Backup
            )

            SecondaryButton(
                text = "Restore from Cloud",
                onClick = { /* Restore logic */ }
            )
        }
    }
}

@Composable
fun SyncSettingRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge, color = TextPrimary, fontWeight = FontWeight.Bold)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = PrimaryAccent,
                checkedTrackColor = PrimaryAccent.copy(alpha = 0.5f)
            )
        )
    }
}
