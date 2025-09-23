package de.beigel.list.onboarding

import android.content.Context
import android.content.SharedPreferences

class OnboardingManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("onboarding_prefs", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_ONBOARDING_COMPLETED = "onboarding_completed"
        private const val KEY_APP_VERSION = "app_version"
        private const val CURRENT_VERSION = "1.0.0"
    }

    var isOnboardingCompleted: Boolean
        get() {
            val completed = prefs.getBoolean(KEY_ONBOARDING_COMPLETED, false)
            val savedVersion = prefs.getString(KEY_APP_VERSION, "")

            // Reset onboarding for new app versions if needed
            return if (savedVersion != CURRENT_VERSION) {
                false // Zeige Onboarding bei neuer Version
            } else {
                completed
            }
        }
        set(value) {
            prefs.edit()
                .putBoolean(KEY_ONBOARDING_COMPLETED, value)
                .putString(KEY_APP_VERSION, CURRENT_VERSION)
                .apply()
        }

    fun completeOnboarding() {
        isOnboardingCompleted = true
    }

    fun resetOnboarding() {
        prefs.edit()
            .putBoolean(KEY_ONBOARDING_COMPLETED, false)
            .remove(KEY_APP_VERSION)
            .apply()
    }

    // Optional: Show onboarding again for major feature updates
    fun shouldShowOnboardingForUpdate(): Boolean {
        val savedVersion = prefs.getString(KEY_APP_VERSION, "")
        return savedVersion != CURRENT_VERSION
    }
}