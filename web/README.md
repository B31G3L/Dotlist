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

## Web-Push einrichten

In der Firebase Console unter *Projekteinstellungen → Cloud Messaging →
Web Push certificates* ein Schlüsselpaar erzeugen und den öffentlichen
Schlüssel als `PUBLIC_FIREBASE_VAPID_KEY` in die `.env` eintragen. Ohne
diesen Wert blendet die App den Schalter aus.

Der Service Worker wird nur im Produktions-Build registriert. Push lässt
sich deshalb mit `npm run dev` nicht testen – dafür `npm run build` und
`npm run preview` verwenden oder deployen.

## Vor dem ersten Deploy

Die Domain muss in Firebase Auth unter *Authentifizierung → Einstellungen →
Autorisierte Domains* eingetragen sein, sonst schlägt der Google-Login fehl.
`*.web.app` und `*.firebaseapp.com` sind ab Werk freigegeben.

## Stand

Fertig: Google-Login, Listenübersicht, Aufgaben in Echtzeit, anlegen,
abhaken, löschen. Detailansicht mit Titel, Beschreibung, Priorität,
Zuständigkeit, Fälligkeit, Erinnerung, Unteraufgaben und Kommentaren.
Benachrichtigungen werden erzeugt wie in der App, Pushes verschickt also
auch bei Web-Aktionen die Cloud Function. Installierbar als PWA (Manifest
liegt bei, die Icons in `static/icons/` sind einfarbige Platzhalter und
gehören ersetzt).

Mitglieder werden angezeigt, Einladungscodes lassen sich erzeugen und als
Link `/join/{code}` teilen, Beitreten und Verlassen funktionieren.

Web-Push funktioniert: Schalter auf der Startseite, Token landet in
`deviceTokens/{uid}/tokens/{installationId}` wie bei Android. Der Service
Worker legt zusätzlich die App-Shell in den Cache, die installierte PWA
startet also auch offline.

Fehlt noch: Mitglieder entfernen und zu Admins machen (nur in der App),
Offline-Anzeige, Sortieren per Drag & Drop.

Verlässt die letzte Person eine Liste, bleibt sie im Web als verwaistes
Dokument zurück. Die App löscht sie in diesem Fall; vom Web aus ist das
nicht sicher erkennbar, weil nach dem Austritt kein Lesezugriff mehr
besteht.
