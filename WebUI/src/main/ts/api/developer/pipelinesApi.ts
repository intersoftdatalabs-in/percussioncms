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
import type { ApplicationDetail, ApplicationSummary } from "./types";

export interface ListApplicationsOptions {
  name?: string;
  limit?: number;
  offset?: number;
}

function asArray<T>(payload: unknown): T[] {
  if (payload == null) return [];
  if (Array.isArray(payload)) return payload as T[];
  // Defensive: some CXF/XML-bridge shapes wrap a single element
  if (typeof payload === "object") {
    const obj = payload as Record<string, unknown>;
    const raw = obj.Application ?? obj.application;
    if (raw == null) return [];
    return Array.isArray(raw) ? (raw as T[]) : [raw as T];
  }
  return [];
}

/** GET /services/pipelines?name=&limit=&offset= */
export async function listApplications(
  options: ListApplicationsOptions = {},
): Promise<ApplicationSummary[]> {
  const params = new URLSearchParams();
  if (options.name) params.set("name", options.name);
  if (options.limit != null) params.set("limit", String(options.limit));
  if (options.offset != null) params.set("offset", String(options.offset));
  const q = params.toString();
  const url = q ? `${PATHS.PIPELINES}?${q}` : PATHS.PIPELINES;
  const payload = await get<unknown>(url);
  return asArray<ApplicationSummary>(payload);
}

/** GET /services/pipelines/{idOrName} — name or numeric id */
export async function getApplicationDetail(
  idOrName: string,
): Promise<ApplicationDetail> {
  const key = encodeURIComponent(idOrName);
  return get<ApplicationDetail>(`${PATHS.PIPELINES}/${key}`);
}
