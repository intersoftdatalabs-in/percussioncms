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
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/**
 * Pure validation / name helpers for Explorer Create Site (#3002).
 *
 * <p>Mirrors classic hostname filter for site names (alpha-numeric, dash,
 * dot) and provides default template-name derivation so the wizard shell
 * can be unit-tested without DOM or REST.</p>
 */

/** Max length aligned with PSSiteDataRestService site-name allow-list (1–100). */
export const SITE_NAME_MAX_LENGTH = 100;

/** Description maxlength from classic new-site dialog. */
export const SITE_DESCRIPTION_MAX_LENGTH = 255;

/**
 * Classic HOSTNAME filter: keep only alpha-numeric, {@code -}, and {@code .}.
 */
export function filterSiteNameInput(raw: string): string {
  if (raw == null) return "";
  return String(raw).replace(/[^a-zA-Z0-9\-.]/g, "");
}

/**
 * Classic URL-ish filter for template names: strip spaces and path separators.
 */
export function filterTemplateNameInput(raw: string): string {
  if (raw == null) return "";
  return String(raw).replace(/[\s\\/]/g, "");
}

export type SiteNameValidation =
  | { ok: true; name: string }
  | { ok: false; reason: "empty" | "invalidChars" | "tooLong" };

/**
 * Validate a site name after hostname filtering.
 */
export function validateSiteName(raw: string): SiteNameValidation {
  const trimmed = (raw ?? "").trim();
  if (!trimmed) {
    return { ok: false, reason: "empty" };
  }
  const filtered = filterSiteNameInput(trimmed);
  if (filtered !== trimmed) {
    return { ok: false, reason: "invalidChars" };
  }
  if (filtered.length > SITE_NAME_MAX_LENGTH) {
    return { ok: false, reason: "tooLong" };
  }
  return { ok: true, name: filtered };
}

export type TemplateNameValidation =
  | { ok: true; name: string }
  | { ok: false; reason: "empty" | "tooLong" };

/**
 * Validate template name (required non-blank, max 100).
 */
export function validateTemplateName(raw: string): TemplateNameValidation {
  const trimmed = (raw ?? "").trim();
  if (!trimmed) {
    return { ok: false, reason: "empty" };
  }
  if (trimmed.length > SITE_NAME_MAX_LENGTH) {
    return { ok: false, reason: "tooLong" };
  }
  return { ok: true, name: trimmed };
}

/**
 * Default site template name from site name (classic: operator-chosen; we
 * seed {@code {SiteName}Template} for a one-click QA path).
 */
export function defaultTemplateNameForSite(siteName: string): string {
  const base = filterSiteNameInput((siteName ?? "").trim());
  if (!base) {
    return "SiteTemplate";
  }
  const candidate = `${base}Template`;
  return candidate.length > SITE_NAME_MAX_LENGTH
    ? candidate.slice(0, SITE_NAME_MAX_LENGTH)
    : candidate;
}

/**
 * Clamp description to product max length.
 */
export function clampSiteDescription(raw: string): string {
  const s = raw ?? "";
  if (s.length <= SITE_DESCRIPTION_MAX_LENGTH) {
    return s;
  }
  return s.slice(0, SITE_DESCRIPTION_MAX_LENGTH);
}

/**
 * Site kinds shown on the Create Site type picker (#3522 / parent #3512).
 * Slice 1 ships Traditional only; Page and Virtual stay blocked until
 * later slices.
 */
export type SiteCreateKind = "traditional" | "page" | "virtual";

export const SITE_CREATE_KINDS: readonly SiteCreateKind[] = [
  "traditional",
  "page",
  "virtual",
];

/**
 * True when the chosen kind may leave the type-picker step. Slice 1:
 * Traditional only.
 */
export function isSiteCreateKindEnabled(kind: SiteCreateKind): boolean {
  return kind === "traditional";
}

/**
 * True when the create form has enough fields to submit (client-side only).
 *
 * <p>Traditional (#3522) does not prompt for template/base — those values
 * are generated. When {@code siteType} is Traditional (default) and
 * template/base are omitted, only the site name is required. Page and
 * Virtual cannot submit from this slice.</p>
 */
export function canSubmitCreateSite(fields: {
  siteName: string;
  templateName?: string;
  baseTemplateName?: string;
  siteType?: SiteCreateKind;
}): boolean {
  const kind = fields.siteType ?? "traditional";
  if (!isSiteCreateKindEnabled(kind)) {
    return false;
  }
  const site = validateSiteName(fields.siteName);
  if (!site.ok) {
    return false;
  }
  const hasTemplateFields =
    fields.templateName != null || fields.baseTemplateName != null;
  if (!hasTemplateFields) {
    return true;
  }
  const tmpl = validateTemplateName(fields.templateName ?? "");
  const base = (fields.baseTemplateName ?? "").trim();
  return tmpl.ok && base.length > 0;
}
