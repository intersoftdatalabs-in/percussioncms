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
  replaceSlotCreatePickerSession,
  resolveSlotCreatePick,
  settleSlotCreatePickerSession,
} from "../../../main/ts/assembly/slotCreatePick";

describe("slotCreatePick", () => {
  it("returns null when type or folder is missing", () => {
    expect(
      resolveSlotCreatePick({
        contentType: "",
        folderPath: "/Sites/A",
        snippetTemplateId: 4,
      }),
    ).toBeNull();
    expect(
      resolveSlotCreatePick({
        contentType: "percRichText",
        folderPath: "  ",
        snippetTemplateId: 4,
      }),
    ).toBeNull();
  });

  it("maps a valid type, folder, and snippet template", () => {
    expect(
      resolveSlotCreatePick({
        contentType: " percRichText ",
        folderPath: " /Sites/A ",
        snippetTemplateId: 4,
        templateId: "7",
      }),
    ).toEqual({
      contentType: "percRichText",
      folderPath: "/Sites/A",
      snippetTemplateId: 4,
      templateId: "7",
    });
  });

  it("yields snippetTemplateId 0 when the template is missing", () => {
    expect(
      resolveSlotCreatePick({
        contentType: "percRichText",
        folderPath: "/Sites/A",
        snippetTemplateId: 0,
      }),
    ).toEqual({
      contentType: "percRichText",
      folderPath: "/Sites/A",
      snippetTemplateId: 0,
    });
  });

  it("replace session cancels the previous waiter", () => {
    const first = vi.fn();
    const second = vi.fn();
    const next = replaceSlotCreatePickerSession(
      { slot: { ownerId: 1, slotId: 2 }, resolve: first },
      { slot: { ownerId: 1, slotId: 2 }, resolve: second },
    );
    expect(first).toHaveBeenCalledWith(null);
    settleSlotCreatePickerSession(next, {
      contentType: "percRichText",
      folderPath: "/Sites/A",
      snippetTemplateId: 4,
    });
    expect(second).toHaveBeenCalledWith({
      contentType: "percRichText",
      folderPath: "/Sites/A",
      snippetTemplateId: 4,
    });
  });
});
