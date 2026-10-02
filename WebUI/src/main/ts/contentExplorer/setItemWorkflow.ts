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
 * Content → Set workflow for one selected page or asset (#5076 / parent #4530).
 *
 * <p>This assigns a content-type workflow ({@code changeWorkflow}). It is not
 * a workflow transition. Cancel, folders, multi-select, and HTTP 400/403/409
 * must not be reported as a saved workflow.</p>
 */

import { isApiError } from "../api/client";
import {
  changeItemWorkflow,
  getItemWorkflowChoices,
  type ItemWorkflowChoice,
} from "../api/contentExplorer/itemWorkflowApi";
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
