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
 * Pure validation / name helpers for Explorer Create Site (#3002 / #3512).
 *
 * <p>Mirrors classic hostname filter for site names (alpha-numeric, dash,
 * dot) and provides default template-name derivation so the wizard shell
 * can be unit-tested without DOM or REST. Type-picker helpers encode
 * Traditional / Page / Virtual product rules (#3512).</p>
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
 * Site kinds on the Create Site type picker (#3512).
 * Slice 3 enables Traditional, Page, and Virtual.
 */
export type SiteCreateKind = "traditional" | "page" | "virtual";

export const SITE_CREATE_KINDS: readonly SiteCreateKind[] = [
  "traditional",
  "page",
  "virtual",
];

/**
 * True when the chosen kind may leave the type-picker step.
 */
export function isSiteCreateKindEnabled(kind: SiteCreateKind): boolean {
  return (
    kind === "traditional" || kind === "page" || kind === "virtual"
  );
}

/**
 * Page sites require a page / base template step. Traditional and Virtual do not.
 */
export function requiresPageTemplate(kind: SiteCreateKind): boolean {
  return kind === "page";
}

/**
 * Page sites force managed navigation on. Traditional may opt out.
 * Virtual never shows the managed-nav option.
 */
export function managedNavigationForcedOn(kind: SiteCreateKind): boolean {
  return kind === "page";
}

/**
 * Virtual sites have no managed-navigation checkbox (discriminator is sourceKind).
 */
export function hidesManagedNavigation(kind: SiteCreateKind): boolean {
  return kind === "virtual";
}

/**
 * Wizard step ids for the chosen kind. Page inserts a template step.
 */
export function wizardStepsForKind(kind: SiteCreateKind): readonly string[] {
  if (kind === "page") {
    return ["type", "details", "template", "confirm", "progress"];
  }
  return ["type", "details", "confirm", "progress"];
}

/**
 * Optional Virtual {@code rootPath} on confirm. Blank is allowed (handoff
 * to Developer → Sites). Reject {@code ..} traversal in the string.
 */
export function validateVirtualRootPath(
  raw: string,
): { ok: true; path: string | null } | { ok: false; reason: "unsafe" } {
  const trimmed = (raw ?? "").trim();
  if (!trimmed) {
    return { ok: true, path: null };
  }
  if (trimmed.includes("..")) {
    return { ok: false, reason: "unsafe" };
  }
  return { ok: true, path: trimmed };
}

/**
 * True when the create form has enough fields to submit (client-side only).
 *
 * <p>Traditional and Virtual do not prompt for template/base. Page requires
 * a template name and base template. Virtual root path is optional.</p>
 */
export function canSubmitCreateSite(fields: {
  siteName: string;
  templateName?: string;
  baseTemplateName?: string;
  siteType?: SiteCreateKind;
  virtualRootPath?: string;
}): boolean {
  const kind = fields.siteType ?? "traditional";
  if (!isSiteCreateKindEnabled(kind)) {
    return false;
  }
  const site = validateSiteName(fields.siteName);
  if (!site.ok) {
    return false;
  }
  if (kind === "virtual") {
    return validateVirtualRootPath(fields.virtualRootPath ?? "").ok;
  }
  if (!requiresPageTemplate(kind)) {
    return true;
  }
  const tmpl = validateTemplateName(fields.templateName ?? "");
  const base = (fields.baseTemplateName ?? "").trim();
  return tmpl.ok && base.length > 0;
}
