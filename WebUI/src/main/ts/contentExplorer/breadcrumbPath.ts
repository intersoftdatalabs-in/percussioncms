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

/**
 * Breadcrumb wayfinding helpers for the Content Explorer path bar
 * (#6468 / parent #4530).
 *
 * <p>CMS folder paths always use {@code /} (not OS file separators) — the
 * same repository convention as {@link folderPath.ts}. These helpers are
 * pure so Vitest can cover overflow collapse and deep-link building without
 * a browser.</p>
 */

export const EXPLORER_BREADCRUMB_MAX_VISIBLE = 4;

/**
 * Split a CMS folder path into breadcrumb segment names.
 * Root ({@code /}), blank, and nullish paths yield no segments; the
 * wayfinding bar renders only the content-root home button for those.
 */
export function splitBreadcrumbSegments(
  folderPath: string | null | undefined,
): string[] {
  if (folderPath == null) return [];
  const raw = String(folderPath).trim().replace(/\\/g, "/");
  if (raw.length === 0) return [];
  return raw
    .split("/")
    .map((part) => part.trim())
    .filter((part) => part.length > 0);
}

/**
 * Cumulative CMS path for the segment at {@code index}
 * ({@code ["Sites","Demo"]} + 1 → {@code /Sites/Demo}).
 */
export function breadcrumbPathAt(segments: readonly string[], index: number): string {
  const upto = segments.slice(0, Math.max(0, Math.min(index + 1, segments.length)));
  return `/${upto.join("/")}`;
}

export type BreadcrumbCrumb =
  | { kind: "segment"; name: string; path: string; index: number }
  | { kind: "overflow"; names: string[] };

/**
 * Collapse long trails behind an overflow affordance. Short trails
 * ({@code <= maxVisible} segments) render every segment. Longer trails
 * keep the first segment plus the last {@code maxVisible - 1} segments and
 * collapse the middle behind one {@code overflow} crumb that carries the
 * hidden names for its accessible label.
 */
export function collapseBreadcrumbSegments(
  segments: readonly string[],
  maxVisible: number = EXPLORER_BREADCRUMB_MAX_VISIBLE,
): BreadcrumbCrumb[] {
  const names = [...segments];
  const limit = Math.max(2, Math.floor(maxVisible) || EXPLORER_BREADCRUMB_MAX_VISIBLE);
  if (names.length <= limit) {
    return names.map((name, index) => ({
      kind: "segment" as const,
      name,
      path: breadcrumbPathAt(names, index),
      index,
    }));
  }
  const tailCount = limit - 1;
  const tail = names.slice(names.length - tailCount);
  const hidden = names.slice(1, names.length - tailCount);
  const crumbs: BreadcrumbCrumb[] = [
    {
      kind: "segment",
      name: names[0],
      path: breadcrumbPathAt(names, 0),
      index: 0,
    },
    { kind: "overflow", names: hidden },
  ];
  tail.forEach((name, offset) => {
    const index = names.length - tailCount + offset;
    crumbs.push({ kind: "segment", name, path: breadcrumbPathAt(names, index), index });
  });
  return crumbs;
}

/**
 * Build a shareable deep link to an Explorer folder from a page URL:
 * the same {@code ExplorerRoute} entry with the folder as {@code ?path=}.
 * Returns {@code null} when there is no usable folder path or the base URL
 * cannot be parsed (never throws — callers surface the empty status).
 */
export function buildExplorerDeepLink(
  pageUrl: string | null | undefined,
  folderPath: string | null | undefined,
): string | null {
  const segments = splitBreadcrumbSegments(folderPath);
  if (segments.length === 0 || pageUrl == null) return null;
  const raw = String(pageUrl).trim();
  if (raw.length === 0) return null;
  try {
    const url = new URL(raw);
    url.searchParams.set("path", `/${segments.join("/")}`);
    url.hash = "";
    return url.toString();
  } catch {
    return null;
  }
}
