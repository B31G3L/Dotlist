/**
 * Reine Client-App: kein Server-Rendering, weil jede Seite den angemeldeten
 * Nutzer und einen Firestore-Listener braucht. Beides gibt es in Node nicht.
 */
export const ssr = false;
export const prerender = false;
