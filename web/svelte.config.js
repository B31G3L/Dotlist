import adapter from "@sveltejs/adapter-static";
import { vitePreprocess } from "@sveltejs/vite-plugin-svelte";

/**
 * Reine Single-Page-App: alles wird zu statischen Dateien, die Firebase
 * Hosting ausliefert. Kein Node-Server nötig – die Daten kommen direkt aus
 * Firestore, die Rechteprüfung machen die Security Rules.
 *
 * `fallback` liefert für unbekannte Pfade index.html aus, damit tiefe Links
 * wie /lists/abc nach einem Neuladen funktionieren.
 */
export default {
  preprocess: vitePreprocess(),
  kit: {
    adapter: adapter({ fallback: "index.html" }),
  },
};
