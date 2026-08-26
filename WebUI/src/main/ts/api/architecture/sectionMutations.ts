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
  CreateSectionFromFolderFields,
  CreateSiteSectionFields,
  MoveSiteSectionFields,
  NavTreeNode,
  ReplaceLandingPageFields,
  ReplaceLandingPageResult,
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
 * Signed Navigation support for blog-typed navons (#3351).
 * This surface is <strong>read-only</strong> for blog type: badge + structure
 * parent / delete / move. It is not a blog editor.
 *
 * <p>Create or edit a blog section with the existing
 * {@code POST /sitemanage/section} API ({@code sectionType=blog}) from the
 * Home dashboard <strong>Blogs</strong> gadget. Write posts from Home →
 * Create. Do not invent a second blog authoring product here.</p>
 */
export const BLOG_NAVON_NAVIGATION_SUPPORT = "read-only" as const;

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
 * Rename in Navigation applies to regular sections only.
 * Blogs, section links, and external links are not renamed here (#3351).
 */
export function canRenameNavNode(node: NavTreeNode | null): boolean {
  if (!node) return false;
  return String(node.sectionType || "section").toLowerCase() === "section";
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
 * Resolve the nav node created under {@code parentId} after a tree reload.
 *
 * Prefers a POST-returned id when it exists in {@code nextRoot} and is not
 * the site root. Otherwise diffs the parent's children (optional title hint)
 * so Create can auto-select when the create payload omits an id (#3821).
 */
export function resolveCreatedNavNodeId(
  previousRoot: NavTreeNode | null | undefined,
  nextRoot: NavTreeNode | null | undefined,
  parentId: string,
  hint?: { id?: string | null; title?: string | null },
): string | null {
  if (!nextRoot) {
    return null;
  }
  const hintId = hint?.id != null ? String(hint.id).trim() : "";
  if (
    hintId &&
    findNavNodeById(nextRoot, hintId) &&
    !isRootNavNode(nextRoot, hintId)
  ) {
    return hintId;
  }
  const parentKey = parentId.trim();
  if (!parentKey) {
    return null;
  }
  const nextParent = findNavNodeById(nextRoot, parentKey);
  if (!nextParent) {
    return null;
  }
  const prevParent = previousRoot
    ? findNavNodeById(previousRoot, parentKey)
    : null;
  const prevIds = new Set((prevParent?.children ?? []).map((c) => c.id));
  const added = nextParent.children.filter((c) => !prevIds.has(c.id));
  const hintTitle =
    hint?.title != null ? String(hint.title).trim().toLowerCase() : "";
  const pickNonRoot = (id: string): string | null =>
    isRootNavNode(nextRoot, id) ? null : id;
  if (hintTitle) {
    const byTitle = added.find(
      (c) => c.title.trim().toLowerCase() === hintTitle,
    );
    if (byTitle) {
      return pickNonRoot(byTitle.id);
    }
    const amongChildren = nextParent.children.find(
      (c) => c.title.trim().toLowerCase() === hintTitle,
    );
    if (amongChildren) {
      return pickNonRoot(amongChildren.id);
    }
  }
  if (added.length === 1) {
    return pickNonRoot(added[0].id);
  }
  return null;
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
 * Whether the selected node may be reparented via the Move Section picker.
 * Root cannot be moved; any other navon (section, link, blog) can.
 */
export function canMoveNavNode(
  root: NavTreeNode | null | undefined,
  node: NavTreeNode | null,
): boolean {
  if (!root || !node) return false;
  return !isRootNavNode(root, node.id);
}

/**
 * True when {@code maybeDescendantId} is {@code ancestorId} or a descendant
 * of that node (depth-first).
 */
export function isNavNodeInSubtree(
  root: NavTreeNode | null | undefined,
  ancestorId: string,
  maybeDescendantId: string,
): boolean {
  const ancestor = findNavNodeById(root, ancestorId);
  if (!ancestor || !maybeDescendantId) return false;
  return findNavNodeById(ancestor, maybeDescendantId) != null;
}

/**
 * Drop {@code excludeId} and its descendants from a tree copy (picker filter).
 */
export function omitNavSubtree(
  root: NavTreeNode | null,
  excludeId: string | null | undefined,
): NavTreeNode | null {
  const skip = excludeId != null ? excludeId.trim() : "";
  if (!root || !skip) return root;
  if (root.id === skip) return null;
  const children: NavTreeNode[] = [];
  for (const child of root.children) {
    const kept = omitNavSubtree(child, skip);
    if (kept) {
      children.push(kept);
    }
  }
  return { ...root, children };
}

/**
 * Whether {@code targetParentId} may receive {@code sourceId} as a child.
 * Rejects missing ids, moving the root, self-parent, descendants of the
 * source (cycle), and non-section parents (links cannot host children).
 */
export function isValidMoveTargetParent(
  root: NavTreeNode | null | undefined,
  sourceId: string,
  targetParentId: string,
): boolean {
  const source = sourceId.trim();
  const target = targetParentId.trim();
  if (!root || !source || !target) return false;
  if (source === target) return false;
  if (isRootNavNode(root, source)) return false;
  const sourceNode = findNavNodeById(root, source);
  const targetNode = findNavNodeById(root, target);
  if (!sourceNode || !targetNode) return false;
  if (!canCreateChildUnder(targetNode)) return false;
  if (findNavNodeById(sourceNode, target)) return false;
  return true;
}

/**
 * Sibling insert slots under {@code target} (excluding the moving node).
 * {@code targetIndex} {@code -1} means append (CM1 {@code POST /section/move}).
 */
export function listMoveTargetPositions(
  target: NavTreeNode | null,
  sourceId: string,
): { targetIndex: number; beforeId: string | null; beforeTitle: string | null }[] {
  const source = sourceId.trim();
  if (!target) {
    return [{ targetIndex: -1, beforeId: null, beforeTitle: null }];
  }
  const slots: {
    targetIndex: number;
    beforeId: string | null;
    beforeTitle: string | null;
  }[] = [];
  target.children.forEach((child, index) => {
    if (child.id === source) return;
    slots.push({
      targetIndex: index,
      beforeId: child.id,
      beforeTitle: child.title,
    });
  });
  slots.push({ targetIndex: -1, beforeId: null, beforeTitle: null });
  return slots;
}

/**
 * Build move payload for reparent (or same-parent insert) via the picker.
 * {@code targetIndex} {@code -1} appends under the target (CM1 parity).
 * Same-parent inserts after the source apply the CM1 index adjustment.
 */
export function buildReparentMove(
  root: NavTreeNode | null | undefined,
  sourceId: string,
  targetParentId: string,
  targetIndex: number = -1,
): MoveSiteSectionFields | null {
  const source = sourceId.trim();
  const target = targetParentId.trim();
  if (!isValidMoveTargetParent(root, source, target)) {
    return null;
  }
  const place = findSiblingPlacement(root, source);
  if (!place) return null;
  if (
    typeof targetIndex !== "number" ||
    Number.isNaN(targetIndex) ||
    targetIndex < -1
  ) {
    return null;
  }
  let index = targetIndex;
  if (place.parent.id === target && targetIndex >= 0 && place.index < targetIndex) {
    index = targetIndex - 1;
  }
  return {
    sourceId: source,
    targetId: target,
    sourceParentId: place.parent.id,
    targetIndex: index,
  };
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
 * Folder path for {@code POST /section/create}. Prefers the tree node's
 * {@code folderPath}, then a freshly loaded section path (the nav tree wire
 * often omits folderPath), then the site-name fallback.
 */
export function resolveCreateFolderPath(
  parent: NavTreeNode | null,
  loadedFolderPath: string | null | undefined,
  siteName: string,
): string {
  const fromParent = parent?.folderPath != null ? parent.folderPath.trim() : "";
  if (fromParent) {
    return toRepositoryCmsPath(fromParent);
  }
  const fromLoaded = loadedFolderPath != null ? loadedFolderPath.trim() : "";
  if (fromLoaded) {
    return toRepositoryCmsPath(fromLoaded);
  }
  return resolveCreateParentFolderPath(parent, siteName);
}

/**
 * Dialog fields for Architecture Create section (landing page + template).
 */
export interface CreateSectionDialogFields {
  title: string;
  urlName: string;
  pageName: string;
  templateId: string;
}

/**
 * Map New Section dialog fields onto {@code CreateSiteSection} wire fields.
 * {@code pageName} is the landing-page file name (not the folder URL).
 */
export function mapCreateSectionDialogToFields(
  input: CreateSectionDialogFields,
  folderPath: string,
): CreateSiteSectionFields {
  return {
    pageTitle: input.title.trim(),
    pageLinkTitle: input.title.trim(),
    pageName: input.pageName.trim(),
    pageUrlIdentifier: input.urlName.trim(),
    templateId: input.templateId.trim(),
    folderPath,
    sectionType: "section",
    copyTemplates: true,
    target: "_self",
  };
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
  const { folderPermission, ...rest } = props;
  const body: SiteSectionPropertiesWire = {
    ...rest,
    id: props.id.trim(),
    title: props.title.trim(),
    folderName: props.folderName.trim(),
    target: props.target ?? "_self",
  };
  if (folderPermission != null) {
    body.folderPermission = folderPermission;
  }
  return {
    SiteSectionProperties: body,
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
 * Regular section (including site root) may open the section-properties editor.
 * Section links, external links, and blogs use other editors.
 */
export function canEditSectionProperties(node: NavTreeNode | null): boolean {
  return canReplaceLandingPage(node);
}

/** Form fields for the section-properties dialog (CM1 configure parity). */
export interface SectionPropertiesFormValues {
  title: string;
  folderName: string;
  target: string;
  cssClassNames: string;
  requiresLogin: boolean;
  allowAccessTo: string;
}

/**
 * Login checkbox is editable only on a secure site when no ancestor already
 * requires login (CM1 {@code perc_editSectionDialog} rule).
 */
export function canToggleRequiresLogin(
  props: Pick<
    SiteSectionPropertiesWire,
    "secureSite" | "secureAncestor"
  > | null,
): boolean {
  if (!props) return false;
  return Boolean(props.secureSite) && !props.secureAncestor;
}

/** CSS class tokens: letters, digits, hyphen, underscore, spaces. Max 255. */
const CSS_CLASS_NAMES_RE = /^[A-Za-z0-9_\- ]*$/;

/**
 * Validate nav-widget CSS class names (legacy edit-section dialog).
 */
export function validateCssClassNames(value: string): string | null {
  const t = value.replace(/ +/g, " ").trim();
  if (t.length > 255) {
    return message(
      "perc.ui.architecture.modern@CSS classes are too long (max 255 characters)",
    );
  }
  if (t && !CSS_CLASS_NAMES_RE.test(t)) {
    return message(
      "perc.ui.architecture.modern@CSS classes may only contain letters, numbers, dash, underscore, and spaces",
    );
  }
  return null;
}

/**
 * Validate the section-properties form before POST.
 * Returns the first error or {@code null} when valid.
 */
export function validateSectionPropertiesForm(
  form: SectionPropertiesFormValues,
  options?: { folderNameLocked?: boolean },
): string | null {
  const titleErr = validateSectionTitle(form.title);
  if (titleErr) return titleErr;
  if (!options?.folderNameLocked) {
    const folderErr = validateSectionFolderName(form.folderName);
    if (folderErr) return folderErr;
  }
  return validateCssClassNames(form.cssClassNames);
}

/**
 * Merge operator-edited form fields onto loaded properties.
 * Preserves {@code folderPermission} and security flags. Root folder name
 * is not rewritten (site folder). Login/groups follow CM1 enablement.
 */
export function applySectionPropertiesForm(
  props: SiteSectionPropertiesWire,
  form: SectionPropertiesFormValues,
): SiteSectionPropertiesWire {
  const folderNameLocked = Boolean(props.siteRootSection);
  const folderName = folderNameLocked
    ? props.folderName
    : form.folderName.trim();
  const loginEditable = canToggleRequiresLogin(props);
  const requiresLogin = loginEditable
    ? Boolean(form.requiresLogin)
    : Boolean(props.requiresLogin);
  let allowAccessTo: string | null;
  if (!requiresLogin) {
    allowAccessTo = "";
  } else if (loginEditable) {
    allowAccessTo = form.allowAccessTo.trim();
  } else {
    allowAccessTo = props.allowAccessTo != null ? String(props.allowAccessTo) : "";
  }
  const css = form.cssClassNames.replace(/ +/g, " ").trim();
  return {
    ...props,
    title: form.title.trim(),
    folderName,
    target: form.target?.trim() || "_self",
    cssClassNames: css.length > 0 ? css : "",
    requiresLogin,
    allowAccessTo,
  };
}

/**
 * Regular non-root navon may be converted to a plain folder.
 * Root, blogs, section links, and external links are blocked (matches docs).
 */
export function canConvertSectionToFolder(
  root: NavTreeNode | null | undefined,
  node: NavTreeNode | null,
): boolean {
  if (!root || !node) return false;
  if (isRootNavNode(root, node.id)) return false;
  const t = String(node.sectionType || "section").toLowerCase();
  return t === "section";
}

/**
 * Validate a landing page file name used by create-from-folder.
 * Returns an error message or {@code null} when valid.
 */
export function validateLandingPageName(name: string): string | null {
  const t = name.trim();
  if (!t) {
    return message("perc.ui.architecture.modern@Landing page name is required");
  }
  if (t.length > 255) {
    return message(
      "perc.ui.architecture.modern@Landing page name is too long (max 255 characters)",
    );
  }
  if (t.includes("/") || t.includes("\\")) {
    return message(
      "perc.ui.architecture.modern@Landing page name must be a file name, not a path",
    );
  }
  return null;
}

/**
 * Client-side validation for Create section (no POST on empty/invalid).
 * Returns the first error message or {@code null} when valid.
 */
export function validateCreateSectionForm(
  input: CreateSectionDialogFields,
): string | null {
  const titleErr = validateSectionTitle(input.title);
  if (titleErr) return titleErr;
  const urlErr = validateSectionFolderName(input.urlName);
  if (urlErr) return urlErr;
  const pageErr = validateLandingPageName(input.pageName);
  if (pageErr) return pageErr;
  if (!input.templateId.trim()) {
    return message(
      "perc.ui.architecture.modern@No templates are available for this site.",
    );
  }
  return null;
}

/**
 * Validate a source folder path for create-from-folder.
 */
export function validateSourceFolderPath(path: string): string | null {
  const t = path.trim();
  if (!t) {
    return message("perc.ui.architecture.modern@Folder path is required");
  }
  const repo = toRepositoryCmsPath(t);
  const parts = repo.split("/").filter(Boolean);
  if (
    parts.length < 3 ||
    parts[0].toLowerCase() !== "sites"
  ) {
    return message(
      "perc.ui.architecture.modern@Folder path must be a site folder under /Sites/",
    );
  }
  return null;
}

/**
 * Split a CMS page path into parent folder + file name.
 * Accepts {@code /Sites/...} or {@code //Sites/...}.
 */
export function splitCmsPagePath(
  path: string,
): { folderPath: string; pageName: string } | null {
  const repo = toRepositoryCmsPath(path.trim());
  if (!repo) return null;
  const trimmed = repo.replace(/\/+$/, "");
  const slash = trimmed.lastIndexOf("/");
  if (slash <= 1) return null;
  const pageName = trimmed.slice(slash + 1).trim();
  const folderPath = trimmed.slice(0, slash);
  if (!pageName || !folderPath) return null;
  return { folderPath, pageName };
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
/** Schemes that must never be stored as external link targets (XSS / drive-by). */
const BLOCKED_EXTERNAL_SCHEMES = /^(javascript|data|vbscript|file)\s*:/i;

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
  // Block dangerous schemes even when they match a generic scheme: pattern.
  // Key is declared in architecture/messages.ts KEYS.VALIDATION_URL_SCHEME_BLOCKED + CmsUi.tmx.
  if (BLOCKED_EXTERNAL_SCHEMES.test(t)) {
    return message(
      "perc.ui.architecture.modern@URL scheme is not allowed (use http(s) or a site path)",
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
 * Unwrap Jackson {@code ReplaceLandingPage} (or a bare object) after POST.
 */
export function parseReplaceLandingPagePayload(
  payload: unknown,
  fallback: ReplaceLandingPageFields,
): ReplaceLandingPageResult {
  const empty: ReplaceLandingPageResult = {
    sectionId: fallback.sectionId.trim(),
    newLandingPageId: fallback.newLandingPageId.trim(),
    newLandingPageName: null,
    oldLandingPageName: null,
  };
  if (payload == null || typeof payload !== "object") {
    return empty;
  }
  const obj = payload as Record<string, unknown>;
  const wrapped = obj.ReplaceLandingPage ?? obj.replaceLandingPage;
  const root = wrapped !== undefined ? wrapped : payload;
  if (root == null || typeof root !== "object" || Array.isArray(root)) {
    return empty;
  }
  const rec = root as Record<string, unknown>;
  const optionalName = (value: unknown): string | null => {
    if (value == null) {
      return null;
    }
    const trimmed = String(value).trim();
    return trimmed ? trimmed : null;
  };
  const sectionId =
    rec.sectionId != null ? String(rec.sectionId).trim() : empty.sectionId;
  const newLandingPageId =
    rec.newLandingPageId != null
      ? String(rec.newLandingPageId).trim()
      : empty.newLandingPageId;
  return {
    sectionId: sectionId || empty.sectionId,
    newLandingPageId: newLandingPageId || empty.newLandingPageId,
    newLandingPageName: optionalName(rec.newLandingPageName),
    oldLandingPageName: optionalName(rec.oldLandingPageName),
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

/**
 * Build Jackson-rooted body for {@code POST /section/createSectionFromFolder}.
 * Sends both historic CM1 XML root and {@code @JsonRootName}.
 */
export function buildCreateSectionFromFolderBody(
  fields: CreateSectionFromFolderFields,
): {
  CreateSectionFromFolderRequest: CreateSectionFromFolderFields;
  PSCreateSectionFromFolderRequest: CreateSectionFromFolderFields;
} {
  const payload: CreateSectionFromFolderFields = {
    sourceFolderPath: toRepositoryCmsPath(fields.sourceFolderPath),
    pageName: fields.pageName.trim(),
    parentFolderPath: toRepositoryCmsPath(fields.parentFolderPath),
  };
  return {
    CreateSectionFromFolderRequest: payload,
    PSCreateSectionFromFolderRequest: payload,
  };
}
