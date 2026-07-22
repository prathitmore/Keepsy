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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.keepsy.app.model.ActivityLog
import com.keepsy.app.model.ItemWithDetails
import com.keepsy.app.model.Space
import com.keepsy.app.ui.theme.*
import com.keepsy.app.utils.getSmartItemIconVector
import com.keepsy.app.utils.getSpaceIconVector
import com.keepsy.app.utils.parseCategoryColor
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
            .width(210.dp)
            .height(180.dp)
            .graphicsLayer(scaleX = scale, scaleY = scale)
            .shadow(12.dp, RoundedCornerShape(24.dp))
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            if (space.photoUrl != null && space.photoUrl != "") {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current).data(space.photoUrl).crossfade(true).build(),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
                Box(modifier = Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(0.6f)))))
            }

            Column(
                modifier = Modifier.fillMaxSize().padding(20.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                if (space.photoUrl == null || space.photoUrl == "") {
                    Box(
                        modifier = Modifier.size(52.dp).clip(RoundedCornerShape(14.dp))
                            .background(MaterialTheme.colorScheme.primary.copy(0.1f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(imageVector = getSpaceIconVector(space.icon), contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(28.dp))
                    }
                } else { Spacer(modifier = Modifier.height(1.dp)) }
                
                Column {
                    Text(text = space.name, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold, maxLines = 1, overflow = TextOverflow.Ellipsis, color = if (space.photoUrl != null && space.photoUrl != "") Color.White else MaterialTheme.colorScheme.onSurface)
                    Text(text = if (space.parentSpaceId != null) "Sub-space" else "Location", style = MaterialTheme.typography.bodySmall, color = if (space.photoUrl != null && space.photoUrl != "") Color.White.copy(0.7f) else MaterialTheme.colorScheme.secondary)
                }
            }
        }
    }
}

@Composable
fun ItemRowCard(itemDetails: ItemWithDetails, onClick: () -> Unit) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(if (isPressed) 0.98f else 1f, label = "scale")

    Surface(
        onClick = onClick,
        color = MaterialTheme.colorScheme.surface.copy(0.4f),
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.05f)),
        interactionSource = interactionSource,
        modifier = Modifier.fillMaxWidth().graphicsLayer(scaleX = scale, scaleY = scale)
    ) {
        Row(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            if (itemDetails.item.photoUrl != null && itemDetails.item.photoUrl != "") {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current).data(itemDetails.item.photoUrl).crossfade(true).build(),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.size(60.dp).clip(RoundedCornerShape(14.dp))
                )
            } else {
                Box(modifier = Modifier.size(60.dp).clip(RoundedCornerShape(14.dp)).background(parseCategoryColor(itemDetails.category?.color).copy(0.06f)), contentAlignment = Alignment.Center) {
                    Icon(imageVector = getSmartItemIconVector(itemDetails.item.name, itemDetails.category?.icon), contentDescription = null, tint = parseCategoryColor(itemDetails.category?.color), modifier = Modifier.size(28.dp))
                }
            }
            Spacer(modifier = Modifier.width(20.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(text = itemDetails.item.name, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.onSurface, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(text = itemDetails.space?.name ?: "No location", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)
                    Text(text = "•", color = MaterialTheme.colorScheme.secondary.copy(0.4f), style = MaterialTheme.typography.bodySmall)
                    Text(text = itemDetails.category?.name ?: "Other", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.secondary)
                }
            }
            if (itemDetails.item.isFavorite) { Icon(imageVector = Icons.Default.AutoAwesome, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp)) }
        }
    }
}

@Composable
fun SpaceTreeNode(space: Space, allSpaces: List<Space>, depth: Int, expandedIds: List<Long>, onNodeClick: (Long) -> Unit, onNodeDetails: (Long) -> Unit) {
    val children = remember(allSpaces, space.spaceId) { allSpaces.filter { it.parentSpaceId == space.spaceId } }
    val isExp = expandedIds.contains(space.spaceId)
    Column(modifier = Modifier.fillMaxWidth().padding(start = (depth * 20).dp)) {
        Card(
            onClick = { onNodeDetails(space.spaceId) },
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(0.6f)),
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(0.05f)),
            modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp)
        ) {
            Row(modifier = Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                if (children.isNotEmpty()) {
                    IconButton(onClick = { onNodeClick(space.spaceId) }, modifier = Modifier.size(36.dp)) {
                        Icon(imageVector = if (isExp) Icons.Default.KeyboardArrowDown else Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, modifier = Modifier.size(26.dp))
                    }
                } else { Spacer(modifier = Modifier.size(36.dp)) }
                Box(modifier = Modifier.size(40.dp).clip(RoundedCornerShape(10.dp)).background(MaterialTheme.colorScheme.primary.copy(0.1f)), contentAlignment = Alignment.Center) {
                    Icon(imageVector = getSpaceIconVector(space.icon), contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(22.dp))
                }
                Spacer(modifier = Modifier.width(14.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = space.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                if (children.isNotEmpty()) {
                    Surface(color = MaterialTheme.colorScheme.primary.copy(0.1f), shape = CircleShape, modifier = Modifier.size(28.dp)) {
                        Box(contentAlignment = Alignment.Center) { Text(text = "${children.size}", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary) }
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
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(if (isPressed) 0.95f else 1f, label = "scale")

    Card(
        onClick = onClick,
        modifier = modifier.height(180.dp).graphicsLayer(scaleX = scale, scaleY = scale),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.05f))
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            if (itemDetails.item.photoUrl != null && itemDetails.item.photoUrl != "") {
                AsyncImage(model = ImageRequest.Builder(LocalContext.current).data(itemDetails.item.photoUrl).crossfade(true).build(), contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                Box(modifier = Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(0.7f)))))
            } else {
                Box(modifier = Modifier.fillMaxSize().background(Brush.linearGradient(listOf(MaterialTheme.colorScheme.primary.copy(0.05f), MaterialTheme.colorScheme.surface))), contentAlignment = Alignment.Center) {
                    Icon(imageVector = getSmartItemIconVector(itemDetails.item.name, itemDetails.category?.icon), contentDescription = null, tint = parseCategoryColor(itemDetails.category?.color).copy(0.3f), modifier = Modifier.size(52.dp))
                }
            }
            Column(modifier = Modifier.align(Alignment.BottomStart).padding(20.dp)) {
                Text(text = itemDetails.item.name, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold, color = Color.White, maxLines = 1)
                Text(text = itemDetails.space?.name ?: "No location", style = MaterialTheme.typography.labelSmall, color = Color.White.copy(0.7f))
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
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp).clickable(enabled = log.itemId != 0L, onClick = onClickItem)) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(top = 4.dp, end = 16.dp)) {
            Box(modifier = Modifier.size(36.dp).clip(CircleShape).background(color.copy(0.1f)), contentAlignment = Alignment.Center) {
                Icon(imageVector = icon, contentDescription = null, tint = color, modifier = Modifier.size(20.dp))
            }
            Box(modifier = Modifier.width(1.dp).height(44.dp).background(Brush.verticalGradient(listOf(color.copy(0.3f), Color.Transparent))))
        }
        Column(modifier = Modifier.weight(1f).padding(bottom = 24.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(text = log.actionType, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Black, color = color, letterSpacing = 1.2.sp)
                Text(text = date, style = MaterialTheme.typography.labelSmall, color = TextSecondary.copy(0.6f))
            }
            Text(text = log.details, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
        }
    }
}
