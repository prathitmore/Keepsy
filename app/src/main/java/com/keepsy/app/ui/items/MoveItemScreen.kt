package com.keepsy.app.ui.items

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.keepsy.app.navigation.SubScreen
import com.keepsy.app.ui.theme.HighlightTeal
import com.keepsy.app.utils.getSpaceIconVector
import com.keepsy.app.viewmodel.KeepsyViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MoveItemScreen(
    itemId: Long,
    viewModel: KeepsyViewModel,
    onPop: () -> Unit
) {
    val context = LocalContext.current
    val spacesList by viewModel.spaces.collectAsStateWithLifecycle(emptyList())
    val itemDetails by viewModel.selectedItem.collectAsStateWithLifecycle()

    var selectedDestSpaceId by remember { mutableStateOf(0L) }
    var moveReason by remember { mutableStateOf("") }

    LaunchedEffect(itemId) {
        viewModel.selectItem(itemId)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Relocate tracked item") },
                navigationIcon = {
                    IconButton(onClick = onPop) {
                        Icon(imageVector = Icons.AutoMirrored.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { innerPadding ->
        itemDetails?.let { details ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .background(MaterialTheme.colorScheme.background)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                    Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Default.AllInbox, contentDescription = "", tint = HighlightTeal, modifier = Modifier.size(32.dp))
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(text = "Currently relocating", fontSize = 11.sp, color = Color.Gray)
                            Text(text = details.item.name, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                            Text(text = "Currently stored inside: ${details.space?.name ?: "Unknown"}", fontSize = 12.sp, color = HighlightTeal)
                        }
                    }
                }

                Text(text = "Select New Container Space Destination *", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    val alternateSpaces = spacesList.filter { it.spaceId != details.item.spaceId }
                    items(alternateSpaces) { space ->
                        val isSelected = selectedDestSpaceId == space.spaceId
                        Card(
                            onClick = { selectedDestSpaceId = space.spaceId },
                            colors = CardDefaults.cardColors(
                                containerColor = if (isSelected) HighlightTeal else MaterialTheme.colorScheme.surface
                            ),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = getSpaceIconVector(space.icon),
                                    contentDescription = null,
                                    tint = if (isSelected) Color.White else HighlightTeal
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = space.name,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }

                OutlinedTextField(
                    value = moveReason,
                    onValueChange = { moveReason = it },
                    label = { Text("Reason for relocation (optional)") },
                    placeholder = { Text("e.g. Taking on trip, safe storage cleanup") },
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = HighlightTeal, focusedTextColor = MaterialTheme.colorScheme.onBackground, unfocusedTextColor = MaterialTheme.colorScheme.onBackground),
                    modifier = Modifier.fillMaxWidth()
                )

                Button(
                    onClick = {
                        if (selectedDestSpaceId == 0L) {
                            Toast.makeText(context, "Please select destination space container first", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        viewModel.moveItem(itemId, selectedDestSpaceId, moveReason) {
                            Toast.makeText(context, "Item Moved", Toast.LENGTH_SHORT).show()
                            onPop()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = HighlightTeal),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("confirm_relocation_item_btn")
                ) {
                    Text("Relocate Belonging Now")
                }
            }
        }
    }
}
