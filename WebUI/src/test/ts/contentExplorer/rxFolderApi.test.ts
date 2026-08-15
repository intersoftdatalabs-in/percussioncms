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
  addRxFolder,
  deleteRxFolder,
  encodeRxFolderPath,
  loadFolderByPath,
  moveRxFolderChildren,
  parentFolderPath,
  RX_FOLDER_REST_BASE,
  rxFolderToPathItem,
  saveRxFolder,
  wrapAddFolderRequest,
} from "../../../main/ts/api/contentExplorer/rxFolderApi";
import { mockFetch } from "./setup";

describe("encodeRxFolderPath", () => {
  it("encodes segments without inventing a third dialect", () => {
    expect(encodeRxFolderPath("/Folders/Foo Bar")).toBe("Folders/Foo%20Bar");
    expect(encodeRxFolderPath("//Sites/MySite")).toBe("Sites/MySite");
    expect(encodeRxFolderPath("Folders/Child")).toBe("Folders/Child");
  });
});

describe("parentFolderPath", () => {
  it("returns parent for nested finder and repo paths", () => {
    expect(parentFolderPath("/Folders/A/B")).toBe("/Folders/A");
    expect(parentFolderPath("//Folders/A/B")).toBe("//Folders/A");
    expect(parentFolderPath("/Sites/MySite")).toBe("/Sites");
  });

  it("returns null at root folders", () => {
    expect(parentFolderPath("/Folders")).toBeNull();
    expect(parentFolderPath("//Sites")).toBeNull();
    expect(parentFolderPath("/")).toBeNull();
  });
});

describe("rxFolderToPathItem", () => {
  it("maps REST folder to PSPathItem with finder-style path", () => {
    const item = rxFolderToPathItem({
      id: "1-101-9",
      name: "Child",
      path: "//Folders/Child",
    });
    expect(item.id).toBe("1-101-9");
    expect(item.name).toBe("Child");
    expect(item.path).toBe("/Folders/Child");
    expect(item.type).toBe("folder");
  });
});

describe("wrapAddFolderRequest", () => {
  it("wraps flat name/parentPath under AddFolderRequest for JAXB", () => {
    expect(
      wrapAddFolderRequest({ name: "New", parentPath: "/Folders" }),
    ).toEqual({
      AddFolderRequest: { name: "New", parentPath: "/Folders" },
    });
  });

  it("includes optional sourcePath", () => {
    expect(
      wrapAddFolderRequest({
        name: "New",
        parentPath: "/Sites/Help",
        sourcePath: "/Sites/Help/Src",
      }),
    ).toEqual({
      AddFolderRequest: {
        name: "New",
        parentPath: "/Sites/Help",
        sourcePath: "/Sites/Help/Src",
      },
    });
  });

  it("does not double-wrap an already enveloped body", () => {
    expect(
      wrapAddFolderRequest({
        AddFolderRequest: { name: "X", parentPath: "/Folders" },
      }),
    ).toEqual({
      AddFolderRequest: { name: "X", parentPath: "/Folders" },
    });
  });
});

describe("rxFolderApi HTTP", () => {
  it("loadFolderByPath hits content-explorer folders by-path", async () => {
    let last = "";
    mockFetch(async (input) => {
      last = typeof input === "string" ? input : (input as Request).url;
      return new Response(
        JSON.stringify({ id: "1-101-1", name: "Folders", path: "//Folders" }),
        { status: 200, headers: { "Content-Type": "application/json" } },
      );
    });
    const folder = await loadFolderByPath("/Folders");
    expect(last).toContain(`${RX_FOLDER_REST_BASE}/by-path/Folders`);
    expect(folder.name).toBe("Folders");
  });

  it("addRxFolder POSTs AddFolderRequest root with name + parentPath", async () => {
    let method = "";
    let body: unknown;
    mockFetch(async (input, init) => {
      method = (init as RequestInit)?.method ?? "GET";
      body = JSON.parse(String((init as RequestInit)?.body ?? "{}"));
      return new Response(
        JSON.stringify({ id: "9-101-1", name: "New", path: "//Folders/New" }),
        { status: 200, headers: { "Content-Type": "application/json" } },
      );
    });
    const created = await addRxFolder("/Folders", "New");
    expect(method).toBe("POST");
    expect(body).toEqual({
      AddFolderRequest: { name: "New", parentPath: "/Folders" },
    });
    expect(created.id).toBe("9-101-1");
  });

  it("saveRxFolder PUTs by id", async () => {
    let method = "";
    let url = "";
    mockFetch(async (input, init) => {
      method = (init as RequestInit)?.method ?? "GET";
      url = typeof input === "string" ? input : (input as Request).url;
      return new Response(
        JSON.stringify({ id: "9-101-1", name: "Renamed" }),
        { status: 200, headers: { "Content-Type": "application/json" } },
      );
    });
    await saveRxFolder("9-101-1", { name: "Renamed" });
    expect(method).toBe("PUT");
    expect(url).toContain("/by-id/9-101-1");
  });

  it("moveRxFolderChildren POSTs move-children", async () => {
    let url = "";
    let body: unknown;
    mockFetch(async (input, init) => {
      url = typeof input === "string" ? input : (input as Request).url;
      body = JSON.parse(String((init as RequestInit)?.body ?? "{}"));
      return new Response(null, { status: 204 });
    });
    await moveRxFolderChildren({
      sourcePath: "/Folders",
      targetPath: "/Folders/Other",
      childIds: ["9-101-1"],
    });
    expect(url).toContain("/move-children");
    expect(body).toMatchObject({
      sourcePath: "/Folders",
      targetPath: "/Folders/Other",
      childIds: ["9-101-1"],
    });
  });

  it("deleteRxFolder DELETEs by id with purge query", async () => {
    let method = "";
    let url = "";
    mockFetch(async (input, init) => {
      method = (init as RequestInit)?.method ?? "GET";
      url = typeof input === "string" ? input : (input as Request).url;
      return new Response(null, { status: 204 });
    });
    await deleteRxFolder("9-101-1", false);
    expect(method).toBe("DELETE");
    expect(url).toContain("/by-id/9-101-1");
    expect(url).toContain("purge=false");
  });
});
