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
 * Flatten slot-canvas + local (inline) related items for EditorHost browse.
 */

import { isApiError } from "../api/client";
import { RelationshipSummaryAuthError } from "../api/contentExplorer/relationshipsApi";
import type { PSLocalDependencySummary } from "../api/contentExplorer/relationship";
import type { SlotCanvas } from "../api/contentExplorer/slotRelationshipApi";

export type RelatedContentKind = "slot" | "inline";

export interface RelatedContentRow {
  key: string;
  kind: RelatedContentKind;
  itemId: string;
  slotLabel: string;
}

export type RelatedContentErrorReason = "forbidden" | "failed";

export function relatedContentErrorReason(
  err: unknown,
): RelatedContentErrorReason {
  if (err instanceof RelationshipSummaryAuthError && err.status === 403) {
    return "forbidden";
  }
  if (isApiError(err) && err.status === 403) {
    return "forbidden";
  }
  if (
    err != null &&
    typeof err === "object" &&
    "status" in err &&
    (err as { status: unknown }).status === 403
  ) {
    return "forbidden";
  }
  return "failed";
}

export function flattenRelatedContent(
  canvas: SlotCanvas | null | undefined,
  local: PSLocalDependencySummary | null | undefined,
): RelatedContentRow[] {
  const rows: RelatedContentRow[] = [];
  const seen = new Set<string>();
  if (canvas?.slots) {
    for (const slot of canvas.slots) {
      const slotLabel = (slot.label || slot.name || "").trim() || "Slot";
      for (const item of slot.items ?? []) {
        const itemId = String(item.dependentId ?? "");
        if (!itemId || itemId === "0") {
          continue;
        }
        const key = `slot:${item.relationshipId || itemId}:${itemId}`;
        if (seen.has(key)) {
          continue;
        }
        seen.add(key);
        rows.push({
          key,
          kind: "slot",
          itemId,
          slotLabel,
        });
      }
    }
  }
  for (const link of local?.links ?? []) {
    const itemId = String(link.targetId ?? "").trim();
    if (!itemId) {
      continue;
    }
    const key = `inline:${link.type}:${itemId}`;
    if (seen.has(key)) {
      continue;
    }
    seen.add(key);
    rows.push({
      key,
      kind: "inline",
      itemId,
      slotLabel: link.type === "local" ? "Inline" : link.type || "Inline",
    });
  }
  return rows;
}
