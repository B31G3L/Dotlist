<script lang="ts">
  import { goto } from "$app/navigation";
  import { page } from "$app/state";
  import { activeInvite, createInvite, leaveList, type Invite } from "./invites";
  import { canManageMembers, roleOf, type TodoList } from "./types";

  interface Props {
    list: TodoList;
    uid: string;
  }

  let { list, uid }: Props = $props();

  let invite = $state<Invite | null>(null);
  let loading = $state(true);
  let error = $state<string | null>(null);
  let copied = $state(false);

  const canManage = $derived(canManageMembers(list, uid));
  const inviteUrl = $derived(invite ? `${page.url.origin}/join/${invite.code}` : "");

  $effect(() => {
    const listId = list.id;
    let cancelled = false;

    loading = true;
    error = null;
    activeInvite(listId)
      .then((result) => {
        if (cancelled) return;
        invite = result;
      })
      .catch((e) => {
        if (cancelled) return;
        error = "Einladung konnte nicht geladen werden.";
        console.error("Einladung laden", e);
      })
      .finally(() => {
        if (!cancelled) loading = false;
      });

    return () => {
      cancelled = true;
    };
  });

  async function newCode() {
    loading = true;
    error = null;
    copied = false;
    try {
      invite = await createInvite(list, uid);
    } catch (e) {
      error = "Code konnte nicht erzeugt werden.";
      console.error("Einladung erzeugen", e);
    } finally {
      loading = false;
    }
  }

  async function copyLink() {
    await navigator.clipboard.writeText(inviteUrl);
    copied = true;
  }

  async function leave() {
    if (!confirm(`„${list.name}" wirklich verlassen?`)) return;
    await leaveList(list.id, uid);
    await goto("/");
  }

  function labelFor(memberId: string): string {
    const role = roleOf(list, memberId);
    if (role === "BESITZER") return "Besitzer";
    if (role === "ADMIN") return "Admin";
    return "";
  }
</script>

<section class="members">
  <h2>Mitglieder</h2>

  <ul>
    {#each list.memberIds as memberId (memberId)}
      <li>
        <span class="name">{list.memberNames[memberId] ?? "Unbekannt"}</span>
        {#if memberId === uid}
          <span class="tag">Du</span>
        {/if}
        {#if labelFor(memberId)}
          <span class="tag">{labelFor(memberId)}</span>
        {/if}
      </li>
    {/each}
  </ul>

  {#if canManage}
    <h3>Einladung</h3>
    {#if loading}
      <p class="muted">Einen Moment …</p>
    {:else if invite}
      <div class="invite">
        <code>{invite.code}</code>
        <button class="text-button" onclick={copyLink}>
          {copied ? "Kopiert" : "Link kopieren"}
        </button>
        <button class="text-button" onclick={newCode}>Neuer Code</button>
      </div>
      <p class="muted">
        Gültig bis {invite.expiresAt.toDate().toLocaleDateString("de-DE")}. Ein neuer Code macht
        diesen ungültig.
      </p>
    {:else}
      <p class="muted">Es gibt gerade keinen gültigen Code.</p>
      <button class="filled-button" onclick={newCode}>Code erzeugen</button>
    {/if}
  {/if}

  {#if error}
    <p class="error">{error}</p>
  {/if}

  <footer>
    <button class="text-button danger" onclick={leave}>Liste verlassen</button>
  </footer>
</section>

<style>
  .members {
    border-top: 1px solid var(--outline-variant);
    margin-top: 2.5rem;
    padding-top: 1.5rem;
  }

  h2 {
    font-size: 1.125rem;
  }

  h3 {
    font-size: 0.9375rem;
    font-weight: 500;
    margin: 1.5rem 0 0.5rem;
  }

  ul {
    display: flex;
    flex-direction: column;
    gap: 0.375rem;
    list-style: none;
    margin: 0.75rem 0 0;
    padding: 0;
  }

  li {
    align-items: center;
    display: flex;
    gap: 0.5rem;
  }

  .tag {
    background: var(--secondary-container);
    border-radius: var(--radius-full);
    color: var(--on-secondary-container);
    font-size: 0.75rem;
    padding: 0.0625rem 0.5rem;
  }

  .invite {
    align-items: center;
    display: flex;
    flex-wrap: wrap;
    gap: 0.5rem;
  }

  code {
    background: var(--surface-container-high);
    border-radius: var(--radius-s);
    font-family: ui-monospace, monospace;
    font-size: 1rem;
    letter-spacing: 0.1em;
    padding: 0.375rem 0.75rem;
  }

  .muted {
    color: var(--on-surface-variant);
    font-size: 0.875rem;
  }

  .error {
    color: var(--error);
  }

  footer {
    margin-top: 1.5rem;
  }

  .danger {
    color: var(--error);
  }
</style>
