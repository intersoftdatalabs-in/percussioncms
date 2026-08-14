/*
 * Copyright (c) 2026 Intersoft Data Labs, Inc.
 */

import { get } from "../client";
import {
  normalizeDesignObjectGuid,
  resolveActionMenuObjectGuid,
} from "../displayFormatGuid";
import { PATHS } from "../paths";
import type { ActionMenu } from "./types";

export { resolveActionMenuObjectGuid };

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

/**
 * Unwrap Jackson WRAP_ROOT_VALUE {@code {"ActionMenu":{…}}} and fill
 * {@code guid.stringValue} / {@code guidString} (nested Guid, catalog, or
 * {@code 0-107-{id}}) so Object ACL can bind (#3380).
 */
export function unwrapActionMenu(payload: unknown): ActionMenu {
  if (payload == null || typeof payload !== "object" || Array.isArray(payload)) {
    return {};
  }
  const root = payload as Record<string, unknown>;
  const nested = root.ActionMenu ?? root.actionMenu;
  let body: ActionMenu;
  if (nested != null && typeof nested === "object" && !Array.isArray(nested)) {
    body = nested as ActionMenu;
  } else {
    body = root as ActionMenu;
  }
  const gs = resolveActionMenuObjectGuid(body);
  return normalizeDesignObjectGuid(body, gs);
}

/** Unwrap list envelopes and normalize each row GUID (#3380). */
export function unwrapActionMenuList(payload: unknown): ActionMenu[] {
  return asArray<ActionMenu>(payload).map((item) => unwrapActionMenu(item));
}

/** GET /services/actions/catalog */
export async function listActionMenus(): Promise<ActionMenu[]> {
  const payload = await get<unknown>(PATHS.ACTION_MENUS);
  return unwrapActionMenuList(payload);
}

/** GET /services/actions/catalog/{idOrName} */
export async function getActionMenuDetail(idOrName: string): Promise<ActionMenu> {
  const key = encodeURIComponent(idOrName);
  const payload = await get<unknown>(`${PATHS.ACTION_MENUS}/${key}`);
  return unwrapActionMenu(payload);
}
