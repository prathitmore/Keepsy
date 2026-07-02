package com.keepsy.app.navigation

sealed class Screen {
    object Splash : Screen()
    object Auth : Screen()
    data class AuthSuccess(val name: String) : Screen()
    object Onboarding : Screen()
    object Tutorial : Screen()
    object VerifyEmail : Screen()
    object Dashboard : Screen()
}

sealed class AuthSubScreen {
    object Welcome : AuthSubScreen()
    object SignIn : AuthSubScreen()
    object SignUp : AuthSubScreen()
}

sealed class TabScreen {
    object Home : TabScreen()
    object Spaces : TabScreen()
    object Search : TabScreen()
    object Activity : TabScreen()
    object Settings : TabScreen()
}

sealed class SubScreen {
    object None : SubScreen()
    data class ItemDetails(val itemId: Long) : SubScreen()
    data class SpaceDetails(val spaceId: Long) : SubScreen()
    data class AddEditItem(val itemId: Long? = null, val spaceId: Long? = null) : SubScreen()
    data class AddEditSpace(val spaceId: Long? = null, val parentSpaceId: Long? = null) : SubScreen()
    data class MoveItem(val itemId: Long) : SubScreen()
    object TrashBin : SubScreen()
}
