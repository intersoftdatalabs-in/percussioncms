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

import { normalizeContentTypeFields } from "../api/developer/contentTypeLists";
import type { ContentTypeFieldSummary } from "../api/developer/types";

/** Origins accepted by POST .../fields/include (CD-04). Local create is CD-03. */
export const INCLUDE_FIELD_ORIGINS = ["system", "shared"] as const;

export type IncludeFieldOrigin = (typeof INCLUDE_FIELD_ORIGINS)[number];

export type IncludeFieldCandidate = {
  name: string;
  fieldType: IncludeFieldOrigin;
  label?: string;
};

function asRecord(value: unknown): Record<string, unknown> | null {
  if (value != null && typeof value === "object" && !Array.isArray(value)) {
    return value as Record<string, unknown>;
  }
  return null;
}

/** Parse include origin; anything other than system/shared is rejected. */
export function parseIncludeFieldOrigin(raw: unknown): IncludeFieldOrigin | null {
  const value = String(raw ?? "")
    .trim()
    .toLowerCase();
  if (value === "system" || value === "shared") {
    return value;
  }
  return null;
}

export function fieldNameKey(name: string | undefined | null): string {
  return (name || "").trim().toLowerCase();
}

export function isFieldIncluded(
  fields: ContentTypeFieldSummary[] | undefined,
  name: string,
): boolean {
  const key = fieldNameKey(name);
  if (!key) {
    return false;
  }
  return (fields ?? []).some((f) => fieldNameKey(f.name) === key);
}

/** Origin of an included field on the type (empty when missing). */
export function includedFieldOrigin(
  fields: ContentTypeFieldSummary[] | undefined,
  name: string,
): string {
  const key = fieldNameKey(name);
  if (!key) {
    return "";
  }
  const row = (fields ?? []).find((f) => fieldNameKey(f.name) === key);
  return (row?.fieldType || "").trim().toLowerCase();
}

/**
 * True when a 409 include failure is a missing/stolen lock rather than a
 * duplicate field. Duplicate 409 must keep the held lock.
 */
export function isIncludeLockConflict(err: { status?: number; body?: unknown }): boolean {
  if (err.status !== 409) {
    return false;
  }
  const text =
    typeof err.body === "string"
      ? err.body
      : JSON.stringify(err.body ?? "");
  if (/already included|duplicate/i.test(text)) {
    return false;
  }
  return /design lock|locked by|lock required/i.test(text);
}

function fieldSummariesFrom(raw: unknown): ContentTypeFieldSummary[] {
  return normalizeContentTypeFields(raw).filter((f) => !!fieldNameKey(f.name));
}

/**
 * Flatten GET systemdef / shared-group payloads (WRAP_ROOT or flat) to named
 * field rows for the include picker catalog.
 */
export function extractIncludeCatalogFields(payload: unknown): ContentTypeFieldSummary[] {
  if (payload == null) {
    return [];
  }
  if (Array.isArray(payload)) {
    return fieldSummariesFrom(payload);
  }
  const root = asRecord(payload);
  if (!root) {
    return [];
  }
  const nested = asRecord(
    root.SystemDefDetail ??
      root.systemDefDetail ??
      root.SharedFieldGroupDetail ??
      root.sharedFieldGroupDetail ??
      root.ContentTypeDetail ??
      root.contentTypeDetail,
  );
  const body = nested ?? root;
  if (body.fields != null) {
    return fieldSummariesFrom(body.fields);
  }
  return fieldSummariesFrom(body);
}

export function toIncludeCandidates(
  fields: ContentTypeFieldSummary[],
  origin: IncludeFieldOrigin,
): IncludeFieldCandidate[] {
  const seen = new Set<string>();
  const out: IncludeFieldCandidate[] = [];
  for (const f of fields) {
    const name = (f.name || "").trim();
    const key = fieldNameKey(name);
    if (!key || seen.has(key)) {
      continue;
    }
    seen.add(key);
    out.push({
      name,
      fieldType: origin,
      label: (f.label || "").trim() || undefined,
    });
  }
  return out.sort((a, b) => a.name.localeCompare(b.name, undefined, { sensitivity: "base" }));
}

export function unusedIncludeCandidates(
  candidates: IncludeFieldCandidate[],
  existing: ContentTypeFieldSummary[] | undefined,
): IncludeFieldCandidate[] {
  return candidates.filter((c) => !isFieldIncluded(existing, c.name));
}
