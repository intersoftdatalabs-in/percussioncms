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
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import { afterEach, describe, expect, it, vi } from "vitest";
import {
  createNewCopy,
  createPromotableVersion,
  unwrapItemCopyResult,
} from "../../../../main/ts/api/contentExplorer/itemCopyApi";

afterEach(() => {
  vi.restoreAllMocks();
});

describe("itemCopyApi", () => {
  it("unwraps ItemCopyResult envelope", () => {
    const result = unwrapItemCopyResult({
      ItemCopyResult: { itemId: "99", folderPath: "//Sites/Demo", promotable: true },
    });
    expect(result.itemId).toBe("99");
    expect(result.folderPath).toBe("//Sites/Demo");
    expect(result.promotable).toBe(true);
  });

  it("createNewCopy POSTs the item id", async () => {
    vi.spyOn(global, "fetch").mockResolvedValueOnce(
      new Response(JSON.stringify({ itemId: "99", folderPath: "//Sites/Demo" }), {
        status: 200,
        headers: { "Content-Type": "application/json" },
      }),
    );
    const result = await createNewCopy("42");
    expect(result.itemId).toBe("99");
    const init = global.fetch.mock.calls[0]?.[1] as RequestInit;
    expect(init.method).toBe("POST");
    expect(String(global.fetch.mock.calls[0]?.[0] ?? "")).toContain(
      "itemmanagement/item/newCopy/42",
    );
  });

  it("createPromotableVersion POSTs the promotable path", async () => {
    vi.spyOn(global, "fetch").mockResolvedValueOnce(
      new Response(JSON.stringify({ itemId: "100", promotable: true }), {
        status: 200,
        headers: { "Content-Type": "application/json" },
      }),
    );
    const result = await createPromotableVersion("1-101-42");
    expect(result.promotable).toBe(true);
    expect(String(global.fetch.mock.calls[0]?.[0] ?? "")).toContain(
      "itemmanagement/item/promotableVersion/1-101-42",
    );
  });
});
