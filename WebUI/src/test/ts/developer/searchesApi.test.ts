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
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import {
  SEARCH_DESIGN_GAPS,
  createSearch,
  deleteSearch,
  executeSearch,
  getSearchDetail,
  isSearchWriteReady,
  isValidSearchName,
  listExplorerSavedSearches,
  listSearches,
  normalizeSearchName,
  saveSearch,
  unwrapSearchDef,
  unwrapSearchDefList,
  unwrapSearchExecuteResult,
  withoutStaleSearchWriteGap,
  wrapSearchDefForWire,
  wrapSearchExecuteRequest,
} from "../../../main/ts/api/developer/searchesApi";
import { PATHS } from "../../../main/ts/api/paths";

afterEach(() => {
  vi.restoreAllMocks();
});

describe("unwrapSearchExecuteResult", () => {
  it("accepts a flat payload", () => {
    const out = unwrapSearchExecuteResult({
      children: [{ id: "1", title: "A" }],
      totalCount: 1,
      startIndex: 1,
      searchName: "All Content",
    });
    expect(out.children).toHaveLength(1);
    expect(out.children?.[0]?.title).toBe("A");
    expect(out.totalCount).toBe(1);
    expect(out.searchName).toBe("All Content");
  });

  it("unwraps Jackson root name SearchExecuteResult", () => {
    const out = unwrapSearchExecuteResult({
      SearchExecuteResult: {
        children: [{ id: "2", name: "b" }],
        totalCount: 2,
        startIndex: 1,
      },
    });
    expect(out.children).toHaveLength(1);
    expect(out.totalCount).toBe(2);
  });

  it("returns empty shape for null / non-object", () => {
    expect(unwrapSearchExecuteResult(null).children).toEqual([]);
    expect(unwrapSearchExecuteResult("x").children).toEqual([]);
  });
});

describe("unwrapSearchDefList", () => {
  it("unwraps nested SearchDefList.SearchDef so the picker sees rows (#3576)", () => {
    expect(
      unwrapSearchDefList({
        SearchDefList: {
          SearchDef: [
            { name: "View_All", label: "All" },
            { name: "My Pages" },
          ],
        },
      }).map((s) => s.name),
    ).toEqual(["View_All", "My Pages"]);
  });

  it("unwraps ArrayList and ignores empty beans", () => {
    expect(unwrapSearchDefList({ ArrayList: [{ name: "All Content" }] })).toEqual(
      [{ name: "All Content" }],
    );
    expect(unwrapSearchDefList({ empty: true })).toEqual([]);
  });
});

describe("listSearches", () => {
  it("GETs /searches without includeViews by default", async () => {
    const fetchMock = vi.spyOn(global, "fetch").mockResolvedValueOnce(
      new Response(JSON.stringify([{ name: "My Pages" }]), {
        status: 200,
        headers: { "Content-Type": "application/json" },
      }),
    );
    const out = await listSearches();
    expect(String(fetchMock.mock.calls[0]?.[0])).toBe(PATHS.SEARCHES);
    expect(out).toHaveLength(1);
    expect(out[0]?.name).toBe("My Pages");
  });

  it("Explorer helper requests includeViews=true", async () => {
    const fetchMock = vi.spyOn(global, "fetch").mockResolvedValueOnce(
      new Response(
        JSON.stringify({
          SearchDef: [
            { name: "View_All", label: "All" },
            { name: "My Pages" },
          ],
        }),
        { status: 200, headers: { "Content-Type": "application/json" } },
      ),
    );
    const out = await listExplorerSavedSearches();
    expect(String(fetchMock.mock.calls[0]?.[0])).toBe(
      `${PATHS.SEARCHES}?includeViews=true`,
    );
    expect(out.map((s) => s.name)).toEqual(["View_All", "My Pages"]);
  });

  it("listExplorerSavedSearches unwraps nested SearchDefList (#3576)", async () => {
    vi.spyOn(global, "fetch").mockResolvedValueOnce(
      new Response(
        JSON.stringify({
          SearchDefList: {
            SearchDef: [{ name: "View_All", label: "All" }],
          },
        }),
        { status: 200, headers: { "Content-Type": "application/json" } },
      ),
    );
    const out = await listExplorerSavedSearches();
    expect(out).toHaveLength(1);
    expect(out[0]?.name).toBe("View_All");
  });
});

describe("wrapSearchExecuteRequest", () => {
  it("wraps flat overrides under SearchExecuteRequest for JAXB", () => {
    expect(
      wrapSearchExecuteRequest({
        folderPath: "/Sites/Foo",
        startIndex: 1,
        maxResults: 25,
      }),
    ).toEqual({
      SearchExecuteRequest: {
        folderPath: "//Sites/Foo",
        startIndex: 1,
        maxResults: 25,
      },
    });
  });

  it("sends an empty SearchExecuteRequest root when overrides are omitted", () => {
    expect(wrapSearchExecuteRequest()).toEqual({ SearchExecuteRequest: {} });
    expect(wrapSearchExecuteRequest(null)).toEqual({ SearchExecuteRequest: {} });
  });

  it("does not double-wrap an already enveloped body", () => {
    expect(
      wrapSearchExecuteRequest({
        SearchExecuteRequest: { startIndex: 2, maxResults: 10 },
      }),
    ).toEqual({
      SearchExecuteRequest: { startIndex: 2, maxResults: 10 },
    });
  });

  it("normalizes a single-slash folderPath on an already enveloped body", () => {
    expect(
      wrapSearchExecuteRequest({
        SearchExecuteRequest: { folderPath: "/Sites", startIndex: 1 },
      }),
    ).toEqual({
      SearchExecuteRequest: { folderPath: "//Sites", startIndex: 1 },
    });
  });

  it("omits Explorer root folderPath so execute stays unscoped (#3517)", () => {
    expect(
      wrapSearchExecuteRequest({ folderPath: "/", startIndex: 1 }),
    ).toEqual({
      SearchExecuteRequest: { startIndex: 1 },
    });
  });
});

describe("executeSearch", () => {
  it("POSTs to /searches/{id}/execute with optional overrides", async () => {
    const fetchMock = vi.spyOn(global, "fetch").mockResolvedValueOnce(
      new Response(
        JSON.stringify({
          children: [
            {
              id: "10",
              title: "Welcome",
              folderPath: "/Sites/Foo",
              type: "page",
            },
          ],
          totalCount: 1,
          startIndex: 1,
          searchName: "All Content",
        }),
        { status: 200, headers: { "Content-Type": "application/json" } },
      ),
    );

    const result = await executeSearch("All Content", {
      folderPath: "/Sites/Foo",
      startIndex: 1,
      maxResults: 25,
    });

    expect(fetchMock).toHaveBeenCalledTimes(1);
    const [url, init] = fetchMock.mock.calls[0] ?? [];
    expect(String(url)).toBe(
      `${PATHS.SEARCHES}/${encodeURIComponent("All Content")}/execute`,
    );
    expect(init?.method).toBe("POST");
    expect(JSON.parse(String(init?.body))).toEqual({
      SearchExecuteRequest: {
        folderPath: "//Sites/Foo",
        startIndex: 1,
        maxResults: 25,
      },
    });
    expect(result.children).toHaveLength(1);
    expect(result.children?.[0]?.title).toBe("Welcome");
    expect(result.searchName).toBe("All Content");
  });

  it("rejects blank idOrName without calling the network", async () => {
    const fetchMock = vi.spyOn(global, "fetch");
    await expect(executeSearch("   ")).rejects.toThrow(/required/i);
    expect(fetchMock).not.toHaveBeenCalled();
  });

  it("encodes special characters in the path segment", async () => {
    const fetchMock = vi.spyOn(global, "fetch").mockResolvedValueOnce(
      new Response(JSON.stringify({ children: [], totalCount: 0, startIndex: 1 }), {
        status: 200,
        headers: { "Content-Type": "application/json" },
      }),
    );
    await executeSearch("A/B Search");
    const [url] = fetchMock.mock.calls[0] ?? [];
    expect(String(url)).toBe(
      `${PATHS.SEARCHES}/${encodeURIComponent("A/B Search")}/execute`,
    );
  });
});

describe("search name validation", () => {
  it("trims names", () => {
    expect(normalizeSearchName("  MySearch  ")).toBe("MySearch");
    expect(normalizeSearchName("")).toBe("");
    expect(normalizeSearchName(null)).toBe("");
  });

  it("accepts REST-safe create names and rejects junk", () => {
    expect(isValidSearchName("MySearch")).toBe(true);
    expect(isValidSearchName("qa4076")).toBe(true);
    expect(isValidSearchName("  StandardOne  ")).toBe(true);
    expect(isValidSearchName("")).toBe(false);
    expect(isValidSearchName("has space")).toBe(false);
    expect(isValidSearchName("wild*card")).toBe(false);
    expect(isValidSearchName("pct%name")).toBe(false);
    expect(isValidSearchName("../x")).toBe(false);
    expect(isValidSearchName("a/b")).toBe(false);
    expect(isValidSearchName("a\\b")).toBe(false);
  });

  it("disables write until the search name is valid on create", () => {
    expect(isSearchWriteReady({ isNew: true, name: "" })).toBe(false);
    expect(isSearchWriteReady({ isNew: true, name: "has space" })).toBe(false);
    expect(isSearchWriteReady({ isNew: true, name: "MySearch" })).toBe(true);
    expect(isSearchWriteReady({ isNew: false, name: "MySearch" })).toBe(true);
    expect(isSearchWriteReady({ isNew: false, name: "" })).toBe(false);
  });
});

describe("search wire wrap", () => {
  it("wraps POST/PUT under SearchDef root", () => {
    expect(
      wrapSearchDefForWire({
        name: "MySearch",
        label: "My Search",
        description: "Created via REST",
        type: "StandardSearch",
        displayFormatId: "1",
      }),
    ).toEqual({
      SearchDef: {
        name: "MySearch",
        label: "My Search",
        description: "Created via REST",
        type: "StandardSearch",
        displayFormatId: "1",
      },
    });
  });

  it("unwraps SearchDef envelope and flat bodies", () => {
    expect(
      unwrapSearchDef({ SearchDef: { name: "MySearch", label: "My Search" } }),
    ).toEqual({ name: "MySearch", label: "My Search" });
    expect(unwrapSearchDef({ name: "All Content" })).toEqual({ name: "All Content" });
    expect(unwrapSearchDef(null)).toEqual({});
  });

  it("drops the create/update/delete gap from SEARCH_DESIGN_GAPS", () => {
    expect(SEARCH_DESIGN_GAPS.some((g) => /create/i.test(g))).toBe(false);
    expect(SEARCH_DESIGN_GAPS.some((g) => /field criterion/i.test(g))).toBe(true);
  });

  it("filters a stale REST write gap on GET detail", () => {
    expect(
      withoutStaleSearchWriteGap([
        "Search create / update / delete not supported via this API",
        "Search field criterion editing not supported via this API",
      ]),
    ).toEqual(["Search field criterion editing not supported via this API"]);
  });
});

describe("searchesApi write paths", () => {
  const fetchMock = vi.fn();

  beforeEach(() => {
    fetchMock.mockReset();
    vi.stubGlobal("fetch", fetchMock);
  });

  afterEach(() => {
    vi.unstubAllGlobals();
  });

  function jsonResponse(body: unknown, status = 200): Response {
    return new Response(JSON.stringify(body), {
      status,
      headers: { "Content-Type": "application/json" },
    });
  }

  it("POSTs create body to /services/searches", async () => {
    fetchMock.mockResolvedValueOnce(
      jsonResponse({ name: "MySearch", label: "My Search", type: "StandardSearch" }),
    );
    const saved = await createSearch({
      name: "MySearch",
      label: "My Search",
      type: "StandardSearch",
    });
    expect(saved.name).toBe("MySearch");
    expect(fetchMock).toHaveBeenCalledTimes(1);
    const init = fetchMock.mock.calls[0][1] as RequestInit;
    expect(init.method).toBe("POST");
    expect(String(fetchMock.mock.calls[0][0])).toContain(PATHS.SEARCHES);
    expect(JSON.parse(String(init.body))).toEqual({
      SearchDef: { name: "MySearch", label: "My Search", type: "StandardSearch" },
    });
  });

  it("PUTs save body to /services/searches/{idOrName}", async () => {
    fetchMock.mockResolvedValueOnce(jsonResponse({ name: "MySearch", label: "Updated" }));
    const saved = await saveSearch("MySearch", { name: "MySearch", label: "Updated" });
    expect(saved.label).toBe("Updated");
    const init = fetchMock.mock.calls[0][1] as RequestInit;
    expect(init.method).toBe("PUT");
    expect(String(fetchMock.mock.calls[0][0])).toContain(`${PATHS.SEARCHES}/MySearch`);
  });

  it("DELETEs /services/searches/{idOrName}", async () => {
    fetchMock.mockResolvedValueOnce(new Response(null, { status: 204 }));
    await deleteSearch("MySearch");
    const init = fetchMock.mock.calls[0][1] as RequestInit;
    expect(init.method).toBe("DELETE");
    expect(String(fetchMock.mock.calls[0][0])).toContain(`${PATHS.SEARCHES}/MySearch`);
  });

  it("unwraps GET /services/searches/{idOrName} Jackson root", async () => {
    fetchMock.mockResolvedValueOnce(
      jsonResponse({ SearchDef: { name: "MySearch", label: "My Search", fields: [] } }),
    );
    const detail = await getSearchDetail("MySearch");
    expect(detail.name).toBe("MySearch");
    expect(detail.label).toBe("My Search");
  });

  it("surfaces 400 invalid name", async () => {
    fetchMock.mockResolvedValueOnce(
      jsonResponse({ message: "name cannot contain whitespace" }, 400),
    );
    await expect(createSearch({ name: "bad name" })).rejects.toMatchObject({
      status: 400,
    });
    expect(fetchMock).toHaveBeenCalledTimes(1);
  });

  it("surfaces 409 duplicate name", async () => {
    fetchMock.mockResolvedValueOnce(
      jsonResponse({ message: "Search already exists: MySearch" }, 409),
    );
    await expect(createSearch({ name: "MySearch" })).rejects.toMatchObject({
      status: 409,
    });
  });

  it("surfaces 404 missing search", async () => {
    fetchMock.mockResolvedValueOnce(jsonResponse({ message: "Search not found" }, 404));
    await expect(getSearchDetail("missing")).rejects.toMatchObject({ status: 404 });
    fetchMock.mockResolvedValueOnce(jsonResponse({ message: "Search not found" }, 404));
    await expect(deleteSearch("missing")).rejects.toMatchObject({ status: 404 });
  });

  it("surfaces 403 non-Admin create", async () => {
    fetchMock.mockResolvedValueOnce(jsonResponse({ message: "Admin role required" }, 403));
    await expect(createSearch({ name: "MySearch" })).rejects.toMatchObject({
      status: 403,
    });
  });
});
