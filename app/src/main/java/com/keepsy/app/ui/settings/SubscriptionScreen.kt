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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.keepsy.app.ui.components.*
import com.keepsy.app.ui.theme.*
import com.keepsy.app.viewmodel.KeepsyViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SubscriptionScreen(
    viewModel: KeepsyViewModel,
    onPop: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Keepsy Premium", fontWeight = FontWeight.Bold) },
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
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(28.dp),
                colors = CardDefaults.cardColors(containerColor = SurfaceSecondary),
                border = androidx.compose.foundation.BorderStroke(1.dp, PrimaryAccent.copy(alpha = 0.2f))
            ) {
                Column(modifier = Modifier.padding(24.dp)) {
                    Text("Current Plan", style = MaterialTheme.typography.labelMedium, color = TextSecondary)
                    Text("Free Edition", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.ExtraBold, color = TextPrimary)
                    Spacer(modifier = Modifier.height(16.dp))
                    HorizontalDivider(color = Color.White.copy(alpha = 0.05f))
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Upgrade for unlimited cloud storage and advanced security features.", style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                }
            }

            Text("Select a Plan", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = TextPrimary)

            PlanCard("Monthly", "$4.99/mo", "Best for starters")
            PlanCard("Yearly", "$49.99/yr", "Most popular choice", isPopular = true)
            PlanCard("Lifetime", "$199.99", "Pay once, keep forever")
        }
    }
}

@Composable
fun PlanCard(title: String, price: String, subtitle: String, isPopular: Boolean = false) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = if (isPopular) SurfaceTertiary else SurfaceSecondary),
        border = androidx.compose.foundation.BorderStroke(1.dp, if (isPopular) PrimaryAccent else Color.White.copy(alpha = 0.03f))
    ) {
        Row(modifier = Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
            Column {
                Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = TextPrimary)
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
            }
            Text(price, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold, color = PrimaryAccent)
        }
    }
}
