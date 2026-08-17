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

import { describe, expect, it, vi } from "vitest";
import { restoreLastCommunityIfNeeded } from "../../../../main/ts/app/layout/restoreLastCommunity";
import type { CurrentUserProfile } from "../../../../main/ts/api/user/userProfileApi";

function profile(
  overrides: Partial<CurrentUserProfile> = {},
): CurrentUserProfile {
  return {
    name: "Admin",
    email: "admin@example.com",
    providerType: "INTERNAL",
    roles: ["Admin"],
    communities: ["Default", "Corporate"],
    currentCommunity: "Default",
    adminUser: true,
    designerUser: false,
    accessibilityUser: false,
    emailEditable: true,
    ...overrides,
  };
}

describe("restoreLastCommunityIfNeeded (#3507)", () => {
  it("POSTs switch when remember-last is on and last is still allowed", async () => {
    const switchCommunity = vi.fn().mockResolvedValue(undefined);
    const notifyChanged = vi.fn();
    const restored = await restoreLastCommunityIfNeeded({
      loadPrefs: async () => ({ remember: true, last: "Corporate" }),
      loadProfile: async () => profile(),
      switchCommunity,
      notifyChanged,
    });
    expect(restored).toBe("Corporate");
    expect(switchCommunity).toHaveBeenCalledWith("Corporate");
    expect(notifyChanged).toHaveBeenCalledWith("Corporate");
  });

  it("does not switch when the option is off", async () => {
    const switchCommunity = vi.fn();
    const restored = await restoreLastCommunityIfNeeded({
      loadPrefs: async () => ({ remember: false, last: "Corporate" }),
      loadProfile: async () => profile(),
      switchCommunity,
      notifyChanged: vi.fn(),
    });
    expect(restored).toBeNull();
    expect(switchCommunity).not.toHaveBeenCalled();
  });

  it("falls back to the login default when last community is revoked", async () => {
    const switchCommunity = vi.fn();
    const restored = await restoreLastCommunityIfNeeded({
      loadPrefs: async () => ({ remember: true, last: "Gone" }),
      loadProfile: async () => profile(),
      switchCommunity,
      notifyChanged: vi.fn(),
    });
    expect(restored).toBeNull();
    expect(switchCommunity).not.toHaveBeenCalled();
  });

  it("does not fail login when switch throws", async () => {
    const restored = await restoreLastCommunityIfNeeded({
      loadPrefs: async () => ({ remember: true, last: "Corporate" }),
      loadProfile: async () => profile(),
      switchCommunity: async () => {
        throw new Error("not allowed");
      },
      notifyChanged: vi.fn(),
    });
    expect(restored).toBeNull();
  });

  it("does not fail login when prefs list fails", async () => {
    const switchCommunity = vi.fn();
    const restored = await restoreLastCommunityIfNeeded({
      loadPrefs: async () => {
        throw new Error("prefs down");
      },
      loadProfile: async () => profile(),
      switchCommunity,
      notifyChanged: vi.fn(),
    });
    expect(restored).toBeNull();
    expect(switchCommunity).not.toHaveBeenCalled();
  });
});
