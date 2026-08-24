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

import { describe, it, expect, vi, beforeEach, afterEach } from "vitest";
import {
  addToMyPages,
  BLOG_LIST_WIDGET_ID,
  BLOG_POST_WIDGET_ID,
  contentItemId,
  createPage,
  createPageAndItem,
  ensurePageFileName,
  unwrapCreatedPageId,
  mapSiteSummary,
  resolveSiteRootFolderPath,
  extractTemplateWidgetDefinitionIds,
  fetchAssetTypes,
  fetchMyContent,
  fetchRecentItems,
  fetchSites,
  fetchTemplatesForSite,
  fetchTemplatesForSectionCreate,
  formatApiError,
  isBookmarkableItem,
  isMyPage,
  mapAssetType,
  mapTemplateSummary,
  normalizeContentItem,
  removeFromMyPages,
  searchContent,
  templateHasWidget,
  unwrapTemplateSummaries,
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

  it("mapSiteSummary normalizes FastForward folderPath vs SITENAME (#3726)", () => {
    expect(
      mapSiteSummary({
        name: "Corporate_Investments",
        folderPath: "//Sites/CorporateInvestments",
        id: "350",
      }),
    ).toMatchObject({
      name: "Corporate_Investments",
      folderPath: "/Sites/CorporateInvestments",
      id: "350",
    });
    expect(
      mapSiteSummary({
        Name: "Enterprise_Investments",
        folderPaths: ["//Sites/EnterpriseInvestments"],
      }),
    ).toMatchObject({
      name: "Enterprise_Investments",
      folderPath: "/Sites/EnterpriseInvestments",
    });
  });

  it("resolveSiteRootFolderPath uses PathItem.folderPath not finder SITENAME (#3726)", async () => {
    const fetchMock = globalThis.fetch as ReturnType<typeof vi.fn>;
    fetchMock.mockResolvedValue(
      mockJsonResponse({
        PathItem: {
          path: "/Sites/Corporate_Investments/",
          folderPath: "//Sites/CorporateInvestments",
          name: "Corporate_Investments",
        },
      }),
    );
    const root = await resolveSiteRootFolderPath({
      name: "Corporate_Investments",
      folderPath: "/Sites/Corporate_Investments",
    });
    expect(root).toBe("/Sites/CorporateInvestments");
    const url = String(fetchMock.mock.calls[0]?.[0]);
    expect(url).toContain("/pathmanagement/path/item");
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

  it("createPage posts FastForward repository folder not SITENAME (#3726)", async () => {
    const fetchMock = globalThis.fetch as ReturnType<typeof vi.fn>;
    fetchMock.mockResolvedValue(
      mockJsonResponse({ Page: { id: "1", name: "p" } }),
    );
    await createPage({
      name: "about.html",
      title: "About",
      linkTitle: "About",
      templateId: "t1",
      folderPath: "/Sites/CorporateInvestments",
    });
    const body = JSON.parse(
      String((fetchMock.mock.calls[0] as [string, RequestInit])[1].body),
    );
    expect(body.Page.folderPath).toBe("//Sites/CorporateInvestments");
    expect(body.Page.folderPath).not.toContain("Corporate_Investments");
  });

  it("createPage appends .html when name has no extension", async () => {
    const fetchMock = globalThis.fetch as ReturnType<typeof vi.fn>;
    fetchMock.mockResolvedValue(
      mockJsonResponse({ Page: { id: "1", name: "about.html" } }),
    );
    await createPage({
      name: "about",
      title: "About",
      linkTitle: "About",
      templateId: "t1",
      folderPath: "/Sites/Demo",
    });
    const body = JSON.parse(
      String((fetchMock.mock.calls[0] as [string, RequestInit])[1].body),
    );
    expect(body.Page.name).toBe("about.html");
  });

  it("unwraps created page id and createPageAndItem returns path plus id", async () => {
    expect(unwrapCreatedPageId({ Page: { id: "1-101-9", name: "p" } })).toBe(
      "1-101-9",
    );
    const fetchMock = globalThis.fetch as ReturnType<typeof vi.fn>;
    fetchMock.mockResolvedValue(
      mockJsonResponse({ Page: { id: "1-101-9", name: "about.html" } }),
    );
    const created = await createPageAndItem({
      name: "about",
      title: "About",
      linkTitle: "About",
      templateId: "t1",
      folderPath: "/Sites/Demo",
    });
    expect(created.path).toMatch(/about\.html$/);
    expect(created.itemId).toBe("1-101-9");
  });

  it("mapAssetType prefers widgetId over contentTypeId", () => {
    expect(
      mapAssetType({
        contentTypeId: 1075,
        contentTypeName: "percImageAsset",
        widgetId: "percImage",
        widgetLabel: "Image",
      }),
    ).toMatchObject({
      id: "percImage",
      name: "Image",
      label: "Image",
      contentTypeId: "1075",
    });
  });

  it("fetchAssetTypes maps WidgetContentType list", async () => {
    const fetchMock = globalThis.fetch as ReturnType<typeof vi.fn>;
    fetchMock.mockResolvedValue(
      mockJsonResponse({
        WidgetContentType: [
          {
            contentTypeId: 1075,
            widgetId: "percImage",
            widgetLabel: "Image",
          },
        ],
      }),
    );
    const types = await fetchAssetTypes();
    expect(types).toEqual([
      expect.objectContaining({ id: "percImage", label: "Image" }),
    ]);
  });

  it("ensurePageFileName", () => {
    expect(ensurePageFileName("foo")).toBe("foo.html");
    expect(ensurePageFileName("foo.html")).toBe("foo.html");
    expect(ensurePageFileName("foo.XML")).toBe("foo.XML");
  });

  it("extractTemplateWidgetDefinitionIds finds blog widgets", () => {
    const raw = {
      Template: {
        regionTree: {
          regionWidgetAssociations: [
            {
              regionId: "content",
              widgetItems: [
                { definitionId: BLOG_LIST_WIDGET_ID, id: "w1" },
              ],
            },
          ],
        },
      },
    };
    expect(extractTemplateWidgetDefinitionIds(raw)).toContain(
      BLOG_LIST_WIDGET_ID,
    );
    expect(templateHasWidget(raw, BLOG_LIST_WIDGET_ID)).toBe(true);
    expect(templateHasWidget(raw, BLOG_POST_WIDGET_ID)).toBe(false);
  });

  it("extractTemplateWidgetDefinitionIds respects depth limit on deep trees", () => {
    // Nest past TEMPLATE_WIDGET_WALK_MAX_DEPTH (32) so definitionId is unreachable.
    let deep: Record<string, unknown> = {
      definitionId: BLOG_POST_WIDGET_ID,
    };
    for (let i = 0; i < 40; i++) {
      deep = { child: deep };
    }
    const raw = { Template: { regionTree: deep } };
    expect(extractTemplateWidgetDefinitionIds(raw)).not.toContain(
      BLOG_POST_WIDGET_ID,
    );
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

  it("isMyPage parses JSON boolean body with standard Accept", async () => {
    const fetchMock = globalThis.fetch as ReturnType<typeof vi.fn>;
    fetchMock.mockResolvedValue(mockJsonResponse(true));
    await expect(isMyPage("p1")).resolves.toBe(true);
    const [url, init] = fetchMock.mock.calls[0] as [string, RequestInit];
    expect(url).toContain("/itemmanagement/item/ismypage/");
    expect(url).toContain("p1");
    const headers = new Headers(init.headers as HeadersInit);
    expect(headers.get("Accept") ?? "").toMatch(/application\/json/i);
  });

  it("isMyPage treats JSON false as not bookmarked", async () => {
    const fetchMock = globalThis.fetch as ReturnType<typeof vi.fn>;
    fetchMock.mockResolvedValue(mockJsonResponse(false));
    await expect(isMyPage("p2")).resolves.toBe(false);
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

  it("unwrapTemplateSummaries maps TemplateSummary envelope and field aliases", () => {
    expect(
      unwrapTemplateSummaries({
        TemplateSummary: [
          { id: "1-101-7", name: "Home", imageThumbPath: "/t.png" },
          { templateId: 1037, templateName: "perc.page", templateLabel: "Page" },
        ],
      }),
    ).toEqual([
      { id: "1-101-7", name: "Home", thumbPath: "/t.png" },
      { id: "1037", name: "perc.page" },
    ]);
  });

  it("unwrapTemplateSummaries accepts raw arrays and nested WRAP_ROOT_VALUE rows", () => {
    expect(
      unwrapTemplateSummaries([
        { TemplateSummary: { id: "a", label: "Alpha" } },
        { Id: "b", Name: "Beta" },
      ]),
    ).toEqual([
      { id: "a", name: "Alpha" },
      { id: "b", name: "Beta" },
    ]);
  });

  it("unwrapTemplateSummaries treats ArrayList empty-bean as no templates", () => {
    expect(unwrapTemplateSummaries({ TemplateSummary: { empty: false } })).toEqual(
      [],
    );
    expect(unwrapTemplateSummaries({ empty: true })).toEqual([]);
  });

  it("unwrapTemplateSummaries unwraps TemplateSummaryList envelope", () => {
    expect(
      unwrapTemplateSummaries({
        TemplateSummaryList: {
          TemplateSummary: [{ id: "t9", name: "Base" }],
        },
      }),
    ).toEqual([{ id: "t9", name: "Base" }]);
  });

  it("mapTemplateSummary drops rows without an id", () => {
    expect(mapTemplateSummary({ name: "orphan" })).toBeNull();
    expect(mapTemplateSummary({ empty: false })).toBeNull();
  });

  it("fetchTemplatesForSite unwraps the live TemplateSummary envelope", async () => {
    const fetchMock = globalThis.fetch as ReturnType<typeof vi.fn>;
    fetchMock.mockResolvedValue(
      mockJsonResponse({
        TemplateSummary: [
          { id: "1-101-7", name: "Home" },
          { templateId: "tmpl-2", templateLabel: "Article" },
        ],
      }),
    );
    const list = await fetchTemplatesForSite("QaSite3002");
    expect(list).toEqual([
      { id: "1-101-7", name: "Home" },
      { id: "tmpl-2", name: "Article" },
    ]);
    const [url] = fetchMock.mock.calls[0] as [string, RequestInit];
    expect(url).toContain("/sitemanage/sitetemplates/templates/");
    expect(url).toContain(encodeURIComponent("QaSite3002"));
  });

  it("fetchTemplatesForSite returns empty for the Jackson ArrayList bean", async () => {
    const fetchMock = globalThis.fetch as ReturnType<typeof vi.fn>;
    fetchMock.mockResolvedValue(
      mockJsonResponse({ TemplateSummary: { empty: false } }),
    );
    await expect(fetchTemplatesForSite("Demo")).resolves.toEqual([]);
  });

  it("fetchTemplatesForSectionCreate falls back to the readonly catalog (#3661)", async () => {
    const fetchMock = globalThis.fetch as ReturnType<typeof vi.fn>;
    fetchMock
      .mockResolvedValueOnce(
        mockJsonResponse({ TemplateSummary: [] }),
      )
      .mockResolvedValueOnce(
        mockJsonResponse({
          TemplateSummary: [{ id: "0-4-1050", name: "perc.base.plain" }],
        }),
      );
    const list = await fetchTemplatesForSectionCreate("Corporate_Investments");
    expect(list).toEqual([{ id: "0-4-1050", name: "perc.base.plain" }]);
    const urls = fetchMock.mock.calls.map((c) => String(c[0]));
    expect(urls[0]).toContain("/sitemanage/sitetemplates/templates/");
    expect(urls[1]).toContain("/pagemanagement/template/summary/all/readonly");
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

    it("does not map empty-body 500 to notAuthorizedMsg (#3726)", () => {
      const err: ApiError = {
        status: 500,
        statusText: "Internal Server Error",
        body: "",
      };
      expect(formatApiError(err, notAuth)).toBe("Internal Server Error");
      expect(
        formatApiError({ status: 500, statusText: "OK", body: null }, notAuth),
      ).toBe("Create failed");
    });
  });
});
