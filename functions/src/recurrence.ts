import { Timestamp } from "firebase-admin/firestore";

/**
 * Wiederholungen: wann ist der nächste Termin, und wer ist als Nächstes dran?
 *
 * Alles hier ist reine Rechnung ohne Firestore, damit sich die Randfälle
 * testen lassen – Monatsenden, Zeitumstellung, überfällige Aufgaben.
 */

/** Zeitzone für die Datumsrechnung, dieselbe wie beim Erinnerungs-Job. */
export const TIME_ZONE = "Europe/Berlin";

export type RecurrenceUnit = "TAG" | "WOCHE" | "MONAT" | "JAHR";

/**
 * Woran hängt der nächste Termin?
 *  - FAELLIG:  am bisherigen Fälligkeitsdatum. Für Termine, die feststehen –
 *              Miete, Müllabfuhr. Drei Tage zu spät abgehakt heißt trotzdem:
 *              nächster Termin wie geplant.
 *  - ERLEDIGT: am Zeitpunkt des Abhakens. Für Aufgaben, bei denen der Abstand
 *              zählt – Blumen gießen, Bettwäsche wechseln.
 */
export type RecurrenceAnchor = "FAELLIG" | "ERLEDIGT";

export interface Recurrence {
  unit: RecurrenceUnit;
  /** Alle wie viel Einheiten, mindestens 1. */
  interval: number;
  anchor: RecurrenceAnchor;
}

/** Prüft eine aus Firestore gelesene Wiederholung, bevor damit gerechnet wird. */
export function parseRecurrence(value: unknown): Recurrence | null {
  if (!value || typeof value !== "object") return null;
  const raw = value as Record<string, unknown>;

  const unit = raw.unit;
  if (unit !== "TAG" && unit !== "WOCHE" && unit !== "MONAT" && unit !== "JAHR") return null;

  const interval = raw.interval;
  if (typeof interval !== "number" || !Number.isInteger(interval) || interval < 1) return null;

  const anchor = raw.anchor === "ERLEDIGT" ? "ERLEDIGT" : "FAELLIG";
  return { unit, interval, anchor };
}

interface DateParts {
  year: number;
  month: number; // 1-12
  day: number;
  hour: number;
  minute: number;
  second: number;
}

const formatter = new Intl.DateTimeFormat("en-US", {
  timeZone: TIME_ZONE,
  hour12: false,
  year: "numeric",
  month: "2-digit",
  day: "2-digit",
  hour: "2-digit",
  minute: "2-digit",
  second: "2-digit",
});

function partsOf(utcMs: number): DateParts {
  const parts = formatter.formatToParts(new Date(utcMs));
  const value = (type: string) => Number(parts.find((p) => p.type === type)?.value ?? 0);
  return {
    year: value("year"),
    month: value("month"),
    day: value("day"),
    // Mitternacht liefert je nach Umgebung 24 statt 0.
    hour: value("hour") % 24,
    minute: value("minute"),
    second: value("second"),
  };
}

/** Abstand der Zeitzone zu UTC für diesen Zeitpunkt, in Millisekunden. */
function offsetAt(utcMs: number): number {
  const p = partsOf(utcMs);
  return Date.UTC(p.year, p.month - 1, p.day, p.hour, p.minute, p.second) - utcMs;
}

/**
 * Ortszeit zurück in einen Zeitpunkt umrechnen.
 *
 * Zwei Durchläufe, weil der Abstand zur UTC selbst vom Ergebnis abhängt: rund
 * um die Zeitumstellung liefert der erste Versuch sonst eine Stunde daneben.
 */
function fromParts(p: DateParts): number {
  const naive = Date.UTC(p.year, p.month - 1, p.day, p.hour, p.minute, p.second);
  const first = naive - offsetAt(naive);
  return naive - offsetAt(first);
}

/** Letzter Tag eines Monats (1-12). */
function daysInMonth(year: number, month: number): number {
  return new Date(Date.UTC(year, month, 0)).getUTCDate();
}

/**
 * Einen Zeitpunkt um `count` Einheiten weiterschieben.
 *
 * Gerechnet wird auf der Ortszeit, nicht auf Millisekunden. Nur so bleibt bei
 * der Zeitumstellung die Uhrzeit erhalten: eine Aufgabe für 8:00 ist eine Woche
 * später wieder um 8:00 fällig und nicht um 7:00 oder 9:00.
 *
 * Der 31. plus einen Monat landet auf dem letzten Tag des Folgemonats. Der
 * Termin wandert dadurch nicht dauerhaft nach vorn, weil immer vom
 * ursprünglichen Datum aus gerechnet wird, nicht vom gekappten.
 */
export function addInterval(
  from: Date,
  unit: RecurrenceUnit,
  count: number
): Date {
  const p = partsOf(from.getTime());

  switch (unit) {
    case "TAG":
      return new Date(fromParts({ ...p, day: p.day + count }));
    case "WOCHE":
      return new Date(fromParts({ ...p, day: p.day + count * 7 }));
    case "MONAT":
    case "JAHR": {
      const added = unit === "JAHR" ? count * 12 : count;
      const total = p.month - 1 + added;
      const year = p.year + Math.floor(total / 12);
      const month = (total % 12) + 1;
      const day = Math.min(p.day, daysInMonth(year, month));
      return new Date(fromParts({ ...p, year, month, day }));
    }
  }
}

/**
 * Nächster Fälligkeitstermin nach dem Abhaken.
 *
 * Bei `FAELLIG` wird so lange weitergeschoben, bis der Termin in der Zukunft
 * liegt. Ohne das bekäme man bei einer wochenlang liegengebliebenen Aufgabe
 * einen Stapel längst vergangener Termine, die der Erinnerungs-Job alle sofort
 * als überfällig abarbeitet.
 */
export function nextDueDate(
  dueDate: Date | null,
  completedAt: Date,
  recurrence: Recurrence
): Date {
  if (recurrence.anchor === "ERLEDIGT" || !dueDate) {
    return addInterval(completedAt, recurrence.unit, recurrence.interval);
  }

  let next = addInterval(dueDate, recurrence.unit, recurrence.interval);
  // Obergrenze als Schutz: bei einer täglichen Aufgabe, die zwei Jahre liegen
  // geblieben ist, sind das sonst hunderte Schleifendurchläufe.
  for (let i = 0; i < 500 && next.getTime() <= completedAt.getTime(); i++) {
    next = addInterval(next, recurrence.unit, recurrence.interval);
  }
  return next;
}

/**
 * Wer ist als Nächstes dran?
 *
 * Reihum durch `rotateAmong`. Wer gerade zuständig war, gibt weiter; steht die
 * Person nicht mehr in der Runde – etwa nach dem Verlassen der Liste – fängt es
 * vorn an. Leere Runde heißt: Zuständigkeit bleibt, wie sie war.
 */
export function nextAssignee(
  rotateAmong: string[],
  currentAssignee: string | null
): string | null {
  if (rotateAmong.length === 0) return currentAssignee;

  const index = currentAssignee ? rotateAmong.indexOf(currentAssignee) : -1;
  if (index < 0) return rotateAmong[0];
  return rotateAmong[(index + 1) % rotateAmong.length];
}

/** Bequemer Wrapper für die Function, die mit Firestore-Timestamps arbeitet. */
export function nextDueTimestamp(
  dueDate: Timestamp | null,
  completedAt: Timestamp,
  recurrence: Recurrence
): Timestamp {
  return Timestamp.fromDate(
    nextDueDate(dueDate ? dueDate.toDate() : null, completedAt.toDate(), recurrence)
  );
}
