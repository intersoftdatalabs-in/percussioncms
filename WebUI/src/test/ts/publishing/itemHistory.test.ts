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

import { describe, expect, it } from "vitest";
import {
  itemHistoryShellHref,
  itemPubHistoryUrl,
  normalizeItemPublishingHistory,
  sortHistoryNewestFirst,
  spaItemHistoryHref,
} from "@/publishing/itemHistory";

describe("normalizeItemPublishingHistory", () => {
  it("returns empty for null, empty array, and empty wrap", () => {
    expect(normalizeItemPublishingHistory(null)).toEqual([]);
    expect(normalizeItemPublishingHistory([])).toEqual([]);
    expect(normalizeItemPublishingHistory({ ItemPublishingHistory: [] })).toEqual(
      [],
    );
  });

  it("unwraps a JAXB list wrap and a single object wrap", () => {
    const row = {
      server: "prod",
      location: "/index.html",
      revisionId: 2,
      publishedDate: 1_700_000_000_000,
      operation: "publish",
      status: "SUCCESS",
    };
    expect(
      normalizeItemPublishingHistory({ ItemPublishingHistory: [row] }),
    ).toHaveLength(1);
    expect(
      normalizeItemPublishingHistory({ ItemPublishingHistory: row }),
    ).toEqual([
      expect.objectContaining({ server: "prod", status: "SUCCESS" }),
    ]);
  });

  it("accepts a bare array of rows", () => {
    const list = normalizeItemPublishingHistory([
      { server: "a", status: "SUCCESS", operation: "publish" },
      { server: "b", status: "FAILURE", errorMessage: "boom" },
    ]);
    expect(list).toHaveLength(2);
    expect(list[1].errorMessage).toBe("boom");
  });
});

describe("sortHistoryNewestFirst", () => {
  it("orders by publishedDate descending", () => {
    const sorted = sortHistoryNewestFirst([
      { server: "old", publishedDate: 100 },
      { server: "new", publishedDate: 300 },
      { server: "mid", publishedDate: 200 },
    ]);
    expect(sorted.map((r) => r.server)).toEqual(["new", "mid", "old"]);
  });
});

describe("item history hrefs", () => {
  it("builds pubhistory URL and status/logs deep links", () => {
    expect(itemPubHistoryUrl("16777215-101-9", "/services")).toBe(
      "/services/itemmanagement/item/pubhistory/16777215-101-9",
    );
    expect(itemHistoryShellHref({ section: "logs", itemId: "42" })).toBe(
      "/cm/app/publish/logs?itemId=42",
    );
    expect(spaItemHistoryHref({ section: "status", itemId: "42" })).toContain(
      "entry=publish",
    );
    expect(spaItemHistoryHref({ section: "status", itemId: "42" })).toContain(
      "section=status",
    );
    expect(spaItemHistoryHref({ section: "status", itemId: "42" })).toContain(
      "itemId=42",
    );
    expect(itemHistoryShellHref({ section: "logs", itemId: "a/b" })).toBe(
      "/cm/app/publish/logs",
    );
  });
});
