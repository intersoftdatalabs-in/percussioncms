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
 * Resolve a ContentBrowser / product page-picker selection into a landing
 * page id for {@code POST /section/replaceLandingPage} (#3304).
 *
 * Empty, cancel, folder, and asset selections must not reach the server
 * (blank ids 500 in {@code ReplaceLandingPageHelper}).
 */

export const LANDING_PAGE_ALLOWED_TYPES = ["page"] as const;

export type LandingPagePickError = "empty" | "notPage";

export type LandingPagePick =
  | { ok: true; id: string; label: string }
  | { ok: false; error: LandingPagePickError };

export interface LandingPagePickItem {
  id?: string | null;
  name?: string | null;
  path?: string | null;
  type?: string | null;
  category?: string | null;
}

export interface LandingPageSelection {
  items?: LandingPagePickItem[] | null;
}

function itemKind(item: LandingPagePickItem): string {
  return String(item.type ?? item.category ?? "")
    .trim()
    .toLowerCase();
}

function isPageKind(kind: string): boolean {
  if (!kind) {
    return true;
  }
  return kind === "page" || kind === "percpage";
}

/**
 * First selected item → landing page id, or a reason to show a local error.
 */
export function resolveLandingPagePick(
  selection: LandingPageSelection | null | undefined,
): LandingPagePick {
  const item = selection?.items?.[0];
  if (!item) {
    return { ok: false, error: "empty" };
  }
  const id = String(item.id ?? "").trim();
  if (!id) {
    return { ok: false, error: "empty" };
  }
  if (!isPageKind(itemKind(item))) {
    return { ok: false, error: "notPage" };
  }
  const label = String(item.name || item.path || id).trim() || id;
  return { ok: true, id, label };
}

/**
 * Client-side replace body guard — never POST blank section or page ids.
 */
export function canPostReplaceLandingPage(
  sectionId: string | null | undefined,
  newLandingPageId: string | null | undefined,
): boolean {
  return (
    String(sectionId ?? "").trim().length > 0 &&
    String(newLandingPageId ?? "").trim().length > 0
  );
}
