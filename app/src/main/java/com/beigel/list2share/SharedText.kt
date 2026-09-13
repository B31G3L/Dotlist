package com.beigel.list2share

import android.content.Intent
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Text, der von außen an die App geteilt wurde.
 *
 * Aufgebaut wie [com.beigel.list2share.notifications.NotificationRoute]: der
 * Intent kommt in der Activity an, die Oberfläche holt ihn sich hier ab, sobald
 * die Listen geladen sind, und quittiert mit [consume].
 *
 * Als Titel dient der geteilte Text, gekürzt auf [MAX_LENGTH] – aus einem
 * Browser kommt oft ein ganzer Absatz plus URL, und eine Aufgabe mit 800
 * Zeichen Titel ist unbrauchbar. Der volle Text landet in der Beschreibung.
 */
object SharedText {

    /** Titel-Obergrenze; darüber wird am letzten Wort vor der Grenze getrennt. */
    private const val MAX_LENGTH = 80

    data class Pending(val title: String, val description: String)

    private val _pending = MutableStateFlow<Pending?>(null)
    val pending: StateFlow<Pending?> = _pending.asStateFlow()

    /** Aus einem Start-Intent geteilten Text ableiten, falls einer enthalten ist. */
    fun submit(intent: Intent?) {
        if (intent?.action != Intent.ACTION_SEND) return
        if (intent.type != "text/plain") return

        val raw = intent.getStringExtra(Intent.EXTRA_TEXT)?.trim()?.takeIf { it.isNotEmpty() } ?: return
        // Manche Apps schicken einen Betreff mit, der oft der bessere Titel ist.
        val subject = intent.getStringExtra(Intent.EXTRA_SUBJECT)?.trim()?.takeIf { it.isNotEmpty() }

        val title = shorten(subject ?: raw)
        // Beschreibung nur, wenn sie mehr sagt als der Titel.
        val description = if (raw == title) "" else raw

        _pending.value = Pending(title, description)
    }

    fun consume() {
        _pending.value = null
    }

    /** Auf [MAX_LENGTH] kürzen, möglichst an einer Wortgrenze. */
    internal fun shorten(text: String): String {
        val single = text.replace(Regex("\\s+"), " ").trim()
        if (single.length <= MAX_LENGTH) return single

        val cut = single.take(MAX_LENGTH)
        val lastSpace = cut.lastIndexOf(' ')
        // Nur an der Wortgrenze trennen, wenn dabei noch genug übrig bleibt –
        // sonst stünde bei einer langen URL ohne Leerzeichen fast nichts da.
        return if (lastSpace > MAX_LENGTH / 2) cut.take(lastSpace) + "…" else cut + "…"
    }
}
