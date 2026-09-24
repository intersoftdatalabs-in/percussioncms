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

import { afterEach, describe, expect, it, vi } from "vitest";
import type { PSPathItem } from "../../../main/ts/api/contentExplorer/types";
import {
  formatTakedownConfirmBody,
  isPublishingHistoryActionName,
  isRemoveFromStagingActionName,
  isStageActionName,
  isTakedownActionName,
  linkedPagePathsForConfirm,
  loadLinkedPagesForTakedown,
  parseLinkedPagesForTakedown,
  describePublishBatch,
  publishSelectedItem,
  publishSelectedItems,
  removeFromStagingSelectedItem,
  removeFromStagingSelectedItems,
  resolvePublishKind,
  stageSelectedItem,
  stageSelectedItems,
  takedownSelectedItem,
  takedownSelectedItems,
} from "../../../main/ts/contentExplorer/itemPublish";

afterEach(() => {
  vi.restoreAllMocks();
});

function item(overrides: Partial<PSPathItem> = {}): PSPathItem {
  return {
    name: "page",
    path: "/Sites/Demo/page",
    type: "percPage",
    id: "42",
    ...overrides,
  };
}

describe("publishSelectedItem", () => {
  it("GETs sitemanage publish/page for a page", async () => {
    vi.spyOn(global, "fetch").mockResolvedValueOnce(
      new Response("{}", {
        status: 200,
        headers: { "Content-Type": "application/json" },
      }),
    );
    expect(await publishSelectedItem(item())).toBe(true);
    const url = String(global.fetch.mock.calls[0]?.[0] ?? "");
    expect(url).toContain("sitemanage/publish/page/42");
    expect(url).not.toContain("demandpublishing");
  });

  it("GETs sitemanage publish/resource for an asset", async () => {
    vi.spyOn(global, "fetch").mockResolvedValueOnce(
      new Response("{}", {
        status: 200,
        headers: { "Content-Type": "application/json" },
      }),
    );
    expect(
      await publishSelectedItem(
        item({ path: "/Assets/img.png", type: "percImageAsset", id: "99" }),
      ),
    ).toBe(true);
    expect(String(global.fetch.mock.calls[0]?.[0] ?? "")).toContain(
      "sitemanage/publish/resource/99",
    );
  });

  it("returns false without an id", async () => {
    const fetchSpy = vi.spyOn(global, "fetch");
    expect(await publishSelectedItem(item({ id: "" }))).toBe(false);
    expect(fetchSpy).not.toHaveBeenCalled();
  });

  it("returns false for non-page/non-asset types that preview would call page", async () => {
    const fetchSpy = vi.spyOn(global, "fetch");
    const template = item({
      name: "base",
      path: "/Design/Templates/base",
      type: "percTemplate",
      category: "template",
      id: "77",
    });
    const link = item({
      name: "ext",
      path: "/Other/ext",
      type: "percExternalLink",
      id: "88",
    });
    expect(resolvePublishKind(template)).toBe("none");
    expect(resolvePublishKind(link)).toBe("none");
    expect(await publishSelectedItem(template)).toBe(false);
    expect(await publishSelectedItem(link)).toBe(false);
    expect(fetchSpy).not.toHaveBeenCalled();
  });

  it("throws when the publish GET returns 403 or 500", async () => {
    vi.spyOn(global, "fetch")
      .mockResolvedValueOnce(
        new Response("denied", { status: 403, statusText: "Forbidden" }),
      )
      .mockResolvedValueOnce(
        new Response("boom", { status: 500, statusText: "Server Error" }),
      );
    await expect(publishSelectedItem(item())).rejects.toMatchObject({
      status: 403,
    });
    await expect(publishSelectedItem(item())).rejects.toMatchObject({
      status: 500,
    });
  });

  it("throws when the publish GET fails on the network", async () => {
    vi.spyOn(global, "fetch").mockRejectedValueOnce(
      new TypeError("Failed to fetch"),
    );
    await expect(publishSelectedItem(item())).rejects.toThrow("Failed to fetch");
  });

  it("throws on HTTP 200 unwrapped FORBIDDEN instead of returning true", async () => {
    vi.spyOn(global, "fetch").mockResolvedValueOnce(
      new Response(JSON.stringify({ status: "FORBIDDEN" }), {
        status: 200,
        headers: { "Content-Type": "application/json" },
      }),
    );
    await expect(publishSelectedItem(item())).rejects.toThrow("FORBIDDEN");
  });

  it("throws on wrapped SitePublishResponse BADCONFIG warning", async () => {
    vi.spyOn(global, "fetch").mockResolvedValueOnce(
      new Response(
        JSON.stringify({
          SitePublishResponse: {
            status: "BADCONFIG",
            warningMessage:
              "Could not connect to publishing server, please check publishing server configuration.",
          },
        }),
        {
          status: 200,
          headers: { "Content-Type": "application/json" },
        },
      ),
    );
    await expect(publishSelectedItem(item())).rejects.toThrow(
      "Could not connect to publishing server",
    );
  });

  it("publishes each eligible item and records a 403 without stopping the batch", async () => {
    vi.spyOn(global, "fetch").mockImplementation(async (input) => {
      const url = String(input);
      if (url.includes("/publish/page/43")) {
        return new Response("denied", { status: 403, statusText: "Forbidden" });
      }
      return new Response("{}", {
        status: 200,
        headers: { "Content-Type": "application/json" },
      });
    });
    const result = await publishSelectedItems([
      item({ id: "42", name: "Home" }),
      item({
        id: "7",
        name: "News",
        path: "/Sites/Demo/News",
        type: "folder",
        leaf: false,
      }),
      item({ id: "43", name: "About" }),
      item({
        id: "99",
        name: "logo",
        path: "/Assets/logo.png",
        type: "percImageAsset",
      }),
    ]);
    expect(result.publishedIds).toEqual(["42", "99"]);
    expect(result.skippedFolders).toEqual(["News"]);
    expect(result.failures).toEqual([
      expect.objectContaining({ id: "43", name: "About", status: 403 }),
    ]);
    const urls = global.fetch.mock.calls.map((call) => String(call[0]));
    expect(urls.some((url) => url.includes("/publish/page/42"))).toBe(true);
    expect(urls.some((url) => url.includes("/publish/page/43"))).toBe(true);
    expect(urls.some((url) => url.includes("/publish/resource/99"))).toBe(true);
    expect(urls.some((url) => url.includes("/7"))).toBe(false);
    const text = describePublishBatch(result) ?? "";
    expect(text).toMatch(/Folders are not published: News/);
    expect(text).toMatch(/About \(HTTP 403\)/);
    expect(text).toMatch(/Not every selected item was published/);
  });
});

describe("takedownSelectedItem", () => {
  it("GETs sitemanage takedown/page for a page with no linked items", async () => {
    vi.spyOn(global, "fetch").mockResolvedValueOnce(
      new Response("{}", {
        status: 200,
        headers: { "Content-Type": "application/json" },
      }),
    );
    expect(await takedownSelectedItem(item())).toBe(true);
    const req = global.fetch.mock.calls[0];
    expect(String(req?.[0] ?? "")).toContain(
      "sitemanage/publish/takedown/page/42",
    );
    expect(String(req?.[0] ?? "")).not.toMatch(/\/delete/i);
    expect(req?.[1]?.method ?? "GET").toBe("GET");
  });

  it("GETs sitemanage takedown/resource for an asset", async () => {
    vi.spyOn(global, "fetch").mockResolvedValueOnce(
      new Response("{}", {
        status: 200,
        headers: { "Content-Type": "application/json" },
      }),
    );
    expect(
      await takedownSelectedItem(
        item({ path: "/Assets/img.png", type: "percImageAsset", id: "99" }),
      ),
    ).toBe(true);
    expect(String(global.fetch.mock.calls[0]?.[0] ?? "")).toContain(
      "sitemanage/publish/takedown/resource/99",
    );
  });

  it("PUTs linked pages when the confirm list is non-empty", async () => {
    vi.spyOn(global, "fetch").mockResolvedValueOnce(
      new Response("{}", {
        status: 200,
        headers: { "Content-Type": "application/json" },
      }),
    );
    const linked = [
      { id: "7", pagePath: "/Sites/Demo/Home", relationshipId: "rel-1" },
    ];
    expect(await takedownSelectedItem(item(), linked)).toBe(true);
    const req = global.fetch.mock.calls[0];
    expect(req?.[1]?.method).toBe("PUT");
    expect(JSON.parse(String(req?.[1]?.body ?? "[]"))).toEqual(linked);
  });

  it("returns false for folders and non-page types", async () => {
    const fetchSpy = vi.spyOn(global, "fetch");
    expect(
      await takedownSelectedItem(
        item({
          id: "1",
          name: "Sites",
          path: "/Sites",
          type: "folder",
          leaf: false,
        }),
      ),
    ).toBe(false);
    expect(
      await takedownSelectedItem(
        item({
          path: "/Design/Templates/base",
          type: "percTemplate",
          category: "template",
          id: "77",
        }),
      ),
    ).toBe(false);
    expect(fetchSpy).not.toHaveBeenCalled();
  });

  it("throws on HTTP 200 unwrapped FORBIDDEN instead of returning true", async () => {
    vi.spyOn(global, "fetch").mockResolvedValueOnce(
      new Response(JSON.stringify({ status: "FORBIDDEN" }), {
        status: 200,
        headers: { "Content-Type": "application/json" },
      }),
    );
    await expect(takedownSelectedItem(item())).rejects.toThrow("FORBIDDEN");
  });

  it("throws on wrapped SitePublishResponse BADCONFIG warning", async () => {
    vi.spyOn(global, "fetch").mockResolvedValueOnce(
      new Response(
        JSON.stringify({
          SitePublishResponse: {
            status: "BADCONFIG",
            warningMessage:
              "Could not connect to publishing server, please check publishing server configuration.",
          },
        }),
        {
          status: 200,
          headers: { "Content-Type": "application/json" },
        },
      ),
    );
    await expect(takedownSelectedItem(item())).rejects.toThrow(
      "Could not connect to publishing server",
    );
  });

  it("throws when the takedown GET returns 403", async () => {
    vi.spyOn(global, "fetch").mockResolvedValueOnce(
      new Response("denied", { status: 403, statusText: "Forbidden" }),
    );
    await expect(takedownSelectedItem(item())).rejects.toMatchObject({
      status: 403,
    });
  });

  it("takes down each eligible item, skips folders, and records a 403", async () => {
    vi.spyOn(global, "fetch").mockImplementation(async (input) => {
      const url = String(input);
      if (url.includes("findLinkedItems")) {
        return new Response(JSON.stringify({ ArrayList: [] }), {
          status: 200,
          headers: { "Content-Type": "application/json" },
        });
      }
      if (url.includes("/takedown/page/43")) {
        return new Response("denied", { status: 403, statusText: "Forbidden" });
      }
      return new Response("{}", {
        status: 200,
        headers: { "Content-Type": "application/json" },
      });
    });
    const result = await takedownSelectedItems([
      item({ id: "42", name: "Home" }),
      item({
        id: "7",
        name: "News",
        path: "/Sites/Demo/News",
        type: "folder",
        leaf: false,
      }),
      item({ id: "43", name: "About" }),
      item({
        id: "77",
        name: "base",
        path: "/Design/Templates/base",
        type: "percTemplate",
        category: "template",
      }),
    ]);
    expect(result.takenDownIds).toEqual(["42"]);
    expect(result.skippedFolders).toEqual(["News"]);
    expect(result.skippedOther).toEqual(["base"]);
    expect(result.failures).toEqual([
      expect.objectContaining({ id: "43", name: "About", status: 403 }),
    ]);
    const urls = global.fetch.mock.calls.map((call) => String(call[0]));
    expect(urls.some((url) => url.includes("takedown/page/42"))).toBe(true);
    expect(urls.some((url) => url.includes("takedown/page/43"))).toBe(true);
    expect(urls.some((url) => url.includes("takedown/page/7"))).toBe(false);
    expect(urls.some((url) => url.includes("/staging/"))).toBe(false);
  });
});

describe("linked pages for takedown", () => {
  it("parses ArrayList envelopes and formats confirm copy", () => {
    const linked = parseLinkedPagesForTakedown({
      ArrayList: [
        { pagePath: "/Sites/Demo/Home", id: "1" },
        { PageLinkedToItem: { pagePath: "/Sites/Demo/About" } },
      ],
    });
    expect(linked.map((row) => row.pagePath)).toEqual([
      "/Sites/Demo/Home",
      "/Sites/Demo/About",
    ]);
    expect(formatTakedownConfirmBody(linked)).toMatch(/Take down/i);
    expect(formatTakedownConfirmBody(linked)).toContain("/Sites/Demo/Home");
    expect(formatTakedownConfirmBody([])).toMatch(/Take down/i);
    expect(formatTakedownConfirmBody([])).not.toContain("/Sites");
    expect(linkedPagePathsForConfirm(linked)).toEqual([
      "/Sites/Demo/Home",
      "/Sites/Demo/About",
    ]);
    const eleven = Array.from({ length: 11 }, (_, i) => ({
      pagePath: `/Sites/Demo/p${i}`,
    }));
    expect(linkedPagePathsForConfirm(eleven)).toHaveLength(10);
  });

  it("returns [] when findLinkedItems fails", async () => {
    vi.spyOn(global, "fetch").mockRejectedValueOnce(
      new TypeError("Failed to fetch"),
    );
    expect(await loadLinkedPagesForTakedown("42")).toEqual([]);
  });

  it("recognizes Take Down action name variants", () => {
    expect(isTakedownActionName("Take_Down")).toBe(true);
    expect(isTakedownActionName("Take Down")).toBe(true);
    expect(isTakedownActionName("unpublish")).toBe(true);
    expect(isTakedownActionName("Publish_Now")).toBe(false);
    expect(isTakedownActionName("Stage")).toBe(false);
  });

  it("recognizes Publishing History action name variants", () => {
    expect(isPublishingHistoryActionName("Publishing_History")).toBe(true);
    expect(isPublishingHistoryActionName("Publishing History")).toBe(true);
    expect(isPublishingHistoryActionName("pubhistory")).toBe(true);
    expect(isPublishingHistoryActionName("Publish_Now")).toBe(false);
  });
});

describe("stageSelectedItem", () => {
  it("GETs sitemanage publish/page/staging for a page", async () => {
    vi.spyOn(global, "fetch").mockResolvedValueOnce(
      new Response("{}", {
        status: 200,
        headers: { "Content-Type": "application/json" },
      }),
    );
    expect(await stageSelectedItem(item())).toBe(true);
    expect(String(global.fetch.mock.calls[0]?.[0] ?? "")).toContain(
      "sitemanage/publish/page/staging/42",
    );
  });

  it("GETs sitemanage publish/resource/staging for an asset", async () => {
    vi.spyOn(global, "fetch").mockResolvedValueOnce(
      new Response("{}", {
        status: 200,
        headers: { "Content-Type": "application/json" },
      }),
    );
    expect(
      await stageSelectedItem(
        item({ path: "/Assets/img.png", type: "percImageAsset", id: "99" }),
      ),
    ).toBe(true);
    expect(String(global.fetch.mock.calls[0]?.[0] ?? "")).toContain(
      "sitemanage/publish/resource/staging/99",
    );
  });

  it("returns false for folders and non-page types", async () => {
    const fetchSpy = vi.spyOn(global, "fetch");
    expect(
      await stageSelectedItem(
        item({
          id: "1",
          name: "Sites",
          path: "/Sites",
          type: "folder",
          leaf: false,
        }),
      ),
    ).toBe(false);
    expect(
      await stageSelectedItem(
        item({
          path: "/Design/Templates/base",
          type: "percTemplate",
          category: "template",
          id: "77",
        }),
      ),
    ).toBe(false);
    expect(fetchSpy).not.toHaveBeenCalled();
  });

  it("throws on HTTP 200 unwrapped FORBIDDEN instead of returning true", async () => {
    vi.spyOn(global, "fetch").mockResolvedValueOnce(
      new Response(JSON.stringify({ status: "FORBIDDEN" }), {
        status: 200,
        headers: { "Content-Type": "application/json" },
      }),
    );
    await expect(stageSelectedItem(item())).rejects.toThrow("FORBIDDEN");
  });

  it("throws on wrapped NOSTAGING_SERVERS instead of returning true", async () => {
    vi.spyOn(global, "fetch").mockResolvedValueOnce(
      new Response(
        JSON.stringify({
          SitePublishResponse: {
            status: "NOSTAGING_SERVERS",
            warningMessage: "No staging servers are configured.",
          },
        }),
        {
          status: 200,
          headers: { "Content-Type": "application/json" },
        },
      ),
    );
    await expect(stageSelectedItem(item())).rejects.toThrow(
      /No staging servers|NOSTAGING_SERVERS/,
    );
  });

  it("throws when the stage GET returns 403", async () => {
    vi.spyOn(global, "fetch").mockResolvedValueOnce(
      new Response("denied", { status: 403, statusText: "Forbidden" }),
    );
    await expect(stageSelectedItem(item())).rejects.toMatchObject({
      status: 403,
    });
  });

  it("stages each eligible item and records a 403 without stopping the batch", async () => {
    vi.spyOn(global, "fetch").mockImplementation(async (input) => {
      const url = String(input);
      if (url.includes("/staging/43")) {
        return new Response("denied", { status: 403, statusText: "Forbidden" });
      }
      return new Response("{}", {
        status: 200,
        headers: { "Content-Type": "application/json" },
      });
    });
    const result = await stageSelectedItems([
      item({ id: "42", name: "Home" }),
      item({
        id: "7",
        name: "News",
        path: "/Sites/Demo/News",
        type: "folder",
        leaf: false,
      }),
      item({ id: "43", name: "About" }),
      item({
        id: "77",
        name: "base",
        path: "/Design/Templates/base",
        type: "percTemplate",
        category: "template",
      }),
    ]);
    expect(result.stagedIds).toEqual(["42"]);
    expect(result.skippedFolders).toEqual(["News"]);
    expect(result.skippedOther).toEqual(["base"]);
    expect(result.failures).toEqual([
      expect.objectContaining({ id: "43", name: "About", status: 403 }),
    ]);
    const urls = global.fetch.mock.calls.map((call) => String(call[0]));
    expect(urls.some((url) => url.includes("/staging/42"))).toBe(true);
    expect(urls.some((url) => url.includes("/staging/43"))).toBe(true);
    expect(urls.some((url) => url.includes("/staging/7"))).toBe(false);
  });

  it("recognizes Stage action name variants", () => {
    expect(isStageActionName("Stage")).toBe(true);
    expect(isStageActionName("stage")).toBe(true);
    expect(isStageActionName("Take_Down")).toBe(false);
    expect(isStageActionName("Publish_Now")).toBe(false);
  });
});

describe("removeFromStagingSelectedItem", () => {
  it("GETs sitemanage takedown/page/staging for a page", async () => {
    vi.spyOn(global, "fetch").mockResolvedValueOnce(
      new Response("{}", {
        status: 200,
        headers: { "Content-Type": "application/json" },
      }),
    );
    expect(await removeFromStagingSelectedItem(item())).toBe(true);
    expect(String(global.fetch.mock.calls[0]?.[0] ?? "")).toContain(
      "sitemanage/publish/takedown/page/staging/42",
    );
  });

  it("GETs sitemanage takedown/resource/staging for an asset", async () => {
    vi.spyOn(global, "fetch").mockResolvedValueOnce(
      new Response("{}", {
        status: 200,
        headers: { "Content-Type": "application/json" },
      }),
    );
    expect(
      await removeFromStagingSelectedItem(
        item({ path: "/Assets/img.png", type: "percImageAsset", id: "99" }),
      ),
    ).toBe(true);
    expect(String(global.fetch.mock.calls[0]?.[0] ?? "")).toContain(
      "sitemanage/publish/takedown/resource/staging/99",
    );
  });

  it("returns false for folders", async () => {
    const fetchSpy = vi.spyOn(global, "fetch");
    expect(
      await removeFromStagingSelectedItem(
        item({
          id: "1",
          name: "Sites",
          path: "/Sites",
          type: "folder",
          leaf: false,
        }),
      ),
    ).toBe(false);
    expect(fetchSpy).not.toHaveBeenCalled();
  });

  it("throws on HTTP 200 FORBIDDEN instead of returning true", async () => {
    vi.spyOn(global, "fetch").mockResolvedValueOnce(
      new Response(JSON.stringify({ status: "FORBIDDEN" }), {
        status: 200,
        headers: { "Content-Type": "application/json" },
      }),
    );
    await expect(removeFromStagingSelectedItem(item())).rejects.toThrow(
      "FORBIDDEN",
    );
  });

  it("throws on wrapped BADCONFIG warning", async () => {
    vi.spyOn(global, "fetch").mockResolvedValueOnce(
      new Response(
        JSON.stringify({
          SitePublishResponse: {
            status: "BADCONFIG",
            warningMessage:
              "Could not connect to publishing server, please check publishing server configuration.",
          },
        }),
        {
          status: 200,
          headers: { "Content-Type": "application/json" },
        },
      ),
    );
    await expect(removeFromStagingSelectedItem(item())).rejects.toThrow(
      "Could not connect to publishing server",
    );
  });

  it("removes each eligible item and records a 409 without stopping the batch", async () => {
    vi.spyOn(global, "fetch").mockImplementation(async (input) => {
      const url = String(input);
      if (url.includes("/staging/43")) {
        return new Response("locked", { status: 409, statusText: "Conflict" });
      }
      return new Response("{}", {
        status: 200,
        headers: { "Content-Type": "application/json" },
      });
    });
    const result = await removeFromStagingSelectedItems([
      item({ id: "42", name: "Home" }),
      item({
        id: "7",
        name: "News",
        path: "/Sites/Demo/News",
        type: "folder",
        leaf: false,
      }),
      item({ id: "43", name: "About" }),
      item({
        id: "77",
        name: "base",
        path: "/Design/Templates/base",
        type: "percTemplate",
        category: "template",
      }),
    ]);
    expect(result.removedIds).toEqual(["42"]);
    expect(result.skippedFolders).toEqual(["News"]);
    expect(result.skippedOther).toEqual(["base"]);
    expect(result.failures).toEqual([
      expect.objectContaining({ id: "43", name: "About", status: 409 }),
    ]);
    const urls = global.fetch.mock.calls.map((call) => String(call[0]));
    expect(urls.some((url) => url.includes("takedown/page/staging/42"))).toBe(
      true,
    );
    expect(urls.some((url) => url.includes("takedown/page/staging/43"))).toBe(
      true,
    );
    expect(urls.some((url) => url.includes("/staging/7"))).toBe(false);
  });

  it("recognizes Remove from Staging action name variants", () => {
    expect(isRemoveFromStagingActionName("Remove_from_Staging")).toBe(true);
    expect(isRemoveFromStagingActionName("Remove from Staging")).toBe(true);
    expect(isRemoveFromStagingActionName("unstage")).toBe(true);
    expect(isRemoveFromStagingActionName("Stage")).toBe(false);
    expect(isRemoveFromStagingActionName("Take_Down")).toBe(false);
  });
});
