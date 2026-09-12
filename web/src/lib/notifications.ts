import { Timestamp, addDoc, collection } from "firebase/firestore";
import { db } from "./firebase";
import type { NotificationType } from "./types";

/**
 * Legt ein Dokument in `notifications` an. Den Rest erledigt die Cloud
 * Function `onNotificationCreated`: sie verschickt den Push an alle Geräte des
 * Empfängers und respektiert dabei stummgeschaltete Listen.
 *
 * Wann benachrichtigt wird, ist bewusst exakt wie in der Android-App
 * (TodosViewModel):
 *  - ZUGEWIESEN: sobald eine Aufgabe jemandem zugewiesen wird
 *  - ERLEDIGT:   beim Abhaken, an die zuständige Person
 *  - KOMMENTAR:  an die zuständige Person
 * Niemals an sich selbst, und nie ohne Empfänger.
 */
export async function notify(params: {
  recipientId: string | null | undefined;
  actorId: string;
  actorName: string;
  type: NotificationType;
  todoTitle: string;
  listId: string;
  todoId?: string;
}): Promise<void> {
  const { recipientId, actorId } = params;
  if (!recipientId || recipientId === actorId) return;

  await addDoc(collection(db(), "notifications"), {
    recipientId,
    actorId,
    actorName: params.actorName,
    type: params.type,
    todoTitle: params.todoTitle,
    listId: params.listId,
    todoId: params.todoId ?? "",
    isRead: false,
    createdAt: Timestamp.now(),
  });
}
