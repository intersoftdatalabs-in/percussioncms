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
 * Helpers for Minuet-parity incremental publish with related-item approval.
 * Related item ids are POSTed as a JSON array path segment
 * (see publishIncrementalWithApproval / PercPublisherService).
 */

/** Extract a stable content id from a related-queue item DTO. */
export function relatedItemId(item: unknown): string | null {
  if (item == null || typeof item !== "object") {
    return null;
  }
  const obj = item as Record<string, unknown>;
  const raw = obj.id ?? obj.contentId ?? obj.contentid ?? obj.itemId;
  if (raw == null || raw === "") {
    return null;
  }
  return String(raw);
}

/** Collect selectable ids from a related-items queue list. */
export function collectRelatedItemIds(items: unknown[]): string[] {
  const ids: string[] = [];
  for (const item of items) {
    const id = relatedItemId(item);
    if (id != null) {
      ids.push(id);
    }
  }
  return ids;
}

/**
 * Build the related-items payload string for publishIncrementalWithApproval.
 * Minuet sends JSON.stringify(selectedIds) as the final path segment
 * (numeric ids stay numbers when provided as numbers).
 */
export function buildApprovalPayload(selectedIds: Array<string | number>): string {
  const normalized = selectedIds.map((id) => {
    if (typeof id === "number") {
      return id;
    }
    const asNum = Number(id);
    // Preserve numeric string ids as numbers when lossless (Minuet id type).
    if (id !== "" && Number.isFinite(asNum) && String(asNum) === id) {
      return asNum;
    }
    return id;
  });
  return JSON.stringify(normalized);
}

/**
 * Whether incremental publish should use the approval API.
 * When related items were previewed, always use approval (empty selection = none approved).
 */
export function shouldUseApprovalPath(relatedItems: unknown[]): boolean {
  return relatedItems.length > 0;
}

/** Display label for a related queue row. */
export function relatedItemLabel(item: unknown): string {
  if (item == null || typeof item !== "object") {
    return relatedItemId(item) ?? "—";
  }
  const obj = item as Record<string, unknown>;
  const name = obj.name ?? obj.title ?? obj.sys_title;
  if (name != null && String(name) !== "") {
    return String(name);
  }
  return relatedItemId(item) ?? "—";
}
