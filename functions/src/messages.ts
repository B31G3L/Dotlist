import { PushContent } from "./push";

/**
 * Texte der Push-Nachrichten.
 *
 * Absichtlich wortgleich zu den In-App-Texten in den strings.xml der App
 * (notif_assigned, notif_done, …): dieselbe Meldung soll in der Benachrichtigung
 * und in der Liste im App-Screen identisch aussehen. Wird dort etwas geändert,
 * gehört es auch hierher.
 *
 * Die Sprache kommt aus dem Token-Dokument des jeweiligen Geräts. Fehlt sie
 * oder ist sie unbekannt, wird Englisch verwendet.
 */

const APP_NAME = "List2Share";

type Strings = {
  assigned: (actor: string, title: string) => string;
  done: (actor: string, title: string) => string;
  comment: (actor: string, title: string) => string;
  invite: (actor: string, listName: string) => string;
  edited: (actor: string, title: string) => string;
  reminderTitle: string;
  reminderDue: (title: string) => string;
};

const de: Strings = {
  assigned: (a, t) => `${a} hat dir „${t}“ zugewiesen`,
  done: (a, t) => `${a} hat „${t}“ erledigt`,
  comment: (a, t) => `${a} hat zu „${t}“ kommentiert`,
  invite: (a, l) => `${a} lädt dich zur Liste „${l}“ ein`,
  edited: (a, t) => `${a} hat „${t}“ bearbeitet`,
  reminderTitle: "Erinnerung",
  reminderDue: (t) => `„${t}“ wird bald fällig`,
};

const en: Strings = {
  assigned: (a, t) => `${a} assigned you “${t}”`,
  done: (a, t) => `${a} completed “${t}”`,
  comment: (a, t) => `${a} commented on “${t}”`,
  invite: (a, l) => `${a} invites you to the list “${l}”`,
  edited: (a, t) => `${a} edited “${t}”`,
  reminderTitle: "Reminder",
  reminderDue: (t) => `“${t}” is due soon`,
};

/** "de-DE", "de_AT" und "de" führen alle zu Deutsch. */
function stringsFor(lang: string): Strings {
  return lang.slice(0, 2).toLowerCase() === "de" ? de : en;
}

/**
 * Text für eine Benachrichtigung aus der `notifications`-Collection.
 * Unbekannte Typen bekommen bewusst denselben Fallback wie in der App.
 */
export function notificationContent(
  type: string,
  actorName: string,
  todoTitle: string
): (lang: string) => PushContent {
  return (lang: string) => {
    const s = stringsFor(lang);
    switch (type) {
      case "ZUGEWIESEN":
        return { title: APP_NAME, body: s.assigned(actorName, todoTitle) };
      case "ERLEDIGT":
        return { title: APP_NAME, body: s.done(actorName, todoTitle) };
      case "KOMMENTAR":
        return { title: APP_NAME, body: s.comment(actorName, todoTitle) };
      case "EINLADUNG":
        return { title: APP_NAME, body: s.invite(actorName, todoTitle) };
      case "ERINNERUNG":
        return { title: s.reminderTitle, body: s.reminderDue(todoTitle) };
      default:
        return { title: APP_NAME, body: s.edited(actorName, todoTitle) };
    }
  };
}
