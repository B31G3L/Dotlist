package com.beigel.list2share.data

import com.beigel.list2share.R

/**
 * Zuordnung von Einkaufsbegriffen zu Abteilungen.
 *
 * Bewusst eine Liste im Code und kein Dienst: läuft offline, kostet nichts und
 * ist nachvollziehbar. Was nicht erkannt wird, landet unter „Sonstiges" – das
 * ist der Normalfall für alles Ungewöhnliche, kein Fehler.
 *
 * Deutsch und Englisch gemischt, weil in Haushalten beides durcheinander
 * eingetippt wird. Die Stichwörter sind bewusst nicht übersetzt: sie werden
 * gegen die Eingabe verglichen, nicht angezeigt.
 *
 * Die Web-Version hat dieselbe Zuordnung in `web/src/lib/departments.ts`.
 * Wird hier etwas ergänzt, gehört es auch dorthin.
 */
enum class Department(val labelRes: Int) {
    OBST_GEMUESE(R.string.department_produce),
    BACKWAREN(R.string.department_bakery),
    MOLKEREI(R.string.department_dairy),
    FLEISCH_FISCH(R.string.department_meat),
    TIEFKUEHL(R.string.department_frozen),
    VORRAT(R.string.department_pantry),
    GETRAENKE(R.string.department_drinks),
    HAUSHALT(R.string.department_household),
    SONSTIGES(R.string.department_other),
}

private val KEYWORDS: Map<Department, List<String>> = mapOf(
    Department.OBST_GEMUESE to listOf(
        "apfel", "äpfel", "apple", "banane", "banana", "birne", "pear", "orange", "zitrone",
        "lemon", "limette", "lime", "traube", "grape", "erdbeer", "strawberr", "himbeer",
        "raspberr", "blaubeer", "heidelbeer", "blueberr", "kirsche", "cherr", "pfirsich",
        "peach", "melone", "melon", "ananas", "pineapple", "mango", "avocado", "kiwi",
        "pflaume", "plum", "tomate", "tomato", "gurke", "cucumber", "paprika", "zwiebel",
        "onion", "knoblauch", "garlic", "kartoffel", "potato", "karotte", "möhre", "carrot",
        "salat", "lettuce", "spinat", "spinach", "brokkoli", "broccoli", "blumenkohl",
        "cauliflower", "zucchini", "aubergine", "eggplant", "pilz", "champignon", "mushroom",
        "lauch", "leek", "sellerie", "celery", "kürbis", "pumpkin", "ingwer", "ginger",
        "petersilie", "basilikum", "basil", "schnittlauch", "rucola",
    ),
    Department.BACKWAREN to listOf(
        "brot", "bread", "brötchen", "semmel", "baguette", "toast", "croissant", "brezel",
        "pretzel", "kuchen", "cake", "keks", "cookie", "zwieback", "knäckebrot", "crispbread",
    ),
    Department.MOLKEREI to listOf(
        "milch", "milk", "butter", "käse", "cheese", "joghurt", "yogurt", "yoghurt", "quark",
        "sahne", "cream", "schmand", "frischkäse", "mozzarella", "parmesan", "feta", "eier",
        "egg", "margarine", "hüttenkäse",
    ),
    Department.FLEISCH_FISCH to listOf(
        "fleisch", "meat", "hack", "mince", "rind", "beef", "schwein", "pork", "hähnchen",
        "hühner", "chicken", "pute", "turkey", "wurst", "sausage", "schinken", "salami",
        "speck", "bacon", "steak", "schnitzel", "fisch", "fish", "lachs", "salmon",
        "thunfisch", "tuna", "garnele", "shrimp", "forelle",
    ),
    Department.TIEFKUEHL to listOf(
        "tiefkühl", "frozen", "eiscreme", "ice cream", "pizza", "pommes", "fries",
        "fischstäbchen", "fish finger",
    ),
    Department.VORRAT to listOf(
        "mehl", "flour", "zucker", "sugar", "salz", "salt", "pfeffer", "reis", "rice",
        "nudel", "pasta", "spaghetti", "penne", "linsen", "lentil", "bohnen", "bean",
        "kichererbse", "chickpea", "öl", "olivenöl", "oil", "essig", "vinegar", "tomatenmark",
        "passierte tomaten", "dosentomaten", "konserve", "canned", "müsli", "muesli",
        "cereal", "haferflocken", "oat", "marmelade", "jam", "honig", "honey", "schokolade",
        "chocolate", "chips", "nüsse", "nuts", "mandel", "almond", "gewürz", "spice",
        "senf", "mustard", "ketchup", "mayo", "sauce", "soße", "brühe", "broth",
        "backpulver", "hefe", "yeast", "kaffee", "coffee", "tee", "tea", "kakao", "cocoa",
    ),
    Department.GETRAENKE to listOf(
        "wasser", "water", "saft", "juice", "cola", "limo", "limonade", "soda", "bier",
        "beer", "wein", "wine", "sekt", "prosecco", "spirituose", "energy", "eistee",
        "iced tea", "sprudel", "mineralwasser",
    ),
    Department.HAUSHALT to listOf(
        "klopapier", "toilettenpapier", "toilet paper", "küchenrolle", "kitchen roll",
        "taschentuch", "tissue", "spülmittel", "dish soap", "waschmittel", "detergent",
        "weichspüler", "putzmittel", "cleaner", "schwamm", "sponge", "müllbeutel",
        "trash bag", "alufolie", "foil", "frischhalte", "backpapier", "zahnpasta",
        "toothpaste", "zahnbürste", "toothbrush", "shampoo", "duschgel", "shower gel",
        "seife", "soap", "deo", "rasier", "razor", "windel", "diaper", "batterie",
        "battery", "glühbirne", "bulb",
    ),
)

/**
 * Abteilung für einen Eintrag raten.
 *
 * Es gewinnt die längste passende Übereinstimmung. Ohne das würde
 * „Tomatenmark" über „tomate" bei Obst & Gemüse landen statt im Vorrat.
 */
fun departmentFor(title: String): Department {
    val text = title.lowercase()
    var best = Department.SONSTIGES
    var bestLength = 0

    KEYWORDS.forEach { (department, keywords) ->
        keywords.forEach { keyword ->
            if (keyword.length > bestLength && text.contains(keyword)) {
                best = department
                bestLength = keyword.length
            }
        }
    }
    return best
}
