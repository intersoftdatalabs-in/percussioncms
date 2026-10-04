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
 * Content → Set workflow for one selected page or asset (#5076 / parent #4530)
 * and for every checked page or asset (#5155).
 *
 * <p>This assigns a content-type workflow ({@code changeWorkflow}). It is not
 * a workflow transition. Cancel, folders, and HTTP 400/403/409 must not be
 * reported as a saved workflow for the whole selection. A folder in a
 * multi-selection is not written. The workflow name is shown on an item only
 * after that item's change succeeds. Single-item still refuses a multi-count
 * so callers that have not opted into {@link planSetWorkflowMulti} keep
 * #5076.</p>
 */

import { isApiError } from "../api/client";
import {
  changeItemWorkflow,
  getItemWorkflowChoices,
  type ItemWorkflowChoice,
} from "../api/contentExplorer/itemWorkflowApi";
import { message } from "../i18n/message";
import { EXPLORER_MSG } from "./messages";
import { resolvePublishKind } from "./itemPublish";
import { isFolder } from "./selection";
import type { PSPathItem } from "../api/contentExplorer/types";

export type SetWorkflowBlockReason =
  | "empty"
  | "folder"
  | "multi"
  | "not-item"
  | "no-id";

export type SetWorkflowSelection =
  | { status: "blocked"; reason: SetWorkflowBlockReason; name: string }
  | { status: "ready"; itemId: string; name: string };

export function classifySetWorkflowSelection(input: {
  item: PSPathItem | null | undefined;
  selectedCount: number;
}): SetWorkflowSelection {
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

export type WorkflowChoiceGate =
  | { ok: true; workflowId: string }
  | { ok: false; reason: "blank" | "unchanged" | "forbidden" };

/** Client gate before POST changeWorkflow. The server repeats the check. */
export function gateExplorerWorkflowChange(input: {
  selectedId: string;
  currentId: string;
  allowedIds: readonly string[];
}): WorkflowChoiceGate {
  const workflowId = String(input.selectedId ?? "").trim();
  if (!workflowId) {
    return { ok: false, reason: "blank" };
  }
  const current = String(input.currentId ?? "").trim();
  if (current && workflowId === current) {
    return { ok: false, reason: "unchanged" };
  }
  const allowed = input.allowedIds
    .map((id) => String(id ?? "").trim())
    .filter((id) => id.length > 0);
  if (!allowed.includes(workflowId)) {
    return { ok: false, reason: "forbidden" };
  }
  return { ok: true, workflowId };
}

export type SetWorkflowCatalog =
  | {
      status: "ready";
      itemId: string;
      currentId: string;
      choices: ItemWorkflowChoice[];
    }
  | { status: "blocked"; reason: SetWorkflowBlockReason; name: string }
  | { status: "http"; http: 400 | 403 | 409 | "other" }
  | { status: "none" };

export async function loadSetWorkflowCatalog(input: {
  item: PSPathItem | null | undefined;
  selectedCount: number;
  loadChoices?: typeof getItemWorkflowChoices;
}): Promise<SetWorkflowCatalog> {
  const classified = classifySetWorkflowSelection(input);
  if (classified.status === "blocked") {
    return classified;
  }
  const load = input.loadChoices ?? getItemWorkflowChoices;
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
      currentId: String(catalog.currentWorkflowId ?? "").trim(),
      choices,
    };
  } catch (err: unknown) {
    return { status: "http", http: httpBucket(err) };
  }
}

export type SetWorkflowSave =
  | { status: "saved"; workflowId: string }
  | { status: "gate"; reason: "blank" | "unchanged" | "forbidden" }
  | { status: "http"; http: 400 | 403 | 409 | "other" };

export async function saveSetWorkflow(input: {
  itemId: string;
  selectedId: string;
  currentId: string;
  allowedIds: readonly string[];
  change?: typeof changeItemWorkflow;
}): Promise<SetWorkflowSave> {
  const gate = gateExplorerWorkflowChange(input);
  if (!gate.ok) {
    return { status: "gate", reason: gate.reason };
  }
  const change = input.change ?? changeItemWorkflow;
  try {
    await change(input.itemId, gate.workflowId);
    return { status: "saved", workflowId: gate.workflowId };
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

export interface SetWorkflowTarget {
  itemId: string;
  name: string;
}

export type SetWorkflowMultiPlan =
  | { status: "blocked"; reason: SetWorkflowBlockReason; name: string }
  | {
      status: "ready";
      targets: SetWorkflowTarget[];
      skippedFolderNames: string[];
    };

/**
 * Pages and assets are written. Folders are named and skipped. Duplicate ids
 * are written once. An empty check set, or a set with no page or asset, does
 * not open a save.
 */
export function planSetWorkflowMulti(
  items: readonly PSPathItem[],
): SetWorkflowMultiPlan {
  if (items.length === 0) {
    return { status: "blocked", reason: "empty", name: "" };
  }
  const targets: SetWorkflowTarget[] = [];
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

export type SetWorkflowMultiCatalog =
  | {
      status: "ready";
      targets: SetWorkflowTarget[];
      skippedFolderNames: string[];
      currentId: string;
      choices: ItemWorkflowChoice[];
    }
  | { status: "blocked"; reason: SetWorkflowBlockReason; name: string }
  | { status: "http"; http: 400 | 403 | 409 | "other" }
  | { status: "none" };

/** Dialog catalog from the first page or asset. Folders are not loaded. */
export async function loadSetWorkflowMultiCatalog(input: {
  items: readonly PSPathItem[];
  loadChoices?: typeof getItemWorkflowChoices;
}): Promise<SetWorkflowMultiCatalog> {
  const plan = planSetWorkflowMulti(input.items);
  if (plan.status === "blocked") {
    return plan;
  }
  const load = input.loadChoices ?? getItemWorkflowChoices;
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
      currentId: String(catalog.currentWorkflowId ?? "").trim(),
      choices,
    };
  } catch (err: unknown) {
    return { status: "http", http: httpBucket(err) };
  }
}

export type SetWorkflowFailureCode = 400 | 403 | 409 | "other" | "forbidden" | "blank";

export interface SetWorkflowItemFailure {
  itemId: string;
  name: string;
  http: SetWorkflowFailureCode;
}

export interface SetWorkflowMultiSave {
  status: "saved" | "partial" | "failed" | "unchanged";
  workflowId: string;
  saved: SetWorkflowTarget[];
  unchanged: SetWorkflowTarget[];
  failures: SetWorkflowItemFailure[];
  skippedFolderNames: string[];
}

export interface SetWorkflowShown {
  itemId: string;
  name: string;
  workflowId: string;
}

/**
 * One workflow on every target. Each POST is independent. {@code onItemSaved}
 * runs only after that item's change returns, and never for a failure.
 * A folder name is recorded by the caller and is not posted.
 */
export async function saveSetWorkflowOnSelection(input: {
  targets: readonly SetWorkflowTarget[];
  skippedFolderNames?: readonly string[];
  selectedId: string;
  allowedIds: readonly string[];
  loadChoices?: (
    itemId: string,
  ) => Promise<{ currentWorkflowId?: string; choices?: ItemWorkflowChoice[] }>;
  change?: typeof changeItemWorkflow;
  onItemSaved?: (saved: SetWorkflowShown) => void;
}): Promise<SetWorkflowMultiSave> {
  const selectedId = String(input.selectedId ?? "").trim();
  const saved: SetWorkflowTarget[] = [];
  const unchanged: SetWorkflowTarget[] = [];
  const failures: SetWorkflowItemFailure[] = [];
  const load = input.loadChoices ?? getItemWorkflowChoices;
  const change = input.change ?? changeItemWorkflow;

  for (const target of input.targets) {
    let currentId = "";
    let allowed = input.allowedIds;
    if (load) {
      try {
        const catalog = await load(target.itemId);
        currentId = String(catalog.currentWorkflowId ?? "").trim();
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
    const one = await saveSetWorkflow({
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
        workflowId: one.workflowId,
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

  let status: SetWorkflowMultiSave["status"];
  if (failures.length > 0) {
    status = saved.length > 0 ? "partial" : "failed";
  } else if (saved.length === 0) {
    status = "unchanged";
  } else {
    status = "saved";
  }
  return {
    status,
    workflowId: status === "saved" ? selectedId : "",
    saved,
    unchanged,
    failures,
    skippedFolderNames: [...(input.skippedFolderNames ?? [])],
  };
}

/** Status line. Success is only a complete write of every target. */
export function describeSetWorkflowMultiSave(
  result: SetWorkflowMultiSave,
  workflowName: string,
): {
  kind: "success" | "error";
  reason: string;
  workflowId: string;
  workflowName: string;
  text: string;
} {
  const name = workflowName.trim() || result.workflowId;
  const parts: string[] = [];
  if (result.status === "saved") {
    parts.push(`${message(EXPLORER_MSG.SET_WORKFLOW_SAVED)} ${name}`.trim());
  } else if (result.status === "unchanged") {
    parts.push(message(EXPLORER_MSG.SET_WORKFLOW_UNCHANGED));
  } else {
    const detail = result.failures
      .map((failure) => `${failure.name} (${failureLabel(failure.http)})`)
      .join("; ");
    parts.push(
      message(EXPLORER_MSG.SET_WORKFLOW_PARTIAL).split("{detail}").join(detail),
    );
  }
  if (result.skippedFolderNames.length > 0) {
    parts.push(
      `${message(EXPLORER_MSG.SET_WORKFLOW_FOLDER)}: ${result.skippedFolderNames.join(", ")}`,
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
    workflowId: success ? result.workflowId : "",
    workflowName: success ? name : "",
    text: parts.join(" "),
  };
}

function failureLabel(code: SetWorkflowFailureCode): string {
  if (code === 400 || code === 403 || code === 409) {
    return `HTTP ${code}`;
  }
  if (code === "forbidden") {
    return message(EXPLORER_MSG.SET_WORKFLOW_FORBIDDEN);
  }
  if (code === "blank") {
    return message(EXPLORER_MSG.SET_WORKFLOW_BLANK);
  }
  return message(EXPLORER_MSG.SET_WORKFLOW_FAILED);
}
