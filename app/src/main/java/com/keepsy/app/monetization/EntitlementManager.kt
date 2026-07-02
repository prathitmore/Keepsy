package com.keepsy.app.monetization

/**
 * EntitlementManager determines what specific capabilities are granted based on the user's status.
 */
class EntitlementManager(
    private val subscriptionManager: SubscriptionManager,
    private val featureManager: FeatureManager
) {

    /**
     * Determines if a specific feature is available.
     */
    fun hasEntitlement(feature: FeatureManager.Feature): Boolean {
        val isPremium = subscriptionManager.isSubscribed()
        return featureManager.isFeatureEnabled(feature, isPremium)
    }

    /**
     * Logic to determine if ads should be displayed.
     */
    fun canShowAds(): Boolean {
        return !subscriptionManager.isSubscribed()
    }
}
