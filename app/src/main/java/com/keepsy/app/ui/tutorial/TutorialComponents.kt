package com.keepsy.app.ui.tutorial

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.border
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.toSize
import com.keepsy.app.ui.components.PrimaryGradientButton
import com.keepsy.app.ui.theme.*

@Composable
fun TutorialOverlay(
    viewModel: TutorialViewModel,
    modifier: Modifier = Modifier
) {
    val isVisible by viewModel.isVisible.collectAsState()
    val currentStep by viewModel.currentStep.collectAsState()
    val spotlights by viewModel.spotlights.collectAsState()
    
    if (!isVisible) return

    val currentRect = currentStep.spotlightKey?.let { spotlights[it] }

    Box(
        modifier = modifier
            .fillMaxSize()
            .graphicsLayer(alpha = 0.99f) // Required for BlendMode.Clear
    ) {
        // Dimmed Background with Hole
        Canvas(modifier = Modifier
            .fillMaxSize()
            .clickable(enabled = false) {}
        ) {
            drawRect(color = Color.Black.copy(alpha = 0.8f))
            
            if (currentRect != null) {
                drawRoundRect(
                    color = Color.Transparent,
                    topLeft = currentRect.topLeft,
                    size = currentRect.size,
                    cornerRadius = CornerRadius(16.dp.toPx(), 16.dp.toPx()),
                    blendMode = BlendMode.Clear
                )
            }
        }
        
        // Spotlight Border Glow
        if (currentRect != null) {
            Box(
                modifier = Modifier
                    .offset(
                        x = with(LocalDensity.current) { currentRect.left.toDp() },
                        y = with(LocalDensity.current) { currentRect.top.toDp() }
                    )
                    .size(
                        width = with(LocalDensity.current) { currentRect.width.toDp() },
                        height = with(LocalDensity.current) { currentRect.height.toDp() }
                    )
                    .border(2.dp, PrimaryAccent, RoundedCornerShape(16.dp))
                    .graphicsLayer {
                        shadowElevation = 20.dp.toPx()
                        shape = RoundedCornerShape(16.dp)
                        clip = false
                    }
            )
        }

        // Help Bubble
        TutorialBubble(
            step = currentStep,
            onNext = { viewModel.nextStep() },
            onSkip = { viewModel.skipTutorial() },
            modifier = Modifier
                .align(if (currentRect == null || currentRect.top > 400) Alignment.TopCenter else Alignment.BottomCenter)
                .padding(24.dp)
                .padding(top = 40.dp, bottom = 100.dp)
        )
    }
}

@Composable
fun TutorialBubble(
    step: TutorialStep,
    onNext: () -> Unit,
    onSkip: () -> Unit,
    modifier: Modifier = Modifier
) {
    ElevatedCard(
        modifier = modifier
            .fillMaxWidth()
            .wrapContentHeight(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = SurfaceSecondary),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 12.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            val stepIcon = when (step) {
                TutorialStep.WELCOME -> Icons.Default.WavingHand
                TutorialStep.SPACES_EXPLAIN, TutorialStep.CREATE_SPACE -> Icons.Default.Layers
                TutorialStep.SUBSPACE_EXPLAIN -> Icons.Default.AccountTree
                TutorialStep.ITEMS_EXPLAIN, TutorialStep.CREATE_ITEM -> Icons.Default.Inventory2
                TutorialStep.SEARCH_EXPLAIN -> Icons.Default.Search
                TutorialStep.ITEM_DETAILS -> Icons.Default.Info
                TutorialStep.MOVING_ITEMS -> Icons.Default.MoveUp
                TutorialStep.ACTIVITY_EXPLAIN -> Icons.Default.History
                TutorialStep.DASHBOARD_EXPLAIN -> Icons.Default.Dashboard
                TutorialStep.COMPLETION -> Icons.Default.CheckCircle
            }

            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(PrimaryPurple.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = stepIcon,
                    contentDescription = null,
                    tint = PrimaryPurple,
                    modifier = Modifier.size(28.dp)
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Text(
                text = step.title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.ExtraBold,
                color = TextPrimary,
                textAlign = TextAlign.Center
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = step.description,
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary,
                textAlign = TextAlign.Center,
                lineHeight = 22.sp
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = onSkip) {
                    Text("Skip", color = TextSecondary)
                }
                
                PrimaryGradientButton(
                    text = if (step == TutorialStep.COMPLETION) "Start Using Keepsy" else "Next",
                    onClick = onNext,
                    modifier = Modifier.width(if (step == TutorialStep.COMPLETION) 200.dp else 100.dp)
                )
            }
        }
    }
}

fun Modifier.tutorialSpotlight(
    key: String,
    viewModel: TutorialViewModel
): Modifier = this.onGloballyPositioned { layoutCoordinates ->
    val rect = Rect(
        offset = layoutCoordinates.positionInRoot(),
        size = layoutCoordinates.size.toSize()
    )
    viewModel.updateSpotlight(key, rect)
}
