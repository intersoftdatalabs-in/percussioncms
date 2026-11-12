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
import type { RelatedContentRow } from "../../../main/ts/editor/editorRelatedContent";
import {
  canMoveRelatedRowToAnotherSlot,
  relatedDestinationSlotChoices,
  saveRelatedSlotMove,
} from "../../../main/ts/editor/editorRelatedSlotMove";

const slotRow: RelatedContentRow = {
  key: "slot:3:55",
  kind: "slot",
  itemId: "55",
  slotLabel: "Content",
  relationshipId: 3,
  slotId: 9,
  templateId: 7,
};

const inlineRow: RelatedContentRow = {
  key: "inline:local:88",
  kind: "inline",
  itemId: "88",
  slotLabel: "Inline",
};

const pageSlots = [
  { slotId: 9, label: "Content", name: "content" },
  { slotId: 11, label: "Sidebar", name: "sidebar" },
];

describe("canMoveRelatedRowToAnotherSlot", () => {
  it("allows slot rows with a snippet template but not inline or template-less rows", () => {
    expect(canMoveRelatedRowToAnotherSlot(slotRow)).toBe(true);
    expect(canMoveRelatedRowToAnotherSlot(inlineRow)).toBe(false);
    expect(
      canMoveRelatedRowToAnotherSlot({ ...slotRow, templateId: undefined }),
    ).toBe(false);
    expect(
      canMoveRelatedRowToAnotherSlot({ ...slotRow, relationshipId: undefined }),
    ).toBe(false);
  });
});

describe("relatedDestinationSlotChoices", () => {
  it("excludes the source slot and drops non-positive ids", () => {
    expect(relatedDestinationSlotChoices(9, pageSlots)).toEqual([
      { slotId: 11, label: "Sidebar" },
    ]);
    expect(
      relatedDestinationSlotChoices(9, [
        ...pageSlots,
        { slotId: 0, label: "", name: "" },
        { slotId: 11, label: "Sidebar", name: "sidebar" },
      ]),
    ).toEqual([{ slotId: 11, label: "Sidebar" }]);
  });
});

describe("saveRelatedSlotMove", () => {
  it("does not post for inline rows, a missing template, or a bad destination", async () => {
    const move = vi.fn();
    await expect(
      saveRelatedSlotMove({
        row: inlineRow,
        destinationSlotId: 11,
        pageSlotIds: [9, 11],
        move,
      }),
    ).resolves.toEqual({ ok: false, posted: false, reason: "not_slot" });
    await expect(
      saveRelatedSlotMove({
        row: { ...slotRow, templateId: undefined },
        destinationSlotId: 11,
        pageSlotIds: [9, 11],
        move,
      }),
    ).resolves.toEqual({ ok: false, posted: false, reason: "no_template" });
    await expect(
      saveRelatedSlotMove({
        row: slotRow,
        destinationSlotId: 0,
        pageSlotIds: [9, 11],
        move,
      }),
    ).resolves.toEqual({ ok: false, posted: false, reason: "needs_destination" });
    await expect(
      saveRelatedSlotMove({
        row: slotRow,
        destinationSlotId: 9,
        pageSlotIds: [9, 11],
        move,
      }),
    ).resolves.toEqual({ ok: false, posted: false, reason: "same" });
    await expect(
      saveRelatedSlotMove({
        row: slotRow,
        destinationSlotId: 12,
        pageSlotIds: [9, 11],
        move,
      }),
    ).resolves.toEqual({ ok: false, posted: false, reason: "not_allowed" });
    expect(move).not.toHaveBeenCalled();
  });

  it("posts the destination slot with the row's snippet template", async () => {
    const move = vi.fn().mockResolvedValue({
      relationshipId: 3,
      ownerId: 42,
      dependentId: 55,
      slotId: 11,
      templateId: 7,
      sortRank: 0,
    });
    await expect(
      saveRelatedSlotMove({
        row: slotRow,
        destinationSlotId: 11,
        pageSlotIds: [9, 11],
        move,
      }),
    ).resolves.toEqual({ ok: true, posted: true, slotId: 11, templateId: 7 });
    expect(move).toHaveBeenCalledWith(3, 11, 7);
  });

  it("keeps 400, 403, and 404 as panel failures", async () => {
    const move = vi
      .fn()
      .mockRejectedValueOnce({ status: 400, statusText: "Bad", body: {} })
      .mockRejectedValueOnce({ status: 403, statusText: "No", body: {} })
      .mockRejectedValueOnce({ status: 404, statusText: "Missing", body: {} });
    const input = {
      row: slotRow,
      destinationSlotId: 11,
      pageSlotIds: [9, 11],
      move,
    };
    await expect(saveRelatedSlotMove(input)).resolves.toMatchObject({
      ok: false,
      posted: true,
      reason: "bad_request",
    });
    await expect(saveRelatedSlotMove(input)).resolves.toMatchObject({
      ok: false,
      posted: true,
      reason: "forbidden",
    });
    await expect(saveRelatedSlotMove(input)).resolves.toMatchObject({
      ok: false,
      posted: true,
      reason: "not_found",
    });
  });
});
