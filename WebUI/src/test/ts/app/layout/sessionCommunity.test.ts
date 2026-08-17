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

import { describe, expect, it } from "vitest";
import {
  communityOptionTestId,
  dispatchSessionCommunityChanged,
  SESSION_COMMUNITY_CHANGED_EVENT,
} from "../../../../main/ts/app/layout/sessionCommunity";

describe("sessionCommunity helpers", () => {
  it("slugs option test ids", () => {
    expect(communityOptionTestId("Enterprise Investments")).toBe(
      "perc-spa-community-option-enterprise-investments",
    );
    expect(communityOptionTestId("  ")).toBe("perc-spa-community-option-unnamed");
  });

  it("dispatches the session community changed event", () => {
    const seen: string[] = [];
    const handler = (ev: Event): void => {
      seen.push((ev as CustomEvent<{ community: string }>).detail.community);
    };
    window.addEventListener(SESSION_COMMUNITY_CHANGED_EVENT, handler);
    try {
      dispatchSessionCommunityChanged("Corporate");
      expect(seen).toEqual(["Corporate"]);
    } finally {
      window.removeEventListener(SESSION_COMMUNITY_CHANGED_EVENT, handler);
    }
  });
});
