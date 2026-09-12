<script lang="ts">
  import { goto } from "$app/navigation";
  import { page } from "$app/state";
  import { authState } from "$lib/auth.svelte";
  import { joinWithInvite, previewInvite, type Invite } from "$lib/invites";

  const auth = authState();
  const code = $derived(page.params.code ?? "");

  let invite = $state<Invite | null>(null);
  let loading = $state(true);
  let joining = $state(false);
  let error = $state<string | null>(null);

  // Vorschau ohne Anmeldung ist nicht möglich: die Rules verlangen auch für das
  // Einladungsdokument einen angemeldeten Nutzer. Deshalb erst nach dem Login.
  $effect(() => {
    if (auth.loading) return;
    if (!auth.user) {
      loading = false;
      return;
    }

    let cancelled = false;
    loading = true;
    previewInvite(code)
      .then((result) => {
        if (!cancelled) invite = result;
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

  async function join() {
    const uid = auth.uid;
    if (!uid || joining) return;

    joining = true;
    error = null;
    try {
      const listId = await joinWithInvite(code, uid, auth.user?.displayName ?? "Ich");
      if (listId) {
        await goto(`/lists/${listId}`);
        return;
      }
      error = "Diese Einladung ist nicht mehr gültig.";
    } catch (e) {
      error = "Beitreten fehlgeschlagen.";
      console.error("Beitreten", e);
    }
    joining = false;
  }
</script>

<section>
  {#if auth.loading || loading}
    <p class="muted">Einen Moment …</p>
  {:else if !auth.user}
    <h1>Du wurdest eingeladen</h1>
    <p>Melde dich an, um der Liste beizutreten.</p>
    <button class="filled-button" onclick={() => auth.signIn()}>Mit Google anmelden</button>
    {#if auth.error}
      <p class="error">{auth.error}</p>
    {/if}
  {:else if !invite}
    <h1>Einladung ungültig</h1>
    <p>
      Der Code ist unbekannt, abgelaufen oder wurde zurückgezogen. Frag die Person, die dich
      eingeladen hat, nach einem neuen Link.
    </p>
    <a class="text-button" href="/">Zu meinen Listen</a>
  {:else}
    <h1>„{invite.listName}" beitreten</h1>
    <p>
      {invite.memberCount}
      {invite.memberCount === 1 ? "Mitglied" : "Mitglieder"} · gültig bis
      {invite.expiresAt.toDate().toLocaleDateString("de-DE")}
    </p>
    <button class="filled-button" onclick={join} disabled={joining}>Beitreten</button>
    {#if error}
      <p class="error">{error}</p>
    {/if}
  {/if}
</section>

<style>
  section {
    margin-top: 3rem;
    max-width: 32rem;
  }

  p {
    color: var(--on-surface-variant);
    margin: 1rem 0 2rem;
  }

  .muted {
    color: var(--on-surface-variant);
  }

  .error {
    color: var(--error);
    margin: 1rem 0 0;
  }
</style>
