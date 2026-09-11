/**
 * Tests der Erinnerungs-Entscheidung. Bewusst ohne Test-Framework über den
 * node:test-Runner, damit die Functions keine zusätzliche Abhängigkeit
 * bekommen. Ausführen: npm run build && node --test lib/
 */
import { strict as assert } from "node:assert";
import { test } from "node:test";
import { Timestamp } from "firebase-admin/firestore";

import { reminderDecision, STALE_AFTER_MINUTES, TodoFields } from "./reminders";

const NOW = Date.UTC(2026, 8, 11, 12, 0, 0);
const MINUTE = 60_000;

function todo(overrides: Partial<TodoFields> = {}): TodoFields {
  return {
    dueDate: Timestamp.fromMillis(NOW + 10 * MINUTE),
    reminderMinutes: 10,
    isDone: false,
    assignedTo: null,
    createdBy: "uid-ersteller",
    ...overrides,
  };
}

test("genau fällige Erinnerung geht an den Ersteller", () => {
  assert.deepEqual(reminderDecision(todo(), NOW), {
    action: "notify",
    recipientId: "uid-ersteller",
  });
});

test("zugewiesene Aufgabe erinnert die zugewiesene Person", () => {
  assert.deepEqual(reminderDecision(todo({ assignedTo: "uid-jana" }), NOW), {
    action: "notify",
    recipientId: "uid-jana",
  });
});

test("noch nicht fällig bleibt liegen", () => {
  const future = todo({ dueDate: Timestamp.fromMillis(NOW + 30 * MINUTE) });
  assert.deepEqual(reminderDecision(future, NOW), { action: "leave" });
});

test("dueDate null wird ignoriert (Firestore liefert null im <=-Filter mit)", () => {
  assert.deepEqual(reminderDecision(todo({ dueDate: null }), NOW), { action: "leave" });
});

test("ohne eingestellte Vorlaufzeit passiert nichts", () => {
  assert.deepEqual(reminderDecision(todo({ reminderMinutes: null }), NOW), { action: "leave" });
  assert.deepEqual(reminderDecision(todo({ reminderMinutes: 0 }), NOW), { action: "leave" });
});

test("erledigte Aufgabe wird nur abgehakt", () => {
  assert.deepEqual(reminderDecision(todo({ isDone: true }), NOW), { action: "mark" });
});

test("lange verpasste Erinnerung wird abgehakt statt verspätet zugestellt", () => {
  const late = todo({
    dueDate: Timestamp.fromMillis(NOW - (STALE_AFTER_MINUTES + 30) * MINUTE),
  });
  assert.deepEqual(reminderDecision(late, NOW), { action: "mark" });
});

test("kurz verpasste Erinnerung geht noch raus", () => {
  // Erinnerungszeitpunkt = dueDate minus 10 Minuten, liegt also eine Stunde
  // zurück und damit klar innerhalb des Kulanzfensters.
  const late = todo({ dueDate: Timestamp.fromMillis(NOW - 50 * MINUTE) });
  assert.deepEqual(reminderDecision(late, NOW), {
    action: "notify",
    recipientId: "uid-ersteller",
  });
});

test("ohne Empfänger wird nur abgehakt", () => {
  const orphan = todo({ assignedTo: null, createdBy: "" });
  assert.deepEqual(reminderDecision(orphan, NOW), { action: "mark" });
});
