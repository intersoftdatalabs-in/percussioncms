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

import { describe, expect, it } from "vitest";
import {
  explorerRouteForFolder,
  explorerRouteForItemPath,
  openFolderNotice,
} from "../../../main/ts/editor/editorOpenFolder";

describe("editorOpenFolder", () => {
  it("maps a repository item path to the explorer route", () => {
    expect(explorerRouteForItemPath("//Folders/Lab/Home")).toBe(
      "/explorer?path=%2FFolders%2FLab",
    );
  });

  it("accepts a single-slash folder path", () => {
    expect(explorerRouteForFolder("/Sites/Demo")).toBe(
      "/explorer?path=%2FSites%2FDemo",
    );
  });

  it("does not navigate when the item has no folder", () => {
    expect(explorerRouteForItemPath("")).toBeNull();
    expect(explorerRouteForItemPath("/")).toBeNull();
    expect(explorerRouteForFolder(null)).toBeNull();
  });

  it("rejects traversal in the folder path", () => {
    expect(explorerRouteForFolder("/Sites/../etc")).toBeNull();
  });

  it("keeps 403 and 404 distinct from a generic failure", () => {
    expect(openFolderNotice({ status: 403, statusText: "Forbidden", body: {} })).toBe(
      "forbidden",
    );
    expect(openFolderNotice({ status: 404, statusText: "Not Found", body: {} })).toBe(
      "not_found",
    );
    expect(openFolderNotice(new Error("boom"))).toBe("failed");
  });
});
