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

import { beforeEach, describe, expect, it, vi } from "vitest";
import * as client from "../../../main/ts/api/client";
import {
  createTraditionalSite,
  formatCreateSiteError,
} from "../../../main/ts/api/contentExplorer/siteCreateApi";

vi.mock("../../../main/ts/api/client", async (importOriginal) => {
  const actual = await importOriginal<typeof import("../../../main/ts/api/client")>();
  return {
    ...actual,
    get: vi.fn(),
    put: vi.fn(),
    post: vi.fn(),
  };
});

const post = client.post as ReturnType<typeof vi.fn>;

describe("formatCreateSiteError (#4674)", () => {
  it("maps HTTP 403 to permission denied", () => {
    expect(
      formatCreateSiteError({
        status: 403,
        statusText: "Forbidden",
        body: {},
      }),
    ).toMatch(/permission/i);
  });

  it("maps HTTP 400 to invalid/conflict chrome, preferring body text", () => {
    expect(
      formatCreateSiteError({
        status: 400,
        statusText: "Bad Request",
        body: {},
      }),
    ).toMatch(/Could not create the site/i);
    expect(
      formatCreateSiteError({
        status: 400,
        statusText: "Bad Request",
        body: { message: "NavTree already exists" },
      }),
    ).toBe("NavTree already exists");
  });
});

describe("createTraditionalSite (#4674)", () => {
  beforeEach(() => {
    post.mockReset();
  });

  it("POSTs wrapped Site body", async () => {
    post.mockResolvedValue({ Site: { name: "Nightly" } });
    const out = await createTraditionalSite({
      name: "Nightly",
      baseTemplateName: "perc.base.plain",
      templateName: "NightlyTemplate",
    });
    expect(out.name).toBe("Nightly");
    expect(post).toHaveBeenCalledTimes(1);
  });

  it("rethrows 403 as mapped Error", async () => {
    post.mockRejectedValue({
      status: 403,
      statusText: "Forbidden",
      body: {},
    });
    await expect(
      createTraditionalSite({
        name: "Denied",
        baseTemplateName: "perc.base.plain",
        templateName: "DeniedTemplate",
      }),
    ).rejects.toThrow(/permission/i);
  });

  it("rethrows 400 as mapped Error", async () => {
    post.mockRejectedValue({
      status: 400,
      statusText: "Bad Request",
      body: { message: "duplicate" },
    });
    await expect(
      createTraditionalSite({
        name: "Dup",
        baseTemplateName: "perc.base.plain",
        templateName: "DupTemplate",
      }),
    ).rejects.toThrow(/duplicate/i);
  });
});
