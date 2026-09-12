<script lang="ts">
  import { page } from "$app/state";
  import { authState } from "$lib/auth.svelte";
  import {
    ListsQuery,
    TodosQuery,
    createTodo,
    deleteTodo,
    setTodoDone,
  } from "$lib/lists.svelte";
  import type { TodoItem } from "$lib/types";

  const auth = authState();
  const listId = $derived(page.params.id!);

  let lists = $state<ListsQuery | null>(null);
  let todos = $state<TodosQuery | null>(null);
  let newTitle = $state("");

  // Die Liste selbst kommt aus derselben Abfrage wie die Übersicht: ein
  // direkter Zugriff auf lists/{id} wäre eine zweite Verbindung für Daten,
  // die ohnehin schon im Cache liegen.
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

  $effect(() => {
    if (!auth.uid) {
      todos = null;
      return;
    }
    const query = new TodosQuery(listId);
    todos = query;
    return () => query.stop();
  });

  const list = $derived(lists?.items.find((l) => l.id === listId) ?? null);
  const open = $derived(todos?.items.filter((t) => !t.isDone) ?? []);
  const done = $derived(todos?.items.filter((t) => t.isDone) ?? []);

  async function addTodo(event: SubmitEvent) {
    event.preventDefault();
    const title = newTitle.trim();
    const uid = auth.uid;
    if (!title || !uid) return;

    // Ans Ende einsortieren, gleiche Logik wie in der App.
    const highest = todos?.items.reduce((max, t) => Math.max(max, t.position), 0) ?? 0;
    newTitle = "";
    await createTodo(listId, uid, title, highest + 1);
  }

  function toggle(todo: TodoItem) {
    const uid = auth.uid;
    if (!uid) return;
    return setTodoDone(listId, todo, uid, !todo.isDone);
  }

  function formatDue(todo: TodoItem): string | null {
    if (!todo.dueDate) return null;
    return todo.dueDate.toDate().toLocaleString("de-DE", {
      day: "2-digit",
      month: "2-digit",
      hour: "2-digit",
      minute: "2-digit",
    });
  }
</script>

{#if !auth.loading && !auth.user}
  <p class="muted">Bitte zuerst <a href="/">anmelden</a>.</p>
{:else}
  <h1>{list?.name ?? "Liste"}</h1>

  <form onsubmit={addTodo}>
    <input
      class="text-field"
      bind:value={newTitle}
      placeholder="Neue Aufgabe"
      aria-label="Titel der neuen Aufgabe"
    />
    <button class="filled-button" disabled={!newTitle.trim()}>Hinzufügen</button>
  </form>

  {#if todos?.error}
    <p class="error">{todos.error}</p>
  {:else if todos?.loading}
    <p class="muted">Aufgaben werden geladen …</p>
  {:else if todos && todos.items.length === 0}
    <p class="muted">Diese Liste ist leer.</p>
  {:else}
    <ul>
      {#each open as todo (todo.id)}
        {@render row(todo)}
      {/each}
    </ul>

    {#if done.length > 0}
      <h2>Erledigt</h2>
      <ul class="done-list">
        {#each done as todo (todo.id)}
          {@render row(todo)}
        {/each}
      </ul>
    {/if}
  {/if}
{/if}

{#snippet row(todo: TodoItem)}
  <li class:done={todo.isDone}>
    <label>
      <input type="checkbox" checked={todo.isDone} onchange={() => toggle(todo)} />
      <span class="title">{todo.title}</span>
    </label>
    {#if formatDue(todo)}
      <span class="due">{formatDue(todo)}</span>
    {/if}
    {#if todo.priority === "HOCH"}
      <span class="priority">Hoch</span>
    {/if}
    <button
      class="text-button"
      onclick={() => deleteTodo(listId, todo.id)}
      aria-label={`${todo.title} löschen`}>Löschen</button
    >
  </li>
{/snippet}

<style>
  form {
    display: flex;
    gap: 0.75rem;
    margin: 1.5rem 0;
  }

  ul {
    display: flex;
    flex-direction: column;
    gap: 0.25rem;
    list-style: none;
    margin: 0;
    padding: 0;
  }

  li {
    align-items: center;
    background: var(--surface-container);
    border-radius: var(--radius-m);
    display: flex;
    gap: 0.75rem;
    padding: 0.625rem 0.75rem 0.625rem 1rem;
  }

  label {
    align-items: center;
    cursor: pointer;
    display: flex;
    flex: 1;
    gap: 0.75rem;
    min-width: 0;
  }

  input[type="checkbox"] {
    accent-color: var(--primary);
    height: 1.125rem;
    width: 1.125rem;
  }

  .title {
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
  }

  li.done .title {
    color: var(--on-surface-variant);
    text-decoration: line-through;
  }

  .due,
  .priority {
    flex: none;
    font-size: 0.8125rem;
  }

  .due {
    color: var(--on-surface-variant);
  }

  .priority {
    background: var(--primary-container);
    border-radius: var(--radius-full);
    color: var(--on-primary-container);
    padding: 0.125rem 0.625rem;
  }

  h2 {
    font-size: 1rem;
    margin: 2rem 0 0.75rem;
  }

  .done-list li {
    background: none;
    border: 1px solid var(--outline-variant);
  }

  .muted {
    color: var(--on-surface-variant);
  }

  .error {
    color: var(--error);
  }
</style>
