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
  formatMultiFolderMoveStatus,
  moveCheckedItemsToFolder,
  multiMoveOutcome,
  partitionMultiFolderMove,
} from "../../../main/ts/contentExplorer/multiFolderMove";

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

describe("multi-select move to one folder (#4856)", () => {
  it("skips folders by name and moves only pages and assets", () => {
    const parts = partitionMultiFolderMove([
      folder("Section", "/Assets/Section"),
      page("Home", "/Assets/Section/Home"),
      page("Logo", "/Assets/Section/Logo"),
    ]);
    expect(parts.skippedFolderNames).toEqual(["Section"]);
    expect(parts.movable.map((item) => item.name)).toEqual(["Home", "Logo"]);
  });

  it("one destination receives every movable item", async () => {
    const calls: Array<[string, string]> = [];
    const result = await moveCheckedItemsToFolder(
      [page("Home", "/Assets/Home"), page("Logo", "/Assets/Logo")],
      "/Assets/Dest",
      async (source, target) => {
        calls.push([source, target]);
      },
    );
    expect(calls).toEqual([
      ["/Assets/Home", "/Assets/Dest"],
      ["/Assets/Logo", "/Assets/Dest"],
    ]);
    expect(result.fullSuccess).toBe(true);
    expect(multiMoveOutcome(result)).toBe("success");
    expect(formatMultiFolderMoveStatus(result, "/Assets/Dest")).toContain("2");
    expect(formatMultiFolderMoveStatus(result, "/Assets/Dest")).not.toContain(
      message(EXPLORER_MSG.MULTI_MOVE_PARTIAL).slice(0, 12),
    );
  });

  it("keeps earlier moves when a later HTTP move fails", async () => {
    const moved: string[] = [];
    const result = await moveCheckedItemsToFolder(
      [
        folder("SkipMe", "/Assets/SkipMe"),
        page("Ok", "/Assets/Ok"),
        page("Missing", "/Assets/Missing"),
      ],
      "/Assets/Dest",
      async (source) => {
        if (source.endsWith("Missing")) {
          throw { status: 404, statusText: "Not Found", body: {} };
        }
        moved.push(source);
      },
    );
    expect(moved).toEqual(["/Assets/Ok"]);
    expect(result.movedNames).toEqual(["Ok"]);
    expect(result.skippedFolderNames).toEqual(["SkipMe"]);
    expect(result.failures.map((failure) => failure.name)).toEqual(["Missing"]);
    expect(result.fullSuccess).toBe(false);
    expect(multiMoveOutcome(result)).toBe("partial");
    const text = formatMultiFolderMoveStatus(result, "/Assets/Dest");
    expect(text).toContain("SkipMe");
    expect(text).toContain("Missing");
    expect(text).toContain("were not rolled back");
    expect(text).not.toBe(
      message(EXPLORER_MSG.MULTI_MOVE_SUCCESS)
        .replace("{count}", "1")
        .replace("{path}", "/Assets/Dest"),
    );
  });

  it("does not report success when every checked row is a folder", async () => {
    const result = await moveCheckedItemsToFolder(
      [folder("Only", "/Assets/Only")],
      "/Assets/Dest",
      async () => {
        throw new Error("must not move a folder");
      },
    );
    expect(result.fullSuccess).toBe(false);
    expect(multiMoveOutcome(result)).toBe("none");
    expect(formatMultiFolderMoveStatus(result, "/Assets/Dest")).toContain(
      "Only",
    );
    expect(formatMultiFolderMoveStatus(result, "/Assets/Dest")).toContain(
      message(EXPLORER_MSG.MULTI_MOVE_NONE),
    );
  });
});
