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
 * Shared Display Format GUID wire helpers used by both Developer and Content
 * Explorer {@code displayFormatsApi} modules (issues #2689 / #2951).
 *
 * <p>Keep a single implementation so synthesis / nested-Guid rules cannot drift
 * between the two REST clients.
 */

import type { DisplayFormat, RestGuid } from "./developer/types";

/**
 * Extract a usable object GUID string from REST Guid wire shapes.
 *
 * <p>Handles plain strings, {@code { stringValue }}, nested {@code { Guid: … }}
 * wraps, and synthesis from {@code hostId-type-uuid} when {@code stringValue} is
 * missing (issue #2951 residual after #2689 root unwrap).
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
  // Nested root-style wrap (rare): { Guid: { stringValue / parts } }
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
  // Accidental Optional-like object (defensive)
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

/**
 * Ensure {@link DisplayFormat.guid}.stringValue is populated when the wire
 * Guid only carried numeric parts (or was a plain string).
 */
export function normalizeDisplayFormatGuid(df: DisplayFormat): DisplayFormat {
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

/**
 * Unwrap Jackson {@code WRAP_ROOT_VALUE} envelopes for a single display format.
 *
 * <p>REST may wrap DTOs as {@code {"DisplayFormat":{…}}}. Without unwrapping,
 * detail panels read {@code detail.guid} as undefined. Flat payloads pass
 * through. Also normalizes {@code guid.stringValue} when only host/type/uuid
 * parts are present (#2951).
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
    // Flat body (WRAP_ROOT_VALUE off or already unwrapped)
    body = root as DisplayFormat;
  }
  return normalizeDisplayFormatGuid(body);
}
