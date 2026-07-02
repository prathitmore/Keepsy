package com.keepsy.app.monetization

/**
 * FeatureManager handles the definition and status of individual feature flags.
 */
class FeatureManager {

    enum class Feature {
        RemoveAds,
        CloudBackup,
        FamilySharing,
        FutureAI,
        ExportPDF,
        PrioritySupport,
        PremiumThemes
    }

    /**
     * Currently returns default values. 
     * Future implementation will integrate with Remote Config or EntitlementManager.
     */
    fun isFeatureEnabled(feature: Feature, isPremium: Boolean): Boolean {
        return when (feature) {
            Feature.RemoveAds -> isPremium
            Feature.CloudBackup -> isPremium
            Feature.FamilySharing -> isPremium
            Feature.FutureAI -> isPremium
            Feature.ExportPDF -> isPremium
            Feature.PrioritySupport -> isPremium
            Feature.PremiumThemes -> isPremium
        }
    }
}
