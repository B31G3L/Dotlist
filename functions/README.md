# Cloud Functions

## Was hier läuft

| Funktion                | Auslöser                          | Aufgabe |
|-------------------------|-----------------------------------|---------|
| `onNotificationCreated` | neues Dokument in `notifications` | Push an alle Geräte des Empfängers |
| `sendDueReminders`      | alle 5 Minuten                    | fällige Erinnerungen finden und als Benachrichtigung anlegen |

Der Versand hängt allein an `onNotificationCreated`. Wer eine Push-Nachricht
auslösen will – App, Web-Client oder der Erinnerungs-Job – legt einfach ein
Dokument in `notifications` an.

## Datenmodell

    deviceTokens/{uid}                         (alt, wird noch mitgeschrieben)
        token, pushEnabled, updatedAt
    deviceTokens/{uid}/tokens/{installationId} (aktuell, ein Dokument pro Gerät)
        token, platform, lang, pushEnabled, updatedAt

## Befehle

    npm install
    npm run build
    node --test lib/            # Tests der Erinnerungslogik
    npm run deploy

Der Index für die Erinnerungs-Abfrage liegt in `firestore.indexes.json`:

    firebase deploy --only firestore:indexes

## Beim ersten Deploy beachten

Die Funktionen laufen in `europe-west3`. Die Region einer bestehenden Funktion
lässt sich nicht ändern – die bisherige Function muss vorher gelöscht werden:

    firebase functions:list
    firebase functions:delete <alter-name>

Sonst laufen beide parallel und jede Push-Nachricht geht doppelt raus.
