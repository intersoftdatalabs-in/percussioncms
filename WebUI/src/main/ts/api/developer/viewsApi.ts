/*
 * Copyright (c) 2026 Intersoft Data Labs, Inc.
 */

import { get } from "../client";
import { normalizeDesignObjectGuid, resolveViewObjectGuid } from "../displayFormatGuid";
import { PATHS } from "../paths";
import type { ViewDef } from "./types";

export { resolveViewObjectGuid };

/**
 * Catalog-level design gaps (REST-GAPS-02). Server omits these on list rows;
 * detail re-attaches or SPA falls back via this constant.
 */
export const VIEW_DESIGN_GAPS: string[] = [
  "View create / update / delete not supported via this API",
  "View field criterion editing not supported via this API",
  "Searches are a separate catalog (Developer Searches / UI-06)",
];

function asArray<T>(payload: unknown): T[] {
  if (payload == null) return [];
  if (Array.isArray(payload)) return payload as T[];
  if (typeof payload === "object") {
    const obj = payload as Record<string, unknown>;
    const raw = obj.ViewDef ?? obj.viewDef ?? obj.ViewDefList;
    if (raw == null) return [];
    return Array.isArray(raw) ? (raw as T[]) : [raw as T];
  }
  return [];
}

function withGaps(v: ViewDef): ViewDef {
  return {
    ...v,
    designGaps:
      v.designGaps && v.designGaps.length > 0 ? v.designGaps : [...VIEW_DESIGN_GAPS],
  };
}

/**
 * Unwrap Jackson WRAP_ROOT_VALUE {@code {"ViewDef":{…}}} and fill
 * {@code guid.stringValue} / {@code guidString} (nested Guid, catalog, or
 * {@code 0-18-{id}}) so Object ACL can bind (#3380).
 */
export function unwrapViewDef(payload: unknown): ViewDef {
  if (payload == null || typeof payload !== "object" || Array.isArray(payload)) {
    return {};
  }
  const root = payload as Record<string, unknown>;
  const nested = root.ViewDef ?? root.viewDef;
  let body: ViewDef;
  if (nested != null && typeof nested === "object" && !Array.isArray(nested)) {
    body = nested as ViewDef;
  } else {
    body = root as ViewDef;
  }
  const gs = resolveViewObjectGuid(body);
  return normalizeDesignObjectGuid(body, gs);
}

/** Unwrap list envelopes and normalize each row GUID (#3380). */
export function unwrapViewDefList(payload: unknown): ViewDef[] {
  return asArray<ViewDef>(payload).map((item) => unwrapViewDef(item));
}

/** GET /services/views — list omits designGaps on the wire (REST-GAPS-02). */
export async function listViews(): Promise<ViewDef[]> {
  const payload = await get<unknown>(PATHS.VIEWS);
  return unwrapViewDefList(payload);
}

/** GET /services/views/{idOrName} */
export async function getViewDetail(idOrName: string): Promise<ViewDef> {
  const key = encodeURIComponent(idOrName);
  const payload = await get<unknown>(`${PATHS.VIEWS}/${key}`);
  return withGaps(unwrapViewDef(payload));
}
