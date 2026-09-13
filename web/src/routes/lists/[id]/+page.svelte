<script lang="ts">
  import { page } from "$app/state";
  import { authState } from "$lib/auth.svelte";
  import ListMembers from "$lib/ListMembers.svelte";
  import TodoDetail from "$lib/TodoDetail.svelte";
  import {
    ListsQuery,
    TodosQuery,
    createTodo,
    deleteDoneTodos,
    reorderTodos,
    resetAllTodos,
    setTodoDone,
  } from "$lib/lists.svelte";
  import { DEPARTMENT_LABELS, DEPARTMENT_ORDER, departmentFor } from "$lib/departments";
  import type { TodoItem } from "$lib/types";

  const auth = authState();
  const listId = $derived(page.params.id!);

  let lists = $state<ListsQuery | null>(null);
  let todos = $state<TodosQuery | null>(null);
  let newTitle = $state("");
  let selectedId = $state<string | null>(null);
  let draggedId = $state<string | null>(null);

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
  // Bewusst über die ID statt über das Objekt: der Listener liefert bei jeder
  // Änderung neue Objekte, und die geöffnete Aufgabe soll dabei aktuell bleiben.
  const selected = $derived(todos?.items.find((t) => t.id === selectedId) ?? null);
  const actorName = $derived(list && auth.uid ? (list.memberNames[auth.uid] ?? "") : "");
  const shopping = $derived(list?.mode === "EINKAUFEN");
  const checklist = $derived(list?.mode === "CHECKLISTE");
  /** Modi ohne Priorität, Zuständigkeit, Termine und Wiederholung. */
  const simple = $derived(shopping || checklist);
  const open = $derived(todos?.items.filter((t) => !t.isDone) ?? []);

  /**
   * Im Einkaufsmodus nach Abteilungen gruppiert statt nach Position: im Laden
   * will man alles aus einem Gang beieinander haben. Leere Abteilungen fallen
   * weg, „Sonstiges" steht deshalb nur da, wenn wirklich etwas drin ist.
   */
  const byDepartment = $derived(
    DEPARTMENT_ORDER.map((department) => ({
      department,
      items: open.filter((todo) => departmentFor(todo.title) === department),
    })).filter((group) => group.items.length > 0)
  );
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
    return setTodoDone(listId, todo, uid, actorName, !todo.isDone);
  }

  /*
   * Verschieben per Drag & Drop, nur in der Liste der offenen Aufgaben.
   * Erledigte bleiben außen vor: sie stehen ohnehin in einem eigenen Block,
   * und ihre Position spielt für die Anzeige keine Rolle mehr.
   *
   * Gespeichert wird erst beim Loslassen. Während des Ziehens die Positionen
   * zu schreiben würde bei jedem Pixel einen Batch auslösen.
   */
  function onDrop(targetId: string) {
    const sourceId = draggedId;
    draggedId = null;
    if (!sourceId || sourceId === targetId) return;

    const items = [...open];
    const from = items.findIndex((t) => t.id === sourceId);
    const to = items.findIndex((t) => t.id === targetId);
    if (from < 0 || to < 0) return;

    const [moved] = items.splice(from, 1);
    items.splice(to, 0, moved);
    return reorderTodos(listId, items);
  }

  async function resetAll() {
    if (!todos) return;
    const count = todos.items.filter((t) => t.isDone).length;
    if (count === 0) return;
    if (!confirm(`Alle ${count} Haken entfernen?`)) return;
    await resetAllTodos(listId, todos.items);
  }

  async function clearDone() {
    if (!todos) return;
    const count = todos.items.filter((t) => t.isDone).length;
    if (count === 0) return;
    if (!confirm(`${count} erledigte ${count === 1 ? "Zeile" : "Zeilen"} löschen?`)) return;
    await deleteDoneTodos(listId, todos.items);
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

  {#if list && selected && auth.uid}
    {#key selected.id}
      <TodoDetail
        {list}
        todo={selected}
        uid={auth.uid}
        {actorName}
        onClose={() => (selectedId = null)}
      />
    {/key}
  {/if}

  {#if todos?.error}
    <p class="error">{todos.error}</p>
  {:else if todos?.loading}
    <p class="muted">Aufgaben werden geladen …</p>
  {:else if todos && todos.items.length === 0}
    <p class="muted">Diese Liste ist leer.</p>
  {:else if shopping}
    {#each byDepartment as group (group.department)}
      <h2 class="department">{DEPARTMENT_LABELS[group.department]}</h2>
      <ul>
        {#each group.items as todo (todo.id)}
          {@render row(todo, false)}
        {/each}
      </ul>
    {/each}

    {#if done.length > 0}
      <div class="done-header">
        <h2>Erledigt</h2>
        <button class="text-button" onclick={clearDone}>Erledigte löschen</button>
      </div>
      <ul class="done-list">
        {#each done as todo (todo.id)}
          {@render row(todo, false)}
        {/each}
      </ul>
    {/if}
  {:else}
    <ul>
      {#each open as todo (todo.id)}
        <!-- Checklisten bleiben sortierbar: bei einer Packliste ist die
             Reihenfolge der halbe Sinn. Nur im Einkaufsmodus entfällt das,
             dort sortiert die Abteilung. -->
        {@render row(todo, !shopping)}
      {/each}
    </ul>

    {#if done.length > 0}
      <div class="done-header">
        <h2>Erledigt</h2>
        {#if checklist}
          <button class="text-button" onclick={resetAll}>Alle zurücksetzen</button>
        {/if}
      </div>
      <ul class="done-list">
        {#each done as todo (todo.id)}
          {@render row(todo, false)}
        {/each}
      </ul>
    {/if}
  {/if}

  {#if list && auth.uid}
    <ListMembers {list} uid={auth.uid} />
  {/if}
{/if}

{#snippet row(todo: TodoItem, draggable: boolean)}
  <li
    class:done={todo.isDone}
    class:dragging={draggedId === todo.id}
    {draggable}
    ondragstart={() => (draggedId = todo.id)}
    ondragend={() => (draggedId = null)}
    ondragover={(event) => draggable && event.preventDefault()}
    ondrop={(event) => {
      event.preventDefault();
      onDrop(todo.id);
    }}
  >
    <label>
      <input type="checkbox" checked={todo.isDone} onchange={() => toggle(todo)} />
      <span class="title">{todo.title}</span>
    </label>
    {#if shopping && todo.quantity}
      <span class="quantity">{todo.quantity}</span>
    {/if}
    {#if !simple && formatDue(todo)}
      <span class="due">{formatDue(todo)}</span>
    {/if}
    {#if !simple && todo.recurrence}
      <span class="repeat" title="Wiederholt sich">↻</span>
    {/if}
    {#if !simple && todo.priority === "HOCH"}
      <span class="priority">Hoch</span>
    {/if}
    {#if !simple && todo.subtasks.length > 0}
      <span class="subtasks">
        {todo.subtasks.filter((s) => s.isDone).length}/{todo.subtasks.length}
      </span>
    {/if}
    {#if !simple && todo.assignedTo}
      <span class="assignee">{list?.memberNames[todo.assignedTo] ?? "?"}</span>
    {/if}
    <button
      class="text-button"
      onclick={() => (selectedId = todo.id)}
      aria-label={`${todo.title} bearbeiten`}>Details</button
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

  li[draggable="true"] {
    cursor: grab;
  }

  li.dragging {
    opacity: 0.5;
  }

  li.done .title {
    color: var(--on-surface-variant);
    text-decoration: line-through;
  }

  .due,
  .subtasks,
  .assignee,
  .repeat,
  .quantity,
  .priority {
    flex: none;
    font-size: 0.8125rem;
  }

  .due,
  .subtasks,
  .assignee,
  .repeat {
    color: var(--on-surface-variant);
  }

  .quantity {
    background: var(--surface-container-high);
    border-radius: var(--radius-full);
    padding: 0.125rem 0.625rem;
  }

  .department {
    font-size: 0.8125rem;
    font-weight: 500;
    letter-spacing: 0.04em;
    margin: 1.5rem 0 0.5rem;
    text-transform: uppercase;
  }

  .department:first-of-type {
    margin-top: 0;
  }

  .done-header {
    align-items: center;
    display: flex;
    justify-content: space-between;
    margin: 2rem 0 0.75rem;
  }

  .done-header h2 {
    margin: 0;
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
