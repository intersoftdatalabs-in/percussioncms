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
 * Content → Set community for one selected page or asset (#5077 / parent #4530)
 * and for every checked page or asset (#5133).
 *
 * <p>Cancel, folders, and HTTP 400/403/409 must not be reported as a saved
 * community for the whole selection. A folder in a multi-selection is not
 * written. The community name is shown on an item only after that item's
 * change succeeds. This is not the login community switch and not folder
 * ACL community. Single-item still refuses a multi-count so callers that
 * have not opted into {@link planSetCommunityMulti} keep #5077.</p>
 */

import { isApiError } from "../api/client";
import {
  changeItemCommunity,
  getItemCommunityChoices,
  type ItemCommunityChoice,
} from "../api/contentExplorer/itemCommunityApi";
import { message } from "../i18n/message";
import { EXPLORER_MSG } from "./messages";
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

export interface SetCommunityTarget {
  itemId: string;
  name: string;
}

export type SetCommunityMultiPlan =
  | { status: "blocked"; reason: SetCommunityBlockReason; name: string }
  | {
      status: "ready";
      targets: SetCommunityTarget[];
      skippedFolderNames: string[];
    };

/**
 * Pages and assets are written. Folders are named and skipped. Duplicate ids
 * are written once. An empty check set, or a set with no page or asset, does
 * not open a save.
 */
export function planSetCommunityMulti(
  items: readonly PSPathItem[],
): SetCommunityMultiPlan {
  if (items.length === 0) {
    return { status: "blocked", reason: "empty", name: "" };
  }
  const targets: SetCommunityTarget[] = [];
  const seen = new Set<string>();
  const skippedFolderNames: string[] = [];
  const seenFolders = new Set<string>();
  let notItemName = "";
  let noIdName = "";
  for (const item of items) {
    const name = (item.name ?? item.path ?? "").trim();
    if (isFolder(item)) {
      const label = name || (item.id == null ? "" : String(item.id).trim());
      if (label && !seenFolders.has(label)) {
        seenFolders.add(label);
        skippedFolderNames.push(label);
      }
      continue;
    }
    if (resolvePublishKind(item) === "none") {
      if (!notItemName) {
        notItemName = name;
      }
      continue;
    }
    const itemId = item.id == null ? "" : String(item.id).trim();
    if (!itemId) {
      if (!noIdName) {
        noIdName = name;
      }
      continue;
    }
    if (seen.has(itemId)) {
      continue;
    }
    seen.add(itemId);
    targets.push({ itemId, name: name || itemId });
  }
  if (targets.length === 0) {
    if (skippedFolderNames.length > 0) {
      return {
        status: "blocked",
        reason: "folder",
        name: skippedFolderNames[0] ?? "",
      };
    }
    if (noIdName) {
      return { status: "blocked", reason: "no-id", name: noIdName };
    }
    return { status: "blocked", reason: "not-item", name: notItemName };
  }
  return { status: "ready", targets, skippedFolderNames };
}

export type SetCommunityMultiCatalog =
  | {
      status: "ready";
      targets: SetCommunityTarget[];
      skippedFolderNames: string[];
      currentId: string;
      choices: ItemCommunityChoice[];
    }
  | { status: "blocked"; reason: SetCommunityBlockReason; name: string }
  | { status: "http"; http: 400 | 403 | 409 | "other" }
  | { status: "none" };

/** Dialog catalog from the first page or asset. Folders are not loaded. */
export async function loadSetCommunityMultiCatalog(input: {
  items: readonly PSPathItem[];
  loadChoices?: typeof getItemCommunityChoices;
}): Promise<SetCommunityMultiCatalog> {
  const plan = planSetCommunityMulti(input.items);
  if (plan.status === "blocked") {
    return plan;
  }
  const load = input.loadChoices ?? getItemCommunityChoices;
  try {
    const catalog = await load(plan.targets[0].itemId);
    const choices = (catalog.choices ?? []).filter(
      (row) => row && String(row.id ?? "").trim().length > 0,
    );
    if (choices.length === 0) {
      return { status: "none" };
    }
    return {
      status: "ready",
      targets: plan.targets,
      skippedFolderNames: plan.skippedFolderNames,
      currentId: String(catalog.currentCommunityId ?? "").trim(),
      choices,
    };
  } catch (err: unknown) {
    return { status: "http", http: httpBucket(err) };
  }
}

export type SetCommunityFailureCode = 400 | 403 | 409 | "other" | "forbidden" | "blank";

export interface SetCommunityItemFailure {
  itemId: string;
  name: string;
  http: SetCommunityFailureCode;
}

export interface SetCommunityMultiSave {
  status: "saved" | "partial" | "failed" | "unchanged";
  communityId: string;
  saved: SetCommunityTarget[];
  unchanged: SetCommunityTarget[];
  failures: SetCommunityItemFailure[];
  skippedFolderNames: string[];
}

export interface SetCommunityShown {
  itemId: string;
  name: string;
  communityId: string;
}

/**
 * One community on every target. Each POST is independent. {@code onItemSaved}
 * runs only after that item's change returns, and never for a failure.
 * A folder name is recorded by the caller and is not posted.
 */
export async function saveSetCommunityOnSelection(input: {
  targets: readonly SetCommunityTarget[];
  skippedFolderNames?: readonly string[];
  selectedId: string;
  allowedIds: readonly string[];
  loadChoices?: (
    itemId: string,
  ) => Promise<{ currentCommunityId?: string; choices?: ItemCommunityChoice[] }>;
  change?: typeof changeItemCommunity;
  onItemSaved?: (saved: SetCommunityShown) => void;
}): Promise<SetCommunityMultiSave> {
  const selectedId = String(input.selectedId ?? "").trim();
  const saved: SetCommunityTarget[] = [];
  const unchanged: SetCommunityTarget[] = [];
  const failures: SetCommunityItemFailure[] = [];
  const load = input.loadChoices ?? getItemCommunityChoices;
  const change = input.change ?? changeItemCommunity;

  for (const target of input.targets) {
    let currentId = "";
    let allowed = input.allowedIds;
    if (load) {
      try {
        const catalog = await load(target.itemId);
        currentId = String(catalog.currentCommunityId ?? "").trim();
        const ids = (catalog.choices ?? [])
          .map((row) => String(row?.id ?? "").trim())
          .filter((id) => id.length > 0);
        if (ids.length > 0) {
          allowed = ids;
        }
      } catch (err: unknown) {
        failures.push({
          itemId: target.itemId,
          name: target.name,
          http: httpBucket(err),
        });
        continue;
      }
    }
    const one = await saveSetCommunity({
      itemId: target.itemId,
      selectedId,
      currentId,
      allowedIds: allowed,
      change,
    });
    if (one.status === "saved") {
      saved.push(target);
      input.onItemSaved?.({
        itemId: target.itemId,
        name: target.name,
        communityId: one.communityId,
      });
      continue;
    }
    if (one.status === "gate" && one.reason === "unchanged") {
      unchanged.push(target);
      continue;
    }
    if (one.status === "http") {
      failures.push({
        itemId: target.itemId,
        name: target.name,
        http: one.http,
      });
      continue;
    }
    failures.push({
      itemId: target.itemId,
      name: target.name,
      http: one.status === "gate" && one.reason === "forbidden" ? "forbidden" : "blank",
    });
  }

  let status: SetCommunityMultiSave["status"];
  if (failures.length > 0) {
    status = saved.length > 0 ? "partial" : "failed";
  } else if (saved.length === 0) {
    status = "unchanged";
  } else {
    status = "saved";
  }
  return {
    status,
    communityId: status === "saved" ? selectedId : "",
    saved,
    unchanged,
    failures,
    skippedFolderNames: [...(input.skippedFolderNames ?? [])],
  };
}

/** Status line. Success is only a complete write of every target. */
export function describeSetCommunityMultiSave(
  result: SetCommunityMultiSave,
  communityName: string,
): {
  kind: "success" | "error";
  reason: string;
  communityId: string;
  communityName: string;
  text: string;
} {
  const name = communityName.trim() || result.communityId;
  const parts: string[] = [];
  if (result.status === "saved") {
    parts.push(`${message(EXPLORER_MSG.SET_COMMUNITY_SAVED)} ${name}`.trim());
  } else if (result.status === "unchanged") {
    parts.push(message(EXPLORER_MSG.SET_COMMUNITY_UNCHANGED));
  } else {
    const detail = result.failures
      .map((failure) => `${failure.name} (${failureLabel(failure.http)})`)
      .join("; ");
    parts.push(
      message(EXPLORER_MSG.SET_COMMUNITY_PARTIAL).split("{detail}").join(detail),
    );
  }
  if (result.skippedFolderNames.length > 0) {
    parts.push(
      `${message(EXPLORER_MSG.SET_COMMUNITY_FOLDER)}: ${result.skippedFolderNames.join(", ")}`,
    );
  }
  const success = result.status === "saved";
  return {
    kind: success ? "success" : "error",
    reason: success
      ? result.skippedFolderNames.length > 0
        ? "folders-skipped"
        : ""
      : result.status === "unchanged"
        ? "unchanged"
        : "partial",
    communityId: success ? result.communityId : "",
    communityName: success ? name : "",
    text: parts.join(" "),
  };
}

function failureLabel(code: SetCommunityFailureCode): string {
  if (code === 400 || code === 403 || code === 409) {
    return `HTTP ${code}`;
  }
  if (code === "forbidden") {
    return message(EXPLORER_MSG.SET_COMMUNITY_FORBIDDEN);
  }
  if (code === "blank") {
    return message(EXPLORER_MSG.SET_COMMUNITY_BLANK);
  }
  return message(EXPLORER_MSG.SET_COMMUNITY_FAILED);
}
