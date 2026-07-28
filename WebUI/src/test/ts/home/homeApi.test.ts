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
  formatApiError,
  normalizeContentItem,
  searchContent,
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

  function mockJsonResponse(body: unknown, init: { ok?: boolean; status?: number } = {}) {
    const text =
      typeof body === "string" ? body : JSON.stringify(body);
    return {
      ok: init.ok ?? true,
      status: init.status ?? 200,
      statusText: init.ok === false ? "Error" : "OK",
      headers: {
        get: (name: string) =>
          name.toLowerCase() === "content-type" ? "application/json" : null,
      },
      text: async () => text,
      json: async () => body,
    };
  }

  it("fetchRecentItems returns list payload", async () => {
    const fetchMock = globalThis.fetch as ReturnType<typeof vi.fn>;
    fetchMock.mockResolvedValue(
      mockJsonResponse([{ name: "Page A", id: "1" }]),
    );
    const items = await fetchRecentItems("item");
    expect(items).toHaveLength(1);
    expect(items[0].name).toBe("Page A");
  });

  it("fetchRecentItems maps ItemProperties wrapper", async () => {
    const fetchMock = globalThis.fetch as ReturnType<typeof vi.fn>;
    fetchMock.mockResolvedValue(
      mockJsonResponse({
        ItemProperties: [
          { id: "42", name: "Recent Page", path: "/Sites/Demo/page" },
        ],
      }),
    );
    const items = await fetchRecentItems("item");
    expect(items).toHaveLength(1);
    expect(items[0]).toMatchObject({
      id: "42",
      name: "Recent Page",
      path: "/Sites/Demo/page",
    });
  });

  it("fetchMyContent maps ItemProperties list", async () => {
    const fetchMock = globalThis.fetch as ReturnType<typeof vi.fn>;
    fetchMock.mockResolvedValue(
      mockJsonResponse({
        ItemProperties: [{ name: "Bookmarked", path: "/Sites/a/b" }],
      }),
    );
    const items = await fetchMyContent();
    expect(items).toHaveLength(1);
    expect(items[0].name).toBe("Bookmarked");
    expect(items[0].path).toBe("/Sites/a/b");
  });

  it("searchContent wraps SearchCriteria and unwraps paged results", async () => {
    const fetchMock = globalThis.fetch as ReturnType<typeof vi.fn>;
    fetchMock.mockResolvedValue(
      mockJsonResponse({
        PagedItemPropertiesList: {
          childrenCount: 1,
          startIndex: 0,
          childrenInPage: [
            {
              id: "9",
              title: "Hit Title",
              folderPath: "/Sites/Demo/hit",
              type: "page",
            },
          ],
        },
      }),
    );
    const items = await searchContent({ query: "hit", maxResults: 50 });
    expect(items).toHaveLength(1);
    expect(items[0].name).toBe("Hit Title");
    expect(items[0].path).toBe("/Sites/Demo/hit");
    expect(items[0].id).toBe("9");

    expect(fetchMock).toHaveBeenCalledTimes(1);
    const [url, init] = fetchMock.mock.calls[0] as [string, RequestInit];
    expect(url).toContain("/searchmanagement/search/get/extendedresults");
    const body = JSON.parse(String(init.body));
    expect(body).toEqual({
      SearchCriteria: {
        query: "hit",
        maxResults: 50,
        startIndex: 1,
        formatId: 9,
      },
    });
  });

  it("searchContent accepts legacy searchText alias", async () => {
    const fetchMock = globalThis.fetch as ReturnType<typeof vi.fn>;
    fetchMock.mockResolvedValue(
      mockJsonResponse({
        PagedItemPropertiesList: { childrenInPage: [] },
      }),
    );
    await searchContent({ searchText: "legacy", maxResults: 10 });
    const body = JSON.parse(
      String((fetchMock.mock.calls[0] as [string, RequestInit])[1].body),
    );
    expect(body.SearchCriteria.query).toBe("legacy");
  });

  it("normalizeContentItem fills name/path from alternate fields", () => {
    expect(
      normalizeContentItem({
        contentId: "c1",
        title: "T",
        folderPath: "/Sites/x",
      }),
    ).toMatchObject({ id: "c1", name: "T", path: "/Sites/x" });
  });

  it("fetchSites surfaces API errors", async () => {
    const fetchMock = globalThis.fetch as ReturnType<typeof vi.fn>;
    fetchMock.mockResolvedValue(
      mockJsonResponse({ message: "nope" }, { ok: false, status: 403 }),
    );
    await expect(fetchSites()).rejects.toMatchObject({
      status: 403,
    } as Partial<ApiError>);
  });

  describe("formatApiError", () => {
    const notAuth = "Not authorized to create";

    it("maps 401/403 ApiError to notAuthorizedMsg", () => {
      const err: ApiError = { status: 403, statusText: "Forbidden", body: "" };
      expect(formatApiError(err, notAuth)).toBe(notAuth);
      expect(
        formatApiError(
          { status: 401, statusText: "Unauthorized", body: null },
          notAuth,
        ),
      ).toBe(notAuth);
    });

    it("maps body message containing NotAuthorized to notAuthorizedMsg", () => {
      const err: ApiError = {
        status: 500,
        statusText: "Error",
        body: { message: "NotAuthorized" },
      };
      expect(formatApiError(err, notAuth)).toBe(notAuth);
    });

    it("prefers non-auth body string for other failures", () => {
      const err: ApiError = {
        status: 400,
        statusText: "Bad Request",
        body: "Name is invalid",
      };
      expect(formatApiError(err, notAuth)).toBe("Name is invalid");
    });
  });
});
