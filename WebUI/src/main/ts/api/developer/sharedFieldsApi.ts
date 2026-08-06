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

import { get } from "../client";
import { PATHS } from "../paths";
import type { SharedFieldGroupDetail, SharedFieldGroupSummary } from "./types";

function asArray<T>(payload: unknown): T[] {
  if (payload == null) return [];
  if (Array.isArray(payload)) return payload as T[];
  if (typeof payload === "object") {
    const obj = payload as Record<string, unknown>;
    const raw = obj.SharedFieldGroupSummary ?? obj.sharedFieldGroupSummary;
    if (raw == null) return [];
    return Array.isArray(raw) ? (raw as T[]) : [raw as T];
  }
  return [];
}

/** GET /services/sharedfields */
export async function listSharedFieldGroups(): Promise<SharedFieldGroupSummary[]> {
  const payload = await get<unknown>(PATHS.SHARED_FIELDS);
  return asArray<SharedFieldGroupSummary>(payload);
}

/** GET /services/sharedfields/{name} */
export async function getSharedFieldGroupDetail(
  name: string,
): Promise<SharedFieldGroupDetail> {
  const key = encodeURIComponent(name);
  return get<SharedFieldGroupDetail>(`${PATHS.SHARED_FIELDS}/${key}`);
}
