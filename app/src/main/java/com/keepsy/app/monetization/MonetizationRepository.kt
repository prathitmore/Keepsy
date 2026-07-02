package com.keepsy.app.monetization

import kotlinx.coroutines.flow.StateFlow

/**
 * MonetizationRepository coordinates all monetization-related components.
 * It serves as the single point of entry for the UI layer (ViewModels).
 */
class MonetizationRepository(
    val premiumManager: PremiumManager,
    val subscriptionManager: SubscriptionManager,
    val adManager: AdManager,
    val featureManager: FeatureManager
) {

    /**
     * Check if user is premium.
     */
    fun isPremium(): Boolean = premiumManager.isPremium()

    /**
     * Observe the user's current subscription plan.
     */
    val currentPlan: StateFlow<SubscriptionManager.Plan> = premiumManager.currentPlan

    /**
     * Check if ads should be displayed.
     */
    fun canShowAds(): Boolean = premiumManager.canShowAds()

    /**
     * Check if a specific feature is available.
     */
    fun hasFeature(feature: FeatureManager.Feature): Boolean = premiumManager.hasFeature(feature)

    /**
     * Purchase a premium plan.
     */
    fun purchasePlan(plan: SubscriptionManager.Plan) {
        subscriptionManager.purchase(plan)
    }

    /**
     * Restore previous purchases.
     */
    fun restorePurchases() {
        subscriptionManager.restorePurchases()
    }

    /**
     * Global initialization of monetization services.
     */
    fun initialize() {
        adManager.initialize()
    }

    /**
     * Utility to show an interstitial ad if allowed.
     */
    fun showInterstitialIfAllowed(adUnitId: String) {
        if (canShowAds()) {
            adManager.showInterstitial(adUnitId)
        }
    }
}
