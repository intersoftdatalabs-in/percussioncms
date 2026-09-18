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

/**
 * Client-side required-field checks and mapping of save HTTP 400 bodies onto
 * named EditorHost rows. Pure so Vitest can cover without mounting the host.
 */

import {
  formatApiError,
  isApiError,
  isSessionRedirectError,
} from "../api/client";
import type { EditorWidgetKind } from "./controlKinds";
import { isInvalidEditorDate, type EditorDateKind } from "./dateField";

export interface EditorRequiredRow {
  name: string;
  kind: EditorWidgetKind;
  required: boolean;
  value: string;
}

export interface MappedSaveFieldErrors {
  fieldErrors: Record<string, string>;
  banner: string;
}

function asRecord(value: unknown): Record<string, unknown> | null {
  if (value != null && typeof value === "object" && !Array.isArray(value)) {
    return value as Record<string, unknown>;
  }
  return null;
}

function unwrapErrorRoot(body: unknown): Record<string, unknown> | null {
  const root = asRecord(body);
  if (!root) {
    return null;
  }
  return (
    asRecord(root.Error) ??
    asRecord(root.error) ??
    asRecord(root.RestError) ??
    asRecord(root.restError) ??
    root
  );
}

export function isEmptyEditorFieldValue(
  kind: EditorWidgetKind,
  value: string,
  pendingFile?: File | null,
): boolean {
  if (kind === "file" || kind === "image") {
    return !pendingFile && !(value ?? "").trim();
  }
  return !(value ?? "").trim();
}

export function collectRequiredFieldErrors(
  rows: readonly EditorRequiredRow[],
  pendingFiles: Record<string, File>,
  requiredMessage: string,
): Record<string, string> {
  const out: Record<string, string> = {};
  for (const row of rows) {
    if (!row.required) {
      continue;
    }
    if (isEmptyEditorFieldValue(row.kind, row.value, pendingFiles[row.name])) {
      out[row.name] = requiredMessage;
    }
  }
  return out;
}

function isDateKind(kind: EditorWidgetKind): kind is EditorDateKind {
  return kind === "date" || kind === "datetime";
}

export function collectInvalidDateFieldErrors(
  rows: readonly EditorRequiredRow[],
  invalidMessage: string,
): Record<string, string> {
  const out: Record<string, string> = {};
  for (const row of rows) {
    if (!isDateKind(row.kind)) {
      continue;
    }
    if (isInvalidEditorDate(row.kind, row.value)) {
      out[row.name] = invalidMessage;
    }
  }
  return out;
}

function knownNameSet(names: readonly string[]): Map<string, string> {
  const map = new Map<string, string>();
  for (const name of names) {
    const trimmed = name.trim();
    if (trimmed) {
      map.set(trimmed.toLowerCase(), trimmed);
    }
  }
  return map;
}

function matchKnownField(
  raw: unknown,
  known: Map<string, string>,
): string | undefined {
  if (typeof raw !== "string" && typeof raw !== "number") {
    return undefined;
  }
  const key = String(raw).trim().toLowerCase();
  return known.get(key);
}

function fieldFromExplicitKeys(
  rec: Record<string, unknown>,
  known: Map<string, string>,
): string | undefined {
  for (const key of ["field", "fieldName", "name", "property", "path"]) {
    const hit = matchKnownField(rec[key], known);
    if (hit) {
      return hit;
    }
  }
  return undefined;
}

function fieldFromErrorData(
  errorData: unknown,
  known: Map<string, string>,
): string | undefined {
  const direct = matchKnownField(errorData, known);
  if (direct) {
    return direct;
  }
  const rec = asRecord(errorData);
  if (!rec) {
    return undefined;
  }
  return fieldFromExplicitKeys(rec, known) ?? matchKnownField(rec.errorData, known);
}

function collectMapEntries(
  rec: Record<string, unknown>,
  known: Map<string, string>,
  fallbackMessage: string,
): Record<string, string> {
  const out: Record<string, string> = {};
  for (const [key, value] of Object.entries(rec)) {
    const field = known.get(key.trim().toLowerCase());
    if (!field) {
      continue;
    }
    const text =
      typeof value === "string" && value.trim()
        ? value.trim()
        : fallbackMessage;
    out[field] = text;
  }
  return out;
}

function collectListEntries(
  list: unknown[],
  known: Map<string, string>,
  fallbackMessage: string,
): Record<string, string> {
  const out: Record<string, string> = {};
  for (const item of list) {
    const rec = asRecord(item);
    if (!rec) {
      const named = matchKnownField(item, known);
      if (named) {
        out[named] = fallbackMessage;
      }
      continue;
    }
    const field = fieldFromExplicitKeys(rec, known);
    if (!field) {
      continue;
    }
    const text =
      (typeof rec.message === "string" && rec.message.trim()
        ? rec.message.trim()
        : undefined) ??
      (typeof rec.defaultMessage === "string" && rec.defaultMessage.trim()
        ? rec.defaultMessage.trim()
        : undefined) ??
      fallbackMessage;
    out[field] = text;
  }
  return out;
}

function fieldsFromMessage(
  text: string,
  known: Map<string, string>,
): string[] {
  const hits: string[] = [];
  const quoted = [...text.matchAll(/['"`]([A-Za-z][A-Za-z0-9_]*)['"`]/g)];
  for (const match of quoted) {
    const field = known.get((match[1] ?? "").toLowerCase());
    if (field && !hits.includes(field)) {
      hits.push(field);
    }
  }
  if (hits.length > 0) {
    return hits;
  }
  for (const [lower, name] of known) {
    if (lower.length < 3) {
      continue;
    }
    const re = new RegExp(`(?:^|[^A-Za-z0-9_])${lower}(?:$|[^A-Za-z0-9_])`, "i");
    if (re.test(text) && !hits.includes(name)) {
      hits.push(name);
    }
  }
  return hits;
}

/**
 * Map a thrown save error onto named fields when the 400 (or other) body
 * names a known field. Unnamed failures stay on the banner only.
 */
export function mapSaveApiErrorToFieldErrors(
  err: unknown,
  knownFieldNames: readonly string[],
  fallbackMessage: string,
): MappedSaveFieldErrors {
  if (isSessionRedirectError(err)) {
    return { fieldErrors: {}, banner: "" };
  }
  const known = knownNameSet(knownFieldNames);
  const banner = formatApiError(err, fallbackMessage);
  const body = isApiError(err) ? err.body : undefined;
  const root = unwrapErrorRoot(body);
  const fieldErrors: Record<string, string> = {};

  if (root) {
    const named =
      fieldFromExplicitKeys(root, known) ?? fieldFromErrorData(root.errorData, known);
    if (named) {
      fieldErrors[named] = banner || fallbackMessage;
    }
    for (const mapKey of ["fieldErrors", "errors", "Fields", "fields"]) {
      const bag = root[mapKey];
      const rec = asRecord(bag);
      if (rec) {
        Object.assign(fieldErrors, collectMapEntries(rec, known, fallbackMessage));
      } else if (Array.isArray(bag)) {
        Object.assign(fieldErrors, collectListEntries(bag, known, fallbackMessage));
      }
    }
    const dataRec = asRecord(root.errorData);
    if (dataRec) {
      Object.assign(fieldErrors, collectMapEntries(dataRec, known, fallbackMessage));
      if (Array.isArray(dataRec.errors)) {
        Object.assign(
          fieldErrors,
          collectListEntries(dataRec.errors, known, fallbackMessage),
        );
      }
    }
  }

  if (Object.keys(fieldErrors).length === 0 && banner) {
    const fromText = fieldsFromMessage(banner, known);
    if (fromText.length === 1) {
      fieldErrors[fromText[0]] = banner;
    }
  }

  return { fieldErrors, banner };
}
