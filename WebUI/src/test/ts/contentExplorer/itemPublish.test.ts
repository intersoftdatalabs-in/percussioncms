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
  publishSelectedItem,
  resolvePublishKind,
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
});
