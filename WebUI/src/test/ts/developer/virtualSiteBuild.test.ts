/*
 * Copyright (c) 2026 Intersoft Data Labs, Inc.
 */

import { describe, expect, it } from "vitest";
import {
  formatVirtualSiteBuildSummary,
  formatVirtualSitePublishSummary,
  normalizeVirtualSiteLinkProblems,
  sanitizeVirtualPreviewHomePath,
  shouldShowVirtualBuildChrome,
  shouldShowVirtualPreviewChrome,
  shouldShowVirtualPublishChrome,
} from "../../../main/ts/developer/virtualSiteBuild";

describe("virtualSiteBuild helpers", () => {
  it("shouldShowVirtualBuildChrome for git-filesystem, csv-filesystem, sql-database, http-json, object-storage, rss-atom, and icalendar", () => {
    expect(shouldShowVirtualBuildChrome(null)).toBe(false);
    expect(shouldShowVirtualBuildChrome("")).toBe(false);
    expect(shouldShowVirtualBuildChrome("repository")).toBe(false);
    expect(shouldShowVirtualBuildChrome("Repository")).toBe(false);
    expect(shouldShowVirtualBuildChrome("sql-api")).toBe(false);
    expect(shouldShowVirtualBuildChrome("git-filesystem")).toBe(true);
    expect(shouldShowVirtualBuildChrome("  git-filesystem  ")).toBe(true);
    expect(shouldShowVirtualBuildChrome("csv-filesystem")).toBe(true);
    expect(shouldShowVirtualBuildChrome("CSV-Filesystem")).toBe(true);
    expect(shouldShowVirtualBuildChrome("sql-database")).toBe(true);
    expect(shouldShowVirtualBuildChrome("SQL-Database")).toBe(true);
    expect(shouldShowVirtualBuildChrome("http-json")).toBe(true);
    expect(shouldShowVirtualBuildChrome("HTTP-JSON")).toBe(true);
    expect(shouldShowVirtualBuildChrome("  http-json  ")).toBe(true);
    expect(shouldShowVirtualBuildChrome("object-storage")).toBe(true);
    expect(shouldShowVirtualBuildChrome("Object-Storage")).toBe(true);
    expect(shouldShowVirtualBuildChrome("  object-storage  ")).toBe(true);
    expect(shouldShowVirtualBuildChrome("rss-atom")).toBe(true);
    expect(shouldShowVirtualBuildChrome("RSS-Atom")).toBe(true);
    expect(shouldShowVirtualBuildChrome("  rss-atom  ")).toBe(true);
    expect(shouldShowVirtualBuildChrome("icalendar")).toBe(true);
    expect(shouldShowVirtualBuildChrome("ICalendar")).toBe(true);
    expect(shouldShowVirtualBuildChrome("  icalendar  ")).toBe(true);
    expect(shouldShowVirtualBuildChrome("sitemap-xml")).toBe(false);
    expect(shouldShowVirtualBuildChrome("Sitemap-XML")).toBe(false);
    expect(shouldShowVirtualBuildChrome("  sitemap-xml  ")).toBe(false);
  });

  it("shouldShowVirtualPreviewChrome for git, csv, sql-database, http-json, object-storage, rss-atom, and icalendar (not repository)", () => {
    expect(shouldShowVirtualPreviewChrome(null)).toBe(false);
    expect(shouldShowVirtualPreviewChrome("")).toBe(false);
    expect(shouldShowVirtualPreviewChrome("repository")).toBe(false);
    expect(shouldShowVirtualPreviewChrome("sql-api")).toBe(false);
    expect(shouldShowVirtualPreviewChrome("git-filesystem")).toBe(true);
    expect(shouldShowVirtualPreviewChrome("csv-filesystem")).toBe(true);
    expect(shouldShowVirtualPreviewChrome("  CSV-Filesystem  ")).toBe(true);
    expect(shouldShowVirtualPreviewChrome("sql-database")).toBe(true);
    expect(shouldShowVirtualPreviewChrome("SQL-Database")).toBe(true);
    expect(shouldShowVirtualPreviewChrome("http-json")).toBe(true);
    expect(shouldShowVirtualPreviewChrome("HTTP-JSON")).toBe(true);
    expect(shouldShowVirtualPreviewChrome("  http-json  ")).toBe(true);
    expect(shouldShowVirtualPreviewChrome("object-storage")).toBe(true);
    expect(shouldShowVirtualPreviewChrome("Object-Storage")).toBe(true);
    expect(shouldShowVirtualPreviewChrome("  object-storage  ")).toBe(true);
    expect(shouldShowVirtualPreviewChrome("rss-atom")).toBe(true);
    expect(shouldShowVirtualPreviewChrome("RSS-Atom")).toBe(true);
    expect(shouldShowVirtualPreviewChrome("  rss-atom  ")).toBe(true);
    expect(shouldShowVirtualPreviewChrome("icalendar")).toBe(true);
    expect(shouldShowVirtualPreviewChrome("ICalendar")).toBe(true);
    expect(shouldShowVirtualPreviewChrome("  icalendar  ")).toBe(true);
    expect(shouldShowVirtualPreviewChrome("sitemap-xml")).toBe(false);
    expect(shouldShowVirtualPreviewChrome("Sitemap-XML")).toBe(false);
    expect(shouldShowVirtualPreviewChrome("  sitemap-xml  ")).toBe(false);
  });

  it("shouldShowVirtualPublishChrome for git-filesystem, csv-filesystem, sql-database, http-json, object-storage, rss-atom, and icalendar", () => {
    expect(shouldShowVirtualPublishChrome(null)).toBe(false);
    expect(shouldShowVirtualPublishChrome("")).toBe(false);
    expect(shouldShowVirtualPublishChrome("repository")).toBe(false);
    expect(shouldShowVirtualPublishChrome("Repository")).toBe(false);
    expect(shouldShowVirtualPublishChrome("sql-api")).toBe(false);
    expect(shouldShowVirtualPublishChrome("git-filesystem")).toBe(true);
    expect(shouldShowVirtualPublishChrome("  git-filesystem  ")).toBe(true);
    expect(shouldShowVirtualPublishChrome("csv-filesystem")).toBe(true);
    expect(shouldShowVirtualPublishChrome("CSV-Filesystem")).toBe(true);
    expect(shouldShowVirtualPublishChrome("sql-database")).toBe(true);
    expect(shouldShowVirtualPublishChrome("SQL-Database")).toBe(true);
    expect(shouldShowVirtualPublishChrome("http-json")).toBe(true);
    expect(shouldShowVirtualPublishChrome("HTTP-JSON")).toBe(true);
    expect(shouldShowVirtualPublishChrome("  http-json  ")).toBe(true);
    expect(shouldShowVirtualPublishChrome("object-storage")).toBe(true);
    expect(shouldShowVirtualPublishChrome("Object-Storage")).toBe(true);
    expect(shouldShowVirtualPublishChrome("  object-storage  ")).toBe(true);
    expect(shouldShowVirtualPublishChrome("rss-atom")).toBe(true);
    expect(shouldShowVirtualPublishChrome("RSS-Atom")).toBe(true);
    expect(shouldShowVirtualPublishChrome("  rss-atom  ")).toBe(true);
    expect(shouldShowVirtualPublishChrome("icalendar")).toBe(true);
    expect(shouldShowVirtualPublishChrome("ICalendar")).toBe(true);
    expect(shouldShowVirtualPublishChrome("  icalendar  ")).toBe(true);
    expect(shouldShowVirtualPublishChrome("sitemap-xml")).toBe(false);
    expect(shouldShowVirtualPublishChrome("Sitemap-XML")).toBe(false);
    expect(shouldShowVirtualPublishChrome("  sitemap-xml  ")).toBe(false);
  });

  it("formatVirtualSitePublishSummary reports files copied and dest path", () => {
    const ok = formatVirtualSitePublishSummary({
      filesCopied: 15,
      publishPath: " C:/inetpub/help ",
      pagesWritten: 8,
      hasLinkProblems: false,
    });
    expect(ok.filesLine).toBe("15");
    expect(ok.destLine).toBe("C:/inetpub/help");
    expect(ok.pagesLine).toBe("8");
    expect(ok.hasLinkProblems).toBe(false);

    const missing = formatVirtualSitePublishSummary({});
    expect(missing.filesLine).toBe("0");
    expect(missing.destLine).toBeNull();
    expect(missing.pagesLine).toBe("0");
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
