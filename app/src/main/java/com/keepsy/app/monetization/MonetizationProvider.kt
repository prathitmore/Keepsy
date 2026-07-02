package com.keepsy.app.monetization

import android.content.Context

/**
 * MonetizationProvider acts as a simple service locator for monetization components.
 * This ensures that managers are singletons within the application scope.
 */
object MonetizationProvider {
    
    private var monetizationRepository: MonetizationRepository? = null

    fun getRepository(context: Context): MonetizationRepository {
        return monetizationRepository ?: synchronized(this) {
            val repo = monetizationRepository ?: createRepository(context.applicationContext)
            monetizationRepository = repo
            repo
        }
    }

    private fun createRepository(context: Context): MonetizationRepository {
        val featureManager = FeatureManager()
        val adManager = AdManager(context)
        val subscriptionManager = SubscriptionManager()
        val entitlementManager = EntitlementManager(subscriptionManager, featureManager)
        val premiumManager = PremiumManager(subscriptionManager, entitlementManager)
        
        return MonetizationRepository(
            premiumManager,
            subscriptionManager,
            adManager,
            featureManager
        )
    }
}
