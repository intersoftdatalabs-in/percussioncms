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
  extractQueueItems,
  hasMorePages,
  isQueueEmpty,
  queueItemId,
  queueItemLabel,
  queueApproveFailure,
  queueItemApproved,
  queueUnapproveFailure,
  queueRemoveFailure,
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
});
