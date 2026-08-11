/*
 * Copyright (c) 2026 Intersoft Data Labs, Inc.
 */

import { get } from "../client";
import { PATHS } from "../paths";
import type { ServerConfigDef } from "./types";

/**
 * Catalog-level design gaps (REST-GAPS-02). Server omits these on list rows and may still
 * attach them on detail; SPA falls back when the wire array is missing/empty.
 */
export const SERVER_CONFIG_DESIGN_GAPS: string[] = [
  "Configuration create / update / save not supported via this API",
  "Locking and concurrent edit are not exposed on this Developer surface",
];

function parseList(payload: unknown): ServerConfigDef[] {
  if (payload == null) return [];
  if (Array.isArray(payload)) return payload as ServerConfigDef[];
  if (typeof payload === "object") {
    const obj = payload as Record<string, unknown>;
    for (const key of ["ServerConfig", "serverConfig", "ServerConfigs", "entries"] as const) {
      const raw = obj[key];
      if (raw == null) continue;
      if (Array.isArray(raw)) return raw as ServerConfigDef[];
      if (typeof raw === "object") return [raw as ServerConfigDef];
    }
    throw new Error("Unexpected server config list payload");
  }
  throw new Error("Unexpected server config list payload type");
}

function withGaps(c: ServerConfigDef): ServerConfigDef {
  return {
    ...c,
    designGaps:
      c.designGaps && c.designGaps.length > 0
        ? c.designGaps
        : [...SERVER_CONFIG_DESIGN_GAPS],
  };
}

/** GET /services/serverconfigs — list omits designGaps on the wire (REST-GAPS-02). */
export async function listServerConfigs(): Promise<ServerConfigDef[]> {
  // Do not rehydrate gaps on list rows; detail + withGaps handles honesty for the panel.
  return parseList(await get<unknown>(PATHS.SERVER_CONFIGS));
}

/** GET /services/serverconfigs/{name} */
export async function getServerConfigDetail(name: string): Promise<ServerConfigDef> {
  const key = encodeURIComponent(name);
  const detail = await get<ServerConfigDef | null | undefined>(
    `${PATHS.SERVER_CONFIGS}/${key}`,
  );
  if (detail == null || typeof detail !== "object") {
    throw new Error("Configuration not found or empty response");
  }
  if (!detail.name || !String(detail.name).trim()) {
    throw new Error("Configuration response missing name");
  }
  return withGaps(detail);
}
