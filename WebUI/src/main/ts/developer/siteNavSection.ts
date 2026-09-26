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
 * Pure helpers for Developer Sites "add a navigation section" (#4918).
 * Reorder and delete stay outside this surface.
 */

import {
  canCreateChildUnder,
  mapCreateSectionDialogToFields,
  resolveCreateFolderPath,
} from "../api/architecture/sectionMutations";
import type { CreateSiteSectionFields, NavTreeNode } from "../api/architecture/types";
import type { SiteDef } from "../api/developer/types";
import { titleToPageFileName } from "../home/create/filenameUtils";

const FOLDER_SEGMENT_RE = /^[A-Za-z0-9._-]+$/;

/** Parent choices that can host a new regular section. */
export interface DeveloperNavParentOption {
  id: string;
  title: string;
  node: NavTreeNode | null;
}

/**
 * Traditional sites with managed navigation off, and Virtual Sites, do not
 * gain a CMS section editor here.
 */
export function isDeveloperNavSectionReadOnly(site: SiteDef): boolean {
  if (site.managedNavigation === false) {
    return true;
  }
  const kind = (site.virtual?.sourceKind ?? "").trim().toLowerCase();
  if (kind && kind !== "repository") {
    return true;
  }
  return site.virtual?.virtual === true;
}

/**
 * Validate the operator-entered section name.
 * @returns an error code {@code empty} or {@code invalid}, or null when usable
 */
export function validateDeveloperSectionName(name: string): "empty" | "invalid" | null {
  const title = name.trim();
  if (!title) {
    return "empty";
  }
  if (title.length > 100) {
    return "invalid";
  }
  const file = titleToPageFileName(title);
  const segment = file.replace(/\.html$/i, "");
  if (!segment || segment.length > 100 || !FOLDER_SEGMENT_RE.test(segment)) {
    return "invalid";
  }
  if (!file.toLowerCase().endsWith(".html") || !FOLDER_SEGMENT_RE.test(file)) {
    return "invalid";
  }
  return null;
}

/** Depth-first titles for the reload list (root included). */
export function listDeveloperSectionTitles(root: NavTreeNode | null): string[] {
  if (!root) {
    return [];
  }
  const titles: string[] = [];
  const walk = (node: NavTreeNode): void => {
    const title = (node.title || "").trim();
    if (title) {
      titles.push(title);
    }
    for (const child of node.children || []) {
      walk(child);
    }
  };
  walk(root);
  return titles;
}

/** Parents that may receive a new section. Falls back to the site root path. */
export function listDeveloperNavParents(
  root: NavTreeNode | null,
  siteName: string,
): DeveloperNavParentOption[] {
  if (!root) {
    const label = siteName.trim() || "Site";
    return [{ id: "", title: label, node: null }];
  }
  const options: DeveloperNavParentOption[] = [];
  const walk = (node: NavTreeNode): void => {
    if (canCreateChildUnder(node)) {
      options.push({
        id: node.id,
        title: (node.title || node.id || siteName).trim(),
        node,
      });
    }
    for (const child of node.children || []) {
      walk(child);
    }
  };
  walk(root);
  if (options.length === 0) {
    return [{ id: "", title: siteName.trim() || "Site", node: null }];
  }
  return options;
}

/**
 * Map name + parent onto the existing {@code CreateSiteSection} body fields.
 * URL segment and landing page file are derived from the name.
 */
export function buildDeveloperAddSectionFields(opts: {
  name: string;
  siteName: string;
  parent: NavTreeNode | null;
  loadedFolderPath?: string | null;
  templateId: string;
}): CreateSiteSectionFields {
  const title = opts.name.trim();
  const pageName = titleToPageFileName(title);
  const urlName = pageName.replace(/\.html$/i, "");
  const folderPath = resolveCreateFolderPath(
    opts.parent,
    opts.loadedFolderPath,
    opts.siteName,
  );
  return mapCreateSectionDialogToFields(
    {
      title,
      urlName,
      pageName,
      templateId: opts.templateId.trim(),
    },
    folderPath,
  );
}
