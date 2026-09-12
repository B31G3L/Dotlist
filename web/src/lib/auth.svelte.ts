import {
  GoogleAuthProvider,
  onAuthStateChanged,
  signInWithPopup,
  signOut as firebaseSignOut,
  type User,
} from "firebase/auth";
import { auth } from "./firebase";

/**
 * Anmeldestatus als Rune, damit Komponenten einfach darauf reagieren können.
 *
 * Anders als die Android-App gibt es hier bewusst KEINE anonyme Anmeldung:
 * ein anonymer Login würde eine neue, leere UID erzeugen, und der Nutzer
 * stünde vor einer leeren Liste, obwohl seine Daten unter der Google-UID
 * liegen. Web und Desktop starten deshalb direkt mit Google.
 */
class AuthState {
  user = $state<User | null>(null);
  /** Solange true, steht noch nicht fest, ob jemand angemeldet ist. */
  loading = $state(true);
  error = $state<string | null>(null);

  constructor() {
    onAuthStateChanged(auth(), (user) => {
      this.user = user;
      this.loading = false;
    });
  }

  get uid(): string | null {
    return this.user?.uid ?? null;
  }

  async signIn(): Promise<void> {
    this.error = null;
    try {
      await signInWithPopup(auth(), new GoogleAuthProvider());
    } catch (e) {
      const code = (e as { code?: string }).code ?? "";
      // Popup geschlossen oder abgebrochen ist kein Fehler, den man anzeigen muss.
      if (code === "auth/popup-closed-by-user" || code === "auth/cancelled-popup-request") return;
      this.error =
        code === "auth/unauthorized-domain"
          ? "Diese Domain ist in Firebase Auth nicht freigegeben."
          : "Anmeldung fehlgeschlagen. Bitte noch einmal versuchen.";
      console.error("Anmeldung fehlgeschlagen", e);
    }
  }

  async signOut(): Promise<void> {
    await firebaseSignOut(auth());
  }
}

let instance: AuthState | undefined;

/** Erst im Browser erzeugen – onAuthStateChanged gibt es in Node nicht. */
export function authState(): AuthState {
  instance ??= new AuthState();
  return instance;
}
