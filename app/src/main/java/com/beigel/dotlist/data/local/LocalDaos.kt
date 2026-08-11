package com.beigel.dotlist.data.local

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

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertList(list: LocalListEntity)

    @Query("UPDATE local_lists SET name = :name WHERE id = :id")
    suspend fun renameList(id: String, name: String)

    @Query("DELETE FROM local_lists WHERE id = :id")
    suspend fun deleteList(id: String)
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
}
