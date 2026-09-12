# Web-Client

SvelteKit als reine Single-Page-App, ausgeliefert über Firebase Hosting.
Gleiche Firestore-Daten wie die Android-App, gleiche Security Rules.

## Einrichten

1. In der Firebase Console unter *Projekteinstellungen → Meine Apps* eine
   Web-App anlegen und die Config kopieren.
2. `.env.example` nach `.env` kopieren und die Werte eintragen.
3. `npm install`

## Befehle

    npm run dev        # Entwicklungsserver
    npm run check      # Typen und Svelte prüfen
    npm run build      # nach web/build
    firebase deploy --only hosting

## Vor dem ersten Deploy

Die Domain muss in Firebase Auth unter *Authentifizierung → Einstellungen →
Autorisierte Domains* eingetragen sein, sonst schlägt der Google-Login fehl.
`*.web.app` und `*.firebaseapp.com` sind ab Werk freigegeben.

## Stand

Fertig: Google-Login, Listenübersicht, Aufgaben in Echtzeit, anlegen,
abhaken, löschen. Installierbar als PWA (Manifest liegt bei, die Icons in
`static/icons/` sind einfarbige Platzhalter und gehören ersetzt).

Fehlt noch: Kommentare, Unteraufgaben, Fälligkeit und Erinnerung setzen,
Mitglieder einladen, Einladungslinks, Web-Push (Service Worker plus
VAPID-Key), Offline-Anzeige.

Wichtig: Änderungen aus dem Web erzeugen noch keine Einträge in
`notifications`. Die Android-App legt die selbst an, wenn jemand etwas
zuweist, abhakt oder kommentiert. Solange das hier fehlt, lösen Web-Aktionen
keine Push-Nachrichten bei den anderen Mitgliedern aus.
