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
 * Move one related slot row to a different slot on the same page (#6225).
 *
 * <p>Confirm posts the existing slot-relationship template-slot API with the
 * destination slot and the row's current snippet template. Index is omitted
 * so the server appends on the destination instead of replaying the source
 * position. Cancel, an inline row, a missing template, the same slot, a slot
 * that is not on the page, and HTTP 400/403/404 must not be reported as a
 * move. This is not an up or down reorder and not a template-only change
 * that stays on the same slot.</p>
 */

import type { SlotRelationship } from "../api/contentExplorer/slotRelationshipApi";
import {
  canChangeRelatedSnippetTemplate,
  relatedInsertErrorReason,
  type RelatedContentRow,
  type RelatedInsertErrorReason,
} from "./editorRelatedContent";

export interface RelatedSlotDestination {
  slotId: number;
  label: string;
}

/** Slot rows with a relationship, a slot, and a snippet template can move. */
export function canMoveRelatedRowToAnotherSlot(row: RelatedContentRow): boolean {
  return (
    canChangeRelatedSnippetTemplate(row) && Number(row.templateId) > 0
  );
}

/** Visible name for a page slot. A positive id is the fallback, not a move. */
function destinationLabel(
  slot: { slotId: number; label: string; name?: string },
): string {
  const label = (slot.label ?? "").trim();
  if (label) {
    return label;
  }
  const name = (slot.name ?? "").trim();
  if (name) {
    return name;
  }
  return String(slot.slotId);
}

/**
 * Slots the move dialog may offer. The row's current slot is not a
 * destination. Non-positive ids are dropped. Order follows the page canvas.
 */
export function relatedDestinationSlotChoices(
  sourceSlotId: number,
  slots: readonly { slotId: number; label: string; name?: string }[],
): RelatedSlotDestination[] {
  const source = Number(sourceSlotId);
  const seen = new Set<number>();
  const choices: RelatedSlotDestination[] = [];
  for (const slot of slots) {
    const slotId = Number(slot.slotId);
    if (!Number.isFinite(slotId) || slotId <= 0 || slotId === source) {
      continue;
    }
    if (seen.has(slotId)) {
      continue;
    }
    seen.add(slotId);
    choices.push({ slotId, label: destinationLabel(slot) });
  }
  return choices;
}

export type RelatedSlotMoveSave =
  | { ok: true; posted: true; slotId: number; templateId: number }
  | {
      ok: false;
      posted: boolean;
      reason:
        | RelatedInsertErrorReason
        | "not_slot"
        | "no_template"
        | "needs_destination"
        | "same"
        | "not_allowed";
    };

export async function saveRelatedSlotMove(input: {
  row: RelatedContentRow;
  destinationSlotId: number;
  pageSlotIds: readonly number[];
  move: (
    relationshipId: number,
    slotId: number,
    templateId: number,
  ) => Promise<SlotRelationship>;
}): Promise<RelatedSlotMoveSave> {
  if (!canChangeRelatedSnippetTemplate(input.row)) {
    return { ok: false, posted: false, reason: "not_slot" };
  }
  const templateId = Number(input.row.templateId);
  if (!Number.isFinite(templateId) || templateId <= 0) {
    return { ok: false, posted: false, reason: "no_template" };
  }
  const destinationSlotId = Number(input.destinationSlotId);
  if (!Number.isFinite(destinationSlotId) || destinationSlotId <= 0) {
    return { ok: false, posted: false, reason: "needs_destination" };
  }
  const sourceSlotId = Number(input.row.slotId);
  if (destinationSlotId === sourceSlotId) {
    return { ok: false, posted: false, reason: "same" };
  }
  if (!input.pageSlotIds.some((id) => Number(id) === destinationSlotId)) {
    return { ok: false, posted: false, reason: "not_allowed" };
  }
  const relationshipId = Number(input.row.relationshipId);
  try {
    const updated = await input.move(relationshipId, destinationSlotId, templateId);
    const writtenSlot =
      Number(updated.slotId) > 0 ? Number(updated.slotId) : destinationSlotId;
    const writtenTemplate =
      Number(updated.templateId) > 0 ? Number(updated.templateId) : templateId;
    return { ok: true, posted: true, slotId: writtenSlot, templateId: writtenTemplate };
  } catch (err) {
    return {
      ok: false,
      posted: true,
      reason: relatedInsertErrorReason(err),
    };
  }
}
