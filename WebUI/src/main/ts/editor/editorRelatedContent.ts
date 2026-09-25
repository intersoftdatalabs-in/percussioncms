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
import { parseExplorerContentId } from "../contentExplorer/menuCatalogLoad";
import type { EditorHostMode } from "./editorHostUrl";

export type RelatedContentKind = "slot" | "inline";

export interface RelatedContentRow {
  key: string;
  kind: RelatedContentKind;
  itemId: string;
  slotLabel: string;
  /** Slot id when the row is an Active Assembly slot item. */
  slotId?: number;
  /** Active Assembly relationship id when the row can be removed or reordered. */
  relationshipId?: number;
  /** Current snippet template id when the row is a slot relationship. */
  templateId?: number;
}

/** Slot rows with a relationship can change snippet template. Inline cannot. */
export function canChangeRelatedSnippetTemplate(row: RelatedContentRow): boolean {
  return (
    row.kind === "slot" &&
    Number(row.relationshipId) > 0 &&
    Number(row.slotId) > 0
  );
}

export type RelatedContentErrorReason = "forbidden" | "failed";

export type RelatedInsertErrorReason =
  | "bad_request"
  | "forbidden"
  | "not_found"
  | "failed";

export interface InsertSlotChoice {
  slotId: number;
  templateId: number;
  label: string;
}

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

function httpStatus(err: unknown): number | null {
  if (isApiError(err)) {
    return err.status;
  }
  if (
    err != null &&
    typeof err === "object" &&
    "status" in err &&
    typeof (err as { status: unknown }).status === "number"
  ) {
    return (err as { status: number }).status;
  }
  return null;
}

/** Maps insert and remove HTTP failures. Anything else is a generic failure, not success. */
export function relatedInsertErrorReason(err: unknown): RelatedInsertErrorReason {
  const status = httpStatus(err);
  if (status === 400) {
    return "bad_request";
  }
  if (status === 403) {
    return "forbidden";
  }
  if (status === 404) {
    return "not_found";
  }
  return "failed";
}

/** Same status map as insert: 400, 403, and 404 are not a successful remove. */
export function relatedRemoveErrorReason(err: unknown): RelatedInsertErrorReason {
  return relatedInsertErrorReason(err);
}

export type RelatedReorderErrorReason =
  | "forbidden"
  | "not_found"
  | "conflict"
  | "failed";

/** 403, 404, and 409 are not a successful reorder. */
export function relatedReorderErrorReason(
  err: unknown,
): RelatedReorderErrorReason {
  const status = httpStatus(err);
  if (status === 403) {
    return "forbidden";
  }
  if (status === 404) {
    return "not_found";
  }
  if (status === 409) {
    return "conflict";
  }
  return "failed";
}

/** Whether a slot row can move up or down among siblings in the same slot. */
export function relatedReorderEnds(
  rows: RelatedContentRow[],
  row: RelatedContentRow,
): { up: boolean; down: boolean } {
  if (row.kind !== "slot" || !(row.relationshipId != null && row.relationshipId > 0)) {
    return { up: false, down: false };
  }
  const siblings = rows.filter(
    (candidate) =>
      candidate.kind === "slot" &&
      candidate.slotId === row.slotId &&
      candidate.relationshipId != null &&
      candidate.relationshipId > 0,
  );
  const index = siblings.findIndex((candidate) => candidate.key === row.key);
  if (index < 0 || siblings.length < 2) {
    return { up: false, down: false };
  }
  return { up: index > 0, down: index < siblings.length - 1 };
}

/** Slots an author can insert into. Template falls back to the canvas template. */
export function insertSlotChoices(
  canvas: SlotCanvas | null | undefined,
): InsertSlotChoice[] {
  if (!canvas?.slots) {
    return [];
  }
  const canvasTemplate =
    canvas.templateId != null && canvas.templateId > 0 ? canvas.templateId : 0;
  const choices: InsertSlotChoice[] = [];
  for (const slot of canvas.slots) {
    if (!(slot.slotId > 0)) {
      continue;
    }
    let templateId = canvasTemplate;
    if (templateId <= 0) {
      for (const item of slot.items ?? []) {
        if (item.templateId > 0) {
          templateId = item.templateId;
          break;
        }
      }
    }
    if (templateId <= 0) {
      continue;
    }
    const label = (slot.label || slot.name || "").trim() || "Slot";
    choices.push({ slotId: slot.slotId, templateId, label });
  }
  return choices;
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
        const relationshipId =
          item.relationshipId > 0 ? item.relationshipId : undefined;
        rows.push({
          key,
          kind: "slot",
          itemId,
          slotLabel,
          slotId: slot.slotId > 0 ? slot.slotId : undefined,
          relationshipId,
          templateId: item.templateId > 0 ? item.templateId : undefined,
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

/**
 * Open follows the current host: view and promote stay view; otherwise edit.
 */
export function relatedItemOpenMode(
  hostMode: EditorHostMode | null | undefined,
): "edit" | "view" {
  if (hostMode === "view" || hostMode === "promote") {
    return "view";
  }
  return "edit";
}

/** A row with no CMS content id cannot be opened. */
export function relatedRowCanOpen(itemId: string | null | undefined): boolean {
  return parseExplorerContentId(itemId ?? undefined) != null;
}
