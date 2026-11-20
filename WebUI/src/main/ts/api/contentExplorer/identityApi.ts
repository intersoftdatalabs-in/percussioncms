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
 * Batch visibility-identity client for the React Content Explorer (#6783 /
 * parent #4530).
 *
 * <p>Server provider: {@code POST /rest/explorer/identity/batch}
 * ({@code ItemIdentityResource} + sitemanage {@code ItemIdentityAdaptor}).
 * Resolves the checkout-status token plus numeric content / object /
 * workflow / community ids the shell evaluator needs for Workbench
 * hide-on-match rules — facts that are not on {@code PSPathItem} rows
 * (#6618). Unknown ids are omitted server-side; everything missing stays
 * {@code undefined} so the evaluator stays permissive.
 */

import type { ExplorerMenuVisibilityState } from "../../contentExplorer/menuVisibility";
import { formatApiError, post } from "../client";
import { PATHS } from "../paths";

/**
 * One resolved row identity. Mirrors rest {@code ItemIdentity} (nullable
 * facts; {@code null} on the wire means "not known to the server").
 */
export interface ExplorerItemIdentity {
  id?: string;
  checkoutStatus?: string | null;
  contentTypeId?: number | string | null;
  objectType?: number | string | null;
  workflowId?: number | string | null;
  communityId?: number | string | null;
  assignmentType?: number | string | null;
  publishableType?: string | null;
}

/** Jackson WRAP_ROOT_VALUE root for {@code ItemIdentityBatchRequest}. */
export const ITEM_IDENTITY_BATCH_REQUEST_ROOT = "ItemIdentityBatchRequest";

export type ItemIdentityBatchRequestEnvelope = {
  ItemIdentityBatchRequest: { ids: string[] };
};

/**
 * Wrap row ids under {@link ITEM_IDENTITY_BATCH_REQUEST_ROOT}. Trims,
 * drops blanks, de-duplicates. Throws when no usable id remains — the
 * server treats that as HTTP 400, so the client never sends it.
 */
export function wrapItemIdentityBatchRequest(
  ids: ReadonlyArray<string | null | undefined>,
): ItemIdentityBatchRequestEnvelope {
  const seen = new Set<string>();
  const clean: string[] = [];
  for (const raw of ids ?? []) {
    const id = String(raw ?? "").trim();
    if (id && !seen.has(id)) {
      seen.add(id);
      clean.push(id);
    }
  }
  if (clean.length === 0) {
    throw new Error("getItemIdentityBatch requires at least one id");
  }
  return { ItemIdentityBatchRequest: { ids: clean } };
}

function asRecord(value: unknown): Record<string, unknown> | null {
  if (value != null && typeof value === "object" && !Array.isArray(value)) {
    return value as Record<string, unknown>;
  }
  return null;
}

/**
 * Normalize the batch wire shape. The resource returns a bare JSON array;
 * accept the {@code ItemIdentity} / {@code itemIdentity} envelope keys and
 * single-object forms defensively so a serializer drift never empties the
 * evaluator silently.
 */
export function unwrapItemIdentityList(payload: unknown): ExplorerItemIdentity[] {
  if (payload == null) {
    return [];
  }
  if (Array.isArray(payload)) {
    return payload.filter(
      (entry): entry is ExplorerItemIdentity =>
        asRecord(entry)?.id != null,
    );
  }
  const root = asRecord(payload);
  if (!root) {
    return [];
  }
  const nested =
    root.ItemIdentity ?? root.itemIdentity ?? root.Itemidentity;
  if (Array.isArray(nested)) {
    return nested.filter(
      (entry): entry is ExplorerItemIdentity =>
        asRecord(entry)?.id != null,
    );
  }
  const single = asRecord(nested);
  if (single?.id != null) {
    return [single as ExplorerItemIdentity];
  }
  return [];
}

/**
 * Resolve visibility identity for the given row ids. Unknown ids come back
 * omitted; transport failures throw a named {@link ApiError} the caller
 * surfaces without hiding menus (permissive default).
 */
export async function getItemIdentityBatch(
  ids: ReadonlyArray<string | null | undefined>,
): Promise<ExplorerItemIdentity[]> {
  try {
    const raw = await post<unknown>(
      PATHS.EXPLORER_IDENTITY_BATCH,
      wrapItemIdentityBatchRequest(ids),
    );
    return unwrapItemIdentityList(raw);
  } catch (err: unknown) {
    throw new Error(formatApiError(err, "Could not load visibility details"));
  }
}

function toFiniteNumber(value: number | string | null | undefined): number | undefined {
  if (value == null || value === "") {
    return undefined;
  }
  const num = Number(value);
  return Number.isFinite(num) && num > 0 ? num : undefined;
}

function toToken(value: string | null | undefined): string | undefined {
  const token = String(value ?? "").trim();
  return token ? token : undefined;
}

/**
 * Map one resolved identity onto the evaluator state. Every unknown fact
 * stays {@code undefined} so the matching hide-rule stays permissive —
 * exactly as before #6517. Pure: no fetch, no DOM.
 */
export function toVisibilityIdentityFields(
  identity: ExplorerItemIdentity | null | undefined,
): Partial<ExplorerMenuVisibilityState> {
  if (identity == null) {
    return {};
  }
  const fields: Partial<ExplorerMenuVisibilityState> = {};
  const checkoutStatus = toToken(identity.checkoutStatus);
  if (checkoutStatus !== undefined) {
    fields.checkoutStatus = checkoutStatus;
  }
  const contentTypeId = toFiniteNumber(identity.contentTypeId);
  if (contentTypeId !== undefined) {
    fields.contentTypeId = contentTypeId;
  }
  const objectType = toFiniteNumber(identity.objectType);
  if (objectType !== undefined) {
    fields.objectType = objectType;
  }
  const assignmentType = toFiniteNumber(identity.assignmentType);
  if (assignmentType !== undefined) {
    fields.assignmentType = assignmentType;
  }
  const workflowId = toFiniteNumber(identity.workflowId);
  if (workflowId !== undefined) {
    fields.workflowId = workflowId;
  }
  const publishableType = toToken(identity.publishableType);
  if (publishableType !== undefined) {
    fields.publishableType = publishableType;
  }
  return fields;
}

/**
 * Map the community id separately: the evaluator compares it with strict
 * string equality, so the raw id text is preserved (no numeric coercion).
 */
export function toVisibilityCommunityId(
  identity: ExplorerItemIdentity | null | undefined,
): string | number | undefined {
  const raw = identity?.communityId;
  if (raw == null || raw === "") {
    return undefined;
  }
  if (typeof raw === "number") {
    return Number.isFinite(raw) && raw > 0 ? raw : undefined;
  }
  const text = String(raw).trim();
  return text ? text : undefined;
}
