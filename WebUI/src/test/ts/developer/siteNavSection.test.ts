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
import { buildCreateExternalLinkBody } from "../../../main/ts/api/architecture/sectionMutations";
import type { NavTreeNode } from "../../../main/ts/api/architecture/types";
import {
  buildDeveloperAddSectionFields,
  buildDeveloperExternalLinkFields,
  buildDeveloperRenameProperties,
  buildDeveloperReparent,
  buildDeveloperSiblingReorder,
  isDeveloperNavSectionReadOnly,
  isDeveloperSectionNameTaken,
  listDeveloperDeleteTargets,
  listDeveloperExternalLinks,
  listDeveloperNavParents,
  listDeveloperRenameTargets,
  listDeveloperReparentParents,
  listDeveloperReorderTargets,
  listDeveloperSectionTitles,
  listDeveloperTemplateTargets,
  buildSectionLandingTemplateBody,
  landingTemplateSavePosts,
  parseSectionLandingTemplate,
  validateDeveloperSectionName,
} from "../../../main/ts/developer/siteNavSection";

const root: NavTreeNode = {
  id: "root",
  title: "Corporate",
  folderPath: "//Sites/Corporate",
  sectionType: "section",
  requiresLogin: false,
  children: [
    {
      id: "child",
      title: "News",
      folderPath: "//Sites/Corporate/News",
      sectionType: "section",
      requiresLogin: false,
      children: [],
    },
    {
      id: "link",
      title: "External",
      folderPath: null,
      sectionType: "externallink",
      requiresLogin: false,
      children: [],
    },
  ],
};

describe("siteNavSection", () => {
  it("posts a landing template only when the selection changed", () => {
    expect(listDeveloperTemplateTargets(root).map((t) => t.id)).toEqual(["root", "child"]);
    expect(landingTemplateSavePosts("tpl-a", "tpl-a")).toBe(false);
    expect(landingTemplateSavePosts("tpl-a", "  ")).toBe(false);
    expect(landingTemplateSavePosts("tpl-a", "tpl-b")).toBe(true);
    expect(buildSectionLandingTemplateBody(" child ", " tpl-b ")).toEqual({
      SectionLandingTemplate: { sectionId: "child", templateId: "tpl-b" },
    });
    expect(
      parseSectionLandingTemplate({
        SectionLandingTemplate: { sectionId: "child", templateId: "tpl-b" },
      })?.templateId,
    ).toBe("tpl-b");
    expect(parseSectionLandingTemplate({ templateId: "tpl-a" })?.templateId).toBe("tpl-a");
    expect(parseSectionLandingTemplate(null)).toBeNull();
  });

  it("rejects an empty or illegal section name and accepts a plain name", () => {
    expect(validateDeveloperSectionName("  ")).toBe("empty");
    expect(validateDeveloperSectionName("!!!")).toBe("invalid");
    expect(validateDeveloperSectionName("About Us")).toBeNull();
  });

  it("keeps managed-navigation-off and virtual sites read-only", () => {
    expect(isDeveloperNavSectionReadOnly({ name: "Sys", managedNavigation: false })).toBe(true);
    expect(
      isDeveloperNavSectionReadOnly({
        name: "Docs",
        virtual: { sourceKind: "git-filesystem", virtual: true },
      }),
    ).toBe(true);
    expect(isDeveloperNavSectionReadOnly({ name: "Corporate", managedNavigation: true })).toBe(
      false,
    );
    expect(isDeveloperNavSectionReadOnly({ name: "Corporate" })).toBe(false);
  });

  it("builds a CreateExternalLinkSection body from title, URL, target, and parent", () => {
    const fields = buildDeveloperExternalLinkFields({
      title: " Partner ",
      url: " https://partner.example ",
      target: "javascript:alert(1)",
      siteName: "Corporate",
      parent: root,
    });
    expect(fields.target).toBe("_self");
    expect(buildCreateExternalLinkBody(fields)).toEqual({
      CreateExternalLinkSection: {
        externalUrl: "https://partner.example",
        linkTitle: "Partner",
        folderPath: "//Sites/Corporate",
        sectionType: "externallink",
        target: "_self",
      },
    });
    expect(
      buildDeveloperExternalLinkFields({
        title: "Docs",
        url: "https://docs.example",
        target: "_blank",
        siteName: "Corporate",
        parent: root.children[0],
      }).target,
    ).toBe("_blank");
  });

  it("lists titles and only parents that can host a child", () => {
    expect(listDeveloperExternalLinks(root).map((link) => link.id)).toEqual(["link"]);
    expect(listDeveloperExternalLinks(null)).toEqual([]);
    expect(listDeveloperSectionTitles(root)).toEqual(["Corporate", "News", "External"]);
    const parents = listDeveloperNavParents(root, "Corporate");
    expect(parents.map((p) => p.id)).toEqual(["root", "child"]);
  });

  it("builds a create body from name and parent without extra writes", () => {
    const fields = buildDeveloperAddSectionFields({
      name: "About Us",
      siteName: "Corporate",
      parent: root,
      templateId: "tpl-1",
    });
    expect(fields.pageTitle).toBe("About Us");
    expect(fields.pageUrlIdentifier).toBe("about-us");
    expect(fields.pageName).toBe("about-us.html");
    expect(fields.templateId).toBe("tpl-1");
    expect(fields.folderPath.toLowerCase()).toContain("sites/corporate");
    expect(fields.sectionType).toBe("section");
  });

  it("lists rename targets and skips links", () => {
    const targets = listDeveloperRenameTargets(root);
    expect(targets.map((t) => t.id)).toEqual(["root", "child"]);
    expect(targets[0].siteRoot).toBe(true);
    expect(targets[1].siteRoot).toBe(false);
  });

  it("lists delete targets without the site root or links", () => {
    expect(listDeveloperDeleteTargets(root).map((t) => t.id)).toEqual(["child"]);
    expect(listDeveloperDeleteTargets(null)).toEqual([]);
  });

  it("reorders a non-root sibling and refuses the site root and links", () => {
    const tree: NavTreeNode = {
      ...root,
      children: [
        root.children[0],
        {
          id: "about",
          title: "About",
          folderPath: "//Sites/Corporate/About",
          sectionType: "section",
          requiresLogin: false,
          children: [],
        },
        root.children[1],
      ],
    };
    expect(listDeveloperReorderTargets(tree).map((t) => t.id)).toEqual(["child", "about"]);
    expect(listDeveloperReorderTargets(null)).toEqual([]);
    expect(buildDeveloperSiblingReorder(tree, "root", "down")).toBeNull();
    expect(buildDeveloperSiblingReorder(tree, "link", "up")).toBeNull();
    expect(buildDeveloperSiblingReorder(tree, "child", "up")).toBeNull();
    expect(buildDeveloperSiblingReorder(tree, "child", "down")).toEqual({
      sourceId: "child",
      targetId: "root",
      sourceParentId: "root",
      targetIndex: 1,
    });
  });

  it("reparents under a different parent and refuses root, same parent, and cycles", () => {
    const tree: NavTreeNode = {
      ...root,
      children: [
        {
          ...root.children[0],
          children: [
            {
              id: "story",
              title: "Story",
              folderPath: "//Sites/Corporate/News/Story",
              sectionType: "section",
              requiresLogin: false,
              children: [],
            },
          ],
        },
        {
          id: "about",
          title: "About",
          folderPath: "//Sites/Corporate/About",
          sectionType: "section",
          requiresLogin: false,
          children: [],
        },
        root.children[1],
      ],
    };
    expect(listDeveloperReparentParents(tree, "story").map((p) => p.id)).toEqual([
      "root",
      "child",
      "about",
    ]);
    expect(listDeveloperReparentParents(tree, "root")).toEqual([]);
    expect(buildDeveloperReparent(tree, "root", "about")).toBeNull();
    expect(buildDeveloperReparent(tree, "story", "child")).toBeNull();
    expect(buildDeveloperReparent(tree, "story", "story")).toBeNull();
    expect(buildDeveloperReparent(tree, "link", "about")).toBeNull();
    expect(buildDeveloperReparent(tree, "story", "about")).toEqual({
      sourceId: "story",
      targetId: "about",
      sourceParentId: "child",
      targetIndex: 0,
    });
  });

  it("treats another section title as taken and keeps the current section free", () => {
    expect(isDeveloperSectionNameTaken(root, "child", "Corporate")).toBe(true);
    expect(isDeveloperSectionNameTaken(root, "child", "news")).toBe(false);
    expect(isDeveloperSectionNameTaken(root, "child", "External")).toBe(true);
  });

  it("renames the display title and keeps the folder segment", () => {
    const child = buildDeveloperRenameProperties(
      { id: "child", title: "News", folderName: "News", siteRootSection: false },
      "Press Room",
    );
    expect(child.title).toBe("Press Room");
    expect(child.folderName).toBe("News");
  });
});
