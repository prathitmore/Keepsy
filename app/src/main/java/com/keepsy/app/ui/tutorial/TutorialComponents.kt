package com.keepsy.app.ui.tutorial

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.toSize
import com.keepsy.app.ui.components.PrimaryGradientButton
import com.keepsy.app.ui.theme.*
import com.keepsy.app.utils.KeepsyLogger

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

    // Track the overlay's own position to calculate relative offsets
    var overlayOffset by remember { mutableStateOf(Offset.Zero) }

    val rawRect = currentStep.spotlightKey?.let { spotlights[it] }
    
    // Calculate the rect relative to THIS overlay Box
    val relativeRect = remember(rawRect, overlayOffset) {
        rawRect?.translate(-overlayOffset)
    }

    // Add padding around the spotlight
    val inflatedRect = remember(relativeRect) {
        relativeRect?.let {
            if (it.width <= 0 || it.height <= 0) return@let null
            val padding = with(density) { 10.dp.toPx() }
            Rect(
                left = it.left - padding,
                top = it.top - padding,
                right = it.right + padding,
                bottom = it.bottom + padding
            )
        }
    }

    // Animate the spotlight transition smoothly
    val animatedRect by animateRectAsState(
        targetValue = inflatedRect ?: Rect(0f, 0f, 1f, 1f),
        animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessLow),
        label = "spotlight_rect"
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .onGloballyPositioned { overlayOffset = it.positionInWindow() }
            .graphicsLayer(alpha = 0.99f) // Required for BlendMode.Clear
            .pointerInput(animatedRect) {
                // Intercept touches: allow if inside spotlight, block otherwise
                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent(PointerEventPass.Initial)
                        val position = event.changes.first().position
                        if (animatedRect.contains(position)) {
                            // User is interacting with the real UI element
                        } else {
                            // Block interaction with the rest of the UI
                            event.changes.forEach { it.consume() }
                        }
                    }
                }
            }
    ) {
        // 1. Dimmed Background with Hole
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawRect(color = Color.Black.copy(alpha = 0.7f))
            
            if (inflatedRect != null && !animatedRect.isEmpty) {
                try {
                    val key = currentStep.spotlightKey ?: ""
                    val isCircular = key.contains("fab") || key.contains("tab")
                    
                    if (isCircular) {
                        val radius = (maxOf(animatedRect.width, animatedRect.height) / 2)
                        drawCircle(
                            color = Color.Transparent,
                            center = animatedRect.center,
                            radius = radius,
                            blendMode = BlendMode.Clear
                        )
                    } else {
                        drawRoundRect(
                            color = Color.Transparent,
                            topLeft = animatedRect.topLeft,
                            size = animatedRect.size,
                            cornerRadius = CornerRadius(16.dp.toPx(), 16.dp.toPx()),
                            blendMode = BlendMode.Clear
                        )
                    }
                } catch (e: Exception) {
                    KeepsyLogger.w("Canvas draw issue: ${e.message}")
                }
            }
        }
        
        // 2. Animated Border & Arrow
        if (inflatedRect != null && !animatedRect.isEmpty) {
            val key = currentStep.spotlightKey ?: ""
            val isCircular = key.contains("fab") || key.contains("tab")
            
            val infiniteTransition = rememberInfiniteTransition(label = "bounce")
            val arrowBounce by infiniteTransition.animateFloat(
                initialValue = 0f,
                targetValue = 15f,
                animationSpec = infiniteRepeatable(
                    animation = tween(600, easing = EaseInOutQuad),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "bounce"
            )

            // Border
            Box(
                modifier = Modifier
                    .offset(
                        x = with(density) { (if (isCircular) animatedRect.center.x - maxOf(animatedRect.width, animatedRect.height)/2 else animatedRect.left).toDp() },
                        y = with(density) { (if (isCircular) animatedRect.center.y - maxOf(animatedRect.width, animatedRect.height)/2 else animatedRect.top).toDp() }
                    )
                    .size(
                        width = with(density) { (if (isCircular) maxOf(animatedRect.width, animatedRect.height) else animatedRect.width).toDp() },
                        height = with(density) { (if (isCircular) maxOf(animatedRect.width, animatedRect.height) else animatedRect.height).toDp() }
                    )
                    .border(2.dp, PrimaryAccent, if (isCircular) CircleShape else RoundedCornerShape(16.dp))
            )

            // Arrow
            val isTargetInBottomHalf = animatedRect.center.y > screenHeightPx / 2
            Icon(
                imageVector = if (isTargetInBottomHalf) Icons.Default.ArrowDownward else Icons.Default.ArrowUpward,
                contentDescription = null,
                tint = PrimaryAccent,
                modifier = Modifier
                    .offset(
                        x = with(density) { (animatedRect.center.x - 12.dp.toPx()).toDp() },
                        y = with(density) { 
                            if (isTargetInBottomHalf) 
                                (animatedRect.top - 45f - arrowBounce).toDp()
                            else 
                                (animatedRect.bottom + 10f + arrowBounce).toDp()
                        }
                    )
                    .size(24.dp)
            )
        }

        // 3. Tutorial Bubble
        val isTargetInTopHalf = inflatedRect?.let { it.center.y < screenHeightPx / 2 } ?: true
        val bubbleAlignment = if (isTargetInTopHalf) Alignment.BottomCenter else Alignment.TopCenter
        val bubblePadding = if (isTargetInTopHalf) PaddingValues(bottom = 140.dp) else PaddingValues(top = 100.dp)

        AnimatedContent(
            targetState = currentStep,
            transitionSpec = {
                (fadeIn() + slideInHorizontally { it / 2 }).togetherWith(fadeOut() + slideOutHorizontally { -it / 2 })
            },
            modifier = Modifier
                .align(bubbleAlignment)
                .padding(24.dp)
                .padding(bubblePadding),
            label = "bubble"
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
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 20.dp)
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            val stepIcon = when (step) {
                TutorialStep.WELCOME -> Icons.Default.WavingHand
                TutorialStep.INTERFACE_OVERVIEW -> Icons.Default.Dashboard
                TutorialStep.SPACE_INTRO -> Icons.Default.Layers
                TutorialStep.SUBSPACE_INTRO -> Icons.Default.AccountTree
                TutorialStep.ITEM_INTRO -> Icons.Default.Inventory2
                TutorialStep.RETRIEVAL_INTRO -> Icons.Default.Search
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
                lineHeight = 22.sp
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
                    text = if (step == TutorialStep.COMPLETION) "Finish" else "Next Step",
                    onClick = onNext,
                    modifier = Modifier.width(if (step == TutorialStep.COMPLETION) 160.dp else 120.dp)
                )
            }
        }
    }
}

fun Modifier.tutorialSpotlight(
    key: String,
    viewModel: TutorialViewModel
): Modifier = this.onGloballyPositioned { layoutCoordinates ->
    try {
        if (layoutCoordinates.isAttached) {
            val rect = Rect(
                offset = layoutCoordinates.positionInWindow(),
                size = layoutCoordinates.size.toSize()
            )
            if (!rect.left.isNaN() && !rect.top.isNaN()) {
                viewModel.updateSpotlight(key, rect)
            }
        }
    } catch (e: Exception) {
        // Safe fail
    }
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
