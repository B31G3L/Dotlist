import { arrayRemove, arrayUnion, deleteField, doc, runTransaction, updateDoc } from "firebase/firestore";
import { db } from "./firebase";
import type { TodoList } from "./types";

/**
 * Mitgliederverwaltung, gleiche Operationen wie im TodoRepository der App.
 * Wer das darf, entscheidet die Oberfläche über canManageMembers; die Rules
 * erlauben jedem Mitglied Schreibzugriff auf das Listendokument.
 */

export async function promoteToAdmin(listId: string, memberId: string): Promise<void> {
  await updateDoc(doc(db(), "lists", listId), { adminIds: arrayUnion(memberId) });
}

export async function demoteAdmin(listId: string, memberId: string): Promise<void> {
  await updateDoc(doc(db(), "lists", listId), { adminIds: arrayRemove(memberId) });
}

/**
 * Mitglied entfernen. Der Name verschwindet mit – die App macht es genauso,
 * und ein stehengebliebener Eintrag in memberNames würde die Person in der
 * Zuständigen-Auswahl weiter anbieten.
 */
export async function removeMember(listId: string, memberId: string): Promise<void> {
  await updateDoc(doc(db(), "lists", listId), {
    memberIds: arrayRemove(memberId),
    adminIds: arrayRemove(memberId),
    mutedBy: arrayRemove(memberId),
    [`memberNames.${memberId}`]: deleteField(),
  });
}

/**
 * Besitz übertragen.
 *
 * Als Transaktion, damit der bisherige Besitzer nicht seine Admin-Rechte
 * verliert, wenn parallel jemand die Adminliste ändert. Ohne das könnte man
 * sich selbst aus einer Liste aussperren, die man gerade noch verwaltet hat.
 */
export async function transferOwnership(
  list: TodoList,
  currentUid: string,
  newOwnerId: string
): Promise<void> {
  const ref = doc(db(), "lists", list.id);
  await runTransaction(db(), async (transaction) => {
    const snapshot = await transaction.get(ref);
    const data = snapshot.data();
    if (!data) return;

    const memberIds = (data.memberIds ?? []) as string[];
    if (!memberIds.includes(newOwnerId)) return;

    const adminIds = (data.adminIds ?? []) as string[];
    // Der alte Besitzer bleibt als Admin drin, der neue braucht den Eintrag nicht mehr.
    const next = [...new Set([...adminIds, currentUid])].filter((id) => id !== newOwnerId);

    transaction.update(ref, { createdBy: newOwnerId, adminIds: next });
  });
}
