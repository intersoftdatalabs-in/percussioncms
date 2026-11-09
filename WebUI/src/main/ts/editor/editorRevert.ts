/*
 * Copyright (c) 2026 Intersoft Data Labs, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/**
 * Bulk revert of unsaved field edits in the React Content Editor host
 * (#5889 / parent #4532).
 *
 * <p>Revert-all is client-side only: it rebuilds the editable draft from the
 * last loaded field map and drops staged binaries, so it never issues a
 * fields PUT/POST. Per-field dirty tracking names the fields whose draft
 * differs from the loaded values (plus staged binaries) for the row
 * markers and the revert control visibility.</p>
 */

import type { EditorHostMode } from "./editorHostUrl";

function fieldAsString(value: unknown): string {
  return value == null ? "" : String(value);
}

/**
 * Rebuild the editable draft from the last loaded field map. Every loaded
 * field gets an entry so controlled inputs return to the stored values.
 */
export function revertedEditorDraft(
  saved: ReadonlyArray<{ name: string; value?: unknown }> | undefined,
): Record<string, string> {
  if (!saved) {
    return {};
  }
  return Object.fromEntries(
    saved.map((field) => [field.name, fieldAsString(field.value)]),
  );
}

/**
 * Names of loaded fields whose draft differs, in loaded order. Schema
 * read-only names are ignored: a client edit of those fields is not an
 * unsaved change.
 */
function loadedDirtyFieldNames(
  saved: ReadonlyArray<{ name: string; value?: unknown }>,
  draft: Record<string, string>,
  /** Schema read-only names. A client edit of those fields is not an unsaved change. */
  ignoredNames?: ReadonlySet<string>,
): string[] {
  const dirty: string[] = [];
  for (const field of saved) {
    if (ignoredNames?.has(field.name) || dirty.includes(field.name)) {
      continue;
    }
    const current = Object.prototype.hasOwnProperty.call(draft, field.name)
      ? draft[field.name]
      : fieldAsString(field.value);
    if (current !== fieldAsString(field.value)) {
      dirty.push(field.name);
    }
  }
  return dirty;
}

/** Append staged-binary names that are not already listed. */
function withStagedNames(
  dirty: string[],
  staged: Record<string, unknown>,
): string[] {
  const names = [...dirty];
  for (const name of Object.keys(staged)) {
    if (!names.includes(name)) {
      names.push(name);
    }
  }
  return names;
}

/**
 * Names of fields with unsaved changes, in loaded-field order with staged
 * binaries appended. Schema read-only names are ignored: a client edit of
 * those fields is not an unsaved change.
 */
export function editorDirtyFieldNames(
  saved: ReadonlyArray<{ name: string; value?: unknown }> | undefined,
  draft: Record<string, string>,
  pendingFiles: Record<string, File>,
  pendingClears?: Record<string, boolean>,
  /** Schema read-only names. A client edit of those fields is not an unsaved change. */
  ignoredNames?: ReadonlySet<string>,
): string[] {
  const dirty = saved ? loadedDirtyFieldNames(saved, draft, ignoredNames) : [];
  return withStagedNames(
    withStagedNames(dirty, pendingFiles),
    pendingClears ?? {},
  );
}

/** Revert control visibility: edit mode with unsaved changes. */
export function canRevertFromEditor(
  mode: EditorHostMode,
  dirty: boolean,
): boolean {
  return mode === "edit" && dirty;
}
