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
import * as client from "../../../../main/ts/api/client";
import {
  loadSectionTree,
  sectionRootUrl,
  sectionTreeUrl,
} from "../../../../main/ts/api/architecture/sectionApi";
import { PATHS } from "../../../../main/ts/api/paths";

describe("sectionApi (#3095)", () => {
  beforeEach(() => {
    vi.restoreAllMocks();
  });

  afterEach(() => {
    vi.restoreAllMocks();
  });

  it("builds encoded tree and root URLs", () => {
    expect(sectionTreeUrl("Corporate Investments")).toBe(
      `${PATHS.SECTION_TREE}/${encodeURIComponent("Corporate Investments")}`,
    );
    expect(sectionRootUrl("Demo")).toBe(`${PATHS.SECTION_ROOT}/Demo`);
  });

  it("loadSectionTree maps GET payload to NavTreeNode", async () => {
    const getSpy = vi.spyOn(client, "get").mockResolvedValue({
      SectionNode: {
        id: "guid-1",
        title: "Home",
        sectionType: "section",
        childNodes: [{ id: "guid-2", title: "News", sectionType: "section" }],
      },
    });

    const tree = await loadSectionTree("Demo Site");
    expect(getSpy).toHaveBeenCalledWith(
      `${PATHS.SECTION_TREE}/${encodeURIComponent("Demo Site")}`,
    );
    expect(tree).not.toBeNull();
    expect(tree!.id).toBe("guid-1");
    expect(tree!.children).toHaveLength(1);
    expect(tree!.children[0].title).toBe("News");
  });

  it("loadSectionTree rejects blank site name", async () => {
    await expect(loadSectionTree("  ")).rejects.toThrow(/site name/i);
  });

  it("loadSectionTree returns null for empty payload", async () => {
    vi.spyOn(client, "get").mockResolvedValue(null);
    await expect(loadSectionTree("Demo")).resolves.toBeNull();
  });
});
