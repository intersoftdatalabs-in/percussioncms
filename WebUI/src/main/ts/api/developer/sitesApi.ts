/*
 * Copyright (c) 2026 Intersoft Data Labs, Inc.
 */

import { get, post, put } from "../client";
import { objectGuidString } from "../displayFormatGuid";
import { PATHS } from "../paths";
import type {
  RestGuid,
  SiteDef,
  VirtualSiteBuildRequest,
  VirtualSiteBuildResult,
  VirtualSitePreviewStatus,
  VirtualSiteProperties,
} from "./types";

/** Honest design gaps for Developer SY-04 site browse (not full site design). */
export const SITE_DESIGN_GAPS: string[] = [
  "Site create / update / delete is not supported from this Developer surface",
  "Full site publish and section design live outside the Developer catalog",
  "Workflow association is browsed under the Workflows catalog",
];

const LIST_WRAPPER_KEYS = [
  "SiteList",
  "siteList",
  "Site",
  "site",
  "sites",
  "entries",
] as const;

/**
 * Parse GET /services/sites JSON.
 *
 * <p>Accepts a bare array or Jackson WRAP_ROOT envelopes ({@code SiteList} / {@code Site}).
 * Nested wraps ({@code {SiteList:{Site:[...]}}}) and per-item {@code {Site:{name}}} objects
 * are unwrapped so Developer Sites does not render a silent empty table after HTTP 200
 * (#3198). Unknown object shapes throw so the panel shows an error instead of empty.
 */
export function parseSiteList(payload: unknown): SiteDef[] {
  return collectSiteRows(payload, 0).map(normalizeSiteRow);
}

function collectSiteRows(payload: unknown, depth: number): unknown[] {
  if (payload == null) {
    return [];
  }
  if (depth > 5) {
    throw new Error("Unexpected site list payload (too deeply nested)");
  }
  if (Array.isArray(payload)) {
    return payload.flatMap((item) => unwrapArrayItem(item, depth));
  }
  if (typeof payload === "object") {
    const obj = payload as Record<string, unknown>;
    for (const key of LIST_WRAPPER_KEYS) {
      const raw = obj[key];
      if (raw == null) continue;
      if (Array.isArray(raw)) {
        return collectSiteRows(raw, depth + 1);
      }
      if (typeof raw === "object") {
        if (looksLikeSite(raw as Record<string, unknown>)) {
          return [raw];
        }
        return collectSiteRows(raw, depth + 1);
      }
    }
    if (looksLikeSite(obj)) {
      return [obj];
    }
    throw new Error("Unexpected site list payload (expected array or known Site wrapper)");
  }
  throw new Error("Unexpected site list payload type");
}

function unwrapArrayItem(item: unknown, depth: number): unknown[] {
  if (item == null || typeof item !== "object" || Array.isArray(item)) {
    return item == null ? [] : [item];
  }
  const obj = item as Record<string, unknown>;
  if (looksLikeSite(obj)) {
    return [obj];
  }
  const nested = obj.Site ?? obj.site;
  if (nested != null && typeof nested === "object") {
    return collectSiteRows(nested, depth + 1);
  }
  return [obj];
}

const SITE_SIGNAL_KEYS = [
  "baseUrl",
  "pageBasedSite",
  "guid",
  "defaultDocument",
  "siteProtocol",
  "canonicalDist",
  "description",
  "defaultFileExtention",
] as const;

const NAME_ONLY_KEYS = new Set(["name", "label", "id"]);

/**
 * A Site row must have a name. Name-only summaries are accepted; a lone
 * {@code name} plus unrelated keys (e.g. nested metadata) is not a Site.
 */
function looksLikeSite(obj: Record<string, unknown>): boolean {
  if (!hasSiteName(obj.name)) {
    return false;
  }
  if (SITE_SIGNAL_KEYS.some((key) => key in obj)) {
    return true;
  }
  return Object.keys(obj).every((key) => NAME_ONLY_KEYS.has(key));
}

function hasSiteName(value: unknown): boolean {
  if (typeof value === "string") {
    return value.trim().length > 0;
  }
  if (value != null && typeof value === "object" && !Array.isArray(value)) {
    const obj = value as Record<string, unknown>;
    return typeof obj.value === "string" && obj.value.trim().length > 0;
  }
  return false;
}

function normalizeSiteRow(raw: unknown): SiteDef {
  if (raw == null || typeof raw !== "object" || Array.isArray(raw)) {
    return {};
  }
  const obj = raw as Record<string, unknown>;
  const name = coerceDisplayString(obj.name);
  const guidString = objectGuidString(obj.guid);
  let guid = obj.guid as RestGuid | undefined;
  if (guidString) {
    const existing =
      guid != null && typeof guid === "object" && !Array.isArray(guid) ? guid : {};
    guid = { ...existing, stringValue: guidString };
  }
  return {
    ...(obj as SiteDef),
    name: name || undefined,
    guid,
  };
}

/** Jackson Optional leftovers may arrive as objects; only strings bind as names. */
export function coerceDisplayString(value: unknown): string {
  if (typeof value === "string") {
    return value.trim();
  }
  if (value != null && typeof value === "object" && !Array.isArray(value)) {
    const obj = value as Record<string, unknown>;
    if (typeof obj.value === "string") {
      return obj.value.trim();
    }
  }
  return "";
}

function withGaps(s: SiteDef): SiteDef {
  return {
    ...s,
    designGaps:
      s.designGaps && s.designGaps.length > 0 ? s.designGaps : [...SITE_DESIGN_GAPS],
  };
}

/** GET /services/sites */
export async function listSites(): Promise<SiteDef[]> {
  const payload = await get<unknown>(PATHS.SITES);
  return parseSiteList(payload).map(withGaps);
}

/**
 * Normalize virtual properties payload (Jackson root wrap or plain DTO).
 */
export function parseVirtualSiteProperties(payload: unknown): VirtualSiteProperties {
  if (payload == null || typeof payload !== "object") {
    return {};
  }
  const obj = payload as Record<string, unknown>;
  const root =
    (obj.VirtualSiteProperties as Record<string, unknown> | undefined) ??
    (obj.virtualSiteProperties as Record<string, unknown> | undefined) ??
    obj;
  return {
    sourceKind: asNullableString(root.sourceKind),
    rootPath: asNullableString(root.rootPath),
    configFile: asNullableString(root.configFile),
    siteKey: asNullableString(root.siteKey),
    virtual: typeof root.virtual === "boolean" ? root.virtual : undefined,
  };
}

/** GET /services/sites/{nameOrId}/virtual */
export async function getVirtualSiteProperties(
  nameOrId: string,
): Promise<VirtualSiteProperties> {
  const key = encodeURIComponent(nameOrId.trim());
  const payload = await get<unknown>(`${PATHS.SITES}/${key}/virtual`);
  return parseVirtualSiteProperties(payload);
}

/** PUT /services/sites/{nameOrId}/virtual */
export async function updateVirtualSiteProperties(
  nameOrId: string,
  props: VirtualSiteProperties,
): Promise<VirtualSiteProperties> {
  const key = encodeURIComponent(nameOrId.trim());
  const payload = await put<unknown>(`${PATHS.SITES}/${key}/virtual`, props);
  return parseVirtualSiteProperties(payload);
}

/**
 * Normalize Virtual Site build result (Jackson root wrap or plain DTO).
 */
export function parseVirtualSiteBuildResult(payload: unknown): VirtualSiteBuildResult {
  if (payload == null || typeof payload !== "object") {
    return {};
  }
  const obj = payload as Record<string, unknown>;
  const root =
    (obj.VirtualSiteBuildResult as Record<string, unknown> | undefined) ??
    (obj.virtualSiteBuildResult as Record<string, unknown> | undefined) ??
    obj;
  return {
    siteName: asNullableString(root.siteName),
    siteKey: asNullableString(root.siteKey),
    outputPath: asNullableString(root.outputPath),
    pagesWritten: asNullableNumber(root.pagesWritten),
    linkProblemCount: asNullableNumber(root.linkProblemCount),
    hasLinkProblems:
      typeof root.hasLinkProblems === "boolean" ? root.hasLinkProblems : undefined,
    linkProblems: asStringArray(root.linkProblems),
    writtenFiles: asStringArray(root.writtenFiles),
  };
}

/**
 * POST /services/sites/{nameOrId}/virtual/build
 *
 * <p>Uses the Site's <em>saved</em> virtual.* properties on the server. Optional
 * {@code outputRoot} overrides the default install tmp path. Requires Admin.
 * Traditional repository Sites return 4xx.
 */
export async function buildVirtualSite(
  nameOrId: string,
  request?: VirtualSiteBuildRequest | null,
): Promise<VirtualSiteBuildResult> {
  const key = encodeURIComponent(nameOrId.trim());
  const body =
    request && typeof request.outputRoot === "string" && request.outputRoot.trim()
      ? { outputRoot: request.outputRoot.trim() }
      : {};
  const payload = await post<unknown>(`${PATHS.SITES}/${key}/virtual/build`, body);
  return parseVirtualSiteBuildResult(payload);
}

/**
 * Normalize Virtual Site preview status (Jackson root wrap or plain DTO).
 */
export function parseVirtualSitePreviewStatus(payload: unknown): VirtualSitePreviewStatus {
  if (payload == null || typeof payload !== "object") {
    return {};
  }
  const obj = payload as Record<string, unknown>;
  const root =
    (obj.VirtualSitePreviewStatus as Record<string, unknown> | undefined) ??
    (obj.virtualSitePreviewStatus as Record<string, unknown> | undefined) ??
    obj;
  return {
    available: typeof root.available === "boolean" ? root.available : undefined,
    homePath: asNullableString(root.homePath),
    outputPath: asNullableString(root.outputPath),
    message: asNullableString(root.message),
  };
}

/** GET /services/sites/{nameOrId}/virtual/preview — last-build availability (Admin). */
export async function getVirtualSitePreviewStatus(
  nameOrId: string,
): Promise<VirtualSitePreviewStatus> {
  const key = encodeURIComponent(nameOrId.trim());
  const payload = await get<unknown>(`${PATHS.SITES}/${key}/virtual/preview`);
  return parseVirtualSitePreviewStatus(payload);
}

/**
 * Browser URL for the assembled preview file stream (same-origin cookies).
 * {@code homePath} must be a relative path such as {@code 8.2/index.html}.
 */
export function virtualSitePreviewContentHref(nameOrId: string, homePath: string): string {
  const key = encodeURIComponent(nameOrId.trim());
  const rel = homePath
    .trim()
    .replace(/\\/g, "/")
    .replace(/^\/+/, "")
    .split("/")
    .filter((seg) => seg.length > 0 && seg !== "." && seg !== "..")
    .map((seg) => encodeURIComponent(seg))
    .join("/");
  return `${PATHS.SITES}/${key}/virtual/preview/${rel}`;
}

function asNullableString(value: unknown): string | null | undefined {
  if (value === undefined) return undefined;
  if (value === null) return null;
  if (typeof value === "string") return value;
  return undefined;
}

function asNullableNumber(value: unknown): number | null | undefined {
  if (value === undefined) return undefined;
  if (value === null) return null;
  if (typeof value === "number" && Number.isFinite(value)) return value;
  if (typeof value === "string" && value.trim() !== "") {
    const n = Number(value);
    if (Number.isFinite(n)) return n;
  }
  return undefined;
}

function asStringArray(value: unknown): string[] | undefined {
  if (value == null) return undefined;
  if (!Array.isArray(value)) return undefined;
  return value.filter((v): v is string => typeof v === "string");
}
