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

import type {
  VirtualSiteBuildResult,
  VirtualSitePublishResult,
} from "../api/developer/types";

function normalizedSourceKind(sourceKind: string | null | undefined): string {
  return (sourceKind ?? "").trim().toLowerCase();
}

/**
 * True when the Build Virtual Site control should be shown.
 * Git-filesystem, csv-filesystem, sql-database, http-json, object-storage,
 * rss-atom, icalendar, sitemap-xml, and robots-txt Virtual Sites all run
 * POST /virtual/build (SQL JDBC, HTTP JSON catalog, object-storage keys,
 * rss-atom feeds, icalendar .ics, sitemap.xml, and robots.txt stay in
 * _config.yaml / the local root). robots-txt Build is shown so operators can
 * produce last-build HTML for Preview. Publish stays hidden (later slice).
 * Repository / blank / unknown kinds must not display this chrome.
 */
export function shouldShowVirtualBuildChrome(
  sourceKind: string | null | undefined,
): boolean {
  const v = normalizedSourceKind(sourceKind);
  return (
    v === "git-filesystem" ||
    v === "csv-filesystem" ||
    v === "sql-database" ||
    v === "http-json" ||
    v === "object-storage" ||
    v === "rss-atom" ||
    v === "icalendar" ||
    v === "sitemap-xml" ||
    v === "robots-txt"
  );
}

/**
 * True when Preview assembled site should be shown.
 * Last-output preview for git-filesystem, csv-filesystem, sql-database,
 * http-json, object-storage, rss-atom, icalendar, sitemap-xml, and robots-txt.
 * Missing last-build stays unavailable (GET /virtual/preview available=false;
 * no fake preview). Repository / blank / unknown kinds stay hidden.
 */
export function shouldShowVirtualPreviewChrome(
  sourceKind: string | null | undefined,
): boolean {
  const v = normalizedSourceKind(sourceKind);
  return (
    v === "git-filesystem" ||
    v === "csv-filesystem" ||
    v === "sql-database" ||
    v === "http-json" ||
    v === "object-storage" ||
    v === "rss-atom" ||
    v === "icalendar" ||
    v === "sitemap-xml" ||
    v === "robots-txt"
  );
}

/**
 * True when the Publish Virtual Site control should be shown.
 * Git-filesystem, csv-filesystem, sql-database, http-json, object-storage,
 * rss-atom, icalendar, and sitemap-xml all run POST /virtual/publish (build
 * then copy last-build HTML to IPSSite.root). sitemap-xml leftover remoteUrl
 * and credentials fail closed on the server. Repository / blank / unknown
 * kinds stay hidden.
 */
export function shouldShowVirtualPublishChrome(
  sourceKind: string | null | undefined,
): boolean {
  const v = normalizedSourceKind(sourceKind);
  return (
    v === "git-filesystem" ||
    v === "csv-filesystem" ||
    v === "sql-database" ||
    v === "http-json" ||
    v === "object-storage" ||
    v === "rss-atom" ||
    v === "icalendar" ||
    v === "sitemap-xml"
  );
}

/**
 * Trim and drop empty {@code linkProblems} lines from a build result.
 * HTTP 200 + {@code hasLinkProblems} is still a completed build — this is
 * presentation only (same text as {@code link-report.txt}).
 */
export function normalizeVirtualSiteLinkProblems(
  result: VirtualSiteBuildResult | null | undefined,
): string[] {
  if (!result || !Array.isArray(result.linkProblems)) {
    return [];
  }
  const lines: string[] = [];
  for (const raw of result.linkProblems) {
    if (typeof raw !== "string") {
      continue;
    }
    const line = raw.trim();
    if (line.length > 0) {
      lines.push(line);
    }
  }
  return lines;
}

/**
 * Build a short operator-facing success summary from a build result DTO.
 * Pure helper for Vitest and the panel (no i18n — callers prefix labels).
 */
export function formatVirtualSiteBuildSummary(result: VirtualSiteBuildResult): {
  pagesLine: string;
  outputLine: string | null;
  linkLine: string | null;
  hasLinkProblems: boolean;
  linkProblems: string[];
} {
  const pages =
    typeof result.pagesWritten === "number" && Number.isFinite(result.pagesWritten)
      ? result.pagesWritten
      : 0;
  const pagesLine = String(pages);

  const output =
    typeof result.outputPath === "string" && result.outputPath.trim()
      ? result.outputPath.trim()
      : null;

  const linkProblems = normalizeVirtualSiteLinkProblems(result);
  const linkCount =
    typeof result.linkProblemCount === "number" && Number.isFinite(result.linkProblemCount)
      ? result.linkProblemCount
      : 0;
  const hasLinkProblems =
    result.hasLinkProblems === true || linkCount > 0 || linkProblems.length > 0;

  const resolvedCount = linkCount > 0 ? linkCount : linkProblems.length;
  const linkLine = hasLinkProblems ? String(resolvedCount) : null;

  return {
    pagesLine,
    outputLine: output,
    linkLine,
    hasLinkProblems,
    linkProblems,
  };
}

/**
 * Operator-facing success summary from a publish result DTO.
 * Highlights files copied and the Site filesystem destination path.
 */
export function formatVirtualSitePublishSummary(result: VirtualSitePublishResult): {
  filesLine: string;
  destLine: string | null;
  pagesLine: string;
  hasLinkProblems: boolean;
  linkLine: string | null;
  linkProblems: string[];
} {
  const files =
    typeof result.filesCopied === "number" && Number.isFinite(result.filesCopied)
      ? result.filesCopied
      : 0;
  const pages =
    typeof result.pagesWritten === "number" && Number.isFinite(result.pagesWritten)
      ? result.pagesWritten
      : 0;
  const dest =
    typeof result.publishPath === "string" && result.publishPath.trim()
      ? result.publishPath.trim()
      : null;

  const linkProblems = normalizeVirtualSiteLinkProblems(result);
  const linkCount =
    typeof result.linkProblemCount === "number" && Number.isFinite(result.linkProblemCount)
      ? result.linkProblemCount
      : 0;
  const hasLinkProblems =
    result.hasLinkProblems === true || linkCount > 0 || linkProblems.length > 0;
  const resolvedCount = linkCount > 0 ? linkCount : linkProblems.length;

  return {
    filesLine: String(files),
    destLine: dest,
    pagesLine: String(pages),
    hasLinkProblems,
    linkLine: hasLinkProblems ? String(resolvedCount) : null,
    linkProblems,
  };
}

/**
 * Safe relative home path for the preview stream. Rejects empty, absolute, and {@code ..}
 * segments so the UI never opens a traversal URL.
 */
export function sanitizeVirtualPreviewHomePath(
  homePath: string | null | undefined,
): string | null {
  if (typeof homePath !== "string") {
    return null;
  }
  const trimmed = homePath.trim().replace(/\\/g, "/");
  if (!trimmed) {
    return null;
  }
  if (trimmed.includes("://") || /^[a-zA-Z]:/.test(trimmed)) {
    return null;
  }
  const parts = trimmed.split("/").filter((seg) => seg.length > 0);
  if (parts.length === 0) {
    return null;
  }
  if (parts.some((seg) => seg === ".." || seg === ".")) {
    return null;
  }
  return parts.join("/");
}
