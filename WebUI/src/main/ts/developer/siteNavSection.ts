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
 * Pure helpers for Developer Sites navigation sections
 * (#4918 add, #4919 rename, #4920 delete, #4956 same-parent reorder).
 * Moving a section under a different parent stays outside this surface.
 */

import {
  applyTitleToProperties,
  buildSiblingReorderMove,
  canCreateChildUnder,
  mapCreateSectionDialogToFields,
  resolveCreateFolderPath,
} from "../api/architecture/sectionMutations";
import type {
  CreateSiteSectionFields,
  MoveSiteSectionFields,
  NavTreeNode,
  SiteSectionPropertiesWire,
} from "../api/architecture/types";
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

/** A regular or blog section the operator may rename (not a link). */
export interface DeveloperNavRenameTarget {
  id: string;
  title: string;
  siteRoot: boolean;
}

const RENAMEABLE_TYPES = new Set(["section", "blog"]);

/** Depth-first rename targets, including the site root section. */
export function listDeveloperRenameTargets(root: NavTreeNode | null): DeveloperNavRenameTarget[] {
  if (!root) {
    return [];
  }
  const targets: DeveloperNavRenameTarget[] = [];
  const walk = (node: NavTreeNode, siteRoot: boolean): void => {
    const type = String(node.sectionType || "").toLowerCase();
    if (node.id && RENAMEABLE_TYPES.has(type)) {
      targets.push({
        id: node.id,
        title: (node.title || node.id).trim(),
        siteRoot,
      });
    }
    for (const child of node.children || []) {
      walk(child, false);
    }
  };
  walk(root, true);
  return targets;
}

/**
 * True when another node already uses this display name (case-insensitive).
 * The section being renamed is excluded.
 */
export function isDeveloperSectionNameTaken(
  root: NavTreeNode | null,
  sectionId: string,
  name: string,
): boolean {
  const want = name.trim().toLowerCase();
  if (!want || !root) {
    return false;
  }
  let taken = false;
  const walk = (node: NavTreeNode): void => {
    if (taken) {
      return;
    }
    const title = (node.title || "").trim().toLowerCase();
    if (node.id !== sectionId && title === want) {
      taken = true;
      return;
    }
    for (const child of node.children || []) {
      walk(child);
    }
  };
  walk(root);
  return taken;
}

/** A regular or blog section the operator may delete (not the site root, not a link). */
export interface DeveloperNavDeleteTarget {
  id: string;
  title: string;
}

/**
 * Depth-first delete targets. The site root is excluded so this action cannot
 * remove the site. External links and section links are excluded.
 */
export function listDeveloperDeleteTargets(root: NavTreeNode | null): DeveloperNavDeleteTarget[] {
  return listDeveloperRenameTargets(root)
    .filter((target) => !target.siteRoot)
    .map((target) => ({ id: target.id, title: target.title }));
}

/** Same-parent reorder targets. The site root and links are not offered. */
export function listDeveloperReorderTargets(root: NavTreeNode | null): DeveloperNavDeleteTarget[] {
  return listDeveloperDeleteTargets(root);
}

/**
 * One-step same-parent reorder for a non-root section.
 * Returns null for the site root, a link, a missing id, or a step past either end.
 */
export function buildDeveloperSiblingReorder(
  root: NavTreeNode | null,
  sectionId: string,
  direction: "up" | "down",
): MoveSiteSectionFields | null {
  const id = sectionId.trim();
  if (!root || !id || id === root.id) {
    return null;
  }
  if (!listDeveloperReorderTargets(root).some((target) => target.id === id)) {
    return null;
  }
  return buildSiblingReorderMove(root, id, direction);
}

/**
 * Apply a Developer rename onto loaded section properties.
 * Only the display title changes. The folder segment stays so the section is
 * not moved and the update does not lock the folder row while the navon
 * title is saved.
 */
export function buildDeveloperRenameProperties(
  props: SiteSectionPropertiesWire,
  name: string,
): SiteSectionPropertiesWire {
  return applyTitleToProperties(props, name.trim());
}
