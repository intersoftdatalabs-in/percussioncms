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
 * Move one owned Active Assembly relationship to a different slot on the
 * same page (#5265 / parent #4530).
 *
 * <p>Confirm posts the existing template-slot API with the destination
 * slot and the row's current snippet template. Index is omitted so the
 * server appends on the destination instead of replaying the source
 * position. Cancel, a folder row, a relationship with no slot, the same
 * slot, and HTTP 400/403/409 must not be reported as a move. This is not
 * an up or down reorder and not a template-only change that stays on the
 * same slot.</p>
 */

import type { PSExplorerRelationshipEdge } from "../api/contentExplorer/relationship";
import type { SlotCanvasSlot } from "../api/contentExplorer/slotRelationshipApi";
import {
  isActiveAssemblyRelationship,
  isFolderRelationshipCategory,
} from "../api/contentExplorer/relationshipsApi";

export type RelationshipSlotMoveBlock =
  | "empty"
  | "folder"
  | "not_assembly"
  | "no_slot"
  | "no_template"
  | "same"
  | "not_allowed";

export type RelationshipSlotMoveGate =
  | {
      ok: true;
      relationshipId: number;
      sourceSlotId: number;
      destinationSlotId: number;
      templateId: number;
    }
  | { ok: false; reason: RelationshipSlotMoveBlock };

export interface RelationshipSlotChoice {
  slotId: number;
  label: string;
}

export interface RelationshipSlotMoveWrite {
  relationshipId: number;
  slotId: number;
  templateId: number;
  sortRank?: number;
}

export interface ActiveAssemblySlotGroups {
  groups: { slotId: number; edges: PSExplorerRelationshipEdge[] }[];
  rest: PSExplorerRelationshipEdge[];
}

/** Active Assembly rows with a slot can open the move-to-slot dialog. */
export function canMoveRelationshipToAnotherSlot(
  edge: Pick<
    PSExplorerRelationshipEdge,
    "category" | "configName" | "relationshipId" | "slotId"
  >,
): boolean {
  if (
    isFolderRelationshipCategory(edge.category ?? "") ||
    isFolderRelationshipCategory(edge.configName ?? "")
  ) {
    return false;
  }
  if (!isActiveAssemblyRelationship(edge)) {
    return false;
  }
  return Number(edge.relationshipId) > 0 && Number(edge.slotId) > 0;
}

/** Visible name for a page slot. A positive id is the fallback, not a move. */
export function slotChoiceLabel(
  slot: Pick<SlotCanvasSlot, "slotId" | "label" | "name">,
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
 * Slots the dialog may offer. The row's current slot is not a destination.
 * Non-positive ids are dropped. Order follows the page canvas.
 */
export function destinationSlotChoices(
  sourceSlotId: number,
  slots: readonly Pick<SlotCanvasSlot, "slotId" | "label" | "name">[],
): RelationshipSlotChoice[] {
  const source = Number(sourceSlotId);
  const seen = new Set<number>();
  const choices: RelationshipSlotChoice[] = [];
  for (const slot of slots) {
    const slotId = Number(slot.slotId);
    if (!Number.isFinite(slotId) || slotId <= 0 || slotId === source) {
      continue;
    }
    if (seen.has(slotId)) {
      continue;
    }
    seen.add(slotId);
    choices.push({ slotId, label: slotChoiceLabel(slot) });
  }
  return choices;
}

/**
 * Whether confirm may post one slot move. The template stays the row's
 * current snippet template. The destination must be a different slot on
 * the page.
 */
export function gateMoveRelationshipToSlot(input: {
  edge: PSExplorerRelationshipEdge | null | undefined;
  destinationSlotId: number;
  pageSlotIds: readonly number[];
}): RelationshipSlotMoveGate {
  const edge = input.edge ?? null;
  if (!edge) {
    return { ok: false, reason: "empty" };
  }
  if (
    isFolderRelationshipCategory(edge.category ?? "") ||
    isFolderRelationshipCategory(edge.configName ?? "")
  ) {
    return { ok: false, reason: "folder" };
  }
  if (!isActiveAssemblyRelationship(edge)) {
    return { ok: false, reason: "not_assembly" };
  }
  const relationshipId = Number(edge.relationshipId);
  const sourceSlotId = Number(edge.slotId ?? 0);
  if (!(relationshipId > 0) || !(sourceSlotId > 0)) {
    return { ok: false, reason: "no_slot" };
  }
  const templateId = Number(edge.templateId ?? 0);
  if (!Number.isFinite(templateId) || templateId <= 0) {
    return { ok: false, reason: "no_template" };
  }
  const destinationSlotId = Number(input.destinationSlotId);
  if (!Number.isFinite(destinationSlotId) || destinationSlotId <= 0) {
    return { ok: false, reason: "empty" };
  }
  if (destinationSlotId === sourceSlotId) {
    return { ok: false, reason: "same" };
  }
  if (!input.pageSlotIds.some((id) => Number(id) === destinationSlotId)) {
    return { ok: false, reason: "not_allowed" };
  }
  return {
    ok: true,
    relationshipId,
    sourceSlotId,
    destinationSlotId,
    templateId,
  };
}

/**
 * Replace one row after a successful slot move. The snippet template name
 * stays. Other rows are copied unchanged. Call this only after the server
 * returns the destination slot.
 */
export function applyRelationshipSlotMove(
  edges: readonly PSExplorerRelationshipEdge[],
  relationshipId: number,
  updated: RelationshipSlotMoveWrite,
): PSExplorerRelationshipEdge[] {
  return edges.map((edge) => {
    if (edge.relationshipId !== relationshipId) {
      return edge;
    }
    const nextId =
      Number.isFinite(updated.relationshipId) && updated.relationshipId > 0
        ? updated.relationshipId
        : edge.relationshipId;
    const nextSlot =
      Number.isFinite(updated.slotId) && updated.slotId > 0
        ? updated.slotId
        : Number(edge.slotId ?? 0);
    const nextTemplate =
      Number.isFinite(updated.templateId) && updated.templateId > 0
        ? updated.templateId
        : Number(edge.templateId ?? 0);
    const rank = Number(updated.sortRank);
    return {
      ...edge,
      relationshipId: nextId,
      slotId: nextSlot,
      templateId: nextTemplate,
      sortRank: Number.isInteger(rank) && rank >= 0 ? rank : edge.sortRank,
    };
  });
}

/**
 * Active Assembly rows with a slot are listed under that slot. Folder rows
 * and rows with no slot stay outside every slot group.
 */
export function groupActiveAssemblyBySlot(
  edges: readonly PSExplorerRelationshipEdge[],
): ActiveAssemblySlotGroups {
  const groups: { slotId: number; edges: PSExplorerRelationshipEdge[] }[] = [];
  const indexBySlot = new Map<number, number>();
  const rest: PSExplorerRelationshipEdge[] = [];
  for (const edge of edges) {
    const slotId = Number(edge.slotId ?? 0);
    if (
      !isActiveAssemblyRelationship(edge) ||
      !(Number.isFinite(slotId) && slotId > 0)
    ) {
      rest.push(edge);
      continue;
    }
    const existing = indexBySlot.get(slotId);
    if (existing == null) {
      indexBySlot.set(slotId, groups.length);
      groups.push({ slotId, edges: [edge] });
    } else {
      groups[existing].edges.push(edge);
    }
  }
  return { groups, rest };
}
