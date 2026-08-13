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
 * When Explorer may call itemmanagement workflow endpoints (#3330 / #3329).
 *
 * Folders (and objects with {@code workflowId == -1}) are not workflowed
 * content. Loading transitions for them hits {@code getWorkflow(-1)} on
 * the server.
 */

import type { PSPathItem } from "../api/contentExplorer/types";
import { isFolder } from "./selection";

function numericWorkflowId(item: PSPathItem): number | null {
  const extras = item as PSPathItem & { workflowId?: string | number };
  const raw =
    extras.workflowId ??
    item.displayProperties?.workflowId ??
    item.displayProperties?.sys_workflowid ??
    item.displayProperties?.sys_workflow;
  if (raw == null || raw === "") return null;
  const n = Number(raw);
  return Number.isFinite(n) ? n : null;
}

/** True when the item is a folder or its workflow id is the CMS "none" sentinel. */
export function isNonWorkflowedItem(item: PSPathItem | null | undefined): boolean {
  if (!item) return true;
  if (isFolder(item)) return true;
  const wf = numericWorkflowId(item);
  return wf != null && wf <= 0;
}

/** True when the selection can receive workflow transitions. */
export function isWorkflowEligibleItem(
  item: PSPathItem | null | undefined,
): boolean {
  if (!item || isNonWorkflowedItem(item)) return false;
  const id = item.id != null ? String(item.id).trim() : "";
  return id.length > 0;
}
