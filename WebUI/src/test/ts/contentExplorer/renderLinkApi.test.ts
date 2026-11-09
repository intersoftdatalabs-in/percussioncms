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
import {
  fetchInlineRenderLink,
  unwrapInlineRenderLink,
} from "../../../main/ts/api/contentExplorer/renderLinkApi";

afterEach(() => {
  vi.restoreAllMocks();
});

describe("unwrapInlineRenderLink", () => {
  it("unwraps the server InlineRenderLink envelope", () => {
    expect(
      unwrapInlineRenderLink({
        InlineRenderLink: {
          sys_dependentid: 77,
          sys_dependentvariantid: 512,
          url: "/Sites/About",
          title: "About",
        },
      }),
    ).toEqual({
      sys_dependentid: 77,
      sys_dependentvariantid: 512,
      url: "/Sites/About",
      title: "About",
    });
  });

  it("accepts a bare wire object for tests", () => {
    expect(unwrapInlineRenderLink({ url: "/Assets/logo.png" })).toEqual({
      url: "/Assets/logo.png",
    });
  });

  it("returns null when no link is present", () => {
    expect(unwrapInlineRenderLink(null)).toBeNull();
    expect(unwrapInlineRenderLink(undefined)).toBeNull();
    expect(unwrapInlineRenderLink({})).toBeNull();
    expect(unwrapInlineRenderLink({ InlineRenderLink: null })).toBeNull();
    expect(unwrapInlineRenderLink([])).toBeNull();
  });
});

describe("fetchInlineRenderLink", () => {
  it("GETs the renderlink preview endpoint and unwraps the envelope", async () => {
    vi.spyOn(global, "fetch").mockResolvedValueOnce(
      new Response(
        JSON.stringify({
          InlineRenderLink: {
            sys_dependentid: 77,
            sys_dependentvariantid: 512,
            url: "/Sites/About",
            title: "About",
            stateClass: "perc-managed",
          },
        }),
        { status: 200, headers: { "Content-Type": "application/json" } },
      ),
    );
    const link = await fetchInlineRenderLink(77);
    expect(link?.url).toBe("/Sites/About");
    expect(link?.sys_dependentid).toBe(77);
    const called = String(global.fetch.mock.calls[0]?.[0] ?? "");
    expect(called).toContain("pagemanagement/renderlink/preview/77/default");
  });

  it("passes titleField through as a query param", async () => {
    vi.spyOn(global, "fetch").mockResolvedValueOnce(
      new Response(JSON.stringify({ InlineRenderLink: { url: "/x" } }), {
        status: 200,
        headers: { "Content-Type": "application/json" },
      }),
    );
    await fetchInlineRenderLink("1-101-77", "page_title");
    const called = String(global.fetch.mock.calls[0]?.[0] ?? "");
    expect(called).toContain("preview/1-101-77/default?titleField=page_title");
  });

  it("rejects a blank content id without fetching", async () => {
    const spy = vi.spyOn(global, "fetch");
    await expect(fetchInlineRenderLink("  ")).rejects.toThrow(
      "requires contentId",
    );
    expect(spy).not.toHaveBeenCalled();
  });
});
