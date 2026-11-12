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

import { afterEach, describe, expect, it, vi } from "vitest";
import * as client from "../../../main/ts/api/client";
import {
  checkinEditorItem,
  checkoutEditorItem,
  fetchItemEditorFields,
  saveItemEditorFields,
  visibleEditorRevision,
} from "../../../main/ts/editor/itemFieldsApi";

describe("visibleEditorRevision", () => {
  it("keeps positive integer revisions", () => {
    expect(visibleEditorRevision(1)).toBe(1);
    expect(visibleEditorRevision(7)).toBe(7);
  });

  it("accepts numeric strings", () => {
    expect(visibleEditorRevision("5")).toBe(5);
    expect(visibleEditorRevision("  9  ")).toBe(9);
  });

  it("omits zero, blank, and non-integer revisions", () => {
    expect(visibleEditorRevision(0)).toBeUndefined();
    expect(visibleEditorRevision(-3)).toBeUndefined();
    expect(visibleEditorRevision(2.5)).toBeUndefined();
    expect(visibleEditorRevision("")).toBeUndefined();
    expect(visibleEditorRevision("   ")).toBeUndefined();
    expect(visibleEditorRevision("tip")).toBeUndefined();
    expect(visibleEditorRevision("4.2")).toBeUndefined();
  });

  it("omits nullish and non-numeric values", () => {
    expect(visibleEditorRevision(undefined)).toBeUndefined();
    expect(visibleEditorRevision(null)).toBeUndefined();
    expect(visibleEditorRevision({})).toBeUndefined();
    expect(visibleEditorRevision(Number.NaN)).toBeUndefined();
  });
});

describe("fetchItemEditorFields", () => {
  afterEach(() => {
    vi.restoreAllMocks();
  });

  it("GETs the fields path and unwraps the Jackson envelope", async () => {
    const get = vi.spyOn(client, "get").mockResolvedValue({
      ItemEditorFields: {
        contentId: "42",
        contentType: "percPage",
        name: "Home",
        checkoutUser: "admin",
        revision: 7,
        fields: [{ name: "sys_title", value: "Home" }],
      },
    });
    await expect(fetchItemEditorFields("42")).resolves.toEqual({
      contentId: "42",
      contentType: "percPage",
      name: "Home",
      checkoutUser: "admin",
      revision: 7,
      fields: [{ name: "sys_title", value: "Home" }],
    });
    expect(get).toHaveBeenCalledWith(
      expect.stringContaining("/itemmanagement/item/fields/42"),
    );
  });

  it("drops unnamed rows and omits a zero revision", async () => {
    vi.spyOn(client, "get").mockResolvedValue({
      itemEditorFields: {
        ContentId: "43",
        ContentType: "percPage",
        Name: "Page",
        CheckoutUser: "",
        Revision: 0,
        Fields: [{ Name: "  ", Value: "x" }, { name: "sys_title", value: "Page" }],
      },
    });
    await expect(fetchItemEditorFields("43")).resolves.toEqual({
      contentId: "43",
      contentType: "percPage",
      name: "Page",
      checkoutUser: "",
      fields: [{ name: "sys_title", value: "Page" }],
    });
  });
});

describe("saveItemEditorFields", () => {
  afterEach(() => {
    vi.restoreAllMocks();
  });

  it("PUTs the payload and unwraps the saved fields", async () => {
    const put = vi.spyOn(client, "put").mockResolvedValue({
      ItemEditorFields: {
        contentId: "42",
        contentType: "percPage",
        name: "Home",
        checkoutUser: "admin",
        revision: 8,
        fields: [{ name: "sys_title", value: "Updated" }],
      },
    });
    const payload = {
      contentId: "42",
      contentType: "percPage",
      name: "Home",
      checkoutUser: "admin",
      fields: [{ name: "sys_title", value: "Updated" }],
    };
    await expect(saveItemEditorFields("42", payload)).resolves.toEqual({
      ...payload,
      revision: 8,
    });
    expect(put).toHaveBeenCalledWith(
      expect.stringContaining("/itemmanagement/item/fields/42"),
      payload,
    );
  });
});

describe("checkoutEditorItem", () => {
  afterEach(() => {
    vi.restoreAllMocks();
  });

  it("POSTs the lock path and unwraps the user info", async () => {
    const post = vi.spyOn(client, "post").mockResolvedValue({
      EditorItemLockInfo: {
        itemName: "Home",
        checkOutUser: "admin",
        currentUser: "admin",
        assignmentType: "Assignee",
      },
    });
    await expect(checkoutEditorItem("42")).resolves.toEqual({
      itemName: "Home",
      checkOutUser: "admin",
      currentUser: "admin",
      assignmentType: "Assignee",
    });
    expect(post).toHaveBeenCalledWith(
      expect.stringContaining("/rest/editor/items/42/checkout"),
      {},
    );
  });
});

describe("checkinEditorItem", () => {
  afterEach(() => {
    vi.restoreAllMocks();
  });

  it("POSTs without a comment query when the comment is blank", async () => {
    const post = vi.spyOn(client, "post").mockResolvedValue(undefined);
    await checkinEditorItem("42", "   ");
    expect(post).toHaveBeenCalledWith(
      expect.stringMatching(/\/rest\/editor\/items\/42\/checkin$/),
      {},
    );
  });

  it("POSTs a trimmed comment query when provided", async () => {
    const post = vi.spyOn(client, "post").mockResolvedValue(undefined);
    await checkinEditorItem("42", "  done  ");
    expect(post).toHaveBeenCalledWith(
      expect.stringContaining("/rest/editor/items/42/checkin?comment=done"),
      {},
    );
  });
});
