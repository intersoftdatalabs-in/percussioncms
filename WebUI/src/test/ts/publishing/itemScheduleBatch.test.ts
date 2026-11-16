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
  clearScheduleDatesBatch,
  formatScheduleBatchDetail,
  loadScheduleDatesBatch,
  parseScheduleItemIds,
  saveScheduleDatesBatch,
} from "@/publishing/itemScheduleBatch";
import type { ItemScheduleDates } from "@/contentExplorer/itemScheduleDates";

function dates(itemId: string): ItemScheduleDates {
  return {
    itemId,
    startDate: "09/18/2026 09:00 am",
    endDate: "09/19/2026 10:00 am",
    comments: "",
  };
}

describe("parseScheduleItemIds", () => {
  it("returns an empty list for blank input", () => {
    expect(parseScheduleItemIds("")).toEqual([]);
    expect(parseScheduleItemIds("   ")).toEqual([]);
    expect(parseScheduleItemIds(null)).toEqual([]);
    expect(parseScheduleItemIds(undefined)).toEqual([]);
  });

  it("splits commas, spaces, and semicolons", () => {
    expect(parseScheduleItemIds("42, 43 44;45")).toEqual([
      "42",
      "43",
      "44",
      "45",
    ]);
  });

  it("drops duplicates and unsafe tokens", () => {
    expect(parseScheduleItemIds("42, 42, <b>evil</b>, 43")).toEqual([
      "42",
      "43",
    ]);
  });
});

describe("formatScheduleBatchDetail", () => {
  it("names each failed item with its reason", () => {
    expect(
      formatScheduleBatchDetail([
        { id: "42", message: "Forbidden" },
        { id: "43", message: "Invalid schedule dates" },
      ]),
    ).toBe("42 (Forbidden); 43 (Invalid schedule dates)");
  });
});

describe("saveScheduleDatesBatch", () => {
  it("saves then reloads every id (round-trip)", async () => {
    const save = vi.fn().mockResolvedValue(undefined);
    const fetch = vi.fn().mockImplementation(async (id: string) => dates(id));
    const report = await saveScheduleDatesBatch(
      ["42", "43"],
      { startDate: "09/18/2026 09:00 am", endDate: "", comments: "go" },
      save,
      fetch,
    );
    expect(report).toEqual({ total: 2, saved: ["42", "43"], failures: [] });
    expect(save).toHaveBeenCalledTimes(2);
    expect(save).toHaveBeenNthCalledWith(1, {
      itemId: "42",
      startDate: "09/18/2026 09:00 am",
      endDate: "",
      comments: "go",
    });
    expect(fetch).toHaveBeenCalledTimes(2);
  });

  it("continues past a per-item failure and names it", async () => {
    const save = vi
      .fn()
      .mockRejectedValueOnce(new Error("FORBIDDEN"))
      .mockRejectedValueOnce({
        status: 400,
        statusText: "Bad Request",
        body: { message: "Expiry is not after embargo" },
      })
      .mockResolvedValue(undefined);
    const fetch = vi.fn().mockImplementation(async (id: string) => dates(id));
    const report = await saveScheduleDatesBatch(
      ["42", "43", "44"],
      { startDate: "", endDate: "", comments: "" },
      save,
      fetch,
    );
    expect(report.saved).toEqual(["44"]);
    expect(report.failures).toEqual([
      { id: "42", message: "Publish Forbidden" },
      { id: "43", message: "Expiry is not after embargo" },
    ]);
  });

  it("a failed reload is a per-item failure, not success", async () => {
    const save = vi.fn().mockResolvedValue(undefined);
    const fetch = vi
      .fn()
      .mockRejectedValueOnce(new Error("Gone"))
      .mockImplementation(async (id: string) => dates(id));
    const report = await saveScheduleDatesBatch(["42", "43"], {
      startDate: "",
      endDate: "",
      comments: "",
    }, save, fetch);
    expect(report.saved).toEqual(["43"]);
    expect(report.failures).toEqual([{ id: "42", message: "Gone" }]);
  });
});

describe("loadScheduleDatesBatch", () => {
  it("loads every id and reports failures per item", async () => {
    const fetch = vi
      .fn()
      .mockRejectedValueOnce(new Error("Forbidden"))
      .mockImplementation(async (id: string) => dates(id));
    const report = await loadScheduleDatesBatch(["42", "43"], fetch);
    expect(report.loaded.map((row) => row.id)).toEqual(["43"]);
    expect(report.failures).toEqual([{ id: "42", message: "Publish Forbidden" }]);
  });
});

describe("clearScheduleDatesBatch", () => {
  it("clears ids whose reload is empty and names the rest", async () => {
    const save = vi.fn().mockResolvedValue(undefined);
    const fetch = vi
      .fn()
      .mockResolvedValueOnce({
        itemId: "42",
        startDate: "",
        endDate: "",
        comments: "",
      })
      .mockResolvedValueOnce({
        itemId: "43",
        startDate: "09/18/2026 09:00 am",
        endDate: "",
        comments: "",
      });
    const report = await clearScheduleDatesBatch(
      ["42", "43"],
      "Invalid schedule dates",
      save,
      fetch,
    );
    expect(report.saved).toEqual(["42"]);
    expect(report.failures).toEqual([
      { id: "43", message: "Invalid schedule dates" },
    ]);
    expect(save).toHaveBeenCalledTimes(2);
  });
});
