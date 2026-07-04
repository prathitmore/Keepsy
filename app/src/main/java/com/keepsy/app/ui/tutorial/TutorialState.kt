package com.keepsy.app.ui.tutorial

enum class TutorialStep(
    val title: String,
    val description: String,
    val spotlightKey: String? = null
) {
    WELCOME(
        "Welcome to Keepsy",
        "Let's learn how to organize your world in 3 simple steps."
    ),
    INTERFACE_OVERVIEW(
        "The Dashboard",
        "This is your home base. You can see your recent items and total inventory at a glance.",
        "home_tab"
    ),
    SPACE_INTRO(
        "Step 1: Create a Space",
        "Spaces are the places where you keep things. Tap the '+' button in the Spaces tab to create your first space, like 'Home'.",
        "add_space_fab"
    ),
    SUBSPACE_INTRO(
        "Step 2: Add a Subspace",
        "Inside 'Home', you can add a 'Bedroom'. Nesting spaces helps you stay precise about where things are.",
        "add_subspace_btn"
    ),
    ITEM_INTRO(
        "Step 3: Save an Item",
        "Now, let's save your first item, like 'Car Keys', into your 'Bedroom'.",
        "add_item_fab"
    ),
    RETRIEVAL_INTRO(
        "Instant Search",
        "When you need something, just search for it! Keepsy will show you the exact spot instantly.",
        "search_bar"
    ),
    COMPLETION(
        "You're Ready!",
        "You've mastered the fundamentals. Welcome to an organized life!"
    )
}
