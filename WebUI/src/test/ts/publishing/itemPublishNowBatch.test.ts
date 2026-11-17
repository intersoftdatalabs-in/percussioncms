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
  formatPublishNowBatchDetail,
  parsePublishNowItemIds,
  publishNowBatchFailureMessage,
  runPublishNowBatch,
} from "@/publishing/itemPublishNowBatch";
import type { PSPathItem } from "@/api/contentExplorer/types";

describe("parsePublishNowItemIds", () => {
  it("returns [] for nullish input", () => {
    expect(parsePublishNowItemIds(null)).toEqual([]);
    expect(parsePublishNowItemIds(undefined)).toEqual([]);
  });

  it("splits commas, spaces, and semicolons and dedupes in order", () => {
    expect(parsePublishNowItemIds("42, 43 44;42")).toEqual(["42", "43", "44"]);
  });

  it("drops blank tokens", () => {
    expect(parsePublishNowItemIds("  , , ")).toEqual([]);
  });
});

describe("formatPublishNowBatchDetail", () => {
  it("names each failure as id (reason)", () => {
    expect(
      formatPublishNowBatchDetail([
        { id: "42", message: "boom" },
        { id: "43", message: "bam" },
      ]),
    ).toBe("42 (boom); 43 (bam)");
  });

  it("is empty with no failures", () => {
    expect(formatPublishNowBatchDetail([])).toBe("");
  });
});

describe("publishNowBatchFailureMessage", () => {
  it("maps 403 to forbidden", () => {
    expect(
      publishNowBatchFailureMessage({
        status: 403,
        statusText: "Forbidden",
        body: null,
      }),
    ).toMatch(/forbidden/i);
  });

  it("maps 400 to bad config", () => {
    expect(
      publishNowBatchFailureMessage({
        status: 400,
        statusText: "Bad Request",
        body: null,
      }),
    ).toMatch(/bad|invalid|config/i);
  });

  it("maps 404 to not found", () => {
    expect(
      publishNowBatchFailureMessage({
        status: 404,
        statusText: "Not Found",
        body: null,
      }),
    ).toMatch(/not found/i);
  });

  it("maps 409 to the checkout conflict", () => {
    expect(
      publishNowBatchFailureMessage({
        status: 409,
        statusText: "Conflict",
        body: null,
      }),
    ).toMatch(/editing this/i);
  });

  it("keeps the server detail when the body names it", () => {
    expect(
      publishNowBatchFailureMessage({
        status: 409,
        statusText: "Conflict",
        body: { message: "checked out" },
      }),
    ).toBe("checked out");
  });

  it("maps conflict text to the checkout message", () => {
    expect(publishNowBatchFailureMessage(new Error("CONFLICT"))).toMatch(
      /editing this/i,
    );
  });

  it("keeps unknown server text", () => {
    expect(publishNowBatchFailureMessage(new Error("teapot exploded"))).toBe(
      "teapot exploded",
    );
  });
});

describe("runPublishNowBatch", () => {
  const item = (id: string): PSPathItem =>
    ({ id, path: `/Sites/Demo/${id}` }) as PSPathItem;

  it("publishes every id through the page route", async () => {
    const publish = vi.fn(async () => true);
    const batch = await runPublishNowBatch(["42", "43"], "page", publish);
    expect(batch).toEqual({ total: 2, published: ["42", "43"], failures: [] });
    expect(publish).toHaveBeenCalledTimes(2);
    expect(publish.mock.calls[0]?.[0]).toMatchObject({ id: "42" });
    expect(publish.mock.calls[1]?.[0]).toMatchObject({ id: "43" });
  });

  it("continues past a per-item failure and names it", async () => {
    const publish = vi.fn(async (next: PSPathItem) => {
      if ((next.id ?? "") === "43") {
        throw { status: 403, statusText: "Forbidden", body: null };
      }
      return true;
    });
    const batch = await runPublishNowBatch(["42", "43"], "page", publish);
    expect(batch.total).toBe(2);
    expect(batch.published).toEqual(["42"]);
    expect(batch.failures).toHaveLength(1);
    expect(batch.failures[0]?.id).toBe("43");
    expect(batch.failures[0]?.message).toMatch(/forbidden/i);
  });

  it("names a false return instead of counting success", async () => {
    const publish = vi.fn(async () => false);
    const batch = await runPublishNowBatch(["42"], "page", publish);
    expect(batch.published).toEqual([]);
    expect(batch.failures).toEqual([{ id: "42", message: "not published" }]);
  });
});
