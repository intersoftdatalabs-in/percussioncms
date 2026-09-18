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

import type { ExtensionMethodDef, ExtensionMethodParam } from "./types";

/** Matches ExtensionAdaptor.DEFAULT_METHOD_RETURN_TYPE. */
export const DEFAULT_METHOD_RETURN_TYPE = "java.lang.Object";

export function emptyMethod(): ExtensionMethodDef {
  return {
    name: "",
    returnType: DEFAULT_METHOD_RETURN_TYPE,
    description: "",
    parameters: [],
  };
}

export function emptyMethodParam(): ExtensionMethodParam {
  return { name: "", dataType: "java.lang.String", description: "" };
}

function asParam(raw: unknown): ExtensionMethodParam | null {
  if (raw == null) return null;
  if (Array.isArray(raw)) return null;
  if (typeof raw !== "object") return null;
  const o = raw as Record<string, unknown>;
  const nested = o.ExtensionParameter ?? o.extensionParameter ?? o.parameter;
  if (nested != null && nested !== raw) {
    if (Array.isArray(nested)) {
      return asParam(nested[0]);
    }
    return asParam(nested);
  }
  const name = o.name == null ? "" : String(o.name);
  if (!name.trim()) return null;
  return {
    name,
    dataType:
      o.dataType != null
        ? String(o.dataType)
        : o.type != null
          ? String(o.type)
          : "java.lang.String",
    description: o.description == null ? "" : String(o.description),
  };
}

function asMethod(raw: unknown, fallbackName: string): ExtensionMethodDef | null {
  if (raw == null) return null;
  if (Array.isArray(raw)) return asMethod(raw[0], fallbackName);
  if (typeof raw !== "object") return null;
  const o = raw as Record<string, unknown>;
  const nested = o.ExtensionMethod ?? o.extensionMethod ?? o.method;
  if (nested != null && nested !== raw) {
    return asMethod(nested, fallbackName);
  }
  const name = String(o.name ?? fallbackName ?? "").trim();
  if (!name) return null;
  // Parameter-shaped rows (dataType, no returnType) are not methods.
  if (
    (o.dataType != null || o.type != null) &&
    o.returnType == null &&
    o.parameters == null &&
    fallbackName !== name
  ) {
    return null;
  }
  const paramsRaw = o.parameters ?? o.PSExtensionMethodParam;
  const parameters: ExtensionMethodParam[] = [];
  const paramList = Array.isArray(paramsRaw)
    ? paramsRaw
    : paramsRaw != null && typeof paramsRaw === "object"
      ? [paramsRaw]
      : [];
  for (const p of paramList) {
    const param = asParam(p);
    if (param) parameters.push(param);
  }
  return {
    name,
    returnType:
      o.returnType == null || String(o.returnType).trim() === ""
        ? DEFAULT_METHOD_RETURN_TYPE
        : String(o.returnType).trim(),
    description: o.description == null ? "" : String(o.description),
    parameters,
  };
}

/**
 * Flatten Jackson/JAXB method map wire (`{ entry: [{ key, value }] }` or a plain
 * object) into a name-keyed record.
 */
export function normalizeMethods(raw: unknown): Record<string, ExtensionMethodDef> {
  const out: Record<string, ExtensionMethodDef> = {};
  if (raw == null) return out;
  if (Array.isArray(raw)) {
    for (const row of raw) {
      const meth = asMethod(row, "");
      if (meth) out[meth.name as string] = meth;
    }
    return out;
  }
  if (typeof raw !== "object") return out;
  const obj = raw as Record<string, unknown>;
  // JAXB unwraps a single-element methods list to a bare ExtensionMethod object.
  if (typeof obj.name === "string" && (obj.returnType != null || obj.parameters != null)) {
    const meth = asMethod(obj, String(obj.name));
    if (meth) out[meth.name as string] = meth;
    return out;
  }
  if (obj.ExtensionMethod != null || obj.method != null) {
    const wrapped = obj.ExtensionMethod ?? obj.method;
    const rows = Array.isArray(wrapped) ? wrapped : [wrapped];
    for (const row of rows) {
      const meth = asMethod(row, "");
      if (meth) out[meth.name as string] = meth;
    }
    return out;
  }
  if (Array.isArray(obj.entry)) {
    for (const row of obj.entry) {
      if (row == null || typeof row !== "object") continue;
      const e = row as Record<string, unknown>;
      const key = e.key == null ? "" : String(e.key);
      const meth = asMethod(e.value, key);
      if (meth) out[meth.name as string] = meth;
    }
    return out;
  }
  for (const [k, v] of Object.entries(obj)) {
    const meth = asMethod(v, k);
    if (meth) out[meth.name as string] = meth;
  }
  return out;
}

export function methodsToRows(
  methods: Record<string, ExtensionMethodDef> | ExtensionMethodDef[] | undefined | null,
): ExtensionMethodDef[] {
  if (methods == null) return [];
  const values = Array.isArray(methods) ? methods : Object.values(methods);
  return values.map((m) => ({
    name: m.name ?? "",
    returnType: m.returnType?.trim() || DEFAULT_METHOD_RETURN_TYPE,
    description: m.description ?? "",
    parameters: (m.parameters ?? []).map((p) => ({
      name: p.name ?? "",
      dataType: p.dataType ?? "java.lang.String",
      description: p.description ?? "",
    })),
  }));
}

/** Drop blank-name rows; keep named methods (including empty params). */
export function rowsToMethods(rows: ExtensionMethodDef[]): Record<string, ExtensionMethodDef> {
  const out: Record<string, ExtensionMethodDef> = {};
  for (const row of rows) {
    const name = (row.name ?? "").trim();
    if (!name) continue;
    out[name] = {
      name,
      returnType: (row.returnType ?? "").trim() || DEFAULT_METHOD_RETURN_TYPE,
      description: row.description ?? "",
      parameters: (row.parameters ?? [])
        .map((p) => ({
          name: (p.name ?? "").trim(),
          dataType: (p.dataType ?? "").trim() || "java.lang.String",
          description: p.description ?? "",
        }))
        .filter((p) => p.name.length > 0),
    };
  }
  return out;
}

export function methodsFingerprint(rows: ExtensionMethodDef[] | undefined | null): string {
  return JSON.stringify(rowsToMethods(rows ?? []));
}

export type JacksonMethodMapWire = {
  entry: Array<{ key: string; value: ExtensionMethodDef }>;
};

/** Convert a method map to the Jackson/JAXB entry-list map wire form. */
export function toJacksonMethodMapWire(
  map: Record<string, ExtensionMethodDef> | undefined | null,
): JacksonMethodMapWire | undefined {
  if (map == null) return undefined;
  const entry: Array<{ key: string; value: ExtensionMethodDef }> = Object.entries(map).map(
    ([key, value]) => ({
      key,
      value,
    }),
  );
  return { entry };
}
