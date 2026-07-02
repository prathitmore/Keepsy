package com.keepsy.app.monetization

import kotlinx.coroutines.flow.StateFlow

/**
 * PremiumManager is the primary authority for checking premium status across the app.
 */
class PremiumManager(
    private val subscriptionManager: SubscriptionManager,
    private val entitlementManager: EntitlementManager
) {

    /**
     * Observable plan status.
     */
    val currentPlan: StateFlow<SubscriptionManager.Plan> = subscriptionManager.currentPlan

    /**
     * Immediate check for premium status.
     */
    fun isPremium(): Boolean {
        return subscriptionManager.isSubscribed()
    }

    /**
     * Determines if ads can be shown to the current user.
     */
    fun canShowAds(): Boolean {
        return entitlementManager.canShowAds()
    }

    /**
     * Determines if a specific premium feature is accessible.
     */
    fun hasFeature(feature: FeatureManager.Feature): Boolean {
        return entitlementManager.hasEntitlement(feature)
    }
}
