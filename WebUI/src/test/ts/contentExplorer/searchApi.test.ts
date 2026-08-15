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

import { afterEach, describe, expect, it, vi } from "vitest";
import {
  DEFAULT_SEARCH_FORMAT_ID,
  normalizeSearchCriteria,
  sanitizeQuery,
  searchExtended,
} from "../../../main/ts/api/contentExplorer/searchApi";
import { PATHS } from "../../../main/ts/api/paths";

afterEach(() => {
  vi.restoreAllMocks();
});

describe("normalizeSearchCriteria (GH-2950)", () => {
  it("defaults formatId and coerces startIndex < 1 for minimal free-text criteria", () => {
    const out = normalizeSearchCriteria({ query: "a", maxResults: 25 });
    expect(out.formatId).toBe(DEFAULT_SEARCH_FORMAT_ID);
    expect(out.startIndex).toBe(1);
    expect(out.query).toBe("a");
    expect(out.maxResults).toBe(25);
  });

  it("preserves explicit formatId and valid startIndex", () => {
    const out = normalizeSearchCriteria({
      query: "x",
      formatId: 12,
      startIndex: 5,
    });
    expect(out.formatId).toBe(12);
    expect(out.startIndex).toBe(5);
  });

  it("does not mutate the input object and omits client-only caseSensitive", () => {
    const input = {
      query: "Original",
      startIndex: 0,
      caseSensitive: true,
    };
    const out = normalizeSearchCriteria(input);
    expect(input.startIndex).toBe(0);
    expect(out.startIndex).toBe(1);
    expect(out).not.toHaveProperty("caseSensitive");
  });

  it("normalizes explorer folderPath to repository form (#3438)", () => {
    const out = normalizeSearchCriteria({
      query: "a",
      folderPath: "/Sites",
    });
    expect(out.folderPath).toBe("//Sites");
    expect(
      normalizeSearchCriteria({ query: "a", folderPath: "//Sites/Foo" })
        .folderPath,
    ).toBe("//Sites/Foo");
    expect(
      normalizeSearchCriteria({ query: "a" }).folderPath,
    ).toBeUndefined();
  });
});

describe("searchExtended wire shape", () => {
  it("POSTs the {SearchCriteria: ...} envelope and unwraps the PagedItemPropertiesList", async () => {
    const fetchMock = vi
      .spyOn(global, "fetch")
      .mockResolvedValueOnce(
        new Response(
          JSON.stringify({
            PagedItemPropertiesList: {
              childrenCount: 1,
              startIndex: 1,
              childrenInPage: [
                {
                  id: "1",
                  title: "Test Page",
                  folderPath: "/Sites/Foo",
                  type: "page",
                },
              ],
            },
          }),
          { status: 200, headers: { "Content-Type": "application/json" } },
        ),
      );
    const result = await searchExtended({
      query: "Test",
      startIndex: 0,
      maxResults: 10,
    });
    expect(fetchMock).toHaveBeenCalledTimes(1);
    const [url, init] = fetchMock.mock.calls[0] ?? [];
    expect(typeof url).toBe("string");
    expect(String(url)).toBe(PATHS.FINDER_SEARCH_EXTENDED);
    expect(init?.method).toBe("POST");
    // GH-2950: formatId required server-side; startIndex 0 coerced to 1
    expect(JSON.parse(String(init?.body))).toEqual({
      SearchCriteria: {
        query: "Test",
        startIndex: 1,
        maxResults: 10,
        formatId: DEFAULT_SEARCH_FORMAT_ID,
      },
    });
    expect(result.children).toHaveLength(1);
    expect(result.children[0]?.title).toBe("Test Page");
    expect(result.totalCount).toBe(1);
    expect(result.startIndex).toBe(1);
  });

  it("returns empty shape on envelope miss / empty body", async () => {
    vi.spyOn(global, "fetch").mockResolvedValueOnce(
      new Response(JSON.stringify({}), {
        status: 200,
        headers: { "Content-Type": "application/json" },
      }),
    );
    const result = await searchExtended({ query: "", startIndex: 5 });
    expect(result.children).toEqual([]);
    // totalCount defaults to 0 when the envelope is missing (defensive);
    // startIndex is preserved from the supplied criteria.
    expect(result.totalCount).toBe(0);
    expect(result.startIndex).toBe(5);
  });

  it("uses the startIndex supplied in the criteria when the response omits it", async () => {
    vi.spyOn(global, "fetch").mockResolvedValueOnce(
      new Response(
        JSON.stringify({
          PagedItemPropertiesList: { childrenInPage: [] },
        }),
        { status: 200, headers: { "Content-Type": "application/json" } },
      ),
    );
    const result = await searchExtended({ query: "x", startIndex: 42 });
    expect(result.startIndex).toBe(42);
  });

  it("is tolerant of the server returning childrenInPage as null/missing", async () => {
    vi.spyOn(global, "fetch").mockResolvedValueOnce(
      new Response(
        JSON.stringify({
          PagedItemPropertiesList: { childrenCount: 0, startIndex: 0 },
        }),
        { status: 200, headers: { "Content-Type": "application/json" } },
      ),
    );
    const result = await searchExtended({ query: "x", startIndex: 0 });
    expect(result.children).toEqual([]);
  });

  it("does not mutate the input criteria (POST wraps a fresh object)", async () => {
    const fetchMock = vi.spyOn(global, "fetch").mockResolvedValueOnce(
      new Response(JSON.stringify({ PagedItemPropertiesList: { childrenInPage: [] } }), {
        status: 200,
        headers: { "Content-Type": "application/json" },
      }),
    );
    const criteria = {
      query: "Original",
      startIndex: 0,
      maxResults: 25,
    };
    await searchExtended(criteria);
    expect(criteria.query).toBe("Original");
    expect(criteria.startIndex).toBe(0);
    // Fetch was called with a wrapped + normalized body; local criteria untouched.
    const init = fetchMock.mock.calls[0]?.[1];
    const body = JSON.parse(String(init?.body));
    expect(body.SearchCriteria.query).toBe("Original");
    expect(body.SearchCriteria.formatId).toBe(DEFAULT_SEARCH_FORMAT_ID);
    expect(body.SearchCriteria.startIndex).toBe(1);
  });

  it("sends formatId on a simple query-only search (Explorer GH-2950 shape)", async () => {
    const fetchMock = vi.spyOn(global, "fetch").mockResolvedValueOnce(
      new Response(
        JSON.stringify({
          PagedItemPropertiesList: { childrenCount: 0, childrenInPage: [] },
        }),
        { status: 200, headers: { "Content-Type": "application/json" } },
      ),
    );
    await searchExtended({ query: "a", startIndex: 1, maxResults: 25 });
    const body = JSON.parse(String(fetchMock.mock.calls[0]?.[1]?.body));
    expect(body).toEqual({
      SearchCriteria: {
        query: "a",
        startIndex: 1,
        maxResults: 25,
        formatId: DEFAULT_SEARCH_FORMAT_ID,
      },
    });
  });
});

describe("sanitizeQuery (defensive mirror of the server-side sanitizer)", () => {
  it("strips control characters", () => {
    expect(sanitizeQuery("hello\u0000world")).toBe("helloworld");
  });

  it("escapes Lucene special characters", () => {
    const escaped = sanitizeQuery("foo+bar-baz!");
    // Plus / minus / bang should be backslash-escaped.
    expect(escaped).toBe("foo\\+bar\\-baz\\!");
  });

  it("leaves a plain alphanumeric query untouched", () => {
    expect(sanitizeQuery("hello world")).toBe("hello world");
  });
});
