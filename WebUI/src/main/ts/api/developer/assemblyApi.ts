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
import type {
  CommunitySummary,
  SlotSummary,
  TemplateDetail,
  TemplateSummary,
} from "./types";

function asArray<T>(payload: unknown, keys: string[]): T[] {
  if (payload == null) return [];
  if (Array.isArray(payload)) return payload as T[];
  if (typeof payload === "object") {
    const obj = payload as Record<string, unknown>;
    for (const k of keys) {
      const raw = obj[k];
      if (raw == null) continue;
      return Array.isArray(raw) ? (raw as T[]) : [raw as T];
    }
  }
  return [];
}

/** GET /services/templates */
export async function listTemplates(): Promise<TemplateSummary[]> {
  const payload = await get<unknown>(PATHS.TEMPLATES);
  return asArray<TemplateSummary>(payload, [
    "TemplateSummaryList",
    "TemplateSummary",
    "templateSummaryList",
  ]);
}

/** GET /services/templates/{idOrName} */
export async function getTemplateDetail(
  idOrName: string,
): Promise<TemplateDetail> {
  const key = encodeURIComponent(idOrName);
  return get<TemplateDetail>(`${PATHS.TEMPLATES}/${key}`);
}

/** GET /services/slots */
export async function listSlots(): Promise<SlotSummary[]> {
  const payload = await get<unknown>(PATHS.SLOTS);
  return asArray<SlotSummary>(payload, ["Slot", "slot", "SlotList"]);
}

/** GET /services/communities/find?name=* */
export async function listCommunities(): Promise<CommunitySummary[]> {
  const payload = await get<unknown>(
    `${PATHS.COMMUNITIES}/find?name=${encodeURIComponent("*")}`,
  );
  // CommunityList extends Array — may serialize as array or envelope
  return asArray<CommunitySummary>(payload, [
    "CommunityList",
    "Community",
    "communityList",
  ]);
}
