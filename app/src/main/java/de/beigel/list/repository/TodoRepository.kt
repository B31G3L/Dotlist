package de.beigel.list.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import de.beigel.list.data.AppNotification
import de.beigel.list.data.Comment
import de.beigel.list.data.ListCounts
import de.beigel.list.data.NotificationType
import de.beigel.list.data.Subtask
import de.beigel.list.data.TodoItem
import de.beigel.list.data.TodoList
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

/**
 * Alle Firestore-Operationen.
 *
 * Firestore-Struktur:
 *   lists/{listId}
 *       name, memberIds, createdBy, createdAt, color
 *   lists/{listId}/todos/{todoId}
 *       title, isDone, createdBy, createdAt, doneBy, doneAt, position
 */
class TodoRepository(private val deviceId: String) {

    private val db = FirebaseFirestore.getInstance()
    private val listsRef = db.collection("lists")

    // ─── Listen ──────────────────────────────────────────────────────────────

    /**
     * Alle Listen, bei denen das Gerät Mitglied ist – als Echtzeit-Flow.
     */
    fun observeLists(): Flow<List<TodoList>> = callbackFlow {
        val registration: ListenerRegistration = listsRef
            .whereArrayContains("memberIds", deviceId)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                val lists = snapshot?.documents?.mapNotNull { doc ->
                    doc.toObject(TodoList::class.java)?.copy(id = doc.id)
                } ?: emptyList()
                trySend(lists)
            }
        awaitClose { registration.remove() }
    }

    /**
     * Neue Liste erstellen. Gibt die neue Listen-ID zurück.
     */
    suspend fun createList(name: String, color: String, creatorName: String, icon: String = ""): String {
        val list = TodoList(
            name        = name,
            memberIds   = listOf(deviceId),
            memberNames = mapOf(deviceId to creatorName),
            createdBy   = deviceId,
            color       = color,
            icon        = icon
        )
        val doc = listsRef.add(list).await()
        return doc.id
    }

    /**
     * Liste umbenennen.
     */
    suspend fun renameList(listId: String, newName: String) {
        listsRef.document(listId).update("name", newName).await()
    }

    /**
     * Liste löschen (inkl. aller Todos).
     */
    suspend fun deleteList(listId: String) {
        // Zuerst alle Todos löschen
        val todos = listsRef.document(listId).collection("todos").get().await()
        val batch = db.batch()
        todos.documents.forEach { batch.delete(it.reference) }
        batch.delete(listsRef.document(listId))
        batch.commit().await()
    }

    /**
     * Liste duplizieren (neue Liste mit "Kopie"-Zusatz, alle Todos werden mitkopiert).
     * Die Kopie gehört nur dem aktuellen Gerät (keine geteilten Mitglieder).
     */
    suspend fun duplicateList(list: TodoList, creatorName: String, copySuffix: String = "Copy"): String {
        val newList = TodoList(
            name        = "${list.name} $copySuffix",
            memberIds   = listOf(deviceId),
            memberNames = mapOf(deviceId to creatorName),
            createdBy   = deviceId,
            color       = list.color,
            icon        = list.icon
        )
        val newDoc = listsRef.add(newList).await()

        val todos = listsRef.document(list.id).collection("todos").get().await()
        if (!todos.isEmpty) {
            val batch = db.batch()
            todos.documents.forEach { doc ->
                val todo = doc.toObject(TodoItem::class.java) ?: return@forEach
                val newTodoRef = listsRef.document(newDoc.id).collection("todos").document()
                batch.set(newTodoRef, todo)
            }
            batch.commit().await()
        }
        return newDoc.id
    }

    /**
     * Liste anhand der ID ansehen, ohne beizutreten (für den Einladungs-Screen).
     */
    suspend fun previewList(listId: String): TodoList? {
        val doc = listsRef.document(listId).get().await()
        if (!doc.exists()) return null
        return doc.toObject(TodoList::class.java)?.copy(id = doc.id)
    }

    /**
     * Dem Gerät über einen Einladungslink einer Liste beitreten.
     * Der eigene Anzeigename wird dabei mit in die Liste übernommen.
     * Gibt die Liste zurück wenn erfolgreich, sonst null.
     */
    suspend fun joinList(listId: String, displayName: String): TodoList? {
        val docRef = listsRef.document(listId)
        val doc = docRef.get().await()
        if (!doc.exists()) return null

        val list = doc.toObject(TodoList::class.java)?.copy(id = doc.id) ?: return null

        // deviceId zu memberIds hinzufügen falls nicht bereits dabei, Name immer aktualisieren
        val updates = mutableMapOf<String, Any>("memberNames.$deviceId" to displayName)
        if (deviceId !in list.memberIds) {
            updates["memberIds"] = list.memberIds + deviceId
        }
        docRef.update(updates).await()
        return list
    }

    /**
     * Liste verlassen (eigene ID aus memberIds entfernen).
     */
    suspend fun leaveList(listId: String) {
        val docRef = listsRef.document(listId)
        val doc = docRef.get().await()
        val list = doc.toObject(TodoList::class.java) ?: return
        val updated = list.memberIds.filter { it != deviceId }
        if (updated.isEmpty()) {
            // Letzte Person – Liste löschen
            deleteList(listId)
        } else {
            docRef.update(
                mapOf(
                    "memberIds" to updated,
                    "adminIds"  to list.adminIds.filter { it != deviceId },
                    "memberNames.$deviceId" to com.google.firebase.firestore.FieldValue.delete()
                )
            ).await()
        }
    }

    suspend fun promoteToAdmin(listId: String, memberId: String) {
        listsRef.document(listId).update(
            "adminIds", com.google.firebase.firestore.FieldValue.arrayUnion(memberId)
        ).await()
    }

    suspend fun demoteAdmin(listId: String, memberId: String) {
        listsRef.document(listId).update(
            "adminIds", com.google.firebase.firestore.FieldValue.arrayRemove(memberId)
        ).await()
    }

    suspend fun removeMember(listId: String, memberId: String) {
        val docRef = listsRef.document(listId)
        val doc = docRef.get().await()
        val list = doc.toObject(TodoList::class.java) ?: return
        docRef.update(
            mapOf(
                "memberIds" to list.memberIds.filter { it != memberId },
                "adminIds"  to list.adminIds.filter { it != memberId },
                "memberNames.$memberId" to com.google.firebase.firestore.FieldValue.delete()
            )
        ).await()
    }

    suspend fun transferOwnership(listId: String, newOwnerId: String) {
        val docRef = listsRef.document(listId)
        val doc = docRef.get().await()
        val list = doc.toObject(TodoList::class.java) ?: return
        if (newOwnerId !in list.memberIds) return
        val newAdminIds = (list.adminIds + deviceId - newOwnerId).distinct()
        docRef.update(
            mapOf(
                "createdBy" to newOwnerId,
                "adminIds"  to newAdminIds
            )
        ).await()
    }

    // ─── Todos ───────────────────────────────────────────────────────────────

    /**
     * Anzahl erledigter/aller Todos einer Liste als Echtzeit-Flow (für die Listen-Übersicht).
     */
    fun observeTodoCounts(listId: String): Flow<ListCounts> = callbackFlow {
        val registration: ListenerRegistration = listsRef
            .document(listId)
            .collection("todos")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                val docs  = snapshot?.documents ?: emptyList()
                val total = docs.size
                val done  = docs.count { it.getBoolean("isDone") == true }
                trySend(ListCounts(done, total))
            }
        awaitClose { registration.remove() }
    }

    /**
     * Alle Todos einer Liste als Echtzeit-Flow.
     */
    fun observeTodos(listId: String): Flow<List<TodoItem>> = callbackFlow {
        val registration: ListenerRegistration = listsRef
            .document(listId)
            .collection("todos")
            .orderBy("position", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                val todos = snapshot?.documents?.mapNotNull { doc ->
                    doc.toObject(TodoItem::class.java)?.copy(id = doc.id)
                } ?: emptyList()
                trySend(todos)
            }
        awaitClose { registration.remove() }
    }

    /**
     * Neues Todo hinzufügen.
     */
    suspend fun addTodo(
        listId          : String,
        title           : String,
        description     : String = "",
        priority        : de.beigel.list.data.Priority = de.beigel.list.data.Priority.MITTEL,
        dueDate         : com.google.firebase.Timestamp? = null,
        assignedTo      : String? = null,
        reminderMinutes : Int? = null,
    ) {
        // Position = aktuelle Anzahl Todos
        val count = listsRef
            .document(listId)
            .collection("todos")
            .get().await().size().toLong()

        val todo = TodoItem(
            title           = title.trim(),
            description     = description.trim(),
            isDone          = false,
            priority        = priority.name,
            dueDate         = dueDate,
            assignedTo      = assignedTo,
            reminderMinutes = reminderMinutes,
            createdBy       = deviceId,
            position        = count
        )
        listsRef.document(listId).collection("todos").add(todo).await()
    }

    /**
     * Todo als erledigt/offen markieren.
     */
    suspend fun toggleTodo(listId: String, todo: TodoItem) {
        val newDone = !todo.isDone
        val updates = if (newDone) {
            mapOf(
                "isDone" to true,
                "doneBy" to deviceId,
                "doneAt" to com.google.firebase.Timestamp.now()
            )
        } else {
            mapOf(
                "isDone" to false,
                "doneBy" to null,
                "doneAt" to null
            )
        }
        listsRef.document(listId).collection("todos")
            .document(todo.id).update(updates).await()
    }

    /**
     * Todo löschen.
     */
    suspend fun deleteTodo(listId: String, todoId: String) {
        listsRef.document(listId).collection("todos")
            .document(todoId).delete().await()
    }

    /**
     * Ein zuvor gelöschtes Todo mit exakt derselben ID wiederherstellen (Undo).
     */
    suspend fun restoreTodo(listId: String, todo: de.beigel.list.data.TodoItem) {
        listsRef.document(listId).collection("todos")
            .document(todo.id).set(todo).await()
    }

    /**
     * Todo-Titel bearbeiten.
     */
    suspend fun editTodo(listId: String, todoId: String, newTitle: String) {
        listsRef.document(listId).collection("todos")
            .document(todoId).update("title", newTitle.trim()).await()
    }

    /**
     * Todo vollständig aktualisieren (aus der Detailansicht).
     */
    suspend fun updateTodo(
        listId          : String,
        todoId          : String,
        title           : String,
        description     : String,
        priority        : de.beigel.list.data.Priority,
        dueDate         : com.google.firebase.Timestamp?,
        assignedTo      : String?,
        reminderMinutes : Int?,
    ) {
        val updates = mapOf(
            "title"           to title.trim(),
            "description"     to description.trim(),
            "priority"        to priority.name,
            "dueDate"         to dueDate,
            "assignedTo"      to assignedTo,
            "reminderMinutes" to reminderMinutes,
        )
        listsRef.document(listId).collection("todos")
            .document(todoId).update(updates).await()
    }

    /**
     * Todo in eine andere Liste verschieben (gleiche Dokument-ID, neue Position am Ende).
     */
    suspend fun moveTodo(fromListId: String, toListId: String, todo: TodoItem) {
        if (fromListId == toListId) return
        val newPosition = listsRef.document(toListId).collection("todos").get().await().size().toLong()
        val movedTodo = todo.copy(position = newPosition)
        listsRef.document(toListId).collection("todos").document(todo.id).set(movedTodo).await()
        listsRef.document(fromListId).collection("todos").document(todo.id).delete().await()
    }

    /**
     * Komplette Unteraufgaben-Liste eines Todos ersetzen.
     */
    suspend fun updateSubtasks(listId: String, todoId: String, subtasks: List<Subtask>) {
        listsRef.document(listId).collection("todos")
            .document(todoId).update("subtasks", subtasks).await()
    }

    /**
     * Kommentar zu einem Todo hinzufügen.
     */
    suspend fun addComment(listId: String, todoId: String, comment: Comment) {
        listsRef.document(listId).collection("todos")
            .document(todoId).update("comments", com.google.firebase.firestore.FieldValue.arrayUnion(comment)).await()
    }

    // ─── Benachrichtigungen ──────────────────────────────────────────────────

    private val notificationsRef = db.collection("notifications")
    private val deviceTokensRef  = db.collection("deviceTokens")

    // ─── Push-Benachrichtigungen (FCM) ─────────────────────────────────────────

    /**
     * Aktuellen FCM-Geräte-Token in Firestore hinterlegen, damit die Cloud
     * Function weiß, an welches Gerät sie Pushes schicken soll.
     */
    suspend fun saveDeviceToken(token: String) {
        deviceTokensRef.document(deviceId)
            .set(mapOf("token" to token, "updatedAt" to com.google.firebase.Timestamp.now()), com.google.firebase.firestore.SetOptions.merge())
            .await()
    }

    /**
     * Globaler Push-Schalter (Profil-Einstellung) – wird zusätzlich zur
     * lokalen Einstellung nach Firestore gespiegelt, da die Cloud Function
     * nur von dort aus lesen kann.
     */
    suspend fun setPushEnabled(enabled: Boolean) {
        deviceTokensRef.document(deviceId)
            .set(mapOf("pushEnabled" to enabled), com.google.firebase.firestore.SetOptions.merge())
            .await()
    }

    /**
     * Push-Benachrichtigungen für eine bestimmte Liste stummschalten/wieder aktivieren.
     */
    suspend fun setListMuted(listId: String, muted: Boolean) {
        val update = if (muted) {
            com.google.firebase.firestore.FieldValue.arrayUnion(deviceId)
        } else {
            com.google.firebase.firestore.FieldValue.arrayRemove(deviceId)
        }
        listsRef.document(listId).update("mutedBy", update).await()
    }

    private suspend fun notify(
        recipientId : String?,
        actorName   : String,
        type        : NotificationType,
        todoTitle   : String,
        listId      : String,
        todoId      : String = "",
    ) {
        if (recipientId.isNullOrBlank() || recipientId == deviceId) return
        val notification = AppNotification(
            recipientId = recipientId,
            actorId     = deviceId,
            actorName   = actorName,
            type        = type.name,
            todoTitle   = todoTitle,
            listId      = listId,
            todoId      = todoId,
        )
        notificationsRef.add(notification).await()
    }

    suspend fun notifyAssigned(recipientId: String?, actorName: String, todoTitle: String, listId: String, todoId: String) =
        notify(recipientId, actorName, NotificationType.ZUGEWIESEN, todoTitle, listId, todoId)

    suspend fun notifyDone(recipientId: String?, actorName: String, todoTitle: String, listId: String, todoId: String) =
        notify(recipientId, actorName, NotificationType.ERLEDIGT, todoTitle, listId, todoId)

    suspend fun notifyComment(recipientId: String?, actorName: String, todoTitle: String, listId: String, todoId: String) =
        notify(recipientId, actorName, NotificationType.KOMMENTAR, todoTitle, listId, todoId)

    suspend fun notifyInvite(recipientId: String?, actorName: String, listName: String, listId: String) =
        notify(recipientId, actorName, NotificationType.EINLADUNG, listName, listId)

    /**
     * Live-Stream aller Benachrichtigungen für das aktuelle Gerät, neueste zuerst.
     */
    fun observeNotifications(): Flow<List<AppNotification>> = callbackFlow {
        val registration = notificationsRef
            .whereEqualTo("recipientId", deviceId)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .limit(50)
            .addSnapshotListener { snapshot, error ->
                if (error != null) { close(error); return@addSnapshotListener }
                val items = snapshot?.documents?.mapNotNull { doc ->
                    doc.toObject(AppNotification::class.java)?.copy(id = doc.id)
                } ?: emptyList()
                trySend(items)
            }
        awaitClose { registration.remove() }
    }

    suspend fun markNotificationRead(id: String) {
        notificationsRef.document(id).update("isRead", true).await()
    }

    suspend fun markAllNotificationsRead(ids: List<String>) {
        if (ids.isEmpty()) return
        val batch = db.batch()
        ids.forEach { batch.update(notificationsRef.document(it), "isRead", true) }
        batch.commit().await()
    }
}