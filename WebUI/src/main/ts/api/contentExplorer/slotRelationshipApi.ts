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
 * Active Assembly slot relationship REST (add / arrange / remove).
 *
 * <p>Server: {@code /services/assembly/slot-relationships}. Replaces Data
 * Flow {@code variantlistwithslots.html} / {@code itemassembly.html}.</p>
 */

import { del, get, post } from "../client";
import { PATHS } from "../paths";

export interface SlotRelationship {
  relationshipId: number;
  ownerId: number;
  dependentId: number;
  slotId: number;
  templateId: number;
  sortRank: number;
}

export interface SlotCanvasSlot {
  slotId: number;
  name: string;
  label: string;
  items: SlotRelationship[];
}

export interface SlotCanvas {
  ownerId: number;
  templateId: number | null;
  slots: SlotCanvasSlot[];
}

export interface SlotAddRequest {
  ownerId: number;
  dependentId: number;
  slotId: number;
  templateId: number;
  folderId?: number;
  siteId?: number;
  index?: number;
}

export interface SlotAllowedChoice {
  id: number;
  name: string;
  label: string;
}

function asRecord(value: unknown): Record<string, unknown> | null {
  if (value != null && typeof value === "object" && !Array.isArray(value)) {
    return value as Record<string, unknown>;
  }
  return null;
}

function unwrapInner(payload: unknown, wrapKeys: string[]): Record<string, unknown> {
  const root = asRecord(payload);
  if (!root) {
    return {};
  }
  for (const key of wrapKeys) {
    const inner = asRecord(root[key]);
    if (inner) {
      return inner;
    }
  }
  return root;
}

function optionalNum(value: unknown, fallback = 0): number {
  const n = Number(value);
  return Number.isFinite(n) ? n : fallback;
}

function requireNum(value: unknown, field: string): number {
  if (value == null || value === "") {
    throw new Error(`Invalid slot payload: ${field} is missing`);
  }
  const n = Number(value);
  if (!Number.isFinite(n)) {
    throw new Error(`Invalid slot payload: ${field} is not numeric`);
  }
  return n;
}

export function unwrapSlotRelationship(payload: unknown): SlotRelationship {
  const inner = unwrapInner(payload, ["SlotRelationship", "slotRelationship"]);
  return {
    relationshipId: requireNum(
      inner.relationshipId ?? inner.RelationshipId,
      "relationshipId",
    ),
    ownerId: requireNum(inner.ownerId ?? inner.OwnerId, "ownerId"),
    dependentId: requireNum(inner.dependentId ?? inner.DependentId, "dependentId"),
    slotId: requireNum(inner.slotId ?? inner.SlotId, "slotId"),
    templateId: optionalNum(inner.templateId ?? inner.TemplateId),
    sortRank: optionalNum(inner.sortRank ?? inner.SortRank),
  };
}

function unwrapItems(raw: unknown): SlotRelationship[] {
  if (!Array.isArray(raw)) {
    return [];
  }
  return raw.map((row) => unwrapSlotRelationship(row));
}

export function unwrapSlotCanvas(payload: unknown): SlotCanvas {
  const inner = unwrapInner(payload, ["SlotCanvas", "slotCanvas"]);
  const slotsRaw = inner.slots ?? inner.Slots;
  const slots: SlotCanvasSlot[] = [];
  if (Array.isArray(slotsRaw)) {
    for (const row of slotsRaw) {
      const rec = unwrapInner(row, ["SlotCanvasSlot", "slotCanvasSlot"]);
      slots.push({
        slotId: requireNum(rec.slotId ?? rec.SlotId, "slotId"),
        name: String(rec.name ?? rec.Name ?? ""),
        label: String(rec.label ?? rec.Label ?? rec.name ?? rec.Name ?? ""),
        items: unwrapItems(rec.items ?? rec.Items),
      });
    }
  }
  const templateRaw = inner.templateId ?? inner.TemplateId;
  const templateId =
    templateRaw == null || templateRaw === ""
      ? null
      : optionalNum(templateRaw);
  return {
    ownerId: requireNum(inner.ownerId ?? inner.OwnerId, "ownerId"),
    templateId: templateId != null && templateId > 0 ? templateId : null,
    slots,
  };
}

export function unwrapSlotAllowedChoices(payload: unknown): SlotAllowedChoice[] {
  const inner = unwrapInner(payload, [
    "SlotAllowedChoiceList",
    "slotAllowedChoiceList",
  ]);
  const raw = inner.items ?? inner.Items;
  if (!Array.isArray(raw)) {
    return [];
  }
  return raw.map((row) => {
    const rec = unwrapInner(row, ["SlotAllowedChoice", "slotAllowedChoice"]);
    return {
      id: requireNum(rec.id ?? rec.Id, "id"),
      name: String(rec.name ?? rec.Name ?? ""),
      label: String(rec.label ?? rec.Label ?? rec.name ?? rec.Name ?? ""),
    };
  });
}

export async function fetchSlotCanvas(
  ownerId: number,
  templateId?: number | null,
): Promise<SlotCanvas> {
  const q = new URLSearchParams();
  q.set("ownerId", String(ownerId));
  if (templateId != null && templateId > 0) {
    q.set("templateId", String(templateId));
  }
  const res = await get<unknown>(`${PATHS.ASSEMBLY_SLOT_RELATIONSHIPS}/canvas?${q}`);
  return unwrapSlotCanvas(res);
}

export async function addSlotRelationship(
  request: SlotAddRequest,
): Promise<SlotRelationship> {
  const res = await post<unknown>(PATHS.ASSEMBLY_SLOT_RELATIONSHIPS, request);
  const created = unwrapSlotRelationship(res);
  if (created.relationshipId <= 0) {
    throw new Error("Add returned no relationship id");
  }
  return created;
}

export function requirePositiveRelationshipId(relationshipId: number): number {
  if (!(Number.isFinite(relationshipId) && relationshipId > 0)) {
    throw new Error("relationshipId must be a positive id");
  }
  return relationshipId;
}

export async function removeSlotRelationship(relationshipId: number): Promise<void> {
  const id = requirePositiveRelationshipId(relationshipId);
  await del<void>(`${PATHS.ASSEMBLY_SLOT_RELATIONSHIPS}/${id}`);
}

export async function moveSlotRelationship(
  relationshipId: number,
  direction: "UP" | "DOWN" | "INDEX",
  index?: number,
): Promise<void> {
  const id = requirePositiveRelationshipId(relationshipId);
  await post<void>(`${PATHS.ASSEMBLY_SLOT_RELATIONSHIPS}/${id}/move`, {
    direction,
    index,
  });
}

export async function changeSlotTemplateSlot(
  relationshipId: number,
  slotId: number,
  templateId: number,
  index?: number,
): Promise<SlotRelationship> {
  const id = requirePositiveRelationshipId(relationshipId);
  const res = await post<unknown>(
    `${PATHS.ASSEMBLY_SLOT_RELATIONSHIPS}/${id}/template-slot`,
    { slotId, templateId, index },
  );
  return unwrapSlotRelationship(res);
}

export async function fetchSlotAllowedTypes(
  slotId: number,
): Promise<SlotAllowedChoice[]> {
  const q = new URLSearchParams();
  q.set("slotId", String(slotId));
  const res = await get<unknown>(
    `${PATHS.ASSEMBLY_SLOT_RELATIONSHIPS}/allowed-types?${q}`,
  );
  return unwrapSlotAllowedChoices(res);
}

export async function fetchSlotAllowedTemplates(
  slotId: number,
  contentTypeId?: number | null,
): Promise<SlotAllowedChoice[]> {
  const q = new URLSearchParams();
  q.set("slotId", String(slotId));
  if (contentTypeId != null && contentTypeId > 0) {
    q.set("contentTypeId", String(contentTypeId));
  }
  const res = await get<unknown>(
    `${PATHS.ASSEMBLY_SLOT_RELATIONSHIPS}/allowed-templates?${q}`,
  );
  return unwrapSlotAllowedChoices(res);
}
