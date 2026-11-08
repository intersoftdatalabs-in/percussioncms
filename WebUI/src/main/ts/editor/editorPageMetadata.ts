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
 * Page-metadata dialog chrome parity for the React Content Editor
 * (#5739, parent #5622 slice 6 batch 2 — leftovers from #5733 batch 1).
 *
 * <p>The retired classic {@code perc_page_edit_dialog} formatted the generic
 * content-editor form for pages with four pieces of chrome this module
 * ports to the React host for {@code percPage} items:</p>
 *
 * <ol>
 *   <li>Collapsible field groups (Page Summary open; SEO, Tags And
 *   Categories, Calendar, Additional Code collapsed).</li>
 *   <li>Auto-generate summary toggle: when {@code auto_generate_summary} is
 *   on, the summary editor hides behind a sanitized read-only preview.</li>
 *   <li>Filename filter on {@code sys_title} before submit (trim plus the
 *   legacy {@code WINDOWS_FILE_NAME} trailing-dot strip).</li>
 *   <li>Site-impact panel: reverse links / relationships for the page via
 *   the same {@code item/siteimpact/page/{id}} endpoint, no contract
 *   changes.</li>
 * </ol>
 */

import type { EditorFieldRow } from "./controlKinds";
import type { ItemEditorField } from "./itemFieldsApi";

/** Filename field the Windows-filename filter applies to on save. */
export const PAGE_METADATA_FILENAME_FIELD = "sys_title";

/** Rich-text summary hidden behind the read-only preview when auto-summary is on. */
export const PAGE_METADATA_SUMMARY_FIELD = "page_summary";

/** Yes-no field driving the auto-generate summary toggle. */
export const PAGE_METADATA_AUTO_SUMMARY_FIELD = "auto_generate_summary";

/**
 * Fields the classic dialog forced {@code aria-required} on, independent
 * of schema required flags (validation is unchanged).
 */
export const PAGE_METADATA_ARIA_REQUIRED_FIELDS: ReadonlySet<string> = new Set([
  "sys_title",
  "resource_link_title",
  "page_title",
]);

export interface PageMetadataGroupDef {
  id: string;
  labelKey: string;
  fieldNames: string[];
  openByDefault: boolean;
}

/**
 * Classic {@code _addFieldGroups} membership and initial collapse state,
 * in dialog order. The first group renders open; the rest collapsed.
 */
export const PAGE_METADATA_GROUP_DEFS: readonly PageMetadataGroupDef[] = [
  {
    id: "page-summary",
    labelKey: "perc.ui.editor@Page Summary",
    fieldNames: [
      "resource_link_title",
      "sys_title",
      "page_summary",
      "sys_contentpostdate",
      "sys_contentpostdatetz",
    ],
    openByDefault: true,
  },
  {
    id: "seo",
    labelKey: "perc.ui.editor@SEO",
    fieldNames: ["page_title", "page_description", "page_noindex"],
    openByDefault: false,
  },
  {
    id: "tags-categories",
    labelKey: "perc.ui.editor@Tags And Categories",
    fieldNames: ["page_tags", "page_categories_tree"],
    openByDefault: false,
  },
  {
    id: "calendar",
    labelKey: "perc.ui.editor@Calendar",
    fieldNames: ["page_calendar"],
    openByDefault: false,
  },
  {
    id: "additional-code",
    labelKey: "perc.ui.editor@Additional Code",
    fieldNames: [
      "additional_head_content",
      "code_insert_after_body_start",
      "code_insert_before_body_close",
    ],
    openByDefault: false,
  },
];

export interface PageMetadataSection {
  id: string;
  labelKey: string;
  openByDefault: boolean;
  rows: EditorFieldRow[];
}

export interface GroupedPageMetadata {
  sections: PageMetadataSection[];
  /** Rows no group claims, kept in original relative order. */
  ungrouped: EditorFieldRow[];
}

function fieldKey(name: string | undefined | null): string {
  return (name ?? "").trim().toLowerCase();
}

/**
 * Partition editor rows into the classic dialog groups. Rows inside a
 * section follow dialog field order; unclaimed rows keep payload order.
 */
export function groupPageMetadataRows(
  rows: readonly EditorFieldRow[],
): GroupedPageMetadata {
  const byName = new Map<string, EditorFieldRow>();
  for (const row of rows) {
    const key = fieldKey(row.name);
    if (key && !byName.has(key)) {
      byName.set(key, row);
    }
  }
  const claimed = new Set<string>();
  const sections: PageMetadataSection[] = PAGE_METADATA_GROUP_DEFS.map(
    (def) => {
      const sectionRows: EditorFieldRow[] = [];
      for (const name of def.fieldNames) {
        const row = byName.get(fieldKey(name));
        if (row) {
          sectionRows.push(row);
          claimed.add(fieldKey(row.name));
        }
      }
      return {
        id: def.id,
        labelKey: def.labelKey,
        openByDefault: def.openByDefault,
        rows: sectionRows,
      };
    },
  );
  return {
    sections,
    ungrouped: rows.filter((row) => !claimed.has(fieldKey(row.name))),
  };
}

/** Default open map for the group toggles (first group open). */
export function defaultPageMetadataOpenGroups(): Record<string, boolean> {
  const open: Record<string, boolean> = {};
  for (const def of PAGE_METADATA_GROUP_DEFS) {
    open[def.id] = def.openByDefault;
  }
  return open;
}

/**
 * Groups that must auto-expand because they hold a field error, mirroring
 * the classic dialog expanding collapsed sections with save errors.
 */
export function pageMetadataGroupsWithErrors(
  sections: readonly PageMetadataSection[],
  fieldErrors: Readonly<Record<string, string>>,
): string[] {
  const errorNames = new Set(
    Object.keys(fieldErrors).map((name) => fieldKey(name)),
  );
  if (errorNames.size === 0) {
    return [];
  }
  const ids: string[] = [];
  for (const section of sections) {
    if (section.rows.some((row) => errorNames.has(fieldKey(row.name)))) {
      ids.push(section.id);
    }
  }
  return ids;
}

/**
 * Whether the auto-generate summary preview replaces the summary editor.
 * The React yes-no control stores {@code "yes"} when on (classic checkbox
 * {@code checked}).
 */
export function isAutoSummaryOn(value: string | undefined | null): boolean {
  return (value ?? "").trim().toLowerCase() === "yes";
}

/**
 * Classic {@code _saveMetadata} filename filter: trim, then the legacy
 * {@code $.perc_textFilters.WINDOWS_FILE_NAME} trailing-dot strip
 * ({@code txt.replace(/\.*$/g, "")}).
 */
export function applyPageFilenameFilter(value: string | undefined | null): string {
  return (value ?? "").trim().replace(/\.*$/g, "");
}

/**
 * Apply the filename filter to the {@code sys_title} save field for page
 * items. Other fields pass through untouched.
 */
export function withPageFilenameField(
  fields: readonly ItemEditorField[],
): ItemEditorField[] {
  return fields.map((field) =>
    fieldKey(field.name) === PAGE_METADATA_FILENAME_FIELD
      ? { ...field, value: applyPageFilenameFilter(field.value) }
      : field,
  );
}

const SUMMARY_DANGEROUS_TAGS = new Set([
  "script",
  "iframe",
  "object",
  "embed",
  "form",
  "style",
  "base",
  "link",
  "meta",
]);

const SUMMARY_DANGEROUS_URL_ATTRS = new Set([
  "href",
  "src",
  "action",
  "formaction",
  "xlink:href",
]);

const SUMMARY_DANGEROUS_URL_SCHEMES = ["javascript:", "data:", "vbscript:"];

function isDangerousSummaryUrl(value: string | null): boolean {
  if (!value) {
    return false;
  }
  const lowered = value.trim().toLowerCase();
  return SUMMARY_DANGEROUS_URL_SCHEMES.some((scheme) =>
    lowered.startsWith(scheme),
  );
}

function stripDangerousSummaryAttributes(el: Element): void {
  for (const attr of Array.from(el.attributes ?? [])) {
    const name = attr.name ?? "";
    if (!name) {
      continue;
    }
    if (name.length >= 2 && name.toLowerCase().startsWith("on")) {
      el.removeAttribute(attr.name);
      continue;
    }
    if (
      SUMMARY_DANGEROUS_URL_ATTRS.has(name.toLowerCase()) &&
      isDangerousSummaryUrl(el.getAttribute(attr.name))
    ) {
      el.removeAttribute(attr.name);
    }
  }
}

function stripDangerousSummaryNodes(root: ParentNode): void {
  const all = root.querySelectorAll("*");
  for (const el of Array.from(all)) {
    const tag = el.tagName?.toLowerCase() ?? "";
    if (SUMMARY_DANGEROUS_TAGS.has(tag)) {
      el.parentNode?.removeChild(el);
      continue;
    }
    stripDangerousSummaryAttributes(el);
  }
}

/**
 * Render the auto-generated summary preview into {@code host}, mirroring
 * the classic {@code _handleAutoSummary} read-only div: the stored summary
 * HTML is parsed detached, stripped of dangerous elements / event-handler
 * and dangerous-scheme attributes, then moved into the live document with
 * {@code importNode} + {@code appendChild} — never {@code innerHTML} — so
 * no string-to-HTML sink carries untrusted markup (CodeQL
 * js/xss-through-dom).
 */
export function appendSanitizedSummaryHtml(
  host: Element,
  html: string | undefined | null,
): void {
  while (host.firstChild) {
    host.removeChild(host.firstChild);
  }
  if (!html || !html.trim()) {
    return;
  }
  const parser = new DOMParser();
  const doc = parser.parseFromString(html, "text/html");
  const body = doc.body;
  if (!body) {
    return;
  }
  stripDangerousSummaryNodes(body);
  const owner = host.ownerDocument ?? document;
  let next = body.firstChild;
  while (next) {
    const imported = owner.importNode(next, true);
    body.removeChild(next);
    host.appendChild(imported);
    next = body.firstChild;
  }
}

export interface PageSiteImpactPage {
  id: string;
  name: string;
  path: string;
  site: string;
  status: string;
  type: string;
}

export interface PageSiteImpactTemplate {
  name: string;
  site: string;
}

export interface PageSiteImpact {
  pages: PageSiteImpactPage[];
  templates: PageSiteImpactTemplate[];
}

function asRecord(value: unknown): Record<string, unknown> | null {
  if (value != null && typeof value === "object" && !Array.isArray(value)) {
    return value as Record<string, unknown>;
  }
  return null;
}

function asString(value: unknown): string {
  return typeof value === "string" ? value : "";
}

/**
 * Classic {@code _createSiteImpactHtml} site derivation: strip the
 * {@code /Sites/} prefix, site is the first segment.
 */
export function siteImpactSiteFromPath(path: string): string {
  const stripped = path.replace("/Sites/", "");
  const slash = stripped.indexOf("/");
  if (slash < 0) {
    return stripped;
  }
  return stripped.substring(0, slash);
}

function parseSiteImpactPage(value: unknown): PageSiteImpactPage | null {
  const rec = asRecord(value);
  if (!rec) {
    return null;
  }
  const id = asString(rec.id ?? rec.Id ?? rec.pageId).trim();
  const name = asString(rec.name ?? rec.Name ?? rec.pageName).trim();
  const path = asString(rec.path ?? rec.pagePath ?? rec.Path);
  if (!id && !name && !path) {
    return null;
  }
  return {
    id,
    name: name || id,
    path,
    site: siteImpactSiteFromPath(path),
    status: asString(rec.status ?? rec.Status),
    type: asString(rec.type ?? rec.templateName ?? rec.Type),
  };
}

function parseSiteImpactTemplate(value: unknown): PageSiteImpactTemplate | null {
  const rec = asRecord(value);
  if (!rec) {
    return null;
  }
  const template = asRecord(rec.template ?? rec.Template);
  const name = asString(
    template?.name ?? template?.Name ?? rec.name ?? rec.Name,
  ).trim();
  const site = asString(rec.site ?? rec.Site).trim();
  if (!name && !site) {
    return null;
  }
  return { name, site };
}

/**
 * Defensive parse of the {@code item/siteimpact/page/{id}} payload. Accepts
 * the bare {@code {pages, templates}} shape and the legacy
 * {@code {data: {pages, templates}}} envelope; unknown shapes yield empty
 * lists instead of throwing.
 */
export function parsePageSiteImpact(payload: unknown): PageSiteImpact {
  const root = asRecord(payload);
  const body = asRecord(root?.data) ?? root ?? {};
  const rawPages = Array.isArray(body.pages) ? body.pages : [];
  const rawTemplates = Array.isArray(body.templates) ? body.templates : [];
  return {
    pages: rawPages
      .map((entry) => parseSiteImpactPage(entry))
      .filter((entry): entry is PageSiteImpactPage => entry != null),
    templates: rawTemplates
      .map((entry) => parseSiteImpactTemplate(entry))
      .filter((entry): entry is PageSiteImpactTemplate => entry != null),
  };
}
