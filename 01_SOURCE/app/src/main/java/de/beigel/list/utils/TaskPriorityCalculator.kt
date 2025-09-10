package de.beigel.list.utils

import de.beigel.list.data.*
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.*

@Singleton
class TaskPriorityCalculator @Inject constructor() {

    companion object {
        // Gewichtungen für Smart Score
        private const val PRIORITY_WEIGHT = 30.0
        private const val URGENCY_WEIGHT = 25.0
        private const val DUE_DATE_WEIGHT = 20.0
        private const val AGE_WEIGHT = 15.0
        private const val ENERGY_MATCH_WEIGHT = 10.0

        // Basis-Scores
        private const val MAX_SCORE = 100.0
        private const val MIN_SCORE = 0.0

        // Zeit-Konstanten
        private const val DAYS_TO_URGENT = 2.0
        private const val MAX_AGE_DAYS = 30.0
    }

    fun calculateScore(task: TaskEntity): Double {
        if (task.isCompleted) return 0.0

        val priorityScore = calculatePriorityScore(task.priority)
        val urgencyScore = calculateUrgencyScore(task)
        val dueDateScore = calculateDueDateScore(task.dueDate)
        val ageScore = calculateAgeScore(task.createdAt)
        val energyScore = calculateEnergyMatchScore(task.energyLevel)

        val totalScore = (priorityScore * PRIORITY_WEIGHT +
                urgencyScore * URGENCY_WEIGHT +
                dueDateScore * DUE_DATE_WEIGHT +
                ageScore * AGE_WEIGHT +
                energyScore * ENERGY_MATCH_WEIGHT) / 100.0

        return totalScore.coerceIn(MIN_SCORE, MAX_SCORE)
    }

    fun calculateInitialScore(
        priority: TaskPriority,
        dueDate: LocalDate?,
        estimatedMinutes: Int?,
        energyLevel: EnergyLevel
    ): Double {
        val priorityScore = calculatePriorityScore(priority)
        val dueDateScore = calculateDueDateScore(dueDate)
        val energyScore = calculateEnergyMatchScore(energyLevel)

        // Vereinfachter Score für neue Tasks (ohne Alter und aktuelle Dringlichkeit)
        val baseScore = (priorityScore * 0.4 + dueDateScore * 0.4 + energyScore * 0.2)

        // Bonus für Quick Wins
        val quickWinBonus = if (estimatedMinutes != null && estimatedMinutes <= 15) 10.0 else 0.0

        return (baseScore + quickWinBonus).coerceIn(MIN_SCORE, MAX_SCORE)
    }

    private fun calculatePriorityScore(priority: TaskPriority): Double {
        return when (priority) {
            TaskPriority.HIGH -> 100.0
            TaskPriority.MEDIUM -> 60.0
            TaskPriority.LOW -> 30.0
        }
    }

    private fun calculateUrgencyScore(task: TaskEntity): Double {
        // Urgency basiert auf verschiedenen Faktoren
        val dueUrgency = calculateDueUrgency(task.dueDate)
        val contextUrgency = calculateContextUrgency(task.context)
        val lastShownPenalty = calculateLastShownPenalty(task.lastShownInTodayList)

        return maxOf(dueUrgency, contextUrgency) + lastShownPenalty
    }

    private fun calculateDueUrgency(dueDate: LocalDate?): Double {
        if (dueDate == null) return 20.0 // Moderate Urgency für Tasks ohne Deadline

        val today = LocalDate.now()
        val daysUntilDue = ChronoUnit.DAYS.between(today, dueDate).toDouble()

        return when {
            daysUntilDue < 0 -> 100.0 // Überfällig
            daysUntilDue == 0.0 -> 95.0 // Heute fällig
            daysUntilDue == 1.0 -> 80.0 // Morgen fällig
            daysUntilDue <= DAYS_TO_URGENT -> 60.0 // Bald fällig
            daysUntilDue <= 7.0 -> 40.0 // Diese Woche
            daysUntilDue <= 30.0 -> 20.0 // Diesen Monat
            else -> 10.0 // Weit in der Zukunft
        }
    }

    private fun calculateContextUrgency(context: TaskContext): Double {
        val currentHour = LocalDateTime.now().hour

        return when (context) {
            TaskContext.WORK -> if (currentHour in 8..18) 70.0 else 30.0
            TaskContext.HOME -> if (currentHour in 17..22) 60.0 else 40.0
            TaskContext.CALLS -> if (currentHour in 9..17) 80.0 else 20.0
            TaskContext.ERRANDS -> if (currentHour in 9..18) 70.0 else 20.0
            TaskContext.COMPUTER -> if (currentHour in 8..20) 60.0 else 30.0
            TaskContext.EXERCISE -> if (currentHour in 6..9 || currentHour in 17..20) 80.0 else 30.0
            TaskContext.LEARNING -> if (currentHour in 8..12 || currentHour in 14..17) 70.0 else 40.0
            TaskContext.CREATIVE -> if (currentHour in 8..12 || currentHour in 14..18) 75.0 else 45.0
            TaskContext.NONE -> 50.0
        }
    }

    private fun calculateLastShownPenalty(lastShown: LocalDate?): Double {
        if (lastShown == null) return 0.0

        val daysSinceShown = ChronoUnit.DAYS.between(lastShown, LocalDate.now()).toDouble()

        // Penalty für Tasks, die lange nicht mehr in der Today-Liste waren
        return when {
            daysSinceShown >= 7 -> 15.0
            daysSinceShown >= 3 -> 10.0
            daysSinceShown >= 1 -> 5.0
            else -> 0.0
        }
    }

    private fun calculateDueDateScore(dueDate: LocalDate?): Double {
        if (dueDate == null) return 40.0 // Moderate Score für Tasks ohne Deadline

        val today = LocalDate.now()
        val daysUntilDue = ChronoUnit.DAYS.between(today, dueDate).toDouble()

        return when {
            daysUntilDue < 0 -> 100.0 // Überfällig - höchste Priorität
            daysUntilDue == 0.0 -> 90.0 // Heute fällig
            daysUntilDue <= 1.0 -> 80.0 // Morgen
            daysUntilDue <= 3.0 -> 70.0 // Diese Woche (früh)
            daysUntilDue <= 7.0 -> 60.0 // Diese Woche (spät)
            daysUntilDue <= 14.0 -> 50.0 // Nächste Woche
            daysUntilDue <= 30.0 -> 40.0 // Diesen Monat
            else -> 30.0 // Weit in der Zukunft
        }
    }

    private fun calculateAgeScore(createdAt: LocalDateTime): Double {
        val hoursOld = ChronoUnit.HOURS.between(createdAt, LocalDateTime.now()).toDouble()
        val daysOld = hoursOld / 24.0

        // Tasks werden wichtiger je älter sie werden (bis zu einem Maximum)
        return when {
            daysOld <= 1 -> 20.0 // Sehr neu
            daysOld <= 3 -> 40.0 // Wenige Tage alt
            daysOld <= 7 -> 60.0 // Eine Woche alt
            daysOld <= 14 -> 80.0 // Zwei Wochen alt
            daysOld <= MAX_AGE_DAYS -> 100.0 // Alt - höchste Priorität
            else -> 100.0 // Sehr alt
        }
    }

    private fun calculateEnergyMatchScore(energyLevel: EnergyLevel): Double {
        val currentHour = LocalDateTime.now().hour
        val currentEnergyLevel = getCurrentEnergyLevel(currentHour)

        return when {
            energyLevel == currentEnergyLevel -> 100.0 // Perfektes Match
            energyLevel == EnergyLevel.LOW -> 80.0 // Low energy tasks passen fast immer
            energyLevel == EnergyLevel.HIGH && currentEnergyLevel == EnergyLevel.MEDIUM -> 70.0
            energyLevel == EnergyLevel.MEDIUM && currentEnergyLevel == EnergyLevel.HIGH -> 60.0
            else -> 40.0 // Mismatch
        }
    }

    private fun getCurrentEnergyLevel(hour: Int): EnergyLevel {
        return when (hour) {
            in 6..9 -> EnergyLevel.HIGH // Morgen - hohe Energie
            in 10..12 -> EnergyLevel.HIGH // Vormittag - hohe Energie
            in 13..14 -> EnergyLevel.LOW // Mittagstief
            in 15..17 -> EnergyLevel.MEDIUM // Nachmittag - moderate Energie
            in 18..21 -> EnergyLevel.MEDIUM // Früher Abend
            else -> EnergyLevel.LOW // Nacht und späte Stunden
        }
    }

    // === Zusätzliche Utility-Funktionen ===

    fun getTaskReasoning(task: TaskEntity): TaskReasoning {
        val reasons = mutableListOf<String>()
        val scoreBreakdown = mutableMapOf<ScoreCategory, Double>()

        // Priorität
        val priorityScore = calculatePriorityScore(task.priority)
        scoreBreakdown[ScoreCategory.PRIORITY] = priorityScore
        when (task.priority) {
            TaskPriority.HIGH -> reasons.add("Hohe Priorität")
            TaskPriority.MEDIUM -> reasons.add("Mittlere Priorität")
            TaskPriority.LOW -> reasons.add("Niedrige Priorität")
        }

        // Due Date
        val dueDateScore = calculateDueDateScore(task.dueDate)
        scoreBreakdown[ScoreCategory.DUE_DATE] = dueDateScore
        task.dueDate?.let { dueDate ->
            val daysUntil = ChronoUnit.DAYS.between(LocalDate.now(), dueDate)
            when {
                daysUntil < 0 -> reasons.add("Überfällig seit ${-daysUntil} Tag(en)")
                daysUntil == 0L -> reasons.add("Heute fällig")
                daysUntil == 1L -> reasons.add("Morgen fällig")
                daysUntil <= 7 -> reasons.add("In ${daysUntil} Tag(en) fällig")
            }
        }

        // Alter
        val ageScore = calculateAgeScore(task.createdAt)
        scoreBreakdown[ScoreCategory.AGE] = ageScore
        val daysOld = ChronoUnit.DAYS.between(task.createdAt.toLocalDate(), LocalDate.now())
        if (daysOld > 7) {
            reasons.add("Seit ${daysOld} Tag(en) offen")
        }

        // Energie Match
        val energyScore = calculateEnergyMatchScore(task.energyLevel)
        scoreBreakdown[ScoreCategory.ENERGY_MATCH] = energyScore
        if (energyScore > 80) {
            reasons.add("Passt zu deiner aktuellen Energie")
        }

        // Quick Win
        if (task.estimatedMinutes != null && task.estimatedMinutes <= 15) {
            reasons.add("Quick Win (${task.estimatedMinutes} Min)")
        }

        return TaskReasoning(
            task = task,
            reasons = reasons,
            scoreBreakdown = scoreBreakdown,
            totalScore = calculateScore(task)
        )
    }

    fun getRecommendedAction(task: TaskEntity): RecommendedAction {
        val score = calculateScore(task)
        val urgency = calculateDueDateScore(task.dueDate)

        return when {
            score >= 90 -> RecommendedAction.DO_NOW
            score >= 70 && urgency >= 80 -> RecommendedAction.DO_TODAY
            score >= 50 -> RecommendedAction.SCHEDULE_SOON
            score >= 30 -> RecommendedAction.DO_LATER
            else -> RecommendedAction.SOMEDAY
        }
    }
}

data class TaskReasoning(
    val task: TaskEntity,
    val reasons: List<String>,
    val scoreBreakdown: Map<ScoreCategory, Double>,
    val totalScore: Double
)

enum class ScoreCategory {
    PRIORITY, DUE_DATE, AGE, ENERGY_MATCH, URGENCY
}

enum class RecommendedAction {
    DO_NOW, DO_TODAY, SCHEDULE_SOON, DO_LATER, SOMEDAY
}