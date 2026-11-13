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
  MAX_RECENT_ITEMS,
  dropRecentItem,
  loadRecentItems,
  persistRecentItems,
  recentItemKey,
  recentStorageKey,
  recordRecentItem,
  toRecentItem,
  type RecentEntry,
  type RecentStorage,
} from "../../../main/ts/contentExplorer/recentItems";

function memoryStorage(seed: Record<string, string> = {}): RecentStorage {
  const map = new Map(Object.entries(seed));
  return {
    getItem: (key) => (map.has(key) ? map.get(key)! : null),
    setItem: (key, value) => {
      map.set(key, value);
    },
    removeItem: (key) => {
      map.delete(key);
    },
  };
}

function entry(overrides: Partial<RecentEntry> = {}): RecentEntry {
  return {
    id: "16777215-101-551",
    name: "Home",
    path: "/Sites/Demo/Home",
    kind: "item",
    openedAt: new Date("2026-01-01T00:00:00.000Z").toISOString(),
    ...overrides,
  };
}

describe("recentItems store (#6256)", () => {
  it("keys recents per operator", () => {
    expect(recentStorageKey("Admin")).toBe("perc.explorer.recent.Admin");
    expect(recentStorageKey(null)).toBe("perc.explorer.recent.anonymous");
    expect(recentStorageKey("  ")).toBe("perc.explorer.recent.anonymous");
    expect(recentStorageKey("op erator/x")).toBe(
      `perc.explorer.recent.${encodeURIComponent("op erator/x")}`,
    );
  });

  it("records opens most-recent-first with dedupe", () => {
    const first = recordRecentItem([], {
      id: "1",
      name: "Home",
      path: "/Sites/Demo/Home",
      kind: "item",
    });
    expect(first).toHaveLength(1);
    const second = recordRecentItem(first, {
      id: null,
      name: "Demo",
      path: "/Sites/Demo/",
      kind: "folder",
    });
    expect(second.map((e) => e.path)).toEqual([
      "/Sites/Demo/",
      "/Sites/Demo/Home",
    ]);
    // Re-open moves to front without duplicating.
    const third = recordRecentItem(second, {
      id: "1",
      name: "Home",
      path: "/Sites/Demo/Home",
      kind: "item",
    });
    expect(third.map((e) => recentItemKey(e))).toEqual(["id:1", "path:/Sites/Demo/"]);
    expect(third[0].name).toBe("Home");
  });

  it("includes folders and caps the list", () => {
    let list: RecentEntry[] = [];
    for (let i = 0; i < MAX_RECENT_ITEMS + 5; i += 1) {
      list = recordRecentItem(list, {
        id: null,
        name: `Folder ${i}`,
        path: `/Sites/F${i}/`,
        kind: "folder",
      });
    }
    expect(list).toHaveLength(MAX_RECENT_ITEMS);
    expect(list[0].path).toBe(`/Sites/F${MAX_RECENT_ITEMS + 4}/`);
  });

  it("ignores blank rows with neither id nor path", () => {
    const list = recordRecentItem([], { id: null, name: "", path: "  ", kind: "item" });
    expect(list).toEqual([]);
  });

  it("loads stored recents and drops stale entries", () => {
    const storage = memoryStorage({
      [recentStorageKey("Admin")]: JSON.stringify([
        entry(),
        { id: "  ", path: " ", name: "stale", kind: "item" },
        "garbage",
        null,
        entry({ id: "16777215-101-551", path: "/Sites/Demo/Home" }),
        { kind: "folder", path: "/Sites/Demo/", name: "" },
      ]),
    });
    const loaded = loadRecentItems(storage, "Admin");
    expect(loaded.map((e) => recentItemKey(e))).toEqual([
      "id:16777215-101-551",
      "path:/Sites/Demo/",
    ]);
    // Blank names fall back to the path.
    expect(loaded[1].name).toBe("/Sites/Demo/");
  });

  it("returns empty on corrupt payloads and isolates operators", () => {
    const storage = memoryStorage({
      [recentStorageKey("Admin")]: "not-json{{{",
      [recentStorageKey("Designer")]: JSON.stringify([entry()]),
    });
    expect(loadRecentItems(storage, "Admin")).toEqual([]);
    expect(loadRecentItems(storage, "Designer")).toHaveLength(1);
    expect(loadRecentItems(null, "Admin")).toEqual([]);
  });

  it("persists capped entries and drops one row by key", () => {
    const storage = memoryStorage();
    const list = [entry(), entry({ id: "2", path: "/Sites/Demo/Two" })];
    persistRecentItems(storage, "Admin", list);
    expect(loadRecentItems(storage, "Admin")).toHaveLength(2);
    expect(loadRecentItems(storage, "Designer")).toEqual([]);
    const dropped = dropRecentItem(list, "id:16777215-101-551");
    expect(dropped.map((e) => e.id)).toEqual(["2"]);
    expect(dropRecentItem(list, "id:missing")).toHaveLength(2);
  });

  it("converts shell rows and rejects keyless rows", () => {
    expect(
      toRecentItem({
        id: "16777215-101-551",
        name: "Home",
        path: "/Sites/Demo/Home",
        type: "percPage",
      }),
    ).toMatchObject({ kind: "item", name: "Home" });
    expect(
      toRecentItem({
        id: "16777215-101-703",
        name: "Demo",
        path: "/Sites/Demo/",
        type: "folder",
      }),
    ).toMatchObject({ kind: "folder" });
    expect(toRecentItem(null)).toBeNull();
    expect(
      toRecentItem({ id: undefined, name: "", path: "  " }),
    ).toBeNull();
  });
});
