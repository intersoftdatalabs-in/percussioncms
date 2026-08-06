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
 * Maps legacy Home {@code initialScreen} query values to modern sections.
 *
 * @see specs/989-react-cui-widget-builder/contracts/home-deep-links.md
 */

export type HomeSection =
  | "recent"
  | "bookmarks"
  | "library"
  | "search"
  | "create"
  | "gadgets";

const INITIAL_SCREEN_MAP: Record<string, HomeSection> = {
  library: "library",
  list: "recent",
  search: "search",
  newitem: "create",
  bookmarks: "bookmarks",
  bookmark: "bookmarks",
  // Former peer dashboard surface → Home gadgets (PR-7)
  dash: "gadgets",
  dashboard: "gadgets",
  widgets: "gadgets",
  gadget: "gadgets",
};

const MODERN_SECTIONS: readonly HomeSection[] = [
  "recent",
  "bookmarks",
  "library",
  "search",
  "create",
  "gadgets",
];

/**
 * Map a legacy initialScreen value (or modern section name) to a Home section.
 * Unknown values default to {@code recent}.
 */
export function mapInitialScreenToSection(
  initialScreen: string | null | undefined,
): HomeSection {
  if (initialScreen == null || initialScreen === "") {
    return "recent";
  }
  const normalized = initialScreen.trim().toLowerCase();
  if ((MODERN_SECTIONS as readonly string[]).includes(normalized)) {
    return normalized as HomeSection;
  }
  return INITIAL_SCREEN_MAP[normalized] ?? "recent";
}

/** All known legacy initialScreen values that map to modern sections. */
export function knownLegacyInitialScreens(): string[] {
  return Object.keys(INITIAL_SCREEN_MAP);
}
