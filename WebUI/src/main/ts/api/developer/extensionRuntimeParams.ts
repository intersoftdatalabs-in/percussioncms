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
 *
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import type { ExtensionMethodParam } from "./types";

/** Default data type for runtime parameters without an explicit type. */
export const DEFAULT_RUNTIME_PARAM_TYPE = "java.lang.String";

export function emptyRuntimeParam(): ExtensionMethodParam {
  return { name: "", dataType: DEFAULT_RUNTIME_PARAM_TYPE, description: "" };
}

function asRuntimeParam(raw: unknown): ExtensionMethodParam | null {
  if (raw == null) return null;
  if (Array.isArray(raw)) return asRuntimeParam(raw[0]);
  if (typeof raw !== "object") return null;
  const o = raw as Record<string, unknown>;
  const nested = o.ExtensionParameter ?? o.extensionParameter ?? o.parameter;
  if (nested != null && nested !== raw) {
    return asRuntimeParam(nested);
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
          : DEFAULT_RUNTIME_PARAM_TYPE,
    description: o.description == null ? "" : String(o.description),
  };
}

/**
 * Normalize GET runtimeParameters wire (array, JAXB single object, or null)
 * into editor rows. Jackson/JAXB unwraps a single-element list to a bare
 * object — without this the panel would show zero rows for one-param
 * extensions and a later Save would silently clear the list.
 */
export function runtimeParamsToRows(
  params: { name?: string; dataType?: string; description?: string }[] | unknown,
): ExtensionMethodParam[] {
  if (params == null) return [];
  const list = Array.isArray(params) ? params : [params];
  const rows: ExtensionMethodParam[] = [];
  for (const raw of list) {
    const row = asRuntimeParam(raw);
    if (row) rows.push(row);
  }
  return rows.map((p) => ({
    name: p.name ?? "",
    dataType: p.dataType ?? DEFAULT_RUNTIME_PARAM_TYPE,
    description: p.description ?? "",
  }));
}

/** Drop blank-name rows; keep named params (trimmed, typed default). */
export function rowsToRuntimeParams(
  rows: ExtensionMethodParam[],
): ExtensionMethodParam[] {
  const out: ExtensionMethodParam[] = [];
  for (const row of rows) {
    const name = (row.name ?? "").trim();
    if (!name) continue;
    out.push({
      name,
      dataType: (row.dataType ?? "").trim() || DEFAULT_RUNTIME_PARAM_TYPE,
      description: row.description ?? "",
    });
  }
  return out;
}

export function runtimeParamsFingerprint(
  rows: ExtensionMethodParam[] | undefined | null,
): string {
  return JSON.stringify(rowsToRuntimeParams(rows ?? []));
}
