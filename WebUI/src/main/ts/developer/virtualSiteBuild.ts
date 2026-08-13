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

import type { VirtualSiteBuildResult } from "../api/developer/types";

/**
 * True when the Build Virtual Site control should be shown.
 * Repository / blank source kinds must not display build chrome.
 */
export function shouldShowVirtualBuildChrome(
  sourceKind: string | null | undefined,
): boolean {
  const v = (sourceKind ?? "").trim().toLowerCase();
  return v.length > 0 && v !== "repository";
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
