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

import { get } from "../client";
import { PATHS } from "../paths";
import type { DisplayFormat, DisplayFormatColumn } from "./types";

function asArray<T>(payload: unknown): T[] {
  if (payload == null) return [];
  if (Array.isArray(payload)) return payload as T[];
  if (typeof payload === "object") {
    const obj = payload as Record<string, unknown>;
    const raw =
      obj.DisplayFormat ??
      obj.displayFormat ??
      obj.DisplayFormatList ??
      obj.displayFormatList;
    if (raw == null) return [];
    return Array.isArray(raw) ? (raw as T[]) : [raw as T];
  }
  return [];
}

/**
 * Unwrap Jackson {@code WRAP_ROOT_VALUE} envelopes for a single display format.
 *
 * <p>REST {@code JacksonContextResolver} wraps DTOs as
 * {@code {"DisplayFormat":{…}}}. Without unwrapping, detail panels read
 * {@code detail.guid} as undefined and Object ACL shows
 * "Object GUID not available" even though the server supplied a GUID
 * (issue #2689). Flat payloads (no wrap) pass through unchanged.
 */
export function unwrapDisplayFormat(payload: unknown): DisplayFormat {
  if (payload == null) {
    return {};
  }
  if (typeof payload !== "object" || Array.isArray(payload)) {
    return {};
  }
  const root = payload as Record<string, unknown>;
  const nested = root.DisplayFormat ?? root.displayFormat;
  if (nested != null && typeof nested === "object" && !Array.isArray(nested)) {
    return nested as DisplayFormat;
  }
  if (Array.isArray(nested) && nested.length > 0) {
    const first = nested[0];
    if (first != null && typeof first === "object") {
      return first as DisplayFormat;
    }
  }
  // Flat body (WRAP_ROOT_VALUE off or already unwrapped)
  return root as DisplayFormat;
}

export function normalizeColumns(
  columns: DisplayFormat["columns"],
): DisplayFormatColumn[] {
  if (columns == null) return [];
  if (Array.isArray(columns)) return columns;
  const wrapped = columns.DisplayFormatColumn;
  if (wrapped == null) return [];
  return Array.isArray(wrapped) ? wrapped : [wrapped];
}

/** GET /services/displayformats */
export async function listDisplayFormats(): Promise<DisplayFormat[]> {
  const payload = await get<unknown>(PATHS.DISPLAY_FORMATS);
  return asArray<DisplayFormat>(payload);
}

/** GET /services/displayformats/{idOrName} */
export async function getDisplayFormatDetail(idOrName: string): Promise<DisplayFormat> {
  const key = encodeURIComponent(idOrName);
  const payload = await get<unknown>(`${PATHS.DISPLAY_FORMATS}/${key}`);
  return unwrapDisplayFormat(payload);
}
