/*
 * Copyright 1999-2026 Percussion Software, Inc.
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
import type { LocaleDetail, LocaleSummary } from "./types";

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

/** GET /services/locales */
export async function listLocales(): Promise<LocaleSummary[]> {
  const payload = await get<unknown>(PATHS.LOCALES);
  return asArray<LocaleSummary>(payload);
}

/** GET /services/locales/{idOrLang} — language string or numeric locale id */
export async function getLocaleDetail(idOrLang: string): Promise<LocaleDetail> {
  const key = encodeURIComponent(idOrLang);
  return get<LocaleDetail>(`${PATHS.LOCALES}/${key}`);
}
