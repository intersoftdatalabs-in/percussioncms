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

import { EXTENSION_CLASSNAME_PARAM, normalizeInitParameters } from "./extensionsApi";

/** IPSExtensionDef.INIT_PARAM_VERSION — owned by the version field, not the dialog. */
export const EXTENSION_VERSION_PARAM = "com.percussion.extension.version";

export type InitParamRow = { key: string; value: string };

export function emptyInitParamRow(): InitParamRow {
  return { key: "", value: "" };
}

/** Keys edited on the main form, not in the Workbench-parity init-param dialog. */
export function isReservedInitParamKey(key: string | undefined | null): boolean {
  const k = (key ?? "").trim();
  if (!k) return false;
  return (
    k === EXTENSION_CLASSNAME_PARAM ||
    k === EXTENSION_VERSION_PARAM ||
    k === "version"
  );
}

/** Dialog rows from GET initParameters (reserved keys omitted). */
export function initParamsToRows(raw: unknown): InitParamRow[] {
  const map = normalizeInitParameters(raw) ?? {};
  const rows: InitParamRow[] = [];
  for (const [key, value] of Object.entries(map)) {
    if (isReservedInitParamKey(key)) continue;
    rows.push({ key, value: value ?? "" });
  }
  return rows;
}

/**
 * Named rows only. Duplicate keys keep the last trimmed value.
 */
export function rowsToInitParams(rows: InitParamRow[]): Record<string, string> {
  const out: Record<string, string> = {};
  for (const row of rows) {
    const key = (row.key ?? "").trim();
    if (!key || isReservedInitParamKey(key)) continue;
    out[key] = row.value ?? "";
  }
  return out;
}

/**
 * Full PUT map: reserved className/version plus dialog rows. Keys that were on
 * GET but removed in the dialog are set to null so REST mergeInitParams deletes them.
 */
export function mergeInitParametersForWrite(opts: {
  previous: Record<string, string> | undefined | null;
  className: string;
  rows: InitParamRow[];
}): Record<string, string | null> {
  const previous = opts.previous ?? {};
  const next: Record<string, string | null> = {};
  const fromRows = rowsToInitParams(opts.rows);
  for (const [k, v] of Object.entries(fromRows)) {
    next[k] = v;
  }
  const className = opts.className.trim();
  if (className) {
    next[EXTENSION_CLASSNAME_PARAM] = className;
  }
  if (previous[EXTENSION_VERSION_PARAM] != null) {
    next[EXTENSION_VERSION_PARAM] = previous[EXTENSION_VERSION_PARAM];
  }
  // Omitted extra keys are dropped server-side (PUT initParameters is a replace).
  return next;
}

export function initParamsFingerprint(rows: InitParamRow[] | undefined | null): string {
  return JSON.stringify(rowsToInitParams(rows ?? []));
}
