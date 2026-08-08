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
  listDisplayFormats,
  normalizeDisplayFormatColumns,
} from "../../../main/ts/api/contentExplorer/displayFormatsApi";
import { mockFetch } from "./setup";

describe("displayFormatsApi", () => {
  it("listDisplayFormats unwraps array envelope and passes filters", async () => {
    mockFetch(async (input) => {
      const url = typeof input === "string" ? input : (input as Request).url;
      expect(url).toContain("/displayformats");
      expect(url).toContain("validForFolder=true");
      return new Response(
        JSON.stringify({
          DisplayFormat: [
            { name: "FolderList", displayId: 1, validForFolder: true },
          ],
        }),
        { status: 200, headers: { "Content-Type": "application/json" } },
      );
    });
    const list = await listDisplayFormats({ validForFolder: true });
    expect(list).toHaveLength(1);
    expect(list[0].name).toBe("FolderList");
  });

  it("normalizeDisplayFormatColumns unwraps nested envelope", () => {
    expect(
      normalizeDisplayFormatColumns({
        DisplayFormatColumn: [{ source: "sys_title" }, { source: "type" }],
      }),
    ).toHaveLength(2);
    expect(normalizeDisplayFormatColumns(undefined)).toEqual([]);
  });
});
