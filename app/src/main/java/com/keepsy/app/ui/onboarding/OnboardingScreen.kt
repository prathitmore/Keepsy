package com.keepsy.app.ui.onboarding

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.keepsy.app.ui.components.*
import com.keepsy.app.ui.theme.*
import com.keepsy.app.viewmodel.KeepsyViewModel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

@Composable
fun OnboardingScreenView(viewModel: KeepsyViewModel, onFinished: () -> Unit) {
    var onboardingStep by remember { mutableIntStateOf(1) }
    val context = LocalContext.current

    var firstSpaceName by remember { mutableStateOf("Home") }
    var firstItemName by remember { mutableStateOf("") }

    val totalSteps = 5

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Background)
    ) {
        KeepsyBackgroundEffects()
        
        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Progress Indicator (Dots)
            Row(
                modifier = Modifier.padding(top = 16.dp, bottom = 32.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                repeat(totalSteps) { index ->
                    val isActive = index + 1 == onboardingStep
                    val width by animateDpAsState(
                        targetValue = if (isActive) 24.dp else 8.dp,
                        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy)
                    )
                    val color by animateColorAsState(
                        targetValue = if (isActive) PrimaryAccent else Color.DarkGray.copy(alpha = 0.5f)
                    )
                    
                    Box(
                        modifier = Modifier
                            .size(height = 8.dp, width = width)
                            .clip(CircleShape)
                            .background(color)
                    )
                }
            }

            AnimatedContent(
                targetState = onboardingStep,
                transitionSpec = {
                    if (targetState > initialState) {
                        slideInHorizontally { it } + fadeIn() togetherWith
                        slideOutHorizontally { -it } + fadeOut()
                    } else {
                        slideInHorizontally { -it } + fadeIn() togetherWith
                        slideOutHorizontally { it } + fadeOut()
                    }
                },
                label = "OnboardingStep",
                modifier = Modifier.weight(1f)
            ) { step ->
                when (step) {
                    1 -> OnboardingContent(
                        icon = Icons.Default.Security,
                        title = "Keep Everything Safe",
                        description = "Store your important things in one secure place. Your data is encrypted and only yours."
                    )
                    2 -> OnboardingContent(
                        icon = Icons.Default.Layers,
                        title = "Organized Automatically",
                        description = "Spaces, categories, and tags help you find anything instantly. Say goodbye to the search."
                    )
                    3 -> OnboardingContent(
                        icon = Icons.Default.CloudSync,
                        title = "Sync Across Devices",
                        description = "Your memories stay updated wherever you go. Seamless access across all your hardware."
                    )
                    4 -> OnboardingContent(
                        icon = Icons.Default.AutoAwesome,
                        title = "Beautiful & Modern",
                        description = "A premium experience designed for everyday use. Simple, fast, and delightful."
                    )
                    5 -> {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center,
                            modifier = Modifier
                                .fillMaxSize()
                                .verticalScroll(rememberScrollState())
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(120.dp)
                                    .clip(CircleShape)
                                    .background(PrimaryAccent.copy(alpha = 0.05f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CardGiftcard,
                                    contentDescription = null,
                                    tint = PrimaryAccent,
                                    modifier = Modifier.size(60.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(32.dp))
                            Text(
                                text = "Let's Get You Started!",
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.ExtraBold,
                                color = TextPrimary,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "Create your first storage space to activate your memory tracker.",
                                style = MaterialTheme.typography.bodyLarge,
                                color = TextSecondary,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(40.dp))
                            
                            KeepsyTextField(
                                value = firstSpaceName,
                                onValueChange = { firstSpaceName = it },
                                label = "First Storage Space",
                                placeholder = "e.g. Home, Studio, Garage",
                                leadingIcon = Icons.Default.Home
                            )
                            
                            Spacer(modifier = Modifier.height(16.dp))
                            
                            KeepsyTextField(
                                value = firstItemName,
                                onValueChange = { firstItemName = it },
                                label = "First Item Tracked (Optional)",
                                placeholder = "e.g. Passport, Camera, Spare Keys",
                                leadingIcon = Icons.Default.Inventory2
                            )
                        }
                    }
                }
            }

            // Navigation Buttons
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (onboardingStep > 1) {
                    TextButton(onClick = { onboardingStep-- }) {
                        Text("Back", color = TextSecondary)
                    }
                } else {
                    TextButton(onClick = { onFinished() }) {
                        Text("Skip", color = TextSecondary)
                    }
                }

                PrimaryGradientButton(
                    text = if (onboardingStep == totalSteps) "Create My First Space" else "Next",
                    onClick = {
                        if (onboardingStep < totalSteps) {
                            onboardingStep++
                        } else {
                            if (firstSpaceName.trim().isEmpty()) {
                                Toast.makeText(context, "Please name your first space", Toast.LENGTH_SHORT).show()
                                return@PrimaryGradientButton
                            }
                            
                            viewModel.completeOnboarding(firstSpaceName, firstItemName)
                            // Navigation will happen automatically via KeepsyApp observing onboardingDone
                        }
                    },
                    modifier = Modifier.width(if (onboardingStep == totalSteps) 220.dp else 120.dp)
                )
            }
        }
    }
}

@Composable
fun OnboardingContent(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    description: String
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(200.dp)
                .clip(CircleShape)
                .background(PrimaryPurple.copy(alpha = 0.03f)),
            contentAlignment = Alignment.Center
        ) {
            // Glow effect simulated with multiple boxes
            Box(
                modifier = Modifier
                    .size(140.dp)
                    .clip(CircleShape)
                    .background(PrimaryPurple.copy(alpha = 0.05f))
            )
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = PrimaryPurple,
                modifier = Modifier
                    .size(100.dp)
                    .graphicsLayer(alpha = 0.9f)
            )
        }
        
        Spacer(modifier = Modifier.height(48.dp))
        
        Text(
            text = title,
            style = MaterialTheme.typography.displaySmall,
            fontWeight = FontWeight.ExtraBold,
            color = TextPrimary,
            textAlign = TextAlign.Center
        )
        
        Spacer(modifier = Modifier.height(20.dp))
        
        Text(
            text = description,
            style = MaterialTheme.typography.bodyLarge,
            color = TextSecondary,
            textAlign = TextAlign.Center,
            lineHeight = 32.sp,
            modifier = Modifier.padding(horizontal = 24.dp)
        )
    }
}
