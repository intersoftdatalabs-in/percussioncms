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

import { describe, expect, it, vi } from "vitest";
import {
  replaceSlotTemplateSlotPickerSession,
  resolveSlotTemplateSlotPick,
  settleSlotTemplateSlotPickerSession,
  slotsFromContext,
} from "../../../main/ts/assembly/slotTemplateSlotPick";

describe("slotTemplateSlotPick", () => {
  it("returns null when the slot id is missing", () => {
    expect(resolveSlotTemplateSlotPick({ slotId: 0, templateId: 4 })).toBeNull();
    expect(
      resolveSlotTemplateSlotPick({ slotId: Number.NaN, templateId: 4 }),
    ).toBeNull();
  });

  it("maps a valid slot and snippet template", () => {
    expect(resolveSlotTemplateSlotPick({ slotId: 3, templateId: 4 })).toEqual({
      slotId: 3,
      templateId: 4,
    });
  });

  it("yields templateId 0 when the template is missing", () => {
    expect(resolveSlotTemplateSlotPick({ slotId: 3, templateId: 0 })).toEqual({
      slotId: 3,
      templateId: 0,
    });
  });

  it("builds a one-slot canvas from the selected context", () => {
    expect(slotsFromContext({ ownerId: 42, slotId: 3 })).toEqual([
      { slotId: 3, name: "3", label: "3", items: [] },
    ]);
  });

  it("replace session cancels the previous waiter", () => {
    const first = vi.fn();
    const second = vi.fn();
    const slots = slotsFromContext({ ownerId: 1, slotId: 2 });
    const next = replaceSlotTemplateSlotPickerSession(
      { slot: { ownerId: 1, slotId: 2 }, slots, resolve: first },
      { slot: { ownerId: 1, slotId: 2 }, slots, resolve: second },
    );
    expect(first).toHaveBeenCalledWith(null);
    settleSlotTemplateSlotPickerSession(next, { slotId: 5, templateId: 6 });
    expect(second).toHaveBeenCalledWith({ slotId: 5, templateId: 6 });
  });
});
