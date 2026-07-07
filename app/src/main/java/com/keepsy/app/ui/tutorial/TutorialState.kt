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
        "This is your command center. Tap the Home icon below to start.",
        "home_tab",
        true
    ),
    SPACE_TAB_INTRO(
        "Inventory Map",
        "Tap the 'Spaces' tab to start building your physical storage structure.",
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
        "Tap the Name field to name your space 'Home', then tap 'Save Space'.",
        "space_form_name_input",
        true
    ),
    SUBSPACE_INTRO(
        "Step 2: Add a Subspace",
        "Now tap 'Add Subspace' to add a 'Bedroom' inside your Home. Nesting helps you stay precise.",
        "add_subspace_btn"
    ),
    ITEM_INTRO(
        "Step 3: Save an Item",
        "Finally, go to the Home tab and tap the '+' button to save 'Car Keys' into your 'Bedroom'.",
        "add_item_fab"
    ),
    ITEM_FORM_DETAILS(
        "Item Details",
        "Name your item and pick the space you just created. Then tap 'Save Item'.",
        "item_form_name_input",
        true
    ),
    RETRIEVAL_INTRO(
        "Find Anything Instantly",
        "Next time you lose your keys, just search for them. Tap the Search bar to see retrieval in action.",
        "search_bar",
        true
    ),
    COMPLETION(
        "Launch Ready!",
        "You're all set to organize your life. Start tracking your belongings now!"
    )
}
