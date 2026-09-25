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
import {
  formatMultiSelectPurgeStatus,
  multiPurgeOutcome,
  partitionMultiSelectPurge,
  purgeCheckedItems,
} from "../../../main/ts/contentExplorer/multiSelectPurge";
import { message } from "../../../main/ts/i18n/message";

const page = (name: string, id: string): PSPathItem => ({
  id,
  name,
  path: `/Recycling/${name}`,
  type: "percPage",
  category: "page",
  leaf: true,
});

const folder = (name: string): PSPathItem => ({
  id: name,
  name,
  path: `/Recycling/${name}/`,
  type: "folder",
  category: "folder",
  leaf: false,
});

describe("multi-select purge (#4883)", () => {
  it("skips folders by name and purges only pages and assets", () => {
    const parts = partitionMultiSelectPurge([
      folder("News"),
      page("Home", "guid-home"),
      page("Logo", "guid-logo"),
    ]);
    expect(parts.skippedFolderNames).toEqual(["News"]);
    expect(parts.purgable.map((item) => item.id)).toEqual([
      "guid-home",
      "guid-logo",
    ]);
  });

  it("purges every page and asset with one guid each", async () => {
    const calls: string[] = [];
    const items = [page("Home", "guid-home"), page("Logo", "guid-logo")];
    const result = await purgeCheckedItems(items, async (guid) => {
      calls.push(guid);
    });
    expect(calls).toEqual(["guid-home", "guid-logo"]);
    expect(result.fullSuccess).toBe(true);
    expect(multiPurgeOutcome(result)).toBe("success");
    expect(formatMultiSelectPurgeStatus(result)).toContain("2");
  });

  it("does not call purge when the caller never confirms", async () => {
    const calls: string[] = [];
    const confirmed = false;
    if (confirmed) {
      await purgeCheckedItems([page("Home", "guid-home")], async (guid) => {
        calls.push(guid);
      });
    }
    expect(calls).toEqual([]);
  });

  it("names skipped folders and a later HTTP failure without full success", async () => {
    const purged: string[] = [];
    const result = await purgeCheckedItems(
      [
        folder("SkipMe"),
        page("Ok", "guid-ok"),
        page("Missing", "guid-miss"),
      ],
      async (guid) => {
        if (guid === "guid-miss") {
          throw { status: 404, statusText: "Not Found", body: {} };
        }
        purged.push(guid);
      },
    );
    expect(purged).toEqual(["guid-ok"]);
    expect(result.purgedNames).toEqual(["Ok"]);
    expect(result.skippedFolderNames).toEqual(["SkipMe"]);
    expect(result.failures.map((failure) => failure.name)).toEqual(["Missing"]);
    expect(result.fullSuccess).toBe(false);
    expect(multiPurgeOutcome(result)).toBe("partial");
    const text = formatMultiSelectPurgeStatus(result);
    expect(text).toContain("SkipMe");
    expect(text).toContain("Missing");
    expect(text).not.toBe(
      message(EXPLORER_MSG.MULTI_PURGE_SUCCESS).replace("{count}", "1"),
    );
  });

  it("does not report success when every checked row is a folder", async () => {
    const result = await purgeCheckedItems([folder("Only")], async () => {
      throw new Error("must not purge a folder");
    });
    expect(result.fullSuccess).toBe(false);
    expect(multiPurgeOutcome(result)).toBe("none");
    expect(formatMultiSelectPurgeStatus(result)).toContain("Only");
    expect(formatMultiSelectPurgeStatus(result)).toContain(
      message(EXPLORER_MSG.MULTI_PURGE_NONE),
    );
  });

  it("keeps a 403 from looking like the whole selection was purged", async () => {
    const result = await purgeCheckedItems(
      [page("Locked", "guid-lock"), page("Free", "guid-free")],
      async (guid) => {
        if (guid === "guid-lock") {
          throw { status: 403, statusText: "Forbidden", body: {} };
        }
      },
    );
    expect(result.fullSuccess).toBe(false);
    expect(result.purgedNames).toEqual(["Free"]);
    expect(formatMultiSelectPurgeStatus(result)).toContain("Locked");
    expect(formatMultiSelectPurgeStatus(result)).not.toContain("Purged 2");
  });
});
