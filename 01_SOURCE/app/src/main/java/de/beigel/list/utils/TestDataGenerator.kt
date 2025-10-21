package de.beigel.list.utils

import de.beigel.list.data.TaskEntity
import de.beigel.list.data.TaskPriority
import de.beigel.list.repository.TaskRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

/**
 * Test Data Generator für die Daily List App
 * Erstellt realistische Beispielaufgaben für verschiedene Szenarien
 */
object TestDataGenerator {

    /**
     * Generiert vollständige Testdaten für alle Bereiche
     */
    suspend fun generateAllTestData(repository: TaskRepository) {
        generateTodayTasks(repository)
        generateBacklogTasks(repository)
        generateHistoricalTasks(repository)
    }

    /**
     * Generiert Testaufgaben für heute
     */
    suspend fun generateTodayTasks(repository: TaskRepository) {
        val today = LocalDate.now()
        val todayTasks = listOf(
            TaskEntity(
                id = UUID.randomUUID().toString(),
                title = "E-Mails bearbeiten",
                description = "Wichtige E-Mails beantworten und Inbox aufräumen",
                priority = TaskPriority.HIGH,
                isCompleted = true,
                position = 0,
                date = today.toString(),
                createdAt = LocalDateTime.now().minusHours(2).toString(),
                completedAt = LocalDateTime.now().minusHours(1).toString(),
                isInDailyList = true
            ),
            TaskEntity(
                id = UUID.randomUUID().toString(),
                title = "Projekt-Präsentation vorbereiten",
                description = "Slides für das Meeting am Nachmittag fertigstellen",
                priority = TaskPriority.HIGH,
                isCompleted = false,
                position = 1,
                date = today.toString(),
                createdAt = LocalDateTime.now().minusHours(3).toString(),
                isInDailyList = true
            ),
            TaskEntity(
                id = UUID.randomUUID().toString(),
                title = "Team-Meeting um 14:00",
                description = "Sprint Planning und Review",
                priority = TaskPriority.MEDIUM,
                isCompleted = false,
                position = 2,
                date = today.toString(),
                createdAt = LocalDateTime.now().minusHours(4).toString(),
                isInDailyList = true
            ),
            TaskEntity(
                id = UUID.randomUUID().toString(),
                title = "Einkaufen gehen",
                description = "Milch, Brot, Käse, Gemüse",
                priority = TaskPriority.MEDIUM,
                isCompleted = true,
                position = 3,
                date = today.toString(),
                createdAt = LocalDateTime.now().minusHours(5).toString(),
                completedAt = LocalDateTime.now().minusMinutes(30).toString(),
                isInDailyList = true
            ),
            TaskEntity(
                id = UUID.randomUUID().toString(),
                title = "Fitness-Studio",
                description = "Beintraining und 20 Minuten Cardio",
                priority = TaskPriority.LOW,
                isCompleted = false,
                position = 4,
                date = today.toString(),
                createdAt = LocalDateTime.now().minusHours(6).toString(),
                isInDailyList = true
            ),
            TaskEntity(
                id = UUID.randomUUID().toString(),
                title = "Geburtstag Mama anrufen",
                description = "Nicht vergessen! 🎂",
                priority = TaskPriority.HIGH,
                isCompleted = false,
                position = 5,
                date = today.toString(),
                createdAt = LocalDateTime.now().minusHours(1).toString(),
                isInDailyList = true
            ),
            TaskEntity(
                id = UUID.randomUUID().toString(),
                title = "Steuererklärung vorbereiten",
                description = "Belege sammeln und sortieren",
                priority = TaskPriority.MEDIUM,
                isCompleted = false,
                position = 6,
                date = today.toString(),
                createdAt = LocalDateTime.now().minusHours(7).toString(),
                isInDailyList = true
            )
        )

        todayTasks.forEach { task ->
            repository.insertTask(task, addToDaily = true)
        }
    }

    /**
     * Generiert Testaufgaben für das Backlog
     */
    suspend fun generateBacklogTasks(repository: TaskRepository) {
        val backlogTasks = listOf(
            TaskEntity(
                id = UUID.randomUUID().toString(),
                title = "Urlaubsplanung für Sommer",
                description = "Hotels und Flüge recherchieren",
                priority = TaskPriority.LOW,
                isCompleted = false,
                backlogPosition = 0,
                date = LocalDate.now().toString(),
                createdAt = LocalDateTime.now().minusDays(2).toString(),
                isInDailyList = false
            ),
            TaskEntity(
                id = UUID.randomUUID().toString(),
                title = "Auto zur Inspektion bringen",
                description = "Termin vereinbaren bei Werkstatt Müller",
                priority = TaskPriority.MEDIUM,
                isCompleted = false,
                backlogPosition = 1,
                date = LocalDate.now().toString(),
                createdAt = LocalDateTime.now().minusDays(3).toString(),
                isInDailyList = false
            ),
            TaskEntity(
                id = UUID.randomUUID().toString(),
                title = "Neue Laufschuhe kaufen",
                description = "Bei Sport-Check vorbei schauen",
                priority = TaskPriority.LOW,
                isCompleted = false,
                backlogPosition = 2,
                date = LocalDate.now().toString(),
                createdAt = LocalDateTime.now().minusDays(1).toString(),
                isInDailyList = false
            ),
            TaskEntity(
                id = UUID.randomUUID().toString(),
                title = "Buch fertig lesen",
                description = "Noch 3 Kapitel von 'Atomic Habits'",
                priority = TaskPriority.LOW,
                isCompleted = false,
                backlogPosition = 3,
                date = LocalDate.now().toString(),
                createdAt = LocalDateTime.now().minusDays(5).toString(),
                isInDailyList = false
            ),
            TaskEntity(
                id = UUID.randomUUID().toString(),
                title = "Zahnarzttermin vereinbaren",
                description = "Kontrolltermin für nächsten Monat",
                priority = TaskPriority.MEDIUM,
                isCompleted = false,
                backlogPosition = 4,
                date = LocalDate.now().toString(),
                createdAt = LocalDateTime.now().minusDays(4).toString(),
                isInDailyList = false
            ),
            TaskEntity(
                id = UUID.randomUUID().toString(),
                title = "Wohnung aufräumen",
                description = "Komplette Grundreinigung am Wochenende",
                priority = TaskPriority.MEDIUM,
                isCompleted = false,
                backlogPosition = 5,
                date = LocalDate.now().toString(),
                createdAt = LocalDateTime.now().minusDays(2).toString(),
                isInDailyList = false
            ),
            TaskEntity(
                id = UUID.randomUUID().toString(),
                title = "Neue Programmiersprache lernen",
                description = "Kotlin Basics durcharbeiten",
                priority = TaskPriority.LOW,
                isCompleted = false,
                backlogPosition = 6,
                date = LocalDate.now().toString(),
                createdAt = LocalDateTime.now().minusDays(6).toString(),
                isInDailyList = false
            ),
            TaskEntity(
                id = UUID.randomUUID().toString(),
                title = "Geburtstagsgeschenk für Sarah",
                description = "Ideen: Buch, Gutschein oder Schmuck",
                priority = TaskPriority.MEDIUM,
                isCompleted = false,
                backlogPosition = 7,
                date = LocalDate.now().toString(),
                createdAt = LocalDateTime.now().minusDays(1).toString(),
                isInDailyList = false
            ),
            TaskEntity(
                id = UUID.randomUUID().toString(),
                title = "Garten frühlingsfertig machen",
                description = "Rasen mähen, Hecke schneiden, Blumen pflanzen",
                priority = TaskPriority.LOW,
                isCompleted = false,
                backlogPosition = 8,
                date = LocalDate.now().toString(),
                createdAt = LocalDateTime.now().minusDays(7).toString(),
                isInDailyList = false
            ),
            TaskEntity(
                id = UUID.randomUUID().toString(),
                title = "Backup von allen Geräten",
                description = "Laptop, Handy, Tablet - Cloud und externe Festplatte",
                priority = TaskPriority.HIGH,
                isCompleted = false,
                backlogPosition = 9,
                date = LocalDate.now().toString(),
                createdAt = LocalDateTime.now().minusDays(3).toString(),
                isInDailyList = false
            )
        )

        backlogTasks.forEach { task ->
            repository.insertTask(task, addToDaily = false)
        }
    }

    /**
     * Generiert historische Testaufgaben für die letzten 7 Tage
     */
    suspend fun generateHistoricalTasks(repository: TaskRepository) {
        // Gestern
        generateDayTasks(repository, LocalDate.now().minusDays(1), 5, 4)

        // Vor 2 Tagen
        generateDayTasks(repository, LocalDate.now().minusDays(2), 6, 5)

        // Vor 3 Tagen
        generateDayTasks(repository, LocalDate.now().minusDays(3), 4, 4)

        // Vor 4 Tagen
        generateDayTasks(repository, LocalDate.now().minusDays(4), 7, 4)

        // Vor 5 Tagen
        generateDayTasks(repository, LocalDate.now().minusDays(5), 5, 3)

        // Vor 6 Tagen
        generateDayTasks(repository, LocalDate.now().minusDays(6), 6, 6)

        // Vor 7 Tagen
        generateDayTasks(repository, LocalDate.now().minusDays(7), 5, 2)
    }

    /**
     * Generiert Aufgaben für einen bestimmten Tag
     */
    private suspend fun generateDayTasks(
        repository: TaskRepository,
        date: LocalDate,
        totalTasks: Int,
        completedTasks: Int
    ) {
        val taskTemplates = listOf(
            "E-Mails bearbeiten" to "Inbox aufräumen und wichtige Mails beantworten",
            "Sport machen" to "45 Minuten Training",
            "Einkaufen" to "Lebensmittel für die Woche",
            "Projekt arbeiten" to "Fortschritt an aktuellem Projekt",
            "Freunde treffen" to "Kaffee trinken oder spazieren gehen",
            "Lesen" to "Mindestens 30 Minuten",
            "Haushalt" to "Putzen und aufräumen",
            "Termine" to "Wichtige Calls und Meetings",
            "Lernen" to "Neue Skills oder Wissen aneignen",
            "Entspannen" to "Zeit für sich selbst nehmen"
        )

        repeat(totalTasks) { index ->
            val template = taskTemplates[index % taskTemplates.size]
            val isCompleted = index < completedTasks

            val task = TaskEntity(
                id = UUID.randomUUID().toString(),
                title = template.first,
                description = template.second,
                priority = when (index % 3) {
                    0 -> TaskPriority.HIGH
                    1 -> TaskPriority.MEDIUM
                    else -> TaskPriority.LOW
                },
                isCompleted = isCompleted,
                position = index,
                date = date.toString(),
                createdAt = date.atTime(8, 0).plusHours(index.toLong()).toString(),
                completedAt = if (isCompleted) {
                    date.atTime(12, 0).plusHours(index.toLong()).toString()
                } else null,
                isInDailyList = true
            )

            repository.insertTask(task, addToDaily = true)
        }
    }

    /**
     * Löscht alle Testdaten
     */
    suspend fun clearAllTestData(repository: TaskRepository) {
        // Diese Methode würde alle Tasks löschen - vorsichtig verwenden!
        // Implementierung je nach Bedarf
    }

    /**
     * Generiert minimale Testdaten für schnelles Testen
     */
    suspend fun generateMinimalTestData(repository: TaskRepository) {
        val today = LocalDate.now()

        // 3 Heute-Tasks
        repository.insertTask(
            TaskEntity(
                title = "Test Task 1",
                description = "Erste Testaufgabe",
                priority = TaskPriority.HIGH,
                isCompleted = false,
                position = 0,
                date = today.toString(),
                isInDailyList = true
            ),
            addToDaily = true
        )

        repository.insertTask(
            TaskEntity(
                title = "Test Task 2",
                description = "Zweite Testaufgabe",
                priority = TaskPriority.MEDIUM,
                isCompleted = true,
                position = 1,
                date = today.toString(),
                completedAt = LocalDateTime.now().toString(),
                isInDailyList = true
            ),
            addToDaily = true
        )

        repository.insertTask(
            TaskEntity(
                title = "Test Task 3",
                description = "Dritte Testaufgabe",
                priority = TaskPriority.LOW,
                isCompleted = false,
                position = 2,
                date = today.toString(),
                isInDailyList = true
            ),
            addToDaily = true
        )

        // 2 Backlog Tasks
        repository.insertTask(
            TaskEntity(
                title = "Backlog Task 1",
                description = "Erste Backlog-Aufgabe",
                priority = TaskPriority.MEDIUM,
                isCompleted = false,
                backlogPosition = 0,
                date = today.toString(),
                isInDailyList = false
            ),
            addToDaily = false
        )

        repository.insertTask(
            TaskEntity(
                title = "Backlog Task 2",
                description = "Zweite Backlog-Aufgabe",
                priority = TaskPriority.LOW,
                isCompleted = false,
                backlogPosition = 1,
                date = today.toString(),
                isInDailyList = false
            ),
            addToDaily = false
        )
    }
}

/**
 * Extension Function für einfachen Zugriff
 */
fun CoroutineScope.generateTestData(repository: TaskRepository) {
    launch(Dispatchers.IO) {
        TestDataGenerator.generateAllTestData(repository)
    }
}

fun CoroutineScope.generateMinimalTestData(repository: TaskRepository) {
    launch(Dispatchers.IO) {
        TestDataGenerator.generateMinimalTestData(repository)
    }
}