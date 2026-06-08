@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
package com.example.ui

import android.content.Context
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.model.*
import com.example.ui.theme.*
import com.example.viewmodel.KeepsyViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.Flow
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

// Custom Single-Activity Screen Backstack Definition
sealed class Screen {
    object WelcomeSplash : Screen()
    object Onboarding : Screen()
    object MainDashboard : Screen()
}

sealed class TabScreen {
    object Home : TabScreen()
    object Spaces : TabScreen()
    object Search : TabScreen()
    object Activity : TabScreen()
    object Settings : TabScreen()
}

// Navigation parameters holding current detailing views
sealed class SubScreen {
    object None : SubScreen()
    data class ItemDetails(val itemId: Long) : SubScreen()
    data class SpaceDetails(val spaceId: Long) : SubScreen()
    data class AddEditItem(val itemId: Long? = null, val spaceId: Long? = null) : SubScreen()
    data class AddEditSpace(val spaceId: Long? = null, val parentSpaceId: Long? = null) : SubScreen()
    data class MoveItem(val itemId: Long) : SubScreen()
    object TrashBin : SubScreen()
}

@Composable
fun KeepsyApp(viewModel: KeepsyViewModel, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // Preferences states
    val isOnboardingComplete by viewModel.isOnboardingCompleted.collectAsStateWithLifecycle()
    
    // Core Navigation Backstacks
    var appScreen by remember { mutableStateOf<Screen>(Screen.WelcomeSplash) }
    var currentTab by remember { mutableStateOf<TabScreen>(TabScreen.Home) }
    
    // Auxiliary Stack to manage details flow
    val subScreenHistory = remember { mutableStateListOf<SubScreen>() }
    val currentSubScreen = subScreenHistory.lastOrNull() ?: SubScreen.None

    fun navigateToSub(sub: SubScreen) {
        subScreenHistory.add(sub)
    }

    fun popSub(): Boolean {
        if (subScreenHistory.isNotEmpty()) {
            subScreenHistory.removeAt(subScreenHistory.size - 1)
            return true
        }
        return false
    }

    // Edge-to-edge support back handling
    BackHandler(enabled = appScreen == Screen.MainDashboard) {
        if (!popSub()) {
            if (currentTab != TabScreen.Home) {
                currentTab = TabScreen.Home
            } else {
                // If on Home with no details open, minimize app
                (context as? android.app.Activity)?.finish()
            }
        }
    }

    // Launch initial splash sequence
    LaunchedEffect(Unit) {
        delay(1300) // Fast luxurious 1.3s splash fade
        if (isOnboardingComplete) {
            appScreen = Screen.MainDashboard
        } else {
            appScreen = Screen.Onboarding
        }
    }

    Crossfade(targetState = appScreen, label = "AppTransition") { screen ->
        when (screen) {
            Screen.WelcomeSplash -> SplashScreenView()
            Screen.Onboarding -> OnboardingScreenView(
                viewModel = viewModel,
                onFinished = {
                    viewModel.setOnboardingCompleted()
                    appScreen = Screen.MainDashboard
                }
            )
            Screen.MainDashboard -> {
                DashboardScaffold(
                    viewModel = viewModel,
                    currentTab = currentTab,
                    onTabSelected = { tab ->
                        subScreenHistory.clear() // Reset stacks on tap changes
                        currentTab = tab
                    },
                    currentSubScreen = currentSubScreen,
                    onNavigateToSub = ::navigateToSub,
                    onPopSub = { popSub() }
                )
            }
        }
    }
}

// 1. --- SPLASH SCREEN VIEW ---
@Composable
fun SplashScreenView() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Slate900DarkBackground),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(32.dp)
        ) {
            Icon(
                imageVector = Icons.Default.AllInbox,
                contentDescription = "Keepsy Launcher",
                tint = HighlightTeal,
                modifier = Modifier
                    .size(96.dp)
                    .background(DeepIndigoDarkSelection, CircleShape)
                    .padding(20.dp)
            )
            Spacer(modifier = Modifier.height(18.dp))
            Text(
                text = "Keepsy",
                fontSize = 42.sp,
                fontFamily = FontFamily.SansSerif,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                letterSpacing = 1.sp
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Your memory for physical things.",
                fontSize = 15.sp,
                color = TextMutedDark,
                textAlign = TextAlign.Center
            )
        }
    }
}

// 2. --- ONBOARDING EXPERIENCE VIEW ---
@Composable
fun OnboardingScreenView(viewModel: KeepsyViewModel, onFinished: () -> Unit) {
    var onboardingStep by remember { mutableStateOf(1) }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // Temporary on-boarding pre-seed choices
    var firstSpaceName by remember { mutableStateOf("Home") }
    var firstItemName by remember { mutableStateOf("Passport") }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Slate900DarkBackground)
            .windowInsetsPadding(WindowInsets.safeDrawing),
        contentAlignment = Alignment.Center
    ) {
        AnimatedContent(
            targetState = onboardingStep,
            transitionSpec = {
                slideInHorizontally { width -> width } + fadeIn() togetherWith
                        slideOutHorizontally { width -> -width } + fadeOut()
            },
            label = "OnboardingStep"
        ) { step ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(28.dp),
                verticalArrangement = Arrangement.SpaceBetween,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Step Counter header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center
                ) {
                    repeat(3) { index ->
                        Box(
                            modifier = Modifier
                                .padding(horizontal = 4.dp)
                                .size(width = 32.dp, height = 4.dp)
                                .clip(RoundedCornerShape(2.dp))
                                .background(if (index + 1 == step) HighlightTeal else Color.DarkGray)
                        )
                    }
                }

                // Center visual display
                when (step) {
                    1 -> {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = "Searching",
                                tint = MutedRedDanger,
                                modifier = Modifier
                                    .size(110.dp)
                                    .background(Color(0x1AEF4444), CircleShape)
                                    .padding(24.dp)
                            )
                            Spacer(modifier = Modifier.height(36.dp))
                            Text(
                                text = "Stop searching for things you already own.",
                                fontSize = 26.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                textAlign = TextAlign.Center,
                                lineHeight = 34.sp
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "Did you keep the passport in the drawer? Or is it in the safe? We get it. We forget. Keepsy becomes your second brain.",
                                fontSize = 15.sp,
                                color = TextMutedDark,
                                textAlign = TextAlign.Center,
                                lineHeight = 22.sp
                            )
                        }
                    }
                    2 -> {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center,
                            modifier = Modifier.weight(1f)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Description,
                                    contentDescription = "Document",
                                    tint = HighlightTeal,
                                    modifier = Modifier
                                        .size(70.dp)
                                        .background(Color(0x1A14B8A6), CircleShape)
                                        .padding(14.dp)
                                )
                                Icon(
                                    imageVector = Icons.AutoMirrored.Default.KeyboardArrowRight,
                                    contentDescription = "Arrow",
                                    tint = Color.LightGray,
                                    modifier = Modifier.size(36.dp)
                                )
                                Icon(
                                    imageVector = Icons.Default.Inbox,
                                    contentDescription = "Drawer",
                                    tint = DeepIndigoPrimary,
                                    modifier = Modifier
                                        .size(70.dp)
                                        .background(Color(0x1A3F51B5), CircleShape)
                                        .padding(14.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(36.dp))
                            Text(
                                text = "Save where you keep things.",
                                fontSize = 26.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "Create hierarchical Spaces once. Assign items to drawer 1, cabinet shelves, or safe boxes with clear photos.",
                                fontSize = 15.sp,
                                color = TextMutedDark,
                                textAlign = TextAlign.Center,
                                lineHeight = 22.sp
                            )
                        }
                    }
                    3 -> {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center,
                            modifier = Modifier
                                .weight(1f)
                                .verticalScroll(rememberScrollState())
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = "Instant Find",
                                tint = SoftGreenSuccess,
                                modifier = Modifier
                                    .size(86.dp)
                                    .background(Color(0x1A2E7D32), CircleShape)
                                    .padding(16.dp)
                            )
                            Spacer(modifier = Modifier.height(24.dp))
                            Text(
                                text = "Find anything in under 5 seconds.",
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(18.dp))
                            Text(
                                text = "Let's create your first Space and physical Item to activate your memory tracker!",
                                fontSize = 14.sp,
                                color = TextMutedDark,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(20.dp))
                            
                            // Interactive entry fields
                            OutlinedTextField(
                                value = firstSpaceName,
                                onValueChange = { firstSpaceName = it },
                                label = { Text("First Storage Space") },
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = HighlightTeal,
                                    focusedLabelColor = HighlightTeal,
                                    unfocusedBorderColor = Color.Gray,
                                    unfocusedLabelColor = Color.Gray,
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White
                                ),
                                placeholder = { Text("e.g. Master Bedroom, Kitchen Drawer") },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("onboarding_first_space_input")
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            OutlinedTextField(
                                value = firstItemName,
                                onValueChange = { firstItemName = it },
                                label = { Text("First Item Tracked") },
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = HighlightTeal,
                                    focusedLabelColor = HighlightTeal,
                                    unfocusedBorderColor = Color.Gray,
                                    unfocusedLabelColor = Color.Gray,
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White
                                ),
                                placeholder = { Text("e.g. Passport, Car Keys, Charger") },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("onboarding_first_item_input")
                            )
                        }
                    }
                }

                // CTA Controls at bottom
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (step > 1) {
                        TextButton(
                            onClick = { onboardingStep-- },
                            colors = ButtonDefaults.textButtonColors(contentColor = Color.White)
                        ) {
                            Text("Back")
                        }
                    } else {
                        Spacer(modifier = Modifier.width(50.dp))
                    }

                    Button(
                        onClick = {
                            if (step < 3) {
                                onboardingStep++
                            } else {
                                // Final step - insert the preseeded Space and Item
                                if (firstSpaceName.isBlank() || firstItemName.isBlank()) {
                                    Toast.makeText(context, "Please enter both names", Toast.LENGTH_SHORT).show()
                                    return@Button
                                }
                                scope.launch {
                                    // Save Space
                                    viewModel.saveSpace(0L, firstSpaceName.trim(), "My primary room location.", null, "home", null, true) {
                                        // Once first space is generated, get spaces list to map new item to it
                                        scope.launch {
                                            val generatedSpaces = viewModel.spaces.first()
                                            val firstSpaceId = generatedSpaces.find { it.name == firstSpaceName.trim() }?.spaceId ?: 1L
                                            
                                            // Get first default category
                                            val defaultCatId = 1L // ID of Documents / Other
                                            
                                            viewModel.saveItem(0L, firstItemName.trim(), "Tracked item saved during onboarding.", firstSpaceId, defaultCatId, "Saved on onboarding", null, listOf("important", "welcome"), true) {
                                                Toast.makeText(context, "Future You Will Thank You.", Toast.LENGTH_LONG).show() // DELIGHT MOMENT
                                                onFinished()
                                            }
                                        }
                                    }
                                }
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = HighlightTeal, contentColor = Color.White),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .height(48.dp)
                            .testTag("onboarding_next_button")
                    ) {
                        Text(if (step == 3) "Get Started" else "Next")
                    }
                }
            }
        }
    }
}

// 3. --- MAIN SCOPE / VIEW scaffold ---
@Composable
fun DashboardScaffold(
    viewModel: KeepsyViewModel,
    currentTab: TabScreen,
    onTabSelected: (TabScreen) -> Unit,
    currentSubScreen: SubScreen,
    onNavigateToSub: (SubScreen) -> Unit,
    onPopSub: () -> Unit
) {
    val stats by viewModel.appStatistics.collectAsStateWithLifecycle()
    val spacesList by viewModel.spaces.collectAsStateWithLifecycle(emptyList())
    val categoriesList by viewModel.categories.collectAsStateWithLifecycle(emptyList())

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Scaffold(
            bottomBar = {
                if (currentSubScreen == SubScreen.None) {
                    NavigationBar(
                        containerColor = MaterialTheme.colorScheme.surface,
                        tonalElevation = 0.dp,
                        modifier = Modifier
                            .background(MaterialTheme.colorScheme.surface)
                            .padding(bottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding() / 2)
                    ) {
                        val items = listOf(
                            Triple(TabScreen.Home, Icons.Default.Home, "Home"),
                            Triple(TabScreen.Spaces, Icons.Default.Layers, "Spaces"),
                            Triple(TabScreen.Search, Icons.Default.Search, "Search"),
                            Triple(TabScreen.Activity, Icons.Default.History, "Activity"),
                            Triple(TabScreen.Settings, Icons.Default.Settings, "Settings")
                        )

                        items.forEach { (tab, icon, label) ->
                            NavigationBarItem(
                                selected = currentTab == tab,
                                onClick = { onTabSelected(tab) },
                                icon = { Icon(imageVector = icon, contentDescription = label) },
                                label = { Text(text = label, maxLines = 1, fontSize = 11.sp, fontWeight = if (currentTab == tab) FontWeight.Bold else FontWeight.Normal) },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = MaterialTheme.colorScheme.primary,
                                    selectedTextColor = MaterialTheme.colorScheme.primary,
                                    indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                                    unselectedIconColor = Color.Gray,
                                    unselectedTextColor = Color.Gray
                                ),
                                modifier = Modifier.testTag("nav_tab_${label.lowercase()}")
                            )
                        }
                    }
                }
            },
            floatingActionButton = {
                if (currentSubScreen == SubScreen.None && spacesList.isNotEmpty() && categoriesList.isNotEmpty()) {
                    FloatingActionButton(
                        onClick = { onNavigateToSub(SubScreen.AddEditItem()) },
                        containerColor = HighlightTeal,
                        contentColor = Color.White,
                        modifier = Modifier
                            .testTag("primary_add_fab")
                            .navigationBarsPadding()
                    ) {
                        Icon(imageVector = Icons.Default.Add, contentDescription = "Add Item")
                    }
                }
            }
        ) { padding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
            ) {
                // Tab Views Routing
                if (currentSubScreen == SubScreen.None) {
                    when (currentTab) {
                        TabScreen.Home -> HomeScreen(viewModel, onNavigateToSub)
                        TabScreen.Spaces -> SpacesScreen(viewModel, onNavigateToSub)
                        TabScreen.Search -> SearchScreen(viewModel, onNavigateToSub)
                        TabScreen.Activity -> ActivityScreen(viewModel, onNavigateToSub)
                        TabScreen.Settings -> SettingsScreen(viewModel, onNavigateToSub)
                    }
                } else {
                    // Navigate to Deep details views
                    Box(modifier = Modifier.fillMaxSize()) {
                        when (currentSubScreen) {
                            is SubScreen.ItemDetails -> ItemDetailsScreen(
                                itemId = currentSubScreen.itemId,
                                viewModel = viewModel,
                                onPop = onPopSub,
                                onNavigateToSub = onNavigateToSub
                            )
                            is SubScreen.SpaceDetails -> SpaceDetailsScreen(
                                spaceId = currentSubScreen.spaceId,
                                viewModel = viewModel,
                                onPop = onPopSub,
                                onNavigateToSub = onNavigateToSub
                            )
                            is SubScreen.AddEditItem -> AddEditItemScreen(
                                itemId = currentSubScreen.itemId,
                                initialSpaceId = currentSubScreen.spaceId,
                                viewModel = viewModel,
                                onPop = onPopSub
                            )
                            is SubScreen.AddEditSpace -> AddEditSpaceScreen(
                                spaceId = currentSubScreen.spaceId,
                                parentSpaceId = currentSubScreen.parentSpaceId,
                                viewModel = viewModel,
                                onPop = onPopSub
                            )
                            is SubScreen.MoveItem -> MoveItemScreen(
                                itemId = currentSubScreen.itemId,
                                viewModel = viewModel,
                                onPop = onPopSub
                            )
                            SubScreen.TrashBin -> TrashBinScreen(
                                viewModel = viewModel,
                                onPop = onPopSub
                            )
                            else -> {}
                        }
                    }
                }
            }
        }
    }
}

// Helper category color generator mapping hex directly to Compose Color
fun parseCategoryColor(hex: String?): Color {
    if (hex == null) return DeepIndigoPrimary
    return try {
        Color(android.graphics.Color.parseColor(hex))
    } catch (e: Exception) {
        DeepIndigoPrimary
    }
}

// Map strings to static Vector icons
@Composable
fun getCategoryIconVector(iconName: String?): ImageVector {
    return when (iconName) {
        "description" -> Icons.Default.Description
        "devices" -> Icons.Default.Devices
        "key" -> Icons.Default.VpnKey
        "medical_services" -> Icons.Default.MedicalServices
        "diamond" -> Icons.Default.Diamond
        "build" -> Icons.Default.Build
        "inventory_2" -> Icons.Default.Inventory2
        "home" -> Icons.Default.Home
        "more_horiz" -> Icons.Default.MoreHoriz
        else -> Icons.Default.Category
    }
}

@Composable
fun getSpaceIconVector(iconName: String?): ImageVector {
    return when (iconName) {
        "home" -> Icons.Default.Home
        "bedroom" -> Icons.Default.Bed
        "kitchen" -> Icons.Default.Kitchen
        "cabinet" -> Icons.Default.Inventory
        "backpack" -> Icons.Default.Work
        "suitcase" -> Icons.Default.Luggage
        "lock" -> Icons.Default.Lock
        else -> Icons.Default.Inbox
    }
}

// 4. --- TAB 1: HOME SCREEN FEED ---
@Composable
fun HomeScreen(viewModel: KeepsyViewModel, onNavigateToSub: (SubScreen) -> Unit) {
    val stats by viewModel.appStatistics.collectAsStateWithLifecycle()
    val favoritesList by viewModel.favoriteSpaces.collectAsStateWithLifecycle()
    val recentItemsList by viewModel.recentItems.collectAsStateWithLifecycle()
    val recentlyViewedList by viewModel.recentlyViewedItems.collectAsStateWithLifecycle()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("home_screen_container")
            .background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(16.dp)
    ) {
        // Welcoming Title Area
        item {
            Column(modifier = Modifier.padding(bottom = 16.dp)) {
                Text(
                    text = "Keepsy",
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Black, // Ultra bold
                    fontFamily = FontFamily.SansSerif,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = "A physical memory for your items.",
                    fontSize = 14.sp,
                    color = Color.Gray
                )
            }
        }

        // Hero dominates direct search click overlay
        item {
            Card(
                onClick = { /* Navigate to search tab directly */ },
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(28.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp)
                    .testTag("home_quick_search_card")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Search Items",
                        tint = HighlightTeal,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "Where did I keep that...?",
                        fontSize = 15.sp,
                        color = Color.Gray,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        // Stats Row Card Layouts
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                shape = RoundedCornerShape(24.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(text = "External Memory Status", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimaryContainer)
                        Text(text = "Keeping track on-device, 100% offline", fontSize = 11.sp, color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f))
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(text = "${stats.totalItems}", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimaryContainer)
                            Text(text = "Items", fontSize = 11.sp, color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f))
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(text = "${stats.totalSpaces}", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimaryContainer)
                            Text(text = "Spaces", fontSize = 11.sp, color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f))
                        }
                    }
                }
            }
        }

        // Section: Favorite Spaces (Horizontal Scroll)
        if (favoritesList.isNotEmpty()) {
            item {
                Column(modifier = Modifier.padding(top = 12.dp, bottom = 12.dp)) {
                    Text(
                        text = "Favorite Spaces",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(favoritesList) { space ->
                            SpaceHorizontalCard(space = space, onClick = { onNavigateToSub(SubScreen.SpaceDetails(space.spaceId)) })
                        }
                    }
                }
            }
        }

        // Recently Viewed items
        if (recentlyViewedList.isNotEmpty()) {
            item {
                Text(
                    text = "Recently Viewed",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.padding(top = 16.dp, bottom = 8.dp)
                )
            }
            items(recentlyViewedList) { itemDetails ->
                ItemRowCard(itemDetails = itemDetails, onClick = { onNavigateToSub(SubScreen.ItemDetails(itemDetails.item.itemId)) })
            }
        }

        // Recently Added tracked physical items
        item {
            Text(
                text = "Recently Logged Belongings",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.padding(top = 16.dp, bottom = 8.dp)
            )
        }

        if (recentItemsList.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(imageVector = Icons.Default.SentimentDissatisfied, contentDescription = "", tint = Color.LightGray, modifier = Modifier.size(48.dp))
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(text = "No items tracked yet.", color = Color.Gray, fontSize = 14.sp)
                    }
                }
            }
        } else {
            items(recentItemsList) { itemDetails ->
                ItemRowCard(itemDetails = itemDetails, onClick = { onNavigateToSub(SubScreen.ItemDetails(itemDetails.item.itemId)) })
            }
        }
    }
}

@Composable
fun SpaceHorizontalCard(space: Space, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)),
        modifier = Modifier
            .width(135.dp)
            .height(130.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Icon(
                imageVector = getSpaceIconVector(space.icon),
                contentDescription = null,
                tint = HighlightTeal,
                modifier = Modifier.size(32.dp)
            )
            Column {
                Text(
                    text = space.name,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = if (space.parentSpaceId != null) "Nested sub-space" else "Root-level location",
                    fontSize = 10.sp,
                    color = Color.Gray
                )
            }
        }
    }
}

@Composable
fun ItemRowCard(itemDetails: ItemWithDetails, onClick: () -> Unit) {
    val context = LocalContext.current
    Card(
        onClick = onClick,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .testTag("item_card_${itemDetails.item.itemId}")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Fast loading dynamic local photos with Coil
            if (itemDetails.item.photoPath != null && File(itemDetails.item.photoPath).exists()) {
                AsyncImage(
                    model = File(itemDetails.item.photoPath),
                    contentDescription = itemDetails.item.name,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(54.dp)
                        .clip(RoundedCornerShape(14.dp))
                )
            } else {
                // Fallback elegant category badge
                Box(
                    modifier = Modifier
                        .size(54.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(parseCategoryColor(itemDetails.category?.color).copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = getCategoryIconVector(itemDetails.category?.icon),
                        contentDescription = "Fallback",
                        tint = parseCategoryColor(itemDetails.category?.color),
                        modifier = Modifier.size(26.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = itemDetails.item.name,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = Icons.Default.Launch, contentDescription = "", tint = HighlightTeal, modifier = Modifier.size(11.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = itemDetails.space?.name ?: "Unknown area",
                        fontSize = 11.sp,
                        color = HighlightTeal,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "• ${itemDetails.category?.name ?: "Other"}",
                        fontSize = 11.sp,
                        color = Color.Gray
                    )
                }
            }
            
            if (itemDetails.item.isFavorite) {
                Icon(
                    imageVector = Icons.Default.Favorite,
                    contentDescription = "Starred",
                    tint = MutedRedDanger,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

// 5. --- TAB 2: SPACES TREE BOARD ---
@Composable
fun SpacesScreen(viewModel: KeepsyViewModel, onNavigateToSub: (SubScreen) -> Unit) {
    val spacesList by viewModel.spaces.collectAsStateWithLifecycle(emptyList())
    var expandedSpaceIds = remember { mutableStateListOf<Long>() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Nesting Spaces",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = "A physical map of your containers.",
                    fontSize = 13.sp,
                    color = Color.Gray
                )
            }
            IconButton(
                onClick = { onNavigateToSub(SubScreen.AddEditSpace()) },
                modifier = Modifier.testTag("add_space_action_btn")
            ) {
                Icon(imageVector = Icons.Default.AddHome, contentDescription = "New Space", tint = HighlightTeal)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (spacesList.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(24.dp)) {
                    Icon(imageVector = Icons.Default.Layers, contentDescription = null, tint = Color.LightGray, modifier = Modifier.size(72.dp))
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(text = "No spaces built yet", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    Text(text = "To keep track of passports, chargers or keys, build nesting drawer models.", textAlign = TextAlign.Center, color = Color.Gray, fontSize = 14.sp)
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = { onNavigateToSub(SubScreen.AddEditSpace()) },
                        colors = ButtonDefaults.buttonColors(containerColor = HighlightTeal)
                    ) {
                        Text("Build First Space")
                    }
                }
            }
        } else {
            // Draw Hierarchical Tree
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // Find all Root spaces (where parentSpaceId is null)
                val rootSpaces = spacesList.filter { it.parentSpaceId == null }
                items(rootSpaces) { rootSpace ->
                    SpaceTreeNode(
                        space = rootSpace,
                        allSpaces = spacesList,
                        depth = 0,
                        expandedIds = expandedSpaceIds,
                        onNodeClick = { spaceId ->
                            if (expandedSpaceIds.contains(spaceId)) {
                                expandedSpaceIds.remove(spaceId)
                            } else {
                                expandedSpaceIds.add(spaceId)
                            }
                        },
                        onNodeDetails = { spaceId -> onNavigateToSub(SubScreen.SpaceDetails(spaceId)) }
                    )
                }
            }
        }
    }
}

// Recursive Tree Row Node styling
@Composable
fun SpaceTreeNode(
    space: Space,
    allSpaces: List<Space>,
    depth: Int,
    expandedIds: List<Long>,
    onNodeClick: (Long) -> Unit,
    onNodeDetails: (Long) -> Unit
) {
    val childSpaces = allSpaces.filter { it.parentSpaceId == space.spaceId }
    val isExpanded = expandedIds.contains(space.spaceId)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = (depth * 20).dp)
    ) {
        Card(
            onClick = { onNodeDetails(space.spaceId) },
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(20.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)),
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp)
                .testTag("space_node_card_${space.spaceId}")
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (childSpaces.isNotEmpty()) {
                    IconButton(
                        onClick = { onNodeClick(space.spaceId) },
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            imageVector = if (isExpanded) Icons.Default.ExpandMore else Icons.Default.ChevronRight,
                            contentDescription = "Expand",
                            tint = Color.Gray,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                } else {
                    Spacer(modifier = Modifier.size(24.dp))
                }

                Spacer(modifier = Modifier.width(4.dp))
                
                Icon(
                    imageVector = getSpaceIconVector(space.icon),
                    contentDescription = null,
                    tint = if (space.isFavorite) MutedRedDanger else HighlightTeal,
                    modifier = Modifier.size(20.dp)
                )

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = space.name,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (space.description.isNotEmpty()) {
                        Text(
                            text = space.description,
                            fontSize = 11.sp,
                            color = Color.Gray,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                
                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = "View Details",
                    tint = Color.LightGray,
                    modifier = Modifier.size(16.dp)
                )
            }
        }

        if (childSpaces.isNotEmpty() && isExpanded) {
            childSpaces.forEach { sub ->
                SpaceTreeNode(
                    space = sub,
                    allSpaces = allSpaces,
                    depth = depth + 1,
                    expandedIds = expandedIds,
                    onNodeClick = onNodeClick,
                    onNodeDetails = onNodeDetails
                )
            }
        }
    }
}

// 6. --- TAB 3: INSTANT SEARCH SCREEN ---
@Composable
fun SearchScreen(viewModel: KeepsyViewModel, onNavigateToSub: (SubScreen) -> Unit) {
    val query by viewModel.searchQuery.collectAsStateWithLifecycle()
    val searchResults by viewModel.searchResults.collectAsStateWithLifecycle()
    val spacesList by viewModel.spaces.collectAsStateWithLifecycle(emptyList())

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp)
    ) {
        // Hero bar input
        OutlinedTextField(
            value = query,
            onValueChange = { viewModel.updateSearchQuery(it) },
            leadingIcon = { Icon(imageVector = Icons.Default.Search, contentDescription = "Search", tint = HighlightTeal) },
            trailingIcon = {
                if (query.isNotEmpty()) {
                    IconButton(onClick = { viewModel.updateSearchQuery("") }) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Clear", tint = Color.Gray)
                    }
                }
            },
            placeholder = { Text("Search items, boxes, notes, folders...") },
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = HighlightTeal,
                unfocusedBorderColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.15f),
                focusedTextColor = MaterialTheme.colorScheme.onBackground,
                unfocusedTextColor = MaterialTheme.colorScheme.onBackground
            ),
            shape = RoundedCornerShape(28.dp),
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("instant_search_input_field")
        )

        Spacer(modifier = Modifier.height(16.dp))

        if (query.trim().isBlank()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(24.dp)) {
                    Icon(imageVector = Icons.Default.ManageSearch, contentDescription = null, tint = HighlightTeal.copy(alpha = 0.4f), modifier = Modifier.size(86.dp))
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(text = "Unlocking instant retrieval", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                    Text(text = "Type item names, categories, spaces, or custom drawer logs. Results populate instantly as you type.", textAlign = TextAlign.Center, color = Color.Gray, fontSize = 13.sp)
                }
            }
        } else if (searchResults.items.isEmpty() && searchResults.spaces.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(24.dp)) {
                    Icon(imageVector = Icons.Default.ContentPasteSearch, contentDescription = null, tint = MutedRedDanger.copy(alpha = 0.4f), modifier = Modifier.size(86.dp))
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(text = "Nothing found.", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                    Text(text = "Try another keyword, tags or parent spaces.", textAlign = TextAlign.Center, color = Color.Gray, fontSize = 13.sp)
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (searchResults.spaces.isNotEmpty()) {
                    item {
                        Text(text = "Matched Spaces (${searchResults.spaces.size})", fontWeight = FontWeight.Bold, color = HighlightTeal, fontSize = 14.sp)
                    }
                    items(searchResults.spaces) { space ->
                        Card(
                            onClick = { onNavigateToSub(SubScreen.SpaceDetails(space.spaceId)) },
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(imageVector = getSpaceIconVector(space.icon), contentDescription = null, tint = HighlightTeal, modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(text = space.name, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                Spacer(modifier = Modifier.weight(1f))
                                Text(text = "Storage Space", fontSize = 11.sp, color = Color.LightGray)
                            }
                        }
                    }
                }

                if (searchResults.items.isNotEmpty()) {
                    item {
                        Text(text = "Matched Items (${searchResults.items.size})", fontWeight = FontWeight.Bold, color = DeepIndigoPrimary, fontSize = 14.sp)
                    }
                    items(searchResults.items) { itemsDetails ->
                        ItemRowCard(itemDetails = itemsDetails, onClick = { onNavigateToSub(SubScreen.ItemDetails(itemsDetails.item.itemId)) })
                    }
                }
            }
        }
    }
}

// 7. --- TAB 4: RECENT ACTIVITY TIMELINE SCREEN ---
@Composable
fun ActivityScreen(viewModel: KeepsyViewModel, onNavigateToSub: (SubScreen) -> Unit) {
    val activityLogs by viewModel.activityLogs.collectAsStateWithLifecycle(emptyList())

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Memory Trail",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = "System log of your belongings and spaces.",
                    fontSize = 13.sp,
                    color = Color.Gray
                )
            }
            TextButton(
                onClick = { onNavigateToSub(SubScreen.TrashBin) }
            ) {
                Icon(imageVector = Icons.Default.DeleteSweep, contentDescription = "", tint = HighlightTeal)
                Spacer(modifier = Modifier.width(4.dp))
                Text("Trash Bin", color = HighlightTeal)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (activityLogs.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Text(text = "No recorded history yet.", color = Color.Gray)
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f)
            ) {
                items(activityLogs) { log ->
                    TimelineCard(log = log, onClickItem = {
                        if (log.itemId != 0L) {
                            onNavigateToSub(SubScreen.ItemDetails(log.itemId))
                        }
                    })
                }
            }
        }
    }
}

// Timeline Graphic representation
@Composable
fun TimelineCard(log: ActivityLog, onClickItem: () -> Unit) {
    val formattedDate = remember(log.timestamp) {
        try {
            val format = SimpleDateFormat("MMM dd, yyyy • hh:mm a", Locale.getDefault())
            format.format(Date(log.timestamp))
        } catch (e: Exception) {
            ""
        }
    }

    val actionColor = when (log.actionType) {
        "CREATED" -> SoftGreenSuccess
        "MOVED" -> WarmAmberWarning
        "DELETED", "PURGED" -> MutedRedDanger
        "RESTORED" -> HighlightTeal
        "VIEWED" -> DeepIndigoPrimary
        else -> Color.Gray
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clickable(enabled = log.itemId != 0L, onClick = onClickItem)
            .testTag("activity_row_item_${log.activityId}")
    ) {
        // Vertical Timeline Graphic Node
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(top = 4.dp, end = 12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(14.dp)
                    .background(actionColor, CircleShape)
            )
            Box(
                modifier = Modifier
                    .width(2.dp)
                    .height(38.dp)
                    .background(Color.Gray.copy(alpha = 0.5f))
            )
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .padding(bottom = 12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = log.actionType,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = actionColor
                )
                Text(
                    text = formattedDate,
                    fontSize = 10.sp,
                    color = Color.Gray
                )
            }
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = log.details,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

// 8. --- TAB 5: SETTINGS SCREEN ---
@Composable
fun SettingsScreen(viewModel: KeepsyViewModel, onNavigateToSub: (SubScreen) -> Unit) {
    val darkModePreferred by viewModel.darkModePreference.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    var showBackupArea by remember { mutableStateOf(false) }
    var backupString by remember { mutableStateOf("") }
    var restoreString by remember { mutableStateOf("") }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Column {
                Text(
                    text = "Preferences",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = "Manage your database, theme, and offline options.",
                    fontSize = 13.sp,
                    color = Color.Gray
                )
            }
        }

        // Appearance
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(text = "Appearance Theme", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        FilterChip(
                            selected = darkModePreferred == null,
                            onClick = { viewModel.setDarkModePreference(null) },
                            label = { Text("System Default") }
                        )
                        FilterChip(
                            selected = darkModePreferred == false,
                            onClick = { viewModel.setDarkModePreference(false) },
                            label = { Text("Light Mode") }
                        )
                        FilterChip(
                            selected = darkModePreferred == true,
                            onClick = { viewModel.setDarkModePreference(true) },
                            label = { Text("Dark Mode") }
                        )
                    }
                }
            }
        }

        // Data Management / Backup Export Import Option
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(text = "Data Management", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Text(text = "Keepsy is 100% offline-first. Your memories reside solely on your device. Take regular local backups.", fontSize = 12.sp, color = Color.Gray)
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    Button(
                        onClick = {
                            viewModel.exportBackup { json ->
                                backupString = json
                                showBackupArea = true
                                Toast.makeText(context, "Backup Completed! Copy JSON below.", Toast.LENGTH_SHORT).show()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = HighlightTeal),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(imageVector = Icons.Default.Backup, contentDescription = "")
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Export Full Local Backup JSON")
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    if (showBackupArea && backupString.isNotEmpty()) {
                        OutlinedTextField(
                            value = backupString,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Your Encrypted On-Device Backup") },
                            colors = OutlinedTextFieldDefaults.colors(focusedTextColor = MaterialTheme.colorScheme.onBackground, unfocusedTextColor = MaterialTheme.colorScheme.onBackground),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(120.dp)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        TextButton(
                            onClick = {
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                                val clip = android.content.ClipData.newPlainText("Keepsy Backup", backupString)
                                clipboard.setPrimaryClip(clip)
                                Toast.makeText(context, "Copied backup JSON to clipboard!", Toast.LENGTH_SHORT).show()
                            }
                        ) {
                            Text("Copy JSON to Clipboard")
                        }
                    }

                    HorizontalDivider(modifier = Modifier.padding(top = 12.dp, bottom = 12.dp))

                    Text(text = "Restore Local Sync Backup", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Spacer(modifier = Modifier.height(6.dp))
                    OutlinedTextField(
                        value = restoreString,
                        onValueChange = { restoreString = it },
                        label = { Text("Paste Saved JSON String Here") },
                        placeholder = { Text("{ \"version\": 1 ... }") },
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = MaterialTheme.colorScheme.onBackground, unfocusedTextColor = MaterialTheme.colorScheme.onBackground),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(100.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        onClick = {
                            if (restoreString.isBlank()) {
                                Toast.makeText(context, "Please paste valid backup JSON first", Toast.LENGTH_SHORT).show()
                                return@Button
                            }
                            viewModel.importBackup(restoreString) { success ->
                                if (success) {
                                    restoreString = ""
                                    Toast.makeText(context, "Backup restored successfully!", Toast.LENGTH_LONG).show() // SUCCESS FEEDBACK
                                } else {
                                    Toast.makeText(context, "Restore failed. Please check JSON contents.", Toast.LENGTH_LONG).show()
                                }
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = HighlightTeal),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(imageVector = Icons.Default.Restore, contentDescription = "")
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Verify and Restore Backup Now")
                    }
                }
            }
        }

        // About / Clear Memory Safe Guard
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(text = "Danger Zone", fontWeight = FontWeight.Bold, color = MutedRedDanger, fontSize = 16.sp)
                    Spacer(modifier = Modifier.height(6.dp))
                    Button(
                        onClick = {
                            viewModel.resetApp()
                            Toast.makeText(context, "All memory databases wiped.", Toast.LENGTH_LONG).show()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MutedRedDanger),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Erase on-device cache database completely")
                    }
                }
            }
        }

        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(text = "Keepsy Version 1.0 (Founder Edition)", fontSize = 12.sp, color = Color.Gray)
                Text(text = "Made for adults, photographers, and travelers.", fontSize = 10.sp, color = Color.Gray)
                Spacer(modifier = Modifier.height(48.dp))
            }
        }
    }
}

// 9. --- SUBSCREEN: DETAILED ITEM VIEW SCREEN ---
@Composable
fun ItemDetailsScreen(
    itemId: Long,
    viewModel: KeepsyViewModel,
    onPop: () -> Unit,
    onNavigateToSub: (SubScreen) -> Unit
) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val itemDetails by viewModel.selectedItem.collectAsStateWithLifecycle()
    val activityTrail by viewModel.getActivityTrailForItem(itemId).collectAsState(initial = emptyList())

    var displayDeleteDialog by remember { mutableStateOf(false) }

    LaunchedEffect(itemId) {
        viewModel.selectItem(itemId)
    }

    // Scaffold for Item detail screen
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(itemDetails?.item?.name ?: "Belonging details", maxLines = 1, overflow = TextOverflow.Ellipsis) },
                navigationIcon = {
                    IconButton(onClick = onPop) {
                        Icon(imageVector = Icons.AutoMirrored.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    itemDetails?.let { details ->
                        IconButton(onClick = { viewModel.toggleItemFavorite(details.item.itemId) }) {
                            Icon(
                                imageVector = if (details.item.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                contentDescription = "Favorite",
                                tint = if (details.item.isFavorite) MutedRedDanger else Color.Gray
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors()
            )
        }
    ) { innerPadding ->
        val details = itemDetails
        if (details != null) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .background(MaterialTheme.colorScheme.background)
                    .verticalScroll(rememberScrollState())
            ) {
                // Header Hero photo OR color block fallback
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(260.dp)
                ) {
                    if (details.item.photoPath != null && File(details.item.photoPath).exists()) {
                        AsyncImage(
                            model = File(details.item.photoPath),
                            contentDescription = details.item.name,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        // Fallback elegant category badge
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    Brush.verticalGradient(
                                        colors = listOf(
                                            parseCategoryColor(details.category?.color),
                                            parseCategoryColor(details.category?.color).copy(alpha = 0.5f)
                                        )
                                    )
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    imageVector = getCategoryIconVector(details.category?.icon),
                                    contentDescription = "",
                                    tint = Color.White,
                                    modifier = Modifier.size(72.dp)
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    text = details.category?.name ?: "Other",
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    fontSize = 18.sp
                                )
                            }
                        }
                    }
                }

                // Core Metadata layout
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = details.item.name,
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))

                    // Location clickable card
                    Card(
                        onClick = { details.space?.spaceId?.let { onNavigateToSub(SubScreen.SpaceDetails(it)) } },
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(imageVector = Icons.Default.Layers, contentDescription = "", tint = MaterialTheme.colorScheme.onPrimaryContainer)
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(text = "Stored At Location", fontSize = 11.sp, color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.6f))
                                Text(text = details.space?.name ?: "Unknown Area", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimaryContainer)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    if (details.item.description.isNotEmpty()) {
                        Text(text = "Description", fontWeight = FontWeight.Bold, color = Color.Gray, fontSize = 13.sp)
                        Text(text = details.item.description, fontSize = 15.sp, modifier = Modifier.padding(top = 4.dp, bottom = 12.dp))
                    }

                    if (details.item.notes.isNotEmpty()) {
                        Text(text = "Special Retrieval Notes", fontWeight = FontWeight.Bold, color = Color.Gray, fontSize = 13.sp)
                        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant), modifier = Modifier.fillMaxWidth().padding(top = 4.dp, bottom = 12.dp)) {
                            Text(text = details.item.notes, fontSize = 14.sp, modifier = Modifier.padding(12.dp), fontStyle = androidx.compose.ui.text.font.FontStyle.Italic)
                        }
                    }

                    if (details.tags.isNotEmpty()) {
                        Text(text = "Labels", fontWeight = FontWeight.Bold, color = Color.Gray, fontSize = 13.sp)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 4.dp, bottom = 12.dp),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            details.tags.forEach { tag ->
                                Box(
                                    modifier = Modifier
                                        .background(Color.Gray.copy(alpha = 0.15f), RoundedCornerShape(8.dp))
                                        .padding(horizontal = 10.dp, vertical = 4.dp)
                                ) {
                                    Text(text = "#${tag.name}", fontSize = 12.sp, color = HighlightTeal, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }

                    // Memory Trail - Location History section
                    Text(
                        text = "Recent Memory Trail",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.padding(top = 16.dp, bottom = 12.dp)
                    )

                    if (activityTrail.isEmpty()) {
                        Text(text = "Initialization logs pending.", color = Color.Gray, fontSize = 13.sp)
                    } else {
                        activityTrail.forEach { log ->
                            TimelineCard(log = log, onClickItem = {})
                        }
                    }

                    Spacer(modifier = Modifier.height(28.dp))

                    // Buttons list
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { onNavigateToSub(SubScreen.MoveItem(details.item.itemId)) },
                            colors = ButtonDefaults.buttonColors(containerColor = HighlightTeal),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(imageVector = Icons.Default.MultipleStop, contentDescription = "")
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Move Item")
                        }

                        OutlinedButton(
                            onClick = { onNavigateToSub(SubScreen.AddEditItem(itemId = details.item.itemId)) },
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(imageVector = Icons.Default.Edit, contentDescription = "")
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Edit")
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Button(
                        onClick = { displayDeleteDialog = true },
                        colors = ButtonDefaults.buttonColors(containerColor = MutedRedDanger),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(imageVector = Icons.Default.Delete, contentDescription = "")
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Move Item to Trash")
                    }
                }
            }
        }
    }

    if (displayDeleteDialog) {
        AlertDialog(
            onDismissRequest = { displayDeleteDialog = false },
            title = { Text("Confirm deletion?") },
            text = { Text("The item will be moved to the Trash Bin, where you can restore it within 30 days or delete it permanently.") },
            confirmButton = {
                Button(
                    onClick = {
                        displayDeleteDialog = false
                        viewModel.softDeleteSelectedItem {
                            Toast.makeText(context, "Item moved to trash.", Toast.LENGTH_SHORT).show()
                            onPop()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MutedRedDanger)
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { displayDeleteDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

// 10. --- SUBSCREEN: DETAILED SPACE VIEW SCREEN ---
@Composable
fun SpaceDetailsScreen(
    spaceId: Long,
    viewModel: KeepsyViewModel,
    onPop: () -> Unit,
    onNavigateToSub: (SubScreen) -> Unit
) {
    val context = LocalContext.current
    val spaceDetails by viewModel.selectedSpace.collectAsStateWithLifecycle()
    val nestedList by viewModel.nestedSubspaces.collectAsStateWithLifecycle()
    val itemsInThisSpace by viewModel.itemsInSpace.collectAsStateWithLifecycle()

    var showDeleteAlert by remember { mutableStateOf(false) }

    LaunchedEffect(spaceId) {
        viewModel.selectSpace(spaceId)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(spaceDetails?.space?.name ?: "Space details", maxLines = 1, overflow = TextOverflow.Ellipsis) },
                navigationIcon = {
                    IconButton(onClick = onPop) {
                        Icon(imageVector = Icons.AutoMirrored.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    spaceDetails?.let { details ->
                        IconButton(onClick = { viewModel.toggleSpaceFavorite(details.space.spaceId) }) {
                            Icon(
                                imageVector = if (details.space.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                tint = if (details.space.isFavorite) MutedRedDanger else Color.Gray,
                                contentDescription = "Fav"
                            )
                        }
                    }
                }
            )
        }
    ) { innerPadding ->
        spaceDetails?.let { details ->
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .background(MaterialTheme.colorScheme.background),
                contentPadding = PaddingValues(16.dp)
            ) {
                // Header image or fallback
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp)
                            .clip(RoundedCornerShape(12.dp))
                    ) {
                        if (details.space.photoPath != null && File(details.space.photoPath).exists()) {
                            AsyncImage(
                                model = File(details.space.photoPath),
                                contentDescription = details.space.name,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        } else {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(HighlightTeal.copy(alpha = 0.2f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = getSpaceIconVector(details.space.icon),
                                    contentDescription = "",
                                    tint = HighlightTeal,
                                    modifier = Modifier.size(64.dp)
                                )
                            }
                        }
                    }
                }

                item {
                    Column(modifier = Modifier.padding(vertical = 16.dp)) {
                        Text(text = details.space.name, fontSize = 26.sp, fontWeight = FontWeight.Bold)
                        if (details.space.description.isNotEmpty()) {
                            Text(text = details.space.description, fontSize = 14.sp, color = Color.Gray)
                        }
                        if (details.parentSpace != null) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(text = "Nested inside: ${details.parentSpace.name}", fontSize = 12.sp, color = HighlightTeal)
                        }
                    }
                }

                // Subspaces (Nested container spaces) Section
                if (nestedList.isNotEmpty()) {
                    item {
                        Text(text = "Nested Sub-spaces", fontWeight = FontWeight.Bold, fontSize = 16.sp, modifier = Modifier.padding(bottom = 8.dp))
                    }
                    items(nestedList) { sub ->
                        Card(
                            onClick = { onNavigateToSub(SubScreen.SpaceDetails(sub.spaceId)) },
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 3.dp)
                        ) {
                            Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(imageVector = getSpaceIconVector(sub.icon), contentDescription = null, tint = HighlightTeal, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(text = sub.name, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            }
                        }
                    }
                    item { Spacer(modifier = Modifier.height(16.dp)) }
                }

                // Assigned belongings Inside this container
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = "Tracked Items Inside", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        IconButton(onClick = { onNavigateToSub(SubScreen.AddEditItem(spaceId = details.space.spaceId)) }) {
                            Icon(imageVector = Icons.Default.Add, contentDescription = "Add Item Here", tint = HighlightTeal)
                        }
                    }
                }

                if (itemsInThisSpace.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(text = "No items tracked inside this space.", color = Color.Gray, fontSize = 14.sp)
                        }
                    }
                } else {
                    items(itemsInThisSpace) { item ->
                        ItemRowCard(itemDetails = item, onClick = { onNavigateToSub(SubScreen.ItemDetails(item.item.itemId)) })
                    }
                }

                // Space action panel at bottom
                item {
                    Spacer(modifier = Modifier.height(24.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { onNavigateToSub(SubScreen.AddEditSpace(spaceId = details.space.spaceId)) },
                            colors = ButtonDefaults.buttonColors(containerColor = HighlightTeal),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(imageVector = Icons.Default.Edit, contentDescription = "")
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Edit Space")
                        }

                        Button(
                            onClick = { onNavigateToSub(SubScreen.AddEditSpace(parentSpaceId = details.space.spaceId)) },
                            colors = ButtonDefaults.buttonColors(containerColor = DeepIndigoPrimary),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(imageVector = Icons.Default.Subtitles, contentDescription = "")
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Add Sub-Space")
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Button(
                        onClick = { showDeleteAlert = true },
                        colors = ButtonDefaults.buttonColors(containerColor = MutedRedDanger),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(imageVector = Icons.Default.Delete, contentDescription = "")
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Delete Space container")
                    }
                    Spacer(modifier = Modifier.height(24.dp))
                }
            }
        }
    }

    if (showDeleteAlert) {
        AlertDialog(
            onDismissRequest = { showDeleteAlert = false },
            title = { Text("Confirm Space Deletion?") },
            text = { Text("Deleting this space container will un-nest any child sub-spaces dynamically. Items currently mapped directly to this Space container will stay preserved.") },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteAlert = false
                        viewModel.deleteSpace(spaceId) {
                            Toast.makeText(context, "Space container deleted.", Toast.LENGTH_SHORT).show()
                            onPop()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MutedRedDanger)
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteAlert = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

// 11. --- SUBSCREEN: ADD / EDIT ITEM SCREEN ---
@Composable
fun AddEditItemScreen(
    itemId: Long?,
    initialSpaceId: Long?,
    viewModel: KeepsyViewModel,
    onPop: () -> Unit
) {
    val context = LocalContext.current
    val spacesList by viewModel.spaces.collectAsStateWithLifecycle(emptyList())
    val categoriesList by viewModel.categories.collectAsStateWithLifecycle(emptyList())

    // Form Field variables
    var name by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    var tagsInput by remember { mutableStateOf("") }
    var isFavorite by remember { mutableStateOf(false) }

    var selectedSpaceId by remember { mutableStateOf(initialSpaceId ?: 0L) }
    var selectedCategoryId by remember { mutableStateOf(0L) }
    var photoUri by remember { mutableStateOf<Uri?>(null) }

    // Space / Category spinner triggers
    var showSpacesDrop by remember { mutableStateOf(false) }
    var showCategoriesDrop by remember { mutableStateOf(false) }

    // On-Device gallery selector
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            photoUri = uri
        }
    }

    // Load active edit item properties
    LaunchedEffect(itemId) {
        if (itemId != null && itemId != 0L) {
            val itemDetails = viewModel.getItemWithDetails(itemId)
            if (itemDetails != null) {
                val itObj = itemDetails.item
                name = itObj.name
                description = itObj.description
                notes = itObj.notes
                isFavorite = itObj.isFavorite
                selectedSpaceId = itObj.spaceId
                selectedCategoryId = itObj.categoryId
                tagsInput = itemDetails.tags.joinToString(", ") { tag -> tag.name }
            }
        } else {
            // Apply fallback automatic defaults
            if (spacesList.isNotEmpty() && selectedSpaceId == 0L) {
                selectedSpaceId = spacesList.first().spaceId
            }
            if (categoriesList.isNotEmpty()) {
                selectedCategoryId = categoriesList.first().categoryId
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (itemId == null || itemId == 0L) "New physical Item" else "Edit Item") },
                navigationIcon = {
                    IconButton(onClick = onPop) {
                        Icon(imageVector = Icons.AutoMirrored.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Take dynamic photo card action
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(130.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(100.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color.Gray.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        if (photoUri != null) {
                            AsyncImage(model = photoUri, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                        } else {
                            Icon(imageVector = Icons.Default.AddAPhoto, contentDescription = "", tint = HighlightTeal, modifier = Modifier.size(36.dp))
                        }
                    }

                    Column(
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(text = "Belongings Photo", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        Text(text = "Highly recommended for fast search.", fontSize = 11.sp, color = Color.Gray)
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(
                                onClick = { galleryLauncher.launch("image/*") },
                                colors = ButtonDefaults.buttonColors(containerColor = HighlightTeal),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                                modifier = Modifier.height(32.dp)
                            ) {
                                Text("Select Photo", fontSize = 11.sp)
                            }
                            if (photoUri != null) {
                                TextButton(onClick = { photoUri = null }) { Text("Clear", color = MutedRedDanger, fontSize = 11.sp) }
                            }
                        }
                    }
                }
            }

            // Text form fields
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Item Name *") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = HighlightTeal, focusedTextColor = MaterialTheme.colorScheme.onBackground, unfocusedTextColor = MaterialTheme.colorScheme.onBackground),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("item_form_name_input")
            )

            // Space spinner Selector
            Box(modifier = Modifier.fillMaxWidth()) {
                val currentSpace = spacesList.find { it.spaceId == selectedSpaceId }
                OutlinedTextField(
                    value = currentSpace?.name ?: "Select Space *",
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Storage Space Container") },
                    trailingIcon = { Icon(imageVector = Icons.Default.ArrowDropDown, contentDescription = "") },
                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = MaterialTheme.colorScheme.onBackground, unfocusedTextColor = MaterialTheme.colorScheme.onBackground),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showSpacesDrop = true }
                )
                DropdownMenu(
                    expanded = showSpacesDrop,
                    onDismissRequest = { showSpacesDrop = false },
                    modifier = Modifier.fillMaxWidth(0.9f)
                ) {
                    spacesList.forEach { space ->
                        DropdownMenuItem(
                            text = { Text(space.name) },
                            onClick = {
                                selectedSpaceId = space.spaceId
                                showSpacesDrop = false
                            }
                        )
                    }
                }
            }

            // Category spinner select
            Box(modifier = Modifier.fillMaxWidth()) {
                val currentCategory = categoriesList.find { it.categoryId == selectedCategoryId }
                OutlinedTextField(
                    value = currentCategory?.name ?: "Select Category *",
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Belonging Category") },
                    trailingIcon = { Icon(imageVector = Icons.Default.ArrowDropDown, contentDescription = "") },
                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = MaterialTheme.colorScheme.onBackground, unfocusedTextColor = MaterialTheme.colorScheme.onBackground),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showCategoriesDrop = true }
                )
                DropdownMenu(
                    expanded = showCategoriesDrop,
                    onDismissRequest = { showCategoriesDrop = false },
                    modifier = Modifier.fillMaxWidth(0.9f)
                ) {
                    categoriesList.forEach { cat ->
                        DropdownMenuItem(
                            text = { Text(cat.name) },
                            onClick = {
                                selectedCategoryId = cat.categoryId
                                showCategoriesDrop = false
                            }
                        )
                    }
                }
            }

            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text("Description") },
                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = HighlightTeal, focusedTextColor = MaterialTheme.colorScheme.onBackground, unfocusedTextColor = MaterialTheme.colorScheme.onBackground),
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = notes,
                onValueChange = { notes = it },
                label = { Text("Retrieval Notes") },
                placeholder = { Text("e.g., Hidden inside the red pocket envelope.") },
                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = HighlightTeal, focusedTextColor = MaterialTheme.colorScheme.onBackground, unfocusedTextColor = MaterialTheme.colorScheme.onBackground),
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = tagsInput,
                onValueChange = { tagsInput = it },
                label = { Text("Labels / Tags (comma separated)") },
                placeholder = { Text("travel, urgent, personal, box_a") },
                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = HighlightTeal, focusedTextColor = MaterialTheme.colorScheme.onBackground, unfocusedTextColor = MaterialTheme.colorScheme.onBackground),
                modifier = Modifier.fillMaxWidth()
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Checkbox(
                    checked = isFavorite,
                    onCheckedChange = { isFavorite = it },
                    colors = CheckboxDefaults.colors(checkedColor = HighlightTeal)
                )
                Text(text = "Pin to Home Favorites")
            }

            Spacer(modifier = Modifier.height(10.dp))

            Button(
                onClick = {
                    if (name.isBlank()) {
                        Toast.makeText(context, "Item name is required", Toast.LENGTH_SHORT).show()
                        return@Button
                    }
                    if (selectedSpaceId == 0L) {
                        Toast.makeText(context, "Please select or create a Space container first", Toast.LENGTH_SHORT).show()
                        return@Button
                    }

                    val tagList = tagsInput.split(",")
                        .map { it.trim() }
                        .filter { it.isNotEmpty() }

                    viewModel.saveItem(
                        itemId = itemId ?: 0L,
                        name = name.trim(),
                        description = description.trim(),
                        spaceId = selectedSpaceId,
                        categoryId = selectedCategoryId,
                        notes = notes.trim(),
                        photoUri = photoUri,
                        tagList = tagList,
                        isFavorite = isFavorite
                    ) {
                        Toast.makeText(context, "Item Saved", Toast.LENGTH_SHORT).show() // SUCCESS FEEDBACK
                        onPop()
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = HighlightTeal),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("submit_item_form_btn")
            ) {
                Text("Save Belonging")
            }
        }
    }
}

// 12. --- SUBSCREEN: ADD / EDIT SPACE SCREEN ---
@Composable
fun AddEditSpaceScreen(
    spaceId: Long?,
    parentSpaceId: Long?,
    viewModel: KeepsyViewModel,
    onPop: () -> Unit
) {
    val context = LocalContext.current
    val spacesList by viewModel.spaces.collectAsStateWithLifecycle(emptyList())

    var name by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var icon by remember { mutableStateOf("home") }
    var isFavorite by remember { mutableStateOf(false) }
    var selectedParentId by remember { mutableStateOf(parentSpaceId) }
    var photoUri by remember { mutableStateOf<Uri?>(null) }

    var showParentDrop by remember { mutableStateOf(false) }

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            photoUri = uri
        }
    }

    LaunchedEffect(spaceId) {
        if (spaceId != null && spaceId != 0L) {
            val space = viewModel.spaces.first().find { it.spaceId == spaceId }
            if (space != null) {
                name = space.name
                description = space.description
                icon = space.icon ?: "home"
                isFavorite = space.isFavorite
                selectedParentId = space.parentSpaceId
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (spaceId == null || spaceId == 0L) "New Space container" else "Edit Space") },
                navigationIcon = {
                    IconButton(onClick = onPop) {
                        Icon(imageVector = Icons.AutoMirrored.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(130.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(100.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color.Gray.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        if (photoUri != null) {
                            AsyncImage(model = photoUri, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                        } else {
                            Icon(imageVector = Icons.Default.AddAPhoto, contentDescription = "", tint = HighlightTeal, modifier = Modifier.size(36.dp))
                        }
                    }

                    Column(verticalArrangement = Arrangement.Center) {
                        Text(text = "Space Container Photo", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        Text(text = "Helps visually map drawers / shelves.", fontSize = 11.sp, color = Color.Gray)
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(
                                onClick = { galleryLauncher.launch("image/*") },
                                colors = ButtonDefaults.buttonColors(containerColor = HighlightTeal),
                                modifier = Modifier.height(32.dp)
                            ) {
                                Text("Select Photo", fontSize = 11.sp)
                            }
                            if (photoUri != null) {
                                TextButton(onClick = { photoUri = null }) { Text("Clear", color = MutedRedDanger, fontSize = 11.sp) }
                            }
                        }
                    }
                }
            }

            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Space Container Name *") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = HighlightTeal, focusedTextColor = MaterialTheme.colorScheme.onBackground, unfocusedTextColor = MaterialTheme.colorScheme.onBackground),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("space_form_name_input")
            )

            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text("Description") },
                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = HighlightTeal, focusedTextColor = MaterialTheme.colorScheme.onBackground, unfocusedTextColor = MaterialTheme.colorScheme.onBackground),
                modifier = Modifier.fillMaxWidth()
            )

            // Nested Space: Parent Space Spinner selector
            Box(modifier = Modifier.fillMaxWidth()) {
                val availableSpaces = spacesList.filter { it.spaceId != spaceId } // Prevent circular parent mapping!
                val parentSpace = availableSpaces.find { it.spaceId == selectedParentId }
                OutlinedTextField(
                    value = parentSpace?.name ?: "No Parent (Root space location)",
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Parent Space Container (Nesting)") },
                    trailingIcon = { Icon(imageVector = Icons.Default.ArrowDropDown, contentDescription = "") },
                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = MaterialTheme.colorScheme.onBackground, unfocusedTextColor = MaterialTheme.colorScheme.onBackground),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showParentDrop = true }
                )
                DropdownMenu(
                    expanded = showParentDrop,
                    onDismissRequest = { showParentDrop = false },
                    modifier = Modifier.fillMaxWidth(0.9f)
                ) {
                    DropdownMenuItem(
                        text = { Text("No Parent (Root level)") },
                        onClick = {
                            selectedParentId = null
                            showParentDrop = false
                        }
                    )
                    availableSpaces.forEach { space ->
                        DropdownMenuItem(
                            text = { Text(space.name) },
                            onClick = {
                                selectedParentId = space.spaceId
                                showParentDrop = false
                            }
                        )
                    }
                }
            }

            // Visual Icons picker selector representing real physical boxes
            Text(text = "Visual Representation Icon", fontWeight = FontWeight.Bold, fontSize = 14.sp)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf(
                    Pair("home", Icons.Default.Home),
                    Pair("bedroom", Icons.Default.Bed),
                    Pair("kitchen", Icons.Default.Kitchen),
                    Pair("cabinet", Icons.Default.Inventory),
                    Pair("backpack", Icons.Default.Work),
                    Pair("lock", Icons.Default.Lock)
                ).forEach { (iconId, iconVec) ->
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (icon == iconId) HighlightTeal else Color.Gray.copy(alpha = 0.15f))
                            .clickable { icon = iconId }
                            .padding(8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(imageVector = iconVec, contentDescription = null, tint = if (icon == iconId) Color.White else HighlightTeal)
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Checkbox(
                    checked = isFavorite,
                    onCheckedChange = { isFavorite = it },
                    colors = CheckboxDefaults.colors(checkedColor = HighlightTeal)
                )
                Text(text = "Pin Container to Favorites")
            }

            Spacer(modifier = Modifier.height(10.dp))

            Button(
                onClick = {
                    if (name.isBlank()) {
                        Toast.makeText(context, "Space name is required", Toast.LENGTH_SHORT).show()
                        return@Button
                    }
                    viewModel.saveSpace(
                        spaceId = spaceId ?: 0L,
                        name = name.trim(),
                        description = description.trim(),
                        parentSpaceId = selectedParentId,
                        icon = icon,
                        photoUri = photoUri,
                        isFavorite = isFavorite
                    ) {
                        Toast.makeText(context, "Space Saved", Toast.LENGTH_SHORT).show() // SUCCESS FEEDBACK
                        onPop()
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = HighlightTeal),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("submit_space_form_btn")
            ) {
                Text("Save Storage Space")
            }
        }
    }
}

// 13. --- SUBSCREEN: MOVE ITEM SELECTOR SCREEN ---
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
                            Toast.makeText(context, "Item Moved", Toast.LENGTH_SHORT).show() // SUCCESS FEEDBACK (SUCCESS ACTION TYPE)
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

// 14. --- SUBSCREEN: TRASH BIN / SOFT DELETION VIEW ---
@Composable
fun TrashBinScreen(
    viewModel: KeepsyViewModel,
    onPop: () -> Unit
) {
    val trashList by viewModel.trashItems.collectAsStateWithLifecycle(emptyList())
    val context = LocalContext.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Trash Bin") },
                navigationIcon = {
                    IconButton(onClick = onPop) {
                        Icon(imageVector = Icons.AutoMirrored.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
                .padding(16.dp)
        ) {
            Text(text = "Soft Delete System", fontWeight = FontWeight.Bold, fontSize = 18.sp)
            Text(text = "Belongings here are stored for up to 30 days before permanent automatic eviction. You can restore or wipe them manually.", fontSize = 12.sp, color = Color.Gray)
            
            Spacer(modifier = Modifier.height(16.dp))

            if (trashList.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(imageVector = Icons.Default.DeleteOutline, contentDescription = null, tint = Color.LightGray, modifier = Modifier.size(72.dp))
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(text = "Trash Bin is empty.", color = Color.Gray)
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(trashList) { details ->
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(text = details.item.name, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                    Text(text = "Belonged to Space: ${details.space?.name ?: "Unknown"}", fontSize = 11.sp, color = Color.Gray)
                                }
                                
                                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    IconButton(
                                        onClick = {
                                            viewModel.restoreItem(details.item.itemId)
                                            Toast.makeText(context, "Item Restored", Toast.LENGTH_SHORT).show()
                                        }
                                    ) {
                                        Icon(imageVector = Icons.Default.SettingsBackupRestore, contentDescription = "Restore", tint = HighlightTeal)
                                    }

                                    IconButton(
                                        onClick = {
                                            viewModel.permanentlyDeleteItem(details.item.itemId)
                                            Toast.makeText(context, "Item permanently deleted.", Toast.LENGTH_SHORT).show()
                                        }
                                    ) {
                                        Icon(imageVector = Icons.Default.DeleteForever, contentDescription = "Delete Forever", tint = MutedRedDanger)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
