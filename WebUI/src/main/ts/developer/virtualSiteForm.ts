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

import type { VirtualSiteProperties } from "../api/developer/types";

/** Traditional repository site (not virtual). */
export const SOURCE_KIND_REPOSITORY = "repository";

/** Phase 1 Virtual Site adapter wire name (Git/Markdown tree). */
export const SOURCE_KIND_GIT_FILESYSTEM = "git-filesystem";

/** Virtual Site adapter wire name for a CSV tree on the filesystem. */
export const SOURCE_KIND_CSV_FILESYSTEM = "csv-filesystem";

/** Virtual Site adapter wire name for in-memory H2 SQL ({@code jdbc:h2:mem:}). */
export const SOURCE_KIND_SQL_DATABASE = "sql-database";

/** Virtual Site adapter wire name for HTTP JSON / local JSON catalog. */
export const SOURCE_KIND_HTTP_JSON = "http-json";

/** Virtual Site adapter wire name for a local object-key directory (no cloud secrets). */
export const SOURCE_KIND_OBJECT_STORAGE = "object-storage";

/** Virtual Site adapter wire name for a local RSS/Atom fixture (no live feed credentials). */
export const SOURCE_KIND_RSS_ATOM = "rss-atom";

/** Virtual Site adapter wire name for a local RFC 5545 .ics fixture (no CalDAV). */
export const SOURCE_KIND_ICALENDAR = "icalendar";

/** Virtual Site adapter wire name for a local sitemap.xml fixture (no live crawl). */
export const SOURCE_KIND_SITEMAP_XML = "sitemap-xml";

/** Virtual Site adapter wire name for a local robots.txt fixture (no live crawl). */
export const SOURCE_KIND_ROBOTS_TXT = "robots-txt";

/** Virtual Site adapter wire name for a local llms.txt fixture (no live HTTP fetch). */
export const SOURCE_KIND_LLMS_TXT = "llms-txt";

/** Form select values for source kind. */
export type VirtualSourceKindOption =
  | typeof SOURCE_KIND_REPOSITORY
  | typeof SOURCE_KIND_GIT_FILESYSTEM
  | typeof SOURCE_KIND_CSV_FILESYSTEM
  | typeof SOURCE_KIND_SQL_DATABASE
  | typeof SOURCE_KIND_HTTP_JSON
  | typeof SOURCE_KIND_OBJECT_STORAGE
  | typeof SOURCE_KIND_RSS_ATOM
  | typeof SOURCE_KIND_ICALENDAR
  | typeof SOURCE_KIND_SITEMAP_XML
  | typeof SOURCE_KIND_ROBOTS_TXT
  | typeof SOURCE_KIND_LLMS_TXT;

/**
 * Product order for the Developer Sites source-kind {@code <select>}.
 * Keep this list as the single option inventory so object-storage cannot
 * drop out of the live SPA independently of the other kinds (#3893).
 */
export const SOURCE_KIND_SELECT_VALUES: readonly VirtualSourceKindOption[] = [
  SOURCE_KIND_REPOSITORY,
  SOURCE_KIND_GIT_FILESYSTEM,
  SOURCE_KIND_CSV_FILESYSTEM,
  SOURCE_KIND_SQL_DATABASE,
  SOURCE_KIND_HTTP_JSON,
  SOURCE_KIND_OBJECT_STORAGE,
  SOURCE_KIND_RSS_ATOM,
  SOURCE_KIND_ICALENDAR,
  SOURCE_KIND_SITEMAP_XML,
  SOURCE_KIND_ROBOTS_TXT,
  SOURCE_KIND_LLMS_TXT,
];

/** Editable form model for the Virtual Site source panel. */
export interface VirtualSiteFormModel {
  sourceKind: VirtualSourceKindOption;
  rootPath: string;
  remoteUrl: string;
  branch: string;
  configFile: string;
  siteKey: string;
}

/**
 * Normalize a wire/sourceKind string into a form select option.
 * Blank, missing, or {@code repository} → repository; git-filesystem,
 * csv-filesystem, sql-database, http-json, object-storage, rss-atom,
 * icalendar, sitemap-xml, robots-txt, and llms-txt map to themselves; unknown
 * kinds → repository (safe default).
 */
export function normalizeSourceKindOption(
  raw: string | null | undefined,
): VirtualSourceKindOption {
  const v = (raw ?? "").trim().toLowerCase();
  if (!v || v === SOURCE_KIND_REPOSITORY) {
    return SOURCE_KIND_REPOSITORY;
  }
  if (v === SOURCE_KIND_GIT_FILESYSTEM) {
    return SOURCE_KIND_GIT_FILESYSTEM;
  }
  if (v === SOURCE_KIND_CSV_FILESYSTEM) {
    return SOURCE_KIND_CSV_FILESYSTEM;
  }
  if (v === SOURCE_KIND_SQL_DATABASE) {
    return SOURCE_KIND_SQL_DATABASE;
  }
  if (v === SOURCE_KIND_HTTP_JSON) {
    return SOURCE_KIND_HTTP_JSON;
  }
  if (v === SOURCE_KIND_OBJECT_STORAGE) {
    return SOURCE_KIND_OBJECT_STORAGE;
  }
  if (v === SOURCE_KIND_RSS_ATOM) {
    return SOURCE_KIND_RSS_ATOM;
  }
  if (v === SOURCE_KIND_ICALENDAR) {
    return SOURCE_KIND_ICALENDAR;
  }
  if (v === SOURCE_KIND_SITEMAP_XML) {
    return SOURCE_KIND_SITEMAP_XML;
  }
  if (v === SOURCE_KIND_ROBOTS_TXT) {
    return SOURCE_KIND_ROBOTS_TXT;
  }
  if (v === SOURCE_KIND_LLMS_TXT) {
    return SOURCE_KIND_LLMS_TXT;
  }
  // Unknown kinds: surface as repository so operators do not accidentally
  // re-save an unsupported adapter without changing the select.
  return SOURCE_KIND_REPOSITORY;
}

/** True when the form (or wire) represents a Virtual Site adapter. */
export function isVirtualSourceKind(kind: string | null | undefined): boolean {
  const v = (kind ?? "").trim().toLowerCase();
  return v.length > 0 && v !== SOURCE_KIND_REPOSITORY;
}

/** True when source kind is the Git filesystem adapter (remote URL/branch fields). */
export function isGitFilesystemSourceKind(kind: string | null | undefined): boolean {
  return (kind ?? "").trim().toLowerCase() === SOURCE_KIND_GIT_FILESYSTEM;
}

/** True when source kind is the CSV filesystem adapter (root path only). */
export function isCsvFilesystemSourceKind(kind: string | null | undefined): boolean {
  return (kind ?? "").trim().toLowerCase() === SOURCE_KIND_CSV_FILESYSTEM;
}

/** True when source kind is the SQL database adapter (root path + {@code _config.yaml} JDBC). */
export function isSqlDatabaseSourceKind(kind: string | null | undefined): boolean {
  return (kind ?? "").trim().toLowerCase() === SOURCE_KIND_SQL_DATABASE;
}

/** True when source kind is the HTTP JSON adapter (root path + {@code _config.yaml} catalog). */
export function isHttpJsonSourceKind(kind: string | null | undefined): boolean {
  return (kind ?? "").trim().toLowerCase() === SOURCE_KIND_HTTP_JSON;
}

/** True when source kind is the local object-storage adapter (root path only; no cloud secrets). */
export function isObjectStorageSourceKind(kind: string | null | undefined): boolean {
  return (kind ?? "").trim().toLowerCase() === SOURCE_KIND_OBJECT_STORAGE;
}

/** True when source kind is the local RSS/Atom adapter (root path only; no live feed credentials). */
export function isRssAtomSourceKind(kind: string | null | undefined): boolean {
  return (kind ?? "").trim().toLowerCase() === SOURCE_KIND_RSS_ATOM;
}

/** True when source kind is the local iCalendar adapter (root path only; no CalDAV). */
export function isIcalendarSourceKind(kind: string | null | undefined): boolean {
  return (kind ?? "").trim().toLowerCase() === SOURCE_KIND_ICALENDAR;
}

/** True when source kind is the local sitemap XML adapter (root path only; no live crawl). */
export function isSitemapXmlSourceKind(kind: string | null | undefined): boolean {
  return (kind ?? "").trim().toLowerCase() === SOURCE_KIND_SITEMAP_XML;
}

/** True when source kind is the local robots.txt adapter (root path only; no live crawl). */
export function isRobotsTxtSourceKind(kind: string | null | undefined): boolean {
  return (kind ?? "").trim().toLowerCase() === SOURCE_KIND_ROBOTS_TXT;
}

/** True when source kind is the local llms.txt adapter (root path only; no live HTTP fetch). */
export function isLlmsTxtSourceKind(kind: string | null | undefined): boolean {
  return (kind ?? "").trim().toLowerCase() === SOURCE_KIND_LLMS_TXT;
}

/**
 * Map API payload → form fields. Defensive string coercion for Optional-ish
 * Jackson shapes.
 */
export function virtualPropsToForm(
  props: VirtualSiteProperties | null | undefined,
): VirtualSiteFormModel {
  return {
    sourceKind: normalizeSourceKindOption(asOptionalString(props?.sourceKind)),
    rootPath: asOptionalString(props?.rootPath) ?? "",
    remoteUrl: asOptionalString(props?.remoteUrl) ?? "",
    branch: asOptionalString(props?.branch) ?? "",
    configFile: asOptionalString(props?.configFile) ?? "",
    siteKey: asOptionalString(props?.siteKey) ?? "",
  };
}

/**
 * Build PUT body from form state.
 * Repository / blank kind clears virtual configuration on the server.
 */
export function formToVirtualProps(form: VirtualSiteFormModel): VirtualSiteProperties {
  const kind = form.sourceKind;
  if (!isVirtualSourceKind(kind)) {
    return {
      sourceKind: SOURCE_KIND_REPOSITORY,
      rootPath: null,
      configFile: null,
      siteKey: null,
    };
  }
  if (kind === SOURCE_KIND_CSV_FILESYSTEM) {
    // CSV rejects a non-blank virtual.remoteUrl (REST 400). Send empty
    // remoteUrl/branch so a prior Git remote is cleared (omit would keep it).
    return {
      sourceKind: SOURCE_KIND_CSV_FILESYSTEM,
      rootPath: form.rootPath.trim() || null,
      remoteUrl: "",
      branch: "",
    };
  }
  if (kind === SOURCE_KIND_SQL_DATABASE) {
    // SQL rejects a non-blank virtual.remoteUrl (REST 400). JDBC URL/user/query
    // stay in _config.yaml under rootPath — never send a password on this envelope.
    return {
      sourceKind: SOURCE_KIND_SQL_DATABASE,
      rootPath: form.rootPath.trim() || null,
      remoteUrl: "",
      branch: "",
    };
  }
  if (kind === SOURCE_KIND_HTTP_JSON) {
    // HTTP JSON rejects a non-blank virtual.remoteUrl (REST 400). Catalog URL or
    // local fixture stay in _config.yaml (http.url / http.file) — never send
    // Authorization or API keys on this envelope.
    return {
      sourceKind: SOURCE_KIND_HTTP_JSON,
      rootPath: form.rootPath.trim() || null,
      remoteUrl: "",
      branch: "",
    };
  }
  if (kind === SOURCE_KIND_OBJECT_STORAGE) {
    // Object storage rejects a non-blank virtual.remoteUrl (REST 400). Local
    // object-key directory only — never send cloud URLs, IAM, or access keys.
    return {
      sourceKind: SOURCE_KIND_OBJECT_STORAGE,
      rootPath: form.rootPath.trim() || null,
      remoteUrl: "",
      branch: "",
    };
  }
  if (kind === SOURCE_KIND_RSS_ATOM) {
    // RSS/Atom rejects a non-blank virtual.remoteUrl (REST 400). Local fixture
    // directory only — never send live feed URLs or credentials.
    return {
      sourceKind: SOURCE_KIND_RSS_ATOM,
      rootPath: form.rootPath.trim() || null,
      remoteUrl: "",
      branch: "",
    };
  }
  if (kind === SOURCE_KIND_ICALENDAR) {
    // iCalendar rejects a non-blank virtual.remoteUrl (REST 400). Local RFC 5545
    // fixture directory only — never send CalDAV URLs or credentials.
    return {
      sourceKind: SOURCE_KIND_ICALENDAR,
      rootPath: form.rootPath.trim() || null,
      remoteUrl: "",
      branch: "",
    };
  }
  if (kind === SOURCE_KIND_SITEMAP_XML) {
    // Sitemap XML rejects a non-blank virtual.remoteUrl (REST 400). Local
    // sitemap.xml fixture directory only — never send crawl URLs or credentials.
    return {
      sourceKind: SOURCE_KIND_SITEMAP_XML,
      rootPath: form.rootPath.trim() || null,
      remoteUrl: "",
      branch: "",
    };
  }
  if (kind === SOURCE_KIND_ROBOTS_TXT) {
    // robots.txt rejects a non-blank virtual.remoteUrl (REST 400). Local
    // robots.txt fixture directory only — never send crawl URLs or credentials.
    return {
      sourceKind: SOURCE_KIND_ROBOTS_TXT,
      rootPath: form.rootPath.trim() || null,
      remoteUrl: "",
      branch: "",
    };
  }
  if (kind === SOURCE_KIND_LLMS_TXT) {
    // llms.txt rejects a non-blank virtual.remoteUrl (REST 400). Local
    // llms.txt fixture directory only — never send live fetch URLs or credentials.
    return {
      sourceKind: SOURCE_KIND_LLMS_TXT,
      rootPath: form.rootPath.trim() || null,
      remoteUrl: "",
      branch: "",
    };
  }
  return {
    sourceKind: SOURCE_KIND_GIT_FILESYSTEM,
    rootPath: form.rootPath.trim() || null,
    // Empty string (not omit) so Save can clear a stored remote on #3568;
    // omitted remoteUrl/branch would keep the previous server value.
    remoteUrl: form.remoteUrl.trim(),
    branch: form.branch.trim(),
    configFile: form.configFile.trim() || null,
    siteKey: form.siteKey.trim() || null,
  };
}

/**
 * Lightweight client-side checks aligned with PSVirtualSiteHelper (not a full
 * NIO walk — server still validates on PUT). Remote URL / branch are optional
 * and are not validated here (checkout and fail-closed URL rules live on the
 * server).
 *
 * @returns error message key fragment, or null when OK
 */
export function validateVirtualSiteForm(
  form: VirtualSiteFormModel,
): "root-required" | "root-unsafe" | "config-unsafe" | null {
  if (!isVirtualSourceKind(form.sourceKind)) {
    return null;
  }
  const root = form.rootPath.trim();
  if (!root) {
    return "root-required";
  }
  // Reject obvious path traversal in the string (server also checks after NIO normalize).
  if (root.includes("..")) {
    return "root-unsafe";
  }
  const config = form.configFile.trim();
  if (config) {
    if (config.includes("..") || config.includes("/") || config.includes("\\")) {
      return "config-unsafe";
    }
  }
  return null;
}

/** Empty form (traditional repository default). */
export function emptyVirtualSiteForm(): VirtualSiteFormModel {
  return {
    sourceKind: SOURCE_KIND_REPOSITORY,
    rootPath: "",
    remoteUrl: "",
    branch: "",
    configFile: "",
    siteKey: "",
  };
}

function asOptionalString(value: unknown): string | undefined {
  if (value == null) return undefined;
  if (typeof value === "string") return value;
  // Jackson Optional sometimes arrives already unwrapped; ignore objects.
  return undefined;
}
