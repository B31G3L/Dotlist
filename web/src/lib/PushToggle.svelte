<script lang="ts">
  import { currentPushState, disablePush, enablePush, pushSupported, type PushState } from "./push";

  interface Props {
    uid: string;
  }

  let { uid }: Props = $props();

  let supported = $state(false);
  let pushState = $state<PushState>("aus");
  let busy = $state(false);
  let error = $state<string | null>(null);

  $effect(() => {
    let cancelled = false;
    pushSupported().then((result) => {
      if (cancelled) return;
      supported = result;
      pushState = currentPushState();
    });
    return () => {
      cancelled = true;
    };
  });

  async function toggle() {
    busy = true;
    error = null;
    try {
      if (pushState === "an") {
        await disablePush(uid);
        pushState = "aus";
      } else if (await enablePush(uid)) {
        pushState = "an";
      } else {
        // Entweder abgelehnt oder der Browser hat die Erlaubnis blockiert.
        pushState = currentPushState();
      }
    } catch (e) {
      error = "Benachrichtigungen konnten nicht eingerichtet werden.";
      console.error("Push einrichten", e);
    } finally {
      busy = false;
    }
  }
</script>

{#if supported}
  <div class="push">
    {#if pushState === "blockiert"}
      <p class="muted">
        Benachrichtigungen sind für diese Seite blockiert. Das lässt sich nur in den
        Browser-Einstellungen der Seite wieder ändern.
      </p>
    {:else}
      <button class="text-button" onclick={toggle} disabled={busy}>
        {pushState === "an" ? "Benachrichtigungen aus" : "Benachrichtigungen an"}
      </button>
    {/if}
    {#if error}
      <p class="error">{error}</p>
    {/if}
  </div>
{/if}

<style>
  .push {
    margin-top: 2rem;
  }

  .muted {
    color: var(--on-surface-variant);
    font-size: 0.875rem;
    margin: 0;
  }

  .error {
    color: var(--error);
    font-size: 0.875rem;
  }
</style>
