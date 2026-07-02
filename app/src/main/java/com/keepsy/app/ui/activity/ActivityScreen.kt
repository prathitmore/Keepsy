package com.keepsy.app.ui.activity

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.History
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.keepsy.app.navigation.SubScreen
import com.keepsy.app.ui.components.*
import com.keepsy.app.ui.theme.*
import com.keepsy.app.ui.tutorial.TutorialViewModel
import com.keepsy.app.ui.tutorial.tutorialSpotlight
import com.keepsy.app.viewmodel.KeepsyViewModel

@Composable
fun ActivityScreen(
    viewModel: KeepsyViewModel, 
    onNavigateToSub: (SubScreen) -> Unit,
    tutorialViewModel: TutorialViewModel? = null
) {
    val activityLogs by viewModel.activityLogs.collectAsStateWithLifecycle(emptyList())

    Column(
        modifier = Modifier
            .fillMaxSize()
            .then(if (tutorialViewModel != null) Modifier.tutorialSpotlight("activity_screen", tutorialViewModel) else Modifier)
            .padding(horizontal = 24.dp)
    ) {
        if (activityLogs.isEmpty()) {
            EmptyState(
                icon = {
                    Icon(
                        imageVector = Icons.Default.History,
                        contentDescription = null,
                        tint = PrimaryPurple,
                        modifier = Modifier.size(32.dp)
                    )
                },
                title = "No history recorded",
                description = "Your actions will appear here as a chronological memory trail of every item and space.",
                modifier = Modifier.weight(1f)
            )
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(top = 8.dp, bottom = 100.dp)
            ) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(
                            onClick = { onNavigateToSub(SubScreen.TrashBin) },
                            colors = ButtonDefaults.textButtonColors(contentColor = ErrorRed)
                        ) {
                            Icon(imageVector = Icons.Default.DeleteSweep, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Open Trash Bin", style = MaterialTheme.typography.labelLarge)
                        }
                    }
                }
                
                items(activityLogs, key = { "log_${it.activityId}" }) { log ->
                    TimelineCard(
                        log = log, 
                        onClickItem = {
                            if (log.itemId != 0L) {
                                onNavigateToSub(SubScreen.ItemDetails(log.itemId))
                            }
                        }
                    )
                }
            }
        }
    }
}
