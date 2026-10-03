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
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/**
 * Explorer set-community client (#5077 / parent #4530).
 *
 * <p>{@code PSItemCommunityChoices} is {@code @XmlRootElement(name =
 * "ItemCommunityChoices")}. REST {@code JacksonContextResolver} enables
 * {@code WRAP_ROOT_VALUE}, so callers must unwrap before reading choices.</p>
 */

import { get, post } from "../client";
import { PATHS } from "../paths";

export interface ItemCommunityChoice {
  id: string;
  name: string;
}

export interface ItemCommunityChoices {
  itemId?: string;
  currentCommunityId?: string;
  choices: ItemCommunityChoice[];
}

const CHOICE_ROOTS = ["ItemCommunityChoices", "PSItemCommunityChoices"] as const;

function asRecord(value: unknown): Record<string, unknown> | null {
  if (value != null && typeof value === "object" && !Array.isArray(value)) {
    return value as Record<string, unknown>;
  }
  return null;
}

function asOptionalString(value: unknown): string | undefined {
  if (value == null) {
    return undefined;
  }
  const s = String(value).trim();
  return s.length > 0 ? s : undefined;
}

function coerceChoices(raw: unknown): ItemCommunityChoice[] {
  if (raw == null) {
    return [];
  }
  const list = Array.isArray(raw)
    ? raw
    : (() => {
        const obj = asRecord(raw);
        if (!obj) {
          return [];
        }
        for (const key of [
          "ItemCommunityChoice",
          "PSItemCommunityChoice",
          "choice",
          "choices",
        ]) {
          if (key in obj) {
            const inner = obj[key];
            return Array.isArray(inner) ? inner : [inner];
          }
        }
        return [];
      })();
  const out: ItemCommunityChoice[] = [];
  for (const entry of list) {
    const row = asRecord(entry);
    if (!row) {
      continue;
    }
    const id = asOptionalString(row.id);
    if (!id) {
      continue;
    }
    out.push({ id, name: asOptionalString(row.name) ?? id });
  }
  return out;
}

export function unwrapItemCommunityChoices(data: unknown): ItemCommunityChoices {
  const root = asRecord(data);
  if (!root) {
    return { choices: [] };
  }
  let body = root;
  for (const name of CHOICE_ROOTS) {
    const nested = asRecord(root[name]);
    if (nested) {
      body = nested;
      break;
    }
  }
  return {
    itemId: asOptionalString(body.itemId),
    currentCommunityId: asOptionalString(body.currentCommunityId),
    choices: coerceChoices(body.choices),
  };
}

/** Communities the server will accept for this item, including the current one. */
export async function getItemCommunityChoices(
  itemId: string,
): Promise<ItemCommunityChoices> {
  const id = String(itemId ?? "").trim();
  if (!id) {
    return { choices: [] };
  }
  const data = await get<unknown>(
    `${PATHS.ITEM_COMMUNITY_ALLOWED}${encodeURIComponent(id)}`,
  );
  return unwrapItemCommunityChoices(data);
}

/**
 * Assign a different allowed community. HTTP errors propagate; callers must not
 * treat them as success.
 */
export async function changeItemCommunity(
  itemId: string,
  communityId: string,
): Promise<ItemCommunityChoices> {
  const id = String(itemId ?? "").trim();
  const community = String(communityId ?? "").trim();
  if (!id || !community) {
    throw new Error("changeItemCommunity requires itemId and communityId");
  }
  const data = await post<unknown>(PATHS.itemCommunityChange(id, community), {});
  return unwrapItemCommunityChoices(data);
}
