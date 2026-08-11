/*
 * Copyright (c) 2026 Intersoft Data Labs, Inc.
 */

import { get } from "../client";
import { PATHS } from "../paths";
import type { ControlDef } from "./types";

/**
 * Catalog-level design gaps (REST-GAPS-02). Server omits these on list rows;
 * detail re-attaches or SPA falls back via this constant.
 */
export const CONTROL_DESIGN_GAPS: string[] = [
  "User control create / edit / delete not supported via this API",
  "Control XSL source editing not supported via this API",
  "System controls are read-only packaged defaults",
];

const LIST_WRAPPER_KEYS = [
  "ControlDef",
  "controlDef",
  "Control",
  "control",
  "controls",
  "entries",
] as const;

function isControlDefShape(raw: unknown): raw is ControlDef {
  return (
    typeof raw === "object" &&
    raw != null &&
    "name" in raw &&
    typeof (raw as ControlDef).name === "string"
  );
}

function parseControlList(payload: unknown): ControlDef[] {
  if (payload == null) return [];
  if (Array.isArray(payload)) {
    return payload.filter(isControlDefShape);
  }
  if (typeof payload === "object") {
    const obj = payload as Record<string, unknown>;
    for (const key of LIST_WRAPPER_KEYS) {
      const raw = obj[key];
      if (raw == null) continue;
      if (Array.isArray(raw)) return raw.filter(isControlDefShape);
      if (isControlDefShape(raw)) return [raw];
    }
    throw new Error("Unexpected control list payload");
  }
  throw new Error("Unexpected control list payload type");
}

function withGaps(c: ControlDef): ControlDef {
  return {
    ...c,
    designGaps:
      c.designGaps && c.designGaps.length > 0 ? c.designGaps : [...CONTROL_DESIGN_GAPS],
  };
}

/** GET /services/cecontrols — list omits designGaps on the wire (REST-GAPS-02). */
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
  return withGaps(detail);
}
