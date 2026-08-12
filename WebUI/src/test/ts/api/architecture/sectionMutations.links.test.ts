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
  buildCreateExternalLinkBody,
  buildCreateSectionLinkPath,
  buildReplaceLandingPageBody,
  buildUpdateSectionLinkBody,
  canEditLinkNode,
  canReplaceLandingPage,
  isBlogSectionType,
  isExternalLinkType,
  isValidSectionLinkTarget,
  parseSiteSectionPayload,
  validateExternalUrl,
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
  node("d", "Ext", [], { sectionType: "externallink" }),
  node("e", "Blog", [], { sectionType: "blog" }),
]);

describe("sectionMutations links & landing (#3097)", () => {
  it("gates landing page and edit-link by type", () => {
    expect(canReplaceLandingPage(sampleTree)).toBe(true);
    expect(
      canReplaceLandingPage(
        sampleTree.children.find((c) => c.id === "c") ?? null,
      ),
    ).toBe(false);
    expect(
      canEditLinkNode(sampleTree.children.find((c) => c.id === "c") ?? null),
    ).toBe(true);
    expect(
      canEditLinkNode(sampleTree.children.find((c) => c.id === "d") ?? null),
    ).toBe(true);
    expect(canEditLinkNode(sampleTree)).toBe(false);
    expect(isExternalLinkType("externallink")).toBe(true);
    expect(isBlogSectionType("blog")).toBe(true);
  });

  it("validates section-link targets (self and direct child rejected)", () => {
    // Direct child of root is duplicate — invalid as link target under root
    expect(isValidSectionLinkTarget(sampleTree, "root", "a")).toBe(false);
    expect(isValidSectionLinkTarget(sampleTree, "root", "root")).toBe(false);
    // Nested child of News is OK as target under root (not a direct child)
    expect(isValidSectionLinkTarget(sampleTree, "root", "b1")).toBe(true);
    // Direct child of b rejected
    expect(isValidSectionLinkTarget(sampleTree, "b", "b1")).toBe(false);
    // Sibling of b under root is OK as target under b
    expect(isValidSectionLinkTarget(sampleTree, "b", "a")).toBe(true);
    expect(isValidSectionLinkTarget(sampleTree, "root", "missing")).toBe(
      false,
    );
    expect(isValidSectionLinkTarget(null, "root", "a")).toBe(false);
  });

  it("validates external URLs", () => {
    expect(validateExternalUrl("")).toMatch(/required/i);
    expect(validateExternalUrl("https://example.com")).toBeNull();
    expect(validateExternalUrl("/relative/path")).toBeNull();
    expect(validateExternalUrl("http://host/x?y=1")).toBeNull();
    expect(validateExternalUrl("ftp://files.example")).toBeNull();
    // Security: reject XSS-capable schemes (CRITICAL #3154 review)
    expect(validateExternalUrl("javascript:alert(1)")).toMatch(/not allowed|scheme/i);
    expect(validateExternalUrl("JAVASCRIPT:void(0)")).toMatch(/not allowed|scheme/i);
    expect(validateExternalUrl("data:text/html,hi")).toMatch(/not allowed|scheme/i);
    expect(validateExternalUrl("vbscript:msgbox(1)")).toMatch(/not allowed|scheme/i);
  });

  it("builds landing / external / section-link bodies", () => {
    const landing = buildReplaceLandingPageBody({
      sectionId: " sec-1 ",
      newLandingPageId: " page-9 ",
    });
    expect(landing.ReplaceLandingPage).toEqual({
      sectionId: "sec-1",
      newLandingPageId: "page-9",
    });

    const ext = buildCreateExternalLinkBody({
      externalUrl: " https://example.com ",
      linkTitle: " Partner ",
      folderPath: "/Sites/Demo",
      target: "_blank",
    });
    expect(ext.CreateExternalLinkSection.externalUrl).toBe(
      "https://example.com",
    );
    expect(ext.CreateExternalLinkSection.linkTitle).toBe("Partner");
    expect(ext.CreateExternalLinkSection.folderPath).toBe("//Sites/Demo");
    expect(ext.CreateExternalLinkSection.sectionType).toBe("externallink");

    const upd = buildUpdateSectionLinkBody({
      oldSectionId: "old",
      newSectionId: "new",
      parentSectionId: "parent",
    });
    expect(upd.UpdateSectionLink.newSectionId).toBe("new");

    expect(buildCreateSectionLinkPath("t1", "p1")).toBe("t1/p1");
    expect(buildCreateSectionLinkPath("", "p1")).toBeNull();
  });

  it("parses SiteSection payloads", () => {
    const parsed = parseSiteSectionPayload({
      SiteSection: {
        id: "g1",
        title: "Ext",
        externalLinkUrl: "https://x.test",
        sectionType: "externallink",
        target: "_blank",
        folderPath: "//Sites/Demo/Ext",
      },
    });
    expect(parsed?.id).toBe("g1");
    expect(parsed?.externalLinkUrl).toBe("https://x.test");
    expect(parsed?.sectionType).toBe("externallink");
    expect(parseSiteSectionPayload(null)).toBeNull();
  });
});
