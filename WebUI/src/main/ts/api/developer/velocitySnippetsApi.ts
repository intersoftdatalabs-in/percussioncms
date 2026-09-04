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
import { asJacksonArray } from "./slotLists";

/** Built-in Velocity macro snippet (AS-09 catalog row). */
export interface VelocitySnippet {
  id: string;
  title: string;
  /** Catalog category: field, slot, or misc. */
  category: string;
  /** Text inserted into the template source editor. */
  insertText: string;
}

function asSnippet(raw: unknown): VelocitySnippet | null {
  if (raw == null || typeof raw !== "object") {
    return null;
  }
  const o = raw as Record<string, unknown>;
  const id = typeof o.id === "string" ? o.id : "";
  const title = typeof o.title === "string" ? o.title : "";
  const category = typeof o.category === "string" ? o.category : "";
  const insertText =
    typeof o.insertText === "string"
      ? o.insertText
      : typeof o.insert_text === "string"
        ? o.insert_text
        : "";
  if (!id && !insertText) {
    return null;
  }
  return {
    id,
    title: title || id,
    category: category || "misc",
    insertText,
  };
}

/**
 * Normalize GET /services/velocity/snippets to a flat array.
 *
 * <p>Accepts a bare JSON array, JAXB/Jackson list envelopes, or a single
 * {@code VelocitySnippet} object (unit tests / proxies).
 */
export function unwrapVelocitySnippets(payload: unknown): VelocitySnippet[] {
  const rows = asJacksonArray<unknown>(payload, [
    "VelocitySnippet",
    "velocitySnippet",
    "Snippet",
    "snippet",
  ]);
  const out: VelocitySnippet[] = [];
  for (const row of rows) {
    const snip = asSnippet(row);
    if (snip) {
      out.push(snip);
    }
  }
  return out;
}

/** GET /services/velocity/snippets — built-in AS-09 macro catalog. */
export async function listVelocitySnippets(): Promise<VelocitySnippet[]> {
  const payload = await get<unknown>(PATHS.VELOCITY_SNIPPETS);
  return unwrapVelocitySnippets(payload);
}

/** GET /services/velocity/snippets/{id} — case-insensitive catalog lookup. */
export async function getVelocitySnippet(id: string): Promise<VelocitySnippet> {
  const payload = await get<unknown>(
    `${PATHS.VELOCITY_SNIPPETS}/${encodeURIComponent(id)}`,
  );
  if (payload != null && typeof payload === "object" && !Array.isArray(payload)) {
    const root = payload as Record<string, unknown>;
    const nested = root.VelocitySnippet ?? root.velocitySnippet;
    const snip = asSnippet(nested ?? payload);
    if (snip) {
      return snip;
    }
  }
  const snip = asSnippet(payload);
  if (!snip) {
    throw new Error("Velocity snippet response was empty");
  }
  return snip;
}
