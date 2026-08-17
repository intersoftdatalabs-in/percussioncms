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
 * Site create transport for modern Explorer (#3002 / parent #2989).
 *
 * <p>Reuses the proven sitemanage create contract used by classic
 * {@code perc_newsitedialog}: {@code POST /services/sitemanage/site/}
 * with a {@code Site} body. Traditional (repository) sites are the default —
 * no virtual properties are sent. Public REST {@code SitesAdaptor#createSite}
 * is intentionally not used (not implemented on the adaptor).</p>
 */

import { formatApiError, get, post } from "../client";
import { PATHS } from "../paths";

/** Product default base template (IPSSitemanageConstants.PLAIN_BASE_TEMPLATE_NAME). */
export const PLAIN_BASE_TEMPLATE_NAME = "perc.base.plain";

/** Default home / navigation title for a new traditional site. */
export const DEFAULT_HOME_PAGE_TITLE = "Home Page";

/** Wire body for {@code POST /sitemanage/site/} (legacy Site wrapper). */
export interface CreateSiteRequest {
  name: string;
  label?: string;
  description?: string;
  homePageTitle?: string;
  navigationTitle?: string;
  baseTemplateName: string;
  templateName: string;
  /**
   * Traditional sites only. Default true. When false, create the site folder
   * without a CMS NavTree / homepage. Virtual Sites do not send this flag.
   */
  managedNavigation?: boolean;
  /**
   * CM1 page-based site ({@code pageBased} / {@code IS_PAGE_BASED} on
   * POST /sitemanage/site/). Send {@code true} for Page type. Traditional
   * omits this field.
   */
  pageBased?: boolean;
}

/** Minimal site summary returned after create (name is required for navigation). */
export interface CreatedSiteSummary {
  name: string;
  id?: string;
  label?: string;
}

/** Base template row from pagemanagement readonly summary. */
export interface BaseTemplateSummary {
  id?: string;
  name: string;
  label?: string;
  thumbPath?: string;
}

/**
 * Build the JSON body matching classic perc_newsitedialog.
 */
export function buildCreateSiteBody(req: CreateSiteRequest): {
  Site: Record<string, string | boolean>;
} {
  const name = req.name.trim();
  const home = (req.homePageTitle ?? DEFAULT_HOME_PAGE_TITLE).trim() || DEFAULT_HOME_PAGE_TITLE;
  const nav = (req.navigationTitle ?? home).trim() || home;
  const managedNavigation = req.managedNavigation !== false;
  const site: Record<string, string | boolean> = {
    name,
    label: (req.label ?? name).trim() || name,
    description: (req.description ?? "").trim(),
    homePageTitle: home,
    navigationTitle: nav,
    baseTemplateName: req.baseTemplateName.trim(),
    templateName: req.templateName.trim(),
    managedNavigation,
  };
  if (req.pageBased === true) {
    site.pageBased = true;
  }
  return { Site: site };
}

/**
 * Parse create-site response (Jackson root wrap {@code Site} or plain object).
 */
export function parseCreatedSite(payload: unknown): CreatedSiteSummary {
  if (payload == null || typeof payload !== "object") {
    throw new Error("Unexpected create-site response");
  }
  const root = payload as Record<string, unknown>;
  const site =
    (root.Site as Record<string, unknown> | undefined) ??
    (root.site as Record<string, unknown> | undefined) ??
    root;
  const name =
    typeof site.name === "string"
      ? site.name.trim()
      : typeof site.Name === "string"
        ? site.Name.trim()
        : "";
  if (!name) {
    throw new Error("Create-site response missing site name");
  }
  const idRaw = site.id ?? site.Id;
  const id =
    idRaw != null && String(idRaw).trim().length > 0
      ? String(idRaw).trim()
      : undefined;
  const label =
    typeof site.label === "string"
      ? site.label
      : typeof site.Label === "string"
        ? site.Label
        : undefined;
  return { name, id, label };
}

const TEMPLATE_LIST_KEYS = [
  "TemplateSummary",
  "templateSummary",
  "Template",
  "templates",
  "entries",
] as const;

/**
 * Normalize base-template catalog payload to a simple name list.
 */
export function parseBaseTemplateList(payload: unknown): BaseTemplateSummary[] {
  if (payload == null) {
    return [];
  }
  let rows: unknown[] = [];
  if (Array.isArray(payload)) {
    rows = payload;
  } else if (typeof payload === "object") {
    const obj = payload as Record<string, unknown>;
    for (const key of TEMPLATE_LIST_KEYS) {
      const raw = obj[key];
      if (raw == null) continue;
      if (Array.isArray(raw)) {
        rows = raw;
        break;
      }
      if (typeof raw === "object") {
        rows = [raw];
        break;
      }
    }
  }
  const out: BaseTemplateSummary[] = [];
  for (const row of rows) {
    if (row == null || typeof row !== "object") continue;
    const r = row as Record<string, unknown>;
    // Require string name (or Name). Non-string name values are intentionally
    // skipped so a single bad catalog row cannot break the create-site wizard.
    const name =
      typeof r.name === "string"
        ? r.name.trim()
        : typeof r.Name === "string"
          ? r.Name.trim()
          : "";
    if (!name) continue;
    const id =
      r.id != null && String(r.id).trim().length > 0
        ? String(r.id).trim()
        : undefined;
    const label =
      typeof r.label === "string"
        ? r.label
        : typeof r.displayName === "string"
          ? r.displayName
          : undefined;
    const thumbPath =
      typeof r.thumbPath === "string"
        ? r.thumbPath
        : typeof r.imagePath === "string"
          ? r.imagePath
          : undefined;
    out.push({ id, name, label, thumbPath });
  }
  return out;
}

/**
 * Pick a default base template: prefer {@link PLAIN_BASE_TEMPLATE_NAME}, else first.
 */
export function pickDefaultBaseTemplate(
  templates: ReadonlyArray<BaseTemplateSummary>,
): string {
  if (templates.length === 0) {
    return PLAIN_BASE_TEMPLATE_NAME;
  }
  const plain = templates.find((t) => t.name === PLAIN_BASE_TEMPLATE_NAME);
  if (plain) {
    return plain.name;
  }
  return templates[0]!.name;
}

/**
 * POST traditional site create via sitemanage (legacy Site contract).
 */
export async function createTraditionalSite(
  req: CreateSiteRequest,
): Promise<CreatedSiteSummary> {
  try {
    const body = buildCreateSiteBody(req);
    // Trailing slash matches classic SITE_CREATE + "/"
    const payload = await post<unknown>(`${PATHS.SITES_ALL}/`, body);
    return parseCreatedSite(payload);
  } catch (err: unknown) {
    throw new Error(formatApiError(err, "Could not create site"));
  }
}

/**
 * GET base template library ({@code type=base}).
 */
export async function listBaseTemplates(
  type: "base" | "resp" = "base",
): Promise<BaseTemplateSummary[]> {
  const url = `${PATHS.TEMPLATES_READONLY}?type=${encodeURIComponent(type)}`;
  try {
    const payload = await get<unknown>(url);
    return parseBaseTemplateList(payload);
  } catch (err: unknown) {
    throw new Error(formatApiError(err, "Could not load base templates"));
  }
}

/**
 * Explorer folder path for a newly created site (product path form).
 */
export function siteFolderPath(siteName: string): string {
  const name = siteName.trim().replace(/^\/+|\/+$/g, "");
  return `/Sites/${name}`;
}
