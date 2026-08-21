@file:OptIn(ExperimentalCoroutinesApi::class)

package com.beigel.list2share.repository

import android.content.Context
import android.util.Log
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import com.beigel.list2share.data.AppNotification
import com.beigel.list2share.data.Comment
import com.beigel.list2share.data.Invite
import com.beigel.list2share.data.isUsable
import com.beigel.list2share.data.generateInviteCode
import com.beigel.list2share.data.normalizeInviteCode
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
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import java.util.Calendar
import java.util.UUID

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
 *       name, memberIds, memberNames, adminIds, createdBy, createdAt, color, icon, mutedBy
 *   lists/{listId}/todos/{todoId}
 *       title, isDone, createdBy, createdAt, doneBy, doneAt, position, subtasks, comments
 */
class TodoRepository(private val deviceId: String, context: Context) {

    private companion object {
        const val TAG = "TodoRepository"

        /**
         * Firestore erlaubt 500 Operationen pro Batch. Mit etwas Reserve, damit
         * ein Batch nicht am Limit kippt, wenn noch eine Operation dazukommt.
         */
        const val BATCH_LIMIT = 450

        /** Standard-Gültigkeit eines Einladungscodes in Tagen. */
        const val DEFAULT_INVITE_DAYS = 7

        /** Wie oft bei einer Code-Kollision ein neuer Code probiert wird. */
        const val INVITE_CODE_ATTEMPTS = 5
    }

    private val db = FirebaseFirestore.getInstance()
    private val listsRef = db.collection("lists")
    private val invitesRef = db.collection("invites")
    private val database = AppDatabase.getInstance(context)
    private val listDao get() = database.listDao()
    private val todoDao get() = database.todoDao()

    private fun todosRef(listId: String): CollectionReference =
        listsRef.document(listId).collection("todos")

    private suspend fun isLocalList(listId: String): Boolean = listDao.getList(listId) != null

    // ─── Firestore-Helfer ────────────────────────────────────────────────────

    /**
     * Löscht Dokumente in Blöcken zu [BATCH_LIMIT], damit auch Listen mit mehr
     * als 500 Todos verarbeitet werden können.
     */
    private suspend fun deleteInChunks(refs: List<DocumentReference>) {
        refs.chunked(BATCH_LIMIT).forEach { chunk ->
            val batch = db.batch()
            chunk.forEach { batch.delete(it) }
            batch.commit().await()
        }
    }

    /** Schreibt beliebig viele Dokumente in Blöcken zu [BATCH_LIMIT]. */
    private suspend fun <T : Any> setInChunks(items: List<Pair<DocumentReference, T>>) {
        items.chunked(BATCH_LIMIT).forEach { chunk ->
            val batch = db.batch()
            chunk.forEach { (ref, value) -> batch.set(ref, value) }
            batch.commit().await()
        }
    }

    /** Listendokument samt aller Todos entfernen. */
    private suspend fun deleteRemoteListCompletely(listId: String) {
        // Erst die Einladungen entwerten – sonst zeigt ein noch kursierender Code
        // auf eine Liste, die es nicht mehr gibt.
        runCatching { revokeInvitesFor(listId) }
            .onFailure { Log.w(TAG, "Einladungen zu $listId nicht widerrufen", it) }
        val todos = todosRef(listId).get().await()
        deleteInChunks(todos.documents.map { it.reference })
        listsRef.document(listId).delete().await()
    }

    /**
     * Nächste freie Position in einer geteilten Liste.
     *
     * Bewusst über das höchste vorhandene `position`-Feld statt über die Anzahl
     * der Dokumente: das kostet einen einzigen Read statt eines Reads pro Todo,
     * funktioniert offline über den Firestore-Cache und vergibt auch dann keine
     * doppelten Positionen, wenn zwischendurch Todos gelöscht wurden.
     */
    private suspend fun nextRemotePosition(listId: String): Long {
        val snapshot = todosRef(listId)
            .orderBy("position", Query.Direction.DESCENDING)
            .limit(1)
            .get()
            .await()
        return (snapshot.documents.firstOrNull()?.getLong("position") ?: -1L) + 1L
    }

    // ─── Listen ──────────────────────────────────────────────────────────────

    /**
     * Alle Listen des Geräts als Echtzeit-Flow: lokale (nicht geteilte) +
     * remote (geteilte, Mitgliedschaft über memberIds) Listen zusammengeführt.
     *
     * Sollte dieselbe ID ausnahmsweise an beiden Orten liegen – etwa weil ein
     * [shareList] mitten im Upload abgebrochen ist – gewinnt die geteilte
     * Version. Ohne diese Deduplizierung käme dieselbe ID zweimal in der UI an,
     * was in einer LazyColumn mit `key = { it.id }` zum Absturz führt.
     */
    fun observeLists(): Flow<List<TodoList>> {
        val localFlow = listDao.observeLists().map { entities -> entities.map { it.toTodoList() } }
        val remoteFlow = observeRemoteLists()
        return combine(localFlow, remoteFlow) { local, remote ->
            val remoteIds = remote.mapTo(HashSet()) { it.id }
            (remote + local.filterNot { it.id in remoteIds })
                .sortedByDescending { it.createdAt.seconds }
        }
    }

    /**
     * Ein Fehler darf diesen Flow NICHT schließen: `observeLists` kombiniert ihn
     * mit den lokalen Listen, und ein geschlossener Flow würde das combine
     * beenden – die rein lokalen Listen wären dann ebenfalls verschwunden,
     * obwohl sie mit Firestore nichts zu tun haben.
     *
     * Stattdessen wird der bisherige Stand beibehalten. Nur wenn noch nie Daten
     * ankamen (z.B. fehlender Composite-Index oder Permission-Denied), wird
     * einmalig eine leere Liste gemeldet, damit das combine nicht ewig wartet.
     */
    private fun observeRemoteLists(): Flow<List<TodoList>> = callbackFlow {
        var received = false
        val registration: ListenerRegistration = listsRef
            .whereArrayContains("memberIds", deviceId)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.w(TAG, "Listen-Listener fehlgeschlagen", error)
                    if (!received) {
                        received = true
                        trySend(emptyList())
                    }
                    return@addSnapshotListener
                }
                received = true
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
            listsRef.document(listId).update("name", newName.trim()).await()
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
        deleteRemoteListCompletely(listId)
    }

    /**
     * Liste in Firestore hochladen und damit teilbar machen ("Teilen"-Regler an).
     * Nimmt eine bisher lokale Liste inkl. aller Todos, legt sie unter derselben
     * ID in Firestore an und entfernt anschließend die lokale Kopie.
     *
     * Reihenfolge ist bewusst "erst hoch, dann lokal weg": bricht der Upload ab,
     * sind die Daten noch lokal vorhanden. Damit dabei keine halb hochgeladene
     * Geisterliste in Firestore zurückbleibt, wird im Fehlerfall aufgeräumt.
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

        try {
            setInChunks(
                localTodos.map { entity ->
                    val todo = entity.toTodoItem()
                    todosRef(list.id).document(todo.id) to todo
                }
            )
        } catch (e: Exception) {
            Log.w(TAG, "Upload der Todos fehlgeschlagen, Rollback", e)
            runCatching { deleteRemoteListCompletely(list.id) }
            throw e
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
        // Einmal lesen und für beides verwenden: lokale Kopie UND Löschliste.
        val todosSnapshot = todosRef(list.id).get().await()
        val todos = todosSnapshot.documents.mapNotNull { doc ->
            doc.toObject(TodoItem::class.java)?.copy(id = doc.id)
        }

        listDao.insertList(
            LocalListEntity(
                id = list.id,
                name = list.name,
                // Besitz bleibt beim ursprünglichen Ersteller, sofern das dieses
                // Gerät ist; sonst übernimmt der ausführende Admin.
                createdBy = if (list.createdBy.isNotBlank()) list.createdBy else deviceId,
                creatorName = list.displayNameForOrFallback(list.createdBy.ifBlank { deviceId }),
                createdAt = list.createdAt.toDate().time,
                color = list.color,
                icon = list.icon
            )
        )
        if (todos.isNotEmpty()) {
            todoDao.insertTodos(todos.map { it.toLocalEntity(list.id) })
        }

        // Erst jetzt remote löschen – schlägt das fehl, sind die Daten lokal schon sicher.
        runCatching { revokeInvitesFor(list.id) }
            .onFailure { Log.w(TAG, "Einladungen zu ${list.id} nicht widerrufen", it) }
        deleteInChunks(todosSnapshot.documents.map { it.reference })
        listsRef.document(list.id).delete().await()
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

        val todos = todosRef(list.id).get().await()
        setInChunks(
            todos.documents.mapNotNull { doc ->
                val todo = doc.toObject(TodoItem::class.java) ?: return@mapNotNull null
                todosRef(newDoc.id).document() to todo
            }
        )
        return newDoc.id
    }

    // ─── Einladungen ─────────────────────────────────────────────────────────

    /**
     * Erzeugt einen neuen Einladungscode und macht alle bisherigen Codes dieser
     * Liste ungültig. Es ist also immer höchstens ein Code gleichzeitig aktiv.
     *
     * Der Code ist die Dokument-ID unter `invites/`. Da die Anzeigedaten der
     * Liste redundant im Einladungsdokument liegen, kommt der Einladungs-Screen
     * ohne Lesezugriff auf das Listendokument aus – Nicht-Mitglieder sehen die
     * Liste selbst also nie.
     */
    suspend fun createInvite(list: TodoList, validDays: Int = DEFAULT_INVITE_DAYS): Invite {
        revokeInvitesFor(list.id)

        val expiresAt = Calendar.getInstance().apply {
            add(Calendar.DAY_OF_YEAR, validDays)
        }.time

        var lastError: Exception? = null
        repeat(INVITE_CODE_ATTEMPTS) {
            val code = generateInviteCode()
            val invite = Invite(
                code = code,
                listId = list.id,
                listName = list.name,
                listColor = list.color,
                listIcon = list.icon,
                memberCount = list.memberIds.size,
                createdBy = deviceId,
                createdAt = Timestamp.now(),
                expiresAt = Timestamp(expiresAt),
                revoked = false,
            )
            val ref = invitesRef.document(code)
            try {
                // Transaktion, damit ein (extrem unwahrscheinlicher) Kollisionsfall
                // nicht die fremde Einladung überschreibt.
                db.runTransaction { transaction ->
                    if (transaction.get(ref).exists()) {
                        throw IllegalStateException("Code bereits vergeben")
                    }
                    transaction.set(ref, invite)
                    null
                }.await()
                return invite
            } catch (e: Exception) {
                Log.w(TAG, "Einladungscode $code konnte nicht angelegt werden", e)
                lastError = e
            }
        }
        throw lastError ?: IllegalStateException("Einladungscode konnte nicht erzeugt werden")
    }

    /**
     * Aktuell gültige Einladung einer Liste (für die Teilen-Ansicht), oder null,
     * wenn es keine gibt bzw. die vorhandene abgelaufen oder widerrufen ist.
     */
    suspend fun activeInvite(listId: String): Invite? {
        val snapshot = invitesRef
            .whereEqualTo("listId", listId)
            .whereEqualTo("revoked", false)
            .get()
            .await()
        return snapshot.documents
            .mapNotNull { doc -> doc.toObject(Invite::class.java)?.copy(code = doc.id) }
            .filter { it.isUsable }
            .maxByOrNull { it.createdAt.seconds }
    }

    /** Alle Einladungen einer Liste entwerten (z. B. beim Erzeugen eines neuen Codes). */
    suspend fun revokeInvitesFor(listId: String) {
        val snapshot = invitesRef
            .whereEqualTo("listId", listId)
            .whereEqualTo("revoked", false)
            .get()
            .await()
        snapshot.documents.chunked(BATCH_LIMIT).forEach { chunk ->
            val batch = db.batch()
            chunk.forEach { batch.update(it.reference, "revoked", true) }
            batch.commit().await()
        }
    }

    /**
     * Einladung ansehen, ohne beizutreten (für den Einladungs-Screen).
     * Gibt null zurück, wenn der Code unbekannt, widerrufen oder abgelaufen ist.
     */
    suspend fun previewInvite(rawCode: String): Invite? {
        val code = rawCode.normalizeInviteCode()
        if (code.isBlank()) return null
        val doc = invitesRef.document(code).get().await()
        if (!doc.exists()) return null
        val invite = doc.toObject(Invite::class.java)?.copy(code = doc.id) ?: return null
        return invite.takeIf { it.isUsable }
    }

    /**
     * Über einen Einladungscode einer Liste beitreten. Der eigene Anzeigename
     * wird dabei mit in die Liste übernommen.
     *
     * `arrayUnion` statt read-modify-write: treten zwei Personen gleichzeitig
     * bei, würde die gelesene Mitgliederliste sonst gegenseitig überschrieben
     * und einer der beiden wieder herausfliegen.
     *
     * Das Feld `joinedVia` trägt den verwendeten Code – die Security Rules
     * prüfen darüber, ob eine gültige Einladung vorliegt, bevor sie das
     * Hinzufügen zur Mitgliederliste erlauben. Erst danach besteht Lesezugriff
     * auf das Listendokument, deshalb wird es anschließend geladen.
     */
    suspend fun joinWithInvite(rawCode: String, displayName: String): TodoList? {
        val invite = previewInvite(rawCode) ?: return null
        val docRef = listsRef.document(invite.listId)

        docRef.update(
            mapOf(
                "memberIds" to FieldValue.arrayUnion(deviceId),
                "memberNames.$deviceId" to displayName,
                "joinedVia" to invite.code,
            )
        ).await()

        val doc = docRef.get().await()
        return doc.toObject(TodoList::class.java)?.copy(id = doc.id, isShared = true)
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
        if (list.memberIds.filter { it != deviceId }.isEmpty()) {
            // Letzte Person – Liste löschen
            deleteRemoteListCompletely(listId)
        } else {
            docRef.update(
                mapOf(
                    "memberIds" to FieldValue.arrayRemove(deviceId),
                    "adminIds"  to FieldValue.arrayRemove(deviceId),
                    "mutedBy"   to FieldValue.arrayRemove(deviceId),
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
        listsRef.document(listId).update(
            mapOf(
                "memberIds" to FieldValue.arrayRemove(memberId),
                "adminIds"  to FieldValue.arrayRemove(memberId),
                "mutedBy"   to FieldValue.arrayRemove(memberId),
                "memberNames.$memberId" to FieldValue.delete()
            )
        ).await()
    }

    /**
     * Besitz übertragen. Läuft als Transaktion, damit der bisherige Besitzer
     * nicht seine Admin-Rechte verliert, wenn parallel jemand die Adminliste
     * ändert.
     */
    suspend fun transferOwnership(listId: String, newOwnerId: String) {
        val docRef = listsRef.document(listId)
        db.runTransaction { transaction ->
            val snapshot = transaction.get(docRef)
            val list = snapshot.toObject(TodoList::class.java) ?: return@runTransaction null
            if (newOwnerId !in list.memberIds) return@runTransaction null
            val newAdminIds = (list.adminIds + deviceId - newOwnerId).distinct()
            transaction.update(
                docRef,
                mapOf(
                    "createdBy" to newOwnerId,
                    "adminIds"  to newAdminIds
                )
            )
            null
        }.await()
    }

    // ─── Todos ───────────────────────────────────────────────────────────────

    /**
     * Anzahl erledigter/aller Todos einer Liste als Echtzeit-Flow (für die Listen-Übersicht).
     * Wechselt wie [observeTodos] automatisch zwischen lokaler und geteilter Quelle.
     */
    fun observeTodoCounts(listId: String): Flow<ListCounts> =
        listDao.observeIsLocal(listId)
            .map { it > 0 }
            .distinctUntilChanged()
            .flatMapLatest { isLocal ->
                if (isLocal) {
                    todoDao.observeTodos(listId).map { todos ->
                        ListCounts(done = todos.count { it.isDone }, total = todos.size)
                    }
                } else {
                    observeRemoteTodoCounts(listId)
                }
            }

    private fun observeRemoteTodoCounts(listId: String): Flow<ListCounts> = callbackFlow {
        var received = false
        val registration: ListenerRegistration = todosRef(listId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.w(TAG, "Zähler-Listener für $listId fehlgeschlagen", error)
                    if (!received) {
                        received = true
                        trySend(ListCounts())
                    }
                    return@addSnapshotListener
                }
                received = true
                val docs  = snapshot?.documents ?: emptyList()
                val total = docs.size
                val done  = docs.count { it.getBoolean("isDone") == true }
                trySend(ListCounts(done, total))
            }
        awaitClose { registration.remove() }
    }

    /**
     * Alle Todos einer Liste als Echtzeit-Flow.
     *
     * Die Quelle (Room oder Firestore) wird nicht einmalig festgelegt, sondern
     * an das Vorhandensein der lokalen Zeile gekoppelt: schaltet jemand den
     * "Teilen"-Regler um, während die Detailansicht offen ist, wechselt der
     * Stream automatisch mit.
     */
    fun observeTodos(listId: String): Flow<List<TodoItem>> =
        listDao.observeIsLocal(listId)
            .map { it > 0 }
            .distinctUntilChanged()
            .flatMapLatest { isLocal ->
                if (isLocal) {
                    todoDao.observeTodos(listId).map { entities -> entities.map { it.toTodoItem() } }
                } else {
                    observeRemoteTodos(listId)
                }
            }

    private fun observeRemoteTodos(listId: String): Flow<List<TodoItem>> = callbackFlow {
        var received = false
        val registration: ListenerRegistration = todosRef(listId)
            .orderBy("position", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.w(TAG, "Todo-Listener für $listId fehlgeschlagen", error)
                    if (!received) {
                        received = true
                        trySend(emptyList())
                    }
                    return@addSnapshotListener
                }
                received = true
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
            val position = todoDao.maxPosition(listId) + 1L
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
                position = position
            )
            todoDao.insertTodo(todo.toLocalEntity(listId))
            return
        }

        val todo = TodoItem(
            title = title.trim(),
            description = description.trim(),
            isDone = false,
            priority = priority.name,
            dueDate = dueDate,
            assignedTo = assignedTo,
            reminderMinutes = reminderMinutes,
            createdBy = deviceId,
            position = nextRemotePosition(listId)
        )
        todosRef(listId).add(todo).await()
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
        todosRef(listId).document(todo.id).update(updates).await()
    }

    /**
     * Todo löschen.
     */
    suspend fun deleteTodo(listId: String, todoId: String) {
        if (isLocalList(listId)) {
            todoDao.deleteTodo(todoId)
            return
        }
        todosRef(listId).document(todoId).delete().await()
    }

    /**
     * Ein zuvor gelöschtes Todo mit exakt derselben ID wiederherstellen (Undo).
     */
    suspend fun restoreTodo(listId: String, todo: TodoItem) {
        if (isLocalList(listId)) {
            todoDao.insertTodo(todo.toLocalEntity(listId))
            return
        }
        todosRef(listId).document(todo.id).set(todo).await()
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
        todosRef(listId).document(todoId).update("title", newTitle.trim()).await()
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
            // Neue Fälligkeit bedeutet: die Erinnerung muss erneut ausgelöst werden.
            "reminderSent"    to false,
        )
        todosRef(listId).document(todoId).update(updates).await()
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
            val newPosition = todoDao.maxPosition(toListId) + 1L
            todoDao.updateTodo(entity.copy(listId = toListId, position = newPosition))
            return
        }
        if (!fromLocal && !toLocal) {
            val newPosition = nextRemotePosition(toListId)
            // Als Batch, damit das Todo nicht doppelt existiert oder verloren geht,
            // falls zwischen Schreiben und Löschen etwas schiefgeht.
            val batch = db.batch()
            batch.set(todosRef(toListId).document(todo.id), todo.copy(position = newPosition))
            batch.delete(todosRef(fromListId).document(todo.id))
            batch.commit().await()
            return
        }
        if (fromLocal && !toLocal) {
            val entity = todoDao.getTodo(todo.id) ?: return
            val newPosition = nextRemotePosition(toListId)
            val movedTodo = entity.toTodoItem().copy(position = newPosition)
            todosRef(toListId).document(todo.id).set(movedTodo).await()
            todoDao.deleteTodo(todo.id)
            return
        }
        // remote -> lokal: erst lokal sichern, dann remote löschen
        val newPosition = todoDao.maxPosition(toListId) + 1L
        todoDao.insertTodo(todo.copy(position = newPosition).toLocalEntity(toListId))
        todosRef(fromListId).document(todo.id).delete().await()
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
        todosRef(listId).document(todoId).update("subtasks", subtasks).await()
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
        todosRef(listId).document(todoId).update("comments", FieldValue.arrayUnion(comment)).await()
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
        var received = false
        val registration = notificationsRef
            .whereEqualTo("recipientId", deviceId)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .limit(50)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.w(TAG, "Benachrichtigungs-Listener fehlgeschlagen", error)
                    if (!received) {
                        received = true
                        trySend(emptyList())
                    }
                    return@addSnapshotListener
                }
                received = true
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
        ids.chunked(BATCH_LIMIT).forEach { chunk ->
            val batch = db.batch()
            chunk.forEach { batch.update(notificationsRef.document(it), "isRead", true) }
            batch.commit().await()
        }
    }
}
