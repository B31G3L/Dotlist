package de.beigel.list.data

import com.google.firebase.Timestamp
import com.google.firebase.firestore.PropertyName

/**
 * Eine geteilte Todo-Liste.
 *
 * @param id            Firestore-Dokument-ID
 * @param name          Anzeigename der Liste
 * @param memberIds     Liste aller Geräte-IDs die Zugriff haben
 * @param createdBy     Geräte-ID des Erstellers
 * @param createdAt     Erstellungszeitpunkt
 * @param color         Farbe der Liste (Hex-String, z.B. "#FF5733")
 */
data class TodoList(
    val id: String = "",
    val name: String = "",
    val memberIds: List<String> = emptyList(),
    val createdBy: String = "",
    val createdAt: Timestamp = Timestamp.now(),
    val color: String = "#6750A4"
) {
    // Parameterloser Konstruktor für Firestore-Deserialisierung
    constructor() : this("", "", emptyList(), "", Timestamp.now(), "#6750A4")
}

/**
 * Ein einzelnes Todo-Element innerhalb einer Liste.
 *
 * @param id            Firestore-Dokument-ID
 * @param title         Text des Todos
 * @param isDone        Ob das Todo erledigt ist
 * @param createdBy     Geräte-ID des Erstellers
 * @param createdAt     Erstellungszeitpunkt
 * @param doneBy        Geräte-ID, wer es erledigt hat (null = noch offen)
 * @param doneAt        Zeitpunkt der Erledigung
 * @param position      Sortierreihenfolge
 */
data class TodoItem(
    val id: String = "",
    val title: String = "",
    @get:PropertyName("isDone") @set:PropertyName("isDone")
    var isDone: Boolean = false,
    val createdBy: String = "",
    val createdAt: Timestamp = Timestamp.now(),
    val doneBy: String? = null,
    val doneAt: Timestamp? = null,
    val position: Long = 0L
) {
    constructor() : this("", "", false, "", Timestamp.now(), null, null, 0L)
}

/**
 * Erledigt-/Gesamtanzahl der Todos einer Liste (für die Listen-Übersicht).
 */
data class ListCounts(val done: Int = 0, val total: Int = 0) {
    val fraction: Float get() = if (total == 0) 0f else done.toFloat() / total.toFloat()
}