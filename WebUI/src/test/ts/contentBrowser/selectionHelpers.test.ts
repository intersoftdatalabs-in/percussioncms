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

import { describe, it, expect } from "vitest";
import {
  appendUniqueById,
  selectionItemFromSearchResult,
} from "../../../main/ts/contentBrowser/selectionHelpers";

describe("appendUniqueById", () => {
  it("appends a new id and ignores repeats", () => {
    const a = { id: "p-1" };
    const once = appendUniqueById([], a);
    expect(once).toEqual([a]);
    expect(appendUniqueById(once, a)).toEqual([a]);
  });
});

describe("selectionItemFromSearchResult (edge cases)", () => {
  it("joins folder + name and collapses trailing slashes on folder", () => {
    const sel = selectionItemFromSearchResult({
      id: "x",
      name: "Child",
      folderPath: "/Sites/Parent/",
      type: "folder",
    });
    expect(sel.path).toBe("/Sites/Parent/Child");
    expect(sel.category).toBe("folder");
  });

  it("falls back id to path/name/unknown when id blank", () => {
    const viaPath = selectionItemFromSearchResult({
      id: "  ",
      name: "A",
      folderPath: "/Sites",
      type: "page",
    });
    expect(viaPath.id).toBe("/Sites/A");

    const viaUnknown = selectionItemFromSearchResult({
      id: "",
      name: "",
      folderPath: "",
      type: "page",
    });
    expect(viaUnknown.id).toBe("unknown");
  });
});
