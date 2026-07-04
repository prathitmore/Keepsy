package com.keepsy.app.ui.tutorial

import androidx.compose.ui.geometry.Rect

enum class TutorialStep(
    val title: String,
    val description: String,
    val spotlightKey: String? = null,
    val isInteractive: Boolean = false
) {
    WELCOME(
        "Welcome to Keepsy",
        "Let's learn how to organize your world and never lose anything again."
    ),
    INTERFACE_OVERVIEW(
        "The Dashboard",
        "This is your command center. You can see your recent items and total inventory at a glance.",
        "home_tab"
    ),
    SPACE_INTRO(
        "Step 1: Create a Space",
        "Tap the '+' button to create your first space, like 'Home'. Spaces are physical locations.",
        "add_space_fab"
    ),
    SUBSPACE_INTRO(
        "Step 2: Add a Subspace",
        "Inside 'Home', you can add 'Bedroom'. Nesting helps you know exactly where things are.",
        "add_subspace_btn"
    ),
    ITEM_INTRO(
        "Step 3: Save an Item",
        "Now add an item like 'Car Keys' into your 'Bedroom'. Add a photo to remember it visually!",
        "add_item_fab"
    ),
    RETRIEVAL_INTRO(
        "Find Anything Instantly",
        "When you need your 'Car Keys', just search for them. Keepsy will lead you straight to the Bedroom.",
        "search_bar"
    ),
    COMPLETION(
        "Launch Ready!",
        "You're all set to organize your life. Start tracking your belongings now!"
    )
}

data class SpotlightInfo(
    val rect: Rect,
    val key: String
)
