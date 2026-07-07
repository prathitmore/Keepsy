package com.keepsy.app.ui.tutorial

enum class TutorialStep(
    val title: String,
    val description: String,
    val spotlightKey: String? = null,
    val advanceOnTap: Boolean = false
) {
    WELCOME(
        "Welcome to Keepsy",
        "Let's learn how to organize your world and never lose anything again."
    ),
    INTERFACE_OVERVIEW(
        "The Dashboard",
        "This is your command center. You can see your recent items and total inventory at a glance.",
        "home_tab",
        true
    ),
    SPACE_TAB_INTRO(
        "Inventory Map",
        "Tap the 'Spaces' tab to see your physical storage structure.",
        "spaces_tab",
        true
    ),
    SPACE_INTRO(
        "Step 1: Create a Space",
        "Spaces are physical locations. Tap the '+' button to create your first space, like 'Home'.",
        "add_space_fab"
    ),
    SPACE_FORM_DETAILS(
        "Describe your Space",
        "Give your space a name like 'Home' or 'Office' and tap 'Save Space'.",
        "submit_space_form_btn"
    ),
    SUBSPACE_INTRO(
        "Step 2: Add a Subspace",
        "Inside 'Home', you can add a 'Bedroom'. Tap 'Add Subspace' to stay precise.",
        "add_subspace_btn"
    ),
    ITEM_INTRO(
        "Step 3: Save an Item",
        "Now, let's save your first item, like 'Car Keys', into your 'Bedroom'.",
        "add_item_fab"
    ),
    ITEM_FORM_DETAILS(
        "Item Details",
        "Name your item and pick the space you just created. Then tap 'Save Item'.",
        "submit_item_form_btn"
    ),
    RETRIEVAL_INTRO(
        "Instant Search",
        "Next time you lose something, just search for it! Keepsy will show you exactly where it is.",
        "search_tab",
        true
    ),
    COMPLETION(
        "You're Ready!",
        "You've mastered the fundamentals of Keepsy. Welcome to an organized life!"
    )
}
