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
  parseOptionalFolderId,
  replaceSlotDependentPickerSession,
  resolveSlotDependentPick,
  settleSlotDependentPickerSession,
} from "../../../main/ts/assembly/slotDependentPick";

describe("slotDependentPick", () => {
  it("returns null for an empty Content Browser selection", async () => {
    const load = vi.fn();
    const picked = await resolveSlotDependentPick({ items: [] }, 3, load);
    expect(picked).toBeNull();
    expect(load).not.toHaveBeenCalled();
  });

  it("maps the first item and first allowed template", async () => {
    const load = vi.fn().mockResolvedValue([
      { id: 4, name: "rffSnTitle", label: "Title" },
    ]);
    const picked = await resolveSlotDependentPick(
      { items: [{ id: "7", path: "/Sites/A/x", name: "x" }] },
      3,
      load,
      11,
    );
    expect(picked).toEqual({ contentId: 7, templateId: 4, folderId: 11 });
    expect(load).toHaveBeenCalledWith(3, null);
  });

  it("passes a content-type hint and yields templateId 0 when none allowed", async () => {
    const load = vi.fn().mockResolvedValue([]);
    const picked = await resolveSlotDependentPick(
      {
        items: [
          {
            id: "1-101-42",
            path: "/Sites/A/x",
            contentTypeIds: ["310"],
          },
        ],
      },
      3,
      load,
    );
    expect(picked).toEqual({ contentId: 42, templateId: 0 });
    expect(load).toHaveBeenCalledWith(3, 310);
  });

  it("does not throw when allowed-template load fails", async () => {
    const load = vi.fn().mockRejectedValue(new Error("down"));
    const picked = await resolveSlotDependentPick(
      { items: [{ id: "9", path: "/Sites/A/x" }] },
      3,
      load,
    );
    expect(picked).toEqual({ contentId: 9, templateId: 0 });
  });

  it("parseOptionalFolderId ignores non-positive values", () => {
    expect(parseOptionalFolderId(8)).toBe(8);
    expect(parseOptionalFolderId("0")).toBeUndefined();
    expect(parseOptionalFolderId(undefined)).toBeUndefined();
  });

  it("replace session cancels the previous waiter", () => {
    const first = vi.fn();
    const second = vi.fn();
    const next = replaceSlotDependentPickerSession(
      { slot: { ownerId: 1, slotId: 2 }, resolve: first },
      { slot: { ownerId: 1, slotId: 2 }, resolve: second },
    );
    expect(first).toHaveBeenCalledWith(null);
    settleSlotDependentPickerSession(next, { contentId: 1, templateId: 2 });
    expect(second).toHaveBeenCalledWith({ contentId: 1, templateId: 2 });
  });
});
