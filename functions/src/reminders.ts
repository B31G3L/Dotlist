import { Timestamp } from "firebase-admin/firestore";

/**
 * Längste einstellbare Vorlaufzeit (1 Tag, siehe reminderOptions in der App).
 * Begrenzt das Zeitfenster, das der Job abfragen muss.
 */
export const MAX_LEAD_MINUTES = 1440;

/**
 * Aufgaben, deren Erinnerungszeitpunkt länger als das zurückliegt, bekommen
 * keine Benachrichtigung mehr – etwa weil die Funktion stundenlang nicht lief.
 * Sie werden trotzdem abgehakt, damit der Job sie nicht ewig erneut findet.
 */
export const STALE_AFTER_MINUTES = 180;

/** Die Felder eines Todo-Dokuments, die für die Entscheidung zählen. */
export interface TodoFields {
  dueDate: unknown;
  reminderMinutes: unknown;
  isDone: unknown;
  assignedTo: unknown;
  createdBy: unknown;
}

export type ReminderDecision =
  /** Nichts tun – die Erinnerung ist noch nicht dran oder gar nicht eingestellt. */
  | { action: "leave" }
  /** Nur `reminderSent` setzen, keine Benachrichtigung (erledigt oder zu spät). */
  | { action: "mark" }
  /** Benachrichtigung anlegen und `reminderSent` setzen. */
  | { action: "notify"; recipientId: string };

/**
 * Entscheidet, was mit einem gefundenen Todo passieren soll.
 *
 * Als reine Funktion gehalten, damit sich die Randfälle ohne Firestore prüfen
 * lassen: fehlende Vorlaufzeit, `dueDate: null`, bereits erledigt, verpasste
 * Erinnerung, kein Empfänger.
 */
export function reminderDecision(todo: TodoFields, now: number): ReminderDecision {
  // Ein `dueDate <= X`-Filter liefert in Firestore auch Dokumente mit null
  // zurück, weil null vor allen Zeitstempeln sortiert.
  if (!(todo.dueDate instanceof Timestamp)) return { action: "leave" };
  if (typeof todo.reminderMinutes !== "number" || todo.reminderMinutes <= 0) {
    return { action: "leave" };
  }

  const remindAt = todo.dueDate.toMillis() - todo.reminderMinutes * 60_000;
  if (remindAt > now) return { action: "leave" };

  if (todo.isDone === true) return { action: "mark" };
  if (remindAt < now - STALE_AFTER_MINUTES * 60_000) return { action: "mark" };

  const recipientId =
    typeof todo.assignedTo === "string" && todo.assignedTo
      ? todo.assignedTo
      : todo.createdBy;
  if (typeof recipientId !== "string" || !recipientId) return { action: "mark" };

  return { action: "notify", recipientId };
}
