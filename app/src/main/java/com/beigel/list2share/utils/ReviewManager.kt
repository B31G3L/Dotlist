package com.beigel.list2share.utils

import android.app.Activity
import android.content.Context
import android.content.SharedPreferences
import com.google.android.play.core.review.ReviewManagerFactory

/**
 * Steuert, wann die native Play-In-App-Review angefragt wird.
 *
 * Nicht an der Anzahl der App-Starts, sondern an gelungenen Momenten. Wer die
 * App zum fünften Mal öffnet, hatte womöglich gerade Ärger damit – und
 * antwortet entsprechend. Gefragt wird deshalb, wenn sichtbar etwas
 * funktioniert hat:
 *
 *  - eine Liste ist vollständig abgehakt
 *  - jemand ist einer geteilten Liste beigetreten
 *
 * Zusätzlich muss die App eine Weile in Gebrauch sein ([MIN_OPENS]), damit die
 * Frage nicht schon am ersten Tag kommt, und zwischen zwei Anfragen liegt eine
 * Sperrfrist. Höchstens [MAX_REVIEW_PROMPTS] Anfragen insgesamt.
 */
object ReviewManager {

    private const val PREFS_NAME = "review_prefs"
    private const val KEY_APP_OPENS = "app_opens"
    private const val KEY_REVIEW_SHOWN_COUNT = "review_shown_count"
    private const val KEY_LAST_PROMPT_AT = "last_prompt_at"

    /** Vorher kennt niemand die App gut genug für ein Urteil. */
    private const val MIN_OPENS = 4

    private const val MAX_REVIEW_PROMPTS = 2

    /** Abstand zwischen zwei Anfragen. */
    private const val COOLDOWN_MS = 30L * 24 * 60 * 60 * 1000

    /** Beim App-Start aufrufen: zählt nur mit, fragt nichts. */
    fun recordAppOpen(context: Context) {
        val prefs = prefs(context)
        prefs.edit().putInt(KEY_APP_OPENS, prefs.getInt(KEY_APP_OPENS, 0) + 1).apply()
    }

    /**
     * Nach einem gelungenen Moment aufrufen.
     *
     * Ob der Dialog tatsächlich erscheint, entscheidet Google (Tageslimit,
     * Kontingent pro Nutzer). Der Versuch wird trotzdem gezählt – sonst würde
     * die App bei jedem weiteren Erfolg erneut anfragen, obwohl Google gerade
     * nichts anzeigt.
     */
    fun onSuccessMoment(activity: Activity) {
        val prefs = prefs(activity)
        if (!shouldAsk(prefs)) return

        val reviewManager = ReviewManagerFactory.create(activity)
        reviewManager.requestReviewFlow().addOnCompleteListener { task ->
            if (task.isSuccessful) {
                reviewManager.launchReviewFlow(activity, task.result)
            }
            prefs.edit()
                .putInt(KEY_REVIEW_SHOWN_COUNT, prefs.getInt(KEY_REVIEW_SHOWN_COUNT, 0) + 1)
                .putLong(KEY_LAST_PROMPT_AT, System.currentTimeMillis())
                .apply()
        }
    }

    private fun shouldAsk(prefs: SharedPreferences): Boolean {
        if (prefs.getInt(KEY_APP_OPENS, 0) < MIN_OPENS) return false
        if (prefs.getInt(KEY_REVIEW_SHOWN_COUNT, 0) >= MAX_REVIEW_PROMPTS) return false

        val last = prefs.getLong(KEY_LAST_PROMPT_AT, 0L)
        return last == 0L || System.currentTimeMillis() - last > COOLDOWN_MS
    }

    private fun prefs(context: Context): SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
}
