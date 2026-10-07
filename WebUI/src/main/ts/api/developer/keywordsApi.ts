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
import type { KeywordChoiceSummary, KeywordSummary } from "./types";

/** Jackson / JAXB root for KeywordSummary. A flat body is HTTP 400. */
export const KEYWORD_ROOT = "Keyword";

/** Wire JSON for POST/PUT — JAXB rejects a bare `label` element. */
export function wrapKeywordForWire(
  body: KeywordSummary,
): Record<string, KeywordSummary> {
  return { [KEYWORD_ROOT]: body };
}

/**
 * One JAXB choice is a JSON object. Several choices are an array, sometimes
 * under KeywordChoice. Always return a list.
 */
function asChoiceList(raw: unknown): KeywordChoiceSummary[] {
  if (raw == null) {
    return [];
  }
  if (Array.isArray(raw)) {
    const out: KeywordChoiceSummary[] = [];
    for (const item of raw) {
      out.push(...asChoiceList(item));
    }
    return out;
  }
  if (typeof raw !== "object") {
    return [];
  }
  const record = raw as Record<string, unknown>;
  const nested = record.KeywordChoice ?? record.keywordChoice;
  if (nested != null) {
    return asChoiceList(nested);
  }
  if ("label" in record || "value" in record) {
    return [raw as KeywordChoiceSummary];
  }
  return [];
}

function withChoiceList(keyword: KeywordSummary): KeywordSummary {
  return { ...keyword, choices: asChoiceList(keyword.choices) };
}

/** Accept a flat keyword or a `{ Keyword }` object envelope. */
export function unwrapKeywordPayload(payload: unknown): KeywordSummary | null {
  if (!payload || typeof payload !== "object" || Array.isArray(payload)) {
    return null;
  }
  const record = payload as Record<string, unknown>;
  const nested = record.Keyword ?? record.keyword;
  if (nested && typeof nested === "object" && !Array.isArray(nested)) {
    return withChoiceList(nested as KeywordSummary);
  }
  if (
    "label" in record ||
    "choices" in record ||
    "description" in record ||
    "guid" in record ||
    "sequence" in record
  ) {
    return withChoiceList(payload as KeywordSummary);
  }
  return null;
}

function asKeyword(payload: unknown): KeywordSummary {
  return unwrapKeywordPayload(payload) ?? {};
}

function asKeywordList(payload: unknown): KeywordSummary[] {
  const rows = keywordRows(payload);
  return rows.map((row) => withChoiceList(row));
}

function keywordRows(payload: unknown): KeywordSummary[] {
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
  const payload = await get<unknown>(
    `${PATHS.KEYWORDS}/${encodeURIComponent(idOrValue)}`,
  );
  return asKeyword(payload);
}

/** POST /services/keywords */
export async function createKeyword(
  body: KeywordSummary,
): Promise<KeywordSummary> {
  const payload = await post<unknown>(PATHS.KEYWORDS, wrapKeywordForWire(body));
  return asKeyword(payload);
}

/** PUT /services/keywords/{id} */
export async function updateKeyword(
  id: string,
  body: KeywordSummary,
): Promise<KeywordSummary> {
  const payload = await put<unknown>(
    `${PATHS.KEYWORDS}/${encodeURIComponent(id)}`,
    wrapKeywordForWire(body),
  );
  return asKeyword(payload);
}

/** DELETE /services/keywords/{id} */
export async function deleteKeyword(id: string): Promise<void> {
  await del(`${PATHS.KEYWORDS}/${encodeURIComponent(id)}`);
}
