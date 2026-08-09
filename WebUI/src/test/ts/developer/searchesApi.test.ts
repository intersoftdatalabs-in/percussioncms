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
  executeSearch,
  unwrapSearchExecuteResult,
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
      folderPath: "/Sites/Foo",
      startIndex: 1,
      maxResults: 25,
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
