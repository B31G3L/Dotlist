package com.beigel.list2share.notifications

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.beigel.list2share.MainActivity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Verbindet angetippte Systembenachrichtigungen mit der Navigation.
 *
 * Die MainActivity läuft als `singleTop`, ein Tap landet also je nach Zustand
 * in `onCreate` oder `onNewIntent`. Beide reichen das Intent hier herein;
 * MainScreen beobachtet [pending] und springt in die passende Liste, sobald
 * diese geladen ist. Danach wird das Ziel per [consume] entwertet, damit eine
 * Bildschirmdrehung nicht erneut dorthin springt.
 */
object NotificationRoute {

    const val EXTRA_LIST_ID = "com.beigel.list2share.extra.LIST_ID"
    const val EXTRA_TODO_ID = "com.beigel.list2share.extra.TODO_ID"

    /** Ziel einer angetippten Benachrichtigung. */
    data class Target(val listId: String, val todoId: String?)

    private val _pending = MutableStateFlow<Target?>(null)
    val pending: StateFlow<Target?> = _pending.asStateFlow()

    /** Aus einem Start-Intent ein Navigationsziel ableiten, falls eines enthalten ist. */
    fun submit(intent: Intent?) {
        val listId = intent?.getStringExtra(EXTRA_LIST_ID)?.takeIf { it.isNotBlank() } ?: return
        val todoId = intent.getStringExtra(EXTRA_TODO_ID)?.takeIf { it.isNotBlank() }
        _pending.value = Target(listId, todoId)
    }

    fun consume() {
        _pending.value = null
    }

    /**
     * PendingIntent, der die App öffnet und zur betroffenen Liste navigiert.
     *
     * `FLAG_IMMUTABLE` ist ab Android 12 Pflicht; `FLAG_UPDATE_CURRENT` sorgt
     * dafür, dass eine erneute Benachrichtigung zur selben Liste die Extras
     * aktualisiert statt die alten weiterzuverwenden.
     */
    fun openIntent(context: Context, listId: String, todoId: String? = null): PendingIntent {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(EXTRA_LIST_ID, listId)
            todoId?.let { putExtra(EXTRA_TODO_ID, it) }
        }
        return PendingIntent.getActivity(
            context,
            listId.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }
}
