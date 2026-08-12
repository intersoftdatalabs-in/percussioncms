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
  createExternalLinkSection,
  createSectionLink,
  loadSection,
  replaceLandingPage,
  sectionCreateLinkUrl,
  sectionLoadUrl,
  sectionUpdateExternalLinkUrl,
  updateExternalLink,
  updateSectionLink,
} from "../../../../main/ts/api/architecture/sectionApi";
import { PATHS } from "../../../../main/ts/api/paths";

describe("sectionApi landing & links (#3097)", () => {
  beforeEach(() => {
    vi.restoreAllMocks();
  });

  afterEach(() => {
    vi.restoreAllMocks();
  });

  it("builds link URLs", () => {
    expect(sectionCreateLinkUrl("t1", "p1")).toBe(
      `${PATHS.SECTION_CREATE_SECTION_LINK}/t1/p1`,
    );
    expect(sectionUpdateExternalLinkUrl("guid/1")).toBe(
      `${PATHS.SECTION_UPDATE_EXTERNAL_LINK}/${encodeURIComponent("guid/1")}`,
    );
    expect(sectionLoadUrl("abc")).toBe(`${PATHS.SECTION}/abc`);
  });

  it("replaceLandingPage posts ReplaceLandingPage body", async () => {
    const postSpy = vi.spyOn(client, "post").mockResolvedValue({ ok: true });
    await replaceLandingPage({
      sectionId: "sec-1",
      newLandingPageId: "page-2",
    });
    expect(postSpy).toHaveBeenCalledWith(
      PATHS.SECTION_REPLACE_LANDING_PAGE,
      expect.objectContaining({
        ReplaceLandingPage: {
          sectionId: "sec-1",
          newLandingPageId: "page-2",
        },
      }),
    );
  });

  it("createSectionLink uses GET mutation", async () => {
    const getSpy = vi.spyOn(client, "get").mockResolvedValue({ ok: true });
    await createSectionLink("target", "parent");
    expect(getSpy).toHaveBeenCalledWith(
      `${PATHS.SECTION_CREATE_SECTION_LINK}/target/parent`,
    );
  });

  it("createExternalLinkSection posts CreateExternalLinkSection", async () => {
    const postSpy = vi.spyOn(client, "post").mockResolvedValue({ ok: true });
    await createExternalLinkSection({
      linkTitle: "Partner",
      externalUrl: "https://example.com",
      folderPath: "//Sites/Demo",
      target: "_blank",
    });
    expect(postSpy).toHaveBeenCalledWith(
      PATHS.SECTION_CREATE_EXTERNAL_LINK,
      expect.objectContaining({
        CreateExternalLinkSection: expect.objectContaining({
          linkTitle: "Partner",
          externalUrl: "https://example.com",
        }),
      }),
    );
  });

  it("updateSectionLink posts UpdateSectionLink", async () => {
    const postSpy = vi.spyOn(client, "post").mockResolvedValue({ ok: true });
    await updateSectionLink({
      oldSectionId: "old",
      newSectionId: "new",
      parentSectionId: "parent",
    });
    expect(postSpy).toHaveBeenCalledWith(
      PATHS.SECTION_UPDATE_SECTION_LINK,
      expect.objectContaining({
        UpdateSectionLink: {
          oldSectionId: "old",
          newSectionId: "new",
          parentSectionId: "parent",
        },
      }),
    );
  });

  it("updateExternalLink posts to section-scoped path", async () => {
    const postSpy = vi.spyOn(client, "post").mockResolvedValue({ ok: true });
    await updateExternalLink("sec-ext", {
      linkTitle: "X",
      externalUrl: "https://x.test",
      folderPath: "//Sites/Demo/X",
    });
    expect(postSpy).toHaveBeenCalledWith(
      sectionUpdateExternalLinkUrl("sec-ext"),
      expect.objectContaining({
        CreateExternalLinkSection: expect.objectContaining({
          linkTitle: "X",
        }),
      }),
    );
  });

  it("loadSection unwraps SiteSection", async () => {
    vi.spyOn(client, "get").mockResolvedValue({
      SiteSection: {
        id: "g1",
        title: "Ext",
        externalLinkUrl: "https://x.test",
        sectionType: "externallink",
      },
    });
    const sec = await loadSection("g1");
    expect(sec.id).toBe("g1");
    expect(sec.externalLinkUrl).toBe("https://x.test");
  });
});
