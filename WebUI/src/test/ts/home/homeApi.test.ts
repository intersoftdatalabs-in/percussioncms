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

import { describe, it, expect, vi, beforeEach, afterEach } from "vitest";
import {
  fetchMyContent,
  fetchRecentItems,
  fetchSites,
} from "@/api/home/homeApi";
import type { ApiError } from "@/api/client";

describe("homeApi", () => {
  beforeEach(() => {
    vi.stubGlobal(
      "fetch",
      vi.fn(),
    );
  });

  afterEach(() => {
    vi.unstubAllGlobals();
  });

  it("fetchRecentItems returns list payload", async () => {
    const fetchMock = globalThis.fetch as ReturnType<typeof vi.fn>;
    fetchMock.mockResolvedValue({
      ok: true,
      status: 200,
      json: async () => [{ name: "Page A", id: "1" }],
    });
    const items = await fetchRecentItems("item");
    expect(items).toHaveLength(1);
    expect(items[0].name).toBe("Page A");
  });

  it("fetchMyContent maps ItemProperties list", async () => {
    const fetchMock = globalThis.fetch as ReturnType<typeof vi.fn>;
    fetchMock.mockResolvedValue({
      ok: true,
      status: 200,
      json: async () => ({
        ItemProperties: [{ name: "Bookmarked", path: "/Sites/a/b" }],
      }),
    });
    const items = await fetchMyContent();
    expect(items).toHaveLength(1);
    expect(items[0].name).toBe("Bookmarked");
  });

  it("fetchSites surfaces API errors", async () => {
    const fetchMock = globalThis.fetch as ReturnType<typeof vi.fn>;
    fetchMock.mockResolvedValue({
      ok: false,
      status: 401,
      statusText: "Unauthorized",
      json: async () => ({ message: "nope" }),
    });
    await expect(fetchSites()).rejects.toMatchObject({
      status: 401,
    } as Partial<ApiError>);
  });
});
