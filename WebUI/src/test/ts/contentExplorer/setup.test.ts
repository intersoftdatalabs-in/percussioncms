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

import { describe, expect, it } from "vitest";
import { defaultExplorerFetchResponse, jsonResponse } from "./setup";

describe("contentExplorer Vitest fetch defaults (#4558)", () => {
  it("answers getitemdates with an empty ItemDates envelope", async () => {
    const res = defaultExplorerFetchResponse(
      "/Rhythmyx/services/itemmanagement/item/getitemdates/42",
    );
    expect(res.status).toBe(200);
    const body = (await res.json()) as {
      ItemDates: { itemId: string; startDate: string; endDate: string };
    };
    expect(body.ItemDates.startDate).toBe("");
    expect(body.ItemDates.endDate).toBe("");
  });

  it("answers setitemdates with SUCCESS", async () => {
    const res = defaultExplorerFetchResponse(
      "/Rhythmyx/services/itemmanagement/item/setitemdates",
    );
    const body = (await res.json()) as { status: string };
    expect(body.status).toBe("SUCCESS");
  });

  it("returns a fresh JSON body for unmatched URLs (no reused Response)", async () => {
    const a = defaultExplorerFetchResponse("/Rhythmyx/services/views");
    const b = defaultExplorerFetchResponse("/Rhythmyx/services/views");
    expect(a).not.toBe(b);
    expect(await a.json()).toEqual({});
    expect(await b.json()).toEqual({});
  });

  it("jsonResponse serializes objects", async () => {
    const res = jsonResponse({ ok: true }, 201);
    expect(res.status).toBe(201);
    expect(await res.json()).toEqual({ ok: true });
  });

  it("reads getitemdates from a Request url", async () => {
    const res = defaultExplorerFetchResponse(
      new Request("http://localhost/itemmanagement/item/getitemdates/7"),
    );
    const body = (await res.json()) as { ItemDates: { itemId: string } };
    expect(body.ItemDates).toBeTruthy();
  });
});
