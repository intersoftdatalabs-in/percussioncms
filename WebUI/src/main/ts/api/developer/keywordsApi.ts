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
import type { KeywordSummary } from "./types";

function asKeywordList(payload: unknown): KeywordSummary[] {
  if (Array.isArray(payload)) {
    return payload as KeywordSummary[];
  }
  if (payload && typeof payload === "object") {
    const env = payload as {
      Keyword?: KeywordSummary[] | KeywordSummary;
      keyword?: KeywordSummary[] | KeywordSummary;
    };
    const raw = env.Keyword ?? env.keyword;
    if (raw == null) return [];
    return Array.isArray(raw) ? raw : [raw];
  }
  return [];
}

/** GET /services/keywords?includeChoices= */
export async function listKeywords(
  includeChoices = true,
): Promise<KeywordSummary[]> {
  const q = includeChoices ? "?includeChoices=true" : "?includeChoices=false";
  const payload = await get<unknown>(`${PATHS.KEYWORDS}${q}`);
  return asKeywordList(payload);
}

/** GET /services/keywords/{idOrValue} */
export async function getKeyword(idOrValue: string): Promise<KeywordSummary> {
  return get<KeywordSummary>(
    `${PATHS.KEYWORDS}/${encodeURIComponent(idOrValue)}`,
  );
}

/** POST /services/keywords */
export async function createKeyword(
  body: KeywordSummary,
): Promise<KeywordSummary> {
  return post<KeywordSummary>(PATHS.KEYWORDS, body);
}

/** PUT /services/keywords/{id} */
export async function updateKeyword(
  id: string,
  body: KeywordSummary,
): Promise<KeywordSummary> {
  return put<KeywordSummary>(
    `${PATHS.KEYWORDS}/${encodeURIComponent(id)}`,
    body,
  );
}

/** DELETE /services/keywords/{id} */
export async function deleteKeyword(id: string): Promise<void> {
  await del(`${PATHS.KEYWORDS}/${encodeURIComponent(id)}`);
}
