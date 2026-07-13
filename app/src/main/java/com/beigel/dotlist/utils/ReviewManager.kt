package com.beigel.dotlist.utils

import android.app.Activity
import android.content.Context
import android.content.SharedPreferences
import com.google.android.play.core.review.ReviewManagerFactory

/**
 * Steuert, wann die native Play-In-App-Review angefragt wird.
 * Fragt max. 2x an, jeweils an sinnvollen App-Start-Marken (5. und 20. Start),
 * damit Nutzer:innen die App schon etwas kennen, bevor sie um eine Bewertung
 * gebeten werden.
 */
object ReviewManager {

    private const val PREFS_NAME = "review_prefs"
    private const val KEY_APP_OPENS = "app_opens"
    private const val KEY_REVIEW_SHOWN_COUNT = "review_shown_count"

    private val TRIGGER_POINTS = setOf(5, 20)
    private const val MAX_REVIEW_PROMPTS = 2

    /**
     * Bei jedem App-Start aufrufen (z. B. in MainActivity).
     * Zeigt den nativen Review-Dialog, wenn der aktuelle Öffnungs-Zähler
     * einem Trigger-Punkt entspricht und die max. Anzahl noch nicht erreicht ist.
     */
    fun maybeRequestReview(activity: Activity) {
        val prefs = prefs(activity)
        val opens = prefs.getInt(KEY_APP_OPENS, 0) + 1
        val reviewsShown = prefs.getInt(KEY_REVIEW_SHOWN_COUNT, 0)

        prefs.edit().putInt(KEY_APP_OPENS, opens).apply()

        val shouldShow = opens in TRIGGER_POINTS && reviewsShown < MAX_REVIEW_PROMPTS
        if (!shouldShow) return

        val reviewManager = ReviewManagerFactory.create(activity)
        val request = reviewManager.requestReviewFlow()
        request.addOnCompleteListener { task ->
            if (task.isSuccessful) {
                // Google entscheidet selbst, ob der Dialog tatsächlich angezeigt wird
                // (z. B. Tageslimit von Google), daher zählen wir den Versuch trotzdem.
                reviewManager.launchReviewFlow(activity, task.result)
            }
            prefs.edit().putInt(KEY_REVIEW_SHOWN_COUNT, reviewsShown + 1).apply()
        }
    }

    private fun prefs(context: Context): SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
}