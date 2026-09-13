import {
  Timestamp,
  addDoc,
  arrayRemove,
  arrayUnion,
  collection,
  deleteDoc,
  doc,
  onSnapshot,
  orderBy,
  query,
  updateDoc,
  where,
  writeBatch,
  type Unsubscribe,
} from "firebase/firestore";
import { db } from "./firebase";
import { notify } from "./notifications";
import type {
  Comment,
  ListMode,
  Priority,
  Recurrence,
  Subtask,
  TodoItem,
  TodoList,
} from "./types";

/**
 * Live-Abfragen auf Firestore, als Runes gekapselt.
 *
 * Jede Klasse hier hält genau einen onSnapshot-Listener. Wer sie benutzt, muss
 * am Ende `stop()` aufrufen – in Komponenten am einfachsten über $effect, das
 * seine Aufräumfunktion beim Verlassen selbst ausführt.
 */

/**
 * Ein Todo-Dokument in eine vollständige TodoItem-Form bringen.
 *
 * Aufgaben, die ältere App-Versionen angelegt haben, kennen `recurrence`,
 * `rotateAmong`, `subtasks` und `comments` nicht. Ohne diese Normalisierung
 * wäre `recurrence` dann `undefined` – und `undefined !== null` hätte die
 * Wiederholung in der Oberfläche fälschlich als aktiv angezeigt.
 */
function normalizeTodo(id: string, data: Record<string, unknown>): TodoItem {
  return {
    ...(data as Omit<TodoItem, "id">),
    id,
    subtasks: Array.isArray(data.subtasks) ? (data.subtasks as Subtask[]) : [],
    comments: Array.isArray(data.comments) ? (data.comments as Comment[]) : [],
    recurrence: (data.recurrence as TodoItem["recurrence"]) ?? null,
    rotateAmong: Array.isArray(data.rotateAmong) ? (data.rotateAmong as string[]) : [],
    quantity: typeof data.quantity === "string" ? data.quantity : "",
  };
}

/** Gegenstück für Listen: `mode` fehlt in allem, was vor dem Einkaufsmodus entstand. */
function normalizeList(id: string, data: Record<string, unknown>): TodoList {
  return {
    ...(data as Omit<TodoList, "id">),
    id,
    adminIds: Array.isArray(data.adminIds) ? (data.adminIds as string[]) : [],
    mutedBy: Array.isArray(data.mutedBy) ? (data.mutedBy as string[]) : [],
    mode:
      data.mode === "EINKAUFEN" || data.mode === "CHECKLISTE"
        ? (data.mode as ListMode)
        : "AUFGABEN",
  };
}

/** Alle Listen, in denen die angemeldete Person Mitglied ist. */
export class ListsQuery {
  items = $state<TodoList[]>([]);
  loading = $state(true);
  error = $state<string | null>(null);

  #unsubscribe: Unsubscribe;

  constructor(uid: string) {
    // Gleiche Abfrage wie die App: whereArrayContains auf memberIds.
    // Sortiert wird in Code statt per orderBy, damit kein zusätzlicher
    // zusammengesetzter Index nötig wird.
    const q = query(collection(db(), "lists"), where("memberIds", "array-contains", uid));

    this.#unsubscribe = onSnapshot(
      q,
      (snapshot) => {
        this.items = snapshot.docs
          .map((d) => normalizeList(d.id, d.data()))
          .sort((a, b) => (b.createdAt?.toMillis() ?? 0) - (a.createdAt?.toMillis() ?? 0));
        this.loading = false;
      },
      (e) => {
        this.error = "Listen konnten nicht geladen werden.";
        this.loading = false;
        console.error("Listen-Listener", e);
      }
    );
  }

  stop() {
    this.#unsubscribe();
  }
}

/** Die Aufgaben einer Liste. */
export class TodosQuery {
  items = $state<TodoItem[]>([]);
  loading = $state(true);
  error = $state<string | null>(null);

  #unsubscribe: Unsubscribe;

  constructor(listId: string) {
    const q = query(collection(db(), "lists", listId, "todos"), orderBy("position", "asc"));

    this.#unsubscribe = onSnapshot(
      q,
      (snapshot) => {
        this.items = snapshot.docs.map((d) => normalizeTodo(d.id, d.data()));
        this.loading = false;
      },
      (e) => {
        this.error = "Aufgaben konnten nicht geladen werden.";
        this.loading = false;
        console.error("Todo-Listener", e);
      }
    );
  }

  stop() {
    this.#unsubscribe();
  }
}

// ─── Schreiboperationen ─────────────────────────────────────────────────────
// Bewusst ohne await auf das Ergebnis in der UI: Firestore übernimmt jede
// Änderung sofort in den lokalen Cache, der Listener feuert also direkt. Das
// Promise wird erst mit der Server-Bestätigung fertig und hinge offline
// beliebig lange.

export async function createList(
  uid: string,
  name: string,
  displayName: string,
  mode: ListMode = "AUFGABEN"
): Promise<string> {
  const ref = await addDoc(collection(db(), "lists"), {
    name,
    memberIds: [uid],
    memberNames: { [uid]: displayName },
    adminIds: [],
    createdBy: uid,
    createdAt: Timestamp.now(),
    color: "#6750A4",
    icon: "",
    mutedBy: [],
    mode,
  });
  return ref.id;
}

export async function createTodo(
  listId: string,
  uid: string,
  title: string,
  position: number
): Promise<void> {
  await addDoc(collection(db(), "lists", listId, "todos"), {
    title,
    description: "",
    isDone: false,
    priority: "MITTEL",
    dueDate: null,
    assignedTo: null,
    reminderMinutes: null,
    reminderSent: false,
    createdBy: uid,
    createdAt: Timestamp.now(),
    doneBy: null,
    doneAt: null,
    position,
    subtasks: [],
    comments: [],
    recurrence: null,
    rotateAmong: [],
    quantity: "",
  });
}

export async function setTodoDone(
  listId: string,
  todo: TodoItem,
  uid: string,
  actorName: string,
  done: boolean
): Promise<void> {
  await updateDoc(doc(db(), "lists", listId, "todos", todo.id), {
    isDone: done,
    doneBy: done ? uid : null,
    doneAt: done ? Timestamp.now() : null,
  });

  // Nur beim Abhaken, und nur wenn jemand zuständig ist – wie in der App.
  if (done) {
    await notify({
      recipientId: todo.assignedTo,
      actorId: uid,
      actorName,
      type: "ERLEDIGT",
      todoTitle: todo.title,
      listId,
      todoId: todo.id,
    });
  }
}

/** Felder, die sich über die Detailansicht ändern lassen. */
export interface TodoEdit {
  title: string;
  description: string;
  priority: Priority;
  dueDate: Timestamp | null;
  assignedTo: string | null;
  reminderMinutes: number | null;
  recurrence: Recurrence | null;
  rotateAmong: string[];
  quantity: string;
}

export async function updateTodo(
  listId: string,
  todo: TodoItem,
  edit: TodoEdit,
  uid: string,
  actorName: string
): Promise<void> {
  const changedDueDate = (todo.dueDate?.toMillis() ?? null) !== (edit.dueDate?.toMillis() ?? null);
  const changedReminder = todo.reminderMinutes !== edit.reminderMinutes;

  await updateDoc(doc(db(), "lists", listId, "todos", todo.id), {
    ...edit,
    // Fälligkeit oder Vorlaufzeit geändert: die Erinnerung muss erneut raus.
    // Ohne das bliebe reminderSent auf true und der Job überspringt die Aufgabe.
    ...(changedDueDate || changedReminder ? { reminderSent: false } : {}),
  });

  // Zuweisung an eine andere Person ist neu und wird gemeldet.
  if (edit.assignedTo && edit.assignedTo !== todo.assignedTo) {
    await notify({
      recipientId: edit.assignedTo,
      actorId: uid,
      actorName,
      type: "ZUGEWIESEN",
      todoTitle: edit.title,
      listId,
      todoId: todo.id,
    });
  }
}

export async function deleteTodo(listId: string, todoId: string): Promise<void> {
  await deleteDoc(doc(db(), "lists", listId, "todos", todoId));
}

// ─── Unteraufgaben und Kommentare ───────────────────────────────────────────
// Beide liegen als Array IM Todo-Dokument. Firestore kann Array-Elemente nicht
// einzeln ändern, deshalb wird jeweils das ganze Array zurückgeschrieben.
// arrayUnion/arrayRemove helfen nur beim Anhängen und exakten Entfernen.

export async function addSubtask(listId: string, todo: TodoItem, title: string): Promise<void> {
  const subtask: Subtask = { id: crypto.randomUUID(), title, isDone: false };
  await updateDoc(doc(db(), "lists", listId, "todos", todo.id), {
    subtasks: arrayUnion(subtask),
  });
}

export async function setSubtaskDone(
  listId: string,
  todo: TodoItem,
  subtaskId: string,
  done: boolean
): Promise<void> {
  await updateDoc(doc(db(), "lists", listId, "todos", todo.id), {
    subtasks: todo.subtasks.map((s) => (s.id === subtaskId ? { ...s, isDone: done } : s)),
  });
}

export async function removeSubtask(
  listId: string,
  todo: TodoItem,
  subtask: Subtask
): Promise<void> {
  await updateDoc(doc(db(), "lists", listId, "todos", todo.id), {
    subtasks: arrayRemove(subtask),
  });
}

export async function addComment(
  listId: string,
  todo: TodoItem,
  text: string,
  uid: string,
  actorName: string
): Promise<void> {
  const comment: Comment = {
    id: crypto.randomUUID(),
    authorId: uid,
    text,
    createdAt: Timestamp.now(),
  };
  await updateDoc(doc(db(), "lists", listId, "todos", todo.id), {
    comments: arrayUnion(comment),
  });

  await notify({
    recipientId: todo.assignedTo,
    actorId: uid,
    actorName,
    type: "KOMMENTAR",
    todoTitle: todo.title,
    listId,
    todoId: todo.id,
  });
}

/**
 * Reihenfolge nach dem Verschieben speichern.
 *
 * Die Positionen werden komplett neu durchnummeriert (0, 1, 2 …) statt einen
 * Zwischenwert zu berechnen. Bei den Listengrößen dieser App ist ein Batch
 * billiger als eine Bruchzahl-Strategie, und die Werte bleiben ganzzahlig –
 * was wichtig ist, weil die App `position` als Long liest.
 *
 * Nur geänderte Dokumente werden geschrieben.
 */
export async function reorderTodos(listId: string, ordered: TodoItem[]): Promise<void> {
  const batch = writeBatch(db());
  let changed = 0;

  ordered.forEach((todo, index) => {
    if (todo.position === index) return;
    batch.update(doc(db(), "lists", listId, "todos", todo.id), { position: index });
    changed++;
  });

  if (changed > 0) await batch.commit();
}

/** Menge im Einkaufsmodus setzen, ohne den Rest der Aufgabe anzufassen. */
export async function setQuantity(listId: string, todoId: string, quantity: string): Promise<void> {
  await updateDoc(doc(db(), "lists", listId, "todos", todoId), { quantity: quantity.trim() });
}

/**
 * Alle erledigten Aufgaben einer Liste löschen.
 *
 * Vor allem für den Einkaufsmodus: nach dem Einkauf soll die Liste leer sein.
 * In Blöcken, weil ein Batch höchstens 500 Schreibvorgänge fasst.
 */
export async function deleteDoneTodos(listId: string, todos: TodoItem[]): Promise<number> {
  const done = todos.filter((t) => t.isDone);
  for (let i = 0; i < done.length; i += 400) {
    const batch = writeBatch(db());
    for (const todo of done.slice(i, i + 400)) {
      batch.delete(doc(db(), "lists", listId, "todos", todo.id));
    }
    await batch.commit();
  }
  return done.length;
}

/** Modus einer bestehenden Liste umstellen. */
export async function setListMode(listId: string, mode: ListMode): Promise<void> {
  await updateDoc(doc(db(), "lists", listId), { mode });
}

/**
 * Alle Aufgaben einer Liste wieder auf offen setzen.
 *
 * Für Checklisten: eine Packliste ist nach der Reise abgehakt und soll vor der
 * nächsten wieder vollständig dastehen. Unteraufgaben werden mit
 * zurückgesetzt, sonst bliebe die Hälfte der Haken stehen.
 */
export async function resetAllTodos(listId: string, todos: TodoItem[]): Promise<number> {
  const affected = todos.filter((t) => t.isDone || t.subtasks.some((s) => s.isDone));

  for (let i = 0; i < affected.length; i += 400) {
    const batch = writeBatch(db());
    for (const todo of affected.slice(i, i + 400)) {
      batch.update(doc(db(), "lists", listId, "todos", todo.id), {
        isDone: false,
        doneBy: null,
        doneAt: null,
        subtasks: todo.subtasks.map((s) => ({ ...s, isDone: false })),
      });
    }
    await batch.commit();
  }
  return affected.length;
}
