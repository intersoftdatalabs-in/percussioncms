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

import type { CommunityNewSearchRef, SearchDef } from "../api/developer/types";

/** Alternate identity keys so GET id-only refs match catalog name+id rows. */
export function searchRefKeys(ref: CommunityNewSearchRef | SearchDef): string[] {
  const keys: string[] = [];
  const name = ref.name?.trim();
  if (name) {
    keys.push(`name:${name.toLowerCase()}`);
  }
  const guid = ref.guid?.stringValue?.trim();
  if (guid) {
    keys.push(`guid:${guid}`);
  }
  if (ref.id != null && ref.id !== 0) {
    keys.push(`id:${ref.id}`);
  }
  return keys;
}

/** Preferred identity for dirty-set compare and checkbox test ids. */
export function searchRefPrimaryKey(ref: CommunityNewSearchRef | SearchDef): string {
  return searchRefKeys(ref)[0] || "";
}

export function collectSearchRefKeys(
  refs: readonly (CommunityNewSearchRef | SearchDef)[],
): Set<string> {
  const out = new Set<string>();
  for (const ref of refs) {
    for (const k of searchRefKeys(ref)) {
      out.add(k);
    }
  }
  return out;
}

export function searchRefIsSelected(
  ref: CommunityNewSearchRef | SearchDef,
  selectedKeys: Set<string>,
): boolean {
  return searchRefKeys(ref).some((k) => selectedKeys.has(k));
}

export function sameSearchKeySet(a: Set<string>, b: Set<string>): boolean {
  if (a.size !== b.size) {
    return false;
  }
  for (const k of a) {
    if (!b.has(k)) {
      return false;
    }
  }
  return true;
}

/**
 * Catalog rows plus assigned refs that are not already in the catalog
 * (so an unknown-to-catalog default can still be unchecked).
 */
export function mergeSearchPickerRows(
  catalog: readonly SearchDef[],
  assigned: readonly CommunityNewSearchRef[],
): CommunityNewSearchRef[] {
  const rows: CommunityNewSearchRef[] = [];
  const seen = new Set<string>();
  function add(ref: CommunityNewSearchRef): void {
    const keys = searchRefKeys(ref);
    if (keys.length === 0) {
      return;
    }
    if (keys.some((k) => seen.has(k))) {
      return;
    }
    for (const k of keys) {
      seen.add(k);
    }
    rows.push(ref);
  }
  for (const s of catalog) {
    add({
      name: s.name,
      id: s.id,
      guid: s.guid,
      label: s.label,
    });
  }
  for (const a of assigned) {
    add(a);
  }
  return rows;
}

/** Primary keys of picker rows that currently match {@code selectedKeys}. */
export function selectedPickerPrimaryKeys(
  rows: readonly CommunityNewSearchRef[],
  selectedKeys: Set<string>,
): Set<string> {
  const out = new Set<string>();
  for (const row of rows) {
    if (!searchRefIsSelected(row, selectedKeys)) {
      continue;
    }
    const pk = searchRefPrimaryKey(row);
    if (pk) {
      out.add(pk);
    }
  }
  return out;
}

/** PUT identity list (name preferred). Empty array clears defaults. */
export function toNewSearchWriteRefs(
  rows: readonly CommunityNewSearchRef[],
  selectedKeys: Set<string>,
): CommunityNewSearchRef[] {
  const out: CommunityNewSearchRef[] = [];
  for (const row of rows) {
    if (!searchRefIsSelected(row, selectedKeys)) {
      continue;
    }
    const ref: CommunityNewSearchRef = {};
    const name = row.name?.trim();
    if (name) {
      ref.name = name;
    }
    if (row.id != null && row.id !== 0) {
      ref.id = row.id;
    }
    if (row.guid?.stringValue) {
      ref.guid = row.guid;
    }
    if (ref.name || ref.id != null || ref.guid) {
      out.push(ref);
    }
  }
  return out;
}

export function toggleSearchRefSelection(
  ref: CommunityNewSearchRef | SearchDef,
  prev: Set<string>,
): Set<string> {
  const keys = searchRefKeys(ref);
  const next = new Set(prev);
  const on = keys.some((k) => next.has(k));
  for (const k of keys) {
    if (on) {
      next.delete(k);
    } else {
      next.add(k);
    }
  }
  return next;
}
