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

import { del, get, post } from "../client";
import { PATHS } from "../paths";

/** Service base; use getter so context-aware SERVICES_ROOT is current. */
function wb(): string {
  return PATHS.WIDGET_BUILDER;
}

/**
 * Backend returns {@code text/plain} boolean ({@code true}/{@code false}), not JSON.
 */
export async function isWidgetBuilderActive(): Promise<boolean> {
  const data = await get<unknown>(`${wb()}/active`);
  if (typeof data === "boolean") {
    return data;
  }
  if (typeof data === "string") {
    const s = data.trim().toLowerCase();
    return s === "true" || s === "1" || s === "yes";
  }
  if (data && typeof data === "object" && "value" in data) {
    return Boolean((data as { value: unknown }).value);
  }
  return Boolean(data);
}

export async function fetchSummaries(): Promise<unknown[]> {
  const data = await get<unknown>(`${wb()}/summaries`);
  if (Array.isArray(data)) {
    return data;
  }
  if (data && typeof data === "object") {
    for (const v of Object.values(data as Record<string, unknown>)) {
      if (Array.isArray(v)) {
        return v;
      }
    }
  }
  return [];
}

export async function loadDefinition(id: number | string): Promise<unknown> {
  return get(`${wb()}/definition/${id}`);
}

export async function saveDefinition(definition: unknown): Promise<unknown> {
  return post(`${wb()}/definition/`, definition);
}

export async function validateDefinition(definition: unknown): Promise<unknown> {
  return post(`${wb()}/validate/`, definition);
}

export async function deployDefinition(id: number | string): Promise<void> {
  await post(`${wb()}/deploy/${id}`);
}

export async function deleteDefinition(id: number | string): Promise<void> {
  await del(`${wb()}/definition/${id}`);
}
