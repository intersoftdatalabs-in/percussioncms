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

import { del, get, post, put } from "../client";
import { PATHS } from "../paths";
import type { LocaleDetail, LocaleSummary } from "./types";

/**
 * BCP-47 style language strings accepted by REST create
 * (`LocalesAdaptor` LANGUAGE_PATTERN).
 */
export const LOCALE_LANGUAGE_PATTERN = /^[a-z]{2,8}(-[a-z0-9]{1,8})*$/;

/** Writable fields for POST/PUT /services/locales. Format / designGaps are not written. */
export type LocaleWriteBody = Pick<
  LocaleDetail,
  "languageString" | "label" | "description" | "status" | "baseLocale"
>;

/** Jackson / JAXB root for LocaleDetail (UNWRAP_ROOT_VALUE on POST/PUT). */
export const LOCALE_DETAIL_ROOT = "LocaleDetail";

/** Wire JSON for POST/PUT — a flat body fails JAXB root unwrap. */
export function wrapLocaleDetailForWire(
  body: LocaleWriteBody,
): Record<string, LocaleWriteBody> {
  return { [LOCALE_DETAIL_ROOT]: body };
}

/** Unwrap GET/POST/PUT payload that may be wrapped as { LocaleDetail: {...} }. */
export function unwrapLocaleDetail(payload: unknown): LocaleDetail {
  if (payload == null || typeof payload !== "object" || Array.isArray(payload)) {
    return {};
  }
  const obj = payload as Record<string, unknown>;
  const raw = obj.LocaleDetail ?? obj.localeDetail;
  if (raw != null && typeof raw === "object" && !Array.isArray(raw)) {
    return raw as LocaleDetail;
  }
  return obj as LocaleDetail;
}

function asArray<T>(payload: unknown): T[] {
  if (payload == null) return [];
  if (Array.isArray(payload)) return payload as T[];
  if (typeof payload === "object") {
    const obj = payload as Record<string, unknown>;
    const raw = obj.LocaleSummary ?? obj.localeSummary;
    if (raw == null) return [];
    return Array.isArray(raw) ? (raw as T[]) : [raw as T];
  }
  return [];
}

/** Lower-case, trim, and map `_` to `-` like the design adaptor. */
export function normalizeLanguageString(lang: string | undefined | null): string {
  if (lang == null) return "";
  return lang.trim().toLowerCase().replace(/_/g, "-");
}

/** True when the (normalized) language string is a safe REST locale key. */
export function isValidLanguageString(lang: string | undefined | null): boolean {
  const code = normalizeLanguageString(lang);
  return code.length > 0 && LOCALE_LANGUAGE_PATTERN.test(code);
}

/** Save is enabled when language (create) and label are both present/valid. */
export function isLocaleWriteReady(opts: {
  isNew: boolean;
  language: string;
  label: string;
}): boolean {
  if (!opts.label.trim()) return false;
  if (opts.isNew && !isValidLanguageString(opts.language)) return false;
  return true;
}

/** GET /services/locales */
export async function listLocales(): Promise<LocaleSummary[]> {
  const payload = await get<unknown>(PATHS.LOCALES);
  return asArray<LocaleSummary>(payload);
}

/** GET /services/locales/{idOrLang} — language string or numeric locale id */
export async function getLocaleDetail(idOrLang: string): Promise<LocaleDetail> {
  const key = encodeURIComponent(idOrLang);
  const payload = await get<unknown>(`${PATHS.LOCALES}/${key}`);
  return unwrapLocaleDetail(payload);
}

/** POST /services/locales — Admin. languageString + label required. Duplicate is 409. */
export async function createLocale(body: LocaleWriteBody): Promise<LocaleDetail> {
  const payload = await post<unknown>(PATHS.LOCALES, wrapLocaleDetailForWire(body));
  return unwrapLocaleDetail(payload);
}

/** PUT /services/locales/{idOrLang} — Admin. languageString is immutable. */
export async function updateLocale(
  idOrLang: string,
  body: LocaleWriteBody,
): Promise<LocaleDetail> {
  const payload = await put<unknown>(
    `${PATHS.LOCALES}/${encodeURIComponent(idOrLang)}`,
    wrapLocaleDetailForWire(body),
  );
  return unwrapLocaleDetail(payload);
}

/** DELETE /services/locales/{idOrLang} — Admin. 204 on success; missing is 404. */
export async function deleteLocale(idOrLang: string): Promise<void> {
  await del(`${PATHS.LOCALES}/${encodeURIComponent(idOrLang)}`);
}
