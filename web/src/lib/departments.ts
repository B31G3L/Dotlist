/**
 * Zuordnung von Einkaufsbegriffen zu Abteilungen.
 *
 * Bewusst eine Liste im Code und keine Erkennung über einen Dienst: das läuft
 * offline, kostet nichts und ist nachvollziehbar. Was nicht erkannt wird,
 * landet unter „Sonstiges" – das ist kein Fehler, sondern der Normalfall für
 * alles Ungewöhnliche.
 *
 * Deutsch und Englisch in einer Liste, weil in gemischten Haushalten beides
 * durcheinander eingetippt wird.
 */

export type Department =
  | "OBST_GEMUESE"
  | "BACKWAREN"
  | "MOLKEREI"
  | "FLEISCH_FISCH"
  | "TIEFKUEHL"
  | "VORRAT"
  | "GETRAENKE"
  | "HAUSHALT"
  | "SONSTIGES";

/** Reihenfolge der Anzeige – grob ein Ladenrundgang. */
export const DEPARTMENT_ORDER: Department[] = [
  "OBST_GEMUESE",
  "BACKWAREN",
  "MOLKEREI",
  "FLEISCH_FISCH",
  "TIEFKUEHL",
  "VORRAT",
  "GETRAENKE",
  "HAUSHALT",
  "SONSTIGES",
];

export const DEPARTMENT_LABELS: Record<Department, string> = {
  OBST_GEMUESE: "Obst & Gemüse",
  BACKWAREN: "Backwaren",
  MOLKEREI: "Molkerei & Eier",
  FLEISCH_FISCH: "Fleisch & Fisch",
  TIEFKUEHL: "Tiefkühl",
  VORRAT: "Vorrat",
  GETRAENKE: "Getränke",
  HAUSHALT: "Haushalt & Drogerie",
  SONSTIGES: "Sonstiges",
};

/**
 * Stichwörter je Abteilung.
 *
 * Zusammengesetzte Begriffe dürfen mehrfach auftauchen: es gewinnt die längste
 * Übereinstimmung, deshalb landet „Tomatenmark" im Vorrat und nicht bei Obst.
 */
const KEYWORDS: Record<Exclude<Department, "SONSTIGES">, string[]> = {
  OBST_GEMUESE: [
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
  ],
  BACKWAREN: [
    "brot", "bread", "brötchen", "semmel", "baguette", "toast", "croissant", "brezel",
    "pretzel", "kuchen", "cake", "keks", "cookie", "zwieback", "knäckebrot", "crispbread",
  ],
  MOLKEREI: [
    "milch", "milk", "butter", "käse", "cheese", "joghurt", "yogurt", "yoghurt", "quark",
    "sahne", "cream", "schmand", "frischkäse", "mozzarella", "parmesan", "feta", "eier",
    "egg", "margarine", "hüttenkäse",
  ],
  FLEISCH_FISCH: [
    "fleisch", "meat", "hack", "mince", "rind", "beef", "schwein", "pork", "hähnchen",
    "hühner", "chicken", "pute", "turkey", "wurst", "sausage", "schinken", "salami",
    "speck", "bacon", "steak", "schnitzel", "fisch", "fish", "lachs", "salmon",
    "thunfisch", "tuna", "garnele", "shrimp", "forelle",
  ],
  TIEFKUEHL: [
    "tiefkühl", "frozen", "eiscreme", "ice cream", "pizza", "pommes", "fries",
    "fischstäbchen", "fish finger",
  ],
  VORRAT: [
    "mehl", "flour", "zucker", "sugar", "salz", "salt", "pfeffer", "reis", "rice",
    "nudel", "pasta", "spaghetti", "penne", "linsen", "lentil", "bohnen", "bean",
    "kichererbse", "chickpea", "öl", "olivenöl", "oil", "essig", "vinegar", "tomatenmark",
    "passierte tomaten", "dosentomaten", "konserve", "canned", "müsli", "muesli",
    "cereal", "haferflocken", "oat", "marmelade", "jam", "honig", "honey", "schokolade",
    "chocolate", "chips", "nüsse", "nuts", "mandel", "almond", "gewürz", "spice",
    "senf", "mustard", "ketchup", "mayo", "sauce", "soße", "brühe", "broth",
    "backpulver", "hefe", "yeast", "kaffee", "coffee", "tee", "tea", "kakao", "cocoa",
  ],
  GETRAENKE: [
    "wasser", "water", "saft", "juice", "cola", "limo", "limonade", "soda", "bier",
    "beer", "wein", "wine", "sekt", "prosecco", "spirituose", "energy", "eistee",
    "iced tea", "sprudel", "mineralwasser",
  ],
  HAUSHALT: [
    "klopapier", "toilettenpapier", "toilet paper", "küchenrolle", "kitchen roll",
    "taschentuch", "tissue", "spülmittel", "dish soap", "waschmittel", "detergent",
    "weichspüler", "putzmittel", "cleaner", "schwamm", "sponge", "müllbeutel",
    "trash bag", "alufolie", "foil", "frischhalte", "backpapier", "zahnpasta",
    "toothpaste", "zahnbürste", "toothbrush", "shampoo", "duschgel", "shower gel",
    "seife", "soap", "deo", "rasier", "razor", "windel", "diaper", "batterie",
    "battery", "glühbirne", "bulb",
  ],
};

/**
 * Abteilung für einen Eintrag raten.
 *
 * Es gewinnt die längste passende Übereinstimmung. Ohne das würde „Tomatenmark"
 * über „tomate" bei Obst & Gemüse landen.
 */
export function departmentFor(title: string): Department {
  const text = title.toLowerCase();

  let best: Department = "SONSTIGES";
  let bestLength = 0;

  for (const [department, keywords] of Object.entries(KEYWORDS)) {
    for (const keyword of keywords) {
      if (keyword.length > bestLength && text.includes(keyword)) {
        best = department as Department;
        bestLength = keyword.length;
      }
    }
  }
  return best;
}
