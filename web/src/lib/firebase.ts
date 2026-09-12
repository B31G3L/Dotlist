import { initializeApp, type FirebaseApp } from "firebase/app";
import { getAuth, type Auth } from "firebase/auth";
import {
  initializeFirestore,
  persistentLocalCache,
  persistentMultipleTabManager,
  type Firestore,
} from "firebase/firestore";
import {
  PUBLIC_FIREBASE_API_KEY,
  PUBLIC_FIREBASE_APP_ID,
  PUBLIC_FIREBASE_AUTH_DOMAIN,
  PUBLIC_FIREBASE_MESSAGING_SENDER_ID,
  PUBLIC_FIREBASE_PROJECT_ID,
  PUBLIC_FIREBASE_STORAGE_BUCKET,
} from "$env/static/public";

/**
 * Firebase-Initialisierung.
 *
 * Alles wird erst beim ersten Zugriff erzeugt, nicht beim Import: Der Build
 * läuft in Node, und dort gibt es weder IndexedDB noch window. Würde hier
 * direkt initialisiert, scheitert schon das Prerendering.
 */

let instance: FirebaseApp | undefined;
let firestore: Firestore | undefined;

export function app(): FirebaseApp {
  instance ??= initializeApp({
    apiKey: PUBLIC_FIREBASE_API_KEY,
    authDomain: PUBLIC_FIREBASE_AUTH_DOMAIN,
    projectId: PUBLIC_FIREBASE_PROJECT_ID,
    storageBucket: PUBLIC_FIREBASE_STORAGE_BUCKET,
    messagingSenderId: PUBLIC_FIREBASE_MESSAGING_SENDER_ID,
    appId: PUBLIC_FIREBASE_APP_ID,
  });
  return instance;
}

export function auth(): Auth {
  return getAuth(app());
}

/**
 * Firestore mit dauerhaftem Cache.
 *
 * Auf Android ist der Cache Standard, im Web nicht – ohne diese Zeilen wäre
 * die Web-Version offline komplett leer, während die App weiterläuft.
 * `persistentMultipleTabManager` erlaubt mehrere offene Tabs; ohne ihn
 * bekommt nur der erste Tab den Cache und die übrigen laufen ohne.
 */
export function db(): Firestore {
  firestore ??= initializeFirestore(app(), {
    localCache: persistentLocalCache({ tabManager: persistentMultipleTabManager() }),
  });
  return firestore;
}
