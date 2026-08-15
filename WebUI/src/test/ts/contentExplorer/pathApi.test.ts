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
  copyFolder,
  COPY_FOLDER_ITEM_REQUEST_ROOT,
  deleteItem,
  encodePath,
  findChildren,
  findItemByPath,
  folderProperties,
  FOLDER_PROPERTIES_ROOT,
  joinPathUrl,
  lastExisting,
  moveItem,
  MOVE_FOLDER_ITEM_ROOT,
  paginatedFolder,
  saveFolderProperties,
  unwrapFolderProperties,
  unwrapPrincipalList,
  validatePath,
  wrapCopyFolderItemRequest,
  wrapFolderProperties,
  wrapMoveFolderItem,
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

  it("findChildren throws when PathItem is a PSErrors envelope (#3196)", async () => {
    mockJson({
      PathItem: [
        {
          Errors: {
            globalError: {
              defaultMessage: "1 counts of IllegalAnnotationExceptions",
            },
          },
        },
      ],
    });
    await expect(findChildren("/")).rejects.toMatchObject({
      status: 500,
      body: {
        PathItem: [{ Errors: { globalError: expect.any(Object) } }],
      },
    });
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

/**
 * #2749 — Jackson WRAP_ROOT_VALUE / UNWRAP_ROOT_VALUE for PSFolderProperties.
 * GET must unwrap FolderProperties; POST must wrap so server id is non-null.
 */
describe("folderProperties Jackson root wrap (#2749)", () => {
  it("unwrapFolderProperties prefers FolderProperties root", () => {
    const props = unwrapFolderProperties({
      [FOLDER_PROPERTIES_ROOT]: {
        id: "16777215-101-703",
        name: "Design",
        permission: { accessLevel: "ADMIN" },
        locale: "en-us",
      },
    });
    expect(props).not.toBeNull();
    expect(props!.id).toBe("16777215-101-703");
    expect(props!.name).toBe("Design");
    expect(props!.permission?.accessLevel).toBe("ADMIN");
    expect(props!.locale).toBe("en-us");
  });

  it("unwrapFolderProperties accepts flat body for tests", () => {
    const props = unwrapFolderProperties({
      id: "1-101-1",
      name: "Flat",
      permission: { accessLevel: "WRITE" },
    });
    expect(props?.id).toBe("1-101-1");
    expect(props?.permission?.accessLevel).toBe("WRITE");
  });

  it("unwrapFolderProperties returns null for empty envelope", () => {
    expect(unwrapFolderProperties({})).toBeNull();
    expect(unwrapFolderProperties(null)).toBeNull();
    expect(unwrapFolderProperties({ [FOLDER_PROPERTIES_ROOT]: {} })).toBeNull();
  });

  it("wrapFolderProperties nests under FolderProperties root", () => {
    const wrapped = wrapFolderProperties({
      id: "9-101-1",
      name: "Assets",
      permission: { accessLevel: "ADMIN" },
    });
    expect(Object.keys(wrapped)).toEqual([FOLDER_PROPERTIES_ROOT]);
    expect(wrapped[FOLDER_PROPERTIES_ROOT].id).toBe("9-101-1");
  });

  it("folderProperties GET unwraps Jackson FolderProperties envelope", async () => {
    mockFetch(async (input) => {
      const url = typeof input === "string" ? input : (input as Request).url;
      expect(url).toContain("/path/folderProperties/16777215-101-703");
      return new Response(
        JSON.stringify({
          FolderProperties: {
            id: "16777215-101-703",
            name: "Design",
            permission: { accessLevel: "ADMIN", adminPrincipals: [] },
            communityId: 1001,
            locale: "en-us",
            workflowId: -1,
          },
        }),
        { status: 200, headers: { "Content-Type": "application/json" } },
      );
    });
    const props = await folderProperties("16777215-101-703");
    expect(props.id).toBe("16777215-101-703");
    expect(props.name).toBe("Design");
    expect(props.permission?.accessLevel).toBe("ADMIN");
    expect(props.locale).toBe("en-us");
  });

  it("folderProperties rejects blank id client-side", async () => {
    await expect(folderProperties("")).rejects.toThrow(/non-empty folder id/);
    await expect(folderProperties("   ")).rejects.toThrow(/non-empty folder id/);
  });

  it("saveFolderProperties POSTs wrapped FolderProperties body with id", async () => {
    let posted: unknown;
    mockFetch(async (input, init) => {
      const url = typeof input === "string" ? input : (input as Request).url;
      expect(url).toContain("/path/saveFolderProperties");
      posted = JSON.parse(String(init?.body ?? "{}"));
      return new Response(JSON.stringify({ NoContent: { operation: "saveFolderProperties" } }), {
        status: 200,
        headers: { "Content-Type": "application/json" },
      });
    });
    await saveFolderProperties({
      id: "16777215-101-703",
      name: "Design",
      permission: {
        accessLevel: "ADMIN",
        adminPrincipals: [{ type: "USER", name: "Admin" }],
      },
      locale: "en-us",
    });
    expect(posted).toEqual({
      FolderProperties: {
        id: "16777215-101-703",
        name: "Design",
        permission: {
          accessLevel: "ADMIN",
          adminPrincipals: [{ type: "USER", name: "Admin" }],
        },
        locale: "en-us",
      },
    });
  });

  it("unwrapPrincipalList accepts array, JAXB wrap, and single principal (#3206)", () => {
    expect(
      unwrapPrincipalList([{ type: "ROLE", name: "Admin" }]),
    ).toEqual([{ type: "ROLE", name: "Admin" }]);
    expect(
      unwrapPrincipalList({
        Principal: [{ type: "ROLE", name: "Admin" }, { type: "USER", name: "alice" }],
      }),
    ).toEqual([
      { type: "ROLE", name: "Admin" },
      { type: "USER", name: "alice" },
    ]);
    expect(unwrapPrincipalList({ type: "USER", name: "solo" })).toEqual([
      { type: "USER", name: "solo" },
    ]);
    expect(unwrapPrincipalList(null)).toEqual([]);
  });

  it("unwrapFolderProperties normalizes JAXB-wrapped ROLE identities (#3206)", () => {
    const props = unwrapFolderProperties({
      FolderProperties: {
        id: "16777215-101-703",
        name: "Design",
        locale: "en-us",
        permission: {
          accessLevel: "ADMIN",
          adminPrincipals: { Principal: [{ type: "ROLE", name: "Admin" }] },
        },
      },
    });
    expect(props?.permission?.adminPrincipals).toEqual([
      { type: "ROLE", name: "Admin" },
    ]);
  });

  it("saveFolderProperties defaults missing permission and rejects missing id", async () => {
    let posted: unknown;
    mockFetch(async (_input, init) => {
      posted = JSON.parse(String(init?.body ?? "{}"));
      return new Response("{}", {
        status: 200,
        headers: { "Content-Type": "application/json" },
      });
    });
    await saveFolderProperties({
      id: "1-101-2",
      name: "NoPerm",
    });
    expect(
      (posted as { FolderProperties: { permission: { accessLevel: string } } })
        .FolderProperties.permission.accessLevel,
    ).toBe("ADMIN");

    await expect(
      saveFolderProperties({ id: "", name: "x" } as { id: string; name: string }),
    ).rejects.toThrow(/props\.id/);
  });
});

/**
 * #3362 — JAXB MoveFolderItem envelope. Copy must not POST a bare sourcePath
 * root (or an invented copy field) to moveItem.
 */
describe("moveItem / copyFolder wire envelopes (#3362)", () => {
  it("wrapMoveFolderItem maps sourcePath/targetPath to itemPath/targetFolderPath", () => {
    const wrapped = wrapMoveFolderItem({
      sourcePath: "/Folders/A",
      targetPath: "/Folders/B",
      copy: true,
    });
    expect(Object.keys(wrapped)).toEqual([MOVE_FOLDER_ITEM_ROOT]);
    expect(wrapped.MoveFolderItem).toEqual({
      itemPath: "/Folders/A",
      targetFolderPath: "/Folders/B",
    });
    expect(wrapped.MoveFolderItem).not.toHaveProperty("sourcePath");
    expect(wrapped.MoveFolderItem).not.toHaveProperty("copy");
  });

  it("wrapMoveFolderItem accepts itemPath aliases and does not double-wrap", () => {
    expect(
      wrapMoveFolderItem({
        MoveFolderItem: {
          itemPath: "/Sites/X",
          targetFolderPath: "/Sites/Y",
        },
      }),
    ).toEqual({
      MoveFolderItem: { itemPath: "/Sites/X", targetFolderPath: "/Sites/Y" },
    });
  });

  it("moveItem POSTs MoveFolderItem wrap, never a bare sourcePath root", async () => {
    let url = "";
    let posted: unknown;
    mockFetch(async (input, init) => {
      url = typeof input === "string" ? input : (input as Request).url;
      posted = JSON.parse(String((init as RequestInit)?.body ?? "{}"));
      return new Response("{}", {
        status: 200,
        headers: { "Content-Type": "application/json" },
      });
    });
    await moveItem({
      sourcePath: "/Folders/Child",
      targetPath: "/Folders/Other",
      copy: true,
    });
    expect(url).toContain("/pathmanagement/path/moveItem");
    expect(posted).toEqual({
      MoveFolderItem: {
        itemPath: "/Folders/Child",
        targetFolderPath: "/Folders/Other",
      },
    });
    expect(posted).not.toHaveProperty("sourcePath");
    expect(posted).not.toHaveProperty("copy");
  });

  it("wrapCopyFolderItemRequest uses CopyFolderItemRequest root", () => {
    const wrapped = wrapCopyFolderItemRequest({
      sourcePath: "/Folders/A",
      targetPath: "/Folders/B",
    });
    expect(Object.keys(wrapped)).toEqual([COPY_FOLDER_ITEM_REQUEST_ROOT]);
    expect(wrapped.CopyFolderItemRequest).toEqual({
      itemPath: "/Folders/A",
      targetFolderPath: "/Folders/B",
    });
  });

  it("copyFolder POSTs CopyFolderItemRequest, not moveItem / sourcePath root", async () => {
    let url = "";
    let posted: unknown;
    mockFetch(async (input, init) => {
      url = typeof input === "string" ? input : (input as Request).url;
      posted = JSON.parse(String((init as RequestInit)?.body ?? "{}"));
      return new Response("{}", {
        status: 200,
        headers: { "Content-Type": "application/json" },
      });
    });
    await copyFolder({
      sourcePath: "/Folders/Child",
      targetPath: "/Folders/Other",
    });
    expect(url).toContain("/folders/copy/folder");
    expect(url).not.toContain("/pathmanagement/path/moveItem");
    expect(posted).toEqual({
      CopyFolderItemRequest: {
        itemPath: "/Folders/Child",
        targetFolderPath: "/Folders/Other",
      },
    });
    expect(posted).not.toHaveProperty("sourcePath");
  });
});
