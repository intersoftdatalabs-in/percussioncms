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
 * Slot + snippet-template pick for arrange change-template. Hosts open the
 * picker; dispatch POSTs changeSlotTemplateSlot.
 */

import type { SlotCanvasSlot } from "../api/contentExplorer/slotRelationshipApi";
import type { AssemblySlotContext } from "./slotContext";

export interface SlotTemplateSlotPick {
  slotId: number;
  templateId: number;
}

export interface SlotTemplateSlotPickerSession {
  slot: AssemblySlotContext;
  slots: SlotCanvasSlot[];
  resolve: (value: SlotTemplateSlotPick | null) => void;
}

export function replaceSlotTemplateSlotPickerSession(
  previous: SlotTemplateSlotPickerSession | null,
  next: SlotTemplateSlotPickerSession,
): SlotTemplateSlotPickerSession {
  if (previous && previous !== next) {
    previous.resolve(null);
  }
  return next;
}

export function settleSlotTemplateSlotPickerSession(
  session: SlotTemplateSlotPickerSession | null,
  value: SlotTemplateSlotPick | null,
): null {
  session?.resolve(value);
  return null;
}

/**
 * Validate picker fields. Missing slot is {@code null} (stay in dialog).
 * Missing snippet template yields {@code templateId: 0} so dispatch can
 * show needs-template instead of posting.
 */
export function resolveSlotTemplateSlotPick(input: {
  slotId: number;
  templateId: number;
}): SlotTemplateSlotPick | null {
  const slotId = Number(input.slotId);
  const templateId = Number(input.templateId);
  if (!Number.isFinite(slotId) || slotId <= 0) {
    return null;
  }
  return {
    slotId,
    templateId:
      Number.isFinite(templateId) && templateId > 0 ? templateId : 0,
  };
}

/** Fallback canvas list when only the selected slot is known. */
export function slotsFromContext(slot: AssemblySlotContext): SlotCanvasSlot[] {
  return [
    {
      slotId: slot.slotId,
      name: String(slot.slotId),
      label: String(slot.slotId),
      items: [],
    },
  ];
}
