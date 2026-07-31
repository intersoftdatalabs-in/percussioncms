/*
 * Copyright 1999-2026 Percussion Software, Inc.
 */

import { get } from "../client";
import { PATHS } from "../paths";
import type { ServerConfigDef } from "./types";

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

/** GET /services/serverconfigs */
export async function listServerConfigs(): Promise<ServerConfigDef[]> {
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
  return detail;
}
