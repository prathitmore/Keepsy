package com.keepsy.app.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.keepsy.app.model.ActivityLog
import com.keepsy.app.model.ItemWithDetails
import com.keepsy.app.model.Space
import com.keepsy.app.ui.theme.*
import com.keepsy.app.utils.getSmartItemIconVector
import com.keepsy.app.utils.getSpaceIconVector
import com.keepsy.app.utils.parseCategoryColor
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun SpaceHorizontalCard(space: Space, onClick: () -> Unit) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(if (isPressed) 0.96f else 1f, label = "scale")

    Card(
        onClick = onClick,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(24.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.1f)),
        interactionSource = interactionSource,
        modifier = Modifier
            .width(200.dp)
            .height(180.dp)
            .graphicsLayer(scaleX = scale, scaleY = scale)
            .shadow(12.dp, RoundedCornerShape(24.dp))
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(22.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Box(
                modifier = Modifier.size(52.dp).clip(RoundedCornerShape(16.dp))
                    .background(Brush.linearGradient(listOf(MaterialTheme.colorScheme.primary.copy(0.2f), MaterialTheme.colorScheme.primary.copy(0.05f)))),
                contentAlignment = Alignment.Center
            ) {
                Icon(imageVector = getSpaceIconVector(space.icon), contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(28.dp))
            }
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(text = space.name, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(text = if (space.parentSpaceId != null) "Sub-space" else "Primary location", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.secondary)
            }
        }
    }
}

@Composable
fun ItemRowCard(itemDetails: ItemWithDetails, onClick: () -> Unit) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(if (isPressed) 0.98f else 1f, label = "scale")

    Card(
        onClick = onClick,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f)),
        shape = RoundedCornerShape(24.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.05f)),
        interactionSource = interactionSource,
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp).graphicsLayer(scaleX = scale, scaleY = scale)
    ) {
        Row(modifier = Modifier.fillMaxWidth().padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
            val photoModel = remember(itemDetails.item.photoPath, itemDetails.item.photoUrl) {
                if (itemDetails.item.photoPath != null && File(itemDetails.item.photoPath).exists()) File(itemDetails.item.photoPath) else itemDetails.item.photoUrl
            }
            if (photoModel != null) {
                AsyncImage(model = photoModel, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.size(72.dp).clip(RoundedCornerShape(18.dp)).border(1.dp, MaterialTheme.colorScheme.outline.copy(0.1f), RoundedCornerShape(18.dp)))
            } else {
                Box(modifier = Modifier.size(72.dp).clip(RoundedCornerShape(18.dp)).background(parseCategoryColor(itemDetails.category?.color).copy(0.1f)), contentAlignment = Alignment.Center) {
                    Icon(imageVector = getSmartItemIconVector(itemDetails.item.name, itemDetails.category?.icon), contentDescription = null, tint = parseCategoryColor(itemDetails.category?.color), modifier = Modifier.size(32.dp))
                }
            }
            Spacer(modifier = Modifier.width(20.dp))
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(text = itemDetails.item.name, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.onSurface, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Surface(color = MaterialTheme.colorScheme.primary.copy(0.1f), shape = RoundedCornerShape(8.dp)) {
                        Text(text = itemDetails.space?.name ?: "No location", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp))
                    }
                    Text(text = "in ${itemDetails.category?.name ?: "Other"}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.secondary)
                }
            }
            if (itemDetails.item.isFavorite) { Icon(imageVector = Icons.Default.AutoAwesome, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp)) }
        }
    }
}

@Composable
fun SpaceTreeNode(space: Space, allSpaces: List<Space>, depth: Int, expandedIds: List<Long>, onNodeClick: (Long) -> Unit, onNodeDetails: (Long) -> Unit) {
    val children = remember(allSpaces, space.spaceId) { allSpaces.filter { it.parentSpaceId == space.spaceId } }
    val isExp = expandedIds.contains(space.spaceId)
    Column(modifier = Modifier.fillMaxWidth().padding(start = (depth * 16).dp)) {
        Card(
            onClick = { onNodeDetails(space.spaceId) },
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(0.7f)),
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp)
        ) {
            Row(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                if (children.isNotEmpty()) {
                    IconButton(onClick = { onNodeClick(space.spaceId) }, modifier = Modifier.size(36.dp)) {
                        Icon(imageVector = if (isExp) Icons.Default.KeyboardArrowDown else Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, modifier = Modifier.size(24.dp))
                    }
                } else { Spacer(modifier = Modifier.size(36.dp)) }
                Box(modifier = Modifier.size(44.dp).clip(RoundedCornerShape(12.dp)).background(MaterialTheme.colorScheme.primary.copy(0.15f)), contentAlignment = Alignment.Center) {
                    Icon(imageVector = getSpaceIconVector(space.icon), contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = space.name, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    if (space.description != "" && space.description != null) { Text(text = space.description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.secondary.copy(0.7f), maxLines = 1) }
                }
                if (children.isNotEmpty()) {
                    Surface(color = MaterialTheme.colorScheme.surfaceVariant, shape = CircleShape, modifier = Modifier.size(28.dp)) {
                        Box(contentAlignment = Alignment.Center) { Text(text = "${children.size}", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.ExtraBold) }
                    }
                }
            }
        }
        AnimatedVisibility(visible = isExp, enter = expandVertically() + fadeIn(), exit = shrinkVertically() + fadeOut()) {
            Column { children.forEach { sub -> SpaceTreeNode(sub, allSpaces, depth + 1, expandedIds, onNodeClick, onNodeDetails) } }
        }
    }
}

@Composable
fun PremiumItemCard(itemDetails: ItemWithDetails, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val isPressed by remember { mutableStateOf(false) } // Placeholder for scale
    Card(
        onClick = onClick,
        modifier = modifier.height(180.dp).graphicsLayer(scaleX = if (isPressed) 0.95f else 1f, scaleY = if (isPressed) 0.95f else 1f),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, Color.White.copy(0.05f))
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            val photoModel = remember(itemDetails.item.photoPath, itemDetails.item.photoUrl) {
                if (itemDetails.item.photoPath != null && File(itemDetails.item.photoPath).exists()) File(itemDetails.item.photoPath) else itemDetails.item.photoUrl
            }
            if (photoModel != null) {
                AsyncImage(model = photoModel, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                Box(modifier = Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(0.8f)))))
            } else {
                Box(modifier = Modifier.fillMaxSize().background(Brush.linearGradient(listOf(MaterialTheme.colorScheme.primary.copy(0.1f), MaterialTheme.colorScheme.surface))), contentAlignment = Alignment.Center) {
                    Icon(imageVector = getSmartItemIconVector(itemDetails.item.name, itemDetails.category?.icon), contentDescription = null, tint = parseCategoryColor(itemDetails.category?.color).copy(0.5f), modifier = Modifier.size(56.dp))
                }
            }
            Column(modifier = Modifier.align(Alignment.BottomStart).padding(20.dp)) {
                Text(text = itemDetails.item.name, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold, color = Color.White, maxLines = 1)
                Text(text = itemDetails.space?.name ?: "No location", style = MaterialTheme.typography.bodySmall, color = Color.White.copy(0.8f))
            }
        }
    }
}

@Composable
fun PremiumSpaceCard(space: Space, onClick: () -> Unit) { SpaceHorizontalCard(space = space, onClick = onClick) }
@Composable
fun ActivityLogCard(log: ActivityLog) { TimelineCard(log = log, onClickItem = {}) }

@Composable
fun TimelineCard(log: ActivityLog, onClickItem: () -> Unit) {
    val date = remember(log.timestamp) { SimpleDateFormat("MMM dd • hh:mm a", Locale.getDefault()).format(Date(log.timestamp)) }
    val color = when (log.actionType) { "CREATED" -> SuccessGreen; "MOVED" -> WarningAmber; "DELETED" -> ErrorRed; "RESTORED" -> PrimaryAccent; else -> TextSecondary }
    val icon = when (log.actionType) { "CREATED" -> Icons.Default.Add; "MOVED" -> Icons.Default.MoveToInbox; "DELETED" -> Icons.Default.DeleteOutline; "RESTORED" -> Icons.Default.Restore; else -> Icons.Default.History }
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp).clickable(enabled = log.itemId != 0L, onClick = onClickItem)) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(top = 4.dp, end = 20.dp)) {
            Box(modifier = Modifier.size(36.dp).clip(CircleShape).background(color.copy(0.15f)), contentAlignment = Alignment.Center) {
                Icon(imageVector = icon, contentDescription = null, tint = color, modifier = Modifier.size(20.dp))
            }
            Box(modifier = Modifier.width(2.dp).height(50.dp).background(Brush.verticalGradient(listOf(color.copy(0.4f), Color.Transparent))))
        }
        Column(modifier = Modifier.weight(1f).padding(bottom = 24.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(text = log.actionType, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Black, color = color, letterSpacing = 1.2.sp)
                Text(text = date, style = MaterialTheme.typography.labelSmall, color = TextSecondary.copy(0.7f))
            }
            Text(text = log.details, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
        }
    }
}
