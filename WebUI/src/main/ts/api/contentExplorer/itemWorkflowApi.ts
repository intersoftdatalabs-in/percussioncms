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
 * Explorer menus (#2732 / parent #2400 / slice #3639).
 *
 * <p>Provider: {@code PSItemWorkflowService} under
 * {@code /services/itemmanagement/workflow/*}. Mirrors the same surface
 * {@code WorkflowActionsPanel} already uses — no new REST contract.</p>
 *
 * <p>{@code PSItemStateTransition} is {@code @XmlRootElement(name =
 * "ItemStateTransition")}. REST {@code JacksonContextResolver} enables
 * {@code WRAP_ROOT_VALUE}, so GET {@code getTransitions} arrives as
 * {@code { ItemStateTransition: { transitionTriggers: [...] } }}. Callers
 * must unwrap before reading triggers or the Explorer Workflow group stays
 * empty on live H2.</p>
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

/** Jackson / JAXB root names for {@link ItemStateTransition}. */
export const ITEM_STATE_TRANSITION_ROOTS = [
  "ItemStateTransition",
  "PSItemStateTransition",
] as const;

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

/**
 * Coerce Jackson list / single-string / JAXB {@code string} wrappers to
 * trigger names. Empty or unknown shapes become {@code []}.
 */
export function coerceTransitionTriggers(raw: unknown): string[] {
  if (raw == null) {
    return [];
  }
  if (typeof raw === "string") {
    const t = raw.trim();
    return t.length > 0 ? [t] : [];
  }
  if (Array.isArray(raw)) {
    return raw
      .map((entry) => String(entry ?? "").trim())
      .filter((entry) => entry.length > 0);
  }
  const obj = asRecord(raw);
  if (!obj) {
    return [];
  }
  for (const key of [
    "string",
    "String",
    "transitionTriggers",
    "transitionTrigger",
  ]) {
    if (!(key in obj)) {
      continue;
    }
    const inner = coerceTransitionTriggers(obj[key]);
    if (inner.length > 0) {
      return inner;
    }
  }
  return [];
}

/**
 * Unwrap Jackson WRAP_ROOT {@code ItemStateTransition} (or a flat DTO).
 */
export function unwrapItemStateTransition(
  data: unknown,
): ItemStateTransition {
  const root = asRecord(data);
  if (!root) {
    return { transitionTriggers: [] };
  }
  let body: Record<string, unknown> = root;
  for (const name of ITEM_STATE_TRANSITION_ROOTS) {
    const nested = asRecord(root[name]);
    if (nested) {
      body = nested;
      break;
    }
  }
  return {
    itemId: asOptionalString(body.itemId),
    stateId: asOptionalString(body.stateId),
    stateName: asOptionalString(body.stateName),
    workflowId: asOptionalString(body.workflowId),
    transitionTriggers: coerceTransitionTriggers(body.transitionTriggers),
  };
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
  const data = await get<unknown>(
    `${PATHS.ITEM_WORKFLOW_TRANSITIONS}${encodeURIComponent(id)}`,
  );
  return unwrapItemStateTransition(data);
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
