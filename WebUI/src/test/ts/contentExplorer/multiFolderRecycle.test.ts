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
import type { PSPathItem } from "../../../main/ts/api/contentExplorer/types";
import { EXPLORER_MSG } from "../../../main/ts/contentExplorer/messages";
import { message } from "../../../main/ts/i18n/message";
import {
  formatMultiFolderRecycleStatus,
  multiRecycleOutcome,
  partitionMultiFolderRecycle,
  recycleCheckedItems,
} from "../../../main/ts/contentExplorer/multiFolderRecycle";

const page = (name: string, path: string): PSPathItem => ({
  id: name,
  name,
  path,
  type: "percPage",
  category: "page",
  leaf: true,
});

const folder = (name: string, path: string): PSPathItem => ({
  id: name,
  name,
  path: path.endsWith("/") ? path : `${path}/`,
  type: "folder",
  category: "folder",
  leaf: false,
});

describe("multi-select recycle (#4857)", () => {
  it("skips folders by name and recycles only pages and assets", () => {
    const parts = partitionMultiFolderRecycle([
      folder("Section", "/Assets/Section"),
      page("Home", "/Assets/Section/Home"),
      page("Logo", "/Assets/Section/Logo"),
    ]);
    expect(parts.skippedFolderNames).toEqual(["Section"]);
    expect(parts.recyclable.map((item) => item.name)).toEqual(["Home", "Logo"]);
  });

  it("recycles every movable item", async () => {
    const calls: string[] = [];
    const result = await recycleCheckedItems(
      [page("Home", "/Assets/Home"), page("Logo", "/Assets/Logo")],
      async (source) => {
        calls.push(source);
      },
    );
    expect(calls).toEqual(["/Assets/Home", "/Assets/Logo"]);
    expect(result.fullSuccess).toBe(true);
    expect(multiRecycleOutcome(result)).toBe("success");
    expect(formatMultiFolderRecycleStatus(result)).toContain("2");
    expect(formatMultiFolderRecycleStatus(result)).not.toContain(
      message(EXPLORER_MSG.MULTI_RECYCLE_PARTIAL).slice(0, 12),
    );
  });

  it("keeps earlier recycles when a later HTTP recycle fails", async () => {
    const recycled: string[] = [];
    const result = await recycleCheckedItems(
      [
        folder("SkipMe", "/Assets/SkipMe"),
        page("Ok", "/Assets/Ok"),
        page("Missing", "/Assets/Missing"),
      ],
      async (source) => {
        if (source.endsWith("Missing")) {
          throw { status: 404, statusText: "Not Found", body: {} };
        }
        recycled.push(source);
      },
    );
    expect(recycled).toEqual(["/Assets/Ok"]);
    expect(result.recycledNames).toEqual(["Ok"]);
    expect(result.skippedFolderNames).toEqual(["SkipMe"]);
    expect(result.failures.map((failure) => failure.name)).toEqual(["Missing"]);
    expect(result.fullSuccess).toBe(false);
    expect(multiRecycleOutcome(result)).toBe("partial");
    const text = formatMultiFolderRecycleStatus(result);
    expect(text).toContain("SkipMe");
    expect(text).toContain("Missing");
    expect(text).toContain("were not rolled back");
    expect(text).not.toBe(
      message(EXPLORER_MSG.MULTI_RECYCLE_SUCCESS).replace("{count}", "1"),
    );
  });

  it("does not report success when every checked row is a folder", async () => {
    const result = await recycleCheckedItems(
      [folder("Only", "/Assets/Only")],
      async () => {
        throw new Error("must not recycle a folder");
      },
    );
    expect(result.fullSuccess).toBe(false);
    expect(multiRecycleOutcome(result)).toBe("none");
    expect(formatMultiFolderRecycleStatus(result)).toContain("Only");
    expect(formatMultiFolderRecycleStatus(result)).toContain(
      message(EXPLORER_MSG.MULTI_RECYCLE_NONE),
    );
  });
});
