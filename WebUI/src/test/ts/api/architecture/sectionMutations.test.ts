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

import { describe, expect, it } from "vitest";
import {
  applyTitleToProperties,
  buildCreateSectionFromFolderBody,
  buildCreateSiteSectionBody,
  buildMoveSiteSectionBody,
  buildReparentMove,
  buildSiblingReorderMove,
  buildUpdateSiteSectionBody,
  canConvertSectionToFolder,
  canCreateChildUnder,
  canDeleteNavNode,
  canMoveNavNode,
  canMoveNavNodeDown,
  canMoveNavNodeUp,
  findNavNodeById,
  findSiblingPlacement,
  isRootNavNode,
  isSectionLinkType,
  isValidMoveTargetParent,
  listMoveTargetPositions,
  omitNavSubtree,
  parseSiteSectionPropertiesPayload,
  resolveCreateParentFolderPath,
  splitCmsPagePath,
  validateLandingPageName,
  validateSectionFolderName,
  validateSectionTitle,
  validateSourceFolderPath,
} from "../../../../main/ts/api/architecture/sectionMutations";
import type { NavTreeNode } from "../../../../main/ts/api/architecture/types";

function node(
  id: string,
  title: string,
  children: NavTreeNode[] = [],
  extras: Partial<NavTreeNode> = {},
): NavTreeNode {
  return {
    id,
    title,
    folderPath: extras.folderPath ?? `//Sites/Demo/${title}`,
    sectionType: extras.sectionType ?? "section",
    requiresLogin: false,
    children,
  };
}

const sampleTree: NavTreeNode = node("root", "Home", [
  node("a", "About"),
  node("b", "News", [node("b1", "Press")]),
  node("c", "Link", [], { sectionType: "sectionlink" }),
]);

describe("sectionMutations (#3096)", () => {
  it("validates title and folder name", () => {
    expect(validateSectionTitle("")).toMatch(/required/i);
    expect(validateSectionTitle("  Hello  ")).toBeNull();
    expect(validateSectionFolderName("")).toMatch(/required/i);
    expect(validateSectionFolderName("bad name")).toMatch(/letters/i);
    expect(validateSectionFolderName("good-name_1")).toBeNull();
  });

  it("finds nodes and sibling placement", () => {
    expect(findNavNodeById(sampleTree, "b1")?.title).toBe("Press");
    expect(findSiblingPlacement(sampleTree, "root")).toBeNull();
    const place = findSiblingPlacement(sampleTree, "b");
    expect(place).not.toBeNull();
    expect(place!.parent.id).toBe("root");
    expect(place!.index).toBe(1);
    expect(isRootNavNode(sampleTree, "root")).toBe(true);
    expect(isRootNavNode(sampleTree, "a")).toBe(false);
  });

  it("gates create/delete/move", () => {
    expect(canCreateChildUnder(sampleTree)).toBe(true);
    expect(
      canCreateChildUnder(findNavNodeById(sampleTree, "c")),
    ).toBe(false);
    expect(canDeleteNavNode(sampleTree, sampleTree)).toBe(false);
    expect(
      canDeleteNavNode(sampleTree, findNavNodeById(sampleTree, "a")),
    ).toBe(true);
    expect(canMoveNavNodeUp(sampleTree, "a")).toBe(false);
    expect(canMoveNavNodeUp(sampleTree, "b")).toBe(true);
    expect(canMoveNavNodeDown(sampleTree, "c")).toBe(false);
    expect(canMoveNavNodeDown(sampleTree, "a")).toBe(true);
  });

  it("gates convert-to-folder to regular non-root navons (#3302)", () => {
    expect(canConvertSectionToFolder(sampleTree, sampleTree)).toBe(false);
    expect(
      canConvertSectionToFolder(sampleTree, findNavNodeById(sampleTree, "a")),
    ).toBe(true);
    expect(
      canConvertSectionToFolder(sampleTree, findNavNodeById(sampleTree, "c")),
    ).toBe(false);
    const withBlog: NavTreeNode = node("root", "Home", [
      node("blog1", "News", [], { sectionType: "blog" }),
    ]);
    expect(
      canConvertSectionToFolder(withBlog, findNavNodeById(withBlog, "blog1")),
    ).toBe(false);
    expect(canConvertSectionToFolder(null, findNavNodeById(sampleTree, "a"))).toBe(
      false,
    );
  });

  it("validates create-from-folder fields and splits page paths (#3302)", () => {
    expect(validateSourceFolderPath("")).toMatch(/required/i);
    expect(validateSourceFolderPath("  //Sites/Demo/F  ")).toBeNull();
    expect(validateSourceFolderPath("not-a-sites-path")).toMatch(/Sites/i);
    expect(validateSourceFolderPath("//Sites")).toMatch(/Sites/i);
    expect(validateSourceFolderPath("/Assets/x")).toMatch(/Sites/i);
    expect(validateLandingPageName("")).toMatch(/required/i);
    expect(validateLandingPageName("index.html")).toBeNull();
    expect(validateLandingPageName("a/b")).toMatch(/file name/i);
    const split = splitCmsPagePath("/Sites/Demo/Folder/index.html");
    expect(split).toEqual({
      folderPath: "//Sites/Demo/Folder",
      pageName: "index.html",
    });
    expect(splitCmsPagePath("   ")).toBeNull();
  });

  it("builds create / update / move Jackson bodies", () => {
    const create = buildCreateSiteSectionBody({
      pageTitle: " Products ",
      pageLinkTitle: " Products ",
      pageName: "products",
      pageUrlIdentifier: "products",
      templateId: "tpl-1",
      folderPath: "/Sites/Demo",
      sectionType: "section",
    });
    expect(create.CreateSiteSection.pageTitle).toBe("Products");
    expect(create.CreateSiteSection.folderPath).toBe("//Sites/Demo");
    expect(create.CreateSiteSection.copyTemplates).toBe(true);

    const update = buildUpdateSiteSectionBody({
      id: "guid-1",
      title: "About Us",
      folderName: "about",
    });
    expect(update.SiteSectionProperties.title).toBe("About Us");

    const move = buildMoveSiteSectionBody({
      sourceId: "a",
      targetId: "root",
      targetIndex: 2,
      sourceParentId: "root",
    });
    expect(move.MoveSiteSection.targetIndex).toBe(2);

    const fromFolder = buildCreateSectionFromFolderBody({
      sourceFolderPath: "/Sites/Demo/Folder",
      pageName: " index.html ",
      parentFolderPath: "/Sites/Demo",
    });
    expect(fromFolder.CreateSectionFromFolderRequest.sourceFolderPath).toBe(
      "//Sites/Demo/Folder",
    );
    expect(fromFolder.CreateSectionFromFolderRequest.pageName).toBe(
      "index.html",
    );
    expect(fromFolder.PSCreateSectionFromFolderRequest.parentFolderPath).toBe(
      "//Sites/Demo",
    );
  });

  it("builds sibling reorder moves", () => {
    const up = buildSiblingReorderMove(sampleTree, "b", "up");
    expect(up).toEqual({
      sourceId: "b",
      targetId: "root",
      sourceParentId: "root",
      targetIndex: 0,
    });
    const down = buildSiblingReorderMove(sampleTree, "a", "down");
    expect(down?.targetIndex).toBe(1);
    expect(buildSiblingReorderMove(sampleTree, "a", "up")).toBeNull();
  });

  it("validates move-section targets and builds reparent payload (#3349)", () => {
    expect(canMoveNavNode(sampleTree, findNavNodeById(sampleTree, "a"))).toBe(
      true,
    );
    expect(canMoveNavNode(sampleTree, sampleTree)).toBe(false);
    expect(isValidMoveTargetParent(sampleTree, "a", "b")).toBe(true);
    expect(isValidMoveTargetParent(sampleTree, "a", "a")).toBe(false);
    expect(isValidMoveTargetParent(sampleTree, "b", "b1")).toBe(false);
    expect(isValidMoveTargetParent(sampleTree, "a", "c")).toBe(false);
    expect(isValidMoveTargetParent(sampleTree, "root", "b")).toBe(false);

    const omitted = omitNavSubtree(sampleTree, "b");
    expect(omitted?.children.map((n) => n.id)).toEqual(["a", "c"]);
    expect(findNavNodeById(omitted, "b1")).toBeNull();

    const positions = listMoveTargetPositions(
      findNavNodeById(sampleTree, "root"),
      "a",
    );
    expect(positions[0]).toEqual({
      targetIndex: 1,
      beforeId: "b",
      beforeTitle: "News",
    });
    expect(positions[positions.length - 1].targetIndex).toBe(-1);

    const reparent = buildReparentMove(sampleTree, "a", "b", -1);
    expect(reparent).toEqual({
      sourceId: "a",
      targetId: "b",
      sourceParentId: "root",
      targetIndex: -1,
    });
    expect(buildReparentMove(sampleTree, "b", "b1", 0)).toBeNull();
    expect(buildReparentMove(sampleTree, "a", "c", 0)).toBeNull();
  });

  it("resolves parent folder path and applies title", () => {
    const parent = node("p", "Parent", [], {
      folderPath: "/Sites/Demo/Parent",
    });
    expect(resolveCreateParentFolderPath(parent, "Demo")).toBe(
      "//Sites/Demo/Parent",
    );
    expect(resolveCreateParentFolderPath(null, "Demo")).toBe("//Sites/Demo");

    const props = applyTitleToProperties(
      { id: "1", title: "Old", folderName: "old" },
      " New Title ",
    );
    expect(props.title).toBe("New Title");
    expect(props.folderName).toBe("old");
  });

  it("parses properties payload and detects section links", () => {
    const parsed = parseSiteSectionPropertiesPayload({
      SiteSectionProperties: {
        id: "guid-9",
        title: "About",
        folderName: "about",
        target: "_blank",
        requiresLogin: true,
      },
    });
    expect(parsed?.id).toBe("guid-9");
    expect(parsed?.requiresLogin).toBe(true);
    expect(isSectionLinkType("sectionlink")).toBe(true);
    expect(isSectionLinkType("section")).toBe(false);
  });
});
