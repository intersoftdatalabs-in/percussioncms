/*
 * Copyright (c) 2026 Intersoft Data Labs, Inc.
 */

import { get } from "../client";
import { PATHS } from "../paths";
import type { ActionMenu } from "./types";

function asArray<T>(payload: unknown): T[] {
  if (payload == null) return [];
  if (Array.isArray(payload)) return payload as T[];
  if (typeof payload === "object") {
    const obj = payload as Record<string, unknown>;
    const raw = obj.ActionMenu ?? obj.actionMenu ?? obj.ActionMenuList;
    if (raw == null) return [];
    return Array.isArray(raw) ? (raw as T[]) : [raw as T];
  }
  return [];
}

/** GET /services/actions/catalog */
export async function listActionMenus(): Promise<ActionMenu[]> {
  const payload = await get<unknown>(PATHS.ACTION_MENUS);
  return asArray<ActionMenu>(payload);
}

/** GET /services/actions/catalog/{idOrName} */
export async function getActionMenuDetail(idOrName: string): Promise<ActionMenu> {
  const key = encodeURIComponent(idOrName);
  return get<ActionMenu>(`${PATHS.ACTION_MENUS}/${key}`);
}
