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

import { afterEach, beforeEach, describe, expect, it } from "vitest";
import {
  addNewFolder,
  deleteItem,
  moveItem,
  renameFolder,
} from "../../../main/ts/api/contentExplorer/folderMutations";
import { setRxFolderMutationsFlagOverride } from "../../../main/ts/api/contentExplorer/rxFolderMutationsFlag";
import { mockFetch } from "./setup";

describe("folderMutations dual-run routing (#3074)", () => {
  beforeEach(() => {
    setRxFolderMutationsFlagOverride(null);
  });

  afterEach(() => {
    setRxFolderMutationsFlagOverride(null);
  });

  it("flag off: addNewFolder uses pathmanagement", async () => {
    setRxFolderMutationsFlagOverride(false);
    let last = "";
    mockFetch(async (input) => {
      last = typeof input === "string" ? input : (input as Request).url;
      return new Response(
        JSON.stringify({ PathItem: { name: "New", path: "/Folders/New" } }),
        { status: 200, headers: { "Content-Type": "application/json" } },
      );
    });
    await addNewFolder("/Folders", "New");
    expect(last).toContain("/pathmanagement/path/addNewFolder");
    expect(last).not.toContain("/content-explorer/folders");
  });

  it("flag on + RX path: addNewFolder uses content-explorer folders REST", async () => {
    setRxFolderMutationsFlagOverride(true);
    let last = "";
    let body: unknown;
    mockFetch(async (input, init) => {
      last = typeof input === "string" ? input : (input as Request).url;
      body = JSON.parse(String((init as RequestInit)?.body ?? "{}"));
      return new Response(
        JSON.stringify({ id: "1-101-9", name: "New", path: "//Folders/New" }),
        { status: 200, headers: { "Content-Type": "application/json" } },
      );
    });
    const item = await addNewFolder("/Folders", "New");
    expect(last).toContain("/content-explorer/folders");
    expect(last).not.toContain("/pathmanagement/");
    expect(body).toEqual({
      AddFolderRequest: { name: "New", parentPath: "/Folders" },
    });
    expect(item.path).toBe("/Folders/New");
    expect(item.id).toBe("1-101-9");
  });

  it("flag on + non-RX path: stays on pathmanagement", async () => {
    setRxFolderMutationsFlagOverride(true);
    let last = "";
    mockFetch(async (input) => {
      last = typeof input === "string" ? input : (input as Request).url;
      return new Response(
        JSON.stringify({ PathItem: { name: "X", path: "/Assets/X" } }),
        { status: 200, headers: { "Content-Type": "application/json" } },
      );
    });
    await addNewFolder("/Assets", "X");
    expect(last).toContain("/pathmanagement/path/addNewFolder");
  });

  it("flag on: renameFolder loads by path then PUTs by id", async () => {
    setRxFolderMutationsFlagOverride(true);
    const urls: string[] = [];
    mockFetch(async (input, init) => {
      const url = typeof input === "string" ? input : (input as Request).url;
      urls.push(url);
      const method = (init as RequestInit)?.method ?? "GET";
      if (method === "GET") {
        return new Response(
          JSON.stringify({ id: "2-101-1", name: "Old", path: "//Folders/Old" }),
          { status: 200, headers: { "Content-Type": "application/json" } },
        );
      }
      return new Response(
        JSON.stringify({ id: "2-101-1", name: "NewName", path: "//Folders/NewName" }),
        { status: 200, headers: { "Content-Type": "application/json" } },
      );
    });
    const item = await renameFolder({ path: "/Folders/Old", newName: "NewName" });
    expect(urls.some((u) => u.includes("/by-path/"))).toBe(true);
    expect(urls.some((u) => u.includes("/by-id/2-101-1"))).toBe(true);
    expect(item.name).toBe("NewName");
  });

  it("flag on: moveItem uses move-children with derived parent + id", async () => {
    setRxFolderMutationsFlagOverride(true);
    const calls: Array<{ url: string; method: string; body?: unknown }> = [];
    mockFetch(async (input, init) => {
      const url = typeof input === "string" ? input : (input as Request).url;
      const method = (init as RequestInit)?.method ?? "GET";
      const rawBody = (init as RequestInit)?.body;
      calls.push({
        url,
        method,
        body: rawBody ? JSON.parse(String(rawBody)) : undefined,
      });
      if (method === "GET") {
        return new Response(
          JSON.stringify({ id: "3-101-1", name: "Child", path: "//Folders/Child" }),
          { status: 200, headers: { "Content-Type": "application/json" } },
        );
      }
      return new Response(null, { status: 204 });
    });
    await moveItem({
      sourcePath: "/Folders/Child",
      targetPath: "/Folders/Other",
    });
    const move = calls.find((c) => c.url.includes("/move-children"));
    expect(move).toBeDefined();
    expect(move!.body).toMatchObject({
      sourcePath: "/Folders",
      targetPath: "/Folders/Other",
      childIds: ["3-101-1"],
    });
  });

  it("copy always uses pathmanagement even when flag on", async () => {
    setRxFolderMutationsFlagOverride(true);
    let last = "";
    mockFetch(async (input) => {
      last = typeof input === "string" ? input : (input as Request).url;
      return new Response(
        JSON.stringify({ PathItem: { name: "C", path: "/Folders/C" } }),
        { status: 200, headers: { "Content-Type": "application/json" } },
      );
    });
    await moveItem({
      sourcePath: "/Folders/Child",
      targetPath: "/Folders/Other",
      copy: true,
    });
    expect(last).toContain("/pathmanagement/path/moveItem");
  });

  it("flag on: deleteItem DELETEs by resolved folder id", async () => {
    setRxFolderMutationsFlagOverride(true);
    const urls: string[] = [];
    mockFetch(async (input, init) => {
      const url = typeof input === "string" ? input : (input as Request).url;
      urls.push(url);
      const method = (init as RequestInit)?.method ?? "GET";
      if (method === "GET") {
        return new Response(
          JSON.stringify({ id: "4-101-1", name: "Gone", path: "//Folders/Gone" }),
          { status: 200, headers: { "Content-Type": "application/json" } },
        );
      }
      return new Response(null, { status: 204 });
    });
    await deleteItem("/Folders/Gone");
    expect(urls.some((u) => u.includes("DELETE") || u.includes("/by-id/4-101-1"))).toBe(
      true,
    );
    expect(urls.some((u) => u.includes("/by-id/4-101-1") && u.includes("purge="))).toBe(
      true,
    );
  });

  it("flag off: deleteItem uses pathmanagement", async () => {
    setRxFolderMutationsFlagOverride(false);
    let last = "";
    mockFetch(async (input) => {
      last = typeof input === "string" ? input : (input as Request).url;
      return new Response(JSON.stringify({}), {
        status: 200,
        headers: { "Content-Type": "application/json" },
      });
    });
    await deleteItem("/Folders/Gone");
    expect(last).toContain("/pathmanagement/path/delete");
  });
});
