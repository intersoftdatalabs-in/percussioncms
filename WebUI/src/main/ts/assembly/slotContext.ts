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
 * Slot / snippet selection from the Active Assembly canvas. Explorer
 * folder browse has no slot — do not invent Arrange_* from a folder.
 */

export const SLOT_ACTION_NAMES = new Set([
  "slot_add",
  "slot_create",
  "arrange",
  "arrange_moveupleft",
  "arrange_movedownright",
  "arrange_changetemplateslot",
  "arrange_remove",
  "change_template",
  "paste_as_link_to_slot",
  "move_to_slot",
]);

export interface AssemblySlotContext {
  ownerId: number;
  slotId: number;
  /** Present when a snippet (relationship) is selected. */
  relationshipId?: number | null;
  snippetTemplateId?: number | null;
  ownerTemplateId?: number | null;
  folderPath?: string | null;
}

export function isSlotActionName(name: string): boolean {
  return SLOT_ACTION_NAMES.has(name);
}

export function slotContextHasSlot(
  slot: AssemblySlotContext | null | undefined,
): slot is AssemblySlotContext {
  return slot != null && slot.ownerId > 0 && slot.slotId > 0;
}

export function slotContextHasRelationship(
  slot: AssemblySlotContext | null | undefined,
): boolean {
  return slotContextHasSlot(slot) && (slot.relationshipId ?? 0) > 0;
}
