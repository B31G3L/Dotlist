package de.beigel.list.data

import androidx.room.*
import java.time.LocalDate
import java.time.LocalDateTime

@Entity(
    tableName = "tasks",
    indices = [
        Index(value = ["date"]),
        Index(value = ["isCompleted"]),
        Index(value = ["date", "isCompleted"]),
        Index(value = ["dueDate"]),
        Index(value = ["priority"]),
        Index(value = ["context"])
    ]
)
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
    val lastModified: String = LocalDateTime.now().toString(),

    // Neue Smart Focus Features
    val dueDate: String? = null,
    val estimatedMinutes: Int? = null,
    val actualMinutes: Int? = null,
    val energyLevel: EnergyLevel = EnergyLevel.MEDIUM,
    val context: TaskContext? = null,
    val isRecurring: Boolean = false,
    val recurrencePattern: RecurrencePattern? = null,
    val tags: String = "", // Komma-separierte Liste
    val reminderTime: String? = null,

    // Smart Scoring Hilfswerte
    val smartScore: Double = 0.0,
    val lastScoreUpdate: String = LocalDateTime.now().toString()
)

enum class TaskPriority(val displayName: String, val color: Long, val scoreMultiplier: Double) {
    HIGH("Hoch", 0xFFE53E3E, 3.0),
    MEDIUM("Mittel", 0xFF009966, 2.0),
    LOW("Niedrig", 0xFF4A5568, 1.0)
}

enum class EnergyLevel(val displayName: String, val icon: String, val score: Int) {
    LOW("Wenig Energie", "🔋", 1),
    MEDIUM("Normale Energie", "🔋🔋", 2),
    HIGH("Viel Energie", "🔋🔋🔋", 3)
}

enum class TaskContext(val displayName: String, val icon: String, val color: Long) {
    WORK("Arbeit", "💼", 0xFF1E88E5),
    HOME("Zuhause", "🏠", 0xFF43A047),
    ERRANDS("Besorgungen", "🛒", 0xFFE53935),
    CALLS("Anrufe", "📞", 0xFF8E24AA),
    COMPUTER("Computer", "💻", 0xFF00ACC1),
    EXERCISE("Sport", "🏃", 0xFFFF9800),
    LEARNING("Lernen", "📚", 0xFF5E35B1),
    CREATIVE("Kreativ", "🎨", 0xFFE91E63)
}

enum class RecurrencePattern(val displayName: String, val daysInterval: Int) {
    DAILY("Täglich", 1),
    WEEKLY("Wöchentlich", 7),
    BIWEEKLY("Alle 2 Wochen", 14),
    MONTHLY("Monatlich", 30),
    QUARTERLY("Vierteljährlich", 90),
    YEARLY("Jährlich", 365)
}

// Datenklassen für UI
data class TaskWithMetrics(
    val task: TaskEntity,
    val smartScore: Double,
    val urgencyLevel: UrgencyLevel,
    val timeToComplete: Int?, // in Minuten
    val energyRequired: EnergyLevel
)

enum class UrgencyLevel(val displayName: String, val color: Long, val icon: String) {
    OVERDUE("Überfällig", 0xFFE53E3E, "🚨"),
    TODAY("Heute", 0xFFFF9800, "🔥"),
    TOMORROW("Morgen", 0xFFFFEB3B, "⏰"),
    THIS_WEEK("Diese Woche", 0xFF2196F3, "📅"),
    FUTURE("Später", 0xFF4CAF50, "🗓️"),
    NO_DUE_DATE("Ohne Frist", 0xFF9E9E9E, "📝")
}

// Extension Functions
fun TaskEntity.toTaskWithMetrics(calculator: de.beigel.list.utils.TaskPriorityCalculator): TaskWithMetrics {
    val score = calculator.calculateSmartScore(this)
    val urgency = getUrgencyLevel()

    return TaskWithMetrics(
        task = this,
        smartScore = score,
        urgencyLevel = urgency,
        timeToComplete = estimatedMinutes,
        energyRequired = energyLevel
    )
}

fun TaskEntity.getUrgencyLevel(): UrgencyLevel {
    return dueDate?.let { dueDateStr ->
        val dueDate = LocalDate.parse(dueDateStr)
        val today = LocalDate.now()
        val daysUntilDue = java.time.temporal.ChronoUnit.DAYS.between(today, dueDate)

        when {
            daysUntilDue < 0 -> UrgencyLevel.OVERDUE
            daysUntilDue == 0L -> UrgencyLevel.TODAY
            daysUntilDue == 1L -> UrgencyLevel.TOMORROW
            daysUntilDue <= 7 -> UrgencyLevel.THIS_WEEK
            else -> UrgencyLevel.FUTURE
        }
    } ?: UrgencyLevel.NO_DUE_DATE
}

fun TaskEntity.getTags(): List<String> {
    return if (tags.isBlank()) emptyList() else tags.split(",").map { it.trim() }
}

fun TaskEntity.hasTag(tag: String): Boolean {
    return getTags().contains(tag)
}

fun TaskEntity.isOverdue(): Boolean {
    return dueDate?.let { dueDateStr ->
        LocalDate.parse(dueDateStr).isBefore(LocalDate.now())
    } ?: false
}

fun TaskEntity.isDueToday(): Boolean {
    return dueDate?.let { dueDateStr ->
        LocalDate.parse(dueDateStr).isEqual(LocalDate.now())
    } ?: false
}

fun TaskEntity.isQuickWin(): Boolean {
    return estimatedMinutes != null && estimatedMinutes <= 15
}

fun TaskEntity.requiresHighEnergy(): Boolean {
    return energyLevel == EnergyLevel.HIGH ||
            (estimatedMinutes != null && estimatedMinutes > 120)
}