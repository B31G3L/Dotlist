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
    val memberNames: Map<String, String> = emptyMap(),
    val createdBy: String = "",
    val createdAt: Timestamp = Timestamp.now(),
    val color: String = "#6750A4"
) {
    // Parameterloser Konstruktor für Firestore-Deserialisierung
    constructor() : this("", "", emptyList(), emptyMap(), "", Timestamp.now(), "#6750A4")
}

/**
 * Anzeigename für ein Mitglied einer Liste. Fällt auf eine Kurzform der
 * Geräte-ID zurück, falls (noch) kein Name hinterlegt ist (z.B. bei alten Listen).
 */
fun TodoList.displayNameFor(memberId: String): String =
    memberNames[memberId] ?: "Mitglied ${memberId.take(4).uppercase()}"

/**
 * Priorität eines Todos.
 */
enum class Priority(val label: String) {
    HOCH("Hoch"),
    MITTEL("Mittel"),
    NIEDRIG("Niedrig");

    companion object {
        fun fromString(value: String?): Priority =
            entries.firstOrNull { it.name == value } ?: MITTEL
    }
}

/**
 * Ein einzelnes Todo-Element innerhalb einer Liste.
 *
 * @param id                Firestore-Dokument-ID
 * @param title             Text des Todos
 * @param description       Optionale Beschreibung
 * @param isDone            Ob das Todo erledigt ist
 * @param priority          Priorität ("HOCH" / "MITTEL" / "NIEDRIG")
 * @param dueDate           Fälligkeitsdatum inkl. Uhrzeit (optional)
 * @param assignedTo        Geräte-ID der zugewiesenen Person (optional)
 * @param reminderMinutes   Erinnerung X Minuten vor Fälligkeit (optional)
 * @param createdBy         Geräte-ID des Erstellers
 * @param createdAt         Erstellungszeitpunkt
 * @param doneBy            Geräte-ID, wer es erledigt hat (null = noch offen)
 * @param doneAt            Zeitpunkt der Erledigung
 * @param position          Sortierreihenfolge
 */
data class TodoItem(
    val id: String = "",
    val title: String = "",
    val description: String = "",
    @get:PropertyName("isDone") @set:PropertyName("isDone")
    var isDone: Boolean = false,
    val priority: String = Priority.MITTEL.name,
    val dueDate: Timestamp? = null,
    val assignedTo: String? = null,
    val reminderMinutes: Int? = null,
    val createdBy: String = "",
    val createdAt: Timestamp = Timestamp.now(),
    val doneBy: String? = null,
    val doneAt: Timestamp? = null,
    val position: Long = 0L
) {
    constructor() : this(
        "", "", "", false, Priority.MITTEL.name, null, null, null,
        "", Timestamp.now(), null, null, 0L
    )
}

/**
 * Erledigt-/Gesamtanzahl der Todos einer Liste (für die Listen-Übersicht).
 */
data class ListCounts(val done: Int = 0, val total: Int = 0) {
    val fraction: Float get() = if (total == 0) 0f else done.toFloat() / total.toFloat()
}