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

/**
 * Typed client for the public REST display-format catalog used by the
 * modern Content Explorer list (#2400 / FR-027).
 *
 * <p>Provider: {@code rest} {@code DisplayFormatResource} at
 * {@code GET /Rhythmyx/rest/displayformats}. Optional query filters
 * {@code validForFolder} and {@code validForViewsAndSearches} are applied
 * server-side.</p>
 */

import { get } from "../client";
import { PATHS } from "../paths";
import type { DisplayFormat, DisplayFormatColumn, RestGuid } from "../developer/types";

export type { DisplayFormat, DisplayFormatColumn };

function asArray(payload: unknown): DisplayFormat[] {
  if (payload == null) return [];
  let rawList: unknown[] = [];
  if (Array.isArray(payload)) {
    rawList = payload;
  } else if (typeof payload === "object") {
    const obj = payload as Record<string, unknown>;
    const raw =
      obj.DisplayFormat ??
      obj.displayFormat ??
      obj.DisplayFormatList ??
      obj.displayFormatList;
    if (raw == null) return [];
    rawList = Array.isArray(raw) ? raw : [raw];
  } else {
    return [];
  }
  return rawList.map((item) => unwrapDisplayFormat(item));
}

/**
 * Extract a usable object GUID string from REST Guid wire shapes
 * (shared behavior with developer displayFormatsApi — issues #2689 / #2951).
 */
export function objectGuidString(guid: unknown): string | undefined {
  if (guid == null) {
    return undefined;
  }
  if (typeof guid === "string") {
    const trimmed = guid.trim();
    return trimmed || undefined;
  }
  if (typeof guid !== "object" || Array.isArray(guid)) {
    return undefined;
  }

  let g = guid as Record<string, unknown>;
  const nestedGuid = g.Guid ?? g.guid;
  if (
    nestedGuid != null &&
    typeof nestedGuid === "object" &&
    !Array.isArray(nestedGuid) &&
    g.stringValue == null &&
    g.hostId == null &&
    g.uuid == null
  ) {
    g = nestedGuid as Record<string, unknown>;
  }

  const sv = g.stringValue;
  if (typeof sv === "string" && sv.trim()) {
    return sv.trim();
  }
  if (sv != null && typeof sv === "object" && !Array.isArray(sv)) {
    const opt = sv as Record<string, unknown>;
    if (typeof opt.value === "string" && opt.value.trim()) {
      return opt.value.trim();
    }
  }

  const hostId = g.hostId;
  const type = g.type;
  const uuid = g.uuid;
  const hostOk = typeof hostId === "number" || typeof hostId === "string";
  const typeOk = typeof type === "number" || typeof type === "string";
  const uuidOk = typeof uuid === "number" || typeof uuid === "string";
  if (hostOk && typeOk && uuidOk) {
    return `${hostId}-${type}-${uuid}`;
  }

  return undefined;
}

function normalizeDisplayFormatGuid(df: DisplayFormat): DisplayFormat {
  const gs = objectGuidString(df.guid);
  if (!gs) {
    return df;
  }
  if (df.guid != null && typeof df.guid === "object" && !Array.isArray(df.guid)) {
    const existing = df.guid as RestGuid;
    if (existing.stringValue === gs) {
      return df;
    }
    return { ...df, guid: { ...existing, stringValue: gs } };
  }
  return { ...df, guid: { stringValue: gs } };
}

export function normalizeDisplayFormatColumns(
  columns: DisplayFormat["columns"],
): DisplayFormatColumn[] {
  if (columns == null) return [];
  if (Array.isArray(columns)) return columns;
  const wrapped = columns.DisplayFormatColumn;
  if (wrapped == null) return [];
  return Array.isArray(wrapped) ? wrapped : [wrapped];
}

export interface ListDisplayFormatsParams {
  validForFolder?: boolean;
  validForViewsAndSearches?: boolean;
}

/** GET /services/displayformats with optional Explorer filters. */
export async function listDisplayFormats(
  params: ListDisplayFormatsParams = {},
): Promise<DisplayFormat[]> {
  const q = new URLSearchParams();
  if (params.validForFolder !== undefined) {
    q.set("validForFolder", String(params.validForFolder));
  }
  if (params.validForViewsAndSearches !== undefined) {
    q.set("validForViewsAndSearches", String(params.validForViewsAndSearches));
  }
  const qs = q.toString();
  const payload = await get<unknown>(
    `${PATHS.DISPLAY_FORMATS}${qs ? `?${qs}` : ""}`,
  );
  return asArray(payload);
}

/**
 * Unwrap Jackson root wrap for a single display format
 * ({@code {"DisplayFormat":{…}}}) so {@code guid.stringValue} is reachable
 * (issue #2689). Flat payloads pass through. Also synthesizes stringValue from
 * host/type/uuid when the wire Guid omitted stringValue (#2951).
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
  let body: DisplayFormat;
  if (nested != null && typeof nested === "object" && !Array.isArray(nested)) {
    body = nested as DisplayFormat;
  } else if (Array.isArray(nested) && nested.length > 0) {
    const first = nested[0];
    if (first != null && typeof first === "object") {
      body = first as DisplayFormat;
    } else {
      body = root as DisplayFormat;
    }
  } else {
    body = root as DisplayFormat;
  }
  return normalizeDisplayFormatGuid(body);
}

/** GET /services/displayformats/{idOrName} */
export async function getDisplayFormatDetail(
  idOrName: string,
): Promise<DisplayFormat> {
  const key = encodeURIComponent(idOrName);
  const payload = await get<unknown>(`${PATHS.DISPLAY_FORMATS}/${key}`);
  return unwrapDisplayFormat(payload);
}
