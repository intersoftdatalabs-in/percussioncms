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
 * Typed client for content-item translation (P-Trans / #2430).
 *
 * <p>Consumes the public REST façade from #2429 / PR #2601 — does not invent
 * endpoints or call SOAP from the SPA:
 *
 * <pre>
 *   GET  /Rhythmyx/rest/content-explorer/translations/{itemId}
 *   POST /Rhythmyx/rest/content-explorer/translations
 * </pre>
 *
 * <p>In-flight translation queue and session content-locale context are
 * intentionally not exposed by the REST contract (product disposition on
 * #2411 / #2428); this client does not fabricate those fields.
 */

import { get, post, isApiError, formatApiError } from "../client";

const BASE_PATH = "/Rhythmyx/rest/content-explorer/translations";

/** One content-item locale variant (source or translation copy). */
export interface TranslationVariant {
  contentId: number;
  revision?: number | null;
  locale?: string | null;
  /** {@code source} for the requested item; {@code translation} for related copies. */
  role?: string | null;
  sourceContentId?: number | null;
}

/** GET response for per-item locale variants. */
export interface ItemTranslationVariants {
  itemId: number;
  locale?: string | null;
  variants?: TranslationVariant[] | null;
}

/** POST body for create-variant (NewTranslations façade). */
export interface CreateTranslationsRequest {
  itemIds: number[];
  locales?: string[] | null;
  relationshipType?: string | null;
  enableRevisions?: boolean | null;
}

/** POST result envelope. */
export interface CreateTranslationsResult {
  created?: TranslationVariant[] | null;
}

/** Catalog row used when choosing target locales for create-variant. */
export interface TranslationLocaleOption {
  languageString: string;
  label?: string;
}

/**
 * Thrown (or re-mapped) when the server denies read/create (403).
 * Mirrors {@link RelationshipSummaryAuthError} for Explorer UI handling.
 */
export class TranslationAuthError extends Error {
  readonly status: number;
  readonly statusText: string;
  constructor(message: string, status = 403, statusText = "Forbidden") {
    super(message);
    this.name = "TranslationAuthError";
    this.status = status;
    this.statusText = statusText;
  }
}

function rethrowAuthOrWrap(err: unknown, fallback: string): never {
  if (err instanceof TranslationAuthError) {
    throw err;
  }
  if (isApiError(err) && err.status === 403) {
    throw new TranslationAuthError(
      formatApiError(err, "Not allowed"),
      err.status,
      err.statusText,
    );
  }
  if (err instanceof Error) {
    throw err;
  }
  throw new Error(formatApiError(err, fallback));
}

/**
 * List current locale + translation-category dependents for a content item.
 *
 * @param itemId legacy content id or guid string
 */
export async function listItemTranslationVariants(
  itemId: string,
): Promise<ItemTranslationVariants> {
  const key = encodeURIComponent(itemId);
  try {
    return await get<ItemTranslationVariants>(`${BASE_PATH}/${key}`);
  } catch (err) {
    rethrowAuthOrWrap(err, "Failed to list translation variants");
  }
}

/**
 * Create locale variants for the given source content ids.
 *
 * <p>When {@code locales} is omitted/empty the server uses all system
 * auto-translations (SOAP NewTranslations parity).</p>
 */
export async function createTranslations(
  body: CreateTranslationsRequest,
): Promise<CreateTranslationsResult> {
  try {
    return await post<CreateTranslationsResult>(BASE_PATH, body);
  } catch (err) {
    rethrowAuthOrWrap(err, "Failed to create translations");
  }
}

/**
 * Filter the CMS locale catalog to locales not already present among variants.
 *
 * <p>Pure helper for create-variant UI (unit-tested).</p>
 */
export function availableTargetLocales(
  catalog: ReadonlyArray<{ languageString?: string | null; label?: string | null }>,
  variants: ReadonlyArray<TranslationVariant>,
  currentLocale?: string | null,
): TranslationLocaleOption[] {
  const taken = new Set<string>();
  if (currentLocale && currentLocale.trim()) {
    taken.add(currentLocale.trim().toLowerCase());
  }
  for (const v of variants) {
    if (v.locale && v.locale.trim()) {
      taken.add(v.locale.trim().toLowerCase());
    }
  }
  const out: TranslationLocaleOption[] = [];
  const seen = new Set<string>();
  for (const row of catalog) {
    const lang = (row.languageString ?? "").trim();
    if (!lang) continue;
    const key = lang.toLowerCase();
    if (taken.has(key) || seen.has(key)) continue;
    seen.add(key);
    out.push({
      languageString: lang,
      label: row.label?.trim() || lang,
    });
  }
  return out;
}
