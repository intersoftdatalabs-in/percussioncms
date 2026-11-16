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
  browserQueuePresetStorage,
  extractQueueItems,
  filterQueueByApproval,
  filterQueueByPreset,
  formatPresetCount,
  hasMorePages,
  isQueueEmpty,
  isQueuePresetFilter,
  queueItemFailed,
  queueItemId,
  queueItemLabel,
  queueItemOwner,
  queueItemScheduled,
  queueApproveFailure,
  queueItemApproved,
  queueUnapproveFailure,
  queueRemoveFailure,
  QUEUE_PRESET_PREFERENCE_KEY,
  readQueuePresetPreference,
  writeQueuePresetPreference,
  type QueuePresetStorage,
} from "@/publishing/incrementalQueue";

describe("incrementalQueue", () => {
  it("treats missing page as empty", () => {
    expect(isQueueEmpty(null)).toBe(true);
    expect(extractQueueItems(undefined)).toEqual([]);
  });

  it("extracts items array", () => {
    const page = { items: [{ id: 1 }, { id: 2 }], pageSize: 10 };
    expect(extractQueueItems(page)).toHaveLength(2);
    expect(isQueueEmpty(page)).toBe(false);
  });

  it("extracts PagedItemList.childrenInPage (product queue shape)", () => {
    const page = {
      PagedItemList: {
        childrenInPage: [{ id: 9 }, { id: 10 }],
        childrenCount: 2,
      },
    };
    expect(extractQueueItems(page)).toHaveLength(2);
    expect(isQueueEmpty(page)).toBe(false);
  });

  it("uses totalCount for paging", () => {
    const page = { items: [{ a: 1 }], totalCount: 5 };
    expect(hasMorePages(page, 1, 1)).toBe(true);
    expect(hasMorePages({ items: [{ a: 1 }], totalCount: 1 }, 1, 1)).toBe(
      false,
    );
  });

  it("assumes more when full page without totalCount", () => {
    const items = Array.from({ length: 10 }, (_, i) => ({ i }));
    expect(hasMorePages({ items }, 1, 10)).toBe(true);
    expect(hasMorePages({ items: [{ i: 1 }] }, 1, 10)).toBe(false);
  });

  it("reads content id and title or name for a queue row", () => {
    expect(queueItemId({ id: "301", name: "Home" })).toBe("301");
    expect(queueItemId({ contentid: 88 })).toBe("88");
    expect(queueItemLabel({ id: "301", name: "Home" })).toBe("Home");
    expect(queueItemLabel({ contentId: 9, title: "About" })).toBe("About");
    expect(queueItemLabel({ id: "12" })).toBe("12");
    expect(queueItemLabel({})).toBe("—");
    expect(queueItemId(null)).toBe("");
  });

  it("maps remove failures so 403 and 404 are not success", () => {
    expect(queueRemoveFailure({ status: 403, statusText: "Forbidden", body: "" })).toBe(
      "forbidden",
    );
    expect(queueRemoveFailure({ status: 404, statusText: "Not Found", body: "" })).toBe(
      "not_found",
    );
    expect(queueRemoveFailure({ status: 500, statusText: "Error", body: "" })).toBe(
      "failed",
    );
    expect(queueRemoveFailure(new Error("network"))).toBe("failed");
  });

  it("maps approve failures so 400, 403, and 404 are not success", () => {
    expect(queueApproveFailure({ status: 400, statusText: "Bad Request", body: "" })).toBe(
      "bad_request",
    );
    expect(queueApproveFailure({ status: 403, statusText: "Forbidden", body: "" })).toBe(
      "forbidden",
    );
    expect(queueApproveFailure({ status: 404, statusText: "Not Found", body: "" })).toBe(
      "not_found",
    );
    expect(queueApproveFailure({ status: 500, statusText: "Error", body: "" })).toBe(
      "failed",
    );
    expect(queueItemApproved({ id: "301", status: "Approved" })).toBe(true);
    expect(queueItemApproved({ id: "301", name: "Home" })).toBe(false);
  });

  it("maps unapprove failures so 400, 403, and 404 are not success", () => {
    expect(queueUnapproveFailure({ status: 400, statusText: "Bad Request", body: "" })).toBe(
      "bad_request",
    );
    expect(queueUnapproveFailure({ status: 403, statusText: "Forbidden", body: "" })).toBe(
      "forbidden",
    );
    expect(queueUnapproveFailure({ status: 404, statusText: "Not Found", body: "" })).toBe(
      "not_found",
    );
    expect(queueUnapproveFailure({ status: 500, statusText: "Error", body: "" })).toBe(
      "failed",
    );
  });

  it("filters queue rows by approval facet (#6259)", () => {
    const rows = [
      { id: "301", name: "Home", approved: true },
      { id: "88", title: "About" },
      { id: "89", title: "Contact", status: "Approved" },
    ];
    expect(filterQueueByApproval(rows, "all")).toEqual(rows);
    expect(filterQueueByApproval(rows, "approved")).toHaveLength(2);
    expect(filterQueueByApproval(rows, "pending")).toHaveLength(1);
    expect(filterQueueByApproval(rows, "pending")[0]).toEqual({
      id: "88",
      title: "About",
    });
    expect(filterQueueByApproval([], "approved")).toEqual([]);
  });

  it("reads owner, failed, and scheduled markers from queue rows (#6471)", () => {
    expect(queueItemOwner({ id: "301", owner: "Admin" })).toBe("Admin");
    expect(queueItemOwner({ id: "302", createdBy: "  editor1 " })).toBe(
      "editor1",
    );
    expect(queueItemOwner({ id: "303", authorName: "Author" })).toBe("Author");
    expect(queueItemOwner({ id: "304", name: "No owner fields" })).toBe("");
    expect(queueItemOwner(null)).toBe("");
    expect(queueItemFailed({ status: "Failed" })).toBe(true);
    expect(queueItemFailed({ jobStatus: "delivery failed" })).toBe(true);
    expect(queueItemFailed({ failed: true })).toBe(true);
    expect(queueItemFailed({ failed: false, status: "Pending" })).toBe(false);
    expect(queueItemFailed({ id: "88", title: "About" })).toBe(false);
    expect(queueItemScheduled({ scheduledDate: "2026-11-20" })).toBe(true);
    expect(queueItemScheduled({ status: "Scheduled" })).toBe(true);
    expect(queueItemScheduled({ publishDate: 1732051200000 })).toBe(true);
    expect(queueItemScheduled({ id: "88", status: "Pending" })).toBe(false);
  });

  it("filters queue rows by mine failed scheduled presets (#6471)", () => {
    const rows = [
      { id: "301", name: "Home", owner: "Admin", status: "Approved" },
      { id: "88", title: "About", createdBy: "editor1", status: "Failed" },
      {
        id: "89",
        title: "Contact",
        owner: "admin",
        scheduledDate: "2026-11-20",
      },
      { id: "90", title: "News" },
    ];
    expect(filterQueueByPreset(rows, "all")).toEqual(rows);
    // Mine matches case-insensitively against any owner-like field.
    expect(
      filterQueueByPreset(rows, "mine", "Admin").map((r) =>
        (r as { id: string }).id,
      ),
    ).toEqual(["301", "89"]);
    expect(filterQueueByPreset(rows, "mine", "editor1")).toHaveLength(1);
    // A blank operator name matches nothing — ownership is never invented.
    expect(filterQueueByPreset(rows, "mine", "")).toEqual([]);
    expect(filterQueueByPreset(rows, "mine")).toEqual([]);
    expect(filterQueueByPreset(rows, "failed")).toHaveLength(1);
    expect(filterQueueByPreset(rows, "scheduled")).toHaveLength(1);
    expect(filterQueueByPreset([], "failed")).toEqual([]);
  });

  it("validates preset filter values (#6471)", () => {
    expect(isQueuePresetFilter("all")).toBe(true);
    expect(isQueuePresetFilter("mine")).toBe(true);
    expect(isQueuePresetFilter("failed")).toBe(true);
    expect(isQueuePresetFilter("scheduled")).toBe(true);
    expect(isQueuePresetFilter("approved")).toBe(false);
    expect(isQueuePresetFilter("")).toBe(false);
    expect(isQueuePresetFilter(null)).toBe(false);
  });

  function memoryStorage(seed?: Record<string, string>): QueuePresetStorage {
    const data = new Map(Object.entries(seed ?? {}));
    return {
      getItem: (key) => data.get(key) ?? null,
      setItem: (key, value) => {
        data.set(key, value);
      },
    };
  }

  it("persists the preset preference and falls back to all (#6471)", () => {
    const storage = memoryStorage();
    expect(readQueuePresetPreference(storage)).toBe("all");
    writeQueuePresetPreference("failed", storage);
    expect(storage.getItem(QUEUE_PRESET_PREFERENCE_KEY)).toBe("failed");
    expect(readQueuePresetPreference(storage)).toBe("failed");
    writeQueuePresetPreference("scheduled", storage);
    expect(readQueuePresetPreference(storage)).toBe("scheduled");
  });

  it("ignores stale or blank persisted presets (#6471)", () => {
    expect(
      readQueuePresetPreference(
        memoryStorage({ [QUEUE_PRESET_PREFERENCE_KEY]: "approved" }),
      ),
    ).toBe("all");
    expect(
      readQueuePresetPreference(
        memoryStorage({ [QUEUE_PRESET_PREFERENCE_KEY]: "" }),
      ),
    ).toBe("all");
  });

  it("survives denied storage without throwing (#6471)", () => {
    const denied: QueuePresetStorage = {
      getItem: () => {
        throw new Error("denied");
      },
      setItem: () => {
        throw new Error("denied");
      },
    };
    expect(readQueuePresetPreference(denied)).toBe("all");
    expect(() => writeQueuePresetPreference("mine", denied)).not.toThrow();
    expect(readQueuePresetPreference(null)).toBe("all");
    expect(() =>
      writeQueuePresetPreference("mine", null),
    ).not.toThrow();
    // Browser adapter resolves without throwing.
    expect(() => browserQueuePresetStorage()).not.toThrow();
  });

  it("formats the preset result count (#6471)", () => {
    expect(formatPresetCount("Showing {0} of {1} queued items", 2, 9)).toBe(
      "Showing 2 of 9 queued items",
    );
    expect(formatPresetCount("plain template", 2, 9)).toBe("plain template");
  });
});
