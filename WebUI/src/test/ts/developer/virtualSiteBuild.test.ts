/*
 * Copyright (c) 2026 Intersoft Data Labs, Inc.
 */

import { describe, expect, it } from "vitest";
import {
  formatVirtualSiteBuildSummary,
  sanitizeVirtualPreviewHomePath,
  shouldShowVirtualBuildChrome,
} from "../../../main/ts/developer/virtualSiteBuild";

describe("virtualSiteBuild helpers", () => {
  it("shouldShowVirtualBuildChrome only for virtual source kinds", () => {
    expect(shouldShowVirtualBuildChrome(null)).toBe(false);
    expect(shouldShowVirtualBuildChrome("")).toBe(false);
    expect(shouldShowVirtualBuildChrome("repository")).toBe(false);
    expect(shouldShowVirtualBuildChrome("Repository")).toBe(false);
    expect(shouldShowVirtualBuildChrome("git-filesystem")).toBe(true);
    expect(shouldShowVirtualBuildChrome("  git-filesystem  ")).toBe(true);
  });

  it("formatVirtualSiteBuildSummary reports pages, output, and link problems", () => {
    const clean = formatVirtualSiteBuildSummary({
      pagesWritten: 10,
      outputPath: "C:/tmp/out",
      linkProblemCount: 0,
      hasLinkProblems: false,
    });
    expect(clean.pagesLine).toBe("10");
    expect(clean.outputLine).toBe("C:/tmp/out");
    expect(clean.hasLinkProblems).toBe(false);
    expect(clean.linkLine).toBeNull();

    const dirty = formatVirtualSiteBuildSummary({
      pagesWritten: 2,
      outputPath: " /docs/out ",
      linkProblemCount: 3,
      hasLinkProblems: true,
      linkProblems: ["a", "b", "c"],
    });
    expect(dirty.pagesLine).toBe("2");
    expect(dirty.outputLine).toBe("/docs/out");
    expect(dirty.hasLinkProblems).toBe(true);
    expect(dirty.linkLine).toBe("3");

    const missing = formatVirtualSiteBuildSummary({});
    expect(missing.pagesLine).toBe("0");
    expect(missing.outputLine).toBeNull();
    expect(missing.hasLinkProblems).toBe(false);
  });

  it("sanitizeVirtualPreviewHomePath rejects traversal and absolute paths", () => {
    expect(sanitizeVirtualPreviewHomePath("8.2/index.html")).toBe("8.2/index.html");
    expect(sanitizeVirtualPreviewHomePath(" /8.2/admin/index.html ")).toBe("8.2/admin/index.html");
    expect(sanitizeVirtualPreviewHomePath("../etc/passwd")).toBeNull();
    expect(sanitizeVirtualPreviewHomePath("C:/tmp/out/index.html")).toBeNull();
    expect(sanitizeVirtualPreviewHomePath("https://evil.example/x")).toBeNull();
    expect(sanitizeVirtualPreviewHomePath("")).toBeNull();
    expect(sanitizeVirtualPreviewHomePath(null)).toBeNull();
  });
});
