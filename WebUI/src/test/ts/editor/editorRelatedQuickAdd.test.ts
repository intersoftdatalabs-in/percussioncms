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
  relatedQuickAddAttachReason,
  relatedQuickAddErrorReason,
  saveRelatedQuickAdd,
} from "../../../main/ts/editor/editorRelatedQuickAdd";

const BASE = {
  ownerId: 42,
  slotId: 9,
  templateId: 7,
  contentType: "percPage",
  folderPath: "/Sites/Enterprise",
  name: "New page",
};

describe("relatedQuickAddErrorReason", () => {
  it("maps 400, 403, 404, and 409 to named reasons", () => {
    expect(relatedQuickAddErrorReason({ status: 400 })).toBe("bad_request");
    expect(relatedQuickAddErrorReason({ status: 403 })).toBe("forbidden");
    expect(relatedQuickAddErrorReason({ status: 404 })).toBe("not_found");
    expect(relatedQuickAddErrorReason({ status: 409 })).toBe("conflict");
    expect(relatedQuickAddErrorReason({ status: 500 })).toBe("failed");
  });
});

describe("relatedQuickAddAttachReason", () => {
  it("maps attach 409 to conflict and keeps the insert map otherwise", () => {
    expect(relatedQuickAddAttachReason({ status: 409 })).toBe("conflict");
    expect(relatedQuickAddAttachReason({ status: 400 })).toBe("bad_request");
    expect(relatedQuickAddAttachReason({ status: 403 })).toBe("forbidden");
  });
});

describe("saveRelatedQuickAdd", () => {
  it("refuses NUL names, NUL folders, and incomplete input before posting", async () => {
    const create = vi.fn();
    const insert = vi.fn();
    await expect(
      saveRelatedQuickAdd({ ...BASE, name: "bad\u0000name", create, insert }),
    ).resolves.toEqual({
      ok: false,
      posted: false,
      stage: "validate",
      reason: "name_nul",
    });
    await expect(
      saveRelatedQuickAdd({
        ...BASE,
        folderPath: "/Sites\u0000/x",
        create,
        insert,
      }),
    ).resolves.toEqual({
      ok: false,
      posted: false,
      stage: "validate",
      reason: "folder_nul",
    });
    await expect(
      saveRelatedQuickAdd({ ...BASE, contentType: "  ", create, insert }),
    ).resolves.toEqual({
      ok: false,
      posted: false,
      stage: "validate",
      reason: "incomplete",
    });
    expect(create).not.toHaveBeenCalled();
    expect(insert).not.toHaveBeenCalled();
  });

  it("creates the item then attaches it to the selected slot", async () => {
    const create = vi.fn().mockResolvedValue({
      itemId: "77",
      folderPath: "/Sites/Enterprise",
      name: "New page",
      contentType: "percPage",
    });
    const insert = vi.fn().mockResolvedValue({
      relationshipId: 3,
      ownerId: 42,
      dependentId: 77,
      slotId: 9,
      templateId: 7,
      sortRank: 0,
    });
    await expect(
      saveRelatedQuickAdd({ ...BASE, create, insert }),
    ).resolves.toEqual({ ok: true, posted: true, itemId: "77" });
    expect(create).toHaveBeenCalledWith({
      contentType: "percPage",
      folderPath: "/Sites/Enterprise",
      name: "New page",
    });
    expect(insert).toHaveBeenCalledWith({
      ownerId: 42,
      dependentId: 77,
      slotId: 9,
      templateId: 7,
    });
  });

  it("never reaches attach when create fails", async () => {
    const create = vi
      .fn()
      .mockRejectedValueOnce({ status: 409 })
      .mockRejectedValueOnce({ status: 403 });
    const insert = vi.fn();
    await expect(
      saveRelatedQuickAdd({ ...BASE, create, insert }),
    ).resolves.toMatchObject({
      ok: false,
      posted: true,
      stage: "create",
      reason: "conflict",
    });
    await expect(
      saveRelatedQuickAdd({ ...BASE, create, insert }),
    ).resolves.toMatchObject({
      ok: false,
      posted: true,
      stage: "create",
      reason: "forbidden",
    });
    expect(insert).not.toHaveBeenCalled();
  });

  it("reports attach failures with the created item id", async () => {
    const create = vi.fn().mockResolvedValue({
      itemId: "77",
      folderPath: "/Sites/Enterprise",
      name: "New page",
      contentType: "percPage",
    });
    const insert = vi.fn().mockRejectedValue({ status: 400 });
    await expect(
      saveRelatedQuickAdd({ ...BASE, create, insert }),
    ).resolves.toEqual({
      ok: false,
      posted: true,
      stage: "attach",
      reason: "bad_request",
      itemId: "77",
    });
  });
});
