/*
 * Copyright 1999-2026 Percussion Software, Inc.
 */

import { get } from "../client";
import { PATHS } from "../paths";
import type { ControlDef } from "./types";

const LIST_WRAPPER_KEYS = [
  "ControlDef",
  "controlDef",
  "Control",
  "control",
  "controls",
  "entries",
] as const;

function parseControlList(payload: unknown): ControlDef[] {
  if (payload == null) return [];
  if (Array.isArray(payload)) return payload as ControlDef[];
  if (typeof payload === "object") {
    const obj = payload as Record<string, unknown>;
    for (const key of LIST_WRAPPER_KEYS) {
      const raw = obj[key];
      if (raw == null) continue;
      if (Array.isArray(raw)) return raw as ControlDef[];
      if (typeof raw === "object") return [raw as ControlDef];
    }
    throw new Error("Unexpected control list payload");
  }
  throw new Error("Unexpected control list payload type");
}

/** GET /services/cecontrols */
export async function listControls(): Promise<ControlDef[]> {
  const payload = await get<unknown>(PATHS.CE_CONTROLS);
  return parseControlList(payload);
}

/** GET /services/cecontrols/{name} */
export async function getControlDetail(name: string): Promise<ControlDef> {
  const key = encodeURIComponent(name);
  const detail = await get<ControlDef | null | undefined>(`${PATHS.CE_CONTROLS}/${key}`);
  if (detail == null || typeof detail !== "object") {
    throw new Error("Control not found or empty response");
  }
  if (!detail.name || !String(detail.name).trim()) {
    throw new Error("Control response missing name");
  }
  return detail;
}
