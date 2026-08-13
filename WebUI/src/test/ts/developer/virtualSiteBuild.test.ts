/*
 * Copyright (c) 2026 Intersoft Data Labs, Inc.
 */

import { describe, expect, it } from "vitest";
import {
  formatVirtualSiteBuildSummary,
  normalizeVirtualSiteLinkProblems,
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
    expect(clean.linkProblems).toEqual([]);

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
    expect(dirty.linkProblems).toEqual(["a", "b", "c"]);

    const missing = formatVirtualSiteBuildSummary({});
    expect(missing.pagesLine).toBe("0");
    expect(missing.outputLine).toBeNull();
    expect(missing.hasLinkProblems).toBe(false);
    expect(missing.linkProblems).toEqual([]);
  });

  it("normalizeVirtualSiteLinkProblems drops blanks and non-strings", () => {
    expect(normalizeVirtualSiteLinkProblems(null)).toEqual([]);
    expect(
      normalizeVirtualSiteLinkProblems({
        linkProblems: ["  missing id:foo  ", "", "   ", "ok", 7 as unknown as string],
      }),
    ).toEqual(["missing id:foo", "ok"]);
  });
});
