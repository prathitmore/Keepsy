package com.keepsy.app.ui.tutorial

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
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

    val density = LocalDensity.current
    val config = LocalConfiguration.current
    val screenHeightPx = with(density) { config.screenHeightDp.dp.toPx() }

    var overlayOffset by remember { mutableStateOf(Offset.Zero) }

    val rawRect = currentStep.spotlightKey?.let { spotlights[it] }
    
    // Translate the rect to be relative to the overlay's origin
    val relativeRect = remember(rawRect, overlayOffset) {
        rawRect?.translate(-overlayOffset)
    }

    // Add 8dp padding around the spotlight for "breathing room"
    val inflatedRect = remember(relativeRect) {
        relativeRect?.let {
            val padding = with(density) { 8.dp.toPx() }
            Rect(
                left = it.left - padding,
                top = it.top - padding,
                right = it.right + padding,
                bottom = it.bottom + padding
            )
        }
    }

    // Animate the spotlight transition
    val animatedRect by animateRectAsState(
        targetValue = inflatedRect ?: Rect(0f, 0f, 0f, 0f),
        animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessLow),
        label = "spotlight_rect"
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .onGloballyPositioned { layoutCoordinates ->
                overlayOffset = layoutCoordinates.positionInRoot()
            }
            .graphicsLayer(alpha = 0.99f) // Required for BlendMode.Clear to work on Canvas
    ) {
        // Dimmed Background with Hole
        Canvas(modifier = Modifier
            .fillMaxSize()
            .clickable(enabled = false) {}
        ) {
            drawRect(color = Color.Black.copy(alpha = 0.65f)) // Lighter dim for visibility
            
            if (inflatedRect != null) {
                drawRoundRect(
                    color = Color.Transparent,
                    topLeft = animatedRect.topLeft,
                    size = animatedRect.size,
                    cornerRadius = CornerRadius(16.dp.toPx(), 16.dp.toPx()),
                    blendMode = BlendMode.Clear
                )
            }
        }
        
        // Spotlight Border & Glow
        if (inflatedRect != null) {
            Box(
                modifier = Modifier
                    .offset(
                        x = with(density) { animatedRect.left.toDp() },
                        y = with(density) { animatedRect.top.toDp() }
                    )
                    .size(
                        width = with(density) { animatedRect.width.toDp() },
                        height = with(density) { animatedRect.height.toDp() }
                    )
                    .border(2.dp, PrimaryAccent.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
                    .graphicsLayer {
                        shadowElevation = 15.dp.toPx()
                        shape = RoundedCornerShape(16.dp)
                    }
            )
        }

        // Smarter Bubble Placement Logic
        val isTargetInTopHalf = inflatedRect?.let { it.center.y < screenHeightPx / 2 } ?: true
        val bubbleAlignment = if (isTargetInTopHalf) Alignment.BottomCenter else Alignment.TopCenter
        val bubblePadding = if (isTargetInTopHalf) PaddingValues(bottom = 120.dp) else PaddingValues(top = 80.dp)

        AnimatedContent(
            targetState = currentStep,
            transitionSpec = {
                (fadeIn() + slideInHorizontally { it / 2 }).togetherWith(fadeOut() + slideOutHorizontally { -it / 2 })
            },
            modifier = Modifier
                .align(bubbleAlignment)
                .padding(24.dp)
                .padding(bubblePadding),
            label = "bubble_transition"
        ) { step ->
            TutorialBubble(
                step = step,
                onNext = { viewModel.nextStep() },
                onSkip = { viewModel.skipTutorial() }
            )
        }
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
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = SurfaceSecondary),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 16.dp)
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
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
                    .size(56.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(PrimaryPurple.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = stepIcon,
                    contentDescription = null,
                    tint = PrimaryPurple,
                    modifier = Modifier.size(32.dp)
                )
            }
            
            Spacer(modifier = Modifier.height(20.dp))
            
            Text(
                text = step.title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.ExtraBold,
                color = TextPrimary,
                textAlign = TextAlign.Center
            )
            
            Spacer(modifier = Modifier.height(10.dp))
            
            Text(
                text = step.description,
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary,
                textAlign = TextAlign.Center,
                lineHeight = 22.sp,
                modifier = Modifier.padding(horizontal = 8.dp)
            )
            
            Spacer(modifier = Modifier.height(28.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = onSkip) {
                    Text("Skip Tour", color = TextSecondary, fontWeight = FontWeight.Medium)
                }
                
                PrimaryGradientButton(
                    text = if (step == TutorialStep.COMPLETION) "Start Organizing" else "Next Step",
                    onClick = onNext,
                    modifier = Modifier.width(if (step == TutorialStep.COMPLETION) 180.dp else 120.dp)
                )
            }
        }
    }
}

fun Modifier.tutorialSpotlight(
    key: String,
    viewModel: TutorialViewModel
): Modifier = this.onGloballyPositioned { layoutCoordinates ->
    // Use positionInRoot to get coordinates relative to the Compose root
    val rect = Rect(
        offset = layoutCoordinates.positionInRoot(),
        size = layoutCoordinates.size.toSize()
    )
    viewModel.updateSpotlight(key, rect)
}

@Composable
fun animateRectAsState(
    targetValue: Rect,
    animationSpec: AnimationSpec<Rect> = spring(),
    label: String = "RectAnimation"
): State<Rect> {
    return animateValueAsState(
        targetValue = targetValue,
        typeConverter = Rect.VectorConverter,
        animationSpec = animationSpec,
        label = label
    )
}

private val Rect.Companion.VectorConverter: TwoWayConverter<Rect, AnimationVector4D>
    get() = TwoWayConverter(
        convertToVector = { AnimationVector4D(it.left, it.top, it.right, it.bottom) },
        convertFromVector = { Rect(it.v1, it.v2, it.v3, it.v4) }
    )
