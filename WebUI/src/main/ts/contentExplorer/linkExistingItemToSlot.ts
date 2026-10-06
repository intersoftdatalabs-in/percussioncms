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
 * Link one existing page or asset into a selected Active Assembly slot
 * (#5266 / parent #4530).
 *
 * <p>Confirm posts the existing slot add
 * ({@code POST /services/assembly/slot-relationships}). The slot lists
 * the item only after that call returns the same slot and dependent.
 * Cancel, a blank or missing target, a selection that is not a slot,
 * and HTTP 400/403/409 must not be reported as a link. This is not the
 * relationships-panel add, not create-in-slot, and not a move or
 * template-only change.</p>
 */

import type { PSExplorerRelationshipEdge } from "../api/contentExplorer/relationship";
import type { SlotRelationship } from "../api/contentExplorer/slotRelationshipApi";
import { parseExplorerContentId } from "../api/contentExplorer/pathItemId";
import { isActiveAssemblyRelationship } from "../api/contentExplorer/relationshipsApi";

export type LinkExistingBlock =
  | "not_slot"
  | "blank"
  | "missing"
  | "no_template"
  | "not_allowed";

export type LinkSlotSelection =
  | { kind: "slot"; slotId: number }
  | { kind: "other" }
  | null;

export type LinkExistingGate =
  | {
      ok: true;
      ownerId: number;
      dependentId: number;
      slotId: number;
      templateId: number;
    }
  | { ok: false; reason: LinkExistingBlock };

/** Slot ids that already list an Active Assembly row. Folder rows are not slots. */
export function knownActiveAssemblySlotIds(
  edges: readonly Pick<
    PSExplorerRelationshipEdge,
    "category" | "configName" | "slotId"
  >[],
): number[] {
  const ids: number[] = [];
  for (const edge of edges) {
    if (!isActiveAssemblyRelationship(edge)) {
      continue;
    }
    const slotId = Number(edge.slotId ?? 0);
    if (!Number.isFinite(slotId) || slotId <= 0 || ids.includes(slotId)) {
      continue;
    }
    ids.push(slotId);
  }
  return ids;
}

/**
 * Whether confirm may post one slot add. The target must be an existing
 * content id. The selection must be a slot that already groups Active
 * Assembly rows. A folder row, a translation row, or no selection is not
 * a slot.
 */
export function gateLinkExistingItem(input: {
  selection: LinkSlotSelection;
  targetRaw: string;
  templateId: number;
  allowedTemplateIds: readonly number[];
  knownSlotIds: readonly number[];
  ownerId: number;
}): LinkExistingGate {
  const ownerId = Number(input.ownerId);
  if (!Number.isFinite(ownerId) || ownerId <= 0) {
    return { ok: false, reason: "not_slot" };
  }
  const selection = input.selection;
  const slotId = selection?.kind === "slot" ? Number(selection.slotId) : 0;
  if (
    selection == null ||
    selection.kind !== "slot" ||
    !Number.isFinite(slotId) ||
    slotId <= 0 ||
    !input.knownSlotIds.some((id) => Number(id) === slotId)
  ) {
    return { ok: false, reason: "not_slot" };
  }
  const raw = input.targetRaw ?? "";
  if (!raw.trim()) {
    return { ok: false, reason: "blank" };
  }
  const dependentId = parseExplorerContentId(raw);
  if (dependentId == null || dependentId <= 0) {
    return { ok: false, reason: "missing" };
  }
  const templateId = Number(input.templateId);
  if (!Number.isFinite(templateId) || templateId <= 0) {
    return { ok: false, reason: "no_template" };
  }
  if (!input.allowedTemplateIds.some((id) => Number(id) === templateId)) {
    return { ok: false, reason: "not_allowed" };
  }
  return {
    ok: true,
    ownerId,
    dependentId,
    slotId,
    templateId,
  };
}

/**
 * Append one row after a successful slot add. Call this only when the
 * server returned the requested slot and dependent. Other rows are copied
 * unchanged. A non-positive relationship id leaves the list as it was.
 */
export function applyLinkedItemToSlot(
  edges: readonly PSExplorerRelationshipEdge[],
  created: SlotRelationship,
  templateName: string,
): PSExplorerRelationshipEdge[] {
  const relationshipId = Number(created.relationshipId);
  const slotId = Number(created.slotId);
  const dependentId = Number(created.dependentId);
  const templateId = Number(created.templateId);
  if (
    !(relationshipId > 0) ||
    !(slotId > 0) ||
    !(dependentId > 0) ||
    !(templateId > 0)
  ) {
    return edges.slice();
  }
  const rank = Number(created.sortRank);
  const next: PSExplorerRelationshipEdge = {
    relationshipId,
    configName: "ActiveAssembly",
    category: "rs_activeassembly",
    dependentId,
    label: String(dependentId),
    slotId,
    sortRank: Number.isInteger(rank) && rank >= 0 ? rank : 0,
    templateId,
    templateName: templateName.trim(),
  };
  const index = edges.findIndex((edge) => edge.relationshipId === relationshipId);
  if (index < 0) {
    return [...edges, next];
  }
  return edges.map((edge, i) =>
    i === index
      ? {
          ...edge,
          ...next,
          label: edge.label.trim() ? edge.label : next.label,
          templateName: next.templateName || edge.templateName,
        }
      : edge,
  );
}
