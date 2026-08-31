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

import {
  extractRestErrorMessage,
  get,
  isApiError,
  put,
} from "../client";
import { PATHS } from "../paths";
import { asJacksonArray } from "./slotLists";
import { normalizeLanguageString } from "./localesApi";
import type { AutoTranslationRow } from "./types";

const ROW_WRAP_KEYS = [
  "AutoTranslationRow",
  "autoTranslationRow",
  "autoTranslations",
  "AutoTranslations",
];

/** Jackson WRAP_ROOT / JAXB list envelope for PUT (bare `[]` is rejected). */
export const AUTO_TRANSLATION_ROW_ROOT = "AutoTranslationRow";

/** Wire JSON for PUT — CXF UNWRAP_ROOT_VALUE requires the row root, not a bare array. */
export function wrapAutoTranslationRowsForWire(
  rows: AutoTranslationRow[],
): Record<string, AutoTranslationRow[]> {
  return { [AUTO_TRANSLATION_ROW_ROOT]: rows.map(toAutoTranslationWriteBody) };
}

function asRecord(value: unknown): Record<string, unknown> | null {
  if (value != null && typeof value !== "object") {
    return null;
  }
  if (value == null || Array.isArray(value)) {
    return null;
  }
  return value as Record<string, unknown>;
}

function looksLikeRow(obj: Record<string, unknown>): boolean {
  return (
    typeof obj.locale === "string" ||
    typeof obj.contentTypeName === "string" ||
    typeof obj.contentTypeId === "number" ||
    typeof obj.workflowName === "string" ||
    typeof obj.communityName === "string"
  );
}

/** Unwrap GET/PUT payload that may be a bare array or Jackson/JAXB envelope. */
export function unwrapAutoTranslationRows(payload: unknown): AutoTranslationRow[] {
  return asJacksonArray<AutoTranslationRow>(payload, ROW_WRAP_KEYS, looksLikeRow).filter(
    (row) => row != null && typeof row === "object",
  );
}

function optionalPositiveId(value: unknown): number | undefined {
  return typeof value === "number" && Number.isFinite(value) && value > 0 ? value : undefined;
}

function optionalName(value: unknown): string | undefined {
  if (typeof value !== "string") return undefined;
  const t = value.trim();
  return t.length > 0 ? t : undefined;
}

/** Locale × content-type identity used to detect duplicate PUT rows. */
export function autoTranslationRowKey(row: AutoTranslationRow): string {
  const locale = normalizeLanguageString(row.locale);
  const typeName = (row.contentTypeName || "").trim().toLowerCase();
  const typeId = optionalPositiveId(row.contentTypeId);
  const type = typeName || (typeId != null ? `id:${typeId}` : "");
  return `${locale}|${type}`;
}

/** True when a row has locale plus content type, workflow, and community (name or id). */
export function isAutoTranslationRowReady(row: AutoTranslationRow): boolean {
  if (!normalizeLanguageString(row.locale)) return false;
  if (!optionalName(row.contentTypeName) && optionalPositiveId(row.contentTypeId) == null) {
    return false;
  }
  if (!optionalName(row.workflowName) && optionalPositiveId(row.workflowId) == null) {
    return false;
  }
  if (!optionalName(row.communityName) && optionalPositiveId(row.communityId) == null) {
    return false;
  }
  return true;
}

/**
 * Empty list is valid (PUT clears). Non-empty lists require every row ready
 * and unique locale × content-type keys.
 */
export function isAutoTranslationSetReady(rows: AutoTranslationRow[]): boolean {
  if (rows.length === 0) return true;
  if (!rows.every(isAutoTranslationRowReady)) return false;
  return duplicateAutoTranslationKey(rows) == null;
}

/** First duplicate locale × content-type key, or null. */
export function duplicateAutoTranslationKey(rows: AutoTranslationRow[]): string | null {
  const seen = new Set<string>();
  for (const row of rows) {
    if (!isAutoTranslationRowReady(row)) continue;
    const key = autoTranslationRowKey(row);
    if (!key.endsWith("|") && seen.has(key)) {
      return key;
    }
    if (!key.endsWith("|")) {
      seen.add(key);
    }
  }
  return null;
}

/** PUT body: omit blank names; keep ids when present. */
export function toAutoTranslationWriteBody(row: AutoTranslationRow): AutoTranslationRow {
  const body: AutoTranslationRow = {
    locale: normalizeLanguageString(row.locale) || undefined,
  };
  const typeName = optionalName(row.contentTypeName);
  const typeId = optionalPositiveId(row.contentTypeId);
  const wfName = optionalName(row.workflowName);
  const wfId = optionalPositiveId(row.workflowId);
  const commName = optionalName(row.communityName);
  const commId = optionalPositiveId(row.communityId);
  if (typeName) body.contentTypeName = typeName;
  if (typeId != null) body.contentTypeId = typeId;
  if (wfName) body.workflowName = wfName;
  if (wfId != null) body.workflowId = wfId;
  if (commName) body.communityName = commName;
  if (commId != null) body.communityId = commId;
  return body;
}

/** HTTP 400 with unknown locale or content type in the REST body. */
export function isUnknownLocaleOrTypeError(err: unknown): boolean {
  if (!isApiError(err) || err.status !== 400) {
    return false;
  }
  const msg = (extractRestErrorMessage(err.body) || "").toLowerCase();
  return msg.includes("unknown locale") || msg.includes("unknown content type");
}

/** HTTP 409 (or lock wording) for the auto-translation design lock. */
export function isAutoTranslationLockError(err: unknown): boolean {
  if (!isApiError(err)) {
    return false;
  }
  if (err.status === 409) {
    return true;
  }
  const msg = (extractRestErrorMessage(err.body) || "").toLowerCase();
  return msg.includes("locked") && msg.includes("auto-translation");
}

export type AutoTranslationSaveErrorKind = "unknown" | "lock" | "other";

export function classifyAutoTranslationSaveError(err: unknown): AutoTranslationSaveErrorKind {
  if (isUnknownLocaleOrTypeError(err)) return "unknown";
  if (isAutoTranslationLockError(err)) return "lock";
  return "other";
}

/** GET /services/locales/auto-translations — Admin. Empty list when none configured. */
export async function listAutoTranslations(): Promise<AutoTranslationRow[]> {
  const payload = await get<unknown>(PATHS.AUTO_TRANSLATIONS);
  return unwrapAutoTranslationRows(payload);
}

/**
 * PUT /services/locales/auto-translations — Admin. Full replace.
 * Empty list clears. Unknown locale/content type is 400. Lock conflict is 409.
 */
export async function saveAutoTranslations(
  rows: AutoTranslationRow[],
): Promise<AutoTranslationRow[]> {
  const payload = await put<unknown>(
    PATHS.AUTO_TRANSLATIONS,
    wrapAutoTranslationRowsForWire(rows),
  );
  return unwrapAutoTranslationRows(payload);
}

/** Exported for tests that assert envelope unwrap of a single object. */
export function looksLikeAutoTranslationRow(value: unknown): boolean {
  const obj = asRecord(value);
  return obj != null && looksLikeRow(obj);
}
