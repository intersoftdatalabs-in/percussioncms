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
 * Change the snippet template of one owned Active Assembly relationship
 * on the Explorer relationships list (#5219 / parent #4530).
 *
 * <p>Confirm posts the existing slot-relationship template API and keeps
 * the row's slot. Cancel, a folder row, an empty template choice, and
 * HTTP 400/403/409 must not be reported as a new template. This is not
 * the editor Related content template control and not a slot change.</p>
 */

import type { PSExplorerRelationshipEdge } from "../api/contentExplorer/relationship";
import {
  isActiveAssemblyRelationship,
  isFolderRelationshipCategory,
} from "../api/contentExplorer/relationshipsApi";

export type RelationshipTemplateBlock =
  | "empty"
  | "folder"
  | "not_assembly"
  | "no_slot"
  | "same"
  | "not_allowed";

export type RelationshipTemplateGate =
  | {
      ok: true;
      relationshipId: number;
      slotId: number;
      templateId: number;
    }
  | { ok: false; reason: RelationshipTemplateBlock };

export interface RelationshipTemplateWrite {
  relationshipId: number;
  slotId: number;
  templateId: number;
}

/** Active Assembly rows with a slot can open the template dialog. */
export function canChangeRelationshipSnippetTemplate(
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

/**
 * Text the list shows for the row's snippet template. A name wins.
 * Otherwise a positive id is shown. An empty result is not a success claim.
 */
export function relationshipTemplateLabel(
  edge: Pick<PSExplorerRelationshipEdge, "templateId" | "templateName">,
): string {
  const name = (edge.templateName ?? "").trim();
  if (name) {
    return name;
  }
  const templateId = Number(edge.templateId ?? 0);
  if (Number.isFinite(templateId) && templateId > 0) {
    return String(templateId);
  }
  return "";
}

/**
 * Whether confirm may post one template change. The slot is the row's
 * current slot. The chosen id must be a different allowed template.
 */
export function gateRelationshipSnippetTemplate(input: {
  edge: PSExplorerRelationshipEdge | null | undefined;
  templateId: number;
  allowedIds: readonly number[];
}): RelationshipTemplateGate {
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
  const slotId = Number(edge.slotId ?? 0);
  if (!(relationshipId > 0) || !(slotId > 0)) {
    return { ok: false, reason: "no_slot" };
  }
  const templateId = Number(input.templateId);
  if (!Number.isFinite(templateId) || templateId <= 0) {
    return { ok: false, reason: "empty" };
  }
  const current = Number(edge.templateId ?? 0);
  if (current > 0 && templateId === current) {
    return { ok: false, reason: "same" };
  }
  if (!input.allowedIds.some((id) => Number(id) === templateId)) {
    return { ok: false, reason: "not_allowed" };
  }
  return { ok: true, relationshipId, slotId, templateId };
}

/**
 * Replace one row after a successful template-slot write. The slot stays
 * the slot that was posted. Other rows are copied unchanged.
 */
export function applyRelationshipSnippetTemplate(
  edges: readonly PSExplorerRelationshipEdge[],
  relationshipId: number,
  updated: RelationshipTemplateWrite,
  templateName: string,
): PSExplorerRelationshipEdge[] {
  const name = templateName.trim();
  return edges.map((edge) => {
    if (edge.relationshipId !== relationshipId) {
      return edge;
    }
    const nextId =
      Number.isFinite(updated.relationshipId) && updated.relationshipId > 0
        ? updated.relationshipId
        : edge.relationshipId;
    const nextTemplate =
      Number.isFinite(updated.templateId) && updated.templateId > 0
        ? updated.templateId
        : Number(edge.templateId ?? 0);
    const slotId = Number(edge.slotId ?? 0);
    return {
      ...edge,
      relationshipId: nextId,
      slotId: slotId > 0 ? slotId : updated.slotId,
      templateId: nextTemplate,
      templateName: name || edge.templateName,
    };
  });
}
