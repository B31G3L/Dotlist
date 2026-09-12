/// <reference types="@sveltejs/kit" />
/// <reference lib="webworker" />

import { build, files, version } from "$service-worker";
import { initializeApp } from "firebase/app";
import { getMessaging } from "firebase/messaging/sw";
import {
  PUBLIC_FIREBASE_API_KEY,
  PUBLIC_FIREBASE_APP_ID,
  PUBLIC_FIREBASE_AUTH_DOMAIN,
  PUBLIC_FIREBASE_MESSAGING_SENDER_ID,
  PUBLIC_FIREBASE_PROJECT_ID,
  PUBLIC_FIREBASE_STORAGE_BUCKET,
} from "$env/static/public";

/**
 * Service Worker – zwei Aufgaben:
 *  1. App-Shell zwischenspeichern, damit die installierte PWA auch offline
 *     startet. Die Daten selbst kommen aus dem Firestore-Cache.
 *  2. Firebase Messaging initialisieren. Der Aufruf sieht nutzlos aus, ist es
 *     aber nicht: dabei registriert die Bibliothek ihre push- und
 *     notificationclick-Handler. Ohne ihn erscheint keine Benachrichtigung,
 *     und der Klick öffnet nicht das Ziel aus fcmOptions.link.
 *
 * Kein onBackgroundMessage: die Cloud Function schickt eine notification-
 * Payload, die der Browser selbst anzeigt. Ein eigener Handler würde jede
 * Nachricht ein zweites Mal darstellen.
 */

const worker = self as unknown as ServiceWorkerGlobalScope;

getMessaging(
  initializeApp({
    apiKey: PUBLIC_FIREBASE_API_KEY,
    authDomain: PUBLIC_FIREBASE_AUTH_DOMAIN,
    projectId: PUBLIC_FIREBASE_PROJECT_ID,
    storageBucket: PUBLIC_FIREBASE_STORAGE_BUCKET,
    messagingSenderId: PUBLIC_FIREBASE_MESSAGING_SENDER_ID,
    appId: PUBLIC_FIREBASE_APP_ID,
  })
);

/** Pro Build ein eigener Cache, damit alte Dateien nicht hängen bleiben. */
const CACHE = `list2share-${version}`;
const PRECACHE = [...build, ...files];

worker.addEventListener("install", (event) => {
  event.waitUntil(
    caches
      .open(CACHE)
      .then((cache) => cache.addAll(PRECACHE))
      .then(() => worker.skipWaiting())
  );
});

worker.addEventListener("activate", (event) => {
  event.waitUntil(
    caches
      .keys()
      .then((keys) => Promise.all(keys.filter((key) => key !== CACHE).map((key) => caches.delete(key))))
      .then(() => worker.clients.claim())
  );
});

worker.addEventListener("fetch", (event) => {
  const { request } = event;
  if (request.method !== "GET") return;

  const url = new URL(request.url);
  if (url.origin !== location.origin) return;

  // Gebaute Dateien tragen einen Hash im Namen und ändern sich nie – direkt
  // aus dem Cache, ohne Netzumweg.
  if (PRECACHE.includes(url.pathname)) {
    event.respondWith(
      caches.open(CACHE).then(async (cache) => (await cache.match(url.pathname)) ?? fetch(request))
    );
    return;
  }

  // Seitenaufrufe: Netz zuerst, sonst die zwischengespeicherte App-Shell. Die
  // App ist eine SPA mit Fallback auf index.html, deshalb passt sie für jeden
  // Pfad – auch für /lists/abc nach einem Neuladen im Flugmodus.
  if (request.mode === "navigate") {
    event.respondWith(
      fetch(request).catch(async () => {
        const cache = await caches.open(CACHE);
        return (await cache.match("/index.html")) ?? Response.error();
      })
    );
  }
});
