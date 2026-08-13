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

import { afterEach, describe, expect, it, vi } from "vitest";
import {
  executeView,
  unwrapViewExecuteResult,
} from "../../../main/ts/api/contentExplorer/viewsApi";
import { PATHS } from "../../../main/ts/api/paths";

afterEach(() => {
  vi.restoreAllMocks();
});

describe("unwrapViewExecuteResult", () => {
  it("accepts a flat payload", () => {
    const out = unwrapViewExecuteResult({
      children: [{ id: "1", title: "A" }],
      totalCount: 1,
      startIndex: 1,
      viewName: "View_All",
    });
    expect(out.children).toHaveLength(1);
    expect(out.children?.[0]?.title).toBe("A");
    expect(out.totalCount).toBe(1);
    expect(out.viewName).toBe("View_All");
  });

  it("unwraps Jackson root name ViewExecuteResult", () => {
    const out = unwrapViewExecuteResult({
      ViewExecuteResult: {
        children: [{ id: "2", name: "b" }],
        totalCount: 2,
        startIndex: 1,
      },
    });
    expect(out.children).toHaveLength(1);
    expect(out.totalCount).toBe(2);
  });

  it("returns empty shape for null / non-object", () => {
    expect(unwrapViewExecuteResult(null).children).toEqual([]);
    expect(unwrapViewExecuteResult("x").children).toEqual([]);
  });
});

describe("executeView", () => {
  it("POSTs to /views/{id}/execute with optional overrides", async () => {
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
          viewName: "View_All",
        }),
        { status: 200, headers: { "Content-Type": "application/json" } },
      ),
    );

    const result = await executeView("View_All", {
      folderPath: "/Sites/Foo",
      startIndex: 1,
      maxResults: 25,
    });

    expect(fetchMock).toHaveBeenCalledTimes(1);
    const [url, init] = fetchMock.mock.calls[0] ?? [];
    expect(String(url)).toBe(
      `${PATHS.VIEWS}/${encodeURIComponent("View_All")}/execute`,
    );
    expect(init?.method).toBe("POST");
    expect(JSON.parse(String(init?.body))).toEqual({
      folderPath: "/Sites/Foo",
      startIndex: 1,
      maxResults: 25,
    });
    expect(result.children).toHaveLength(1);
    expect(result.children?.[0]?.title).toBe("Welcome");
    expect(result.viewName).toBe("View_All");
  });

  it("rejects blank idOrName without calling the network", async () => {
    const fetchMock = vi.spyOn(global, "fetch");
    await expect(executeView("   ")).rejects.toThrow(/required/i);
    expect(fetchMock).not.toHaveBeenCalled();
  });

  it("encodes special characters in the path segment", async () => {
    const fetchMock = vi.spyOn(global, "fetch").mockResolvedValueOnce(
      new Response(JSON.stringify({ children: [], totalCount: 0, startIndex: 1 }), {
        status: 200,
        headers: { "Content-Type": "application/json" },
      }),
    );
    await executeView("A/B View");
    const [url] = fetchMock.mock.calls[0] ?? [];
    expect(String(url)).toBe(
      `${PATHS.VIEWS}/${encodeURIComponent("A/B View")}/execute`,
    );
  });
});
