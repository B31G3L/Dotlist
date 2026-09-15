package com.beigel.list2share.data
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.google.firebase.Timestamp
import com.google.firebase.firestore.Exclude
import com.google.firebase.firestore.PropertyName
import com.beigel.list2share.R

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
 * @param icon          Name des gewählten Icons (siehe ALL_LIST_ICONS), leer = alte Positions-Rotation
 * @param mutedBy       Geräte-IDs, die für diese Liste keine Push-Benachrichtigungen wollen
 * @param isShared      True = Liste liegt in Firestore und ist geräteübergreifend teilbar.
 *                      False (Standard) = Liste liegt nur lokal auf diesem Gerät (Room-DB).
 *                      Wird nicht in Firestore gespeichert, sondern beim Laden abgeleitet:
 *                      aus Firestore gelesene Listen sind immer true, aus der lokalen
 *                      DB gelesene Listen immer false.
 */
data class TodoList(
    @get:Exclude val id: String = "",
    val name: String = "",
    val memberIds: List<String> = emptyList(),
    val memberNames: Map<String, String> = emptyMap(),
    val adminIds: List<String> = emptyList(),
    val createdBy: String = "",
    val createdAt: Timestamp = Timestamp.now(),
    val color: String = "#6750A4",
    val icon: String = "",
    val mutedBy: List<String> = emptyList(),
    val mode: String = ListMode.AUFGABEN.name,
    @get:Exclude val isShared: Boolean = false
) {
    // Parameterloser Konstruktor für Firestore-Deserialisierung
    constructor() : this("", "", emptyList(), emptyMap(), emptyList(), "", Timestamp.now(), "#6750A4", "", emptyList(), ListMode.AUFGABEN.name, false)
}

/**
 * Art einer Liste.
 *
 * AUFGABEN ist der Standard und der Rückfall für alles, was das Feld noch
 * nicht kennt – ältere Listen haben es schlicht nicht.
 *
 * EINKAUFEN blendet Priorität, Zuständigkeit, Fälligkeit, Erinnerung und
 * Wiederholung aus und zeigt stattdessen eine Mengenangabe sowie eine
 * Gliederung nach Abteilungen.
 *
 * CHECKLISTE ist für Listen, die immer wieder abgearbeitet werden – Packliste,
 * Putzplan. Ebenfalls ohne Termine und Prioritäten, aber in fester Reihenfolge
 * und mit „Alle zurücksetzen" statt „Erledigte löschen".
 *
 * ANSCHAFFUNG ist für größere Käufe, über die man länger nachdenkt:
 * Waschmaschine, Fahrrad. Behält Priorität und Fälligkeit, ergänzt Preis und
 * Link und zeigt die Summe der offenen Posten.
 *
 * Gesetzte Werte bleiben in jedem Fall im Dokument stehen, ein Umschalten
 * verliert also nichts.
 */
enum class ListMode(val labelRes: Int, val hintRes: Int) {
    AUFGABEN(R.string.list_mode_tasks, R.string.list_mode_hint_tasks),
    EINKAUFEN(R.string.list_mode_shopping, R.string.list_mode_hint_shopping),
    CHECKLISTE(R.string.list_mode_checklist, R.string.list_mode_hint_checklist),
    ANSCHAFFUNG(R.string.list_mode_purchase, R.string.list_mode_hint_purchase),
}

/** Modus einer Liste, unbekannte oder fehlende Werte gelten als AUFGABEN. */
val TodoList.listMode: ListMode
    get() = runCatching { ListMode.valueOf(mode) }.getOrDefault(ListMode.AUFGABEN)

/**
 * Modi ohne Priorität, Zuständigkeit, Termine und Wiederholung.
 *
 * ANSCHAFFUNG gehört ausdrücklich nicht dazu: dort bedeutet die Priorität
 * tatsächlich etwas („brauchen wir bald" gegen „irgendwann mal").
 */
val ListMode.isSimple: Boolean
    get() = this == ListMode.EINKAUFEN || this == ListMode.CHECKLISTE

/**
 * Rolle eines Mitglieds innerhalb einer Liste.
 */
enum class MemberRole(val label: String) {
    BESITZER("Besitzer"),
    ADMIN("Admin"),
    MITGLIED("Bearbeiter")
}

/** Lokalisiertes Label für die UI (ersetzt das interne, deutsche [MemberRole.label]). */
@Composable
fun MemberRole.displayLabel(): String = when (this) {
    MemberRole.BESITZER -> stringResource(R.string.role_owner)
    MemberRole.ADMIN    -> stringResource(R.string.role_admin)
    MemberRole.MITGLIED -> stringResource(R.string.role_editor)
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

/** Lokalisiertes Label für die UI (ersetzt das interne, deutsche [Priority.label]). */
@Composable
fun Priority.displayLabel(): String = when (this) {
    Priority.HOCH    -> stringResource(R.string.priority_high)
    Priority.MITTEL  -> stringResource(R.string.priority_medium)
    Priority.NIEDRIG -> stringResource(R.string.priority_low)
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
 * @param quantity          Mengenangabe als Freitext, nur im Einkaufsmodus genutzt
 * @param price             Geschätzter Preis, nur im Anschaffungsmodus genutzt
 * @param link              Link zum Angebot, nur im Anschaffungsmodus genutzt
 * @param recurrence        Wiederholungsmuster (null = einmalige Aufgabe)
 * @param rotateAmong       Geräte-IDs, unter denen die Zuständigkeit reihum wechselt
 * @param recurrenceSpawned Von der Cloud Function gesetzt, sobald die Folgeaufgabe existiert
 */
data class TodoItem(
    @get:Exclude val id: String = "",
    val title: String = "",
    val description: String = "",
    @get:PropertyName("isDone") @set:PropertyName("isDone")
    var isDone: Boolean = false,
    val priority: String = Priority.MITTEL.name,
    val dueDate: Timestamp? = null,
    val assignedTo: String? = null,
    val reminderMinutes: Int? = null,
    val reminderSent: Boolean = false,
    val createdBy: String = "",
    val createdAt: Timestamp = Timestamp.now(),
    val doneBy: String? = null,
    val doneAt: Timestamp? = null,
    val position: Long = 0L,
    val subtasks: List<Subtask> = emptyList(),
    val comments: List<Comment> = emptyList(),
    val quantity: String = "",
    /**
     * Geschätzter Preis im Anschaffungsmodus. null heißt „noch kein Preis",
     * nicht „kostenlos" – die Unterscheidung zählt für die Summe.
     */
    val price: Double? = null,
    /** Link zum Angebot im Anschaffungsmodus. */
    val link: String = "",
    val recurrence: Recurrence? = null,
    val rotateAmong: List<String> = emptyList(),
    @get:PropertyName("recurrenceSpawned") @set:PropertyName("recurrenceSpawned")
    var recurrenceSpawned: Boolean = false,
) {
    constructor() : this(
        "", "", "", false, Priority.MITTEL.name, null, null, null, false,
        "", Timestamp.now(), null, null, 0L, emptyList(), emptyList(),
        "", null, "", null, emptyList(), false
    )
}

/** Einheit einer Wiederholung. */
enum class RecurrenceUnit { TAG, WOCHE, MONAT, JAHR }

/**
 * Woran der nächste Termin hängt.
 *
 * FAELLIG rechnet vom bisherigen Fälligkeitsdatum – für Termine, die
 * feststehen (Miete, Müllabfuhr). ERLEDIGT rechnet ab dem Abhaken – für
 * Aufgaben, bei denen der Abstand zählt (Blumen gießen).
 */
enum class RecurrenceAnchor { FAELLIG, ERLEDIGT }

/**
 * Wiederholungsmuster einer Aufgabe.
 *
 * Ausgewertet wird es NICHT in der App, sondern von der Cloud Function
 * `onTodoCompleted`: sie legt beim Abhaken die nächste Instanz an und dreht
 * die Zuständigkeit weiter. Damit rechnen App, Web und Desktop nicht jeweils
 * eigene Monatsenden und Zeitumstellungen aus.
 *
 * Nur für geteilte Listen – lokale Listen liegen nicht in Firestore, dort
 * käme die Function nie zum Zug.
 */
data class Recurrence(
    val unit: String = RecurrenceUnit.WOCHE.name,
    val interval: Int = 1,
    val anchor: String = RecurrenceAnchor.FAELLIG.name,
) {
    constructor() : this(RecurrenceUnit.WOCHE.name, 1, RecurrenceAnchor.FAELLIG.name)
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
    ZUGEWIESEN, ERLEDIGT, KOMMENTAR, EINLADUNG, ERINNERUNG
}

/**
 * Eine Benachrichtigung für ein Gerät (z.B. "Jana hat dir eine Aufgabe zugewiesen").
 */
data class AppNotification(
    @get:Exclude val id: String = "",
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

/**
 * Eine Einladung zu einer geteilten Liste.
 *
 * Bewusst ein eigenes Dokument statt der blanken Listen-ID als Code:
 *
 *  - Die Anzeigedaten (Name, Farbe, Icon, Mitgliederzahl) liegen redundant hier
 *    drin, damit der Einladungs-Screen die Liste vorab zeigen kann, ohne dass
 *    Nicht-Mitglieder Lesezugriff auf das Listendokument brauchen.
 *  - Einladungen laufen ab und können widerrufen werden. Mit der Listen-ID als
 *    Code war jeder einmal verteilte Link dauerhaft gültig und nur durch
 *    Löschen der Liste aus dem Verkehr zu ziehen.
 *
 * Die Dokument-ID ist der Code selbst (siehe [generateInviteCode]).
 */
data class Invite(
    @get:Exclude val code: String = "",
    val listId: String = "",
    val listName: String = "",
    val listColor: String = "#6750A4",
    val listIcon: String = "",
    val memberCount: Int = 0,
    val createdBy: String = "",
    val createdAt: Timestamp = Timestamp.now(),
    val expiresAt: Timestamp = Timestamp.now(),
    val revoked: Boolean = false,
) {
    constructor() : this("", "", "", "#6750A4", "", 0, "", Timestamp.now(), Timestamp.now(), false)
}

val Invite.isExpired: Boolean get() = expiresAt.toDate().time < System.currentTimeMillis()

val Invite.isUsable: Boolean get() = !revoked && !isExpired

/**
 * Zeichenvorrat ohne verwechselbare Zeichen (kein I/1, kein O/0), damit ein Code
 * auch mündlich oder abgetippt ankommt.
 */
private const val INVITE_ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"

const val INVITE_CODE_LENGTH = 8

fun generateInviteCode(): String =
    (1..INVITE_CODE_LENGTH).map { INVITE_ALPHABET.random() }.joinToString("")

/** Anzeigeform mit Trennstrich: ABCD-EFGH */
fun String.asDisplayInviteCode(): String =
    if (length == INVITE_CODE_LENGTH) "${take(4)}-${drop(4)}" else this

/**
 * Eingabe des Nutzers auf die gespeicherte Form bringen: Groß-/Kleinschreibung,
 * Leerzeichen und Trennstriche sind egal.
 */
fun String.normalizeInviteCode(): String =
    filter { it.isLetterOrDigit() }.uppercase()
