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
  VirtualSitePublishResult,
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
  "SiteSummary",
  "siteSummary",
  "Site",
  "site",
  "sites",
  "entries",
  "item",
  "Item",
] as const;

/**
 * Parse GET /services/sites JSON (and the sitemanage SiteSummary list fallback).
 *
 * <p>Accepts a bare array or Jackson WRAP_ROOT envelopes ({@code SiteList} / {@code Site} /
 * {@code SiteSummary}). Nested wraps ({@code {SiteList:{Site:[...]}}},
 * {@code {SiteList:{sites:[...]}}}, JAXB {@code item}) and per-item {@code {Site:{name}}}
 * objects are unwrapped so Developer Sites does not render a silent empty table after
 * HTTP 200 (#3198 / #3368). Unknown object shapes throw so the panel shows an error
 * instead of empty. XML text (wrong Content-Type) is parsed when {@code DOMParser} exists.
 */
export function parseSiteList(payload: unknown): SiteDef[] {
  if (typeof payload === "string") {
    const trimmed = payload.trim();
    if (!trimmed) {
      return [];
    }
    if (trimmed.startsWith("{") || trimmed.startsWith("[")) {
      try {
        return parseSiteList(JSON.parse(trimmed) as unknown);
      } catch {
        throw new Error("Unexpected site list payload type");
      }
    }
    if (trimmed.startsWith("<")) {
      return parseXmlSiteList(trimmed);
    }
    throw new Error("Unexpected site list payload type");
  }
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
    const emptyBean = collectionBeanEmpty(obj);
    if (emptyBean === true) {
      return [];
    }
    if (emptyBean === false) {
      throw new Error(
        "Unexpected site list payload (collection serialized as empty bean)",
      );
    }
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

/**
 * Jackson may serialize an {@code ArrayList} subclass as {@code {empty:true|false}}
 * instead of a JSON array (#3368 / ActionMenuList). {@code empty:true} is a real
 * empty list; {@code empty:false} means rows were dropped.
 */
function collectionBeanEmpty(obj: Record<string, unknown>): boolean | null {
  const keys = Object.keys(obj);
  if (keys.length !== 1 || keys[0] !== "empty" || typeof obj.empty !== "boolean") {
    return null;
  }
  return obj.empty;
}

function unwrapArrayItem(item: unknown, depth: number): unknown[] {
  if (item == null || typeof item !== "object" || Array.isArray(item)) {
    return item == null ? [] : [item];
  }
  const obj = item as Record<string, unknown>;
  if (looksLikeSite(obj)) {
    return [obj];
  }
  const nested = obj.Site ?? obj.site ?? obj.SiteSummary ?? obj.siteSummary;
  if (nested != null && typeof nested === "object") {
    return collectSiteRows(nested, depth + 1);
  }
  return [obj];
}

function xmlLocalName(el: Element): string {
  const raw = el.localName || el.tagName || "";
  const colon = raw.lastIndexOf(":");
  return colon >= 0 ? raw.slice(colon + 1) : raw;
}

function xmlDirectChildText(el: Element, names: string[]): string {
  const want = new Set(names.map((n) => n.toLowerCase()));
  for (const child of Array.from(el.children)) {
    if (want.has(xmlLocalName(child).toLowerCase())) {
      return (child.textContent || "").trim();
    }
  }
  return "";
}

/**
 * CXF may still emit XML when Accept negotiation misses JSON (#3368 content-type).
 */
function parseXmlSiteList(xml: string): SiteDef[] {
  if (typeof DOMParser === "undefined") {
    throw new Error("Unexpected site list payload type");
  }
  const doc = new DOMParser().parseFromString(xml, "text/xml");
  if (doc.getElementsByTagName("parsererror").length > 0) {
    throw new Error("Unexpected site list payload type");
  }
  const rowTags = new Set(["site", "sitesummary", "item"]);
  const rows: SiteDef[] = [];
  const walk = (el: Element): void => {
    const name = xmlLocalName(el).toLowerCase();
    if (rowTags.has(name)) {
      const siteName =
        xmlDirectChildText(el, ["name"]) || (el.getAttribute("name") || "").trim();
      const description = xmlDirectChildText(el, ["description"]);
      const baseUrl = xmlDirectChildText(el, ["baseUrl", "base-url"]);
      if (siteName) {
        rows.push({
          name: siteName,
          description: description || undefined,
          baseUrl: baseUrl || undefined,
        });
        return;
      }
    }
    for (const child of Array.from(el.children)) {
      walk(child);
    }
  };
  if (doc.documentElement) {
    walk(doc.documentElement);
  }
  if (rows.length > 0) {
    return rows.map(normalizeSiteRow);
  }
  const rootName = doc.documentElement ? xmlLocalName(doc.documentElement).toLowerCase() : "";
  if (rootName === "sitelist" || rootName === "sitesummary" || rootName === "sitesummarylist") {
    return [];
  }
  throw new Error("Unexpected site list payload type");
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

/**
 * Catalog list URLs. Primary is public REST {@code /services/sites}. Trailing-slash
 * and sitemanage {@code SiteSummary} are fallbacks when the primary bind is empty
 * or an unreadable envelope (#3368).
 */
function siteListUrls(): string[] {
  return [PATHS.SITES, `${PATHS.SITES}/`, `${PATHS.SITES_ALL}/`];
}

function isSessionRedirect(err: unknown): boolean {
  return (
    !!err &&
    typeof err === "object" &&
    (err as { name?: string }).name === "SessionRedirectError"
  );
}

/** GET /services/sites (sitemanage SiteSummary fallback when that list is empty). */
export async function listSites(): Promise<SiteDef[]> {
  let lastError: unknown = null;
  let sawEmpty = false;
  const seen = new Set<string>();
  for (const url of siteListUrls()) {
    if (seen.has(url)) {
      continue;
    }
    seen.add(url);
    try {
      const payload = await get<unknown>(url);
      try {
        const rows = parseSiteList(payload);
        if (rows.length > 0) {
          return rows.map(withGaps);
        }
        sawEmpty = true;
      } catch (bindErr) {
        lastError = bindErr;
      }
    } catch (httpErr) {
      if (isSessionRedirect(httpErr)) {
        throw httpErr;
      }
      lastError = httpErr;
    }
  }
  if (sawEmpty) {
    return [];
  }
  if (lastError != null) {
    throw lastError;
  }
  return [];
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
    remoteUrl: asNullableString(root.remoteUrl),
    branch: asNullableString(root.branch),
    configFile: asNullableString(root.configFile),
    siteKey: asNullableString(root.siteKey),
    virtual: typeof root.virtual === "boolean" ? root.virtual : undefined,
  };
}

/**
 * PUT body for {@code /services/sites/{nameOrId}/virtual}.
 *
 * <p>Production CXF Jackson uses WRAP/UNWRAP_ROOT_VALUE and JAXB expects root
 * {@code VirtualSiteProperties}. A flat {@code {sourceKind,...}} body is treated
 * as unexpected element {@code sourceKind} (QA #3030 / #3365).
 */
export function toVirtualSitePropertiesEnvelope(
  props: VirtualSiteProperties,
): { VirtualSiteProperties: Record<string, string | null> } {
  const body: Record<string, string | null> = {
    sourceKind: props.sourceKind ?? null,
    rootPath: props.rootPath ?? null,
    configFile: props.configFile ?? null,
    siteKey: props.siteKey ?? null,
  };
  // Omit when undefined so older create/save callers (no remote) stay
  // compatible with servers that do not yet declare the properties.
  // Empty string is intentional: #3568 treats "" as clear, null/omit as keep.
  if (props.remoteUrl !== undefined) {
    body.remoteUrl = props.remoteUrl;
  }
  if (props.branch !== undefined) {
    body.branch = props.branch;
  }
  return {
    VirtualSiteProperties: body,
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
  const payload = await put<unknown>(
    `${PATHS.SITES}/${key}/virtual`,
    toVirtualSitePropertiesEnvelope(props),
  );
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
/**
 * Normalize Virtual Site publish result (Jackson root wrap or plain DTO).
 */
export function parseVirtualSitePublishResult(payload: unknown): VirtualSitePublishResult {
  if (payload == null || typeof payload !== "object") {
    return {};
  }
  const obj = payload as Record<string, unknown>;
  const root =
    (obj.VirtualSitePublishResult as Record<string, unknown> | undefined) ??
    (obj.virtualSitePublishResult as Record<string, unknown> | undefined) ??
    obj;
  return {
    siteName: asNullableString(root.siteName),
    siteKey: asNullableString(root.siteKey),
    publishPath: asNullableString(root.publishPath),
    buildOutputPath: asNullableString(root.buildOutputPath),
    pagesWritten: asNullableNumber(root.pagesWritten),
    filesCopied: asNullableNumber(root.filesCopied),
    linkProblemCount: asNullableNumber(root.linkProblemCount),
    hasLinkProblems:
      typeof root.hasLinkProblems === "boolean" ? root.hasLinkProblems : undefined,
    linkProblems: asStringArray(root.linkProblems),
  };
}

/**
 * POST /services/sites/{nameOrId}/virtual/publish
 *
 * <p>Admin-only. Builds the saved Virtual Site then copies assembled files to
 * the Site filesystem publish root. Traditional repository Sites return 4xx.
 */
export async function publishVirtualSite(nameOrId: string): Promise<VirtualSitePublishResult> {
  const key = encodeURIComponent(nameOrId.trim());
  const payload = await post<unknown>(`${PATHS.SITES}/${key}/virtual/publish`, {});
  return parseVirtualSitePublishResult(payload);
}

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
