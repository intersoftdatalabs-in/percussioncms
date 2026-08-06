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

import { describe, expect, it } from "vitest";
import {
  addNewFolder,
  deleteItem,
  encodePath,
  findChildren,
  findItemByPath,
  joinPathUrl,
  lastExisting,
  paginatedFolder,
  validatePath,
} from "../../../main/ts/api/contentExplorer/pathApi";
import { mockFetch } from "./setup";

describe("encodePath", () => {
  it("strips leading/trailing slashes for the folder/{path:.*} URL suffix", () => {
    // Wire form must be "Sites/Foo/Bar", never "/Sites/…" (that becomes folder//Sites → 400).
    expect(encodePath("/Sites/Foo/Bar")).toBe("Sites/Foo/Bar");
    expect(encodePath("Sites/Foo/Bar")).toBe("Sites/Foo/Bar");
    expect(encodePath("/Sites/")).toBe("Sites");
  });

  it("encodes spaces and special characters per segment", () => {
    expect(encodePath("/Folder With Space/Child")).toBe(
      "Folder%20With%20Space/Child",
    );
    expect(encodePath("/A&B/C D")).toBe("A%26B/C%20D");
  });

  it("handles the empty / root path as empty suffix (folder/)", () => {
    expect(encodePath("/")).toBe("");
    expect(encodePath("")).toBe("");
    expect(encodePath("///")).toBe("");
  });

  it("encodes each segment independently so the slash separator is preserved", () => {
    // Defensive: if a future change accidentally encodes /, this test fails.
    const out = encodePath("a/b/c");
    expect(out.split("/")).toHaveLength(3);
    expect(out).toBe("a/b/c");
  });
});

describe("joinPathUrl", () => {
  const base = "/Rhythmyx/services/pathmanagement/path/folder";

  it("appends a single trailing slash for CMS root paths", () => {
    expect(joinPathUrl(base, "/")).toBe(`${base}/`);
    expect(joinPathUrl(base, "")).toBe(`${base}/`);
    expect(joinPathUrl(base, "///")).toBe(`${base}/`);
  });

  it("never produces a double slash before the first segment", () => {
    expect(joinPathUrl(base, "/Sites/")).toBe(`${base}/Sites`);
    expect(joinPathUrl(base, "/Sites/Foo")).toBe(`${base}/Sites/Foo`);
    expect(joinPathUrl(base, "Sites/Foo")).toBe(`${base}/Sites/Foo`);
  });
});

describe("paginatedFolder", () => {
  it("builds the paginated URL with required pagination params", async () => {
    const fn = mockFetch(async (input) => {
      const url = typeof input === "string" ? input : (input as Request).url;
      expect(url).toContain("/pathmanagement/path/paginatedFolder/Sites/Foo");
      expect(url).not.toContain("paginatedFolder//");
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

describe("pathmanagement URL shape (no double-slash)", () => {
  function mockJson(body: unknown = { PathItem: [] }): { lastUrl: () => string } {
    let last = "";
    mockFetch(async (input) => {
      last = typeof input === "string" ? input : (input as Request).url;
      return new Response(JSON.stringify(body), {
        status: 200,
        headers: { "Content-Type": "application/json" },
      });
    });
    return { lastUrl: () => last };
  }

  it("findChildren root uses folder/ not folder//", async () => {
    const cap = mockJson({
      PathItem: [{ name: "Sites", path: "/Sites/" }],
    });
    await findChildren("/");
    expect(cap.lastUrl()).toMatch(/\/path\/folder\/$/);
    expect(cap.lastUrl()).not.toContain("folder//");
  });

  it("findChildren Sites strips leading slash", async () => {
    const cap = mockJson({ PathItem: [] });
    await findChildren("/Sites/");
    expect(cap.lastUrl()).toContain("/path/folder/Sites");
    expect(cap.lastUrl()).not.toContain("folder//Sites");
  });

  it("findItemByPath joins without double slash", async () => {
    const cap = mockJson({ PathItem: { name: "Foo", path: "/Sites/Foo" } });
    await findItemByPath("/Sites/Foo");
    expect(cap.lastUrl()).toContain("/path/item/Sites/Foo");
    expect(cap.lastUrl()).not.toContain("item//Sites");
  });

  it("addNewFolder joins path and query without double slash", async () => {
    const cap = mockJson({ PathItem: { name: "New", path: "/Sites/New" } });
    await addNewFolder("/Sites/", "New");
    expect(cap.lastUrl()).toContain("/path/addNewFolder/Sites?name=New");
    expect(cap.lastUrl()).not.toContain("addNewFolder//");
  });

  it("deleteItem joins without double slash", async () => {
    const cap = mockJson({});
    await deleteItem("/Sites/Foo");
    expect(cap.lastUrl()).toContain("/path/delete/Sites/Foo");
    expect(cap.lastUrl()).not.toContain("delete//");
  });

  it("validatePath joins without double slash", async () => {
    const cap = mockJson("ok");
    await validatePath("/Sites/");
    expect(cap.lastUrl()).toContain("/path/validate/Sites");
    expect(cap.lastUrl()).not.toContain("validate//");
  });

  it("lastExisting joins without double slash", async () => {
    const cap = mockJson("/Sites");
    await lastExisting("/Sites/Missing");
    expect(cap.lastUrl()).toContain("/path/lastExisting/Sites/Missing");
    expect(cap.lastUrl()).not.toContain("lastExisting//");
  });
});
