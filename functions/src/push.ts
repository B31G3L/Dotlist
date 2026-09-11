import { getFirestore, DocumentReference } from "firebase-admin/firestore";
import { getMessaging, TokenMessage } from "firebase-admin/messaging";
import { logger } from "firebase-functions";

/** Eine versandfähige Registrierung: Token plus die Stelle, an der er steht. */
interface TokenEntry {
  token: string;
  /** Dokument, aus dem der Token stammt – für das Aufräumen toter Tokens. */
  ref: DocumentReference;
  /** Altes Ein-Token-pro-Nutzer-Dokument (dort wird nur das Feld geleert). */
  legacy: boolean;
  /** Sprache des Geräts ("de", "en", …); steuert die Texte der Push-Nachricht. */
  lang: string;
}

/**
 * FCM-Fehlercodes, die bedeuten: dieser Token ist endgültig tot (App
 * deinstalliert, Daten gelöscht, Token rotiert). Alles andere – vor allem
 * `unavailable` und `internal` – ist vorübergehend und darf NICHT zum Löschen
 * führen, sonst verliert ein Nutzer bei einer FCM-Störung seine Registrierung.
 */
const DEAD_TOKEN_ERRORS = new Set([
  "messaging/registration-token-not-registered",
  "messaging/invalid-registration-token",
  "messaging/invalid-argument",
]);

/**
 * Alle Tokens eines Nutzers einsammeln.
 *
 * Zwei Quellen, weil das Datenmodell gerade umgestellt wird:
 *  - `deviceTokens/{uid}/tokens/{installationId}` – ein Dokument pro Gerät,
 *    das aktuelle Format (Android, Web, Desktop).
 *  - `deviceTokens/{uid}` – altes Format mit genau einem Token pro Nutzer.
 *    Wird nur noch von App-Versionen vor der Umstellung geschrieben.
 *
 * TODO: Sobald keine alte App-Version mehr aktiv ist, den Legacy-Teil und die
 *  Deduplizierung entfernen – aktuelle Versionen schreiben beides, derselbe
 *  Token taucht also zweimal auf.
 */
async function collectTokens(uid: string): Promise<TokenEntry[]> {
  const db = getFirestore();
  const ownerRef = db.collection("deviceTokens").doc(uid);

  const [installations, legacy] = await Promise.all([
    ownerRef.collection("tokens").get(),
    ownerRef.get(),
  ]);

  const entries: TokenEntry[] = [];
  const seen = new Set<string>();

  for (const doc of installations.docs) {
    const token = doc.get("token");
    // pushEnabled fehlt bei älteren Dokumenten – dann gilt "an".
    if (typeof token !== "string" || !token) continue;
    if (doc.get("pushEnabled") === false) continue;
    if (seen.has(token)) continue;
    seen.add(token);
    entries.push({
      token,
      ref: doc.ref,
      legacy: false,
      lang: typeof doc.get("lang") === "string" ? doc.get("lang") : "en",
    });
  }

  const legacyToken = legacy.get("token");
  if (
    typeof legacyToken === "string" &&
    legacyToken &&
    legacy.get("pushEnabled") !== false &&
    !seen.has(legacyToken)
  ) {
    entries.push({ token: legacyToken, ref: ownerRef, legacy: true, lang: "en" });
  }

  return entries;
}

/** Tote Tokens entfernen, damit sie nicht bei jedem Versand erneut scheitern. */
async function removeDeadTokens(entries: TokenEntry[]): Promise<void> {
  if (entries.length === 0) return;
  const db = getFirestore();
  const batch = db.batch();
  for (const entry of entries) {
    if (entry.legacy) {
      // Das Legacy-Dokument nicht löschen: es kann noch pushEnabled enthalten.
      batch.set(entry.ref, { token: null }, { merge: true });
    } else {
      batch.delete(entry.ref);
    }
  }
  await batch.commit();
}

/** Inhalt einer Push-Nachricht, aufgelöst je nach Gerätesprache. */
export interface PushContent {
  title: string;
  body: string;
}

export interface PushPayload {
  /** Liefert Titel und Text für die Sprache eines Geräts. */
  content: (lang: string) => PushContent;
  /** Landet im data-Teil, damit die App die richtige Stelle öffnen kann. */
  data: Record<string, string>;
}

/**
 * Schickt eine Nachricht an alle Geräte eines Nutzers.
 *
 * Bewusst `sendEach` statt `sendEachForMulticast`: die Nachrichten
 * unterscheiden sich pro Gerät (Sprache), und die Antworten kommen in
 * derselben Reihenfolge zurück wie die Eingabe.
 */
export async function pushToUser(uid: string, payload: PushPayload): Promise<number> {
  const entries = await collectTokens(uid);
  if (entries.length === 0) return 0;

  const messages: TokenMessage[] = entries.map((entry) => {
    const { title, body } = payload.content(entry.lang);
    return {
      token: entry.token,
      notification: { title, body },
      data: payload.data,
      android: {
        priority: "high" as const,
        notification: {
          // Eine Benachrichtigung je Liste, passend zur Gruppierung in der App.
          tag: payload.data.listId || undefined,
          channelId: "dotlist_notifications",
        },
      },
      webpush: {
        notification: { title, body, icon: "/icons/icon-192.png" },
        fcmOptions: {
          // Klick auf die Web-Benachrichtigung öffnet die passende Liste.
          link: payload.data.listId ? `/lists/${payload.data.listId}` : "/",
        },
      },
    };
  });

  const response = await getMessaging().sendEach(messages);

  const dead: TokenEntry[] = [];
  response.responses.forEach((result, index) => {
    if (result.success) return;
    const code = result.error?.code ?? "unknown";
    if (DEAD_TOKEN_ERRORS.has(code)) {
      dead.push(entries[index]);
    } else {
      logger.warn("Push fehlgeschlagen", { uid, code });
    }
  });
  await removeDeadTokens(dead);

  return response.successCount;
}
