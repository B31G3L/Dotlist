import {
  Timestamp,
  addDoc,
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
import type { TodoItem, TodoList } from "./types";

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

export async function createTodo(listId: string, uid: string, title: string, position: number) {
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

export async function setTodoDone(listId: string, todo: TodoItem, uid: string, done: boolean) {
  await updateDoc(doc(db(), "lists", listId, "todos", todo.id), {
    isDone: done,
    doneBy: done ? uid : null,
    doneAt: done ? Timestamp.now() : null,
  });
}

export async function renameTodo(listId: string, todoId: string, title: string) {
  await updateDoc(doc(db(), "lists", listId, "todos", todoId), { title });
}

export async function deleteTodo(listId: string, todoId: string) {
  await deleteDoc(doc(db(), "lists", listId, "todos", todoId));
}
