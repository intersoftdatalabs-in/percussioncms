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
 * Typed section REST client for Architecture nav tree (#3095).
 *
 * <p>Read-only Slice C surface: load full tree via
 * {@code GET /sitemanage/section/tree/{siteName}}. Mutations land in Slice D.</p>
 */

import { get } from "../client";
import { PATHS } from "../paths";
import {
  mapSectionNodeToTree,
  parseSectionNodePayload,
} from "./mapSectionTree";
import type { NavTreeNode, SiteSectionWire } from "./types";

/**
 * Build the tree URL for a site name (exported for tests).
 * Site names may contain spaces; encode path segment safely.
 */
export function sectionTreeUrl(siteName: string): string {
  const key = encodeURIComponent(siteName.trim());
  return `${PATHS.SECTION_TREE}/${key}`;
}

/** Build the root-section URL for a site name (exported for tests). */
export function sectionRootUrl(siteName: string): string {
  const key = encodeURIComponent(siteName.trim());
  return `${PATHS.SECTION_ROOT}/${key}`;
}

/**
 * Load the full section/navon tree for a site.
 *
 * @param siteName CMS site name (e.g. {@code Corporate Investments})
 * @returns Normalized root {@link NavTreeNode}, or {@code null} when the
 *          payload is empty / unparseable (caller may treat as empty).
 */
export async function loadSectionTree(
  siteName: string,
): Promise<NavTreeNode | null> {
  const name = siteName.trim();
  if (!name) {
    throw new Error("Site name is required to load the navigation tree");
  }
  const payload = await get<unknown>(sectionTreeUrl(name));
  const wire = parseSectionNodePayload(payload);
  if (!wire) {
    return null;
  }
  return mapSectionNodeToTree(wire);
}

/**
 * Load the root section only (no children expansion).
 * Prefer {@link loadSectionTree} for Architecture browse.
 */
export async function loadRootSection(
  siteName: string,
): Promise<SiteSectionWire | null> {
  const name = siteName.trim();
  if (!name) {
    throw new Error("Site name is required to load the root section");
  }
  const payload = await get<unknown>(sectionRootUrl(name));
  if (payload == null || typeof payload !== "object") {
    return null;
  }
  const obj = payload as Record<string, unknown>;
  const root =
    (obj.SiteSection as SiteSectionWire | undefined) ??
    (obj.siteSection as SiteSectionWire | undefined) ??
    (payload as SiteSectionWire);
  return root ?? null;
}

export type { NavTreeNode, SiteSectionWire };
