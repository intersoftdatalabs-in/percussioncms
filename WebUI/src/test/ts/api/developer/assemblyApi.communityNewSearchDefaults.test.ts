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

import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import {
  COMMUNITY_NEW_SEARCH_DEFAULTS_ROOT,
  asCommunityNewSearchRefs,
  getCommunityNewSearchDefaults,
  replaceCommunityNewSearchDefaults,
  unwrapCommunityNewSearchDefaults,
  wrapCommunityNewSearchDefaultsForWire,
} from "../../../../main/ts/api/developer/assemblyApi";
import { PATHS } from "../../../../main/ts/api/paths";

describe("community new-search defaults wire (UI-09)", () => {
  it("wraps PUT under CommunityNewSearchDefaults including empty set", () => {
    expect(wrapCommunityNewSearchDefaultsForWire([])).toEqual({
      [COMMUNITY_NEW_SEARCH_DEFAULTS_ROOT]: { searches: [] },
    });
    expect(
      wrapCommunityNewSearchDefaultsForWire([{ name: "SimpleSearch", id: 42 }]),
    ).toEqual({
      [COMMUNITY_NEW_SEARCH_DEFAULTS_ROOT]: {
        searches: [{ name: "SimpleSearch", id: 42 }],
      },
    });
  });

  it("unwraps WRAP_ROOT, flat body, JAXB item wrap, and empty", () => {
    expect(
      unwrapCommunityNewSearchDefaults({
        CommunityNewSearchDefaults: {
          communityName: "Default",
          searches: [{ name: "SimpleSearch" }],
        },
      }).searches,
    ).toEqual([{ name: "SimpleSearch" }]);
    expect(
      unwrapCommunityNewSearchDefaults({
        communityName: "Default",
        searches: { CommunityNewSearchRef: { name: "Solo" } },
      }).searches,
    ).toEqual([{ name: "Solo" }]);
    expect(unwrapCommunityNewSearchDefaults(null).searches).toEqual([]);
    expect(asCommunityNewSearchRefs(undefined)).toEqual([]);
  });
});

describe("get/replace community new-search defaults", () => {
  const fetchMock = vi.fn();

  beforeEach(() => {
    fetchMock.mockReset();
    vi.stubGlobal("fetch", fetchMock);
  });

  afterEach(() => {
    vi.unstubAllGlobals();
  });

  function jsonResponse(body: unknown, status = 200): Response {
    return new Response(JSON.stringify(body), {
      status,
      statusText: status === 200 ? "OK" : "Error",
      headers: { "Content-Type": "application/json" },
    });
  }

  it("GET unwraps empty set as 200", async () => {
    fetchMock.mockResolvedValueOnce(
      jsonResponse({
        CommunityNewSearchDefaults: { communityName: "Default", searches: [] },
      }),
    );
    const out = await getCommunityNewSearchDefaults("Default");
    expect(out.communityName).toBe("Default");
    expect(out.searches).toEqual([]);
    expect(String(fetchMock.mock.calls[0]?.[0])).toContain(
      `${PATHS.COMMUNITIES}/Default/new-search-defaults`,
    );
  });

  it("PUT wraps searches and returns replaced set", async () => {
    fetchMock.mockResolvedValueOnce(
      jsonResponse({
        CommunityNewSearchDefaults: {
          searches: [{ name: "SimpleSearch", id: 42 }],
        },
      }),
    );
    const out = await replaceCommunityNewSearchDefaults("Default", [
      { name: "SimpleSearch" },
    ]);
    expect(out.searches).toEqual([{ name: "SimpleSearch", id: 42 }]);
    const init = fetchMock.mock.calls[0]?.[1] as RequestInit;
    expect(init.method).toBe("PUT");
    expect(JSON.parse(String(init.body))).toEqual({
      CommunityNewSearchDefaults: { searches: [{ name: "SimpleSearch" }] },
    });
  });

  it("PUT empty set clears", async () => {
    fetchMock.mockResolvedValueOnce(
      jsonResponse({ CommunityNewSearchDefaults: { searches: [] } }),
    );
    const out = await replaceCommunityNewSearchDefaults("Default", []);
    expect(out.searches).toEqual([]);
    const init = fetchMock.mock.calls[0]?.[1] as RequestInit;
    expect(JSON.parse(String(init.body))).toEqual({
      CommunityNewSearchDefaults: { searches: [] },
    });
  });

  it("PUT unknown search is 400", async () => {
    fetchMock.mockResolvedValueOnce(jsonResponse({ message: "Unknown search" }, 400));
    await expect(
      replaceCommunityNewSearchDefaults("Default", [{ name: "MissingSearch" }]),
    ).rejects.toMatchObject({ status: 400 });
  });

  it("GET/PUT non-Admin is 403", async () => {
    fetchMock.mockResolvedValueOnce(jsonResponse({ message: "Admin role required" }, 403));
    await expect(getCommunityNewSearchDefaults("Default")).rejects.toMatchObject({
      status: 403,
    });
    fetchMock.mockResolvedValueOnce(jsonResponse({ message: "Admin role required" }, 403));
    await expect(replaceCommunityNewSearchDefaults("Default", [])).rejects.toMatchObject({
      status: 403,
    });
  });
});
