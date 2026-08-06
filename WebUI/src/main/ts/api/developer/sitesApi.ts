/*
 * Copyright (c) 2026 Intersoft Data Labs, Inc.
 */

import { get } from "../client";
import { PATHS } from "../paths";
import type { SiteDef } from "./types";

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
