package com.beigel.list2share.data.local

import android.content.Context
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface LocalListDao {

    @Query("SELECT * FROM local_lists ORDER BY createdAt DESC")
    fun observeLists(): Flow<List<LocalListEntity>>

    @Query("SELECT * FROM local_lists WHERE id = :id LIMIT 1")
    suspend fun getList(id: String): LocalListEntity?

    /** Einmaliger Schnappschuss aller lokalen Listen (für die Cloud-Migration). */
    @Query("SELECT * FROM local_lists ORDER BY createdAt ASC")
    suspend fun getListsOnce(): List<LocalListEntity>

    /**
     * Liegt diese Liste (noch) lokal? Als Flow, damit ein laufender Todo-Stream
     * automatisch von Room auf Firestore umschaltet, sobald die Liste geteilt
     * wird – und umgekehrt.
     */
    @Query("SELECT COUNT(*) FROM local_lists WHERE id = :id")
    fun observeIsLocal(id: String): Flow<Int>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertList(list: LocalListEntity)

    @Query("UPDATE local_lists SET name = :name WHERE id = :id")
    suspend fun renameList(id: String, name: String)

    @Query("DELETE FROM local_lists WHERE id = :id")
    suspend fun deleteList(id: String)

    /** Nach einem Wechsel der Firebase-UID: alte UID auf die neue umschreiben. */
    @Query("UPDATE local_lists SET createdBy = :newUid WHERE createdBy = :oldUid")
    suspend fun reassignCreator(oldUid: String, newUid: String)

    /** Anzeigename des Besitzers aktualisieren (z. B. Google-Name nach dem Login). */
    @Query("UPDATE local_lists SET creatorName = :name WHERE createdBy = :uid")
    suspend fun updateCreatorName(uid: String, name: String)
}

@Dao
interface LocalTodoDao {

    @Query("SELECT * FROM local_todos WHERE listId = :listId ORDER BY position ASC")
    fun observeTodos(listId: String): Flow<List<LocalTodoEntity>>

    @Query("SELECT * FROM local_todos WHERE listId = :listId ORDER BY position ASC")
    suspend fun getTodosOnce(listId: String): List<LocalTodoEntity>

    @Query("SELECT * FROM local_todos WHERE id = :id LIMIT 1")
    suspend fun getTodo(id: String): LocalTodoEntity?

    @Query("SELECT COUNT(*) FROM local_todos WHERE listId = :listId")
    suspend fun countTodos(listId: String): Int

    /**
     * Höchste vergebene Position. Basis für die nächste freie Position – im
     * Gegensatz zu COUNT(*) entstehen so keine doppelten Positionen, nachdem
     * zwischendurch Todos gelöscht wurden.
     */
    @Query("SELECT COALESCE(MAX(position), -1) FROM local_todos WHERE listId = :listId")
    suspend fun maxPosition(listId: String): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTodo(todo: LocalTodoEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTodos(todos: List<LocalTodoEntity>)

    @Update
    suspend fun updateTodo(todo: LocalTodoEntity)

    @Query("DELETE FROM local_todos WHERE id = :id")
    suspend fun deleteTodo(id: String)

    @Query("DELETE FROM local_todos WHERE listId = :listId")
    suspend fun deleteTodosForList(listId: String)

    /** Nach einem Wechsel der Firebase-UID: alle Referenzen umschreiben. */
    @Query("UPDATE local_todos SET createdBy = :newUid WHERE createdBy = :oldUid")
    suspend fun reassignCreator(oldUid: String, newUid: String)

    @Query("UPDATE local_todos SET assignedTo = :newUid WHERE assignedTo = :oldUid")
    suspend fun reassignAssignee(oldUid: String, newUid: String)

    @Query("UPDATE local_todos SET doneBy = :newUid WHERE doneBy = :oldUid")
    suspend fun reassignDoneBy(oldUid: String, newUid: String)
}

/**
 * Lokale Listen und Todos referenzieren die Firebase-UID (createdBy, assignedTo,
 * doneBy). Wechselt die UID – etwa weil nach einer Neuinstallation der frische
 * anonyme Account zugunsten des bestehenden Google-Accounts verworfen wurde –,
 * würden sonst eigene Listen plötzlich fremd wirken (roleOf() liefert MITGLIED
 * statt BESITZER).
 *
 * Reines UPDATE, keine Schemaänderung: es ist keine Room-Migration nötig.
 */
object LocalOwnership {

    suspend fun migrate(context: Context, oldUid: String, newUid: String, newName: String? = null) {
        if (oldUid.isBlank() || oldUid == newUid) return
        val db = AppDatabase.getInstance(context)
        db.listDao().reassignCreator(oldUid, newUid)
        db.todoDao().reassignCreator(oldUid, newUid)
        db.todoDao().reassignAssignee(oldUid, newUid)
        db.todoDao().reassignDoneBy(oldUid, newUid)
        newName?.takeIf { it.isNotBlank() }?.let { db.listDao().updateCreatorName(newUid, it) }
    }
}