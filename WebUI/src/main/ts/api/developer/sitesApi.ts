/*
 * Copyright (c) 2026 Intersoft Data Labs, Inc.
 */

import { get, post, put } from "../client";
import { PATHS } from "../paths";
import type {
  SiteDef,
  VirtualSiteBuildRequest,
  VirtualSiteBuildResult,
  VirtualSiteProperties,
} from "./types";

/** Honest design gaps for Developer SY-04 site browse (not full site design). */
export const SITE_DESIGN_GAPS: string[] = [
  "Site create / update / delete is not supported from this Developer surface",
  "Full site publish and section design live outside the Developer catalog",
  "Workflow association is browsed under the Workflows catalog",
];

const LIST_WRAPPER_KEYS = ["Site", "site", "SiteList", "sites", "entries"] as const;

function parseSiteList(payload: unknown): SiteDef[] {
  if (payload == null) {
    return [];
  }
  if (Array.isArray(payload)) {
    return payload as SiteDef[];
  }
  if (typeof payload === "object") {
    const obj = payload as Record<string, unknown>;
    for (const key of LIST_WRAPPER_KEYS) {
      const raw = obj[key];
      if (raw == null) continue;
      if (Array.isArray(raw)) {
        return raw as SiteDef[];
      }
      if (typeof raw === "object") {
        return [raw as SiteDef];
      }
    }
    throw new Error("Unexpected site list payload (expected array or known Site wrapper)");
  }
  throw new Error("Unexpected site list payload type");
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
