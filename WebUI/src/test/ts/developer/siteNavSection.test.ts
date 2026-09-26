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
import type { NavTreeNode } from "../../../main/ts/api/architecture/types";
import {
  buildDeveloperAddSectionFields,
  isDeveloperNavSectionReadOnly,
  listDeveloperNavParents,
  listDeveloperSectionTitles,
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

  it("lists titles and only parents that can host a child", () => {
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
});
