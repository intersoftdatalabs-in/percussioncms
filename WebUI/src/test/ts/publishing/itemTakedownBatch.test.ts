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
  formatTakedownBatchDetail,
  parseTakedownItemIds,
  reviewTakedownBatch,
  runTakedownBatch,
  takedownBatchFailureMessage,
} from "@/publishing/itemTakedownBatch";

describe("parseTakedownItemIds", () => {
  it("returns an empty list for blank input", () => {
    expect(parseTakedownItemIds("")).toEqual([]);
    expect(parseTakedownItemIds("   ")).toEqual([]);
    expect(parseTakedownItemIds(null)).toEqual([]);
    expect(parseTakedownItemIds(undefined)).toEqual([]);
  });

  it("splits commas, spaces, and semicolons", () => {
    expect(parseTakedownItemIds("42, 43 44;45")).toEqual([
      "42",
      "43",
      "44",
      "45",
    ]);
  });

  it("drops duplicates and unsafe tokens", () => {
    expect(parseTakedownItemIds("42, 42, <b>evil</b>, 43")).toEqual([
      "42",
      "43",
    ]);
  });
});

describe("formatTakedownBatchDetail", () => {
  it("joins id (reason) pairs", () => {
    expect(
      formatTakedownBatchDetail([
        { id: "42", message: "gone" },
        { id: "43", message: "forbidden" },
      ]),
    ).toBe("42 (gone); 43 (forbidden)");
  });
});

describe("takedownBatchFailureMessage", () => {
  it("names HTTP statuses", () => {
    expect(
      takedownBatchFailureMessage({
        status: 403,
        statusText: "Forbidden",
        body: null,
      }),
    ).toBe("Publish Forbidden (HTTP 403 Forbidden)");
    expect(
      takedownBatchFailureMessage({
        status: 400,
        statusText: "Bad Request",
        body: { message: "Expiry is not after embargo" },
      }),
    ).toBe("Expiry is not after embargo");
    expect(
      takedownBatchFailureMessage({
        status: 404,
        statusText: "Not Found",
        body: null,
      }),
    ).toBe("Item not found (HTTP 404 Not Found)");
    expect(
      takedownBatchFailureMessage({
        status: 409,
        statusText: "Conflict",
        body: null,
      }),
    ).toBe("Someone else is editing this item (HTTP 409 Conflict)");
  });

  it("names application-level failure text", () => {
    expect(takedownBatchFailureMessage(new Error("FORBIDDEN"))).toBe(
      "Publish Forbidden",
    );
    expect(takedownBatchFailureMessage(new Error("checked out"))).toBe(
      "Someone else is editing this item",
    );
  });
});

describe("reviewTakedownBatch", () => {
  it("reviews every id and names lookup failures without blocking", async () => {
    const loadLinked = vi
      .fn()
      .mockRejectedValueOnce(new Error("Gone"))
      .mockImplementation(async (id: string) => [
        { id: "7", pagePath: `/Sites/Demo/${id}` },
      ]);
    const report = await reviewTakedownBatch(["42", "43"], loadLinked);
    expect(report.total).toBe(2);
    expect(report.reviewed.map((row) => row.id)).toEqual(["42", "43"]);
    expect(report.reviewed[0].linked).toEqual([]);
    expect(report.reviewed[1].linked).toEqual([
      { id: "7", pagePath: "/Sites/Demo/43" },
    ]);
    expect(report.failures).toEqual([{ id: "42", message: "Gone" }]);
  });
});

describe("runTakedownBatch", () => {
  it("takes down every id with the reviewed linked pages", async () => {
    const takeDown = vi.fn().mockResolvedValue(true);
    const report = await runTakedownBatch(["42", "43"], "page", {
      "42": [{ id: "7", pagePath: "/Sites/Demo/Home" }],
    }, takeDown);
    expect(report).toEqual({ total: 2, takenDown: ["42", "43"], failures: [] });
    expect(takeDown).toHaveBeenCalledTimes(2);
    expect(takeDown).toHaveBeenNthCalledWith(
      1,
      expect.objectContaining({ id: "42" }),
      [{ id: "7", pagePath: "/Sites/Demo/Home" }],
    );
    expect(takeDown).toHaveBeenNthCalledWith(
      2,
      expect.objectContaining({ id: "43" }),
      [],
    );
  });

  it("continues past a per-item failure and names it", async () => {
    const takeDown = vi
      .fn()
      .mockRejectedValueOnce(new Error("FORBIDDEN"))
      .mockRejectedValueOnce({
        status: 409,
        statusText: "Conflict",
        body: null,
      })
      .mockResolvedValue(true);
    const report = await runTakedownBatch(
      ["42", "43", "44"],
      "resource",
      {},
      takeDown,
    );
    expect(report.takenDown).toEqual(["44"]);
    expect(report.failures).toEqual([
      { id: "42", message: "Publish Forbidden" },
      { id: "43", message: "Someone else is editing this item (HTTP 409 Conflict)" },
    ]);
  });

  it("a false return is a per-item failure, not success", async () => {
    const takeDown = vi.fn().mockResolvedValue(false);
    const report = await runTakedownBatch(["42"], "page", {}, takeDown);
    expect(report.takenDown).toEqual([]);
    expect(report.failures).toEqual([{ id: "42", message: "not taken down" }]);
  });
});
