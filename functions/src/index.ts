import { initializeApp } from "firebase-admin/app";
import { getFirestore, Timestamp } from "firebase-admin/firestore";
import { logger } from "firebase-functions";
import { setGlobalOptions } from "firebase-functions/v2";
import { onDocumentCreated, onDocumentUpdated } from "firebase-functions/v2/firestore";
import { onSchedule } from "firebase-functions/v2/scheduler";

import { notificationContent } from "./messages";
import { MAX_LEAD_MINUTES, reminderDecision } from "./reminders";
import { nextAssignee, nextDueTimestamp, parseRecurrence } from "./recurrence";
import { pushToUser } from "./push";

initializeApp();

/**
 * Region und Ressourcen für alle Funktionen.
 *
 * Achtung beim ersten Deploy: die Region einer bestehenden Funktion lässt sich
 * nicht ändern. Läuft die bisherige Function in us-central1, muss sie vorher
 * gelöscht werden (`firebase functions:delete <name>`), sonst legt der Deploy
 * eine zweite Instanz an und jede Push-Nachricht geht doppelt raus.
 */
setGlobalOptions({
  region: "europe-west3",
  maxInstances: 10,
  memory: "256MiB",
});

// ─── Push bei neuer Benachrichtigung ────────────────────────────────────────

/**
 * Schickt für jedes neue Dokument in `notifications` eine Push-Nachricht an
 * alle Geräte des Empfängers.
 *
 * Die Clients (und die Erinnerungs-Funktion unten) legen nur das
 * Benachrichtigungsdokument an – der Versand hängt allein an diesem Trigger.
 * Damit ist die Logik an genau einer Stelle, egal ob Android, Web oder Desktop
 * die Benachrichtigung ausgelöst hat.
 */
export const onNotificationCreated = onDocumentCreated(
  "notifications/{notificationId}",
  async (event) => {
    const data = event.data?.data();
    if (!data) return;

    const recipientId = data.recipientId as string | undefined;
    const actorId = data.actorId as string | undefined;
    if (!recipientId) return;
    // Sollte schon clientseitig ausgeschlossen sein – doppelt hält besser,
    // niemand will eine Push-Nachricht über die eigene Aktion.
    if (recipientId === actorId) return;

    const listId = (data.listId as string) ?? "";
    if (listId && (await isListMuted(listId, recipientId))) {
      logger.debug("Liste stummgeschaltet, kein Push", { listId, recipientId });
      return;
    }

    const sent = await pushToUser(recipientId, {
      content: notificationContent(
        (data.type as string) ?? "",
        (data.actorName as string) ?? "",
        (data.todoTitle as string) ?? ""
      ),
      data: {
        listId,
        todoId: (data.todoId as string) ?? "",
        type: (data.type as string) ?? "",
      },
    });

    logger.info("Benachrichtigung zugestellt", {
      recipientId,
      type: data.type,
      devices: sent,
    });
  }
);

/** Hat der Empfänger diese Liste stummgeschaltet? */
async function isListMuted(listId: string, recipientId: string): Promise<boolean> {
  const snapshot = await getFirestore().collection("lists").doc(listId).get();
  const mutedBy = snapshot.get("mutedBy");
  return Array.isArray(mutedBy) && mutedBy.includes(recipientId);
}

// ─── Erinnerungen an fällige Aufgaben ───────────────────────────────────────

/** Wie viele Aufgaben ein Durchlauf höchstens verarbeitet. */
const BATCH_SIZE = 200;

/**
 * Sucht alle fälligen Erinnerungen und legt dafür Benachrichtigungen an.
 *
 * Der eigentliche Push passiert nicht hier, sondern über
 * [onNotificationCreated] – die Erinnerung landet dadurch automatisch auch im
 * Benachrichtigungs-Screen der App.
 *
 * `reminderSent` wird im selben Batch gesetzt, in dem die Benachrichtigung
 * entsteht. Läuft der Job doppelt an oder bricht er ab, kann dieselbe
 * Erinnerung dadurch nicht zweimal rausgehen.
 *
 * Alle fünf Minuten: feiner lohnt sich nicht, weil die kleinste Vorlaufzeit in
 * der App 10 Minuten beträgt.
 */
export const sendDueReminders = onSchedule(
  {
    schedule: "every 5 minutes",
    timeZone: "Europe/Berlin",
    retryCount: 0,
  },
  async () => {
    const db = getFirestore();
    const now = Date.now();
    const horizon = Timestamp.fromMillis(now + MAX_LEAD_MINUTES * 60_000);

    // Über alle todos-Subcollections hinweg. Die Bedingung "dueDate minus
    // Vorlaufzeit liegt in der Vergangenheit" lässt sich in Firestore nicht
    // ausdrücken, deshalb grob über dueDate vorfiltern und den Rest in Code
    // entscheiden (siehe reminderDecision).
    const snapshot = await db
      .collectionGroup("todos")
      .where("reminderSent", "==", false)
      .where("dueDate", "<=", horizon)
      .orderBy("dueDate", "asc")
      .limit(BATCH_SIZE)
      .get();

    if (snapshot.empty) return;

    const batch = db.batch();
    let notified = 0;
    let skipped = 0;

    for (const doc of snapshot.docs) {
      const decision = reminderDecision(
        {
          dueDate: doc.get("dueDate"),
          reminderMinutes: doc.get("reminderMinutes"),
          isDone: doc.get("isDone"),
          assignedTo: doc.get("assignedTo"),
          createdBy: doc.get("createdBy"),
        },
        now
      );

      if (decision.action === "leave") continue;

      batch.update(doc.ref, { reminderSent: true });

      if (decision.action === "mark") {
        skipped++;
        continue;
      }

      batch.set(db.collection("notifications").doc(), {
        recipientId: decision.recipientId,
        actorId: "",
        actorName: "",
        type: "ERINNERUNG",
        todoTitle: doc.get("title") ?? "",
        // parent = todos-Collection, parent.parent = das Listendokument
        listId: doc.ref.parent.parent?.id ?? "",
        todoId: doc.id,
        isRead: false,
        createdAt: Timestamp.now(),
      });
      notified++;
    }

    await batch.commit();
    logger.info("Erinnerungen verarbeitet", {
      gefunden: snapshot.size,
      benachrichtigt: notified,
      uebersprungen: skipped,
    });
  }
);

// ─── Wiederkehrende Aufgaben ────────────────────────────────────────────────

/**
 * Legt beim Abhaken einer wiederkehrenden Aufgabe die nächste Instanz an.
 *
 * Bewusst hier und nicht in den Clients: Monatsenden, Zeitumstellung und zwei
 * Geräte, die gleichzeitig abhaken, wären sonst dreimal zu lösen. So genügt es,
 * dass App und Web das Muster setzen können – die nächste Aufgabe erscheint
 * überall, auch in App-Versionen, die Wiederholungen noch gar nicht kennen.
 *
 * Ausgelöst wird nur beim Übergang offen -> erledigt. Das Anlegen der neuen
 * Aufgabe ist ein create und löst diesen Trigger deshalb nicht erneut aus.
 */
export const onTodoCompleted = onDocumentUpdated(
  { document: "lists/{listId}/todos/{todoId}" },
  async (event) => {
    const before = event.data?.before.data();
    const after = event.data?.after.data();
    if (!before || !after) return;

    // Nur der Übergang zählt: ein erneutes Speichern einer erledigten Aufgabe
    // darf keine zweite Instanz erzeugen.
    if (before.isDone === true || after.isDone !== true) return;
    // Schutz gegen einen zweiten Lauf derselben Änderung (Retry der Function).
    if (after.recurrenceSpawned === true) return;

    const recurrence = parseRecurrence(after.recurrence);
    if (!recurrence) return;

    const completedAt = after.doneAt instanceof Timestamp ? after.doneAt : Timestamp.now();
    const dueDate = after.dueDate instanceof Timestamp ? after.dueDate : null;

    const rotateAmong: string[] = Array.isArray(after.rotateAmong)
      ? after.rotateAmong.filter((id: unknown): id is string => typeof id === "string")
      : [];
    const assignedTo = typeof after.assignedTo === "string" ? after.assignedTo : null;

    const db = getFirestore();
    const todosRef = db.collection("lists").doc(event.params.listId).collection("todos");

    const batch = db.batch();
    batch.update(event.data!.after.ref, { recurrenceSpawned: true });
    batch.set(todosRef.doc(), {
      title: after.title ?? "",
      description: after.description ?? "",
      isDone: false,
      priority: after.priority ?? "MITTEL",
      dueDate: nextDueTimestamp(dueDate, completedAt, recurrence),
      assignedTo: nextAssignee(rotateAmong, assignedTo),
      reminderMinutes: typeof after.reminderMinutes === "number" ? after.reminderMinutes : null,
      reminderSent: false,
      createdBy: after.createdBy ?? "",
      createdAt: Timestamp.now(),
      doneBy: null,
      doneAt: null,
      // Gleiche Stelle in der Liste wie die abgehakte Aufgabe.
      position: typeof after.position === "number" ? after.position : 0,
      // Unteraufgaben wandern mit, aber wieder offen. Kommentare nicht: die
      // gehören zum vergangenen Durchgang.
      subtasks: Array.isArray(after.subtasks)
        ? after.subtasks.map((s: Record<string, unknown>) => ({ ...s, isDone: false }))
        : [],
      comments: [],
      recurrence: after.recurrence,
      rotateAmong,
    });

    await batch.commit();
    logger.info("Wiederkehrende Aufgabe fortgeschrieben", {
      listId: event.params.listId,
      unit: recurrence.unit,
      interval: recurrence.interval,
    });
  }
);
