package de.beigel.list.debug

import android.content.Context
import com.google.firebase.Timestamp
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.FirebaseFirestore
import de.beigel.list.auth.AuthManager
import de.beigel.list.data.AppNotification
import de.beigel.list.data.Comment
import de.beigel.list.data.DeviceIdManager
import de.beigel.list.data.NotificationType
import de.beigel.list.data.Priority
import de.beigel.list.data.Subtask
import de.beigel.list.data.TodoItem
import de.beigel.list.data.TodoList
import kotlinx.coroutines.tasks.await
import java.util.Calendar

/**
 * Erzeugt ein Set realistischer Testdaten (Listen, Aufgaben, Unteraufgaben,
 * Kommentare, Benachrichtigungen) direkt in Firestore, damit sich die App beim
 * Entwickeln/Testen nicht leer anfühlt.
 *
 * NUR FÜR DEBUG-BUILDS GEDACHT. Alle Listen gehören dem aktuellen Gerät
 * (createdBy = eigene deviceId); zwei erfundene Mitglieder ("Jana", "Tom")
 * werden für eine geteilte Liste simuliert – das sind keine echten Geräte,
 * nur Text-IDs, damit die Mitglieder-UI etwas zu zeigen hat.
 *
 * Aufruf z. B. einmalig aus einem Debug-Button heraus:
 *   scope.launch { DebugSeeder.seedSampleData(context) }
 */
object DebugSeeder {

    private const val JANA_ID = "seed-jana-demo"
    private const val TOM_ID = "seed-tom-demo"

    suspend fun seedSampleData(context: Context) {
        val db = FirebaseFirestore.getInstance()
        val listsRef = db.collection("lists")
        val notificationsRef = db.collection("notifications")

        // WICHTIG: Die Firestore Security Rules prüfen gegen request.auth.uid.
        // Das ist NICHT DeviceIdManager.getDeviceId() (rein lokale UUID, Altlast),
        // sondern die echte Firebase-Auth-UID von AuthManager – exakt die ID, die
        // auch überall sonst in der App als "deviceId" durchgereicht wird
        // (siehe MainActivity: TodoRepository(currentUid), MainScreen(deviceId = currentUid)).
        val deviceId = AuthManager.ensureSignedIn()
        val deviceName = DeviceIdManager.getDeviceName(context)

        // ── Liste 1: Wocheneinkauf ──────────────────────────────────────────
        createList(
            listsRef, name = "Wocheneinkauf", color = "#7FD1BE", icon = "ShoppingCart",
            deviceId = deviceId, deviceName = deviceName,
            todos = listOf(
                seedTodo("Milch", isDone = true, priority = Priority.NIEDRIG, createdBy = deviceId),
                seedTodo("Bio-Eier (6er)", priority = Priority.NIEDRIG, createdBy = deviceId),
                seedTodo("Vollkornbrot", priority = Priority.MITTEL, dueDate = daysFromNow(1, 17, 0), createdBy = deviceId),
                seedTodo("Kaffeebohnen", priority = Priority.MITTEL, createdBy = deviceId),
                seedTodo("Spülmaschinentabs", isDone = true, priority = Priority.NIEDRIG, createdBy = deviceId),
                seedTodo(
                    "Geschenk für Papas Geburtstag", priority = Priority.HOCH,
                    dueDate = daysFromNow(3, 12, 0), createdBy = deviceId,
                    subtasks = listOf(
                        Subtask(title = "Idee überlegen", isDone = true),
                        Subtask(title = "Online bestellen", isDone = false),
                    )
                ),
            )
        )

        // ── Liste 2: Haushalt ────────────────────────────────────────────────
        createList(
            listsRef, name = "Haushalt", color = "#FFD8A8", icon = "Home",
            deviceId = deviceId, deviceName = deviceName,
            todos = listOf(
                seedTodo("Wäsche waschen", isDone = true, priority = Priority.NIEDRIG, createdBy = deviceId),
                seedTodo("Staubsaugen", priority = Priority.MITTEL, createdBy = deviceId),
                seedTodo("Rasen mähen", priority = Priority.HOCH, dueDate = daysFromNow(2, 16, 0), createdBy = deviceId),
                seedTodo("Batterien Rauchmelder tauschen", priority = Priority.NIEDRIG, createdBy = deviceId),
                seedTodo(
                    "Keller aufräumen", priority = Priority.NIEDRIG, createdBy = deviceId,
                    subtasks = listOf(
                        Subtask(title = "Kartons sortieren"),
                        Subtask(title = "Alte Kabel entsorgen"),
                    )
                ),
            )
        )

        // ── Liste 3: Naturfreundehaus Immenreute (geteilt) ──────────────────
        val naturfreundeListId = createList(
            listsRef, name = "Naturfreundehaus Immenreute", color = "#5B8DEF", icon = "Cottage",
            deviceId = deviceId, deviceName = deviceName,
            extraMembers = mapOf(JANA_ID to "Jana", TOM_ID to "Tom"),
            todos = listOf(
                seedTodo("Heizöl bestellen", priority = Priority.HOCH, dueDate = daysFromNow(2, 10, 0), assignedTo = JANA_ID, createdBy = deviceId),
                seedTodo("Rasenmäher warten", priority = Priority.MITTEL, createdBy = deviceId),
                seedTodo(
                    "Winterfest machen (Fenster, Wasser abstellen)", priority = Priority.HOCH,
                    dueDate = daysFromNow(9), createdBy = deviceId,
                    subtasks = listOf(
                        Subtask(title = "Fenster kontrollieren"),
                        Subtask(title = "Wasserleitungen absperren"),
                    )
                ),
                seedTodo(
                    "Vorräte fürs nächste Wochenende einkaufen", priority = Priority.MITTEL, createdBy = deviceId,
                    comments = listOf(Comment(authorId = JANA_ID, text = "Ich kann das übernehmen, bringe auch Kaffee mit."))
                ),
                seedTodo("Rechnungen ablegen", isDone = true, priority = Priority.NIEDRIG, createdBy = deviceId),
            )
        )

        // ── Liste 4: App-Ideen ───────────────────────────────────────────────
        createList(
            listsRef, name = "App-Ideen: Dotlist & Genea", color = "#D0BCFF", icon = "Lightbulb",
            deviceId = deviceId, deviceName = deviceName,
            todos = listOf(
                seedTodo("Dotlist: Dark-Mode Feinschliff", priority = Priority.MITTEL, createdBy = deviceId),
                seedTodo(
                    "Genea: \"Person hinzufügen\"-Formular fertigstellen", priority = Priority.HOCH,
                    dueDate = daysFromNow(5), createdBy = deviceId,
                    subtasks = listOf(
                        Subtask(title = "Validierung"),
                        Subtask(title = "Speichern in Firestore"),
                    )
                ),
                seedTodo("Dotlist: Play-Store-Screenshots erneuern", priority = Priority.NIEDRIG, createdBy = deviceId),
                seedTodo("Genea: Einstellungen-Screen", priority = Priority.MITTEL, createdBy = deviceId),
                seedTodo(
                    "Dotlist: Homescreen-Widget?", priority = Priority.NIEDRIG, createdBy = deviceId,
                    comments = listOf(Comment(authorId = deviceId, text = "Mal schauen, ob sich das lohnt."))
                ),
            )
        )

        // ── Ein paar Benachrichtigungen ──────────────────────────────────────
        notificationsRef.add(
            AppNotification(
                recipientId = deviceId, actorId = JANA_ID, actorName = "Jana",
                type = NotificationType.ZUGEWIESEN.name, todoTitle = "Heizöl bestellen",
                listId = naturfreundeListId, isRead = false, createdAt = daysFromNow(0, 9, 0)
            )
        ).await()
        notificationsRef.add(
            AppNotification(
                recipientId = deviceId, actorId = JANA_ID, actorName = "Jana",
                type = NotificationType.KOMMENTAR.name, todoTitle = "Vorräte fürs nächste Wochenende einkaufen",
                listId = naturfreundeListId, isRead = false, createdAt = daysFromNow(0, 8, 30)
            )
        ).await()
        notificationsRef.add(
            AppNotification(
                recipientId = deviceId, actorId = TOM_ID, actorName = "Tom",
                type = NotificationType.ERLEDIGT.name, todoTitle = "Rechnungen ablegen",
                listId = naturfreundeListId, isRead = true, createdAt = daysFromNow(-1, 19, 0)
            )
        ).await()
    }

    private fun daysFromNow(days: Int, hour: Int = 18, minute: Int = 0): Timestamp {
        val cal = Calendar.getInstance()
        cal.add(Calendar.DAY_OF_YEAR, days)
        cal.set(Calendar.HOUR_OF_DAY, hour)
        cal.set(Calendar.MINUTE, minute)
        cal.set(Calendar.SECOND, 0)
        return Timestamp(cal.time)
    }

    private fun seedTodo(
        title: String,
        isDone: Boolean = false,
        priority: Priority = Priority.MITTEL,
        dueDate: Timestamp? = null,
        assignedTo: String? = null,
        createdBy: String,
        subtasks: List<Subtask> = emptyList(),
        comments: List<Comment> = emptyList(),
    ) = TodoItem(
        title = title,
        isDone = isDone,
        priority = priority.name,
        dueDate = dueDate,
        assignedTo = assignedTo,
        createdBy = createdBy,
        doneBy = if (isDone) createdBy else null,
        doneAt = if (isDone) Timestamp.now() else null,
        subtasks = subtasks,
        comments = comments,
    )

    private suspend fun createList(
        listsRef: CollectionReference,
        name: String,
        color: String,
        icon: String,
        deviceId: String,
        deviceName: String,
        todos: List<TodoItem>,
        extraMembers: Map<String, String> = emptyMap(),
    ): String {
        val memberIds = listOf(deviceId) + extraMembers.keys
        val memberNames = mapOf(deviceId to deviceName) + extraMembers
        val list = TodoList(
            name = name,
            memberIds = memberIds,
            memberNames = memberNames,
            createdBy = deviceId,
            color = color,
            icon = icon,
        )
        val docRef = listsRef.add(list).await()
        todos.forEachIndexed { index, todo ->
            docRef.collection("todos").add(todo.copy(position = index.toLong())).await()
        }
        return docRef.id
    }
}