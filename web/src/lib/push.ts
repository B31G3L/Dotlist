import { Timestamp, doc, setDoc, deleteDoc } from "firebase/firestore";
import { getMessaging, getToken, isSupported } from "firebase/messaging";
import { PUBLIC_FIREBASE_VAPID_KEY } from "$env/static/public";
import { app, db } from "./firebase";

/**
 * Web-Push.
 *
 * Der Token landet in derselben Struktur wie bei Android:
 *   deviceTokens/{uid}/tokens/{installationId}
 * Die Cloud Function liest beide Plattformen aus einer Collection und schickt
 * an alle Geräte gleichzeitig. `lang` steuert die Sprache der Push-Texte.
 */

const INSTALLATION_KEY = "list2share:installationId";

/**
 * Stabile ID dieses Browsers. Ohne sie bekäme jeder Seitenaufruf ein neues
 * Token-Dokument, und es würden sich tote Registrierungen ansammeln.
 * Beim Löschen der Browserdaten entsteht eine neue – dann räumt die Cloud
 * Function den alten Token auf, sobald FCM ihn als ungültig meldet.
 */
function installationId(): string {
  let id = localStorage.getItem(INSTALLATION_KEY);
  if (!id) {
    id = crypto.randomUUID();
    localStorage.setItem(INSTALLATION_KEY, id);
  }
  return id;
}

function tokenRef(uid: string) {
  return doc(db(), "deviceTokens", uid, "tokens", installationId());
}

/** Läuft Web-Push in diesem Browser überhaupt? Safari kann es erst als installierte PWA. */
export async function pushSupported(): Promise<boolean> {
  return Boolean(PUBLIC_FIREBASE_VAPID_KEY) && (await isSupported());
}

export type PushState = "aus" | "an" | "blockiert";

export function currentPushState(): PushState {
  if (typeof Notification === "undefined") return "blockiert";
  if (Notification.permission === "denied") return "blockiert";
  return Notification.permission === "granted" ? "an" : "aus";
}

/**
 * Erlaubnis holen und Token registrieren.
 *
 * Der Service Worker muss bereit sein, bevor FCM ein Token ausstellt – sonst
 * registriert die Bibliothek einen eigenen unter einem anderen Pfad, und der
 * Klick auf eine Benachrichtigung landet im Nichts.
 */
export async function enablePush(uid: string): Promise<boolean> {
  if (!(await pushSupported())) return false;

  const permission = await Notification.requestPermission();
  if (permission !== "granted") return false;

  const registration = await navigator.serviceWorker.ready;
  const token = await getToken(getMessaging(app()), {
    vapidKey: PUBLIC_FIREBASE_VAPID_KEY,
    serviceWorkerRegistration: registration,
  });
  if (!token) return false;

  await setDoc(
    tokenRef(uid),
    {
      token,
      platform: "web",
      lang: navigator.language.slice(0, 2).toLowerCase(),
      pushEnabled: true,
      updatedAt: Timestamp.now(),
    },
    { merge: true }
  );
  return true;
}

/**
 * Registrierung dieses Browsers entfernen.
 *
 * Die Browser-Erlaubnis bleibt bestehen – die kann nur der Nutzer selbst in den
 * Seiteneinstellungen zurücknehmen. Ohne Token-Dokument schickt die Cloud
 * Function aber nichts mehr hierher.
 */
export async function disablePush(uid: string): Promise<void> {
  await deleteDoc(tokenRef(uid));
}
