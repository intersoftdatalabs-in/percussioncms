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
  formatMultiSelectRestoreStatus,
  multiRestoreOutcome,
  partitionMultiSelectRestore,
  restoreCheckedItems,
} from "../../../main/ts/contentExplorer/multiSelectRestore";
import { message } from "../../../main/ts/i18n/message";

const page = (name: string, id: string): PSPathItem => ({
  id,
  name,
  path: `/Recycling/${name}`,
  type: "percPage",
  category: "page",
  leaf: true,
});

const folder = (name: string, id: string): PSPathItem => ({
  id,
  name,
  path: `/Recycling/${name}/`,
  type: "folder",
  category: "folder",
  leaf: false,
});

describe("multi-select restore (#4884)", () => {
  it("restores pages, assets, and folders that have a guid", () => {
    const parts = partitionMultiSelectRestore([
      folder("News", "guid-news"),
      page("Home", "guid-home"),
      page("Logo", ""),
    ]);
    expect(parts.restorable.map((item) => item.id)).toEqual([
      "guid-news",
      "guid-home",
    ]);
    expect(parts.missingId.map((item) => item.name)).toEqual(["Logo"]);
  });

  it("restores every checked item with one guid each", async () => {
    const calls: string[] = [];
    const items = [
      page("Home", "guid-home"),
      folder("News", "guid-news"),
    ];
    const result = await restoreCheckedItems(items, async (guid) => {
      calls.push(guid);
    });
    expect(calls).toEqual(["guid-home", "guid-news"]);
    expect(result.fullSuccess).toBe(true);
    expect(multiRestoreOutcome(result)).toBe("success");
    expect(formatMultiSelectRestoreStatus(result)).toContain("2");
  });

  it("does not call restore when the caller never confirms", async () => {
    const calls: string[] = [];
    const confirmed = false;
    if (confirmed) {
      await restoreCheckedItems([page("Home", "guid-home")], async (guid) => {
        calls.push(guid);
      });
    }
    expect(calls).toEqual([]);
  });

  it("names a later HTTP 409 without full success and still restores the rest", async () => {
    const restored: string[] = [];
    const result = await restoreCheckedItems(
      [
        folder("News", "guid-news"),
        page("Ok", "guid-ok"),
        page("Clash", "guid-clash"),
      ],
      async (guid) => {
        if (guid === "guid-clash") {
          throw { status: 409, statusText: "Conflict", body: {} };
        }
        restored.push(guid);
      },
    );
    expect(restored).toEqual(["guid-news", "guid-ok"]);
    expect(result.restoredNames).toEqual(["News", "Ok"]);
    expect(result.failures.map((failure) => failure.name)).toEqual(["Clash"]);
    expect(result.fullSuccess).toBe(false);
    expect(multiRestoreOutcome(result)).toBe("partial");
    const text = formatMultiSelectRestoreStatus(result);
    expect(text).toContain("Clash");
    expect(text).toContain(message(EXPLORER_MSG.ACTION_RESTORE_CONFLICT));
    expect(text).not.toBe(
      message(EXPLORER_MSG.MULTI_RESTORE_SUCCESS).replace("{count}", "2"),
    );
  });

  it("names a missing guid as not found and does not call restore", async () => {
    const calls: string[] = [];
    const result = await restoreCheckedItems(
      [page("Nameless", "")],
      async (guid) => {
        calls.push(guid);
      },
    );
    expect(calls).toEqual([]);
    expect(result.fullSuccess).toBe(false);
    expect(multiRestoreOutcome(result)).toBe("none");
    expect(formatMultiSelectRestoreStatus(result)).toContain("Nameless");
    expect(formatMultiSelectRestoreStatus(result)).toContain(
      message(EXPLORER_MSG.MULTI_RESTORE_NONE),
    );
  });

  it("keeps a 403 or 404 from looking like the whole selection was restored", async () => {
    const result = await restoreCheckedItems(
      [page("Denied", "guid-deny"), page("Gone", "guid-gone"), page("Free", "guid-free")],
      async (guid) => {
        if (guid === "guid-deny") {
          throw { status: 403, statusText: "Forbidden", body: {} };
        }
        if (guid === "guid-gone") {
          throw { status: 404, statusText: "Not Found", body: {} };
        }
      },
    );
    expect(result.fullSuccess).toBe(false);
    expect(result.restoredNames).toEqual(["Free"]);
    const text = formatMultiSelectRestoreStatus(result);
    expect(text).toContain("Denied");
    expect(text).toContain("Gone");
    expect(text).toContain(message(EXPLORER_MSG.PERMISSION_DENIED));
    expect(text).toContain(message(EXPLORER_MSG.ACTION_RESTORE_NOT_FOUND));
    expect(text).not.toContain("Restored 3");
  });
});
