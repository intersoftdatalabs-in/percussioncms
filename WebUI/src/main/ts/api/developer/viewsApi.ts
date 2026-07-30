/*
 * Copyright 1999-2026 Percussion Software, Inc.
 */

import { get } from "../client";
import { PATHS } from "../paths";
import type { ViewDef } from "./types";

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

/** GET /services/views */
export async function listViews(): Promise<ViewDef[]> {
  const payload = await get<unknown>(PATHS.VIEWS);
  return asArray<ViewDef>(payload);
}

/** GET /services/views/{idOrName} */
export async function getViewDetail(idOrName: string): Promise<ViewDef> {
  const key = encodeURIComponent(idOrName);
  return get<ViewDef>(`${PATHS.VIEWS}/${key}`);
}
