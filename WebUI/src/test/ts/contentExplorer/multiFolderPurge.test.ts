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
  formatMultiFolderPurgeStatus,
  multiPurgeOutcome,
  partitionMultiFolderPurge,
  purgeCheckedItems,
} from "../../../main/ts/contentExplorer/multiFolderPurge";

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

describe("multi-select purge (#4883)", () => {
  it("skips folders by name and purges only pages and assets", () => {
    const parts = partitionMultiFolderPurge([
      folder("Section", "//Folders/$System$/Recycling/Section"),
      page("Home", "//Folders/$System$/Recycling/Home"),
      page("Logo", "//Folders/$System$/Recycling/Logo"),
    ]);
    expect(parts.skippedFolderNames).toEqual(["Section"]);
    expect(parts.purgeable.map((item) => item.name)).toEqual(["Home", "Logo"]);
  });

  it("purges every page and asset once", async () => {
    const calls: string[] = [];
    const result = await purgeCheckedItems(
      [
        page("Home", "//Folders/$System$/Recycling/Home"),
        page("Logo", "//Folders/$System$/Recycling/Logo"),
      ],
      async (item) => {
        calls.push(item.id ?? "");
        return true;
      },
    );
    expect(calls).toEqual(["Home", "Logo"]);
    expect(result.fullSuccess).toBe(true);
    expect(multiPurgeOutcome(result)).toBe("success");
    expect(formatMultiFolderPurgeStatus(result)).toContain("2");
    expect(formatMultiFolderPurgeStatus(result)).not.toContain(
      message(EXPLORER_MSG.MULTI_PURGE_PARTIAL).slice(0, 12),
    );
  });

  it("keeps earlier purges when a later HTTP purge fails", async () => {
    const purged: string[] = [];
    const result = await purgeCheckedItems(
      [
        folder("SkipMe", "//Folders/$System$/Recycling/SkipMe"),
        page("Ok", "//Folders/$System$/Recycling/Ok"),
        page("Missing", "//Folders/$System$/Recycling/Missing"),
      ],
      async (item) => {
        if ((item.name || "").endsWith("Missing")) {
          throw { status: 404, statusText: "Not Found", body: {} };
        }
        purged.push(item.id ?? "");
        return true;
      },
    );
    expect(purged).toEqual(["Ok"]);
    expect(result.purgedNames).toEqual(["Ok"]);
    expect(result.skippedFolderNames).toEqual(["SkipMe"]);
    expect(result.failures.map((failure) => failure.name)).toEqual(["Missing"]);
    expect(result.fullSuccess).toBe(false);
    expect(multiPurgeOutcome(result)).toBe("partial");
    const text = formatMultiFolderPurgeStatus(result);
    expect(text).toContain("SkipMe");
    expect(text).toContain("Missing");
    expect(text).toContain("were not rolled back");
    expect(text).not.toBe(
      message(EXPLORER_MSG.MULTI_PURGE_SUCCESS).replace("{count}", "1"),
    );
  });

  it("does not report success when every checked row is a folder", async () => {
    const result = await purgeCheckedItems(
      [folder("Only", "//Folders/$System$/Recycling/Only")],
      async () => {
        throw new Error("must not purge a folder");
      },
    );
    expect(result.fullSuccess).toBe(false);
    expect(multiPurgeOutcome(result)).toBe("none");
    expect(formatMultiFolderPurgeStatus(result)).toContain("Only");
    expect(formatMultiFolderPurgeStatus(result)).toContain(
      message(EXPLORER_MSG.MULTI_PURGE_NONE),
    );
  });

  it("does not report success when purge returns false", async () => {
    const result = await purgeCheckedItems(
      [page("Nope", "//Folders/$System$/Recycling/Nope")],
      async () => false,
    );
    expect(result.fullSuccess).toBe(false);
    expect(multiPurgeOutcome(result)).toBe("none");
    expect(result.failures[0]?.message).toContain("not available");
  });
});
