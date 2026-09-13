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
  import {
    isSimpleMode,
    type Priority,
    type Recurrence,
    type RecurrenceUnit,
    type TodoItem,
    type TodoList,
  } from "./types";

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

  const units: { value: RecurrenceUnit; label: string }[] = [
    { value: "TAG", label: "Tage" },
    { value: "WOCHE", label: "Wochen" },
    { value: "MONAT", label: "Monate" },
    { value: "JAHR", label: "Jahre" },
  ];

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
  let repeats = $state(initial.recurrence !== null);
  let unit = $state<RecurrenceUnit>(initial.recurrence?.unit ?? "WOCHE");
  let interval = $state(initial.recurrence?.interval ?? 1);
  let anchor = $state(initial.recurrence?.anchor ?? "FAELLIG");
  let rotate = $state(initial.rotateAmong.length > 0);

  let quantity = $state(initial.quantity);
  // Als Text, nicht als Zahl: ein leeres Feld ist „kein Preis" und nicht 0,
  // und beim Tippen von „12," darf der Wert nicht zwischendurch verschwinden.
  let priceInput = $state(initial.price === null ? "" : String(initial.price));
  let link = $state(initial.link);

  let newSubtask = $state("");
  let newComment = $state("");
  let saving = $state(false);
  let error = $state<string | null>(null);

  /**
   * Im Einkaufsmodus bleiben Priorität, Zuständigkeit, Fälligkeit, Erinnerung
   * und Wiederholung aus der Ansicht. Bereits gesetzte Werte werden dabei
   * nicht gelöscht – sie stehen weiter im Dokument, falls die Liste wieder
   * auf Aufgaben umgestellt wird.
   */
  const shopping = $derived(list.mode === "EINKAUFEN");
  /**
   * Einkaufen und Checkliste kommen ohne Priorität, Zuständigkeit, Termine und
   * Wiederholung aus. Die Mengenangabe gibt es nur beim Einkaufen.
   */
  const simple = $derived(isSimpleMode(list.mode));
  /** Anschaffungen: Preis und Link statt Menge, Priorität bleibt sinnvoll. */
  const purchase = $derived(list.mode === "ANSCHAFFUNG");

  const members = $derived(
    list.memberIds.map((id) => ({ id, name: list.memberNames[id] ?? "Unbekannt" }))
  );

  /**
   * „12,50" und „12.50" sollen beide funktionieren; alles Unbrauchbare wird zu
   * null, also „kein Preis". Negative Beträge ergeben hier keinen Sinn.
   */
  function parsePrice(raw: string): number | null {
    const value = Number(raw.replace(",", ".").trim());
    if (!raw.trim() || Number.isNaN(value) || value < 0) return null;
    return Math.round(value * 100) / 100;
  }

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
        recurrence: repeats
          ? ({ unit, interval: Math.max(1, Math.round(interval)), anchor } satisfies Recurrence)
          : null,
        // Die Runde sind die aktuellen Mitglieder. Wer später dazukommt oder
        // geht, ändert daran nichts – die Cloud Function fängt bei einer
        // unbekannten Zuständigkeit wieder vorn an.
        rotateAmong: repeats && rotate ? list.memberIds : [],
        quantity: quantity.trim(),
        price: parsePrice(priceInput),
        link: link.trim(),
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

  {#if shopping}
    <label class="field">
      <span>Menge</span>
      <input class="text-field" bind:value={quantity} placeholder="z. B. 2 kg" />
    </label>
  {/if}

  {#if purchase}
    <div class="row">
      <label class="field">
        <span>Preis</span>
        <input class="text-field" bind:value={priceInput} inputmode="decimal" placeholder="z. B. 649" />
      </label>
      <label class="field">
        <span>Link</span>
        <input class="text-field" bind:value={link} type="url" placeholder="https://…" />
      </label>
    </div>
  {/if}

  <label class="field">
    <span>Beschreibung</span>
    <textarea class="text-field" rows="3" bind:value={description}></textarea>
  </label>

  {#if !simple}
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
    <h3>Wiederholung</h3>
    <label class="check">
      <input type="checkbox" bind:checked={repeats} />
      <span>Aufgabe wiederholt sich</span>
    </label>

    {#if repeats}
      <div class="row">
        <label class="field">
          <span>Alle</span>
          <input class="text-field" type="number" min="1" max="99" bind:value={interval} />
        </label>
        <label class="field">
          <span>Einheit</span>
          <select class="text-field" bind:value={unit}>
            {#each units as option (option.value)}
              <option value={option.value}>{option.label}</option>
            {/each}
          </select>
        </label>
      </div>

      <label class="field">
        <span>Nächster Termin</span>
        <select class="text-field" bind:value={anchor}>
          <option value="FAELLIG">nach dem geplanten Termin</option>
          <option value="ERLEDIGT">nach dem Abhaken</option>
        </select>
      </label>

      {#if list.memberIds.length > 1}
        <label class="check">
          <input type="checkbox" bind:checked={rotate} />
          <span>Zuständigkeit reihum wechseln</span>
        </label>
      {/if}

      {#if !dueInput && anchor === "FAELLIG"}
        <p class="hint">
          Ohne Fälligkeit wird ab dem Abhaken gerechnet.
        </p>
      {/if}
    {/if}
  </section>
  {/if}

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

  .check {
    align-items: center;
    display: flex;
    gap: 0.5rem;
  }

  .hint {
    color: var(--on-surface-variant);
    font-size: 0.8125rem;
    margin: 0.5rem 0 0;
  }

  .error {
    color: var(--error);
    margin: 0;
  }
</style>
