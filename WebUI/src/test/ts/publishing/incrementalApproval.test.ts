/*
 * Copyright 1999-2026 Percussion Software, Inc.
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
import * as publishApi from "@/api/publishing/publishApi";
import {
  buildApprovalPayload,
  collectRelatedItemIds,
  relatedItemId,
  shouldUseApprovalPath,
} from "@/publishing/incrementalApproval";

describe("incremental approval helpers (OPS-18)", () => {
  it("extracts related item ids from queue DTOs", () => {
    expect(relatedItemId({ id: 42, name: "Page" })).toBe("42");
    expect(relatedItemId({ contentId: "abc" })).toBe("abc");
    expect(relatedItemId({})).toBeNull();
    expect(collectRelatedItemIds([{ id: 1 }, { id: 2 }, { name: "x" }])).toEqual(
      ["1", "2"],
    );
  });

  it("builds Minuet-shaped JSON path payload", () => {
    expect(buildApprovalPayload([1, 2, 3])).toBe("[1,2,3]");
    expect(buildApprovalPayload([])).toBe("[]");
    expect(buildApprovalPayload(["101", "202"])).toBe("[101,202]");
    expect(buildApprovalPayload(["a", "b"])).toBe(JSON.stringify(["a", "b"]));
  });

  it("uses approval path only when related items exist", () => {
    expect(shouldUseApprovalPath([])).toBe(false);
    expect(shouldUseApprovalPath([{ id: 1 }])).toBe(true);
  });

  it("calls publishIncrementalWithApproval with encoded payload shape", async () => {
    const spy = vi
      .spyOn(publishApi, "publishIncrementalWithApproval")
      .mockResolvedValue({});
    const payload = buildApprovalPayload(["101", "202"]);
    await publishApi.publishIncrementalWithApproval(
      "mysite",
      "Production",
      payload,
    );
    expect(spy).toHaveBeenCalledWith("mysite", "Production", payload);
    expect(payload).toBe("[101,202]");
    spy.mockRestore();
  });
});
