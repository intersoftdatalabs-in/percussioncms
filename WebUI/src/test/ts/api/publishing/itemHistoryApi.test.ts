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

import { describe, it, expect, vi, beforeEach } from "vitest";
import { fetchItemPublishingHistory } from "@/api/publishing/itemHistoryApi";
import * as client from "@/api/client";

vi.mock("@/api/client", async (importOriginal) => {
  const actual = await importOriginal<typeof import("@/api/client")>();
  return {
    ...actual,
    get: vi.fn(),
  };
});

const getMock = vi.mocked(client.get);

describe("fetchItemPublishingHistory", () => {
  beforeEach(() => {
    getMock.mockReset();
  });

  it("does not call GET for a blank id", async () => {
    await expect(fetchItemPublishingHistory("  ")).resolves.toEqual([]);
    expect(getMock).not.toHaveBeenCalled();
  });

  it("unwraps GET /itemmanagement/item/pubhistory/{id}", async () => {
    getMock.mockResolvedValue({
      ItemPublishingHistory: [
        { server: "prod", status: "SUCCESS", operation: "publish" },
      ],
    });
    const rows = await fetchItemPublishingHistory("16777215-101-9");
    expect(getMock).toHaveBeenCalledWith(
      "/services/itemmanagement/item/pubhistory/16777215-101-9",
    );
    expect(rows).toHaveLength(1);
    expect(rows[0].server).toBe("prod");
  });
});
