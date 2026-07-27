/*
 * Copyright 1999-2026 Percussion Software, Inc.
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

import { describe, it, expect, vi, beforeEach, afterEach } from "vitest";
import {
  fetchMyContent,
  fetchRecentItems,
  fetchSites,
  formatApiError,
} from "@/api/home/homeApi";
import type { ApiError } from "@/api/client";

describe("homeApi", () => {
  beforeEach(() => {
    vi.stubGlobal(
      "fetch",
      vi.fn(),
    );
  });

  afterEach(() => {
    vi.unstubAllGlobals();
  });

  function mockJsonResponse(body: unknown, init: { ok?: boolean; status?: number } = {}) {
    const text =
      typeof body === "string" ? body : JSON.stringify(body);
    return {
      ok: init.ok ?? true,
      status: init.status ?? 200,
      statusText: init.ok === false ? "Error" : "OK",
      headers: {
        get: (name: string) =>
          name.toLowerCase() === "content-type" ? "application/json" : null,
      },
      text: async () => text,
      json: async () => body,
    };
  }

  it("fetchRecentItems returns list payload", async () => {
    const fetchMock = globalThis.fetch as ReturnType<typeof vi.fn>;
    fetchMock.mockResolvedValue(
      mockJsonResponse([{ name: "Page A", id: "1" }]),
    );
    const items = await fetchRecentItems("item");
    expect(items).toHaveLength(1);
    expect(items[0].name).toBe("Page A");
  });

  it("fetchMyContent maps ItemProperties list", async () => {
    const fetchMock = globalThis.fetch as ReturnType<typeof vi.fn>;
    fetchMock.mockResolvedValue(
      mockJsonResponse({
        ItemProperties: [{ name: "Bookmarked", path: "/Sites/a/b" }],
      }),
    );
    const items = await fetchMyContent();
    expect(items).toHaveLength(1);
    expect(items[0].name).toBe("Bookmarked");
  });

  it("fetchSites surfaces API errors", async () => {
    const fetchMock = globalThis.fetch as ReturnType<typeof vi.fn>;
    fetchMock.mockResolvedValue(
      mockJsonResponse({ message: "nope" }, { ok: false, status: 403 }),
    );
    await expect(fetchSites()).rejects.toMatchObject({
      status: 403,
    } as Partial<ApiError>);
  });

  describe("formatApiError", () => {
    const notAuth = "Not authorized to create";

    it("maps 401/403 ApiError to notAuthorizedMsg", () => {
      const err: ApiError = { status: 403, statusText: "Forbidden", body: "" };
      expect(formatApiError(err, notAuth)).toBe(notAuth);
      expect(
        formatApiError(
          { status: 401, statusText: "Unauthorized", body: null },
          notAuth,
        ),
      ).toBe(notAuth);
    });

    it("maps body message containing NotAuthorized to notAuthorizedMsg", () => {
      const err: ApiError = {
        status: 500,
        statusText: "Error",
        body: { message: "NotAuthorized" },
      };
      expect(formatApiError(err, notAuth)).toBe(notAuth);
    });

    it("prefers non-auth body string for other failures", () => {
      const err: ApiError = {
        status: 400,
        statusText: "Bad Request",
        body: "Name is invalid",
      };
      expect(formatApiError(err, notAuth)).toBe("Name is invalid");
    });
  });
});
