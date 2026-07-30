/*
 * Copyright 1999-2026 Percussion Software, Inc.
 */

import { get } from "../client";
import { PATHS } from "../paths";
import type { ExtensionDef } from "./types";

function asArray<T>(payload: unknown): T[] {
  if (payload == null) return [];
  if (Array.isArray(payload)) return payload as T[];
  if (typeof payload === "object") {
    const obj = payload as Record<string, unknown>;
    const raw = obj.Extension ?? obj.extension ?? obj.ExtensionList;
    if (raw == null) return [];
    return Array.isArray(raw) ? (raw as T[]) : [raw as T];
  }
  return [];
}

/** GET /services/extensions/catalog */
export async function listExtensions(): Promise<ExtensionDef[]> {
  const payload = await get<unknown>(PATHS.EXTENSIONS);
  return asArray<ExtensionDef>(payload);
}

/** GET /services/extensions/catalog/item?key= */
export async function getExtensionDetail(idOrName: string): Promise<ExtensionDef> {
  const key = encodeURIComponent(idOrName);
  return get<ExtensionDef>(`${PATHS.EXTENSIONS}/item?key=${key}`);
}
