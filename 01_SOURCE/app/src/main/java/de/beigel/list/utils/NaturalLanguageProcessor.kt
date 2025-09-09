package de.beigel.list.utils

import de.beigel.list.data.TaskPriority
import de.beigel.list.data.TaskContext
import de.beigel.list.ui.dialogs.ParsedTask
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.*

class NaturalLanguageProcessor {

    companion object {
        // Zeitbasierte Patterns
        private val TIME_PATTERNS = mapOf(
            "heute" to 0,
            "heut" to 0,
            "morgen" to 1,
            "übermorgen" to 2,
            "nächste woche" to 7,
            "nächsten montag" to -1, // Special case
            "nächsten dienstag" to -1,
            "nächsten mittwoch" to -1,
            "nächsten donnerstag" to -1,
            "nächsten freitag" to -1,
            "nächsten samstag" to -1,
            "nächsten sonntag" to -1,
            "in (\\d+) tag(?:en?)?" to -1, // Regex pattern
            "in einer woche" to 7,
            "in (\\d+) woche(?:n)?" to -1, // Regex pattern
            "nächsten monat" to 30,
            "in einem monat" to 30
        )

        // Prioritäts-Keywords
        private val PRIORITY_KEYWORDS = mapOf(
            TaskPriority.HIGH to listOf("wichtig", "urgent", "dringend", "hoch", "kritisch", "sofort", "eilig", "asap"),
            TaskPriority.MEDIUM to listOf("normal", "mittel", "standard"),
            TaskPriority.LOW to listOf("niedrig", "später", "irgendwann", "low", "unwichtig")
        )

        // Zeitschätzungs-Patterns
        private val DURATION_PATTERNS = mapOf(
            "(\\d+)\\s*min(?:uten?)?" to 1,
            "(\\d+)\\s*h(?:ours?)?" to 60,
            "(\\d+)\\s*std(?:unden?)?" to 60,
            "(\\d+)\\s*stunde(?:n)?" to 60,
            "schnell" to 15,
            "kurz" to 30,
            "lange" to 120,
            "den ganzen tag" to 480
        )

        // Kontext-Keywords
        private val CONTEXT_KEYWORDS = mapOf(
            TaskContext.WORK to listOf("arbeit", "büro", "meeting", "präsentation", "projekt", "chef", "kollegen"),
            TaskContext.HOME to listOf("zuhause", "haus", "küche", "putzen", "aufräumen", "familie"),
            TaskContext.ERRANDS to listOf("einkaufen", "supermarkt", "bank", "post", "apotheke", "tankstelle"),
            TaskContext.CALLS to listOf("anrufen", "telefonieren", "termin vereinbaren", "rückruf"),
            TaskContext.COMPUTER to listOf("computer", "laptop", "programmieren", "email", "online", "internet"),
            TaskContext.EXERCISE to listOf("sport", "fitness", "laufen", "joggen", "gym", "training"),
            TaskContext.LEARNING to listOf("lernen", "studieren", "kurs", "lesen", "buch", "tutorial"),
            TaskContext.CREATIVE to listOf("zeichnen", "malen", "schreiben", "musik", "kreativ", "basteln")
        )

        // Stopwords die ignoriert werden sollen
        private val STOPWORDS = setOf(
            "der", "die", "das", "den", "dem", "des", "ein", "eine", "einen", "einem", "einer",
            "und", "oder", "aber", "denn", "sondern", "wenn", "weil", "dass", "ob", "als", "wie",
            "ich", "du", "er", "sie", "es", "wir", "ihr", "mich", "dich", "sich", "uns", "euch",
            "bin", "bist", "ist", "sind", "war", "warst", "waren", "wart", "haben", "habe", "hast", "hat", "habt",
            "mit", "ohne", "für", "gegen", "durch", "um", "an", "auf", "aus", "bei", "nach", "seit", "von", "vor", "zu",
            "muss", "soll", "will", "kann", "mag", "darf", "möchte", "sollte", "könnte", "würde"
        )
    }

    fun parseTaskInput(input: String): ParsedTask {
        val normalizedInput = input.lowercase(Locale.GERMAN).trim()

        if (normalizedInput.isBlank()) {
            return ParsedTask()
        }

        // Parsing in Stufen
        val extractedData = extractAllComponents(normalizedInput)
        val cleanTitle = extractCleanTitle(normalizedInput, extractedData)

        return ParsedTask(
            title = cleanTitle.ifBlank { input.take(50) }, // Fallback
            description = extractedData.description,
            priority = extractedData.priority,
            dueDate = extractedData.dueDate,
            estimatedMinutes = extractedData.estimatedMinutes,
            context = extractedData.context,
            tags = extractedData.tags
        )
    }

    private data class ExtractedData(
        val priority: TaskPriority = TaskPriority.MEDIUM,
        val dueDate: String? = null,
        val estimatedMinutes: Int? = null,
        val context: TaskContext? = null,
        val tags: List<String> = emptyList(),
        val description: String = "",
        val extractedPhrases: Set<String> = emptySet()
    )

    private fun extractAllComponents(input: String): ExtractedData {
        val extractedPhrases = mutableSetOf<String>()

        // Priorität extrahieren
        val priority = extractPriority(input)?.also {
            PRIORITY_KEYWORDS[it]?.forEach { keyword ->
                if (input.contains(keyword)) extractedPhrases.add(keyword)
            }
        } ?: TaskPriority.MEDIUM

        // Datum extrahieren
        val (dueDate, datePhrase) = extractDueDate(input)
        datePhrase?.let { extractedPhrases.add(it) }

        // Zeitschätzung extrahieren
        val (estimatedMinutes, timePhrase) = extractEstimatedTime(input)
        timePhrase?.let { extractedPhrases.add(it) }

        // Kontext extrahieren
        val (context, contextPhrase) = extractContext(input)
        contextPhrase?.let { extractedPhrases.add(it) }

        // Tags extrahieren (# oder @)
        val tags = extractTags(input)
        tags.forEach { extractedPhrases.add("#$it") }
        tags.forEach { extractedPhrases.add("@$it") }

        // Beschreibung aus strukturierten Teilen extrahieren
        val description = extractDescription(input, extractedPhrases)

        return ExtractedData(
            priority = priority,
            dueDate = dueDate,
            estimatedMinutes = estimatedMinutes,
            context = context,
            tags = tags,
            description = description,
            extractedPhrases = extractedPhrases
        )
    }

    private fun extractPriority(input: String): TaskPriority? {
        PRIORITY_KEYWORDS.forEach { (priority, keywords) ->
            keywords.forEach { keyword ->
                if (input.contains(keyword)) {
                    return priority
                }
            }
        }

        // Ausrufezeichen als Indikator für hohe Priorität
        if (input.count { it == '!' } >= 2) {
            return TaskPriority.HIGH
        }

        return null
    }

    private fun extractDueDate(input: String): Pair<String?, String?> {
        val today = LocalDate.now()

        // Direkte Zeit-Patterns
        TIME_PATTERNS.forEach { (pattern, days) ->
            if (pattern.contains("(")) {
                // Regex pattern
                val regex = Regex(pattern)
                val match = regex.find(input)
                if (match != null) {
                    val extractedNumber = match.groupValues.getOrNull(1)?.toIntOrNull()
                    if (extractedNumber != null) {
                        val targetDate = when {
                            pattern.contains("tag") -> today.plusDays(extractedNumber.toLong())
                            pattern.contains("woche") -> today.plusWeeks(extractedNumber.toLong())
                            else -> today.plusDays(extractedNumber.toLong())
                        }
                        return targetDate.toString() to match.value
                    }
                }
            } else {
                if (input.contains(pattern)) {
                    val targetDate = when (days) {
                        -1 -> calculateSpecificWeekday(pattern, today)
                        else -> today.plusDays(days.toLong())
                    }
                    return targetDate.toString() to pattern
                }
            }
        }

        // Absolute Datumsangaben (dd.mm, dd/mm, dd-mm)
        val dateRegex = Regex("(\\d{1,2})[./-](\\d{1,2})(?:[./-](\\d{2,4}))?")
        val dateMatch = dateRegex.find(input)
        if (dateMatch != null) {
            try {
                val day = dateMatch.groupValues[1].toInt()
                val month = dateMatch.groupValues[2].toInt()
                val year = dateMatch.groupValues.getOrNull(3)?.toIntOrNull()
                    ?: today.year

                val targetDate = LocalDate.of(year, month, day)
                // Wenn das Datum in der Vergangenheit liegt, nehme nächstes Jahr
                val finalDate = if (targetDate.isBefore(today)) {
                    targetDate.plusYears(1)
                } else {
                    targetDate
                }

                return finalDate.toString() to dateMatch.value
            } catch (e: Exception) {
                // Ungültiges Datum
            }
        }

        return null to null
    }

    private fun calculateSpecificWeekday(pattern: String, today: LocalDate): LocalDate {
        val weekdayMap = mapOf(
            "montag" to 1, "dienstag" to 2, "mittwoch" to 3, "donnerstag" to 4,
            "freitag" to 5, "samstag" to 6, "sonntag" to 7
        )

        weekdayMap.forEach { (weekday, dayOfWeek) ->
            if (pattern.contains(weekday)) {
                val currentDayOfWeek = today.dayOfWeek.value
                val daysUntilTarget = if (dayOfWeek <= currentDayOfWeek) {
                    7 - currentDayOfWeek + dayOfWeek
                } else {
                    dayOfWeek - currentDayOfWeek
                }
                return today.plusDays(daysUntilTarget.toLong())
            }
        }

        return today.plusWeeks(1) // Fallback
    }

    private fun extractEstimatedTime(input: String): Pair<Int?, String?> {
        DURATION_PATTERNS.forEach { (pattern, multiplier) ->
            if (pattern.contains("(")) {
                // Regex pattern
                val regex = Regex(pattern)
                val match = regex.find(input)
                if (match != null) {
                    val number = match.groupValues.getOrNull(1)?.toIntOrNull()
                    if (number != null) {
                        return (number * multiplier) to match.value
                    }
                }
            } else {
                if (input.contains(pattern)) {
                    return multiplier to pattern
                }
            }
        }

        return null to null
    }

    private fun extractContext(input: String): Pair<TaskContext?, String?> {
        CONTEXT_KEYWORDS.forEach { (context, keywords) ->
            keywords.forEach { keyword ->
                if (input.contains(keyword)) {
                    return context to keyword
                }
            }
        }

        return null to null
    }

    private fun extractTags(input: String): List<String> {
        val tags = mutableListOf<String>()

        // #hashtag style
        val hashtagRegex = Regex("#(\\w+)")
        hashtagRegex.findAll(input).forEach { match ->
            tags.add(match.groupValues[1])
        }

        // @mention style
        val mentionRegex = Regex("@(\\w+)")
        mentionRegex.findAll(input).forEach { match ->
            tags.add(match.groupValues[1])
        }

        return tags.distinct()
    }

    private fun extractDescription(input: String, extractedPhrases: Set<String>): String {
        var cleanInput = input

        // Entferne alle extrahierten Phrasen
        extractedPhrases.forEach { phrase ->
            cleanInput = cleanInput.replace(phrase, "", ignoreCase = true)
        }

        // Entferne mehrfache Leerzeichen und trimme
        cleanInput = cleanInput.replace(Regex("\\s+"), " ").trim()

        // Wenn der bereinigte Text zu kurz ist, keine Beschreibung
        return if (cleanInput.length > 10 && cleanInput != input.trim()) {
            cleanInput
        } else {
            ""
        }
    }

    private fun extractCleanTitle(input: String, extractedData: ExtractedData): String {
        var title = input

        // Entferne extrahierte Phrasen für sauberen Titel
        extractedData.extractedPhrases.forEach { phrase ->
            title = title.replace(phrase, "", ignoreCase = true)
        }

        // Entferne Separatoren (-, |, •)
        title = title.replace(Regex("\\s*[-|•]\\s*"), " ")

        // Entferne Stopwords vom Anfang
        val words = title.split("\\s+".toRegex())
        val cleanWords = words.dropWhile { word ->
            word.lowercase() in STOPWORDS
        }

        // Entferne mehrfache Leerzeichen
        title = cleanWords.joinToString(" ").replace(Regex("\\s+"), " ").trim()

        // Capitalize first letter
        return title.replaceFirstChar {
            if (it.isLowerCase()) it.titlecase(Locale.GERMAN) else it.toString()
        }
    }

    // Hilfsfunktionen für erweiterte Parsing-Features
    fun extractSmartSuggestions(input: String): List<String> {
        val suggestions = mutableListOf<String>()

        // Vorschläge basierend auf häufigen Patterns
        if (input.contains("einkaufen")) {
            suggestions.add("Milch, Brot, Obst hinzufügen?")
        }

        if (input.contains("anrufen") && !input.contains("uhr")) {
            suggestions.add("Uhrzeit für Anruf festlegen?")
        }

        if (input.contains("meeting") && !TIME_PATTERNS.keys.any { input.contains(it) }) {
            suggestions.add("Wann findet das Meeting statt?")
        }

        return suggestions
    }

    fun validateAndSuggestCorrections(parsedTask: ParsedTask): List<String> {
        val suggestions = mutableListOf<String>()

        // Validiere Datum
        parsedTask.dueDate?.let { dateStr ->
            try {
                val date = LocalDate.parse(dateStr)
                val daysUntil = ChronoUnit.DAYS.between(LocalDate.now(), date)

                if (daysUntil < 0) {
                    suggestions.add("⚠️ Datum liegt in der Vergangenheit")
                } else if (daysUntil > 365) {
                    suggestions.add("⚠️ Datum liegt sehr weit in der Zukunft")
                }
            } catch (e: Exception) {
                suggestions.add("⚠️ Ungültiges Datumsformat")
            }
        }

        // Validiere Zeitschätzung
        parsedTask.estimatedMinutes?.let { minutes ->
            if (minutes > 480) { // 8 Stunden
                suggestions.add("💡 Sehr lange Aufgabe - in kleinere Teile aufteilen?")
            } else if (minutes < 5) {
                suggestions.add("💡 Sehr kurze Aufgabe - mit anderer Aufgabe kombinieren?")
            }
        }

        // Titel-Validierung
        if (parsedTask.title.length < 3) {
            suggestions.add("⚠️ Titel ist sehr kurz")
        } else if (parsedTask.title.length > 100) {
            suggestions.add("⚠️ Titel ist sehr lang - kürzen?")
        }

        return suggestions
    }
}