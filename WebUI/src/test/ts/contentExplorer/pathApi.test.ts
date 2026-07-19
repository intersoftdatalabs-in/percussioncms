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
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import { describe, expect, it } from "vitest";
import { encodePath, paginatedFolder } from "../../../main/ts/api/contentExplorer/pathApi";
import { mockFetch } from "./setup";

describe("encodePath", () => {
  it("preserves the multi-segment shape for CMS paths", () => {
    expect(encodePath("/Sites/Foo/Bar")).toBe("/Sites/Foo/Bar");
    expect(encodePath("Sites/Foo/Bar")).toBe("Sites/Foo/Bar");
  });

  it("encodes spaces and special characters per segment", () => {
    expect(encodePath("/Folder With Space/Child")).toBe(
      "/Folder%20With%20Space/Child",
    );
    expect(encodePath("/A&B/C D")).toBe("/A%26B/C%20D");
  });

  it("handles the empty / root path", () => {
    expect(encodePath("/")).toBe("/");
    expect(encodePath("")).toBe("");
  });

  it("encodes each segment independently so the slash separator is preserved", () => {
    // Defensive: if a future change accidentally encodes /, this test fails.
    const out = encodePath("a/b/c");
    expect(out.split("/")).toHaveLength(3);
    expect(out).toBe("a/b/c");
  });
});

describe("paginatedFolder", () => {
  it("builds the paginated URL with required pagination params", async () => {
    const fn = mockFetch(async (input) => {
      const url = typeof input === "string" ? input : (input as Request).url;
      expect(url).toContain("/pathmanagement/path/paginatedFolder/Sites/Foo");
      expect(url).toContain("startIndex=0");
      expect(url).toContain("maxResults=50");
      return new Response(
        JSON.stringify({ startIndex: 0, maxResults: 50, children: [] }),
        { status: 200, headers: { "Content-Type": "application/json" } },
      );
    });
    const res = await paginatedFolder("/Sites/Foo", {
      startIndex: 0,
      maxResults: 50,
    });
    expect(res.children ?? res.items ?? []).toEqual([]);
    expect(fn).toHaveBeenCalledTimes(1);
  });

  it("passes optional sortColumn / sortOrder / category / type when set", async () => {
    const fn = mockFetch(async (input) => {
      const url = typeof input === "string" ? input : (input as Request).url;
      expect(url).toContain("sortColumn=name");
      expect(url).toContain("sortOrder=asc");
      expect(url).toContain("category=page");
      expect(url).toContain("type=html");
      expect(url).toContain("child=true");
      return new Response(JSON.stringify({ startIndex: 0, maxResults: 50 }), {
        status: 200,
        headers: { "Content-Type": "application/json" },
      });
    });
    await paginatedFolder("/Sites/Foo", {
      startIndex: 0,
      maxResults: 50,
      sortColumn: "name",
      sortOrder: "asc",
      category: "page",
      type: "html",
      child: true,
    });
    expect(fn).toHaveBeenCalledTimes(1);
  });

  it("propagates HTTP errors via the typed ApiError contract", async () => {
    mockFetch(async () => {
      return new Response(
        JSON.stringify({ message: "permission denied" }),
        { status: 403, headers: { "Content-Type": "application/json" } },
      );
    });
    await expect(
      paginatedFolder("/Sites/Foo", { startIndex: 0, maxResults: 50 }),
    ).rejects.toMatchObject({ status: 403 });
  });
});