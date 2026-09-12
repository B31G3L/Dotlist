import {
  Timestamp,
  arrayRemove,
  arrayUnion,
  collection,
  deleteField,
  doc,
  getDoc,
  getDocs,
  query,
  setDoc,
  updateDoc,
  where,
  writeBatch,
} from "firebase/firestore";
import { db } from "./firebase";
import type { TodoList } from "./types";

/**
 * Einladungen liegen flach unter `invites/{code}`, der Code IST die
 * Dokument-ID. Die Anzeigedaten der Liste stehen redundant im Dokument, damit
 * der Einladungs-Screen ohne Lesezugriff auf die Liste auskommt – wer einen
 * Code hat, sieht also Name und Farbe, aber keine Inhalte.
 */
export interface Invite {
  code: string;
  listId: string;
  listName: string;
  listColor: string;
  listIcon: string;
  memberCount: number;
  createdBy: string;
  createdAt: Timestamp;
  expiresAt: Timestamp;
  revoked: boolean;
}

/** Gültigkeitsdauer neuer Codes, wie DEFAULT_INVITE_DAYS in der App. */
const VALID_DAYS = 7;

const CODE_LENGTH = 8;

/** Ohne I/1 und O/0, damit ein Code auch mündlich oder abgetippt ankommt. */
const ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";

function generateCode(): string {
  const bytes = crypto.getRandomValues(new Uint8Array(CODE_LENGTH));
  return Array.from(bytes, (b) => ALPHABET[b % ALPHABET.length]).join("");
}

/** Tippfehler abfangen: Leerzeichen und Bindestriche raus, alles groß. */
export function normalizeCode(raw: string): string {
  return raw.replace(/[^a-zA-Z0-9]/g, "").toUpperCase();
}

export function isUsable(invite: Invite): boolean {
  return !invite.revoked && invite.expiresAt.toMillis() > Date.now();
}

/** Aktuell gültige Einladung einer Liste, oder null. */
export async function activeInvite(listId: string): Promise<Invite | null> {
  const snapshot = await getDocs(
    query(collection(db(), "invites"), where("listId", "==", listId), where("revoked", "==", false))
  );
  const usable = snapshot.docs
    .map((d) => ({ code: d.id, ...d.data() }) as Invite)
    .filter(isUsable)
    .sort((a, b) => b.createdAt.toMillis() - a.createdAt.toMillis());
  return usable[0] ?? null;
}

/** Alle offenen Einladungen einer Liste entwerten. */
export async function revokeInvitesFor(listId: string): Promise<void> {
  const snapshot = await getDocs(
    query(collection(db(), "invites"), where("listId", "==", listId), where("revoked", "==", false))
  );
  if (snapshot.empty) return;

  const batch = writeBatch(db());
  for (const d of snapshot.docs) batch.update(d.ref, { revoked: true });
  await batch.commit();
}

/**
 * Neuen Code erzeugen und alle bisherigen entwerten – es ist also immer
 * höchstens einer gleichzeitig gültig, wie in der App.
 *
 * Der Code ist die Dokument-ID, also darf er nicht schon existieren. Bei einer
 * Kollision wird ein neuer gezogen; bei 32^8 Möglichkeiten passiert das
 * praktisch nie, aber ein stillschweigend überschriebenes fremdes Dokument
 * wäre der schlimmere Fall.
 */
export async function createInvite(list: TodoList, uid: string): Promise<Invite> {
  await revokeInvitesFor(list.id);

  const expiresAt = Timestamp.fromMillis(Date.now() + VALID_DAYS * 24 * 60 * 60 * 1000);

  for (let attempt = 0; attempt < 5; attempt++) {
    const code = generateCode();
    const ref = doc(db(), "invites", code);
    if ((await getDoc(ref)).exists()) continue;

    const invite: Omit<Invite, "code"> = {
      listId: list.id,
      listName: list.name,
      listColor: list.color,
      listIcon: list.icon,
      memberCount: list.memberIds.length,
      createdBy: uid,
      createdAt: Timestamp.now(),
      expiresAt,
      revoked: false,
    };
    await setDoc(ref, invite);
    return { code, ...invite };
  }
  throw new Error("Einladungscode konnte nicht erzeugt werden");
}

/** Einladung ansehen, ohne beizutreten. Null bei unbekannt, widerrufen, abgelaufen. */
export async function previewInvite(rawCode: string): Promise<Invite | null> {
  const code = normalizeCode(rawCode);
  if (!code) return null;

  const snapshot = await getDoc(doc(db(), "invites", code));
  if (!snapshot.exists()) return null;

  const invite = { code, ...snapshot.data() } as Invite;
  return isUsable(invite) ? invite : null;
}

/**
 * Liste beitreten.
 *
 * `arrayUnion` statt Lesen-Ändern-Schreiben, damit zwei gleichzeitige Beitritte
 * sich nicht gegenseitig überschreiben. Das Feld `joinedVia` trägt den
 * benutzten Code – die Security Rules prüfen darüber, ob überhaupt eine gültige
 * Einladung vorliegt. Lesezugriff auf die Liste besteht erst danach.
 */
export async function joinWithInvite(
  rawCode: string,
  uid: string,
  displayName: string
): Promise<string | null> {
  const invite = await previewInvite(rawCode);
  if (!invite) return null;

  await updateDoc(doc(db(), "lists", invite.listId), {
    memberIds: arrayUnion(uid),
    [`memberNames.${uid}`]: displayName,
    joinedVia: invite.code,
  });
  return invite.listId;
}

/**
 * Liste verlassen.
 *
 * Bewusst nur das Entfernen aus `memberIds`: ob die Liste gelöscht werden muss,
 * weil die letzte Person geht, entscheidet die App. Vom Web aus wäre dieser
 * Fall nicht sicher zu erkennen, weil nach dem Austritt kein Lesezugriff mehr
 * besteht. Der Name bleibt in `memberNames` stehen, damit alte Kommentare
 * weiterhin einen Verfasser haben.
 */
export async function leaveList(listId: string, uid: string): Promise<void> {
  await updateDoc(doc(db(), "lists", listId), {
    memberIds: arrayRemove(uid),
    adminIds: arrayRemove(uid),
    joinedVia: deleteField(),
  });
}
