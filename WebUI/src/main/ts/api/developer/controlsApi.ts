/*
 * Copyright (c) 2026 Intersoft Data Labs, Inc.
 */

import { del, get, post, put } from "../client";
import { PATHS } from "../paths";
import type { ControlDef } from "./types";

/**
 * Catalog-level design gaps (REST-GAPS-02). Server omits these on list rows;
 * detail re-attaches or SPA falls back via this constant.
 *
 * <p>Create / save / delete of user controls is SPA chrome (UI-01). Full XSL
 * IDE and system-control mutation remain out of scope.
 */
export const CONTROL_DESIGN_GAPS: string[] = [
  "Control XSL source editing not supported via this API",
  "System controls are read-only packaged defaults",
];

/** Jackson / JAXB root for ControlDef (UNWRAP_ROOT_VALUE on POST). */
export const CONTROL_DEF_ROOT = "ControlDef";

/** REST default dimension on create when omitted. */
export const CONTROL_DIMENSIONS = ["single", "array", "table"] as const;
export type ControlDimension = (typeof CONTROL_DIMENSIONS)[number];

/** REST default choiceSet on create when omitted. */
export const CONTROL_CHOICE_SETS = ["none", "required", "optional"] as const;
export type ControlChoiceSet = (typeof CONTROL_CHOICE_SETS)[number];

/** Matches {@code ControlAdaptor.MAX_CONTROL_NAME_LENGTH}. */
export const MAX_CONTROL_NAME_LENGTH = 100;

/** Matches {@code ControlAdaptor.isSafeControlKey}. */
export const CONTROL_NAME_PATTERN = /^[A-Za-z0-9_.-]+$/;

/**
 * Writable identity fields for POST/PUT /services/cecontrols. Name is the catalog
 * key (not renamed after create). Optional xslSource; omitted uses server default.
 *
 * <p>Save chrome always sends {@code displayName}, {@code description},
 * {@code dimension}, and {@code choiceSet} so a cleared field is persisted
 * (blank description clears; blank dimension/choiceSet send {@code single}/
 * {@code none}). Omitted {@code xslSource} regenerates the default stylesheet.
 */
export type ControlCreateBody = {
  name: string;
  displayName?: string;
  description?: string;
  dimension?: string;
  choiceSet?: string;
  xslSource?: string;
};

/** PUT body is the same wire shape as create (name is the path key, not renamed). */
export type ControlWriteBody = ControlCreateBody;

const LIST_WRAPPER_KEYS = [
  "ControlDef",
  "controlDef",
  "Control",
  "control",
  "controls",
  "entries",
] as const;

/** Drop pre-write catalog strings now that create/save/delete ship. */
const STALE_WRITE_GAP = /(?:create\s*\/\s*)?edit\s*\/\s*delete/i;

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

/** Drop stale REST write-gap strings now that UI-01 create/save/delete ship. */
export function withoutStaleControlWriteGap(gaps: string[] | undefined | null): string[] {
  if (gaps == null || gaps.length === 0) return [];
  return gaps.filter((g) => !STALE_WRITE_GAP.test(g));
}

function withGaps(c: ControlDef): ControlDef {
  const fromServer = withoutStaleControlWriteGap(c.designGaps);
  return {
    ...c,
    designGaps: fromServer.length > 0 ? fromServer : [...CONTROL_DESIGN_GAPS],
  };
}

/**
 * Unwrap Jackson WRAP_ROOT_VALUE {@code {"ControlDef":{…}}} so POST/GET
 * payloads bind the same as a flat ControlDef.
 */
export function unwrapControlDef(payload: unknown): ControlDef {
  if (payload == null || typeof payload !== "object" || Array.isArray(payload)) {
    throw new Error("Control not found or empty response");
  }
  const root = payload as Record<string, unknown>;
  const nested = root.ControlDef ?? root.controlDef;
  let body: ControlDef;
  if (nested != null && typeof nested === "object" && !Array.isArray(nested)) {
    body = nested as ControlDef;
  } else {
    body = root as ControlDef;
  }
  if (!body.name || !String(body.name).trim()) {
    throw new Error("Control response missing name");
  }
  return body;
}

/** ASCII \\s plus Unicode separators (NBSP, ideographic space) and ZWSP. */
function containsWhitespace(value: string): boolean {
  return /[\s\p{Z}\u200B]/u.test(value);
}

/** Trim; empty when missing. */
export function normalizeControlName(name: string | undefined | null): string {
  if (name == null) {
    return "";
  }
  return name.trim();
}

/**
 * True when the name is a legal REST create key: non-blank, no whitespace,
 * no wildcards, ASCII identifier characters, and within length.
 */
export function isValidControlName(name: string | undefined | null): boolean {
  const n = normalizeControlName(name);
  if (!n) {
    return false;
  }
  if (containsWhitespace(n) || n.includes("*") || n.includes("%")) {
    return false;
  }
  if (n.length > MAX_CONTROL_NAME_LENGTH) {
    return false;
  }
  return CONTROL_NAME_PATTERN.test(n);
}

function isBlankOptional(value: string | undefined | null): boolean {
  return value == null || !String(value).trim();
}

function isAllowedDimension(value: string | undefined | null): boolean {
  if (isBlankOptional(value)) {
    return true;
  }
  const v = String(value).trim().toLowerCase();
  return (CONTROL_DIMENSIONS as readonly string[]).includes(v);
}

function isAllowedChoiceSet(value: string | undefined | null): boolean {
  if (isBlankOptional(value)) {
    return true;
  }
  const v = String(value).trim().toLowerCase();
  return (CONTROL_CHOICE_SETS as readonly string[]).includes(v);
}

/** Create Save is enabled when the internal name is valid. Metadata is optional. */
export function isControlCreateReady(opts: {
  name: string;
  dimension?: string;
  choiceSet?: string;
}): boolean {
  return (
    isValidControlName(opts.name) &&
    isAllowedDimension(opts.dimension) &&
    isAllowedChoiceSet(opts.choiceSet)
  );
}

/** PUT Save is enabled when optional dimension / choiceSet values are allowed. */
export function isControlSaveReady(opts: {
  dimension?: string;
  choiceSet?: string;
}): boolean {
  return isAllowedDimension(opts.dimension) && isAllowedChoiceSet(opts.choiceSet);
}

/** Packaged system controls are 409 on PUT/DELETE. */
export function isSystemControl(scope: string | undefined | null): boolean {
  return (scope || "").trim().toLowerCase() === "system";
}

/** Wire JSON for POST/PUT — a flat body fails JAXB root unwrap. */
export function wrapControlCreateForWire(
  body: ControlCreateBody,
): Record<string, ControlCreateBody> {
  return { [CONTROL_DEF_ROOT]: body };
}

/** GET /services/cecontrols — list omits designGaps on the wire (REST-GAPS-02). */
export async function listControls(): Promise<ControlDef[]> {
  const payload = await get<unknown>(PATHS.CE_CONTROLS);
  return parseControlList(payload);
}

/** GET /services/cecontrols/{name} */
export async function getControlDetail(name: string): Promise<ControlDef> {
  const key = encodeURIComponent(name);
  const detail = await get<unknown>(`${PATHS.CE_CONTROLS}/${key}`);
  return withGaps(unwrapControlDef(detail));
}

/**
 * POST /services/cecontrols — Admin. Creates and persists a user CE control.
 * Duplicate name is 409; blank/whitespace/wildcard name is 400; system names
 * are 409; non-Admin is 403.
 */
export async function createControl(body: ControlCreateBody): Promise<ControlDef> {
  const payload = await post<unknown>(PATHS.CE_CONTROLS, wrapControlCreateForWire(body));
  return withGaps(unwrapControlDef(payload));
}

/**
 * PUT /services/cecontrols/{name} — Admin. Updates a user CE control.
 * Omitted {@code xslSource} regenerates the default stylesheet. Unknown is 404;
 * system is 409; non-Admin is 403.
 */
export async function updateControl(
  name: string,
  body: ControlWriteBody,
): Promise<ControlDef> {
  const payload = await put<unknown>(
    `${PATHS.CE_CONTROLS}/${encodeURIComponent(name)}`,
    wrapControlCreateForWire(body),
  );
  return withGaps(unwrapControlDef(payload));
}

/**
 * DELETE /services/cecontrols/{name} — Admin. 204 on success; following GET is
 * 404. Unknown is 404; system is 409; non-Admin is 403.
 */
export async function deleteControl(name: string): Promise<void> {
  await del(`${PATHS.CE_CONTROLS}/${encodeURIComponent(name)}`);
}
