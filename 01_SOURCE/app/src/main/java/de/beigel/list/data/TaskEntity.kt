package de.beigel.list.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import kotlinx.serialization.Serializable
import java.time.LocalDate
import java.time.LocalDateTime

@Entity(tableName = "tasks")
@TypeConverters(Converters::class)
@Serializable
data class TaskEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    val title: String,
    val description: String = "",

    // Priorität und Status
    val priority: TaskPriority = TaskPriority.MEDIUM,
    val isCompleted: Boolean = false,
    val isInTodayList: Boolean = false,

    // Zeitangaben
    val createdAt: LocalDateTime = LocalDateTime.now(),
    val updatedAt: LocalDateTime = LocalDateTime.now(),
    val completedAt: LocalDateTime? = null,
    val dueDate: LocalDate? = null,

    // Zeitschätzung
    val estimatedMinutes: Int? = null,
    val actualMinutes: Int? = null,

    // Kategorisierung
    val energyLevel: EnergyLevel = EnergyLevel.MEDIUM,
    val context: TaskContext = TaskContext.NONE,
    val tags: List<String> = emptyList(),

    // Wiederholung
    val recurrencePattern: RecurrencePattern? = null,
    val parentTaskId: Long? = null,

    // Smart Focus
    val urgencyLevel: UrgencyLevel = UrgencyLevel.NORMAL,
    val smartScore: Double = 0.0,
    val lastShownInTodayList: LocalDate? = null,

    // Reminder
    val reminderDateTime: LocalDateTime? = null,
    val hasNotificationShown: Boolean = false
)

enum class TaskPriority {
    LOW, MEDIUM, HIGH
}

enum class EnergyLevel {
    LOW, MEDIUM, HIGH
}

enum class TaskContext {
    NONE, WORK, HOME, ERRANDS, CALLS, COMPUTER, EXERCISE, LEARNING, CREATIVE
}

enum class RecurrencePattern {
    DAILY, WEEKLY, MONTHLY
}

enum class UrgencyLevel {
    VERY_LOW, LOW, NORMAL, HIGH, URGENT
}