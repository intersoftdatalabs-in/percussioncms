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
 * ADR-003 slot_layout / slot_styles helpers for Design SPA (#2810).
 * Schema mirrors PSSlotLayoutStyles (schemaVersion = 1).
 */

export const SLOT_SCHEMA_VERSION = 1;

export const LAYOUT_KEYS = {
  schemaVersion: "schemaVersion",
  orientation: "orientation",
  columns: "columns",
  maxItems: "maxItems",
  emptyState: "emptyState",
  wrapperClassPolicy: "wrapperClassPolicy",
} as const;

export const STYLE_KEYS = {
  schemaVersion: "schemaVersion",
  rootclass: "rootclass",
  itemclass: "itemclass",
} as const;

export type SlotLayoutDraft = {
  orientation: string;
  columns: string;
  maxItems: string;
  emptyState: string;
  wrapperClassPolicy: string;
};

export type SlotStylesDraft = {
  rootclass: string;
  itemclass: string;
};

export function emptyLayoutDraft(): SlotLayoutDraft {
  return {
    orientation: "",
    columns: "",
    maxItems: "",
    emptyState: "",
    wrapperClassPolicy: "",
  };
}

export function emptyStylesDraft(): SlotStylesDraft {
  return { rootclass: "", itemclass: "" };
}

function mapStr(map: Record<string, unknown> | undefined, key: string): string {
  if (!map || map[key] == null) return "";
  return String(map[key]);
}

export function layoutDraftFromMap(
  map: Record<string, unknown> | undefined | null,
): SlotLayoutDraft {
  const m = map || undefined;
  return {
    orientation: mapStr(m, LAYOUT_KEYS.orientation),
    columns: mapStr(m, LAYOUT_KEYS.columns),
    maxItems: mapStr(m, LAYOUT_KEYS.maxItems),
    emptyState: mapStr(m, LAYOUT_KEYS.emptyState),
    wrapperClassPolicy: mapStr(m, LAYOUT_KEYS.wrapperClassPolicy),
  };
}

export function stylesDraftFromMap(
  map: Record<string, unknown> | undefined | null,
): SlotStylesDraft {
  const m = map || undefined;
  return {
    rootclass: mapStr(m, STYLE_KEYS.rootclass),
    itemclass: mapStr(m, STYLE_KEYS.itemclass),
  };
}

function putIfPresent(
  out: Record<string, unknown>,
  key: string,
  value: string,
): void {
  const t = value.trim();
  if (t.length > 0) out[key] = t;
}

/**
 * Build a slot_layout map for PUT. Always stamps schemaVersion.
 * Empty structural fields yield schema-only map (server clears to defaults).
 */
export function layoutMapFromDraft(draft: SlotLayoutDraft): Record<string, unknown> {
  const out: Record<string, unknown> = {
    [LAYOUT_KEYS.schemaVersion]: SLOT_SCHEMA_VERSION,
  };
  putIfPresent(out, LAYOUT_KEYS.orientation, draft.orientation);
  putIfPresent(out, LAYOUT_KEYS.columns, draft.columns);
  putIfPresent(out, LAYOUT_KEYS.maxItems, draft.maxItems);
  putIfPresent(out, LAYOUT_KEYS.emptyState, draft.emptyState);
  putIfPresent(out, LAYOUT_KEYS.wrapperClassPolicy, draft.wrapperClassPolicy);
  return out;
}

export function stylesMapFromDraft(draft: SlotStylesDraft): Record<string, unknown> {
  const out: Record<string, unknown> = {
    [STYLE_KEYS.schemaVersion]: SLOT_SCHEMA_VERSION,
  };
  putIfPresent(out, STYLE_KEYS.rootclass, draft.rootclass);
  putIfPresent(out, STYLE_KEYS.itemclass, draft.itemclass);
  return out;
}

export function layoutDraftsEqual(a: SlotLayoutDraft, b: SlotLayoutDraft): boolean {
  return (
    a.orientation === b.orientation &&
    a.columns === b.columns &&
    a.maxItems === b.maxItems &&
    a.emptyState === b.emptyState &&
    a.wrapperClassPolicy === b.wrapperClassPolicy
  );
}

export function stylesDraftsEqual(a: SlotStylesDraft, b: SlotStylesDraft): boolean {
  return a.rootclass === b.rootclass && a.itemclass === b.itemclass;
}

/** Stable key for a template slot summary (name preferred, else guid). */
export function templateSlotKey(slot: {
  name?: string;
  guid?: { stringValue?: string; uuid?: number };
}): string | null {
  if (slot.name && slot.name.trim()) return slot.name.trim();
  if (slot.guid?.stringValue) return slot.guid.stringValue;
  if (slot.guid?.uuid != null) return String(slot.guid.uuid);
  return null;
}
