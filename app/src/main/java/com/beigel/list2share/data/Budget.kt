package com.beigel.list2share.data

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/*
 * Monatsbudget im Anschaffungsmodus.
 *
 * Gekauft ist, was abgehakt wurde; der Kaufmonat ist der Monat von doneAt.
 * Budgets stehen an der Liste als Map "yyyy-MM" → Betrag. Ein Eintrag gilt ab
 * seinem Monat, bis ein späterer ihn ablöst. So bleibt ein vergangener Monat
 * bei seinem damaligen Budget, auch wenn es später geändert wird, und man
 * muss nicht jeden Monat neu eintragen.
 */

/** Monatsschlüssel "yyyy-MM" – sortiert als Text genauso wie zeitlich. */
fun monthKeyOf(millis: Long): String {
    val cal = Calendar.getInstance().apply { timeInMillis = millis }
    return monthKeyOf(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH) + 1)
}

fun monthKeyOf(year: Int, month: Int): String = "%04d-%02d".format(Locale.ROOT, year, month)

fun currentMonthKey(): String = monthKeyOf(System.currentTimeMillis())

/** Vormonat zu einem Schlüssel "yyyy-MM". */
fun previousMonthKey(key: String): String {
    val (y, m) = key.split("-").map { it.toInt() }
    return if (m == 1) monthKeyOf(y - 1, 12) else monthKeyOf(y, m - 1)
}

/** „Oktober 2026" in der Gerätesprache. */
fun formatMonthKey(key: String): String {
    val (y, m) = key.split("-").map { it.toInt() }
    val cal = Calendar.getInstance().apply {
        clear()
        set(y, m - 1, 1)
    }
    // LLLL = eigenständige Monatsform („Oktober", nicht „Oktobers").
    return SimpleDateFormat("LLLL yyyy", Locale.getDefault()).format(Date(cal.timeInMillis))
        .replaceFirstChar { it.titlecase(Locale.getDefault()) }
}

/** Budget, das im Monat [key] gilt, oder null, wenn keins gesetzt ist. */
fun TodoList.budgetFor(key: String): Double? =
    monthlyBudgets.filterKeys { it <= key }
        .maxByOrNull { it.key }
        ?.value
        ?.takeIf { it > 0.0 }

/** Kaufmonat eines erledigten Eintrags, null für offene. */
val TodoItem.purchaseMonthKey: String?
    get() = if (isDone) doneAt?.let { monthKeyOf(it.toDate().time) } else null

/**
 * Zusammenfassung eines Monats.
 *
 * @param spent       Summe der Preise aller in diesem Monat gekauften Einträge
 * @param withoutPrice gekaufte Einträge ohne Preis – sie fehlen in [spent]
 */
data class MonthSummary(
    val key: String,
    val budget: Double?,
    val spent: Double,
    val withoutPrice: Int,
    val items: List<TodoItem>,
) {
    val remaining: Double? get() = budget?.let { it - spent }
}

fun monthSummary(list: TodoList, todos: List<TodoItem>, key: String): MonthSummary {
    val items = todos.filter { it.purchaseMonthKey == key }
        .sortedByDescending { it.doneAt?.toDate()?.time ?: 0L }
    return MonthSummary(
        key          = key,
        budget       = list.budgetFor(key),
        spent        = items.sumOf { it.price ?: 0.0 },
        withoutPrice = items.count { it.price == null },
        items        = items,
    )
}

/**
 * Alle Monate vor dem laufenden, neuester zuerst – vom ersten Monat mit einem
 * Kauf oder Budget an. Monate ohne Käufe erscheinen mit, wenn ein Budget galt:
 * auch „nichts ausgegeben" ist eine Auskunft.
 */
fun pastMonthSummaries(list: TodoList, todos: List<TodoItem>, now: String = currentMonthKey()): List<MonthSummary> {
    val firstKey = (todos.mapNotNull { it.purchaseMonthKey } + list.monthlyBudgets.keys)
        .filter { it < now }
        .minOrNull() ?: return emptyList()

    val result = mutableListOf<MonthSummary>()
    var key = previousMonthKey(now)
    while (key >= firstKey) {
        val summary = monthSummary(list, todos, key)
        if (summary.items.isNotEmpty() || summary.budget != null) result += summary
        key = previousMonthKey(key)
    }
    return result
}
