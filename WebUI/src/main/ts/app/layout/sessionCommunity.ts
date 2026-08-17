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
 * Session community change event so Explorer / Developer remount and
 * refetch with the new community without a logout (#3506).
 */

export const SESSION_COMMUNITY_CHANGED_EVENT =
  "perc:session-community-changed";

export type SessionCommunityChangedDetail = {
  community: string;
};

export function dispatchSessionCommunityChanged(community: string): void {
  if (typeof window === "undefined") {
    return;
  }
  window.dispatchEvent(
    new CustomEvent<SessionCommunityChangedDetail>(
      SESSION_COMMUNITY_CHANGED_EVENT,
      { detail: { community } },
    ),
  );
}

/** Stable test id slug for a community option (spaces → hyphens). */
export function communityOptionTestId(name: string): string {
  const slug = name
    .trim()
    .toLowerCase()
    .replace(/[^a-z0-9]+/g, "-")
    .replace(/^-+|-+$/g, "");
  return `perc-spa-community-option-${slug || "unnamed"}`;
}
