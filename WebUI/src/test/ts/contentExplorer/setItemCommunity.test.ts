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

import { describe, expect, it, vi } from "vitest";
import type { ApiError } from "../../../main/ts/api/client";
import { unwrapItemCommunityChoices } from "../../../main/ts/api/contentExplorer/itemCommunityApi";
import type { PSPathItem } from "../../../main/ts/api/contentExplorer/types";
import {
  classifySetCommunitySelection,
  gateExplorerCommunityChange,
  loadSetCommunityCatalog,
  saveSetCommunity,
} from "../../../main/ts/contentExplorer/setItemCommunity";

function httpError(status: number): ApiError {
  return { status, statusText: String(status), body: null };
}

const page: PSPathItem = {
  id: "16777215-101-551",
  name: "Home",
  path: "/Sites/CI/Home",
  type: "percPage",
  category: "page",
  leaf: true,
};

const folder: PSPathItem = {
  id: "1",
  name: "CI",
  path: "/Sites/CI/",
  type: "folder",
  category: "folder",
  leaf: false,
};

describe("set item community (#5077)", () => {
  it("blocks empty, folder, and multi-select before any save", () => {
    expect(classifySetCommunitySelection({ item: null, selectedCount: 0 }).status).toBe(
      "blocked",
    );
    expect(classifySetCommunitySelection({ item: folder, selectedCount: 1 })).toMatchObject({
      status: "blocked",
      reason: "folder",
      name: "CI",
    });
    expect(classifySetCommunitySelection({ item: page, selectedCount: 2 })).toMatchObject({
      reason: "multi",
    });
  });

  it("refuses the current community and an id that is not listed", () => {
    expect(
      gateExplorerCommunityChange({
        selectedId: "10",
        currentId: "10",
        allowedIds: ["10", "20"],
      }),
    ).toEqual({ ok: false, reason: "unchanged" });
    expect(
      gateExplorerCommunityChange({
        selectedId: "99",
        currentId: "10",
        allowedIds: ["10", "20"],
      }),
    ).toEqual({ ok: false, reason: "forbidden" });
    expect(
      gateExplorerCommunityChange({
        selectedId: "  ",
        currentId: "10",
        allowedIds: ["10", "20"],
      }),
    ).toEqual({ ok: false, reason: "blank" });
    expect(
      gateExplorerCommunityChange({
        selectedId: "20",
        currentId: "10",
        allowedIds: ["10", "20"],
      }),
    ).toEqual({ ok: true, communityId: "20" });
  });

  it("unwraps a root-wrapped community catalog", () => {
    expect(
      unwrapItemCommunityChoices({
        ItemCommunityChoices: {
          itemId: "551",
          currentCommunityId: "10",
          choices: {
            ItemCommunityChoice: [
              { id: "10", name: "Default" },
              { id: "20", name: "Enterprise" },
            ],
          },
        },
      }),
    ).toEqual({
      itemId: "551",
      currentCommunityId: "10",
      choices: [
        { id: "10", name: "Default" },
        { id: "20", name: "Enterprise" },
      ],
    });
  });

  it("does not call change when the catalog is HTTP 400", async () => {
    const change = vi.fn();
    const loaded = await loadSetCommunityCatalog({
      item: page,
      selectedCount: 1,
      loadChoices: vi.fn().mockRejectedValue(httpError(400)),
    });
    expect(loaded).toEqual({ status: "http", http: 400 });
    expect(change).not.toHaveBeenCalled();
  });

  it("saves only a different allowed community", async () => {
    const change = vi.fn().mockResolvedValue({ choices: [] });
    const saved = await saveSetCommunity({
      itemId: page.id as string,
      selectedId: "20",
      currentId: "10",
      allowedIds: ["10", "20"],
      change,
    });
    expect(saved).toEqual({ status: "saved", communityId: "20" });
    expect(change).toHaveBeenCalledWith(page.id, "20");
  });

  it("maps HTTP 403 and 409 to a failure and does not report saved", async () => {
    const forbidden = await saveSetCommunity({
      itemId: "551",
      selectedId: "20",
      currentId: "10",
      allowedIds: ["10", "20"],
      change: vi.fn().mockRejectedValue(httpError(403)),
    });
    expect(forbidden).toEqual({ status: "http", http: 403 });
    const conflict = await saveSetCommunity({
      itemId: "551",
      selectedId: "20",
      currentId: "10",
      allowedIds: ["10", "20"],
      change: vi.fn().mockRejectedValue(httpError(409)),
    });
    expect(conflict).toEqual({ status: "http", http: 409 });
  });
});
