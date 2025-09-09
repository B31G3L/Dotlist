package de.beigel.list.utils

import de.beigel.list.data.*
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.temporal.ChronoUnit
import kotlin.math.*

class TaskPriorityCalculator {

    companion object {
        // Gewichtungen für verschiedene Faktoren (müssen zusammen 100% ergeben)
        private const val PRIORITY_WEIGHT = 0.25 // 25%
        private const val URGENCY_WEIGHT = 0.35  // 35%
        private const val AGE_WEIGHT = 0.15       // 15%
        private const val DURATION_WEIGHT = 0.10  // 10%
        private const val ENERGY_WEIGHT = 0.10    // 10%
        private const val CONTEXT_WEIGHT = 0.05   // 5%

        // Maximale Punktzahl pro Kategorie
        private const val MAX_SCORE_PER_CATEGORY = 100.0

        // Zeitbasierte Faktoren
        private const val OVERDUE_BONUS_MULTIPLIER = 1.5
        private const val TODAY_BONUS_MULTIPLIER = 1.3
        private const val TOMORROW_BONUS_MULTIPLIER = 1.1
    }

    fun calculateSmartScore(
        task: TaskEntity,
        currentTime: LocalDateTime = LocalDateTime.now(),
        userContext: UserContext? = null
    ): Double {
        val priorityScore = calculatePriorityScore(task)
        val urgencyScore = calculateUrgencyScore(task, currentTime)
        val ageScore = calculateAgeScore(task, currentTime)
        val durationScore = calculateDurationScore(task)
        val energyScore = calculateEnergyScore(task, currentTime, userContext)
        val contextScore = calculateContextScore(task, currentTime, userContext)

        val baseScore = (priorityScore * PRIORITY_WEIGHT) +
                (urgencyScore * URGENCY_WEIGHT) +
                (ageScore * AGE_WEIGHT) +
                (durationScore * DURATION_WEIGHT) +
                (energyScore * ENERGY_WEIGHT) +
                (contextScore * CONTEXT_WEIGHT)

        // Anpassungen basierend auf speziellen Umständen
        val adjustedScore = applySpecialAdjustments(task, baseScore, currentTime)

        return adjustedScore.coerceIn(0.0, 100.0)
    }

    private fun calculatePriorityScore(task: TaskEntity): Double {
        return when (task.priority) {
            TaskPriority.HIGH -> 100.0
            TaskPriority.MEDIUM -> 60.0
            TaskPriority.LOW -> 30.0
        }
    }

    private fun calculateUrgencyScore(task: TaskEntity, currentTime: LocalDateTime): Double {
        return task.dueDate?.let { dueDateStr ->
            try {
                val dueDate = LocalDate.parse(dueDateStr)
                val today = currentTime.toLocalDate()
                val daysUntilDue = ChronoUnit.DAYS.between(today, dueDate)

                when {
                    daysUntilDue < -7 -> 100.0 // Sehr überfällig
                    daysUntilDue < 0 -> 95.0   // Überfällig
                    daysUntilDue == 0L -> 90.0 // Heute fällig
                    daysUntilDue == 1L -> 80.0 // Morgen fällig
                    daysUntilDue <= 3 -> 70.0  // Diese Woche
                    daysUntilDue <= 7 -> 50.0  // Nächste Woche
                    daysUntilDue <= 14 -> 30.0 // In 2 Wochen
                    daysUntilDue <= 30 -> 20.0 // Diesen Monat
                    else -> 10.0               // Fernere Zukunft
                }
            } catch (e: Exception) {
                40.0 // Fallback bei parsing-Fehlern
            }
        } ?: 40.0 // Keine Deadline = mittlere Urgency
    }

    private fun calculateAgeScore(task: TaskEntity, currentTime: LocalDateTime): Double {
        return try {
            val createdDate = LocalDateTime.parse(task.createdAt)
            val daysSinceCreated = ChronoUnit.DAYS.between(createdDate.toLocalDate(), currentTime.toLocalDate())

            when {
                daysSinceCreated >= 30 -> 100.0 // Sehr alte Aufgabe
                daysSinceCreated >= 14 -> 80.0  // Alte Aufgabe
                daysSinceCreated >= 7 -> 60.0   // Eine Woche alt
                daysSinceCreated >= 3 -> 40.0   // Paar Tage alt
                daysSinceCreated >= 1 -> 30.0   // Gestern erstellt
                else -> 20.0                    // Heute erstellt
            }
        } catch (e: Exception) {
            50.0 // Fallback
        }
    }

    private fun calculateDurationScore(task: TaskEntity): Double {
        return task.estimatedMinutes?.let { minutes ->
            when {
                minutes <= 5 -> 100.0    // Sehr schnelle Aufgabe
                minutes <= 15 -> 90.0    // Quick Win
                minutes <= 30 -> 70.0    // Kurze Aufgabe
                minutes <= 60 -> 50.0    // Normale Aufgabe
                minutes <= 120 -> 30.0   // Längere Aufgabe
                minutes <= 240 -> 20.0   // Lange Aufgabe
                else -> 10.0             // Sehr lange Aufgabe
            }
        } ?: 50.0 // Unbekannte Dauer = mittlerer Score
    }

    private fun calculateEnergyScore(
        task: TaskEntity,
        currentTime: LocalDateTime,
        userContext: UserContext?
    ): Double {
        val taskEnergyRequirement = task.energyLevel
        val currentEnergyLevel = getCurrentEnergyLevel(currentTime, userContext)

        val baseScore = when (taskEnergyRequirement) {
            EnergyLevel.LOW -> 80.0    // Kann immer gemacht werden
            EnergyLevel.MEDIUM -> 60.0 // Normale Energie nötig
            EnergyLevel.HIGH -> 40.0   // Hohe Energie nötig
        }

        // Anpassung basierend auf aktueller Tageszeit
        return when (currentEnergyLevel) {
            EnergyLevel.HIGH -> {
                if (taskEnergyRequirement == EnergyLevel.HIGH) baseScore + 30.0 else baseScore
            }
            EnergyLevel.MEDIUM -> {
                if (taskEnergyRequirement == EnergyLevel.LOW) baseScore + 20.0 else baseScore
            }
            EnergyLevel.LOW -> {
                if (taskEnergyRequirement == EnergyLevel.LOW) baseScore + 40.0
                else baseScore - 30.0
            }
        }.coerceIn(0.0, 100.0)
    }

    private fun calculateContextScore(
        task: TaskEntity,
        currentTime: LocalDateTime,
        userContext: UserContext?
    ): Double {
        val taskContext = task.context
        val currentContext = getCurrentContext(currentTime, userContext)

        return when {
            taskContext == null -> 50.0 // Kein spezifischer Kontext
            taskContext == currentContext -> 100.0 // Perfekter Kontext-Match
            isContextCompatible(taskContext, currentContext) -> 75.0 // Kompatible Kontexte
            else -> 25.0 // Nicht passender Kontext
        }
    }

    private fun getCurrentEnergyLevel(currentTime: LocalDateTime, userContext: UserContext?): EnergyLevel {
        val hour = currentTime.hour

        // Standard-Energielevel basierend auf Tageszeit
        return when (hour) {
            in 6..9 -> EnergyLevel.HIGH    // Morgen
            in 10..12 -> EnergyLevel.HIGH  // Vormittag
            in 13..14 -> EnergyLevel.MEDIUM // Nach dem Mittagessen
            in 15..17 -> EnergyLevel.HIGH  // Nachmittag
            in 18..20 -> EnergyLevel.MEDIUM // Abend
            else -> EnergyLevel.LOW        // Nacht/sehr früh
        }.let { defaultLevel ->
            // Anpassung basierend auf Nutzerkontext
            userContext?.preferredEnergyTimes?.get(currentTime.toLocalTime().hour) ?: defaultLevel
        }
    }

    private fun getCurrentContext(currentTime: LocalDateTime, userContext: UserContext?): TaskContext? {
        val hour = currentTime.hour
        val dayOfWeek = currentTime.dayOfWeek

        return userContext?.let { context ->
            context.contextSchedule[dayOfWeek.value]?.get(hour)
        } ?: run {
            // Standard-Kontexte basierend auf Tageszeit
            when (hour) {
                in 9..17 -> TaskContext.WORK
                in 18..21 -> TaskContext.HOME
                else -> null
            }
        }
    }

    private fun isContextCompatible(taskContext: TaskContext, currentContext: TaskContext?): Boolean {
        if (currentContext == null) return false

        return when (taskContext) {
            TaskContext.COMPUTER -> currentContext in listOf(TaskContext.WORK, TaskContext.HOME)
            TaskContext.CALLS -> currentContext != TaskContext.EXERCISE // Anrufe nicht beim Sport
            TaskContext.ERRANDS -> currentContext != TaskContext.WORK   // Besorgungen nicht bei der Arbeit
            else -> true // Andere Kontexte sind meist kompatibel
        }
    }

    private fun applySpecialAdjustments(
        task: TaskEntity,
        baseScore: Double,
        currentTime: LocalDateTime
    ): Double {
        var adjustedScore = baseScore

        // Überfällige Aufgaben bekommen einen Bonus
        if (task.isOverdue()) {
            adjustedScore *= OVERDUE_BONUS_MULTIPLIER
        }

        // Heute fällige Aufgaben bekommen einen Bonus
        if (task.isDueToday()) {
            adjustedScore *= TODAY_BONUS_MULTIPLIER
        }

        // Quick Wins bekommen zur richtigen Zeit einen Bonus
        if (task.isQuickWin() && isGoodTimeForQuickWins(currentTime)) {
            adjustedScore += 15.0
        }

        // Recurring Tasks bekommen weniger Priorität wenn sie schon oft gemacht wurden
        if (task.isRecurring) {
            adjustedScore *= 0.9
        }

        // Abends werden einfache Aufgaben bevorzugt
        if (currentTime.hour >= 18 && task.energyLevel == EnergyLevel.LOW) {
            adjustedScore += 10.0
        }

        // Morgens werden wichtige Aufgaben bevorzugt
        if (currentTime.hour in 8..11 && task.priority == TaskPriority.HIGH) {
            adjustedScore += 10.0
        }

        return adjustedScore
    }

    private fun isGoodTimeForQuickWins(currentTime: LocalDateTime): Boolean {
        val hour = currentTime.hour
        // Quick Wins sind gut zwischen Terminen oder am Ende des Tages
        return hour in listOf(11, 14, 17, 19) // Vor Mittag, nach Mittag, Arbeitsende, Abend
    }

    fun getTop5Tasks(allTasks: List<TaskEntity>, userContext: UserContext? = null): List<TaskEntity> {
        return allTasks
            .filter { !it.isCompleted }
            .map { task ->
                task to calculateSmartScore(task, LocalDateTime.now(), userContext)
            }
            .sortedByDescending { it.second }
            .take(5)
            .map { it.first }
    }

    fun getTasksByContext(
        allTasks: List<TaskEntity>,
        context: TaskContext,
        userContext: UserContext? = null
    ): List<TaskEntity> {
        return allTasks
            .filter { !it.isCompleted && (it.context == context || it.context == null) }
            .map { task ->
                task to calculateSmartScore(task, LocalDateTime.now(), userContext)
            }
            .sortedByDescending { it.second }
            .map { it.first }
    }

    fun getQuickWins(allTasks: List<TaskEntity>, maxCount: Int = 3): List<TaskEntity> {
        return allTasks
            .filter { !it.isCompleted && it.isQuickWin() }
            .map { task ->
                task to calculateSmartScore(task)
            }
            .sortedByDescending { it.second }
            .take(maxCount)
            .map { it.first }
    }

    fun generateTaskReasoning(task: TaskEntity, score: Double): TaskReasoning {
        val reasons = mutableListOf<String>()

        // Priorität
        when (task.priority) {
            TaskPriority.HIGH -> reasons.add("🔴 Hohe Priorität")
            TaskPriority.MEDIUM -> reasons.add("🟡 Mittlere Priorität")
            TaskPriority.LOW -> reasons.add("🟢 Niedrige Priorität")
        }

        // Due Date
        task.dueDate?.let { dueDate ->
            val days = ChronoUnit.DAYS.between(LocalDate.now(), LocalDate.parse(dueDate))
            when {
                days < 0 -> reasons.add("🚨 Überfällig seit ${-days} Tag(en)")
                days == 0L -> reasons.add("🔥 Heute fällig")
                days == 1L -> reasons.add("⏰ Morgen fällig")
                days <= 7 -> reasons.add("📅 Fällig in $days Tagen")
            }
        }

        // Geschätzte Zeit
        task.estimatedMinutes?.let { minutes ->
            when {
                minutes <= 15 -> reasons.add("⚡ Quick Win ($minutes Min)")
                minutes <= 60 -> reasons.add("⏱️ Kurze Aufgabe ($minutes Min)")
                else -> reasons.add("🎯 Längere Aufgabe ($minutes Min)")
            }
        }

        // Alter der Aufgabe
        val daysSinceCreated = try {
            ChronoUnit.DAYS.between(
                LocalDateTime.parse(task.createdAt).toLocalDate(),
                LocalDate.now()
            )
        } catch (e: Exception) { 0 }

        if (daysSinceCreated >= 7) {
            reasons.add("📆 Wartet schon seit $daysSinceCreated Tagen")
        }

        // Kontext
        task.context?.let { context ->
            reasons.add("${context.icon} ${context.displayName}")
        }

        return TaskReasoning(
            task = task,
            score = score,
            reasons = reasons,
            recommendation = generateRecommendation(task, score)
        )
    }

    private fun generateRecommendation(task: TaskEntity, score: Double): String {
        return when {
            score >= 90 -> "Solltest du jetzt sofort machen! 🚀"
            score >= 70 -> "Eine der wichtigsten Aufgaben heute. 💪"
            score >= 50 -> "Solltest du heute noch schaffen. 👍"
            score >= 30 -> "Kannst du später machen. ⏰"
            else -> "Nicht dringend, aber nicht vergessen. 📝"
        }
    }
}

// Datenklassen für erweiterte Funktionalität
data class UserContext(
    val preferredEnergyTimes: Map<Int, EnergyLevel> = emptyMap(), // Stunde -> Energielevel
    val contextSchedule: Map<Int, Map<Int, TaskContext>> = emptyMap(), // Wochentag -> Stunde -> Kontext
    val productivityPeaks: List<Int> = listOf(9, 10, 15, 16), // Produktivste Stunden
    val preferredTaskDuration: Int = 30, // Bevorzugte Aufgabenlänge in Minuten
    val workingDays: Set<Int> = setOf(1, 2, 3, 4, 5) // Montag bis Freitag
)

data class TaskReasoning(
    val task: TaskEntity,
    val score: Double,
    val reasons: List<String>,
    val recommendation: String
) {
    fun getScoreCategory(): ScoreCategory {
        return when {
            score >= 90 -> ScoreCategory.CRITICAL
            score >= 70 -> ScoreCategory.HIGH
            score >= 50 -> ScoreCategory.MEDIUM
            score >= 30 -> ScoreCategory.LOW
            else -> ScoreCategory.MINIMAL
        }
    }
}

enum class ScoreCategory(val displayName: String, val color: Long, val icon: String) {
    CRITICAL("Kritisch", 0xFFE53E3E, "🚨"),
    HIGH("Hoch", 0xFFFF9800, "🔥"),
    MEDIUM("Mittel", 0xFF2196F3, "⭐"),
    LOW("Niedrig", 0xFF4CAF50, "📝"),
    MINIMAL("Minimal", 0xFF9E9E9E, "💤")
}