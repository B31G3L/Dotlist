/**
 * Verbindungsstatus des Browsers.
 *
 * `navigator.onLine` ist nur ein grober Hinweis: es meldet, ob überhaupt eine
 * Netzwerkverbindung besteht, nicht ob Firestore erreichbar ist. Für den Zweck
 * reicht das – die Anzeige soll erklären, warum gerade nichts ankommt, und
 * nicht den Verbindungszustand exakt abbilden. Änderungen an den Daten laufen
 * offline ohnehin in den Firestore-Cache und gehen später raus.
 */
class Connection {
  online = $state(true);

  constructor() {
    this.online = navigator.onLine;
    addEventListener("online", () => (this.online = true));
    addEventListener("offline", () => (this.online = false));
  }
}

let instance: Connection | undefined;

/** Erst im Browser erzeugen – navigator gibt es in Node nicht. */
export function connection(): Connection {
  instance ??= new Connection();
  return instance;
}
