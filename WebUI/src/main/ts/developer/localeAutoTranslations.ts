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

import { normalizeLanguageString } from "../api/developer/localesApi";
import {
  duplicateAutoTranslationKey,
  isAutoTranslationSetReady,
} from "../api/developer/autoTranslationsApi";
import type { AutoTranslationRow } from "../api/developer/types";

/**
 * Per-locale slice of the singleton auto-translation set (#5704 / #1690).
 *
 * <p>The REST surface stays the full-set `GET/PUT
 * /services/locales/auto-translations` shipped in #3981. The locale editor
 * narrows that set to one locale client-side: it loads the full set, shows
 * only this locale's rows, and saves the edited rows merged back with the
 * untouched rows of every other locale. An empty edited list clears only
 * this locale's rows. The locale's language, label, description, status, and
 * format fields are never part of this save.</p>
 */

/** Normalized identity of one locale for row scoping. */
export function normalizeScopedLocale(locale: string | undefined | null): string {
  return normalizeLanguageString(locale);
}

/** True when the row belongs to the given locale (case/underscore-insensitive). */
export function isRowForLocale(
  row: AutoTranslationRow | null | undefined,
  locale: string | undefined | null,
): boolean {
  const want = normalizeScopedLocale(locale);
  if (!want || row == null || typeof row !== "object") {
    return false;
  }
  return normalizeLanguageString(row.locale) === want;
}

/** Rows of the full set that belong to one locale, in set order. */
export function filterLocaleAutoTranslations(
  rows: AutoTranslationRow[],
  locale: string | undefined | null,
): AutoTranslationRow[] {
  if (!normalizeScopedLocale(locale)) {
    return [];
  }
  return (rows || []).filter((row) => isRowForLocale(row, locale));
}

/**
 * Rows of the full set that belong to every locale except the given one,
 * in set order. Saved back untouched when this locale's rows are replaced.
 */
export function otherLocaleAutoTranslations(
  rows: AutoTranslationRow[],
  locale: string | undefined | null,
): AutoTranslationRow[] {
  const want = normalizeScopedLocale(locale);
  if (!want) {
    return [...(rows || [])];
  }
  return (rows || []).filter((row) => !isRowForLocale(row, want));
}

/**
 * Edited rows forced onto one locale. The editor fixes the locale column,
 * so every saved row carries the normalized locale of the open editor.
 */
export function scopeAutoTranslationRowsToLocale(
  rows: AutoTranslationRow[],
  locale: string | undefined | null,
): AutoTranslationRow[] {
  const want = normalizeScopedLocale(locale);
  return (rows || []).map((row) => ({ ...row, locale: want }));
}

/**
 * Full set to PUT after a per-locale save: every other locale's rows
 * untouched, followed by this locale's edited (locale-forced) rows. An
 * empty edited list removes this locale's rows and clears that slice.
 */
export function mergeLocaleAutoTranslations(
  fullRows: AutoTranslationRow[],
  locale: string | undefined | null,
  editedRows: AutoTranslationRow[],
): AutoTranslationRow[] {
  return [
    ...otherLocaleAutoTranslations(fullRows, locale),
    ...scopeAutoTranslationRowsToLocale(editedRows, locale),
  ];
}

/**
 * True when this locale's edited rows are a valid save: an empty list
 * clears the slice, otherwise every row must be ready and duplicate-free.
 */
export function isLocaleAutoTranslationSetReady(
  editedRows: AutoTranslationRow[],
): boolean {
  return isAutoTranslationSetReady(editedRows || []);
}

/** First duplicate locale × content-type key in this locale's rows, or null. */
export function duplicateLocaleAutoTranslationKey(
  editedRows: AutoTranslationRow[],
  locale: string | undefined | null,
): string | null {
  return duplicateAutoTranslationKey(scopeAutoTranslationRowsToLocale(editedRows, locale));
}
