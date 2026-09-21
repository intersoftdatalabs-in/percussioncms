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
import * as client from "../../../../main/ts/api/client";
import {
  createSite,
  deleteSite,
  isValidSiteName,
  updateSite,
  wrapSiteForWire,
} from "../../../../main/ts/api/developer/sitesApi";

vi.mock("../../../../main/ts/api/client", () => ({
  get: vi.fn(),
  post: vi.fn(),
  put: vi.fn(),
  del: vi.fn(),
}));

const post = client.post as ReturnType<typeof vi.fn>;
const put = client.put as ReturnType<typeof vi.fn>;
const del = client.del as ReturnType<typeof vi.fn>;

describe("sitesApi CUD", () => {
  beforeEach(() => {
    post.mockReset();
    put.mockReset();
    del.mockReset();
  });

  it("validates names", () => {
    expect(isValidSiteName("NightlySite")).toBe(true);
    expect(isValidSiteName("bad/name")).toBe(false);
    expect(isValidSiteName("")).toBe(false);
  });

  it("wraps Site root for Jackson", () => {
    expect(wrapSiteForWire({ name: "A" })).toEqual({ Site: { name: "A" } });
  });

  it("createSite posts wrapped body", async () => {
    post.mockResolvedValue({ Site: { name: "NightlySite", description: "Docs" } });
    const out = await createSite({ name: "NightlySite", description: "Docs" });
    expect(post).toHaveBeenCalledWith("/services/sites", {
      Site: { name: "NightlySite", description: "Docs" },
    });
    expect(out.name).toBe("NightlySite");
  });

  it("updateSite puts wrapped body", async () => {
    put.mockResolvedValue({ name: "NightlySite", description: "n" });
    const out = await updateSite("NightlySite", {
      name: "NightlySite",
      description: "n",
    });
    expect(put).toHaveBeenCalled();
    expect(out.description).toBe("n");
  });

  it("deleteSite deletes encoded name", async () => {
    del.mockResolvedValue(undefined);
    await deleteSite("My Site");
    expect(del).toHaveBeenCalledWith("/services/sites/My%20Site");
  });
});
