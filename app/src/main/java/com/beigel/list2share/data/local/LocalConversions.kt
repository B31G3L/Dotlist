package com.beigel.list2share.data.local

import com.beigel.list2share.data.Comment
import com.beigel.list2share.data.Subtask
import com.beigel.list2share.data.TodoItem
import com.beigel.list2share.data.TodoList
import com.google.firebase.Timestamp
import org.json.JSONArray
import org.json.JSONObject
import java.util.Date

// ─── TodoList ↔ LocalListEntity ─────────────────────────────────────────────

fun LocalListEntity.toTodoList(): TodoList = TodoList(
    id = id,
    name = name,
    memberIds = listOf(createdBy),
    memberNames = mapOf(createdBy to creatorName),
    adminIds = emptyList(),
    createdBy = createdBy,
    createdAt = Timestamp(Date(createdAt)),
    color = color,
    icon = icon,
    mutedBy = emptyList(),
    isShared = false
)

// ─── TodoItem ↔ LocalTodoEntity ─────────────────────────────────────────────

fun LocalTodoEntity.toTodoItem(): TodoItem = TodoItem(
    id = id,
    title = title,
    description = description,
    isDone = isDone,
    priority = priority,
    dueDate = dueDate?.let { Timestamp(Date(it)) },
    assignedTo = assignedTo,
    reminderMinutes = reminderMinutes,
    reminderSent = reminderSent,
    createdBy = createdBy,
    createdAt = Timestamp(Date(createdAt)),
    doneBy = doneBy,
    doneAt = doneAt?.let { Timestamp(Date(it)) },
    position = position,
    subtasks = subtasksJson.toSubtasks(),
    comments = commentsJson.toComments()
)

fun TodoItem.toLocalEntity(listId: String): LocalTodoEntity = LocalTodoEntity(
    id              = id,
    listId          = listId,
    title           = title,
    description     = description,
    isDone          = isDone,
    priority        = priority,
    dueDate         = dueDate?.toDate()?.time,
    assignedTo      = assignedTo,
    reminderMinutes = reminderMinutes,
    reminderSent    = reminderSent,
    createdBy       = createdBy,
    createdAt       = createdAt.toDate().time,
    doneBy          = doneBy,
    doneAt          = doneAt?.toDate()?.time,
    position        = position,
    subtasksJson    = subtasks.toJson(),
    commentsJson    = comments.toJson()
)

// ─── Subtasks/Comments als JSON (kein Untertabellen-Aufwand nötig) ──────────

@JvmName("subtasksToJson")
fun List<Subtask>.toJson(): String {
    val arr = JSONArray()
    forEach { s ->
        arr.put(JSONObject().apply {
            put("id", s.id)
            put("title", s.title)
            put("isDone", s.isDone)
        })
    }
    return arr.toString()
}

fun String.toSubtasks(): List<Subtask> {
    if (isBlank()) return emptyList()
    val arr = JSONArray(this)
    return (0 until arr.length()).map { i ->
        val o = arr.getJSONObject(i)
        Subtask(
            id = o.getString("id"),
            title = o.getString("title"),
            isDone = o.optBoolean("isDone", false)
        )
    }
}

@JvmName("commentsToJson")
fun List<Comment>.toJson(): String {
    val arr = JSONArray()
    forEach { c ->
        arr.put(JSONObject().apply {
            put("id", c.id)
            put("authorId", c.authorId)
            put("text", c.text)
            put("createdAt", c.createdAt.toDate().time)
        })
    }
    return arr.toString()
}

fun String.toComments(): List<Comment> {
    if (isBlank()) return emptyList()
    val arr = JSONArray(this)
    return (0 until arr.length()).map { i ->
        val o = arr.getJSONObject(i)
        Comment(
            id = o.getString("id"),
            authorId = o.getString("authorId"),
            text = o.getString("text"),
            createdAt = Timestamp(Date(o.getLong("createdAt")))
        )
    }
}
