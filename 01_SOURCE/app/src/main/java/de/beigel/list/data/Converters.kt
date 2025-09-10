package de.beigel.list.data

import androidx.room.TypeConverter
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class Converters {

    private val json = Json { ignoreUnknownKeys = true }

    // LocalDateTime Converters
    @TypeConverter
    fun fromLocalDateTime(dateTime: LocalDateTime?): String? {
        return dateTime?.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
    }

    @TypeConverter
    fun toLocalDateTime(dateTimeString: String?): LocalDateTime? {
        return dateTimeString?.let {
            LocalDateTime.parse(it, DateTimeFormatter.ISO_LOCAL_DATE_TIME)
        }
    }

    // LocalDate Converters
    @TypeConverter
    fun fromLocalDate(date: LocalDate?): String? {
        return date?.format(DateTimeFormatter.ISO_LOCAL_DATE)
    }

    @TypeConverter
    fun toLocalDate(dateString: String?): LocalDate? {
        return dateString?.let {
            LocalDate.parse(it, DateTimeFormatter.ISO_LOCAL_DATE)
        }
    }

    // Enum Converters
    @TypeConverter
    fun fromTaskPriority(priority: TaskPriority): String = priority.name

    @TypeConverter
    fun toTaskPriority(priorityString: String): TaskPriority =
        TaskPriority.valueOf(priorityString)

    @TypeConverter
    fun fromEnergyLevel(energyLevel: EnergyLevel): String = energyLevel.name

    @TypeConverter
    fun toEnergyLevel(energyLevelString: String): EnergyLevel =
        EnergyLevel.valueOf(energyLevelString)

    @TypeConverter
    fun fromTaskContext(context: TaskContext): String = context.name

    @TypeConverter
    fun toTaskContext(contextString: String): TaskContext =
        TaskContext.valueOf(contextString)

    @TypeConverter
    fun fromRecurrencePattern(pattern: RecurrencePattern?): String? = pattern?.name

    @TypeConverter
    fun toRecurrencePattern(patternString: String?): RecurrencePattern? =
        patternString?.let { RecurrencePattern.valueOf(it) }

    @TypeConverter
    fun fromUrgencyLevel(urgency: UrgencyLevel): String = urgency.name

    @TypeConverter
    fun toUrgencyLevel(urgencyString: String): UrgencyLevel =
        UrgencyLevel.valueOf(urgencyString)

    // List<String> Converters (für Tags)
    @TypeConverter
    fun fromStringList(tags: List<String>): String {
        return json.encodeToString(tags)
    }

    @TypeConverter
    fun toStringList(tagsString: String): List<String> {
        return try {
            json.decodeFromString(tagsString)
        } catch (e: Exception) {
            emptyList()
        }
    }
}