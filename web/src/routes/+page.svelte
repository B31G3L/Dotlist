<script lang="ts">
  import { goto } from "$app/navigation";
  import { authState } from "$lib/auth.svelte";
  import PushToggle from "$lib/PushToggle.svelte";
  import { ListsQuery, createList } from "$lib/lists.svelte";
  import type { ListMode } from "$lib/types";

  const auth = authState();

  let lists = $state<ListsQuery | null>(null);
  let newListName = $state("");
  let newListMode = $state<ListMode>("AUFGABEN");
  let creating = $state(false);

  // Listener nur solange jemand angemeldet ist. $effect räumt beim Abmelden
  // und beim Verlassen der Seite selbst auf.
  $effect(() => {
    const uid = auth.uid;
    if (!uid) {
      lists = null;
      return;
    }
    const query = new ListsQuery(uid);
    lists = query;
    return () => query.stop();
  });

  async function addList(event: SubmitEvent) {
    event.preventDefault();
    const name = newListName.trim();
    const uid = auth.uid;
    if (!name || !uid || creating) return;

    creating = true;
    try {
      const id = await createList(uid, name, auth.user?.displayName ?? "Ich", newListMode);
      newListName = "";
      newListMode = "AUFGABEN";
      await goto(`/lists/${id}`);
    } finally {
      creating = false;
    }
  }
</script>

{#if auth.loading}
  <p class="muted">Einen Moment …</p>
{:else if !auth.user}
  <section class="signin">
    <h1>Deine Listen, überall</h1>
    <p>
      Melde dich mit demselben Google-Konto an, das du in der App verwendest.
      Deine Listen sind dann sofort hier.
    </p>
    <button class="filled-button" onclick={() => auth.signIn()}>Mit Google anmelden</button>
    {#if auth.error}
      <p class="error">{auth.error}</p>
    {/if}
  </section>
{:else}
  <h1>Listen</h1>

  <form onsubmit={addList}>
    <input
      class="text-field"
      bind:value={newListName}
      placeholder="Neue Liste"
      aria-label="Name der neuen Liste"
    />
    <select class="text-field mode" bind:value={newListMode} aria-label="Art der Liste">
      <option value="AUFGABEN">Aufgaben</option>
      <option value="EINKAUFEN">Einkaufen</option>
      <option value="CHECKLISTE">Checkliste</option>
    </select>
    <button class="filled-button" disabled={!newListName.trim() || creating}>Anlegen</button>
  </form>

  {#if lists?.error}
    <p class="error">{lists.error}</p>
  {:else if lists?.loading}
    <p class="muted">Listen werden geladen …</p>
  {:else if lists && lists.items.length === 0}
    <p class="muted">Noch keine Listen. Leg oben deine erste an.</p>
  {:else if lists}
    <ul>
      {#each lists.items as list (list.id)}
        <li>
          <a href={`/lists/${list.id}`} style={`--accent: ${list.color}`}>
            <span class="dot"></span>
            <span class="name">{list.name}</span>
            {#if list.mode !== "AUFGABEN"}
              <span class="members">
                {list.mode === "EINKAUFEN" ? "Einkaufen" : "Checkliste"}
              </span>
            {/if}
            {#if list.memberIds.length > 1}
              <span class="members">{list.memberIds.length} Mitglieder</span>
            {/if}
          </a>
        </li>
      {/each}
    </ul>
  {/if}

  <PushToggle uid={auth.uid!} />
{/if}

<style>
  .signin {
    margin-top: 4rem;
    max-width: 32rem;
  }

  .signin p {
    color: var(--on-surface-variant);
    margin: 1rem 0 2rem;
  }

  form {
    display: flex;
    gap: 0.75rem;
    margin: 1.5rem 0;
  }

  .mode {
    flex: none;
    width: auto;
  }

  @media (max-width: 30rem) {
    form {
      flex-wrap: wrap;
    }
  }

  ul {
    display: flex;
    flex-direction: column;
    gap: 0.5rem;
    list-style: none;
    margin: 0;
    padding: 0;
  }

  a {
    align-items: center;
    background: var(--surface-container);
    border-radius: var(--radius-m);
    color: inherit;
    display: flex;
    gap: 0.75rem;
    padding: 1rem;
    text-decoration: none;
  }

  a:hover {
    background: var(--surface-container-high);
  }

  .dot {
    background: var(--accent, var(--primary));
    border-radius: var(--radius-full);
    flex: none;
    height: 0.75rem;
    width: 0.75rem;
  }

  .name {
    flex: 1;
  }

  .members {
    color: var(--on-surface-variant);
    font-size: 0.8125rem;
  }

  .muted {
    color: var(--on-surface-variant);
  }

  .error {
    color: var(--error);
  }
</style>
