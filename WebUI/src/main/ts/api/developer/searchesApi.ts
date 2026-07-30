/*
 * Copyright 1999-2026 Percussion Software, Inc.
 */

import { get } from "../client";
import { PATHS } from "../paths";
import type { SearchDef } from "./types";

function asArray<T>(payload: unknown): T[] {
  if (payload == null) return [];
  if (Array.isArray(payload)) return payload as T[];
  if (typeof payload === "object") {
    const obj = payload as Record<string, unknown>;
    const raw = obj.SearchDef ?? obj.searchDef ?? obj.SearchDefList;
    if (raw == null) return [];
    return Array.isArray(raw) ? (raw as T[]) : [raw as T];
  }
  return [];
}

/** GET /services/searches */
export async function listSearches(): Promise<SearchDef[]> {
  const payload = await get<unknown>(PATHS.SEARCHES);
  return asArray<SearchDef>(payload);
}

/** GET /services/searches/{idOrName} */
export async function getSearchDetail(idOrName: string): Promise<SearchDef> {
  const key = encodeURIComponent(idOrName);
  return get<SearchDef>(`${PATHS.SEARCHES}/${key}`);
}
