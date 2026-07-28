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
  addToMyPages,
  contentItemId,
  createPage,
  fetchMyContent,
  fetchRecentItems,
  fetchSites,
  formatApiError,
  isBookmarkableItem,
  isMyPage,
  normalizeContentItem,
  removeFromMyPages,
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

  it("createPage posts repository // folderPath (getIdByPath requires it)", async () => {
    const fetchMock = globalThis.fetch as ReturnType<typeof vi.fn>;
    fetchMock.mockResolvedValue(
      mockJsonResponse({ Page: { id: "1", name: "p" } }),
    );
    await createPage({
      name: "about.html",
      title: "About",
      linkTitle: "About",
      templateId: "t1",
      folderPath: "/Sites/Demo",
    });
    const [url, init] = fetchMock.mock.calls[0] as [string, RequestInit];
    expect(url).toContain("/pagemanagement/page");
    const body = JSON.parse(String(init.body));
    expect(body.Page.folderPath).toBe("//Sites/Demo");
    expect(body.Page.addToRecent).toBe(true);
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

  it("addToMyPages PUTs encoded page id", async () => {
    const fetchMock = globalThis.fetch as ReturnType<typeof vi.fn>;
    fetchMock.mockResolvedValue(mockJsonResponse({ result: "ok" }));
    await addToMyPages("guid:1-2-3");
    const [url, init] = fetchMock.mock.calls[0] as [string, RequestInit];
    expect(url).toContain("/itemmanagement/item/addtomypages/");
    expect(url).toContain(encodeURIComponent("guid:1-2-3"));
    expect(init.method).toBe("PUT");
  });

  it("removeFromMyPages DELETEs encoded page id", async () => {
    const fetchMock = globalThis.fetch as ReturnType<typeof vi.fn>;
    fetchMock.mockResolvedValue(mockJsonResponse({ result: "ok" }));
    await removeFromMyPages("page-9");
    const [url, init] = fetchMock.mock.calls[0] as [string, RequestInit];
    expect(url).toContain("/itemmanagement/item/removefrommypages/");
    expect(url).toContain("page-9");
    expect(init.method).toBe("DELETE");
  });

  it("isMyPage parses text/plain boolean and prefers Accept text/plain", async () => {
    const fetchMock = globalThis.fetch as ReturnType<typeof vi.fn>;
    fetchMock.mockResolvedValue({
      ok: true,
      status: 200,
      statusText: "OK",
      headers: {
        get: (name: string) =>
          name.toLowerCase() === "content-type" ? "text/plain" : null,
      },
      text: async () => "true",
      json: async () => {
        throw new Error("not json");
      },
    });
    await expect(isMyPage("p1")).resolves.toBe(true);
    const [, init] = fetchMock.mock.calls[0] as [string, RequestInit];
    const headers = new Headers(init.headers as HeadersInit);
    expect(headers.get("Accept") ?? "").toMatch(/text\/plain/i);
  });

  it("contentItemId and isBookmarkableItem helpers", () => {
    expect(contentItemId({ id: " a " })).toBe("a");
    expect(contentItemId({ contentId: "c2" })).toBe("c2");
    expect(contentItemId({})).toBeNull();
    expect(isBookmarkableItem({ id: "1", type: "page" })).toBe(true);
    expect(isBookmarkableItem({ id: "1", folder: true })).toBe(false);
    expect(isBookmarkableItem({ id: "1", type: "Folder" })).toBe(false);
    expect(isBookmarkableItem({ name: "no-id" })).toBe(false);
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
