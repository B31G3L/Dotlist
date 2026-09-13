package com.beigel.list2share.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Lokal (nur auf diesem Gerät) gespeicherte Liste.
 *
 * Eine Liste existiert entweder hier ODER in Firestore (`lists/{id}`) – niemals
 * an beiden Orten gleichzeitig. Standardmäßig landet jede neu erstellte Liste
 * hier und wird erst bei aktiviertem "Teilen"-Regler nach Firestore
 * hochgeladen (siehe [com.beigel.list2share.repository.TodoRepository.shareList]).
 */
@Entity(tableName = "local_lists")
data class LocalListEntity(
    @PrimaryKey val id: String,
    val name: String,
    val createdBy: String,
    val creatorName: String,
    val createdAt: Long,
    val color: String,
    val icon: String,
    /** Art der Liste, siehe ListMode. Leer/unbekannt gilt als AUFGABEN. */
    val mode: String = "AUFGABEN",
)

/**
 * Lokal gespeichertes Todo innerhalb einer lokalen Liste.
 * subtasksJson/commentsJson enthalten die jeweilige Liste als JSON-Array
 * (siehe LocalConversions.kt), um keine zusätzlichen Tabellen zu benötigen.
 */
@Entity(tableName = "local_todos")
data class LocalTodoEntity(
    @PrimaryKey val id: String,
    val listId: String,
    val title: String,
    val description: String,
    val isDone: Boolean,
    val priority: String,
    val dueDate: Long?,
    val assignedTo: String?,
    val reminderMinutes: Int?,
    val reminderSent: Boolean,
    val createdBy: String,
    val createdAt: Long,
    val doneBy: String?,
    val doneAt: Long?,
    val position: Long,
    val subtasksJson: String = "[]",
    val commentsJson: String = "[]",
    /** Mengenangabe als Freitext, nur im Einkaufsmodus genutzt. */
    val quantity: String = "",
    /** Geschätzter Preis, nur im Anschaffungsmodus genutzt. */
    val price: Double? = null,
    /** Link zum Angebot, nur im Anschaffungsmodus genutzt. */
    val link: String = "",
)
