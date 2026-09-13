import { collection, limit, onSnapshot, orderBy, query, updateDoc, doc, where, writeBatch, type Unsubscribe } from "firebase/firestore";
import { db } from "./firebase";
import type { AppNotification } from "./types";

/**
 * Benachrichtigungen des angemeldeten Nutzers, live.
 *
 * Dieselbe Collection, die auch die App liest, und dieselbe Abfrage –
 * recipientId plus Sortierung nach createdAt, wofür der zusammengesetzte Index
 * schon existiert.
 */
export class NotificationsQuery {
  items = $state<AppNotification[]>([]);
  loading = $state(true);
  error = $state<string | null>(null);

  #unsubscribe: Unsubscribe;

  constructor(uid: string, max = 50) {
    const q = query(
      collection(db(), "notifications"),
      where("recipientId", "==", uid),
      orderBy("createdAt", "desc"),
      limit(max)
    );

    this.#unsubscribe = onSnapshot(
      q,
      (snapshot) => {
        this.items = snapshot.docs.map((d) => ({ id: d.id, ...d.data() }) as AppNotification);
        this.loading = false;
      },
      (e) => {
        this.error = "Benachrichtigungen konnten nicht geladen werden.";
        this.loading = false;
        console.error("Benachrichtigungs-Listener", e);
      }
    );
  }

  get unread(): number {
    return this.items.filter((n) => !n.isRead).length;
  }

  stop() {
    this.#unsubscribe();
  }
}

export async function markRead(id: string): Promise<void> {
  await updateDoc(doc(db(), "notifications", id), { isRead: true });
}

/** Alles als gelesen markieren. Die Rules erlauben nur das isRead-Feld. */
export async function markAllRead(items: AppNotification[]): Promise<void> {
  const unread = items.filter((n) => !n.isRead);
  for (let i = 0; i < unread.length; i += 400) {
    const batch = writeBatch(db());
    for (const item of unread.slice(i, i + 400)) {
      batch.update(doc(db(), "notifications", item.id), { isRead: true });
    }
    await batch.commit();
  }
}

/**
 * Text einer Benachrichtigung.
 *
 * Wortgleich zu den Push-Texten der Cloud Function (functions/src/messages.ts)
 * und den In-App-Texten der Android-App. Wird dort etwas geändert, gehört es
 * auch hierher.
 */
export function notificationText(item: AppNotification): string {
  switch (item.type) {
    case "ZUGEWIESEN":
      return `${item.actorName} hat dir „${item.todoTitle}" zugewiesen`;
    case "ERLEDIGT":
      return `${item.actorName} hat „${item.todoTitle}" erledigt`;
    case "KOMMENTAR":
      return `${item.actorName} hat zu „${item.todoTitle}" kommentiert`;
    case "EINLADUNG":
      return `${item.actorName} lädt dich zur Liste „${item.todoTitle}" ein`;
    case "ERINNERUNG":
      return `„${item.todoTitle}" wird bald fällig`;
    default:
      return `${item.actorName} hat „${item.todoTitle}" bearbeitet`;
  }
}
