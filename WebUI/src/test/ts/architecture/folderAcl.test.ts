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
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import { describe, expect, it, vi } from "vitest";
import type { NavTreeNode } from "../../../main/ts/api/architecture/types";
import {
  canEditFolderAcl,
  folderPropertiesFromSection,
  isFolderPropertiesId,
  resolveSectionFolderId,
  resolveSectionFolderPath,
} from "../../../main/ts/architecture/folderAcl";

function section(
  overrides: Partial<NavTreeNode> = {},
): NavTreeNode {
  return {
    id: "c1",
    title: "About",
    folderPath: "//Sites/Demo/About",
    sectionType: "section",
    requiresLogin: false,
    children: [],
    ...overrides,
  };
}

describe("Architecture folder ACL helpers (#3588)", () => {
  it("allows regular sections (including site root) with a folder path", () => {
    expect(canEditFolderAcl(section())).toBe(true);
    expect(
      canEditFolderAcl(
        section({
          id: "root",
          title: "Home",
          folderPath: "//Sites/Demo",
        }),
      ),
    ).toBe(true);
  });

  it("blocks blogs, links, and missing folder paths", () => {
    expect(canEditFolderAcl(null)).toBe(false);
    expect(canEditFolderAcl(section({ sectionType: "blog" }))).toBe(false);
    expect(canEditFolderAcl(section({ sectionType: "sectionlink" }))).toBe(
      false,
    );
    expect(canEditFolderAcl(section({ sectionType: "externallink" }))).toBe(
      false,
    );
    expect(canEditFolderAcl(section({ folderPath: null }))).toBe(false);
    expect(canEditFolderAcl(section({ folderPath: "/" }))).toBe(false);
    expect(canEditFolderAcl(section({ folderPath: "   " }))).toBe(false);
  });

  it("falls back to //Sites/{site} for a root navon with no folderPath", () => {
    const root = section({
      id: "root",
      title: "Home Page",
      folderPath: null,
    });
    expect(canEditFolderAcl(root)).toBe(false);
    expect(
      canEditFolderAcl(root, { siteName: "Acl3588zrunh7", isRoot: true }),
    ).toBe(true);
    expect(
      resolveSectionFolderPath(root, {
        siteName: "Acl3588zrunh7",
        isRoot: true,
      }),
    ).toBe("//Sites/Acl3588zrunh7");
    expect(
      canEditFolderAcl(root, { siteName: "Acl3588zrunh7", isRoot: false }),
    ).toBe(false);
  });

  it("resolves folder id via the injected lookup", async () => {
    const resolve = vi.fn().mockResolvedValue("folder-42");
    await expect(
      resolveSectionFolderId("//Sites/Demo/About", resolve),
    ).resolves.toBe("folder-42");
    expect(resolve).toHaveBeenCalledWith("//Sites/Demo/About");
  });

  it("maps section folderPermission onto FolderSecurityPanel props", () => {
    const mapped = folderPropertiesFromSection({
      id: "16777215-101-10002",
      title: "Home Page",
      folderName: "Acl3588zrtmg8",
      folderPermission: {
        accessLevel: "WRITE",
        adminPrincipals: [{ name: "Admin", type: "ROLE" }],
        writePrincipals: { name: "CI_Members", type: "ROLE" },
      },
    });
    expect(mapped.id).toBe("16777215-101-10002");
    expect(mapped.permission?.accessLevel).toBe("WRITE");
    expect(mapped.permission?.adminPrincipals?.map((p) => p.name)).toEqual([
      "Admin",
    ]);
    expect(mapped.permission?.writePrincipals?.map((p) => p.name)).toEqual([
      "CI_Members",
    ]);
  });

  it("accepts GUID folder ids and rejects site-name slugs", () => {
    expect(isFolderPropertiesId("16777215-101-524")).toBe(true);
    expect(isFolderPropertiesId("10002")).toBe(true);
    expect(isFolderPropertiesId("Acl3588zrtmg8")).toBe(false);
    expect(isFolderPropertiesId("Corporate_Investments")).toBe(false);
    expect(isFolderPropertiesId("")).toBe(false);
  });

  it("does not look up root or blank folder paths", async () => {
    const resolve = vi.fn().mockResolvedValue("should-not-run");
    await expect(resolveSectionFolderId("/", resolve)).resolves.toBeUndefined();
    await expect(resolveSectionFolderId("  ", resolve)).resolves.toBeUndefined();
    await expect(resolveSectionFolderId(null, resolve)).resolves.toBeUndefined();
    expect(resolve).not.toHaveBeenCalled();
  });
});
