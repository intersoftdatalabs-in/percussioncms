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
 * Thin client for sitemanage itemmanagement workflow endpoints used by
 * Explorer menus (#2732 / parent #2400).
 *
 * <p>Provider: {@code PSItemWorkflowService} under
 * {@code /services/itemmanagement/workflow/*}. Mirrors the same surface
 * {@code WorkflowActionsPanel} already uses — no new REST contract.</p>
 */

import { get } from "../client";
import { PATHS } from "../paths";

/**
 * Wire shape for {@code GET .../workflow/getTransitions/{id}}
 * ({@code PSItemStateTransition}).
 */
export interface ItemStateTransition {
  itemId?: string;
  stateId?: string;
  stateName?: string;
  workflowId?: string;
  transitionTriggers?: string[];
}

/**
 * Wire shape for transition results
 * ({@code PSItemTransitionResults}) — fields optional; success is HTTP 200.
 */
export interface ItemTransitionResults {
  itemId?: string;
  [key: string]: unknown;
}

/**
 * List allowed workflow transition trigger names for a content item.
 *
 * @param itemId content / component id as returned by pathmanagement
 */
export async function getItemWorkflowTransitions(
  itemId: string,
): Promise<ItemStateTransition> {
  const id = String(itemId ?? "").trim();
  if (!id) {
    return { transitionTriggers: [] };
  }
  return get<ItemStateTransition>(
    `${PATHS.ITEM_WORKFLOW_TRANSITIONS}${encodeURIComponent(id)}`,
  );
}

/**
 * Execute a named workflow transition for the item (optional comment).
 *
 * <p>Uses {@code transitionWithComments} so comments can be supplied later
 * without a second client path; empty comment is allowed by the service.</p>
 */
export async function transitionItem(
  itemId: string,
  trigger: string,
  comment?: string,
): Promise<ItemTransitionResults> {
  const id = String(itemId ?? "").trim();
  const trig = String(trigger ?? "").trim();
  if (!id || !trig) {
    throw new Error("transitionItem requires itemId and trigger");
  }
  let url = `${PATHS.ITEM_WORKFLOW_TRANSITION_WITH_COMMENTS}${encodeURIComponent(id)}/${encodeURIComponent(trig)}`;
  if (comment != null && String(comment).length > 0) {
    url += `?comment=${encodeURIComponent(String(comment))}`;
  }
  return get<ItemTransitionResults>(url);
}
