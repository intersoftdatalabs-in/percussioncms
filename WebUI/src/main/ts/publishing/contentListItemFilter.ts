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

import type { ItemFilter } from "../api/developer/types";

/** Shown when a content list has no item filter. Not a catalog name. */
export const NO_ITEM_FILTER_LABEL = "No item filter";

export interface ItemFilterChoice {
  id: string;
  name: string;
}

/**
 * Design saves address an item filter by name. Several filters share the same
 * numeric uuid on different hosts, so a bare uuid is not a stable choice id.
 */
export function itemFilterChoiceId(filter: ItemFilter): string | undefined {
  const name = filter.name?.trim();
  return name || undefined;
}

/**
 * Token the editor select and save body use. Prefer the loaded display name so
 * a summary that still carries a colliding uuid selects the matching name.
 */
export function storedFilterToken(
  contentList: { itemFilterName?: string; itemFilterId?: string } | null | undefined,
): string {
  const name = contentList?.itemFilterName?.trim();
  if (name) {
    return name;
  }
  return contentList?.itemFilterId?.trim() ?? "";
}

/** Existing filters the editor can choose. Skips rows with no name or no id. */
export function itemFilterChoices(filters: ItemFilter[]): ItemFilterChoice[] {
  const seen = new Set<string>();
  const choices: ItemFilterChoice[] = [];
  for (const filter of filters) {
    const name = filter.name?.trim();
    const id = itemFilterChoiceId(filter);
    if (!name || !id || seen.has(id)) {
      continue;
    }
    seen.add(id);
    choices.push({ id, name });
  }
  choices.sort((a, b) => a.name.localeCompare(b.name));
  return choices;
}

/**
 * Stored filter text. Uses the last loaded summary, never the draft select.
 * A name wins; otherwise the id; otherwise {@link NO_ITEM_FILTER_LABEL}.
 */
export function storedItemFilterLabel(
  contentList: { itemFilterName?: string; itemFilterId?: string } | null | undefined,
): string {
  const name = contentList?.itemFilterName?.trim();
  if (name) {
    return name;
  }
  const id = contentList?.itemFilterId?.trim();
  if (id) {
    return id;
  }
  return NO_ITEM_FILTER_LABEL;
}
