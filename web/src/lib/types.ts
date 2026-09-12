import type { Timestamp } from "firebase/firestore";

/**
 * Die Feldnamen müssen exakt denen der Android-App entsprechen (Models.kt),
 * sonst sehen die Clients unterschiedliche Daten. Stolperstellen:
 *
 *  - `isDone` heißt in Firestore wirklich so. Kotlin würde daraus beim
 *    Serialisieren "done" machen, deshalb steht dort ein @PropertyName.
 *  - `id` und `isShared` stehen NICHT in Firestore (@Exclude). Die ID kommt
 *    aus dem Dokument, `isShared` gibt es nur in der App.
 *  - `subtasks` und `comments` sind Arrays IM Todo-Dokument, keine
 *    Subcollections. Wer eines ändert, schreibt das ganze Array zurück.
 *  - Rechte hängen an `adminIds`, nicht an einer Rollen-Map.
 *  - `priority` ist ein String, kein verschachteltes Objekt.
 */

export type Priority = "NIEDRIG" | "MITTEL" | "HOCH";

/**
 * Art der Liste.
 *
 * AUFGABEN ist der Standard und der Rückfall für alles, was das Feld noch
 * nicht kennt. EINKAUFEN blendet Priorität, Zuständigkeit, Fälligkeit und
 * Erinnerung aus und zeigt stattdessen Mengen und eine Gliederung nach
 * Abteilungen.
 */
export type ListMode = "AUFGABEN" | "EINKAUFEN";

export type RecurrenceUnit = "TAG" | "WOCHE" | "MONAT" | "JAHR";

/**
 * Woran der nächste Termin hängt:
 *  - FAELLIG:  am bisherigen Fälligkeitsdatum (Miete, Müllabfuhr)
 *  - ERLEDIGT: am Zeitpunkt des Abhakens (Blumen gießen)
 */
export type RecurrenceAnchor = "FAELLIG" | "ERLEDIGT";

export interface Recurrence {
  unit: RecurrenceUnit;
  interval: number;
  anchor: RecurrenceAnchor;
}

/** Eine Liste. Dokument in der Collection `lists`. */
export interface TodoList {
  /** Dokument-ID, nicht Teil der gespeicherten Felder. */
  id: string;
  name: string;
  memberIds: string[];
  /** UID → Anzeigename. Wer die Liste verlässt, bleibt hier stehen. */
  memberNames: Record<string, string>;
  /** UIDs mit Admin-Rechten. Der Ersteller zählt nicht extra dazu. */
  adminIds: string[];
  createdBy: string;
  createdAt: Timestamp | null;
  /** Hex-String, z. B. "#6750A4". */
  color: string;
  /** Icon-Name; leer bedeutet in der App die alte Positions-Rotation. */
  icon: string;
  /** UIDs, die für diese Liste keine Benachrichtigungen wollen. */
  mutedBy: string[];
  mode: ListMode;
}

/** Eine Unteraufgabe, eingebettet im Todo-Dokument. */
export interface Subtask {
  id: string;
  title: string;
  isDone: boolean;
}

/** Ein Kommentar, eingebettet im Todo-Dokument. */
export interface Comment {
  id: string;
  authorId: string;
  text: string;
  createdAt: Timestamp | null;
}

/** Eine Aufgabe. Dokument in `lists/{listId}/todos`. */
export interface TodoItem {
  id: string;
  title: string;
  description: string;
  isDone: boolean;
  priority: Priority;
  dueDate: Timestamp | null;
  assignedTo: string | null;
  /** Vorlaufzeit der Erinnerung in Minuten; null = keine Erinnerung. */
  reminderMinutes: number | null;
  reminderSent: boolean;
  createdBy: string;
  createdAt: Timestamp | null;
  doneBy: string | null;
  doneAt: Timestamp | null;
  /** Sortierreihenfolge, aufsteigend. */
  position: number;
  /**
   * Freitext für die Menge im Einkaufsmodus, z. B. „2 kg" oder „1 Packung".
   * Absichtlich ein Feld statt Zahl plus Einheit – so tippt man es auch.
   */
  quantity: string;
  /** Wiederholung; null = einmalige Aufgabe. */
  recurrence: Recurrence | null;
  /**
   * UIDs, unter denen die Zuständigkeit reihum wechselt. Leer = bleibt, wie
   * sie ist. Die Cloud Function dreht beim Abhaken weiter.
   */
  rotateAmong: string[];
  /** Von der Cloud Function gesetzt, sobald die Folgeaufgabe angelegt wurde. */
  recurrenceSpawned?: boolean;
  subtasks: Subtask[];
  comments: Comment[];
}

/** Rolle eines Mitglieds, abgeleitet aus createdBy und adminIds. */
export type MemberRole = "BESITZER" | "ADMIN" | "MITGLIED";

export function roleOf(list: TodoList, uid: string): MemberRole {
  if (list.createdBy === uid) return "BESITZER";
  if (list.adminIds.includes(uid)) return "ADMIN";
  return "MITGLIED";
}

/** Darf diese Person Mitglieder verwalten und die Liste löschen? */
export function canManageMembers(list: TodoList, uid: string): boolean {
  return list.createdBy === uid || list.adminIds.includes(uid);
}

/** Eine Benachrichtigung. Dokument in der Collection `notifications`. */
export type NotificationType = "ZUGEWIESEN" | "ERLEDIGT" | "KOMMENTAR" | "EINLADUNG" | "ERINNERUNG";

export interface AppNotification {
  id: string;
  recipientId: string;
  actorId: string;
  actorName: string;
  type: NotificationType;
  /** Bei EINLADUNG steht hier der Listenname, sonst der Titel der Aufgabe. */
  todoTitle: string;
  listId: string;
  todoId: string;
  isRead: boolean;
  createdAt: Timestamp | null;
}
