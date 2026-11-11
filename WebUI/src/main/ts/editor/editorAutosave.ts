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
 * Autosave draft recovery for the React Content Editor host (#6076 /
 * parent #4532).
 *
 * <p>Unsaved text edits are mirrored to {@code localStorage} while the form
 * is dirty in Edit mode. After a reload the host offers the stored draft
 * back with restore/discard actions. Restoring only fills the local draft —
 * it never issues a fields PUT, so it can never clobber a newer server save;
 * only an explicit Save writes. A successful save (or a clean form) clears
 * the stored entry.</p>
 *
 * <p>Staged binaries ({@code File} picks) are not serializable and are
 * intentionally excluded: only the text draft map is persisted.</p>
 */

export const AUTOSAVE_STORAGE_KEY_PREFIX = "perc-editor-autosave";

/** Stored payload version; a mismatch is treated as absent. */
export const AUTOSAVE_DRAFT_VERSION = 1;

/** Minimal storage seam so tests can pass a fake (defaults to localStorage). */
export interface AutosaveStorage {
  getItem(key: string): string | null;
  setItem(key: string, value: string): void;
  removeItem(key: string): void;
}

export interface AutosavedEditorDraft {
  version: number;
  contentId: string;
  /** ISO timestamp of the last autosave write. */
  savedAt: string;
  /** Item revision visible when the draft was stored (may be absent). */
  revision?: number | string | null;
  draft: Record<string, string>;
}

/** Storage key for one open item. Content ids are numeric (see editorHostUrl). */
export function editorAutosaveKey(contentId: string): string {
  return `${AUTOSAVE_STORAGE_KEY_PREFIX}-${contentId}`;
}

function isRecord(value: unknown): value is Record<string, unknown> {
  return typeof value === "object" && value !== null;
}

/**
 * Read the stored draft for {@code contentId}. Corrupt JSON, a version
 * mismatch, or a foreign content id reads as absent — never throws.
 */
export function readAutosavedEditorDraft(
  storage: AutosaveStorage | null | undefined,
  contentId: string,
): AutosavedEditorDraft | null {
  if (!storage) {
    return null;
  }
  let raw: string | null = null;
  try {
    raw = storage.getItem(editorAutosaveKey(contentId));
  } catch {
    return null;
  }
  if (!raw) {
    return null;
  }
  try {
    const parsed: unknown = JSON.parse(raw);
    if (!isRecord(parsed)) {
      return null;
    }
    if (parsed["version"] !== AUTOSAVE_DRAFT_VERSION) {
      return null;
    }
    if (parsed["contentId"] !== contentId) {
      return null;
    }
    if (!isRecord(parsed["draft"])) {
      return null;
    }
    const draft: Record<string, string> = {};
    for (const [name, value] of Object.entries(parsed["draft"])) {
      draft[name] = value == null ? "" : String(value);
    }
    const revision = parsed["revision"];
    return {
      version: AUTOSAVE_DRAFT_VERSION,
      contentId,
      savedAt:
        typeof parsed["savedAt"] === "string" ? parsed["savedAt"] : "",
      revision:
        typeof revision === "number" || typeof revision === "string"
          ? revision
          : null,
      draft,
    };
  } catch {
    return null;
  }
}

/**
 * Persist the text draft for {@code contentId}. Storage failures (quota,
 * private mode) are swallowed: autosave is best-effort and must never
 * break editing.
 */
export function writeAutosavedEditorDraft(
  storage: AutosaveStorage | null | undefined,
  contentId: string,
  draft: Record<string, string>,
  revision?: number | string | null,
): void {
  if (!storage) {
    return;
  }
  const payload: AutosavedEditorDraft = {
    version: AUTOSAVE_DRAFT_VERSION,
    contentId,
    savedAt: new Date().toISOString(),
    revision: revision ?? null,
    draft: { ...draft },
  };
  try {
    storage.setItem(editorAutosaveKey(contentId), JSON.stringify(payload));
  } catch {
    // Best-effort only.
  }
}

/** Drop the stored draft for {@code contentId}; never throws. */
export function clearAutosavedEditorDraft(
  storage: AutosaveStorage | null | undefined,
  contentId: string,
): void {
  if (!storage) {
    return;
  }
  try {
    storage.removeItem(editorAutosaveKey(contentId));
  } catch {
    // Best-effort only.
  }
}

function fieldAsString(value: unknown): string {
  return value == null ? "" : String(value);
}

/**
 * True when the stored draft differs from the freshly loaded field values
 * on at least one loaded field. Read-only rows are the caller's concern
 * (they never enter the editable draft).
 */
export function autosavedDraftDiffers(
  stored: AutosavedEditorDraft | null,
  loadedFields: ReadonlyArray<{ name: string; value?: unknown }> | undefined,
): boolean {
  if (!stored || !loadedFields) {
    return false;
  }
  for (const field of loadedFields) {
    const current = Object.prototype.hasOwnProperty.call(stored.draft, field.name)
      ? stored.draft[field.name]
      : fieldAsString(field.value);
    if (current !== fieldAsString(field.value)) {
      return true;
    }
  }
  return false;
}

/**
 * Offer the stale-draft recovery notice when a stored draft exists and
 * differs from the loaded values. Restore stays client-side (no PUT), so a
 * newer server revision is never clobbered by offering the banner.
 */
export function shouldOfferAutosaveRestore(
  stored: AutosavedEditorDraft | null,
  loadedFields: ReadonlyArray<{ name: string; value?: unknown }> | undefined,
): boolean {
  return stored != null && autosavedDraftDiffers(stored, loadedFields);
}

/**
 * Guarded browser storage for the host. Null outside a DOM (SSR/tests
 * without jsdom), matching the guards used by the dashboard gadgets.
 */
export function browserAutosaveStorage(): AutosaveStorage | null {
  if (typeof localStorage === "undefined") {
    return null;
  }
  return localStorage;
}
