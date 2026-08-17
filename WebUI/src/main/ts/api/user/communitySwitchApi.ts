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

/**
 * Session community switch for top-nav chrome (#3506 / parent #3505).
 *
 * <p>POST {@code /services/communities/switch/{name}} — membership is
 * enforced server-side. The UserMenu list must come from
 * {@code GET /user/user/current} ({@code communities}), not the design
 * catalog.</p>
 */

import { post } from "../client";
import { PATHS } from "../paths";

/** Builds POST /services/communities/switch/{name} with a path-safe name. */
export function communitySwitchUrl(name: string): string {
  const trimmed = (name ?? "").trim();
  if (!trimmed) {
    throw new Error("Community name is required");
  }
  return `${PATHS.COMMUNITIES}/switch/${encodeURIComponent(trimmed)}`;
}

/**
 * Deduped membership names from the signed-in user payload.
 * Does not add catalog communities.
 */
export function allowedSessionCommunities(
  communities: readonly string[] | null | undefined,
): string[] {
  const seen = new Set<string>();
  const out: string[] = [];
  for (const raw of communities ?? []) {
    const name =
      typeof raw === "string" ? raw.trim() : String(raw ?? "").trim();
    if (!name || seen.has(name)) {
      continue;
    }
    seen.add(name);
    out.push(name);
  }
  return out;
}

/** Switch the signed-in session to {@code name}. Throws {@link ApiError} on failure. */
export async function switchSessionCommunity(name: string): Promise<void> {
  await post(communitySwitchUrl(name));
}
