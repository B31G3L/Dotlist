package com.beigel.list2share.repository

import android.content.Context
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import com.beigel.list2share.data.AppNotification
import com.beigel.list2share.data.Comment
import com.beigel.list2share.data.ListCounts
import com.beigel.list2share.data.NotificationType
import com.beigel.list2share.data.Subtask
import com.beigel.list2share.data.TodoItem
import com.beigel.list2share.data.TodoList
import com.beigel.list2share.data.local.AppDatabase
import com.beigel.list2share.data.local.LocalListEntity
import com.beigel.list2share.data.local.toComments
import com.beigel.list2share.data.local.toJson
import com.beigel.list2share.data.local.toLocalEntity
import com.beigel.list2share.data.local.toTodoItem
import com.beigel.list2share.data.local.toTodoList
import com.beigel.list2share.data.Priority
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import java.util.UUID
import kotlin.collections.plus

/**
 * Alle Daten-Operationen für Listen und Todos.
 *
 * Jede Liste liegt entweder LOKAL (Room-DB, nur dieses Gerät, Standard bei
 * Neuerstellung) oder REMOTE (Firestore, geräteübergreifend teilbar) –
 * niemals an beiden Orten gleichzeitig. Der Wechsel erfolgt über
 * [shareList] ("Teilen"-Regler an) bzw. [unshareList] ("Teilen"-Regler aus,
 * nur für Besitzer/Admin), siehe TodoList.isShared.
 *
 * Firestore-Struktur (nur für geteilte Listen):
 *   lists/{listId}
 *       name, memberIds, createdBy, createdAt, color
 *   lists/{listId}/todos/{todoId}
 *       title, isDone, createdBy, createdAt, doneBy, doneAt, position
 */
class TodoRepository(private val deviceId: String, context: Context) {

    private val db = FirebaseFirestore.getInstance()
    private val listsRef = db.collection("lists")
    private val database = AppDatabase.getInstance(context)
    private val listDao get() = database.listDao()
    private val todoDao get() = database.todoDao()

    private suspend fun isLocalList(listId: String): Boolean = listDao.getList(listId) != null

    // ─── Listen ──────────────────────────────────────────────────────────────

    /**
     * Alle Listen des Geräts als Echtzeit-Flow: lokale (nicht geteilte) +
     * remote (geteilte, Mitgliedschaft über memberIds) Listen zusammengeführt.
     */
    fun observeLists(): Flow<List<TodoList>> {
        val localFlow = listDao.observeLists().map { entities -> entities.map { it.toTodoList() } }
        val remoteFlow = observeRemoteLists()
        return combine(localFlow, remoteFlow) { local, remote ->
            (local + remote).sortedByDescending { it.createdAt.seconds }
        }
    }

    private fun observeRemoteLists(): Flow<List<TodoList>> = callbackFlow {
        val registration: ListenerRegistration = listsRef
            .whereArrayContains("memberIds", deviceId)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                val lists = snapshot?.documents?.mapNotNull { doc ->
                    doc.toObject(TodoList::class.java)?.copy(id = doc.id, isShared = true)
                } ?: emptyList()
                trySend(lists)
            }
        awaitClose { registration.remove() }
    }

    /**
     * Neue Liste erstellen. Landet standardmäßig NUR lokal auf diesem Gerät
     * (nicht in Firebase) – erst [shareList] macht sie teilbar. Gibt die neue
     * Listen-ID zurück.
     */
    suspend fun createList(name: String, color: String, creatorName: String, icon: String = ""): String {
        val id = UUID.randomUUID().toString()
        listDao.insertList(
            LocalListEntity(
                id = id,
                name = name,
                createdBy = deviceId,
                creatorName = creatorName,
                createdAt = System.currentTimeMillis(),
                color = color,
                icon = icon
            )
        )
        return id
    }

    /**
     * Liste umbenennen.
     */
    suspend fun renameList(listId: String, newName: String) {
        if (isLocalList(listId)) {
            listDao.renameList(listId, newName.trim())
        } else {
            listsRef.document(listId).update("name", newName).await()
        }
    }

    /**
     * Liste löschen (inkl. aller Todos).
     */
    suspend fun deleteList(listId: String) {
        if (isLocalList(listId)) {
            todoDao.deleteTodosForList(listId)
            listDao.deleteList(listId)
            return
        }
        // Zuerst alle Todos löschen
        val todos = listsRef.document(listId).collection("todos").get().await()
        val batch = db.batch()
        todos.documents.forEach { batch.delete(it.reference) }
        batch.delete(listsRef.document(listId))
        batch.commit().await()
    }

    /**
     * Liste in Firestore hochladen und damit teilbar machen ("Teilen"-Regler an).
     * Nimmt eine bisher lokale Liste inkl. aller Todos, legt sie unter derselben
     * ID in Firestore an und entfernt anschließend die lokale Kopie.
     */
    suspend fun shareList(list: TodoList, creatorName: String): TodoList {
        val localTodos = todoDao.getTodosOnce(list.id)

        val remoteList = TodoList(
            id = list.id,
            name = list.name,
            memberIds = listOf(deviceId),
            memberNames = mapOf(deviceId to creatorName),
            adminIds = emptyList(),
            createdBy = deviceId,
            createdAt = list.createdAt,
            color = list.color,
            icon = list.icon,
            mutedBy = emptyList()
        )
        listsRef.document(list.id).set(remoteList).await()

        if (localTodos.isNotEmpty()) {
            val batch = db.batch()
            localTodos.forEach { entity ->
                val todo = entity.toTodoItem()
                val ref = listsRef.document(list.id).collection("todos").document(todo.id)
                batch.set(ref, todo)
            }
            batch.commit().await()
        }

        todoDao.deleteTodosForList(list.id)
        listDao.deleteList(list.id)

        return remoteList.copy(isShared = true)
    }

    /**
     * Liste aus Firestore entfernen und wieder rein lokal machen
     * ("Teilen"-Regler aus). Nur für Besitzer/Admin gedacht (siehe
     * TodoList.canManageMembers in der UI). Entfernt damit auch den Zugriff
     * für alle anderen Mitglieder – die Liste bleibt danach nur noch auf
     * diesem Gerät erhalten.
     */
    suspend fun unshareList(list: TodoList) {
        val todosSnapshot = listsRef.document(list.id).collection("todos").get().await()
        val todos = todosSnapshot.documents.mapNotNull { doc ->
            doc.toObject(TodoItem::class.java)?.copy(id = doc.id)
        }

        listDao.insertList(
            LocalListEntity(
                id = list.id,
                name = list.name,
                createdBy = deviceId,
                creatorName = list.displayNameForOrFallback(deviceId),
                createdAt = list.createdAt.toDate().time,
                color = list.color,
                icon = list.icon
            )
        )
        if (todos.isNotEmpty()) {
            todoDao.insertTodos(todos.map { it.toLocalEntity(list.id) })
        }

        // Firestore-Eintrag komplett löschen (entfernt Zugriff für alle Mitglieder)
        val todosSnap = listsRef.document(list.id).collection("todos").get().await()
        val batch = db.batch()
        todosSnap.documents.forEach { batch.delete(it.reference) }
        batch.delete(listsRef.document(list.id))
        batch.commit().await()
    }

    private fun TodoList.displayNameForOrFallback(memberId: String): String =
        memberNames[memberId] ?: "Ich"

    /**
     * Liste duplizieren (neue Liste mit "Kopie"-Zusatz, alle Todos werden mitkopiert).
     * Die Kopie bleibt im selben Modus (lokal/geteilt) wie das Original und
     * gehört nur dem aktuellen Gerät (keine geteilten Mitglieder).
     */
    suspend fun duplicateList(list: TodoList, creatorName: String, copySuffix: String = "Copy"): String {
        if (!list.isShared) {
            val newId = UUID.randomUUID().toString()
            listDao.insertList(
                LocalListEntity(
                    id = newId,
                    name = "${list.name} $copySuffix",
                    createdBy = deviceId,
                    creatorName = creatorName,
                    createdAt = System.currentTimeMillis(),
                    color = list.color,
                    icon = list.icon
                )
            )
            val todos = todoDao.getTodosOnce(list.id)
            if (todos.isNotEmpty()) {
                todoDao.insertTodos(todos.map { it.copy(id = UUID.randomUUID().toString(), listId = newId) })
            }
            return newId
        }

        val newList = TodoList(
            name = "${list.name} $copySuffix",
            memberIds = listOf(deviceId),
            memberNames = mapOf(deviceId to creatorName),
            createdBy = deviceId,
            color = list.color,
            icon = list.icon
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
     * Betrifft immer nur geteilte (Firestore-)Listen, da nur diese über eine
     * Einladungs-ID erreichbar sind.
     */
    suspend fun previewList(listId: String): TodoList? {
        val doc = listsRef.document(listId).get().await()
        if (!doc.exists()) return null
        return doc.toObject(TodoList::class.java)?.copy(id = doc.id, isShared = true)
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
        return list.copy(isShared = true)
    }

    /**
     * Liste verlassen (eigene ID aus memberIds entfernen). Für lokale Listen
     * (immer nur ein "Mitglied": man selbst) entspricht das dem Löschen.
     */
    suspend fun leaveList(listId: String) {
        if (isLocalList(listId)) {
            deleteList(listId)
            return
        }
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
                    "memberNames.$deviceId" to FieldValue.delete()
                )
            ).await()
        }
    }

    suspend fun promoteToAdmin(listId: String, memberId: String) {
        listsRef.document(listId).update(
            "adminIds", FieldValue.arrayUnion(memberId)
        ).await()
    }

    suspend fun demoteAdmin(listId: String, memberId: String) {
        listsRef.document(listId).update(
            "adminIds", FieldValue.arrayRemove(memberId)
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
                "memberNames.$memberId" to FieldValue.delete()
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
    fun observeTodoCounts(listId: String): Flow<ListCounts> = flow {
        if (isLocalList(listId)) {
            emitAll(
                todoDao.observeTodos(listId).map { todos ->
                    ListCounts(done = todos.count { it.isDone }, total = todos.size)
                }
            )
        } else {
            emitAll(observeRemoteTodoCounts(listId))
        }
    }

    private fun observeRemoteTodoCounts(listId: String): Flow<ListCounts> = callbackFlow {
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
    fun observeTodos(listId: String): Flow<List<TodoItem>> = flow {
        if (isLocalList(listId)) {
            emitAll(todoDao.observeTodos(listId).map { entities -> entities.map { it.toTodoItem() } })
        } else {
            emitAll(observeRemoteTodos(listId))
        }
    }

    private fun observeRemoteTodos(listId: String): Flow<List<TodoItem>> = callbackFlow {
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
        priority        : Priority = Priority.MITTEL,
        dueDate         : Timestamp? = null,
        assignedTo      : String? = null,
        reminderMinutes : Int? = null,
    ) {
        if (isLocalList(listId)) {
            val count = todoDao.countTodos(listId).toLong()
            val todo = TodoItem(
                id = UUID.randomUUID().toString(),
                title = title.trim(),
                description = description.trim(),
                isDone = false,
                priority = priority.name,
                dueDate = dueDate,
                assignedTo = assignedTo,
                reminderMinutes = reminderMinutes,
                createdBy = deviceId,
                position = count
            )
            todoDao.insertTodo(todo.toLocalEntity(listId))
            return
        }

        // Position = aktuelle Anzahl Todos
        val count = listsRef
            .document(listId)
            .collection("todos")
            .get().await().size().toLong()

        val todo = TodoItem(
            title = title.trim(),
            description = description.trim(),
            isDone = false,
            priority = priority.name,
            dueDate = dueDate,
            assignedTo = assignedTo,
            reminderMinutes = reminderMinutes,
            createdBy = deviceId,
            position = count
        )
        listsRef.document(listId).collection("todos").add(todo).await()
    }

    /**
     * Todo als erledigt/offen markieren.
     */
    suspend fun toggleTodo(listId: String, todo: TodoItem) {
        val newDone = !todo.isDone
        if (isLocalList(listId)) {
            val entity = todoDao.getTodo(todo.id) ?: return
            todoDao.updateTodo(
                entity.copy(
                    isDone = newDone,
                    doneBy = if (newDone) deviceId else null,
                    doneAt = if (newDone) System.currentTimeMillis() else null
                )
            )
            return
        }
        val updates = if (newDone) {
            mapOf(
                "isDone" to true,
                "doneBy" to deviceId,
                "doneAt" to Timestamp.now()
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
        if (isLocalList(listId)) {
            todoDao.deleteTodo(todoId)
            return
        }
        listsRef.document(listId).collection("todos")
            .document(todoId).delete().await()
    }

    /**
     * Ein zuvor gelöschtes Todo mit exakt derselben ID wiederherstellen (Undo).
     */
    suspend fun restoreTodo(listId: String, todo: TodoItem) {
        if (isLocalList(listId)) {
            todoDao.insertTodo(todo.toLocalEntity(listId))
            return
        }
        listsRef.document(listId).collection("todos")
            .document(todo.id).set(todo).await()
    }

    /**
     * Todo-Titel bearbeiten.
     */
    suspend fun editTodo(listId: String, todoId: String, newTitle: String) {
        if (isLocalList(listId)) {
            val entity = todoDao.getTodo(todoId) ?: return
            todoDao.updateTodo(entity.copy(title = newTitle.trim()))
            return
        }
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
        priority        : Priority,
        dueDate         : Timestamp?,
        assignedTo      : String?,
        reminderMinutes : Int?,
    ) {
        if (isLocalList(listId)) {
            val entity = todoDao.getTodo(todoId) ?: return
            todoDao.updateTodo(
                entity.copy(
                    title           = title.trim(),
                    description     = description.trim(),
                    priority        = priority.name,
                    dueDate         = dueDate?.toDate()?.time,
                    assignedTo      = assignedTo,
                    reminderMinutes = reminderMinutes
                )
            )
            return
        }
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
     * Todo in eine andere Liste verschieben (gleiche ID, neue Position am Ende).
     * Deckt alle Kombinationen ab: lokal→lokal, lokal→geteilt, geteilt→lokal,
     * geteilt→geteilt.
     */
    suspend fun moveTodo(fromListId: String, toListId: String, todo: TodoItem) {
        if (fromListId == toListId) return
        val fromLocal = isLocalList(fromListId)
        val toLocal   = isLocalList(toListId)

        if (fromLocal && toLocal) {
            val entity = todoDao.getTodo(todo.id) ?: return
            val newPosition = todoDao.countTodos(toListId).toLong()
            todoDao.updateTodo(entity.copy(listId = toListId, position = newPosition))
            return
        }
        if (!fromLocal && !toLocal) {
            val newPosition = listsRef.document(toListId).collection("todos").get().await().size().toLong()
            val movedTodo = todo.copy(position = newPosition)
            listsRef.document(toListId).collection("todos").document(todo.id).set(movedTodo).await()
            listsRef.document(fromListId).collection("todos").document(todo.id).delete().await()
            return
        }
        if (fromLocal && !toLocal) {
            val entity = todoDao.getTodo(todo.id) ?: return
            val newPosition = listsRef.document(toListId).collection("todos").get().await().size().toLong()
            val movedTodo = entity.toTodoItem().copy(position = newPosition)
            listsRef.document(toListId).collection("todos").document(todo.id).set(movedTodo).await()
            todoDao.deleteTodo(todo.id)
            return
        }
        // remote -> lokal
        val newPosition = todoDao.countTodos(toListId).toLong()
        val movedTodo = todo.copy(position = newPosition)
        todoDao.insertTodo(movedTodo.toLocalEntity(toListId))
        listsRef.document(fromListId).collection("todos").document(todo.id).delete().await()
    }

    /**
     * Komplette Unteraufgaben-Liste eines Todos ersetzen.
     */
    suspend fun updateSubtasks(listId: String, todoId: String, subtasks: List<Subtask>) {
        if (isLocalList(listId)) {
            val entity = todoDao.getTodo(todoId) ?: return
            todoDao.updateTodo(entity.copy(subtasksJson = subtasks.toJson()))
            return
        }
        listsRef.document(listId).collection("todos")
            .document(todoId).update("subtasks", subtasks).await()
    }

    /**
     * Kommentar zu einem Todo hinzufügen.
     */
    suspend fun addComment(listId: String, todoId: String, comment: Comment) {
        if (isLocalList(listId)) {
            val entity = todoDao.getTodo(todoId) ?: return
            todoDao.updateTodo(
                entity.copy(
                    commentsJson = (entity.commentsJson.toComments() + comment).toJson()
                )
            )
            return
        }
        listsRef.document(listId).collection("todos")
            .document(todoId).update("comments", FieldValue.arrayUnion(comment)).await()
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
            .set(mapOf("token" to token, "updatedAt" to Timestamp.now()), SetOptions.merge())
            .await()
    }

    /**
     * Globaler Push-Schalter (Profil-Einstellung) – wird zusätzlich zur
     * lokalen Einstellung nach Firestore gespiegelt, da die Cloud Function
     * nur von dort aus lesen kann.
     */
    suspend fun setPushEnabled(enabled: Boolean) {
        deviceTokensRef.document(deviceId)
            .set(mapOf("pushEnabled" to enabled), SetOptions.merge())
            .await()
    }

    /**
     * Push-Benachrichtigungen für eine bestimmte Liste stummschalten/wieder aktivieren.
     * Nur relevant für geteilte Listen – lokale Listen erzeugen ohnehin keine Pushes.
     */
    suspend fun setListMuted(listId: String, muted: Boolean) {
        if (isLocalList(listId)) return
        val update = if (muted) {
            FieldValue.arrayUnion(deviceId)
        } else {
            FieldValue.arrayRemove(deviceId)
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
            actorId = deviceId,
            actorName = actorName,
            type = type.name,
            todoTitle = todoTitle,
            listId = listId,
            todoId = todoId,
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
