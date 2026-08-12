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
 * Pure helpers for Architecture nav structure mutations (#3096 / #3097).
 *
 * <p>No I/O. Builds REST payload wrappers and resolves parent/sibling placement
 * for create / rename / reorder / delete / landing / links. Safe for Vitest
 * without fetch.</p>
 */

import { toRepositoryCmsPath } from "../../home/create/filenameUtils";
import { message } from "../../i18n/message";
import type {
  CreateExternalLinkFields,
  CreateSiteSectionFields,
  MoveSiteSectionFields,
  NavTreeNode,
  ReplaceLandingPageFields,
  SiblingPlacement,
  SiteSectionPropertiesWire,
  SiteSectionWire,
  UpdateSectionLinkFields,
} from "./types";

/** Windows-ish folder / URL segment: letters, digits, dash, underscore, period. */
const FOLDER_NAME_RE = /^[A-Za-z0-9._-]+$/;

/**
 * Validate a section folder / URL name (legacy new-section dialog rules).
 * Returns an error message or {@code null} when valid.
 */
export function validateSectionFolderName(name: string): string | null {
  const t = name.trim();
  if (!t) {
    return message("perc.ui.architecture.modern@URL name is required");
  }
  if (t.length > 100) {
    return message(
      "perc.ui.architecture.modern@URL name is too long (max 100 characters)",
    );
  }
  if (!FOLDER_NAME_RE.test(t)) {
    return message(
      "perc.ui.architecture.modern@URL name may only contain letters, numbers, dash, underscore, and period",
    );
  }
  return null;
}

/**
 * Validate a display title for create / rename.
 * Returns an error message or {@code null} when valid.
 */
export function validateSectionTitle(title: string): string | null {
  const t = title.trim();
  if (!t) {
    return message("perc.ui.architecture.modern@Title is required");
  }
  if (t.length > 512) {
    return message(
      "perc.ui.architecture.modern@Title is too long (max 512 characters)",
    );
  }
  return null;
}

/** Find a node by id (depth-first). */
export function findNavNodeById(
  root: NavTreeNode | null | undefined,
  id: string,
): NavTreeNode | null {
  if (!root || !id) return null;
  if (root.id === id) return root;
  for (const child of root.children) {
    const found = findNavNodeById(child, id);
    if (found) return found;
  }
  return null;
}

/**
 * Locate the parent of {@code id} and the index among siblings.
 * Returns {@code null} when the node is missing or is the root.
 */
export function findSiblingPlacement(
  root: NavTreeNode | null | undefined,
  id: string,
): SiblingPlacement | null {
  if (!root || !id || root.id === id) return null;
  const walk = (parent: NavTreeNode): SiblingPlacement | null => {
    const idx = parent.children.findIndex((c) => c.id === id);
    if (idx >= 0) {
      return { parent, index: idx, siblings: parent.children };
    }
    for (const child of parent.children) {
      const nested = walk(child);
      if (nested) return nested;
    }
    return null;
  };
  return walk(root);
}

/** True when the node is the tree root (no parent). */
export function isRootNavNode(
  root: NavTreeNode | null | undefined,
  id: string,
): boolean {
  return !!root && root.id === id;
}

/**
 * Regular section (or blog) may host children; section/external links may not
 * be create-parents in this slice.
 */
export function canCreateChildUnder(node: NavTreeNode | null): boolean {
  if (!node) return false;
  const t = String(node.sectionType || "section").toLowerCase();
  return t === "section" || t === "blog";
}

/**
 * Whether delete is allowed for the selected node.
 * Root is never deleted from Architecture structure chrome.
 */
export function canDeleteNavNode(
  root: NavTreeNode | null | undefined,
  node: NavTreeNode | null,
): boolean {
  if (!root || !node) return false;
  if (isRootNavNode(root, node.id)) return false;
  return true;
}

/**
 * Whether the node can move up among siblings (same parent).
 */
export function canMoveNavNodeUp(
  root: NavTreeNode | null | undefined,
  id: string,
): boolean {
  const place = findSiblingPlacement(root, id);
  return place != null && place.index > 0;
}

/**
 * Whether the node can move down among siblings (same parent).
 */
export function canMoveNavNodeDown(
  root: NavTreeNode | null | undefined,
  id: string,
): boolean {
  const place = findSiblingPlacement(root, id);
  return place != null && place.index < place.siblings.length - 1;
}

/**
 * Parent folder path for creating a child under {@code parent}.
 * Prefers the parent's {@code folderPath}; falls back to {@code //Sites/{site}}.
 */
export function resolveCreateParentFolderPath(
  parent: NavTreeNode | null,
  siteName: string,
): string {
  const raw =
    parent?.folderPath != null && parent.folderPath.trim().length > 0
      ? parent.folderPath.trim()
      : `/Sites/${siteName.trim()}`;
  return toRepositoryCmsPath(raw);
}

/**
 * Build Jackson-rooted create body for {@code POST /section/create}.
 */
export function buildCreateSiteSectionBody(
  fields: CreateSiteSectionFields,
): { CreateSiteSection: CreateSiteSectionFields } {
  return {
    CreateSiteSection: {
      pageTitle: fields.pageTitle.trim(),
      pageLinkTitle: (fields.pageLinkTitle || fields.pageTitle).trim(),
      pageName: fields.pageName.trim(),
      pageUrlIdentifier: (
        fields.pageUrlIdentifier || fields.pageName
      ).trim(),
      templateId: fields.templateId.trim(),
      folderPath: toRepositoryCmsPath(fields.folderPath),
      sectionType: fields.sectionType ?? "section",
      target: fields.target ?? "_self",
      copyTemplates:
        fields.copyTemplates === undefined ? true : fields.copyTemplates,
      ...(fields.blogPostTemplateId
        ? { blogPostTemplateId: fields.blogPostTemplateId }
        : {}),
    },
  };
}

/**
 * Build Jackson-rooted update body for {@code POST /section/update}.
 */
export function buildUpdateSiteSectionBody(
  props: SiteSectionPropertiesWire,
): { SiteSectionProperties: SiteSectionPropertiesWire } {
  return {
    SiteSectionProperties: {
      ...props,
      id: props.id.trim(),
      title: props.title.trim(),
      folderName: props.folderName.trim(),
      target: props.target ?? "_self",
    },
  };
}

/**
 * Build Jackson-rooted move body for {@code POST /section/move}.
 */
export function buildMoveSiteSectionBody(
  fields: MoveSiteSectionFields,
): { MoveSiteSection: MoveSiteSectionFields } {
  return {
    MoveSiteSection: {
      sourceId: fields.sourceId.trim(),
      targetId: fields.targetId.trim(),
      targetIndex: fields.targetIndex,
      ...(fields.sourceParentId
        ? { sourceParentId: fields.sourceParentId.trim() }
        : {}),
    },
  };
}

/**
 * Compute move payload for shifting a node one step among siblings.
 * @param direction {@code "up"} decreases index; {@code "down"} increases.
 * @returns move fields or {@code null} when the move is not possible.
 */
export function buildSiblingReorderMove(
  root: NavTreeNode | null | undefined,
  id: string,
  direction: "up" | "down",
): MoveSiteSectionFields | null {
  const place = findSiblingPlacement(root, id);
  if (!place) return null;
  const nextIndex = direction === "up" ? place.index - 1 : place.index + 1;
  if (nextIndex < 0 || nextIndex >= place.siblings.length) {
    return null;
  }
  return {
    sourceId: id,
    targetId: place.parent.id,
    sourceParentId: place.parent.id,
    targetIndex: nextIndex,
  };
}

/**
 * Apply a title rename onto loaded properties (keeps folderName unless overridden).
 */
export function applyTitleToProperties(
  props: SiteSectionPropertiesWire,
  newTitle: string,
  newFolderName?: string,
): SiteSectionPropertiesWire {
  return {
    ...props,
    title: newTitle.trim(),
    folderName:
      newFolderName != null && newFolderName.trim().length > 0
        ? newFolderName.trim()
        : props.folderName,
  };
}

/**
 * Unwrap {@code SiteSectionProperties} / plain object from GET properties payload.
 */
export function parseSiteSectionPropertiesPayload(
  payload: unknown,
): SiteSectionPropertiesWire | null {
  if (payload == null || typeof payload !== "object") {
    return null;
  }
  const obj = payload as Record<string, unknown>;
  const root =
    (obj.SiteSectionProperties as unknown) ??
    (obj.siteSectionProperties as unknown) ??
    payload;
  if (root == null || typeof root !== "object" || Array.isArray(root)) {
    return null;
  }
  const rec = root as Record<string, unknown>;
  const id = rec.id != null ? String(rec.id).trim() : "";
  const title = rec.title != null ? String(rec.title) : "";
  const folderName = rec.folderName != null ? String(rec.folderName) : "";
  if (!id) {
    return null;
  }
  return {
    id,
    title,
    folderName,
    target: rec.target != null ? String(rec.target) : "_self",
    requiresLogin: rec.requiresLogin === true,
    allowAccessTo:
      rec.allowAccessTo != null ? String(rec.allowAccessTo) : null,
    cssClassNames:
      rec.cssClassNames != null ? String(rec.cssClassNames) : null,
    secureSite: rec.secureSite === true,
    secureAncestor: rec.secureAncestor === true,
    siteRootSection: rec.siteRootSection === true,
    folderPermission: rec.folderPermission ?? null,
  };
}

/**
 * Whether the section type uses the section-link delete endpoint.
 */
export function isSectionLinkType(sectionType: string | null | undefined): boolean {
  return String(sectionType || "").toLowerCase() === "sectionlink";
}

/** External-link section type. */
export function isExternalLinkType(
  sectionType: string | null | undefined,
): boolean {
  return String(sectionType || "").toLowerCase() === "externallink";
}

/** Blog section type (structure-visible; full blog UX deferred). */
export function isBlogSectionType(
  sectionType: string | null | undefined,
): boolean {
  return String(sectionType || "").toLowerCase() === "blog";
}

/**
 * Regular section (including site root) may have its landing page replaced.
 * Section links / external links / blogs are not landing-page hosts in this slice.
 */
export function canReplaceLandingPage(node: NavTreeNode | null): boolean {
  if (!node) return false;
  const t = String(node.sectionType || "section").toLowerCase();
  return t === "section";
}

/**
 * Whether the selected node can open the edit-link dialog (section or external).
 */
export function canEditLinkNode(node: NavTreeNode | null): boolean {
  if (!node) return false;
  return (
    isSectionLinkType(node.sectionType) || isExternalLinkType(node.sectionType)
  );
}

/**
 * Whether {@code targetId} may be a section-link target under {@code parentId}.
 * Rejects missing ids, self-parent, and when the target is already a
 * <strong>direct</strong> child of the parent (CM1 {@code isChild} / duplicate
 * section guard — not full subtree).
 */
export function isValidSectionLinkTarget(
  root: NavTreeNode | null | undefined,
  parentId: string,
  targetId: string,
): boolean {
  const parent = parentId.trim();
  const target = targetId.trim();
  if (!root || !parent || !target) return false;
  if (parent === target) return false;
  if (!findNavNodeById(root, target)) return false;
  const parentNode = findNavNodeById(root, parent);
  if (!parentNode) return false;
  // CM1 isChild: only direct children count as duplicates
  if (parentNode.children.some((c) => c.id === target)) {
    return false;
  }
  return true;
}

/**
 * Validate external link URL (non-empty; basic scheme or path).
 * Returns error message or {@code null} when valid.
 */
export function validateExternalUrl(url: string): string | null {
  const t = url.trim();
  if (!t) {
    return message("perc.ui.architecture.modern@URL is required");
  }
  if (t.length > 2048) {
    return message(
      "perc.ui.architecture.modern@URL is too long (max 2048 characters)",
    );
  }
  // Accept absolute http(s), protocol-relative, or site-relative paths
  if (
    /^https?:\/\//i.test(t) ||
    t.startsWith("//") ||
    t.startsWith("/") ||
    t.startsWith("#") ||
    /^[a-z][a-z0-9+.-]*:/i.test(t)
  ) {
    return null;
  }
  // Also allow bare hostnames / relative paths used in CM1
  if (/^[\w./?#&=%-]+$/i.test(t)) {
    return null;
  }
  return message(
    "perc.ui.architecture.modern@Enter a valid URL (for example https://example.com or /path)",
  );
}

/**
 * Build Jackson-rooted body for {@code POST /section/replaceLandingPage}.
 */
export function buildReplaceLandingPageBody(
  fields: ReplaceLandingPageFields,
): { ReplaceLandingPage: ReplaceLandingPageFields } {
  return {
    ReplaceLandingPage: {
      sectionId: fields.sectionId.trim(),
      newLandingPageId: fields.newLandingPageId.trim(),
    },
  };
}

/**
 * Build Jackson-rooted body for create / update external link.
 */
export function buildCreateExternalLinkBody(
  fields: CreateExternalLinkFields,
): { CreateExternalLinkSection: CreateExternalLinkFields } {
  return {
    CreateExternalLinkSection: {
      externalUrl: fields.externalUrl.trim(),
      linkTitle: fields.linkTitle.trim(),
      folderPath: toRepositoryCmsPath(fields.folderPath),
      sectionType: fields.sectionType ?? "externallink",
      target: fields.target ?? "_self",
      ...(fields.cssClassNames != null && String(fields.cssClassNames).trim()
        ? { cssClassNames: String(fields.cssClassNames).trim() }
        : {}),
    },
  };
}

/**
 * Build Jackson-rooted body for {@code POST /section/updateSectionLink}.
 */
export function buildUpdateSectionLinkBody(
  fields: UpdateSectionLinkFields,
): { UpdateSectionLink: UpdateSectionLinkFields } {
  return {
    UpdateSectionLink: {
      oldSectionId: fields.oldSectionId.trim(),
      newSectionId: fields.newSectionId.trim(),
      parentSectionId: fields.parentSectionId.trim(),
    },
  };
}

/**
 * Unwrap {@code SiteSection} / plain object from GET section payload.
 */
export function parseSiteSectionPayload(
  payload: unknown,
): SiteSectionWire | null {
  if (payload == null || typeof payload !== "object") {
    return null;
  }
  const obj = payload as Record<string, unknown>;
  const root =
    (obj.SiteSection as unknown) ??
    (obj.siteSection as unknown) ??
    payload;
  if (root == null || typeof root !== "object" || Array.isArray(root)) {
    return null;
  }
  const rec = root as Record<string, unknown>;
  const id = rec.id != null ? String(rec.id).trim() : "";
  if (!id) {
    return null;
  }
  return {
    id,
    title: rec.title != null ? String(rec.title) : "",
    folderPath: rec.folderPath != null ? String(rec.folderPath) : null,
    sectionType:
      rec.sectionType != null ? String(rec.sectionType) : "section",
    externalLinkUrl:
      rec.externalLinkUrl != null ? String(rec.externalLinkUrl) : null,
    target: rec.target != null ? String(rec.target) : "_self",
    requiresLogin: rec.requiresLogin === true,
    allowAccessTo:
      rec.allowAccessTo != null ? String(rec.allowAccessTo) : null,
    cssClassNames:
      rec.cssClassNames != null ? String(rec.cssClassNames) : null,
  };
}

/**
 * Resolve section-link create path segment pair for GET mutation.
 * Returns encoded path suffix {@code target/parent} or {@code null}.
 */
export function buildCreateSectionLinkPath(
  targetSectionId: string,
  parentSectionId: string,
): string | null {
  const target = targetSectionId.trim();
  const parent = parentSectionId.trim();
  if (!target || !parent) return null;
  return `${encodeURIComponent(target)}/${encodeURIComponent(parent)}`;
}
