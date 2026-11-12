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
import * as client from "../../../main/ts/api/client";
import { fetchPageSiteImpact } from "../../../main/ts/editor/pageSiteImpactApi";

describe("fetchPageSiteImpact", () => {
  afterEach(() => {
    vi.restoreAllMocks();
  });

  it("GETs the site-impact path and parses the payload", async () => {
    const get = vi.spyOn(client, "get").mockResolvedValue({
      data: {
        pages: [
          {
            id: "7",
            name: "Home",
            path: "/Sites/Demo/Home",
            site: "Demo",
            status: "Live",
            type: "percPage",
          },
        ],
        templates: [{ name: "Default", site: "site" }],
      },
    });
    await expect(fetchPageSiteImpact("42")).resolves.toEqual({
      pages: [
        {
          id: "7",
          name: "Home",
          path: "/Sites/Demo/Home",
          site: "Demo",
          status: "Live",
          type: "percPage",
        },
      ],
      templates: [{ name: "Default", site: "site" }],
    });
    expect(get).toHaveBeenCalledWith(
      expect.stringContaining("/itemmanagement/item/siteimpact/page/42"),
    );
  });

  it("returns empty lists for an empty payload", async () => {
    vi.spyOn(client, "get").mockResolvedValue({});
    await expect(fetchPageSiteImpact("42")).resolves.toEqual({
      pages: [],
      templates: [],
    });
  });
});
