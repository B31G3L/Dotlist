package de.beigel.list.data

import androidx.room.*
import java.time.LocalDate
import java.time.LocalDateTime

@Entity(tableName = "tasks")
data class TaskEntity(
    @PrimaryKey val id: String = java.util.UUID.randomUUID().toString(),
    val title: String,
    val description: String = "",
    val isCompleted: Boolean = false,
    val priority: TaskPriority = TaskPriority.MEDIUM,
    val position: Int = 0,
    val date: String = LocalDate.now().toString(),
    val createdAt: String = LocalDateTime.now().toString(),
    val completedAt: String? = null,
    val isInDailyList: Boolean = true,
    val backlogPosition: Int = 0
)

enum class TaskPriority(val displayName: String, val color: Long) {
    HIGH("Hoch", 0xFFE53E3E),
    MEDIUM("Mittel", 0xFF009966),
    LOW("Niedrig", 0xFF4A5568)
}