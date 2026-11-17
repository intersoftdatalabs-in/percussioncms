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
 * Session-scoped stale-assembly registry for template normalization (#6478,
 * parent #2626).
 *
 * <p>A template's assembled output goes stale when its assembly inputs change
 * (source, assembler, JEXL bindings, slot layout/styles). The REST template
 * detail carries no modification timestamp, so the Design SPA tracks the
 * change locally: the template editor marks a template {@code unsaved} while
 * assembly-affecting edits are dirty and {@code saved} once those edits are
 * persisted but items have not been republished yet. The Publish Now panel
 * reads the same registry at publish time and warns before pushing items
 * whose templates changed in this session.</p>
 *
 * <p>Browser {@code localStorage} only (same contract as
 * {@code contentExplorer/recentItems}): entries survive navigation between
 * the Design and Publish shells in one browser session. Clearing an entry
 * means the operator republished or otherwise acknowledged the change — it
 * is not done automatically because the panel cannot map item ids to
 * templates.</p>
 */

/** {@code localStorage} key for the session stale-assembly map. */
export const STALE_ASSEMBLY_STORAGE_KEY = "perc.design.staleAssembly.v1";

/** Cap so one long session cannot grow the stored map without bound. */
export const STALE_ASSEMBLY_MAX_ENTRIES = 50;

/**
 * {@code unsaved}: assembly-affecting edits are dirty in the editor and NOT
 * yet persisted — publishing now assembles from the last saved template.
 * {@code saved}: edits were persisted in this session but items have not
 * been republished — previously published output still shows the old
 * assembly.
 */
export type StaleAssemblyState = "unsaved" | "saved";

export interface StaleAssemblyEntry {
  name: string;
  state: StaleAssemblyState;
}

function isStaleAssemblyState(value: unknown): value is StaleAssemblyState {
  return value === "unsaved" || value === "saved";
}

/** Trim a template name for registry keys. Blank / null becomes "". */
export function normalizeStaleTemplateName(
  name: string | undefined | null,
): string {
  return name == null ? "" : name.trim();
}

function storage(): Storage | null {
  try {
    if (typeof window === "undefined" || window.localStorage == null) {
      return null;
    }
    return window.localStorage;
  } catch {
    return null;
  }
}

function readMap(): Record<string, StaleAssemblyState> {
  const store = storage();
  if (!store) {
    return {};
  }
  let raw: string | null = null;
  try {
    raw = store.getItem(STALE_ASSEMBLY_STORAGE_KEY);
  } catch {
    return {};
  }
  if (!raw) {
    return {};
  }
  try {
    const parsed: unknown = JSON.parse(raw);
    if (parsed == null || typeof parsed !== "object" || Array.isArray(parsed)) {
      return {};
    }
    const out: Record<string, StaleAssemblyState> = {};
    for (const [key, value] of Object.entries(
      parsed as Record<string, unknown>,
    )) {
      const name = normalizeStaleTemplateName(key);
      if (name && isStaleAssemblyState(value)) {
        out[name] = value;
      }
    }
    return out;
  } catch {
    return {};
  }
}

function writeMap(map: Record<string, StaleAssemblyState>): void {
  const store = storage();
  if (!store) {
    return;
  }
  try {
    const keys = Object.keys(map);
    const trimmed: Record<string, StaleAssemblyState> = {};
    for (const key of keys.slice(-STALE_ASSEMBLY_MAX_ENTRIES)) {
      const state = map[key];
      if (isStaleAssemblyState(state)) {
        trimmed[key] = state;
      }
    }
    store.setItem(STALE_ASSEMBLY_STORAGE_KEY, JSON.stringify(trimmed));
  } catch {
    // Storage denied or full: staleness warnings degrade to editor-only.
  }
}

function upsert(name: string, state: StaleAssemblyState): boolean {
  const key = normalizeStaleTemplateName(name);
  if (!key) {
    return false;
  }
  const map = readMap();
  // A save upgrades (never downgrades) an unsaved mark; re-marking saved
  // keeps the original position so the oldest change lists first.
  if (map[key] === "unsaved" && state === "saved") {
    const next: Record<string, StaleAssemblyState> = {};
    for (const existing of Object.keys(map)) {
      next[existing] = existing === key ? state : map[existing];
    }
    writeMap(next);
    return true;
  }
  if (map[key] === state) {
    return true;
  }
  writeMap({ ...map, [key]: state });
  return true;
}

/**
 * Mark assembly-affecting edits dirty (not yet persisted). Returns false
 * when the name is blank.
 */
export function markTemplateAssemblyUnsaved(
  name: string | undefined | null,
): boolean {
  const key = normalizeStaleTemplateName(name);
  if (!key) {
    return false;
  }
  // Never downgrade a saved mark back to unsaved: the persisted change still
  // needs a republish even while further edits are dirty.
  if (readMap()[key] === "saved") {
    return true;
  }
  return upsert(key, "unsaved");
}

/**
 * The template was persisted after an assembly-affecting change; published
 * output stays stale until items are republished. Returns false when the
 * name is blank.
 */
export function markTemplateAssemblySaved(
  name: string | undefined | null,
): boolean {
  const key = normalizeStaleTemplateName(name);
  if (!key) {
    return false;
  }
  return upsert(key, "saved");
}

/** Forget a template (republished / acknowledged). Returns false when blank. */
export function clearTemplateAssemblyStale(
  name: string | undefined | null,
): boolean {
  const key = normalizeStaleTemplateName(name);
  if (!key) {
    return false;
  }
  const map = readMap();
  if (!(key in map)) {
    return true;
  }
  const next: Record<string, StaleAssemblyState> = {};
  for (const existing of Object.keys(map)) {
    if (existing !== key) {
      next[existing] = map[existing];
    }
  }
  writeMap(next);
  return true;
}

/** Test/reset hook: drop every session entry. */
export function clearAllTemplateAssemblyStale(): void {
  const store = storage();
  if (!store) {
    return;
  }
  try {
    store.removeItem(STALE_ASSEMBLY_STORAGE_KEY);
  } catch {
    // Best-effort only.
  }
}

/** Session registry snapshot, oldest change first. */
export function listStaleAssemblyTemplates(): StaleAssemblyEntry[] {
  return Object.entries(readMap()).map(([name, state]) => ({ name, state }));
}

/** True when the template has an unacknowledged session change. */
export function isTemplateAssemblyStale(
  name: string | undefined | null,
): boolean {
  const key = normalizeStaleTemplateName(name);
  if (!key) {
    return false;
  }
  return key in readMap();
}

/** The session state for one template, or null when nothing is pending. */
export function staleAssemblyStateFor(
  name: string | undefined | null,
): StaleAssemblyState | null {
  const key = normalizeStaleTemplateName(name);
  if (!key) {
    return null;
  }
  return readMap()[key] ?? null;
}
