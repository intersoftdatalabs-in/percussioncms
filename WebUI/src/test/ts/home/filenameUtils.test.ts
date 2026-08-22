/*
 * Copyright (c) 2026 Intersoft Data Labs, Inc.
 */

import { describe, it, expect } from "vitest";
import {
  cmsPathSegments,
  joinFolderAndName,
  normalizeCmsPath,
  parentCmsPath,
  repositoryFolderFromPathItem,
  siteRootFolderFromSummary,
  titleToBlogFileName,
  titleToPageFileName,
  toRepositoryCmsPath,
} from "@/home/create/filenameUtils";

describe("filenameUtils", () => {
  it("maps title to page file name like classic CUI with .html", () => {
    expect(titleToPageFileName("Hello World!")).toBe("hello-world.html");
  });

  it("maps title to blog file name with .html", () => {
    expect(titleToBlogFileName("My_Post Title")).toBe("my-post-title.html");
  });

  it("normalizes and joins paths", () => {
    expect(normalizeCmsPath("//Sites/a")).toBe("/Sites/a");
    expect(joinFolderAndName("/Sites/a", "page")).toBe("/Sites/a/page");
    expect(joinFolderAndName("/Sites/a/", "page")).toBe("/Sites/a/page");
  });

  it("converts UI paths to repository // form for page create", () => {
    expect(toRepositoryCmsPath("/Sites/Demo")).toBe("//Sites/Demo");
    expect(toRepositoryCmsPath("//Sites/Demo")).toBe("//Sites/Demo");
    expect(toRepositoryCmsPath("Sites/Demo")).toBe("//Sites/Demo");
  });

  it("parentCmsPath goes up and exits site root to null", () => {
    expect(parentCmsPath("/Sites/Demo/blog/posts")).toBe("/Sites/Demo/blog");
    expect(parentCmsPath("/Sites/Demo/blog")).toBe("/Sites/Demo");
    expect(parentCmsPath("/Sites/Demo")).toBeNull();
    expect(parentCmsPath("//Sites/Demo/foo")).toBe("/Sites/Demo");
    expect(parentCmsPath("/Assets")).toBeNull();
  });

  it("cmsPathSegments splits normalized path", () => {
    expect(cmsPathSegments("/Sites/Demo/blog")).toEqual([
      "Sites",
      "Demo",
      "blog",
    ]);
    expect(cmsPathSegments("//Sites/a")).toEqual(["Sites", "a"]);
  });

  it("siteRootFolderFromSummary prefers repository folder over SITENAME (#3726)", () => {
    expect(
      siteRootFolderFromSummary({
        name: "Corporate_Investments",
        folderPath: "//Sites/CorporateInvestments",
      }),
    ).toBe("/Sites/CorporateInvestments");
    expect(
      siteRootFolderFromSummary({
        name: "Corporate_Investments",
        folderPaths: ["//Sites/CorporateInvestments"],
      }),
    ).toBe("/Sites/CorporateInvestments");
    expect(siteRootFolderFromSummary({ name: "Demo" })).toBe("/Sites/Demo");
    expect(siteRootFolderFromSummary("Demo")).toBe("/Sites/Demo");
    expect(siteRootFolderFromSummary("")).toBe("");
  });

  it("repositoryFolderFromPathItem uses PathItem.folderPath like classic get_folder_path", () => {
    expect(
      repositoryFolderFromPathItem(
        { folderPath: "//Sites/CorporateInvestments" },
        "/Sites/Corporate_Investments",
      ),
    ).toBe("/Sites/CorporateInvestments");
    expect(
      repositoryFolderFromPathItem(
        { folderPaths: ["//Sites/EnterpriseInvestments"] },
        "/Sites/Enterprise_Investments",
      ),
    ).toBe("/Sites/EnterpriseInvestments");
    expect(
      repositoryFolderFromPathItem({}, "/Sites/Corporate_Investments"),
    ).toBe("/Sites/Corporate_Investments");
  });
});

