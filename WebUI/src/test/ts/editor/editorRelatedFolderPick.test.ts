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
  isRelatedFolderChild,
  relatedFolderRows,
} from "../../../main/ts/editor/editorRelatedFolderPick";

describe("relatedFolderRows", () => {
  it("keeps folders for navigation and pages or assets as content ids", () => {
    const rows = relatedFolderRows([
      { name: "Sites", path: "/Sites", type: "site" },
      { name: "News", path: "/Sites/News/", type: "folder" },
      { id: "1-101-77", name: "About", path: "/Sites/About", type: "page" },
      { id: "88", name: "Logo", path: "/Assets/logo", category: "asset" },
      { name: "Untitled", path: "/Sites/ghost", type: "page" },
    ]);
    expect(rows.map((row) => [row.folder, row.contentId, row.name])).toEqual([
      [true, null, "Sites"],
      [true, null, "News"],
      [false, 77, "About"],
      [false, 88, "Logo"],
    ]);
    expect(isRelatedFolderChild({ path: "/Sites/section/", type: "page" })).toBe(true);
  });
});
