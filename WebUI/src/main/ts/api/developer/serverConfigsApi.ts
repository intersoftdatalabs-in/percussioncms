/*
 * Copyright (c) 2026 Intersoft Data Labs, Inc.
 */

import { get, put } from "../client";
import { PATHS } from "../paths";
import type { ServerConfigDef } from "./types";

/**
 * Catalog-level design gaps after SY-02 allow-listed PUT ships (REST-GAPS-02).
 * Server omits these on list rows and may still attach them on detail; SPA falls
 * back when the wire array is missing/empty. Stale pre-write "create / update /
 * save" strings are stripped by {@link withoutStaleServerConfigWriteGap}.
 */
export const SERVER_CONFIG_DESIGN_GAPS: string[] = [
  "Configuration create is not supported via this API (fixed allow-listed set only)",
  "Locking and concurrent edit are not exposed on this Developer surface",
];

/** Drop pre-write catalog strings now that Admin PUT save ships. */
const STALE_WRITE_GAP =
  /(?:configuration\s+)?create\s*\/\s*update\s*\/\s*save|update\s*\/\s*save\s+not\s+supported/i;

export type ServerConfigWriteBody = {
  /** File text to persist; empty string is allowed by the REST contract. */
  content: string;
};

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

/** Drop stale REST write-gap strings now that SY-02 SPA save ships. */
export function withoutStaleServerConfigWriteGap(
  gaps: string[] | undefined | null,
): string[] {
  if (gaps == null || gaps.length === 0) return [];
  return gaps.filter((g) => !STALE_WRITE_GAP.test(g));
}

function withGaps(c: ServerConfigDef): ServerConfigDef {
  const fromServer = withoutStaleServerConfigWriteGap(c.designGaps);
  return {
    ...c,
    designGaps: fromServer.length > 0 ? fromServer : [...SERVER_CONFIG_DESIGN_GAPS],
  };
}

/**
 * Unwrap Jackson WRAP_ROOT_VALUE {@code {"ServerConfig":{…}}} so GET/PUT
 * payloads bind the same as a flat ServerConfigDef.
 */
export function unwrapServerConfig(payload: unknown): ServerConfigDef {
  if (payload == null || typeof payload !== "object" || Array.isArray(payload)) {
    throw new Error("Configuration not found or empty response");
  }
  const root = payload as Record<string, unknown>;
  const nested = root.ServerConfig ?? root.serverConfig;
  let body: ServerConfigDef;
  if (nested != null && typeof nested === "object" && !Array.isArray(nested)) {
    body = nested as ServerConfigDef;
  } else {
    body = root as ServerConfigDef;
  }
  if (!body.name || !String(body.name).trim()) {
    throw new Error("Configuration response missing name");
  }
  return body;
}

/** GET /services/serverconfigs — list omits designGaps on the wire (REST-GAPS-02). */
export async function listServerConfigs(): Promise<ServerConfigDef[]> {
  // Do not rehydrate gaps on list rows; detail + withGaps handles honesty for the panel.
  return parseList(await get<unknown>(PATHS.SERVER_CONFIGS));
}

/** GET /services/serverconfigs/{name} */
export async function getServerConfigDetail(name: string): Promise<ServerConfigDef> {
  const key = encodeURIComponent(name);
  const detail = unwrapServerConfig(
    await get<unknown>(`${PATHS.SERVER_CONFIGS}/${key}`),
  );
  return withGaps(detail);
}

/**
 * PUT /services/serverconfigs/{name} — Admin. Replaces the allow-listed file body.
 * Path name is the catalog key (PSConfigurationTypes enum); body must include content.
 */
export async function updateServerConfig(
  name: string,
  body: ServerConfigWriteBody,
): Promise<ServerConfigDef> {
  if (body == null || body.content == null) {
    throw new Error("content is required");
  }
  const key = encodeURIComponent(name);
  const payload = await put<unknown>(`${PATHS.SERVER_CONFIGS}/${key}`, {
    content: body.content,
  });
  return withGaps(unwrapServerConfig(payload));
}
