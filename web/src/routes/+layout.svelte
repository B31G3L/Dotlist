<script lang="ts">
  import "../app.css";
  import { authState } from "$lib/auth.svelte";
  import { connection } from "$lib/connection.svelte";
  import { NotificationsQuery } from "$lib/notificationsFeed.svelte";

  let { children } = $props();
  const auth = authState();
  const net = connection();

  // Ungelesene Benachrichtigungen fürs Abzeichen im Kopf. Derselbe Listener
  // versorgt auch die Benachrichtigungsseite – zwei Abfragen auf dieselbe
  // Collection wären Verschwendung, aber getrennte Komponenten teilen hier
  // keinen Zustand, also bleibt es bei einem schlanken Zähler.
  let feed = $state<NotificationsQuery | null>(null);
  $effect(() => {
    const uid = auth.uid;
    if (!uid) {
      feed = null;
      return;
    }
    const query = new NotificationsQuery(uid, 20);
    feed = query;
    return () => query.stop();
  });
</script>

<div class="shell">
  <header>
    <a class="brand" href="/">List2Share</a>
    {#if auth.user}
      <div class="account">
        <a class="bell" href="/benachrichtigungen" aria-label="Benachrichtigungen">
          Mitteilungen
          {#if feed && feed.unread > 0}
            <span class="badge">{feed.unread}</span>
          {/if}
        </a>
        <span class="email">{auth.user.email}</span>
        <button class="text-button" onclick={() => auth.signOut()}>Abmelden</button>
      </div>
    {/if}
  </header>

  {#if !net.online}
    <p class="offline" role="status">
      Offline – Änderungen werden gespeichert und später übertragen.
    </p>
  {/if}

  <main>
    {@render children()}
  </main>
</div>

<style>
  .shell {
    margin: 0 auto;
    max-width: 52rem;
    padding: 0 1rem 4rem;
  }

  header {
    align-items: center;
    display: flex;
    gap: 1rem;
    justify-content: space-between;
    padding: 1.25rem 0;
  }

  .brand {
    color: var(--on-surface);
    font-size: 1.25rem;
    font-weight: 500;
    text-decoration: none;
  }

  .account {
    align-items: center;
    display: flex;
    gap: 0.5rem;
  }

  .email {
    color: var(--on-surface-variant);
    font-size: 0.875rem;
  }

  .bell {
    align-items: center;
    color: var(--on-surface-variant);
    display: flex;
    font-size: 0.875rem;
    gap: 0.375rem;
    text-decoration: none;
  }

  .badge {
    background: var(--primary);
    border-radius: var(--radius-full);
    color: var(--on-primary);
    font-size: 0.75rem;
    line-height: 1.4;
    min-width: 1.25rem;
    padding: 0 0.375rem;
    text-align: center;
  }

  .offline {
    background: var(--secondary-container);
    border-radius: var(--radius-m);
    color: var(--on-secondary-container);
    font-size: 0.875rem;
    margin: 0 0 1rem;
    padding: 0.625rem 1rem;
  }

  @media (max-width: 30rem) {
    .email {
      display: none;
    }
  }
</style>
