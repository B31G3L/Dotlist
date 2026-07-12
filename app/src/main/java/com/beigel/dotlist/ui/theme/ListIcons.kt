package com.beigel.dotlist.ui.theme

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import com.beigel.dotlist.R
import com.beigel.dotlist.data.TodoList

// ─── Icon-Kategorien ──────────────────────────────────────────────────────────

enum class ListIconCategory(val label: String) {
    ZEIT("Zeit"),
    REISEN("Reisen"),
    FEIERN("Feiern"),
    ARBEIT("Arbeit"),
    SPORT("Sport"),
    NATUR("Natur"),
    ZUHAUSE("Zuhause"),
    SONSTIGES("Sonstiges"),
}

/** Lokalisiertes Label für die UI (ersetzt das interne, deutsche [ListIconCategory.label]). */
@Composable
fun ListIconCategory.displayLabel(): String = when (this) {
    ListIconCategory.ZEIT      -> stringResource(R.string.icon_category_time)
    ListIconCategory.REISEN    -> stringResource(R.string.icon_category_travel)
    ListIconCategory.FEIERN    -> stringResource(R.string.icon_category_celebrate)
    ListIconCategory.ARBEIT    -> stringResource(R.string.icon_category_work)
    ListIconCategory.SPORT     -> stringResource(R.string.icon_category_sport)
    ListIconCategory.NATUR     -> stringResource(R.string.icon_category_nature)
    ListIconCategory.ZUHAUSE   -> stringResource(R.string.icon_category_home)
    ListIconCategory.SONSTIGES -> stringResource(R.string.icon_category_other)
}

data class ListIconOption(
    val name    : String,
    val vector  : ImageVector,
    val category: ListIconCategory,
)

// ─── Vollständige Icon-Liste ──────────────────────────────────────────────────

val ALL_LIST_ICONS: List<ListIconOption> = listOf(

    // ── Zeit ──────────────────────────────────────────────────────────────────
    ListIconOption("Timer",          Icons.Default.Timer,          ListIconCategory.ZEIT),
    ListIconOption("Alarm",          Icons.Default.Alarm,          ListIconCategory.ZEIT),
    ListIconOption("Schedule",       Icons.Default.Schedule,       ListIconCategory.ZEIT),
    ListIconOption("Event",          Icons.Default.Event,          ListIconCategory.ZEIT),
    ListIconOption("EventAvailable", Icons.Default.EventAvailable, ListIconCategory.ZEIT),
    ListIconOption("Today",          Icons.Default.Today,          ListIconCategory.ZEIT),
    ListIconOption("CalendarMonth",  Icons.Default.CalendarMonth,  ListIconCategory.ZEIT),
    ListIconOption("DateRange",      Icons.Default.DateRange,      ListIconCategory.ZEIT),

    // ── Reisen ────────────────────────────────────────────────────────────────
    ListIconOption("Flight",         Icons.Default.Flight,         ListIconCategory.REISEN),
    ListIconOption("FlightTakeoff",  Icons.Default.FlightTakeoff,  ListIconCategory.REISEN),
    ListIconOption("Train",          Icons.Default.Train,          ListIconCategory.REISEN),
    ListIconOption("DirectionsCar",  Icons.Default.DirectionsCar,  ListIconCategory.REISEN),
    ListIconOption("Luggage",        Icons.Default.Luggage,        ListIconCategory.REISEN),
    ListIconOption("Map",            Icons.Default.Map,            ListIconCategory.REISEN),
    ListIconOption("Place",          Icons.Default.Place,          ListIconCategory.REISEN),
    ListIconOption("BeachAccess",    Icons.Default.BeachAccess,    ListIconCategory.REISEN),
    ListIconOption("Sailing",        Icons.Default.Sailing,        ListIconCategory.REISEN),

    // ── Feiern ────────────────────────────────────────────────────────────────
    ListIconOption("Cake",           Icons.Default.Cake,           ListIconCategory.FEIERN),
    ListIconOption("Celebration",    Icons.Default.Celebration,    ListIconCategory.FEIERN),
    ListIconOption("CardGiftcard",   Icons.Default.CardGiftcard,   ListIconCategory.FEIERN),
    ListIconOption("Favorite",       Icons.Default.Favorite,       ListIconCategory.FEIERN),
    ListIconOption("Star",           Icons.Default.Star,           ListIconCategory.FEIERN),
    ListIconOption("EmojiEvents",    Icons.Default.EmojiEvents,    ListIconCategory.FEIERN),
    ListIconOption("Redeem",         Icons.Default.Redeem,         ListIconCategory.FEIERN),
    ListIconOption("LocalBar",       Icons.Default.LocalBar,       ListIconCategory.FEIERN),
    ListIconOption("Nightlife",      Icons.Default.Nightlife,      ListIconCategory.FEIERN),

    // ── Arbeit & Schule ───────────────────────────────────────────────────────
    ListIconOption("Work",           Icons.Default.Work,           ListIconCategory.ARBEIT),
    ListIconOption("School",         Icons.Default.School,         ListIconCategory.ARBEIT),
    ListIconOption("MenuBook",       Icons.Default.MenuBook,       ListIconCategory.ARBEIT),
    ListIconOption("Assignment",     Icons.Default.Assignment,     ListIconCategory.ARBEIT),
    ListIconOption("Task",           Icons.Default.Task,           ListIconCategory.ARBEIT),
    ListIconOption("Checklist",      Icons.Default.Checklist,      ListIconCategory.ARBEIT),
    ListIconOption("Laptop",         Icons.Default.Laptop,         ListIconCategory.ARBEIT),
    ListIconOption("Savings",        Icons.Default.Savings,        ListIconCategory.ARBEIT),
    ListIconOption("AttachMoney",    Icons.Default.AttachMoney,    ListIconCategory.ARBEIT),

    // ── Sport & Fitness ───────────────────────────────────────────────────────
    ListIconOption("FitnessCenter",  Icons.Default.FitnessCenter,  ListIconCategory.SPORT),
    ListIconOption("SportsScore",    Icons.Default.SportsScore,    ListIconCategory.SPORT),
    ListIconOption("DirectionsRun",  Icons.Default.DirectionsRun,  ListIconCategory.SPORT),
    ListIconOption("Pool",           Icons.Default.Pool,           ListIconCategory.SPORT),
    ListIconOption("Hiking",         Icons.Default.Hiking,         ListIconCategory.SPORT),
    ListIconOption("SportsEsports",  Icons.Default.SportsEsports,  ListIconCategory.SPORT),
    ListIconOption("SportsMartialArts", Icons.Default.SportsMartialArts, ListIconCategory.SPORT),

    // ── Natur ─────────────────────────────────────────────────────────────────
    ListIconOption("WbSunny",        Icons.Default.WbSunny,        ListIconCategory.NATUR),
    ListIconOption("AcUnit",         Icons.Default.AcUnit,         ListIconCategory.NATUR),
    ListIconOption("Spa",            Icons.Default.Spa,            ListIconCategory.NATUR),
    ListIconOption("LocalFlorist",   Icons.Default.LocalFlorist,   ListIconCategory.NATUR),
    ListIconOption("Park",           Icons.Default.Park,           ListIconCategory.NATUR),
    ListIconOption("Eco",            Icons.Default.Eco,            ListIconCategory.NATUR),
    ListIconOption("Pets",           Icons.Default.Pets,           ListIconCategory.NATUR),

    // ── Zuhause & Familie ─────────────────────────────────────────────────────
    ListIconOption("Home",           Icons.Default.Home,           ListIconCategory.ZUHAUSE),
    ListIconOption("Cottage",        Icons.Default.Cottage,        ListIconCategory.ZUHAUSE),
    ListIconOption("People",         Icons.Default.People,         ListIconCategory.ZUHAUSE),
    ListIconOption("Person",         Icons.Default.Person,         ListIconCategory.ZUHAUSE),
    ListIconOption("ChildCare",      Icons.Default.ChildCare,      ListIconCategory.ZUHAUSE),
    ListIconOption("ShoppingCart",   Icons.Default.ShoppingCart,   ListIconCategory.ZUHAUSE),
    ListIconOption("Handshake",      Icons.Default.Handshake,      ListIconCategory.ZUHAUSE),

    // ── Sonstiges ─────────────────────────────────────────────────────────────
    ListIconOption("MusicNote",      Icons.Default.MusicNote,      ListIconCategory.SONSTIGES),
    ListIconOption("Movie",          Icons.Default.Movie,          ListIconCategory.SONSTIGES),
    ListIconOption("Restaurant",     Icons.Default.Restaurant,     ListIconCategory.SONSTIGES),
    ListIconOption("LocalCafe",      Icons.Default.LocalCafe,      ListIconCategory.SONSTIGES),
    ListIconOption("Flag",           Icons.Default.Flag,           ListIconCategory.SONSTIGES),
    ListIconOption("Bolt",           Icons.Default.Bolt,           ListIconCategory.SONSTIGES),
    ListIconOption("Diamond",        Icons.Default.Diamond,        ListIconCategory.SONSTIGES),
    ListIconOption("AutoAwesome",    Icons.Default.AutoAwesome,    ListIconCategory.SONSTIGES),
    ListIconOption("Brush",          Icons.Default.Brush,          ListIconCategory.SONSTIGES),
    ListIconOption("Code",           Icons.Default.Code,           ListIconCategory.SONSTIGES),
    ListIconOption("PhotoCamera",    Icons.Default.PhotoCamera,    ListIconCategory.SONSTIGES),
    ListIconOption("Backpack",       Icons.Default.Backpack,       ListIconCategory.SONSTIGES),
    ListIconOption("Lightbulb",      Icons.Default.Lightbulb,      ListIconCategory.SONSTIGES),
    ListIconOption("Build",          Icons.Default.Build,          ListIconCategory.SONSTIGES),
)

// ─── Index & Hilfsfunktionen ───────────────────────────────────────────────────

private val ICON_MAP: Map<String, ImageVector> by lazy {
    ALL_LIST_ICONS.associate { it.name to it.vector }
}

/** Icons nach Kategorie gruppiert (für den Auswahl-Sheet). */
val ICONS_BY_CATEGORY: Map<ListIconCategory, List<ListIconOption>> by lazy {
    ALL_LIST_ICONS.groupBy { it.category }
}

/** Standard-Icon für neue Listen. */
const val DEFAULT_LIST_ICON_NAME = "Checklist"

/** Liefert das ImageVector zum gespeicherten Icon-Namen, Fallback: Checklist. */
fun listIconByName(name: String): ImageVector =
    ICON_MAP[name] ?: ICON_MAP.getValue(DEFAULT_LIST_ICON_NAME)

private val fallbackRotation: List<ImageVector> = listOf(
    ICON_MAP.getValue("Work"), ICON_MAP.getValue("Home"), ICON_MAP.getValue("ShoppingCart"),
    ICON_MAP.getValue("Favorite"), ICON_MAP.getValue("School"), ICON_MAP.getValue("FitnessCenter"),
    ICON_MAP.getValue("Flight"), ICON_MAP.getValue("Star"),
)

/**
 * Icon für eine bestehende Liste ermitteln.
 * Listen mit gespeichertem, bekanntem `icon`-Namen zeigen genau dieses Symbol.
 * Ältere Listen ohne (oder mit unbekanntem) Icon-Namen fallen auf die alte,
 * positionsbasierte Rotation zurück, damit sich ihr Symbol nicht rückwirkend ändert.
 */
fun iconFor(list: TodoList, fallbackIndex: Int): ImageVector =
    ICON_MAP[list.icon] ?: fallbackRotation[fallbackIndex.mod(fallbackRotation.size)]