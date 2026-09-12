<script lang="ts">
  import { untrack } from "svelte";
  import { Timestamp } from "firebase/firestore";
  import {
    addComment,
    addSubtask,
    deleteTodo,
    removeSubtask,
    setSubtaskDone,
    updateTodo,
    type TodoEdit,
  } from "./lists.svelte";
  import type { Priority, TodoItem, TodoList } from "./types";

  interface Props {
    list: TodoList;
    todo: TodoItem;
    uid: string;
    actorName: string;
    onClose: () => void;
  }

  let { list, todo, uid, actorName, onClose }: Props = $props();

  /**
   * Gleiche Vorlaufzeiten wie in der App (reminder_* in strings.xml). Andere
   * Werte kann der Erinnerungs-Job zwar verarbeiten, aber dann sähen App und
   * Web für dieselbe Aufgabe unterschiedlich aus.
   */
  const reminderOptions: { value: number | null; label: string }[] = [
    { value: null, label: "Keine" },
    { value: 10, label: "10 Min vorher" },
    { value: 30, label: "30 Min vorher" },
    { value: 60, label: "1 Stunde vorher" },
    { value: 1440, label: "1 Tag vorher" },
  ];

  const priorities: Priority[] = ["NIEDRIG", "MITTEL", "HOCH"];

  /** Timestamp -> Wert für <input type="datetime-local"> in lokaler Zeit. */
  function toInputValue(ts: Timestamp | null): string {
    if (!ts) return "";
    const d = ts.toDate();
    const pad = (n: number) => String(n).padStart(2, "0");
    return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}T${pad(d.getHours())}:${pad(d.getMinutes())}`;
  }

  /*
   * Formularzustand, einmalig aus der Aufgabe vorbelegt. Die Werte absichtlich
   * über diesen Schnappschuss: `todo` kommt aus einem Live-Listener und würde
   * sonst bei jeder fremden Änderung die Eingaben überschreiben, während man
   * noch tippt. Beim Wechsel auf eine andere Aufgabe baut das key={todo.id}
   * an der Einbindung die Komponente neu auf.
   */
  const initial = untrack(() => ({ ...todo, dueInput: toInputValue(todo.dueDate) }));

  let title = $state(initial.title);
  let description = $state(initial.description);
  let priority = $state<Priority>(initial.priority);
  let dueInput = $state(initial.dueInput);
  let reminderMinutes = $state<number | null>(initial.reminderMinutes);
  let assignedTo = $state<string>(initial.assignedTo ?? "");
  let newSubtask = $state("");
  let newComment = $state("");
  let saving = $state(false);
  let error = $state<string | null>(null);

  const members = $derived(
    list.memberIds.map((id) => ({ id, name: list.memberNames[id] ?? "Unbekannt" }))
  );

  function nameOf(id: string): string {
    return list.memberNames[id] ?? "Unbekannt";
  }

  async function save() {
    if (!title.trim() || saving) return;
    saving = true;
    error = null;
    try {
      const edit: TodoEdit = {
        title: title.trim(),
        description: description.trim(),
        priority,
        dueDate: dueInput ? Timestamp.fromDate(new Date(dueInput)) : null,
        assignedTo: assignedTo || null,
        // Ohne Fälligkeit ergibt eine Vorlaufzeit keinen Sinn.
        reminderMinutes: dueInput ? reminderMinutes : null,
      };
      await updateTodo(list.id, todo, edit, uid, actorName);
      onClose();
    } catch (e) {
      error = "Speichern fehlgeschlagen.";
      console.error("Aufgabe speichern", e);
      saving = false;
    }
  }

  async function submitSubtask(event: SubmitEvent) {
    event.preventDefault();
    const value = newSubtask.trim();
    if (!value) return;
    newSubtask = "";
    await addSubtask(list.id, todo, value);
  }

  async function submitComment(event: SubmitEvent) {
    event.preventDefault();
    const value = newComment.trim();
    if (!value) return;
    newComment = "";
    await addComment(list.id, todo, value, uid, actorName);
  }

  async function remove() {
    await deleteTodo(list.id, todo.id);
    onClose();
  }
</script>

<div class="panel">
  <header>
    <h2>Aufgabe</h2>
    <button class="text-button" onclick={onClose}>Schließen</button>
  </header>

  <label class="field">
    <span>Titel</span>
    <input class="text-field" bind:value={title} />
  </label>

  <label class="field">
    <span>Beschreibung</span>
    <textarea class="text-field" rows="3" bind:value={description}></textarea>
  </label>

  <div class="row">
    <label class="field">
      <span>Priorität</span>
      <select class="text-field" bind:value={priority}>
        {#each priorities as value (value)}
          <option {value}>{value.charAt(0) + value.slice(1).toLowerCase()}</option>
        {/each}
      </select>
    </label>

    <label class="field">
      <span>Zuständig</span>
      <select class="text-field" bind:value={assignedTo}>
        <option value="">Niemand</option>
        {#each members as member (member.id)}
          <option value={member.id}>{member.name}</option>
        {/each}
      </select>
    </label>
  </div>

  <div class="row">
    <label class="field">
      <span>Fällig am</span>
      <input class="text-field" type="datetime-local" bind:value={dueInput} />
    </label>

    <label class="field">
      <span>Erinnerung</span>
      <select class="text-field" bind:value={reminderMinutes} disabled={!dueInput}>
        {#each reminderOptions as option (option.label)}
          <option value={option.value}>{option.label}</option>
        {/each}
      </select>
    </label>
  </div>

  <section>
    <h3>Unteraufgaben</h3>
    {#if todo.subtasks.length > 0}
      <ul>
        {#each todo.subtasks as subtask (subtask.id)}
          <li>
            <label>
              <input
                type="checkbox"
                checked={subtask.isDone}
                onchange={() => setSubtaskDone(list.id, todo, subtask.id, !subtask.isDone)}
              />
              <span class:done={subtask.isDone}>{subtask.title}</span>
            </label>
            <button
              class="text-button"
              onclick={() => removeSubtask(list.id, todo, subtask)}
              aria-label={`${subtask.title} entfernen`}>Entfernen</button
            >
          </li>
        {/each}
      </ul>
    {/if}
    <form onsubmit={submitSubtask}>
      <input class="text-field" bind:value={newSubtask} placeholder="Neue Unteraufgabe" />
      <button class="text-button" disabled={!newSubtask.trim()}>Hinzufügen</button>
    </form>
  </section>

  <section>
    <h3>Kommentare</h3>
    {#if todo.comments.length > 0}
      <ul class="comments">
        {#each todo.comments as comment (comment.id)}
          <li>
            <span class="author">{nameOf(comment.authorId)}</span>
            <span>{comment.text}</span>
          </li>
        {/each}
      </ul>
    {/if}
    <form onsubmit={submitComment}>
      <input class="text-field" bind:value={newComment} placeholder="Kommentar schreiben" />
      <button class="text-button" disabled={!newComment.trim()}>Senden</button>
    </form>
  </section>

  {#if error}
    <p class="error">{error}</p>
  {/if}

  <footer>
    <button class="text-button danger" onclick={remove}>Löschen</button>
    <button class="filled-button" onclick={save} disabled={!title.trim() || saving}>
      Speichern
    </button>
  </footer>
</div>

<style>
  .panel {
    background: var(--surface-container);
    border-radius: var(--radius-l);
    display: flex;
    flex-direction: column;
    gap: 1rem;
    margin: 1.5rem 0;
    padding: 1.25rem;
  }

  header,
  footer {
    align-items: center;
    display: flex;
    justify-content: space-between;
  }

  h2 {
    font-size: 1.125rem;
  }

  h3 {
    font-size: 0.9375rem;
    margin-bottom: 0.5rem;
  }

  .field {
    display: flex;
    flex: 1;
    flex-direction: column;
    gap: 0.375rem;
    min-width: 0;
  }

  .field > span {
    color: var(--on-surface-variant);
    font-size: 0.8125rem;
  }

  .row {
    display: flex;
    gap: 0.75rem;
  }

  @media (max-width: 30rem) {
    .row {
      flex-direction: column;
    }
  }

  ul {
    display: flex;
    flex-direction: column;
    gap: 0.25rem;
    list-style: none;
    margin: 0 0 0.75rem;
    padding: 0;
  }

  li {
    align-items: center;
    display: flex;
    gap: 0.5rem;
    justify-content: space-between;
  }

  li label {
    align-items: center;
    display: flex;
    gap: 0.5rem;
  }

  input[type="checkbox"] {
    accent-color: var(--primary);
  }

  .done {
    color: var(--on-surface-variant);
    text-decoration: line-through;
  }

  .comments li {
    align-items: baseline;
    display: flex;
    gap: 0.5rem;
    justify-content: flex-start;
  }

  .author {
    color: var(--on-surface-variant);
    flex: none;
    font-size: 0.8125rem;
  }

  form {
    display: flex;
    gap: 0.5rem;
  }

  .danger {
    color: var(--error);
  }

  .error {
    color: var(--error);
    margin: 0;
  }
</style>
