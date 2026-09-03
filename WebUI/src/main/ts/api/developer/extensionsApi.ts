/*
 * Copyright (c) 2026 Intersoft Data Labs, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import { del, get, post, put } from "../client";
import { PATHS } from "../paths";
import type { ExtensionDef } from "./types";

/** Jackson/JAXB root for Extension wire payloads. */
export const EXTENSION_ROOT = "Extension";

/** Default handler for Admin registration (matches REST ExtensionAdaptor). */
export const DEFAULT_EXTENSION_HANDLER = "Java";

/** Forced registration context for user extensions. */
export const USER_EXTENSION_CONTEXT = "user/";

/** IPSExtensionDef.INIT_PARAM_CLASSNAME — required for Java handlers. */
export const EXTENSION_CLASSNAME_PARAM = "className";

/** Handler name for handler-owned (immutable) extensions. */
export const EXTENSION_HANDLER_NAME = "ExtensionHandler";

/** Remaining honesty gaps after SY-01 SPA write ships. */
export const EXTENSION_DESIGN_GAPS: string[] = [
  "Workbench parameter dialog parity beyond fields on the wire DTO",
  "Extension method map editing not supported via this chrome",
];

const STALE_WRITE_GAP =
  /extension\s+(install|remove|create|update|delete|register)|parameter and method edit/i;

export type ExtensionWriteBody = {
  extensionName: string;
  handlerName?: string;
  context?: string;
  category?: string;
  supportedInterfaces: string[];
  initParameters?: Record<string, string>;
  runtimeParameters?: ExtensionDef["runtimeParameters"];
  deprecated?: boolean;
  restoreRequestParamsOnError?: boolean;
  version?: number;
  jexlExtension?: boolean;
};

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

/** Unwrap a single Extension from a Jackson root envelope. */
export function unwrapExtension(payload: unknown): ExtensionDef {
  if (payload == null) return {};
  if (typeof payload !== "object" || Array.isArray(payload)) return {};
  const obj = payload as Record<string, unknown>;
  const nested = obj.Extension ?? obj.extension;
  if (nested != null && typeof nested === "object" && !Array.isArray(nested)) {
    return nested as ExtensionDef;
  }
  return payload as ExtensionDef;
}

/** True when context is system (`global/percussion/…`) or handler-owned. */
export function isImmutableExtensionContext(context: string | undefined | null): boolean {
  if (context == null || !context.trim()) return false;
  let c = context.trim().toLowerCase();
  if (!c.endsWith("/")) c = `${c}/`;
  return c.startsWith("global/percussion/") || c.startsWith("handlers/");
}

/**
 * True when the extension cannot be mutated via Admin REST (system or
 * handler-owned). Matches ExtensionAdaptor.isImmutableExtension.
 */
export function isImmutableExtension(
  ext: Pick<ExtensionDef, "context" | "handlerName"> | null | undefined,
): boolean {
  if (ext == null) return false;
  const handler = (ext.handlerName || "").trim();
  // Fail closed: blank handler on a catalog row — treat as immutable (matches ExtensionAdaptor).
  if (!handler) return true;
  if (handler.toLowerCase() === EXTENSION_HANDLER_NAME.toLowerCase()) {
    return true;
  }
  return isImmutableExtensionContext(ext.context);
}

/** Trim an extension name for write. Empty / null becomes "". */
export function normalizeExtensionName(name: string | undefined | null): string {
  return name == null ? "" : name.trim();
}

/**
 * True when the name matches PSExtensionRef.isValidExtensionName
 * (Character.isJavaIdentifierStart / Part). Uses Unicode property escapes so
 * non-ASCII letters are accepted client-side the same way the server accepts them.
 */
export function isValidExtensionName(name: string | undefined | null): boolean {
  const key = normalizeExtensionName(name);
  if (!key) return false;
  // ID_Start + $_ for start; ID_Continue + $ for continuation (Java also allows $).
  return /^[\p{ID_Start}_$][\p{ID_Continue}$]*$/u.test(key);
}

/** Parse interfaces from a textarea (one per line; blank lines skipped). */
export function parseExtensionInterfaces(text: string | undefined | null): string[] {
  if (text == null || !text.trim()) return [];
  return text
    .split(/\r?\n/)
    .map((line) => line.trim())
    .filter((line) => line.length > 0);
}

/**
 * Coerce wire supportedInterfaces to string[]. Jackson/JAXB often emits a bare
 * string when the list has a single element — that used to throw in
 * {@link formatExtensionInterfaces} and abort Extension detail load (#4241).
 */
export function normalizeSupportedInterfaces(
  raw: string[] | string | undefined | null | unknown,
): string[] {
  if (raw == null) return [];
  if (typeof raw === "string") {
    const t = raw.trim();
    return t ? [t] : [];
  }
  if (!Array.isArray(raw)) {
    // Unexpected object wrapper — do not invent entries; callers treat [] as empty.
    return [];
  }
  return raw
    .map((s) => (s == null ? "" : String(s).trim()))
    .filter((s) => s.length > 0);
}

/** Join interfaces for the editor textarea. */
export function formatExtensionInterfaces(
  ifaces: string[] | string | undefined | null,
): string {
  const list = normalizeSupportedInterfaces(ifaces);
  return list.length === 0 ? "" : list.join("\n");
}

/**
 * Flatten Jackson map wire `{ entry: [{ key, value }, ...] }` (or a plain
 * object) into a string record for initParameters.
 */
export function normalizeInitParameters(
  raw: unknown,
): Record<string, string> | undefined {
  if (raw == null || typeof raw !== "object") return undefined;
  const obj = raw as Record<string, unknown>;
  if (Array.isArray(obj.entry)) {
    const out: Record<string, string> = {};
    for (const row of obj.entry) {
      if (row == null || typeof row !== "object") continue;
      const e = row as Record<string, unknown>;
      if (e.key == null) continue;
      out[String(e.key)] = e.value == null ? "" : String(e.value);
    }
    return out;
  }
  const out: Record<string, string> = {};
  for (const [k, v] of Object.entries(obj)) {
    if (v != null && typeof v === "object") continue;
    out[k] = v == null ? "" : String(v);
  }
  return out;
}

/** Read className from initParameters (Java handlers). */
export function extensionClassName(
  initParameters: Record<string, string> | unknown | undefined | null,
): string {
  const map = normalizeInitParameters(initParameters) ?? {};
  const raw = map[EXTENSION_CLASSNAME_PARAM];
  return raw == null ? "" : String(raw).trim();
}

/**
 * Save is enabled when create fields are valid, or when editing a loaded
 * mutable extension with at least one interface.
 */
export function isExtensionWriteReady(opts: {
  isNew: boolean;
  name: string;
  interfaces: string[];
  className: string;
  handlerName?: string;
  immutable?: boolean;
}): boolean {
  if (opts.immutable) return false;
  if (opts.interfaces.length === 0) return false;
  const handler = (opts.handlerName || DEFAULT_EXTENSION_HANDLER).trim() || DEFAULT_EXTENSION_HANDLER;
  if (handler.toLowerCase() === "java" && !opts.className.trim()) return false;
  if (opts.isNew) return isValidExtensionName(opts.name);
  return Boolean(normalizeExtensionName(opts.name));
}

/**
 * Jackson/JAXB Map&lt;String,String&gt; wire shape used by Extension.initParameters
 * on this stack (flat `{ className: "…" }` does not bind; entry list does).
 */
export type JacksonMapEntry = { key: string; value: string };
export type JacksonStringMapWire = { entry: JacksonMapEntry[] };

/** Convert a flat string map to the Jackson/JAXB entry-list map wire form. */
export function toJacksonStringMapWire(
  map: Record<string, string> | undefined | null,
): JacksonStringMapWire | undefined {
  if (map == null) return undefined;
  const entry: JacksonMapEntry[] = Object.entries(map).map(([key, value]) => ({
    key,
    value: value == null ? "" : String(value),
  }));
  return { entry };
}

/** Wire JSON for POST/PUT — Extension root + entry-list initParameters. */
export function wrapExtensionForWire(
  body: ExtensionWriteBody,
): Record<string, unknown> {
  const { initParameters, ...rest } = body;
  const wire: Record<string, unknown> = { ...rest };
  const mapped = toJacksonStringMapWire(initParameters);
  if (mapped) {
    wire.initParameters = mapped;
  }
  return { [EXTENSION_ROOT]: wire };
}

/** Drop stale REST write-gap strings now that SY-01 SPA write ships. */
export function withoutStaleExtensionWriteGap(gaps: string[] | undefined | null): string[] {
  if (gaps == null || gaps.length === 0) return [];
  return gaps.filter((g) => !STALE_WRITE_GAP.test(g));
}

function withGaps(ext: ExtensionDef): ExtensionDef {
  const fromServer = withoutStaleExtensionWriteGap(ext.designGaps);
  const initParameters = normalizeInitParameters(ext.initParameters);
  return {
    ...ext,
    supportedInterfaces: normalizeSupportedInterfaces(
      ext.supportedInterfaces as string[] | string | undefined | null,
    ),
    ...(initParameters ? { initParameters } : {}),
    designGaps: fromServer.length > 0 ? fromServer : EXTENSION_DESIGN_GAPS,
  };
}

/** GET /services/extensions/catalog */
export async function listExtensions(): Promise<ExtensionDef[]> {
  const payload = await get<unknown>(PATHS.EXTENSIONS);
  return asArray<ExtensionDef>(payload).map(withGaps);
}

/** GET /services/extensions/catalog/item?key= */
export async function getExtensionDetail(idOrName: string): Promise<ExtensionDef> {
  const key = encodeURIComponent(idOrName);
  const payload = await get<unknown>(`${PATHS.EXTENSIONS}/item?key=${key}`);
  return withGaps(unwrapExtension(payload));
}

/** POST /services/extensions — Admin. Registers a user extension under user/. */
export async function createExtension(body: ExtensionWriteBody): Promise<ExtensionDef> {
  const payload = await post<unknown>(PATHS.EXTENSIONS_ROOT, wrapExtensionForWire(body));
  return withGaps(unwrapExtension(payload));
}

/** PUT /services/extensions/catalog/item?key= — Admin. Identity is not renamed. */
export async function saveExtension(
  idOrName: string,
  body: ExtensionWriteBody,
): Promise<ExtensionDef> {
  const key = encodeURIComponent(idOrName);
  const payload = await put<unknown>(
    `${PATHS.EXTENSIONS}/item?key=${key}`,
    wrapExtensionForWire(body),
  );
  return withGaps(unwrapExtension(payload));
}

/** DELETE /services/extensions/catalog/item?key= — Admin. 204 on success. */
export async function deleteExtension(idOrName: string): Promise<void> {
  const key = encodeURIComponent(idOrName);
  await del(`${PATHS.EXTENSIONS}/item?key=${key}`);
}
