/**
 * Tests der Wiederholungsrechnung. Ausführen: npm run build && node --test lib/
 */
import { strict as assert } from "node:assert";
import { test } from "node:test";

import {
  addInterval,
  nextAssignee,
  nextDueDate,
  parseRecurrence,
  type Recurrence,
} from "./recurrence";

/** Hilfsfunktion: Ortszeit in Europe/Berlin als ISO-Text, zum Vergleichen. */
function berlin(date: Date): string {
  return new Intl.DateTimeFormat("sv-SE", {
    timeZone: "Europe/Berlin",
    hour12: false,
    year: "numeric",
    month: "2-digit",
    day: "2-digit",
    hour: "2-digit",
    minute: "2-digit",
  }).format(date);
}

/** Zeitpunkt aus Berliner Ortszeit. */
function at(text: string): Date {
  // Nur für die Tests: der Offset wird über den Vergleich ermittelt.
  const naive = new Date(`${text}Z`);
  const guess = new Date(naive.getTime());
  const shown = new Date(`${berlin(guess).replace(" ", "T")}:00Z`);
  return new Date(naive.getTime() - (shown.getTime() - guess.getTime()));
}

const weekly: Recurrence = { unit: "WOCHE", interval: 1, anchor: "FAELLIG" };

test("wöchentlich schiebt um sieben Tage", () => {
  const next = nextDueDate(at("2026-03-02T08:00"), at("2026-03-02T09:00"), weekly);
  assert.equal(berlin(next), "2026-03-09 08:00");
});

test("Zeitumstellung: Uhrzeit bleibt erhalten", () => {
  // Umstellung auf Sommerzeit in der Nacht auf den 29.03.2026.
  const next = nextDueDate(at("2026-03-27T08:00"), at("2026-03-27T09:00"), weekly);
  assert.equal(berlin(next), "2026-04-03 08:00");
});

test("monatlich vom 31. landet auf dem letzten Tag des Folgemonats", () => {
  const monthly: Recurrence = { unit: "MONAT", interval: 1, anchor: "FAELLIG" };
  const next = nextDueDate(at("2026-01-31T10:00"), at("2026-01-31T11:00"), monthly);
  assert.equal(berlin(next), "2026-02-28 10:00");
});

test("gekappter Tag wandert nicht dauerhaft nach vorn", () => {
  // Vom 31. Januar aus zwei Monate: Ziel ist der 31. März, nicht der 28.
  const next = addInterval(at("2026-01-31T10:00"), "MONAT", 2);
  assert.equal(berlin(next), "2026-03-31 10:00");
});

test("29. Februar fällt im Folgejahr auf den 28.", () => {
  const next = addInterval(at("2028-02-29T09:00"), "JAHR", 1);
  assert.equal(berlin(next), "2029-02-28 09:00");
});

test("jährlich trifft im Schaltjahr wieder den 29.", () => {
  const next = addInterval(at("2028-02-29T09:00"), "JAHR", 4);
  assert.equal(berlin(next), "2032-02-29 09:00");
});

test("FAELLIG überspringt vergangene Termine bis in die Zukunft", () => {
  // Drei Wochen liegengeblieben – der nächste Termin liegt nach dem Abhaken.
  const next = nextDueDate(at("2026-03-02T08:00"), at("2026-03-24T19:00"), weekly);
  assert.equal(berlin(next), "2026-03-30 08:00");
});

test("ERLEDIGT rechnet ab dem Abhaken", () => {
  const fromDone: Recurrence = { unit: "TAG", interval: 3, anchor: "ERLEDIGT" };
  const next = nextDueDate(at("2026-03-02T08:00"), at("2026-03-24T19:00"), fromDone);
  assert.equal(berlin(next), "2026-03-27 19:00");
});

test("ohne Fälligkeit wird ab dem Abhaken gerechnet", () => {
  const next = nextDueDate(null, at("2026-03-24T19:00"), weekly);
  assert.equal(berlin(next), "2026-03-31 19:00");
});

test("Rotation geht reihum", () => {
  const runde = ["a", "b", "c"];
  assert.equal(nextAssignee(runde, "a"), "b");
  assert.equal(nextAssignee(runde, "c"), "a");
});

test("unbekannte oder fehlende Zuständigkeit startet vorn", () => {
  assert.equal(nextAssignee(["a", "b"], null), "a");
  assert.equal(nextAssignee(["a", "b"], "weg"), "a");
});

test("leere Runde lässt die Zuständigkeit unverändert", () => {
  assert.equal(nextAssignee([], "a"), "a");
  assert.equal(nextAssignee([], null), null);
});

test("kaputte Wiederholungen aus Firestore werden abgelehnt", () => {
  assert.equal(parseRecurrence(null), null);
  assert.equal(parseRecurrence({ unit: "STUNDE", interval: 1 }), null);
  assert.equal(parseRecurrence({ unit: "TAG", interval: 0 }), null);
  assert.equal(parseRecurrence({ unit: "TAG", interval: 1.5 }), null);
  assert.deepEqual(parseRecurrence({ unit: "TAG", interval: 2 }), {
    unit: "TAG",
    interval: 2,
    anchor: "FAELLIG",
  });
});
