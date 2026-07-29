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
import type { KeywordSummary } from "./types";

/**
 * List keyword definitions.
 *
 * <p>Server: {@code GET /services/keywords?includeChoices=true|false}
 */
export async function listKeywords(
  includeChoices = true,
): Promise<KeywordSummary[]> {
  const q = includeChoices ? "?includeChoices=true" : "?includeChoices=false";
  const payload = await get<unknown>(`${PATHS.KEYWORDS}${q}`);
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
