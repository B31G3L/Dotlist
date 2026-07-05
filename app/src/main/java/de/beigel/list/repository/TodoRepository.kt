package de.beigel.list.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import de.beigel.list.data.ListCounts
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
    suspend fun createList(name: String, color: String): String {
        val list = TodoList(
            name = name,
            memberIds = listOf(deviceId),
            createdBy = deviceId,
            color = color
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
     * Dem Gerät über einen Einladungslink einer Liste beitreten.
     * Gibt die Liste zurück wenn erfolgreich, sonst null.
     */
    suspend fun joinList(listId: String): TodoList? {
        val docRef = listsRef.document(listId)
        val doc = docRef.get().await()
        if (!doc.exists()) return null

        val list = doc.toObject(TodoList::class.java)?.copy(id = doc.id) ?: return null

        // deviceId zu memberIds hinzufügen falls nicht bereits dabei
        if (deviceId !in list.memberIds) {
            val updated = list.memberIds + deviceId
            docRef.update("memberIds", updated).await()
        }
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
            docRef.update("memberIds", updated).await()
        }
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
     * Todo-Titel bearbeiten.
     */
    suspend fun editTodo(listId: String, todoId: String, newTitle: String) {
        listsRef.document(listId).collection("todos")
            .document(todoId).update("title", newTitle.trim()).await()
    }
}