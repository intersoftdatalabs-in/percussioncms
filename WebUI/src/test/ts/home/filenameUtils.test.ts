/*
 * Copyright 1999-2026 Percussion Software, Inc.
 */

import { describe, it, expect } from "vitest";
import {
  joinFolderAndName,
  normalizeCmsPath,
  titleToBlogFileName,
  titleToPageFileName,
} from "@/home/create/filenameUtils";

describe("filenameUtils", () => {
  it("maps title to page file name like classic CUI", () => {
    expect(titleToPageFileName("Hello World!")).toBe("hello-world");
  });

  it("maps title to blog file name", () => {
    expect(titleToBlogFileName("My_Post Title")).toBe("my-post-title");
  });

  it("normalizes and joins paths", () => {
    expect(normalizeCmsPath("//Sites/a")).toBe("/Sites/a");
    expect(joinFolderAndName("/Sites/a", "page")).toBe("/Sites/a/page");
    expect(joinFolderAndName("/Sites/a/", "page")).toBe("/Sites/a/page");
  });
});
