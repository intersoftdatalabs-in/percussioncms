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
 * Content → Set community for one selected page or asset (#5077 / parent #4530).
 *
 * <p>Cancel, folders, multi-select, and HTTP 400/403/409 must not be reported
 * as a saved community. This is not the login community switch and not folder
 * ACL community.</p>
 */

import { isApiError } from "../api/client";
import {
  changeItemCommunity,
  getItemCommunityChoices,
  type ItemCommunityChoice,
} from "../api/contentExplorer/itemCommunityApi";
import { resolvePublishKind } from "./itemPublish";
import { isFolder } from "./selection";
import type { PSPathItem } from "../api/contentExplorer/types";

export type SetCommunityBlockReason =
  | "empty"
  | "folder"
  | "multi"
  | "not-item"
  | "no-id";

export type SetCommunitySelection =
  | { status: "blocked"; reason: SetCommunityBlockReason; name: string }
  | { status: "ready"; itemId: string; name: string };

export function classifySetCommunitySelection(input: {
  item: PSPathItem | null | undefined;
  selectedCount: number;
}): SetCommunitySelection {
  if (input.selectedCount >= 2) {
    return { status: "blocked", reason: "multi", name: "" };
  }
  const item = input.item ?? null;
  if (!item) {
    return { status: "blocked", reason: "empty", name: "" };
  }
  const name = (item.name ?? item.path ?? "").trim();
  if (isFolder(item)) {
    return { status: "blocked", reason: "folder", name };
  }
  if (resolvePublishKind(item) === "none") {
    return { status: "blocked", reason: "not-item", name };
  }
  const itemId = item.id == null ? "" : String(item.id).trim();
  if (!itemId) {
    return { status: "blocked", reason: "no-id", name };
  }
  return { status: "ready", itemId, name };
}

export type CommunityChoiceGate =
  | { ok: true; communityId: string }
  | { ok: false; reason: "blank" | "unchanged" | "forbidden" };

/** Client gate before POST change. The server repeats the check. */
export function gateExplorerCommunityChange(input: {
  selectedId: string;
  currentId: string;
  allowedIds: readonly string[];
}): CommunityChoiceGate {
  const communityId = String(input.selectedId ?? "").trim();
  if (!communityId) {
    return { ok: false, reason: "blank" };
  }
  const current = String(input.currentId ?? "").trim();
  if (current && communityId === current) {
    return { ok: false, reason: "unchanged" };
  }
  const allowed = input.allowedIds
    .map((id) => String(id ?? "").trim())
    .filter((id) => id.length > 0);
  if (!allowed.includes(communityId)) {
    return { ok: false, reason: "forbidden" };
  }
  return { ok: true, communityId };
}

export type SetCommunityCatalog =
  | {
      status: "ready";
      itemId: string;
      currentId: string;
      choices: ItemCommunityChoice[];
    }
  | { status: "blocked"; reason: SetCommunityBlockReason; name: string }
  | { status: "http"; http: 400 | 403 | 409 | "other" }
  | { status: "none" };

export async function loadSetCommunityCatalog(input: {
  item: PSPathItem | null | undefined;
  selectedCount: number;
  loadChoices?: typeof getItemCommunityChoices;
}): Promise<SetCommunityCatalog> {
  const classified = classifySetCommunitySelection(input);
  if (classified.status === "blocked") {
    return classified;
  }
  const load = input.loadChoices ?? getItemCommunityChoices;
  try {
    const catalog = await load(classified.itemId);
    const choices = (catalog.choices ?? []).filter(
      (row) => row && String(row.id ?? "").trim().length > 0,
    );
    if (choices.length === 0) {
      return { status: "none" };
    }
    return {
      status: "ready",
      itemId: classified.itemId,
      currentId: String(catalog.currentCommunityId ?? "").trim(),
      choices,
    };
  } catch (err: unknown) {
    return { status: "http", http: httpBucket(err) };
  }
}

export type SetCommunitySave =
  | { status: "saved"; communityId: string }
  | { status: "gate"; reason: "blank" | "unchanged" | "forbidden" }
  | { status: "http"; http: 400 | 403 | 409 | "other" };

export async function saveSetCommunity(input: {
  itemId: string;
  selectedId: string;
  currentId: string;
  allowedIds: readonly string[];
  change?: typeof changeItemCommunity;
}): Promise<SetCommunitySave> {
  const gate = gateExplorerCommunityChange(input);
  if (!gate.ok) {
    return { status: "gate", reason: gate.reason };
  }
  const change = input.change ?? changeItemCommunity;
  try {
    await change(input.itemId, gate.communityId);
    return { status: "saved", communityId: gate.communityId };
  } catch (err: unknown) {
    return { status: "http", http: httpBucket(err) };
  }
}

function httpBucket(err: unknown): 400 | 403 | 409 | "other" {
  if (isApiError(err)) {
    if (err.status === 400 || err.status === 403 || err.status === 409) {
      return err.status;
    }
  }
  return "other";
}
