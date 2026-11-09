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
  filterStagedStatusLines,
  itemStagedStatusShellHref,
  matchesStagedStatusSeverity,
  normalizeStagedStatusLog,
  spaItemStagedStatusHref,
  stagedStatusErrorMessage,
  summarizeStagedStatus,
} from "@/publishing/itemStagedStatus";

describe("normalizeStagedStatusLog", () => {
  it("unwraps envelopes into log lines", () => {
    expect(
      normalizeStagedStatusLog({
        ItemPublishingHistory: [
          { server: "prod", operation: "publish", status: "SUCCESS" },
        ],
      }),
    ).toEqual([
      expect.objectContaining({ server: "prod", status: "SUCCESS" }),
    ]);
  });

  it("returns empty for null and blank payloads", () => {
    expect(normalizeStagedStatusLog(null)).toEqual([]);
    expect(normalizeStagedStatusLog({})).toEqual([]);
  });
});

describe("summarizeStagedStatus", () => {
  it("summarizes latest status and failure count", () => {
    const summary = summarizeStagedStatus(
      normalizeStagedStatusLog([
        {
          server: "old",
          operation: "publish",
          status: "FAILURE",
          publishedDate: 100,
        },
        {
          server: "new",
          operation: "unpublish",
          status: "SUCCESS",
          publishedDate: 200,
        },
      ]),
    );
    expect(summary.totalCount).toBe(2);
    expect(summary.failureCount).toBe(1);
    expect(summary.latestStatus).toBe("SUCCESS");
    expect(summary.latestServer).toBe("new");
    expect(summary.latestDateMillis).toBe(200);
  });

  it("summarizes an empty log as zero counts", () => {
    const summary = summarizeStagedStatus([]);
    expect(summary.totalCount).toBe(0);
    expect(summary.failureCount).toBe(0);
    expect(summary.latestStatus).toBeUndefined();
  });
});

describe("filterStagedStatusLines (#5887)", () => {
  const lines = normalizeStagedStatusLog([
    { server: "a", operation: "publish", status: "SUCCESS" },
    { server: "b", operation: "publish", status: "FAILURE" },
    { server: "c", operation: "publish", status: "failure" },
    { server: "d", operation: "publish", status: "PENDING" },
  ]);

  it("passes every line through on all", () => {
    expect(filterStagedStatusLines(lines, "all")).toHaveLength(4);
  });

  it("matches failure lines case-insensitively", () => {
    const out = filterStagedStatusLines(lines, "failure");
    expect(out.map((l) => l.server)).toEqual(["b", "c"]);
  });

  it("matches success lines only", () => {
    const out = filterStagedStatusLines(lines, "success");
    expect(out.map((l) => l.server)).toEqual(["a"]);
  });

  it("leaves unknown statuses out of both buckets", () => {
    expect(matchesStagedStatusSeverity({ status: "PENDING" }, "failure")).toBe(
      false,
    );
    expect(matchesStagedStatusSeverity({ status: "PENDING" }, "success")).toBe(
      false,
    );
    expect(
      matchesStagedStatusSeverity({ status: undefined }, "success"),
    ).toBe(false);
  });

  it("filters an empty log to an empty log", () => {
    expect(filterStagedStatusLines([], "failure")).toEqual([]);
  });
});

describe("stagedStatusErrorMessage", () => {
  it.each([400, 403, 404, 409, 500])(
    "maps HTTP %i to a mapped error, never empty status",
    (status) => {
      const text = stagedStatusErrorMessage({
        status,
        statusText: `HTTP ${status}`,
      });
      expect(text.trim().length).toBeGreaterThan(0);
    },
  );

  it("maps 409 to the staged-status conflict text", () => {
    expect(
      stagedStatusErrorMessage({ status: 409, statusText: "Conflict" }),
    ).toMatch(/conflict/i);
  });

  it("maps 403 to the forbidden text", () => {
    expect(
      stagedStatusErrorMessage({ status: 403, statusText: "Forbidden" }),
    ).toMatch(/forbidden/i);
  });
});

describe("itemStagedStatusShellHref", () => {
  it("deep-links the site workspace with the item id", () => {
    expect(
      itemStagedStatusShellHref({ siteId: "12", itemId: "42" }),
    ).toBe("/cm/app/publish/sites?siteId=12&itemId=42");
    expect(spaItemStagedStatusHref({ itemId: "42" })).toContain(
      "entry=publish",
    );
  });
});
