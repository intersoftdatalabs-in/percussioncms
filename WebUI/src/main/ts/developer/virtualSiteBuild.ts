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
 * Build a short operator-facing success summary from a build result DTO.
 * Pure helper for Vitest and the panel (no i18n — callers prefix labels).
 */
export function formatVirtualSiteBuildSummary(result: VirtualSiteBuildResult): {
  pagesLine: string;
  outputLine: string | null;
  linkLine: string | null;
  hasLinkProblems: boolean;
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

  const linkCount =
    typeof result.linkProblemCount === "number" && Number.isFinite(result.linkProblemCount)
      ? result.linkProblemCount
      : 0;
  const hasLinkProblems =
    result.hasLinkProblems === true ||
    linkCount > 0 ||
    (Array.isArray(result.linkProblems) && result.linkProblems.length > 0);

  const linkLine = hasLinkProblems ? String(linkCount > 0 ? linkCount : result.linkProblems?.length ?? 0) : null;

  return {
    pagesLine,
    outputLine: output,
    linkLine,
    hasLinkProblems,
  };
}
