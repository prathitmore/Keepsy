package com.keepsy.app.ui.tutorial

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.WavingHand
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.keepsy.app.navigation.SubScreen
import com.keepsy.app.navigation.TabScreen
import com.keepsy.app.ui.DashboardScaffold
import com.keepsy.app.ui.components.KeepsyBackgroundEffects
import com.keepsy.app.ui.components.PrimaryGradientButton
import com.keepsy.app.ui.theme.*
import com.keepsy.app.viewmodel.KeepsyViewModel
import kotlinx.coroutines.flow.first

@Composable
fun TutorialScreen(
    viewModel: KeepsyViewModel,
    tutorialViewModel: TutorialViewModel,
    onFinished: () -> Unit
) {
    val currentStep by tutorialViewModel.currentStep.collectAsState()
    val isVisible by tutorialViewModel.isVisible.collectAsState()

    var currentTab by remember { mutableStateOf<TabScreen>(TabScreen.Home) }
    val subScreenHistory = remember { mutableStateListOf<SubScreen>() }
    val currentSubScreen = subScreenHistory.lastOrNull() ?: SubScreen.None

    LaunchedEffect(Unit) {
        tutorialViewModel.startTutorial()
    }

    LaunchedEffect(isVisible) {
        if (!isVisible) {
            onFinished()
        }
    }

    // Interactive Step Monitoring
    val spacesList by viewModel.spaces.collectAsState(emptyList())
    val itemsList by viewModel.activeItems.collectAsState(emptyList())

    LaunchedEffect(spacesList.size) {
        if (currentStep == TutorialStep.SPACE_INTRO && spacesList.isNotEmpty()) {
            tutorialViewModel.nextStep()
        }
    }

    LaunchedEffect(itemsList.size) {
        if (currentStep == TutorialStep.ITEM_INTRO && itemsList.isNotEmpty()) {
            tutorialViewModel.nextStep()
        }
    }

    // Automatic Navigation based on Steps
    LaunchedEffect(currentStep) {
        when (currentStep) {
            TutorialStep.INTERFACE_OVERVIEW -> {
                currentTab = TabScreen.Home
                subScreenHistory.clear()
            }
            TutorialStep.SPACE_INTRO -> {
                currentTab = TabScreen.Spaces
                subScreenHistory.clear()
            }
            TutorialStep.SUBSPACE_INTRO -> {
                // Find the first space to add a subspace into
                val firstSpace = viewModel.spaces.first().firstOrNull()
                if (firstSpace != null) {
                    currentTab = TabScreen.Spaces
                    subScreenHistory.clear()
                    subScreenHistory.add(SubScreen.SpaceDetails(firstSpace.spaceId))
                } else {
                    // Fallback: stay on spaces tab if no space exists yet
                    currentTab = TabScreen.Spaces
                    subScreenHistory.clear()
                }
            }
            TutorialStep.ITEM_INTRO -> {
                currentTab = TabScreen.Home
                subScreenHistory.clear()
            }
            TutorialStep.RETRIEVAL_INTRO -> {
                currentTab = TabScreen.Search
                subScreenHistory.clear()
            }
            else -> {}
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(Background)) {
        if (currentStep != TutorialStep.WELCOME && currentStep != TutorialStep.COMPLETION) {
            DashboardScaffold(
                viewModel = viewModel,
                currentTab = currentTab,
                onTabSelected = { currentTab = it },
                currentSubScreen = currentSubScreen,
                onNavigateToSub = { subScreenHistory.add(it) },
                onPopSub = { 
                    if (subScreenHistory.isNotEmpty()) {
                        subScreenHistory.removeAt(subScreenHistory.size - 1)
                    }
                },
                tutorialViewModel = tutorialViewModel
            )
        } else {
            KeepsyBackgroundEffects()
            
            if (currentStep == TutorialStep.WELCOME) {
                TutorialSlide(
                    icon = Icons.Default.WavingHand,
                    title = "Organize Your World",
                    description = "We'll show you how to remember where you've kept everything in 3 interactive steps.",
                    buttonText = "Start Interactive Tour",
                    onButtonClick = { tutorialViewModel.nextStep() },
                    onSkip = { tutorialViewModel.skipTutorial() }
                )
            } else {
                TutorialSlide(
                    icon = Icons.Default.CheckCircle,
                    title = "You're All Set!",
                    description = "You've successfully learned how to use Spaces, Subspaces, and Items. You're ready to launch!",
                    buttonText = "Enter Dashboard",
                    onButtonClick = { tutorialViewModel.nextStep() },
                    onSkip = { tutorialViewModel.skipTutorial() },
                    showSummary = true
                )
            }
        }
        
        if (currentStep != TutorialStep.WELCOME && currentStep != TutorialStep.COMPLETION) {
            TutorialOverlay(viewModel = tutorialViewModel)
        }
    }
}

@Composable
fun TutorialSlide(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    description: String,
    buttonText: String,
    onButtonClick: () -> Unit,
    onSkip: () -> Unit,
    showSummary: Boolean = false
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(120.dp)
                .clip(CircleShape)
                .background(PrimaryPurple.copy(alpha = 0.1f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = PrimaryPurple,
                modifier = Modifier.size(60.dp)
            )
        }
        
        Spacer(modifier = Modifier.height(40.dp))
        
        Text(
            text = title,
            style = MaterialTheme.typography.displaySmall,
            fontWeight = FontWeight.ExtraBold,
            color = TextPrimary,
            textAlign = TextAlign.Center
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            text = description,
            style = MaterialTheme.typography.bodyLarge,
            color = TextSecondary,
            textAlign = TextAlign.Center,
            lineHeight = 28.sp
        )
        
        if (showSummary) {
            Spacer(modifier = Modifier.height(48.dp))
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                SummaryItem("Created your first Space (Home)")
                SummaryItem("Nested a Subspace (Bedroom)")
                SummaryItem("Saved a physical Item (Car Keys)")
                SummaryItem("Mastered Retrieval & Search")
            }
        }
        
        Spacer(modifier = Modifier.height(64.dp))
        
        PrimaryGradientButton(
            text = buttonText,
            onClick = onButtonClick
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        TextButton(onClick = onSkip) {
            Text("Skip and Go to Dashboard", color = TextSecondary, fontWeight = FontWeight.Medium)
        }
    }
}

@Composable
fun SummaryItem(text: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.Check,
            contentDescription = null,
            tint = PrimaryAccent,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = TextPrimary,
            fontWeight = FontWeight.Medium
        )
    }
}
