package com.beigel.list2share.data

import android.text.format.DateUtils
import java.util.Date

/**
 * Verlauf einer Liste.
 *
 * Bewusst abgeleitet aus `doneBy` und `doneAt` der Todos statt aus einer
 * eigenen Collection: das kostet keine zusätzlichen Schreibvorgänge und
 * braucht keine Aufräumregel. Der Preis dafür ist, dass nur das Abhaken
 * auftaucht und immer nur der letzte Stand – wer abhakt und wieder aufmacht,
 * verschwindet aus dem Verlauf.
 *
 * Für echten Verlauf („Tom hat Brot hinzugefügt", „Jana hat den Preis
 * geändert") bräuchte es eine Subcollection und eine Cloud Function, die sie
 * füllt. Solange die einfache Variante reicht, lohnt das nicht.
 */
data class ActivityEntry(
    val todoId: String,
    val title: String,
    /** Anzeigename; leer, wenn die Person nicht mehr in der Liste steht. */
    val who: String,
    val at: Date,
)

/** Wie viele Einträge der Verlauf höchstens zeigt. */
private const val LIMIT = 20

fun activityOf(list: TodoList, todos: List<TodoItem>): List<ActivityEntry> =
    todos.asSequence()
        .filter { it.isDone && it.doneAt != null }
        .map { todo ->
            ActivityEntry(
                todoId = todo.id,
                title = todo.title,
                who = todo.doneBy?.let { list.memberNames[it] } ?: "",
                at = todo.doneAt!!.toDate(),
            )
        }
        .sortedByDescending { it.at.time }
        .take(LIMIT)
        .toList()

/**
 * „vor 5 Minuten", „gestern", „12.09.".
 *
 * DateUtils übernimmt Sprache und Einheit; ab einer Woche liefert es von
 * selbst ein Datum statt einer immer unhandlicheren Anzahl von Tagen.
 */
fun formatWhen(at: Date, now: Long = System.currentTimeMillis()): String =
    DateUtils.getRelativeTimeSpanString(
        at.time,
        now,
        DateUtils.MINUTE_IN_MILLIS,
        DateUtils.FORMAT_ABBREV_RELATIVE,
    ).toString()
