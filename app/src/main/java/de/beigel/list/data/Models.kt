package de.beigel.list.data
import com.google.firebase.Timestamp
import com.google.firebase.firestore.PropertyName

/**
 * Eine geteilte Todo-Liste.
 *
 * @param id            Firestore-Dokument-ID
 * @param name          Anzeigename der Liste
 * @param memberIds     Liste aller Geräte-IDs die Zugriff haben
 * @param adminIds      Geräte-IDs mit Admin-Rechten (Besitzer zählt nicht extra dazu)
 * @param createdBy     Geräte-ID des Erstellers/Besitzers
 * @param createdAt     Erstellungszeitpunkt
 * @param color         Farbe der Liste (Hex-String, z.B. "#FF5733")
 */
data class TodoList(
    val id: String = "",
    val name: String = "",
    val memberIds: List<String> = emptyList(),
    val memberNames: Map<String, String> = emptyMap(),
    val adminIds: List<String> = emptyList(),
    val createdBy: String = "",
    val createdAt: Timestamp = Timestamp.now(),
    val color: String = "#6750A4"
) {
    // Parameterloser Konstruktor für Firestore-Deserialisierung
    constructor() : this("", "", emptyList(), emptyMap(), emptyList(), "", Timestamp.now(), "#6750A4")
}

/**
 * Rolle eines Mitglieds innerhalb einer Liste.
 */
enum class MemberRole(val label: String) {
    BESITZER("Besitzer"),
    ADMIN("Admin"),
    MITGLIED("Bearbeiter")
}

fun TodoList.roleOf(memberId: String): MemberRole = when {
    memberId == createdBy   -> MemberRole.BESITZER
    memberId in adminIds    -> MemberRole.ADMIN
    else                     -> MemberRole.MITGLIED
}

/** Besitzer und Admins dürfen den Einladungscode sehen und Mitglieder verwalten. */
fun TodoList.canManageMembers(memberId: String): Boolean =
    memberId == createdBy || memberId in adminIds

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
 * Eine Unteraufgabe innerhalb eines Todos.
 */
data class Subtask(
    val id: String = "",
    val title: String = "",
    @get:PropertyName("isDone") @set:PropertyName("isDone")
    var isDone: Boolean = false
) {
    constructor() : this("", "", false)
}

/**
 * Ein Kommentar zu einem Todo.
 */
data class Comment(
    val id: String = "",
    val authorId: String = "",
    val text: String = "",
    val createdAt: Timestamp = Timestamp.now()
) {
    constructor() : this("", "", "", Timestamp.now())
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
 * @param subtasks          Liste von Unteraufgaben
 * @param comments          Liste von Kommentaren
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
    val position: Long = 0L,
    val subtasks: List<Subtask> = emptyList(),
    val comments: List<Comment> = emptyList()
) {
    constructor() : this(
        "", "", "", false, Priority.MITTEL.name, null, null, null,
        "", Timestamp.now(), null, null, 0L, emptyList(), emptyList()
    )
}

/**
 * Erledigt-/Gesamtanzahl der Todos einer Liste (für die Listen-Übersicht).
 */
data class ListCounts(val done: Int = 0, val total: Int = 0) {
    val fraction: Float get() = if (total == 0) 0f else done.toFloat() / total.toFloat()
}

/**
 * Art einer Benachrichtigung.
 */
enum class NotificationType {
    ZUGEWIESEN, ERLEDIGT, KOMMENTAR, EINLADUNG
}

/**
 * Eine Benachrichtigung für ein Gerät (z.B. "Jana hat dir eine Aufgabe zugewiesen").
 */
data class AppNotification(
    val id: String = "",
    val recipientId: String = "",
    val actorId: String = "",
    val actorName: String = "",
    val type: String = NotificationType.ZUGEWIESEN.name,
    val todoTitle: String = "",
    val listId: String = "",
    val todoId: String = "",
    @get:PropertyName("isRead") @set:PropertyName("isRead")
    var isRead: Boolean = false,
    val createdAt: Timestamp = Timestamp.now(),
) {
    constructor() : this("", "", "", "", NotificationType.ZUGEWIESEN.name, "", "", "", false, Timestamp.now())
}