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
  describeSetCommunityMultiSave,
  gateExplorerCommunityChange,
  loadSetCommunityCatalog,
  loadSetCommunityMultiCatalog,
  planSetCommunityMulti,
  saveSetCommunity,
  saveSetCommunityOnSelection,
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

const about: PSPathItem = {
  id: "16777215-101-552",
  name: "About",
  path: "/Sites/CI/About",
  type: "percPage",
  category: "page",
  leaf: true,
};

const choices = [
  { id: "10", name: "Default" },
  { id: "20", name: "Enterprise" },
];

function catalogFor(currentId: string) {
  return async () => ({
    currentCommunityId: currentId,
    choices,
  });
}

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

describe("set community on multi-selected items (#5133)", () => {
  it("plans pages and assets and does not write folders", () => {
    const plan = planSetCommunityMulti([page, folder, about, page]);
    expect(plan).toEqual({
      status: "ready",
      targets: [
        { itemId: page.id, name: "Home" },
        { itemId: about.id, name: "About" },
      ],
      skippedFolderNames: ["CI"],
    });
    expect(planSetCommunityMulti([folder, folder])).toMatchObject({
      status: "blocked",
      reason: "folder",
      name: "CI",
    });
    expect(planSetCommunityMulti([])).toMatchObject({
      status: "blocked",
      reason: "empty",
    });
  });

  it("does not open a save when the shared catalog is HTTP 403", async () => {
    const change = vi.fn();
    const loaded = await loadSetCommunityMultiCatalog({
      items: [page, about, folder],
      loadChoices: vi.fn().mockRejectedValue(httpError(403)),
    });
    expect(loaded).toEqual({ status: "http", http: 403 });
    expect(change).not.toHaveBeenCalled();
  });

  it("shows each community only after that save and does not claim success on HTTP 409", async () => {
    const order: string[] = [];
    let releaseAbout: (value?: unknown) => void = () => undefined;
    const aboutGate = new Promise((resolve) => {
      releaseAbout = resolve;
    });
    const change = vi.fn(async (itemId: string) => {
      order.push(`post:${itemId}`);
      if (itemId === about.id) {
        await aboutGate;
        throw httpError(409);
      }
      return { choices: [] };
    });
    const pending = saveSetCommunityOnSelection({
      targets: [
        { itemId: String(page.id), name: "Home" },
        { itemId: String(about.id), name: "About" },
      ],
      skippedFolderNames: ["CI"],
      selectedId: "20",
      allowedIds: ["10", "20"],
      loadChoices: async (itemId) => {
        order.push(`get:${itemId}`);
        return {
          currentCommunityId: "10",
          choices,
        };
      },
      change,
      onItemSaved: (item) => {
        order.push(`shown:${item.itemId}`);
      },
    });
    await vi.waitFor(() => expect(order).toContain(`post:${about.id}`));
    expect(order).toEqual([
      `get:${page.id}`,
      `post:${page.id}`,
      `shown:${page.id}`,
      `get:${about.id}`,
      `post:${about.id}`,
    ]);
    releaseAbout();
    const result = await pending;
    expect(result.status).toBe("partial");
    expect(result.communityId).toBe("");
    expect(result.saved.map((row) => row.itemId)).toEqual([page.id]);
    expect(result.failures).toEqual([
      { itemId: about.id, name: "About", http: 409 },
    ]);
    expect(order.filter((step) => step.startsWith("shown:"))).toEqual([
      `shown:${page.id}`,
    ]);
    const described = describeSetCommunityMultiSave(result, "Enterprise");
    expect(described.kind).toBe("error");
    expect(described.reason).toBe("partial");
    expect(described.communityId).toBe("");
    expect(described.communityName).toBe("");
    expect(described.text).toContain("Not every selected item had its community set");
    expect(described.text).toContain("About (HTTP 409)");
    expect(described.text).toContain("Folders are not assigned a community: CI");
    expect(described.text).not.toContain("Community saved");
  });

  it("claims success only after every page and asset is saved", async () => {
    const change = vi.fn().mockResolvedValue({ choices: [] });
    const shown: string[] = [];
    const result = await saveSetCommunityOnSelection({
      targets: [
        { itemId: "42", name: "Home" },
        { itemId: "43", name: "About" },
      ],
      skippedFolderNames: ["News"],
      selectedId: "20",
      allowedIds: ["10", "20"],
      loadChoices: catalogFor("10"),
      change,
      onItemSaved: (item) => shown.push(item.itemId),
    });
    expect(change).toHaveBeenCalledTimes(2);
    expect(change).toHaveBeenNthCalledWith(1, "42", "20");
    expect(change).toHaveBeenNthCalledWith(2, "43", "20");
    expect(shown).toEqual(["42", "43"]);
    expect(result.status).toBe("saved");
    const described = describeSetCommunityMultiSave(result, "Enterprise");
    expect(described).toMatchObject({
      kind: "success",
      reason: "folders-skipped",
      communityId: "20",
      communityName: "Enterprise",
    });
    expect(described.text).toContain("Community saved Enterprise");
    expect(described.text).toContain("News");
  });

  it.each([400, 403, 409])(
    "HTTP %s on one item is not a full-selection success",
    async (status) => {
      const shown = vi.fn();
      const result = await saveSetCommunityOnSelection({
        targets: [
          { itemId: "42", name: "Home" },
          { itemId: "43", name: "About" },
        ],
        selectedId: "20",
        allowedIds: ["10", "20"],
        loadChoices: catalogFor("10"),
        change: vi.fn(async (itemId: string) => {
          if (itemId === "43") {
            throw httpError(status);
          }
          return { choices: [] };
        }),
        onItemSaved: shown,
      });
      expect(result.status).toBe("partial");
      expect(result.saved.map((row) => row.itemId)).toEqual(["42"]);
      expect(result.failures).toEqual([{ itemId: "43", name: "About", http: status }]);
      expect(shown).toHaveBeenCalledTimes(1);
      expect(describeSetCommunityMultiSave(result, "Enterprise").text).not.toContain(
        "Community saved",
      );
    },
  );

  it("does not post when cancel would leave the choice unchanged", async () => {
    const change = vi.fn();
    const shown = vi.fn();
    const result = await saveSetCommunityOnSelection({
      targets: [{ itemId: "42", name: "Home" }],
      selectedId: "10",
      allowedIds: ["10", "20"],
      loadChoices: catalogFor("10"),
      change,
      onItemSaved: shown,
    });
    expect(change).not.toHaveBeenCalled();
    expect(shown).not.toHaveBeenCalled();
    expect(result.status).toBe("unchanged");
    const described = describeSetCommunityMultiSave(result, "Default");
    expect(described.kind).toBe("error");
    expect(described.communityId).toBe("");
    expect(described.text).not.toContain("Community saved");
  });
});
