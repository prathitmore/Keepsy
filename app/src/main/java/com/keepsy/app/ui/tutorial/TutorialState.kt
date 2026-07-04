package com.keepsy.app.ui.tutorial

enum class TutorialStep(
    val title: String,
    val description: String,
    val spotlightKey: String? = null,
    val advanceOnTap: Boolean = false // If true, tapping the spotlight advances. If false, wait for a specific action (like saving).
) {
    WELCOME(
        "Welcome to Keepsy",
        "Let's learn how to organize your world and never lose anything again."
    ),
    INTERFACE_OVERVIEW(
        "The Dashboard",
        "This is your command center. Tap the Home icon below to start.",
        "home_tab",
        true
    ),
    SPACE_INTRO(
        "Step 1: Create a Space",
        "Tap the '+' button to create your first space, like 'Home'.",
        "add_space_fab"
    ),
    SUBSPACE_INTRO(
        "Step 2: Add a Subspace",
        "Tap 'Add Subspace' to add a 'Bedroom' inside your Home.",
        "add_subspace_btn"
    ),
    ITEM_INTRO(
        "Step 3: Save an Item",
        "Tap the '+' button to save 'Car Keys' into your 'Bedroom'.",
        "add_item_fab"
    ),
    RETRIEVAL_INTRO(
        "Find Anything Instantly",
        "Tap the Search bar to see how retrieval works.",
        "search_bar",
        true
    ),
    COMPLETION(
        "Launch Ready!",
        "You're all set to organize your life. Start tracking your belongings now!"
    )
}
