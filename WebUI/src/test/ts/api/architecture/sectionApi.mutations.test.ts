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
  createSiteSection,
  deleteSectionLink,
  deleteSiteSection,
  loadSectionProperties,
  moveSiteSection,
  sectionDeleteLinkUrl,
  sectionDeleteUrl,
  sectionPropertiesUrl,
  updateSiteSection,
} from "../../../../main/ts/api/architecture/sectionApi";
import { PATHS } from "../../../../main/ts/api/paths";

describe("sectionApi mutations (#3096)", () => {
  beforeEach(() => {
    vi.restoreAllMocks();
  });

  afterEach(() => {
    vi.restoreAllMocks();
  });

  it("builds properties and delete URLs", () => {
    expect(sectionPropertiesUrl("guid/1")).toBe(
      `${PATHS.SECTION_PROPERTIES}/${encodeURIComponent("guid/1")}`,
    );
    expect(sectionDeleteUrl("abc")).toBe(`${PATHS.SECTION}/abc`);
    expect(sectionDeleteLinkUrl("s1", "p1")).toBe(
      `${PATHS.SECTION_DELETE_SECTION_LINK}/s1/p1`,
    );
  });

  it("createSiteSection posts CreateSiteSection body", async () => {
    const postSpy = vi.spyOn(client, "post").mockResolvedValue({ ok: true });
    await createSiteSection({
      pageTitle: "News",
      pageLinkTitle: "News",
      pageName: "news",
      pageUrlIdentifier: "news",
      templateId: "tpl-1",
      folderPath: "//Sites/Demo",
    });
    expect(postSpy).toHaveBeenCalledWith(
      PATHS.SECTION_CREATE_SECTION,
      expect.objectContaining({
        CreateSiteSection: expect.objectContaining({
          pageTitle: "News",
          templateId: "tpl-1",
          folderPath: "//Sites/Demo",
        }),
      }),
    );
  });

  it("loadSectionProperties unwraps SiteSectionProperties", async () => {
    vi.spyOn(client, "get").mockResolvedValue({
      SiteSectionProperties: {
        id: "g1",
        title: "About",
        folderName: "about",
      },
    });
    const props = await loadSectionProperties("g1");
    expect(props.title).toBe("About");
    expect(props.folderName).toBe("about");
  });

  it("updateSiteSection posts SiteSectionProperties", async () => {
    const postSpy = vi.spyOn(client, "post").mockResolvedValue({});
    await updateSiteSection({
      id: "g1",
      title: "About Us",
      folderName: "about",
    });
    expect(postSpy).toHaveBeenCalledWith(
      PATHS.SECTION_UPDATE,
      expect.objectContaining({
        SiteSectionProperties: expect.objectContaining({
          id: "g1",
          title: "About Us",
        }),
      }),
    );
  });

  it("moveSiteSection posts MoveSiteSection", async () => {
    const postSpy = vi.spyOn(client, "post").mockResolvedValue({});
    await moveSiteSection({
      sourceId: "a",
      targetId: "root",
      targetIndex: 1,
      sourceParentId: "root",
    });
    expect(postSpy).toHaveBeenCalledWith(
      PATHS.SECTION_MOVE,
      expect.objectContaining({
        MoveSiteSection: expect.objectContaining({
          sourceId: "a",
          targetIndex: 1,
        }),
      }),
    );
  });

  it("deleteSiteSection and deleteSectionLink call correct methods", async () => {
    const delSpy = vi.spyOn(client, "del").mockResolvedValue({});
    const getSpy = vi.spyOn(client, "get").mockResolvedValue({});
    await deleteSiteSection("sec-1");
    expect(delSpy).toHaveBeenCalledWith(`${PATHS.SECTION}/sec-1`);
    await deleteSectionLink("link-1", "parent-1");
    expect(getSpy).toHaveBeenCalledWith(
      `${PATHS.SECTION_DELETE_SECTION_LINK}/link-1/parent-1`,
    );
  });

  it("rejects blank ids", async () => {
    await expect(createSiteSection({
      pageTitle: "x",
      pageLinkTitle: "x",
      pageName: "x",
      pageUrlIdentifier: "x",
      templateId: "",
      folderPath: "//Sites/D",
    })).rejects.toThrow(/template/i);
    await expect(deleteSiteSection("  ")).rejects.toThrow(/id/i);
  });
});
