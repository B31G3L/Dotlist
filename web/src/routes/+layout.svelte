<script lang="ts">
  import "../app.css";
  import { authState } from "$lib/auth.svelte";

  let { children } = $props();
  const auth = authState();
</script>

<div class="shell">
  <header>
    <a class="brand" href="/">List2Share</a>
    {#if auth.user}
      <div class="account">
        <span class="email">{auth.user.email}</span>
        <button class="text-button" onclick={() => auth.signOut()}>Abmelden</button>
      </div>
    {/if}
  </header>

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

  @media (max-width: 30rem) {
    .email {
      display: none;
    }
  }
</style>
