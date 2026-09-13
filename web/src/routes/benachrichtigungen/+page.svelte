<script lang="ts">
  import { authState } from "$lib/auth.svelte";
  import {
    NotificationsQuery,
    markAllRead,
    markRead,
    notificationText,
  } from "$lib/notificationsFeed.svelte";
  import { formatWhen } from "$lib/activity";
  import type { AppNotification } from "$lib/types";

  const auth = authState();

  let feed = $state<NotificationsQuery | null>(null);

  $effect(() => {
    const uid = auth.uid;
    if (!uid) {
      feed = null;
      return;
    }
    const query = new NotificationsQuery(uid);
    feed = query;
    return () => query.stop();
  });

  /** Ziel des Eintrags; ohne Liste bleibt er nur ein Hinweis. */
  function href(item: AppNotification): string | null {
    return item.listId ? `/lists/${item.listId}` : null;
  }

  function open(item: AppNotification) {
    if (!item.isRead) markRead(item.id);
  }
</script>

{#if auth.loading}
  <p class="muted">Einen Moment …</p>
{:else if !auth.user}
  <p class="muted">Bitte zuerst <a href="/">anmelden</a>.</p>
{:else}
  <div class="header">
    <h1>Benachrichtigungen</h1>
    {#if feed && feed.unread > 0}
      <button class="text-button" onclick={() => markAllRead(feed!.items)}>
        Alle als gelesen
      </button>
    {/if}
  </div>

  {#if feed?.error}
    <p class="error">{feed.error}</p>
  {:else if feed?.loading}
    <p class="muted">Wird geladen …</p>
  {:else if feed && feed.items.length === 0}
    <p class="muted">Noch nichts passiert.</p>
  {:else if feed}
    <ul>
      {#each feed.items as item (item.id)}
        <li class:unread={!item.isRead}>
          {#if href(item)}
            <a href={href(item)} onclick={() => open(item)}>{notificationText(item)}</a>
          {:else}
            <span>{notificationText(item)}</span>
          {/if}
          {#if item.createdAt}
            <span class="when">{formatWhen(item.createdAt.toDate())}</span>
          {/if}
        </li>
      {/each}
    </ul>
  {/if}
{/if}

<style>
  .header {
    align-items: center;
    display: flex;
    gap: 1rem;
    justify-content: space-between;
  }

  ul {
    display: flex;
    flex-direction: column;
    gap: 0.25rem;
    list-style: none;
    margin: 1.5rem 0 0;
    padding: 0;
  }

  li {
    align-items: baseline;
    background: var(--surface-container);
    border-radius: var(--radius-m);
    display: flex;
    gap: 0.75rem;
    justify-content: space-between;
    padding: 0.75rem 1rem;
  }

  li.unread {
    border-left: 3px solid var(--primary);
  }

  a {
    color: inherit;
    text-decoration: none;
  }

  .when {
    color: var(--on-surface-variant);
    flex: none;
    font-size: 0.8125rem;
  }

  .muted {
    color: var(--on-surface-variant);
  }

  .error {
    color: var(--error);
  }
</style>
