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
 * Create one new item and link it into a selected Active Assembly slot
 * (#5267 / parent #4530).
 *
 * <p>Confirm posts {@code POST /services/itemmanagement/item/create}, then
 * the existing slot add ({@code POST /services/assembly/slot-relationships}).
 * The slot lists the item only after the link returns the same slot and
 * dependent. The editor opens only after that. Cancel, a missing content
 * type, folder, or snippet template, a selection that is not a slot, and
 * HTTP 400/403/409 must not be reported as created in the slot. This is
 * not folder Create Page or Create Asset, and not link-existing.</p>
 */

import type { PSExplorerRelationshipEdge } from "../api/contentExplorer/relationship";
import type { SlotAddRequest, SlotRelationship } from "../api/contentExplorer/slotRelationshipApi";
import { parseExplorerContentId } from "../api/contentExplorer/pathItemId";
import type { ItemCreateRequest, ItemCreateResult } from "../editor/itemCreateApi";
import {
  applyLinkedItemToSlot,
  type LinkSlotSelection,
} from "./linkExistingItemToSlot";

export type CreateInSlotBlock =
  | "not_slot"
  | "no_type"
  | "type_not_allowed"
  | "no_folder"
  | "no_template"
  | "not_allowed";

export type CreateInSlotGate =
  | {
      ok: true;
      ownerId: number;
      slotId: number;
      contentType: string;
      folderPath: string;
      templateId: number;
    }
  | { ok: false; reason: CreateInSlotBlock };

export type CreateInSlotRun =
  | {
      ok: true;
      contentId: number;
      linked: SlotRelationship;
      templateName: string;
      label: string;
    }
  | { ok: false; phase: "create" | "link"; error?: unknown };

function distinctTrimmed(values: readonly string[]): string[] {
  const out: string[] = [];
  for (const raw of values) {
    const text = String(raw ?? "").trim();
    if (!text || out.includes(text)) {
      continue;
    }
    out.push(text);
  }
  return out;
}

/**
 * Whether confirm may create and link. A blank content type, folder, or
 * snippet template does not write. The selection must be a slot that
 * already groups Active Assembly rows.
 */
export function gateCreateItemInSlot(input: {
  selection: LinkSlotSelection;
  contentType: string;
  folderPath: string;
  templateId: number;
  allowedTemplateIds: readonly number[];
  allowedContentTypes: readonly string[];
  knownSlotIds: readonly number[];
  ownerId: number;
}): CreateInSlotGate {
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
  const contentType = (input.contentType ?? "").trim();
  if (!contentType) {
    return { ok: false, reason: "no_type" };
  }
  const allowedTypes = distinctTrimmed(input.allowedContentTypes);
  if (!allowedTypes.includes(contentType)) {
    return { ok: false, reason: "type_not_allowed" };
  }
  const folderPath = (input.folderPath ?? "").trim();
  if (!folderPath) {
    return { ok: false, reason: "no_folder" };
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
    slotId,
    contentType,
    folderPath,
    templateId,
  };
}

/**
 * Create, then link. A create failure does not call the slot add. A link
 * failure or a response that is not the requested slot and item is not
 * success. Does not open the editor.
 */
export async function runCreateItemInSlot(input: {
  ownerId: number;
  slotId: number;
  contentType: string;
  folderPath: string;
  templateId: number;
  templateName: string;
  create: (
    request: ItemCreateRequest,
  ) => Promise<Pick<ItemCreateResult, "itemId" | "name">>;
  link: (request: SlotAddRequest) => Promise<SlotRelationship>;
}): Promise<CreateInSlotRun> {
  let created: Pick<ItemCreateResult, "itemId" | "name">;
  try {
    created = await input.create({
      contentType: input.contentType,
      folderPath: input.folderPath,
    });
  } catch (error) {
    return { ok: false, phase: "create", error };
  }
  const contentId = parseExplorerContentId(created.itemId);
  if (contentId == null || contentId <= 0) {
    return { ok: false, phase: "create" };
  }
  let linked: SlotRelationship;
  try {
    linked = await input.link({
      ownerId: input.ownerId,
      dependentId: contentId,
      slotId: input.slotId,
      templateId: input.templateId,
    });
  } catch (error) {
    return { ok: false, phase: "link", error };
  }
  if (
    Number(linked.slotId) !== input.slotId ||
    Number(linked.dependentId) !== contentId ||
    !(Number(linked.relationshipId) > 0)
  ) {
    return { ok: false, phase: "link" };
  }
  const name = String(created.name ?? "").trim();
  return {
    ok: true,
    contentId,
    linked,
    templateName: input.templateName,
    label: name || String(contentId),
  };
}

/**
 * Append the created row only after {@link runCreateItemInSlot} succeeded.
 * Other rows are copied unchanged.
 */
export function applyCreatedItemToSlot(
  edges: readonly PSExplorerRelationshipEdge[],
  created: SlotRelationship,
  templateName: string,
  label: string,
): PSExplorerRelationshipEdge[] {
  const next = applyLinkedItemToSlot(edges, created, templateName);
  const name = label.trim();
  const relationshipId = Number(created.relationshipId);
  if (!name || !(relationshipId > 0)) {
    return next;
  }
  return next.map((edge) =>
    edge.relationshipId === relationshipId ? { ...edge, label: name } : edge,
  );
}
