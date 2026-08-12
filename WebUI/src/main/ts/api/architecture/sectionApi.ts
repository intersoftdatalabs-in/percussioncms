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
 * Typed section REST client for Architecture nav tree
 * (#3095 / #3096 / #3097).
 *
 * <p>Read: {@code GET /sitemanage/section/tree/{siteName}}.
 * Mutations: create / properties / update / move / delete / landing / links.</p>
 */

import { del, extractRestErrorMessage, get, isApiError, post } from "../client";
import { PATHS } from "../paths";
import {
  isEmptySectionTreeWire,
  mapSectionNodeToTree,
  parseSectionNodePayload,
} from "./mapSectionTree";
import {
  buildCreateExternalLinkBody,
  buildCreateSectionLinkPath,
  buildCreateSiteSectionBody,
  buildMoveSiteSectionBody,
  buildReplaceLandingPageBody,
  buildUpdateSectionLinkBody,
  buildUpdateSiteSectionBody,
  parseSiteSectionPayload,
  parseSiteSectionPropertiesPayload,
} from "./sectionMutations";
import type {
  CreateExternalLinkFields,
  CreateSiteSectionFields,
  MoveSiteSectionFields,
  NavTreeNode,
  ReplaceLandingPageFields,
  SiteSectionPropertiesWire,
  SiteSectionWire,
  UpdateSectionLinkFields,
} from "./types";

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

/** Build properties URL for a section id (exported for tests). */
export function sectionPropertiesUrl(sectionId: string): string {
  const key = encodeURIComponent(sectionId.trim());
  return `${PATHS.SECTION_PROPERTIES}/${key}`;
}

/** Build delete URL for a section id (exported for tests). */
export function sectionDeleteUrl(sectionId: string): string {
  const key = encodeURIComponent(sectionId.trim());
  return `${PATHS.SECTION}/${key}`;
}

/** Build convert-to-folder URL (exported for tests). */
export function sectionConvertToFolderUrl(sectionId: string): string {
  const key = encodeURIComponent(sectionId.trim());
  return `${PATHS.SECTION_CONVERT_TO_FOLDER}/${key}`;
}

/** Build delete-section-link URL (exported for tests). */
export function sectionDeleteLinkUrl(
  sectionId: string,
  parentId: string,
): string {
  const sec = encodeURIComponent(sectionId.trim());
  const parent = encodeURIComponent(parentId.trim());
  return `${PATHS.SECTION_DELETE_SECTION_LINK}/${sec}/${parent}`;
}

/** Build create-section-link URL (exported for tests). */
export function sectionCreateLinkUrl(
  targetSectionId: string,
  parentSectionId: string,
): string {
  const path = buildCreateSectionLinkPath(targetSectionId, parentSectionId);
  if (!path) {
    throw new Error("Target and parent section ids are required");
  }
  return `${PATHS.SECTION_CREATE_SECTION_LINK}/${path}`;
}

/** Build update-external-link URL (exported for tests). */
export function sectionUpdateExternalLinkUrl(sectionId: string): string {
  const key = encodeURIComponent(sectionId.trim());
  return `${PATHS.SECTION_UPDATE_EXTERNAL_LINK}/${key}`;
}

/** Build load-one-section URL (exported for tests). */
export function sectionLoadUrl(sectionId: string): string {
  const key = encodeURIComponent(sectionId.trim());
  return `${PATHS.SECTION}/${key}`;
}

/**
 * True when the API failure is a missing/empty NavTree (operator empty
 * state), not a hard tree-load failure (#3218).
 */
export function isMissingNavTreeError(err: unknown): boolean {
  const chunks: string[] = [];
  if (err instanceof Error && err.message) {
    chunks.push(err.message);
  }
  if (isApiError(err)) {
    chunks.push(String(err.statusText || ""));
    const fromBody = extractRestErrorMessage(err.body);
    if (fromBody) {
      chunks.push(fromBody);
    }
  }
  const text = chunks.join(" ").toLowerCase();
  return (
    text.includes("cannot find navigation tree") ||
    text.includes("cannot find navtree") ||
    text.includes("navtree for site") ||
    text.includes("no navigation tree")
  );
}

/**
 * Load the full section/navon tree for a site.
 *
 * @param siteName CMS site name (e.g. {@code Corporate Investments})
 * @returns Normalized root {@link NavTreeNode}, or {@code null} when the
 *          payload is empty / unparseable / missing nav tree (#3218).
 */
export async function loadSectionTree(
  siteName: string,
): Promise<NavTreeNode | null> {
  const name = siteName.trim();
  if (!name) {
    throw new Error("Site name is required to load the navigation tree");
  }
  try {
    const payload = await get<unknown>(sectionTreeUrl(name));
    const wire = parseSectionNodePayload(payload);
    if (!wire || isEmptySectionTreeWire(wire)) {
      return null;
    }
    return mapSectionNodeToTree(wire);
  } catch (err) {
    if (isMissingNavTreeError(err)) {
      return null;
    }
    throw err;
  }
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

/**
 * Load editable section properties ({@code GET /section/properties/{id}}).
 */
export async function loadSectionProperties(
  sectionId: string,
): Promise<SiteSectionPropertiesWire> {
  const id = sectionId.trim();
  if (!id) {
    throw new Error("Section id is required to load properties");
  }
  const payload = await get<unknown>(sectionPropertiesUrl(id));
  const props = parseSiteSectionPropertiesPayload(payload);
  if (!props) {
    throw new Error("Section properties response was empty or unparseable");
  }
  return props;
}

/**
 * Create a regular (or blog) site section under {@code fields.folderPath}.
 */
export async function createSiteSection(
  fields: CreateSiteSectionFields,
): Promise<unknown> {
  if (!fields.templateId?.trim()) {
    throw new Error("Template is required to create a section");
  }
  if (!fields.folderPath?.trim()) {
    throw new Error("Parent folder path is required to create a section");
  }
  const body = buildCreateSiteSectionBody(fields);
  return post<unknown>(PATHS.SECTION_CREATE_SECTION, body);
}

/**
 * Update section properties (rename / folder name / target / security fields).
 */
export async function updateSiteSection(
  props: SiteSectionPropertiesWire,
): Promise<unknown> {
  if (!props.id?.trim()) {
    throw new Error("Section id is required to update");
  }
  if (!props.title?.trim()) {
    throw new Error("Title is required to update a section");
  }
  if (!props.folderName?.trim()) {
    throw new Error("Folder name is required to update a section");
  }
  const body = buildUpdateSiteSectionBody(props);
  return post<unknown>(PATHS.SECTION_UPDATE, body);
}

/**
 * Move / reorder a section ({@code POST /section/move}).
 */
export async function moveSiteSection(
  fields: MoveSiteSectionFields,
): Promise<unknown> {
  if (!fields.sourceId?.trim() || !fields.targetId?.trim()) {
    throw new Error("Source and target section ids are required to move");
  }
  if (
    typeof fields.targetIndex !== "number" ||
    Number.isNaN(fields.targetIndex)
  ) {
    throw new Error("Target index is required to move a section");
  }
  const body = buildMoveSiteSectionBody(fields);
  return post<unknown>(PATHS.SECTION_MOVE, body);
}

/**
 * Delete a regular section ({@code DELETE /section/{id}}).
 */
export async function deleteSiteSection(sectionId: string): Promise<unknown> {
  const id = sectionId.trim();
  if (!id) {
    throw new Error("Section id is required to delete");
  }
  return del<unknown>(sectionDeleteUrl(id));
}

/**
 * Delete a section link ({@code GET /section/deleteSectionLink/{sec}/{parent}}).
 * Wire uses GET for historical CM1 parity.
 */
export async function deleteSectionLink(
  sectionId: string,
  parentId: string,
): Promise<unknown> {
  const sec = sectionId.trim();
  const parent = parentId.trim();
  if (!sec || !parent) {
    throw new Error("Section and parent ids are required to delete a section link");
  }
  return get<unknown>(sectionDeleteLinkUrl(sec, parent));
}

/**
 * Convert a section to a plain folder
 * ({@code DELETE /section/convertToFolder/{id}}).
 */
export async function convertSectionToFolder(
  sectionId: string,
): Promise<unknown> {
  const id = sectionId.trim();
  if (!id) {
    throw new Error("Section id is required to convert to folder");
  }
  return del<unknown>(sectionConvertToFolderUrl(id));
}

/**
 * Load one section by id ({@code GET /section/{id}}).
 * Used when editing external / section links (#3097).
 */
export async function loadSection(
  sectionId: string,
): Promise<SiteSectionWire> {
  const id = sectionId.trim();
  if (!id) {
    throw new Error("Section id is required to load");
  }
  const payload = await get<unknown>(sectionLoadUrl(id));
  const section = parseSiteSectionPayload(payload);
  if (!section) {
    throw new Error("Section response was empty or unparseable");
  }
  return section;
}

/**
 * Replace a section landing page
 * ({@code POST /section/replaceLandingPage}).
 */
export async function replaceLandingPage(
  fields: ReplaceLandingPageFields,
): Promise<unknown> {
  if (!fields.sectionId?.trim() || !fields.newLandingPageId?.trim()) {
    throw new Error("Section id and new landing page id are required");
  }
  const body = buildReplaceLandingPageBody(fields);
  return post<unknown>(PATHS.SECTION_REPLACE_LANDING_PAGE, body);
}

/**
 * Create a section link
 * ({@code GET /section/createSectionLink/{target}/{parent}}).
 * Wire uses GET for historical CM1 parity.
 */
export async function createSectionLink(
  targetSectionId: string,
  parentSectionId: string,
): Promise<unknown> {
  const url = sectionCreateLinkUrl(targetSectionId, parentSectionId);
  return get<unknown>(url);
}

/**
 * Create an external link section
 * ({@code POST /section/createExternalLinkSection}).
 */
export async function createExternalLinkSection(
  fields: CreateExternalLinkFields,
): Promise<unknown> {
  if (!fields.linkTitle?.trim()) {
    throw new Error("Link title is required");
  }
  if (!fields.externalUrl?.trim()) {
    throw new Error("External URL is required");
  }
  if (!fields.folderPath?.trim()) {
    throw new Error("Parent folder path is required");
  }
  const body = buildCreateExternalLinkBody(fields);
  return post<unknown>(PATHS.SECTION_CREATE_EXTERNAL_LINK, body);
}

/**
 * Update a section link target
 * ({@code POST /section/updateSectionLink}).
 */
export async function updateSectionLink(
  fields: UpdateSectionLinkFields,
): Promise<unknown> {
  if (
    !fields.oldSectionId?.trim() ||
    !fields.newSectionId?.trim() ||
    !fields.parentSectionId?.trim()
  ) {
    throw new Error(
      "Old, new, and parent section ids are required to update a section link",
    );
  }
  const body = buildUpdateSectionLinkBody(fields);
  return post<unknown>(PATHS.SECTION_UPDATE_SECTION_LINK, body);
}

/**
 * Update an external link section
 * ({@code POST /section/updateExternalLink/{sectionGuid}}).
 */
export async function updateExternalLink(
  sectionId: string,
  fields: CreateExternalLinkFields,
): Promise<unknown> {
  const id = sectionId.trim();
  if (!id) {
    throw new Error("Section id is required to update an external link");
  }
  if (!fields.linkTitle?.trim() || !fields.externalUrl?.trim()) {
    throw new Error("Link title and URL are required");
  }
  const body = buildCreateExternalLinkBody(fields);
  return post<unknown>(sectionUpdateExternalLinkUrl(id), body);
}

export type {
  CreateExternalLinkFields,
  CreateSiteSectionFields,
  MoveSiteSectionFields,
  NavTreeNode,
  ReplaceLandingPageFields,
  SiteSectionPropertiesWire,
  SiteSectionWire,
  UpdateSectionLinkFields,
};
