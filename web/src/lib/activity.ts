import type { TodoItem, TodoList } from "./types";

/**
 * Verlauf einer Liste.
 *
 * Bewusst abgeleitet aus `doneBy` und `doneAt` der Todos statt aus einer
 * eigenen Collection: das kostet keine zusätzlichen Schreibvorgänge und
 * braucht keine Aufräumregel. Der Preis dafür ist, dass nur das Abhaken
 * auftaucht und immer nur der letzte Stand – wer abhakt und wieder aufmacht,
 * verschwindet aus dem Verlauf.
 *
 * Für echten Verlauf („Tom hat Brot hinzugefügt", „Jana hat den Preis
 * geändert") bräuchte es eine Subcollection und eine Cloud Function, die sie
 * füllt. Solange die einfache Variante reicht, lohnt das nicht.
 */
export interface ActivityEntry {
  todoId: string;
  title: string;
  /** Anzeigename; leer, wenn die Person nicht mehr in der Liste steht. */
  who: string;
  at: Date;
}

/** Wie viele Einträge der Verlauf höchstens zeigt. */
const LIMIT = 20;

export function activityOf(list: TodoList, todos: TodoItem[]): ActivityEntry[] {
  return todos
    .filter((todo) => todo.isDone && todo.doneAt !== null)
    .map((todo) => ({
      todoId: todo.id,
      title: todo.title,
      who: todo.doneBy ? (list.memberNames[todo.doneBy] ?? "") : "",
      at: todo.doneAt!.toDate(),
    }))
    .sort((a, b) => b.at.getTime() - a.at.getTime())
    .slice(0, LIMIT);
}

/**
 * „vor 5 Minuten", „gestern", „12.09.".
 *
 * Intl.RelativeTimeFormat übernimmt die Sprachformen; die Auswahl der Einheit
 * bleibt hier, weil die Bibliothek nur formatiert und nicht entscheidet.
 */
const relative = new Intl.RelativeTimeFormat("de-DE", { numeric: "auto" });
const shortDate = new Intl.DateTimeFormat("de-DE", { day: "2-digit", month: "2-digit" });

export function formatWhen(at: Date, now: Date = new Date()): string {
  const seconds = Math.round((at.getTime() - now.getTime()) / 1000);
  const absolute = Math.abs(seconds);

  if (absolute < 60) return "gerade eben";
  if (absolute < 3600) return relative.format(Math.round(seconds / 60), "minute");
  if (absolute < 86400) return relative.format(Math.round(seconds / 3600), "hour");
  // Ab einer Woche wird „vor 9 Tagen" unhandlich – dann lieber das Datum.
  if (absolute < 7 * 86400) return relative.format(Math.round(seconds / 86400), "day");
  return shortDate.format(at);
}
