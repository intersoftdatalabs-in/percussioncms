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

function num(value: unknown): number {
  const n = Number(value);
  return Number.isFinite(n) ? n : 0;
}

export function unwrapSlotRelationship(payload: unknown): SlotRelationship {
  const inner = unwrapInner(payload, ["SlotRelationship", "slotRelationship"]);
  return {
    relationshipId: num(inner.relationshipId ?? inner.RelationshipId),
    ownerId: num(inner.ownerId ?? inner.OwnerId),
    dependentId: num(inner.dependentId ?? inner.DependentId),
    slotId: num(inner.slotId ?? inner.SlotId),
    templateId: num(inner.templateId ?? inner.TemplateId),
    sortRank: num(inner.sortRank ?? inner.SortRank),
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
        slotId: num(rec.slotId ?? rec.SlotId),
        name: String(rec.name ?? rec.Name ?? ""),
        label: String(rec.label ?? rec.Label ?? rec.name ?? rec.Name ?? ""),
        items: unwrapItems(rec.items ?? rec.Items),
      });
    }
  }
  const templateRaw = inner.templateId ?? inner.TemplateId;
  const templateId =
    templateRaw == null || templateRaw === "" ? null : num(templateRaw) || null;
  return {
    ownerId: num(inner.ownerId ?? inner.OwnerId),
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
      id: num(rec.id ?? rec.Id),
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

export async function removeSlotRelationship(relationshipId: number): Promise<void> {
  await del<void>(`${PATHS.ASSEMBLY_SLOT_RELATIONSHIPS}/${relationshipId}`);
}

export async function moveSlotRelationship(
  relationshipId: number,
  direction: "UP" | "DOWN" | "INDEX",
  index?: number,
): Promise<void> {
  await post<void>(`${PATHS.ASSEMBLY_SLOT_RELATIONSHIPS}/${relationshipId}/move`, {
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
  const res = await post<unknown>(
    `${PATHS.ASSEMBLY_SLOT_RELATIONSHIPS}/${relationshipId}/template-slot`,
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
