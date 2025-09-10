package de.beigel.list.utils

import de.beigel.list.data.*
import de.beigel.list.ui.dialogs.ParsedTask
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NaturalLanguageProcessor @Inject constructor() {

    companion object {
        // Regex Patterns für verschiedene Erkennungen
        private val TIME_PATTERNS = listOf(
            Regex("""(\d+)\s*(?:min|minuten?|m)\b""", RegexOption.IGNORE_CASE),
            Regex("""(\d+)\s*(?:std|stunden?|h)\b""", RegexOption.IGNORE_CASE),
            Regex("""(\d+)\s*(?:tag|tage?|d)\b""", RegexOption.IGNORE_CASE)
        )

        private val PRIORITY_PATTERNS = mapOf(
            TaskPriority.HIGH to listOf(
                Regex("""\b(?:wichtig|urgent|dringend|hoch|high|kritisch|sofort|asap)\b""", RegexOption.IGNORE_CASE),
                Regex("""[!]{2,}"""),
                Regex("""\b(?:prio\s*1|priorität\s*1)\b""", RegexOption.IGNORE_CASE)
            ),
            TaskPriority.MEDIUM to listOf(
                Regex("""\b(?:normal|mittel|medium|standard)\b""", RegexOption.IGNORE_CASE),
                Regex("""\b(?:prio\s*2|priorität\s*2)\b""", RegexOption.IGNORE_CASE)
            ),
            TaskPriority.LOW to listOf(
                Regex("""\b(?:niedrig|low|unwichtig|später|someday|irgendwann)\b""", RegexOption.IGNORE_CASE),
                Regex("""\b(?:prio\s*3|priorität\s*3)\b""", RegexOption.IGNORE_CASE)
            )
        )

        private val DATE_PATTERNS = mapOf(
            "heute" to 0,
            "morgen" to 1,
            "übermorgen" to 2,
            "montag" to null,
            "dienstag" to null,
            "mittwoch" to null,
            "donnerstag" to null,
            "freitag" to null,
            "samstag" to null,
            "sonntag" to null
        )

        private val ENERGY_PATTERNS = mapOf(
            EnergyLevel.HIGH to listOf(
                Regex("""\b(?:viel\s*energie|hohe?\s*energie|anspruchsvoll|schwierig|komplex|konzentration)\b""", RegexOption.IGNORE_CASE),
                Regex("""⚡""")
            ),
            EnergyLevel.MEDIUM to listOf(
                Regex("""\b(?:normale?\s*energie|mittel|standard|normal)\b""", RegexOption.IGNORE_CASE),
                Regex("""🔋""")
            ),
            EnergyLevel.LOW to listOf(
                Regex("""\b(?:wenig\s*energie|niedrige?\s*energie|einfach|quick|leicht|entspannt)\b""", RegexOption.IGNORE_CASE),
                Regex("""💤""")
            )
        )

        private val CONTEXT_PATTERNS = mapOf(
            TaskContext.WORK to listOf(
                Regex("""\b(?:arbeit|büro|work|office|meeting|projekt|kunde|kollege)\b""", RegexOption.IGNORE_CASE)
            ),
            TaskContext.HOME to listOf(
                Regex("""\b(?:zuhause|home|haushalt|putzen|kochen|familie)\b""", RegexOption.IGNORE_CASE)
            ),
            TaskContext.CALLS to listOf(
                Regex("""\b(?:anruf|anrufen|telefonieren|tel|call|phone)\b""", RegexOption.IGNORE_CASE)
            ),
            TaskContext.COMPUTER to listOf(
                Regex("""\b(?:computer|pc|online|email|internet|software|programmieren|code)\b""", RegexOption.IGNORE_CASE)
            ),
            TaskContext.ERRANDS to listOf(
                Regex("""\b(?:einkaufen|besorgung|erledigung|bank|post|apotheke|shopping)\b""", RegexOption.IGNORE_CASE)
            ),
            TaskContext.EXERCISE to listOf(
                Regex("""\b(?:sport|fitness|gym|joggen|laufen|training|workout)\b""", RegexOption.IGNORE_CASE)
            ),
            TaskContext.LEARNING to listOf(
                Regex("""\b(?:lernen|studieren|kurs|buch|lesen|tutorial|weiterbildung)\b""", RegexOption.IGNORE_CASE)
            ),
            TaskContext.CREATIVE to listOf(
                Regex("""\b(?:kreativ|design|zeichnen|schreiben|musik|basteln|kunst)\b""", RegexOption.IGNORE_CASE)
            )
        )

        // Quick Win Indikatoren
        private val QUICK_WIN_PATTERNS = listOf(
            Regex("""\b(?:quick|schnell|kurz|mal\s*eben|fix)\b""", RegexOption.IGNORE_CASE)
        )

        // Tag-Erkennungsmuster
        private val TAG_PATTERN = Regex("""#(\w+)""")
        private val AT_PATTERN = Regex("""@(\w+)""")
    }

    fun parseTaskInput(input: String): ParsedTask {
        var cleanTitle = input.trim()
        val detectedPriority = detectPriority(input)
        val detectedDueDate = detectDueDate(input)
        val detectedTime = detectEstimatedTime(input)
        val detectedEnergy = detectEnergyLevel(input)
        val detectedContext = detectContext(input)
        val detectedTags = detectTags(input)

        // Entferne erkannte Elemente aus dem Titel
        cleanTitle = cleanUpTitle(cleanTitle, detectedPriority, detectedDueDate, detectedTime, detectedEnergy, detectedContext, detectedTags)

        return ParsedTask(
            cleanTitle = cleanTitle,
            priority = detectedPriority,
            dueDate = detectedDueDate,
            estimatedMinutes = detectedTime,
            energyLevel = detectedEnergy,
            context = detectedContext,
            tags = detectedTags
        )
    }

    private fun detectPriority(input: String): TaskPriority? {
        for ((priority, patterns) in PRIORITY_PATTERNS) {
            for (pattern in patterns) {
                if (pattern.containsMatchIn(input)) {
                    return priority
                }
            }
        }
        return null
    }

    private fun detectDueDate(input: String): LocalDate? {
        val today = LocalDate.now()

        // Suche nach relativen Datumsangaben
        for ((keyword, daysOffset) in DATE_PATTERNS) {
            if (input.contains(keyword, ignoreCase = true)) {
                return when (daysOffset) {
                    null -> detectWeekday(input, keyword)
                    else -> today.plusDays(daysOffset.toLong())
                }
            }
        }

        // Suche nach absoluten Datumsangaben
        val datePatterns = listOf(
            Regex("""(\d{1,2})\.(\d{1,2})\.?(?:(\d{4}))?"""), // dd.mm.yyyy oder dd.mm
            Regex("""(\d{1,2})/(\d{1,2})/(\d{4})"""), // mm/dd/yyyy
            Regex("""(\d{4})-(\d{1,2})-(\d{1,2})""") // yyyy-mm-dd
        )

        for (pattern in datePatterns) {
            val match = pattern.find(input)
            if (match != null) {
                return parseAbsoluteDate(match.groupValues)
            }
        }

        // Suche nach "in X Tagen"
        val inDaysPattern = Regex("""in\s+(\d+)\s+tag(?:en?)?""", RegexOption.IGNORE_CASE)
        inDaysPattern.find(input)?.let { match ->
            val days = match.groupValues[1].toIntOrNull()
            if (days != null) {
                return today.plusDays(days.toLong())
            }
        }

        return null
    }

    private fun detectWeekday(input: String, weekdayKeyword: String): LocalDate? {
        val today = LocalDate.now()
        val currentDayOfWeek = today.dayOfWeek.value

        val targetDayOfWeek = when (weekdayKeyword.lowercase()) {
            "montag" -> 1
            "dienstag" -> 2
            "mittwoch" -> 3
            "donnerstag" -> 4
            "freitag" -> 5
            "samstag" -> 6
            "sonntag" -> 7
            else -> return null
        }

        var daysUntilTarget = targetDayOfWeek - currentDayOfWeek
        if (daysUntilTarget <= 0) {
            daysUntilTarget += 7 // Nächste Woche
        }

        return today.plusDays(daysUntilTarget.toLong())
    }

    private fun parseAbsoluteDate(groups: List<String>): LocalDate? {
        return try {
            when (groups.size) {
                4 -> { // dd.mm.yyyy
                    val day = groups[1].toInt()
                    val month = groups[2].toInt()
                    val year = groups[3].toIntOrNull() ?: LocalDate.now().year
                    LocalDate.of(year, month, day)
                }
                3 -> { // dd.mm (aktuelles Jahr)
                    val day = groups[1].toInt()
                    val month = groups[2].toInt()
                    LocalDate.of(LocalDate.now().year, month, day)
                }
                else -> null
            }
        } catch (e: Exception) {
            null
        }
    }

    private fun detectEstimatedTime(input: String): Int? {
        for (pattern in TIME_PATTERNS) {
            val match = pattern.find(input)
            if (match != null) {
                val value = match.groupValues[1].toIntOrNull() ?: continue

                return when {
                    pattern.pattern.contains("std|stunden|h") -> value * 60
                    pattern.pattern.contains("tag|tage|d") -> value * 60 * 8 // 8 Stunden Arbeitstag
                    else -> value // Minuten
                }
            }
        }

        // Quick Win Erkennung
        for (pattern in QUICK_WIN_PATTERNS) {
            if (pattern.containsMatchIn(input)) {
                return 15 // Standard Quick Win Zeit
            }
        }

        return null
    }

    private fun detectEnergyLevel(input: String): EnergyLevel? {
        for ((energy, patterns) in ENERGY_PATTERNS) {
            for (pattern in patterns) {
                if (pattern.containsMatchIn(input)) {
                    return energy
                }
            }
        }
        return null
    }

    private fun detectContext(input: String): TaskContext? {
        for ((context, patterns) in CONTEXT_PATTERNS) {
            for (pattern in patterns) {
                if (pattern.containsMatchIn(input)) {
                    return context
                }
            }
        }
        return null
    }

    private fun detectTags(input: String): List<String> {
        val tags = mutableSetOf<String>()

        // Hashtags (#tag)
        TAG_PATTERN.findAll(input).forEach { match ->
            tags.add(match.groupValues[1].lowercase())
        }

        // At-mentions (@context)
        AT_PATTERN.findAll(input).forEach { match ->
            tags.add(match.groupValues[1].lowercase())
        }

        return tags.toList()
    }

    private fun cleanUpTitle(
        title: String,
        priority: TaskPriority?,
        dueDate: LocalDate?,
        time: Int?,
        energy: EnergyLevel?,
        context: TaskContext?,
        tags: List<String>
    ): String {
        var cleaned = title

        // Entferne erkannte Prioritätswörter
        if (priority != null) {
            for (patterns in PRIORITY_PATTERNS.values) {
                for (pattern in patterns) {
                    cleaned = pattern.replace(cleaned, "")
                }
            }
        }

        // Entferne erkannte Zeitangaben
        if (time != null) {
            for (pattern in TIME_PATTERNS) {
                cleaned = pattern.replace(cleaned, "")
            }
            for (pattern in QUICK_WIN_PATTERNS) {
                cleaned = pattern.replace(cleaned, "")
            }
        }

        // Entferne erkannte Datumsangaben
        if (dueDate != null) {
            for (keyword in DATE_PATTERNS.keys) {
                cleaned = cleaned.replace(keyword, "", ignoreCase = true)
            }
            // Entferne auch absolute Datumsangaben
            cleaned = Regex("""(\d{1,2})\.(\d{1,2})\.?(?:\d{4})?""").replace(cleaned, "")
            cleaned = Regex("""in\s+\d+\s+tag(?:en?)?""", RegexOption.IGNORE_CASE).replace(cleaned, "")
        }

        // Entferne erkannte Energie-Level
        if (energy != null) {
            for (patterns in ENERGY_PATTERNS.values) {
                for (pattern in patterns) {
                    cleaned = pattern.replace(cleaned, "")
                }
            }
        }

        // Entferne erkannte Kontexte
        if (context != null) {
            for (patterns in CONTEXT_PATTERNS.values) {
                for (pattern in patterns) {
                    cleaned = pattern.replace(cleaned, "")
                }
            }
        }

        // Entferne Tags
        cleaned = TAG_PATTERN.replace(cleaned, "")
        cleaned = AT_PATTERN.replace(cleaned, "")

        // Bereinige Leerzeichen und Satzzeichen
        cleaned = cleaned
            .replace(Regex("""\s+"""), " ") // Mehrfache Leerzeichen
            .replace(Regex("""[,\-\s]+$"""), "") // Anhängende Kommas, Bindestriche, Leerzeichen
            .replace(Regex("""^[,\-\s]+"""), "") // Vorangestellte Kommas, Bindestriche, Leerzeichen
            .trim()

        return if (cleaned.isBlank()) title else cleaned
    }

    // Zusätzliche Hilfsfunktionen

    fun getSmartSuggestions(partialInput: String): List<String> {
        val suggestions = mutableListOf<String>()

        when {
            partialInput.contains("morgen", ignoreCase = true) -> {
                suggestions.add("morgen wichtig")
                suggestions.add("morgen 30min")
                suggestions.add("morgen anruf")
            }
            partialInput.contains("email", ignoreCase = true) -> {
                suggestions.add("email beantworten 15min")
                suggestions.add("email senden wichtig")
            }
            partialInput.contains("meeting", ignoreCase = true) -> {
                suggestions.add("meeting vorbereiten 1h")
                suggestions.add("meeting protokoll schreiben")
            }
        }

        return suggestions
    }

    fun getContextSuggestions(input: String): List<TaskContext> {
        val suggestions = mutableListOf<TaskContext>()

        for ((context, patterns) in CONTEXT_PATTERNS) {
            for (pattern in patterns) {
                if (pattern.containsMatchIn(input)) {
                    suggestions.add(context)
                    break
                }
            }
        }

        return suggestions.distinct()
    }
}