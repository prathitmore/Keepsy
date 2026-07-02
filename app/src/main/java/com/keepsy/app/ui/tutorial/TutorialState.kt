package com.keepsy.app.ui.tutorial

import androidx.compose.ui.geometry.Rect

enum class TutorialStep(
    val title: String,
    val description: String,
    val spotlightKey: String? = null
) {
    WELCOME(
        "Welcome to Keepsy",
        "Let's quickly show you how Keepsy works. We'll guide you through the core features."
    ),
    SPACES_EXPLAIN(
        "Understanding Spaces",
        "A Space is any place where you keep things. Think of Home, Office, or even a Backpack.",
        "add_space_fab"
    ),
    CREATE_SPACE(
        "Create a Space",
        "Try creating your first space. It could be your 'Main Wardrobe' or 'Kitchen Cabinet'.",
        "add_space_fab"
    ),
    SUBSPACE_EXPLAIN(
        "Nesting with Subspaces",
        "You can add subspaces inside spaces. For example: Home -> Bedroom -> Wardrobe -> Top Shelf.",
        "add_subspace_btn"
    ),
    ITEMS_EXPLAIN(
        "What are Items?",
        "Items represent your belongings. Passport, Charger, Camera Battery, or even spare keys.",
        "add_item_fab"
    ),
    CREATE_ITEM(
        "Add an Item",
        "Try adding an item now. Name it, pick a category, and assign it to the space you just created.",
        "add_item_fab"
    ),
    SEARCH_EXPLAIN(
        "Instant Recall",
        "Search is the fastest way to find anything. Just type a name, category, or tag.",
        "search_bar"
    ),
    ITEM_DETAILS(
        "Memory Details",
        "Open an item to see its exact location, category, history, and any notes or photos.",
        "item_card_0"
    ),
    MOVING_ITEMS(
        "Moving Things",
        "When you move an item, Keepsy remembers. Previous locations are tracked automatically.",
        "move_item_btn"
    ),
    ACTIVITY_EXPLAIN(
        "Memory Trail",
        "The Activity tab shows every movement. You'll never lose track of where things went.",
        "activity_screen"
    ),
    DASHBOARD_EXPLAIN(
        "Your Dashboard",
        "Here you see your recent items, pinned spaces, and a quick summary of your inventory.",
        "stats_area"
    ),
    COMPLETION(
        "You're All Set!",
        "You've mastered the basics. You're now ready to start remembering everything with Keepsy."
    )
}

data class SpotlightInfo(
    val rect: Rect,
    val key: String
)
