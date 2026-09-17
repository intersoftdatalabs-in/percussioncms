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
  isTakedownActionName,
  loadLinkedPagesForTakedown,
  parseLinkedPagesForTakedown,
  publishSelectedItem,
  resolvePublishKind,
  takedownSelectedItem,
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
  });
});
