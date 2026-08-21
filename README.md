# List2Share

Geteilte To-do-Listen für Android. Listen bleiben standardmäßig lokal auf dem Gerät
und werden erst dann in die Cloud gehoben, wenn man sie tatsächlich teilen will.

> Die App hieß früher *Dotlist* und wurde im Zuge eines Markenkonflikts umbenannt.

## Features

- Listen mit Farbe, Icon und Fortschrittsanzeige
- Aufgaben mit Priorität, Fälligkeit, Erinnerung, Unteraufgaben und Kommentaren
- Zuweisung an Mitglieder, Rollen (Besitzer / Admin / Bearbeiter)
- Teilen per Einladungscode, Push-Benachrichtigungen
- Kalenderansicht, Suche, Benachrichtigungs-Center
- Helles/dunkles Theme mit wählbarer Akzentfarbe
- Deutsch und Englisch

## Architektur

| Schicht | Umsetzung |
|---|---|
| UI | Jetpack Compose, Material 3, eigene Screen-Navigation in `MainScreen.kt` |
| State | ViewModels (`ListsViewModel`, `TodosViewModel`, `NotificationsViewModel`) |
| Daten | `TodoRepository` als einzige Datenquelle |
| Lokal | Room (`local_lists`, `local_todos`) für nicht geteilte Listen |
| Remote | Firestore (`lists/{id}/todos/{id}`) für geteilte Listen |
| Auth | Firebase Auth – anonym beim ersten Start, optional mit Google verknüpft |
| Push | Firebase Cloud Messaging |
| Preferences | DataStore |

Eine Liste liegt **entweder** in Room **oder** in Firestore, nie an beiden Orten.
Der Wechsel passiert über `TodoRepository.shareList()` bzw. `unshareList()`.

### Identität

Die Firebase-UID ist die Identität, gegen die die Firestore Security Rules prüfen
(`request.auth.uid`). Beim ersten Start wird anonym angemeldet; eine spätere
Google-Anmeldung verknüpft denselben Account, sodass die UID erhalten bleibt.
Gehört das Google-Konto bereits zu einem früheren Account (Gerätewechsel,
Neuinstallation), wird der frische anonyme Account verworfen und der bestehende
übernommen – siehe `AuthManager.signInWithGoogle()`.

## Build

Voraussetzungen: Android Studio, JDK 17, Android SDK 36.

```bash
git clone https://github.com/B31G3L/List2Share.git
cd List2Share
./gradlew assembleDebug
```

Für einen eigenen Build wird ein eigenes Firebase-Projekt benötigt:

1. Projekt in der Firebase Console anlegen, Android-App mit dem gewünschten
   Application-ID registrieren
2. `google-services.json` nach `app/` legen
3. Firestore, Authentication (Anonym + Google) und Cloud Messaging aktivieren
4. Web-Client-ID unter `google_web_client_id` in `app/src/main/res/values/strings.xml`
   eintragen

## Lizenz

_(noch festzulegen)_
