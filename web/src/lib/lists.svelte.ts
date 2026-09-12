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
  type Unsubscribe,
} from "firebase/firestore";
import { db } from "./firebase";
import { notify } from "./notifications";
import type { Comment, Priority, Subtask, TodoItem, TodoList } from "./types";

/**
 * Live-Abfragen auf Firestore, als Runes gekapselt.
 *
 * Jede Klasse hier hält genau einen onSnapshot-Listener. Wer sie benutzt, muss
 * am Ende `stop()` aufrufen – in Komponenten am einfachsten über $effect, das
 * seine Aufräumfunktion beim Verlassen selbst ausführt.
 */

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
          .map((d) => ({ id: d.id, ...d.data() }) as TodoList)
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
        this.items = snapshot.docs.map((d) => ({ id: d.id, ...d.data() }) as TodoItem);
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

export async function createList(uid: string, name: string, displayName: string): Promise<string> {
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
