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

import { afterEach, describe, expect, it, vi } from "vitest";
import {
  LAST_COMMUNITY_PREF_NAME,
  REMEMBER_LAST_COMMUNITY_PREF_NAME,
  loadRememberLastCommunityPrefs,
  parseRememberLastCommunityFlag,
  prefsFromList,
  saveLastCommunity,
  saveRememberLastCommunityFlag,
  shouldRestoreLastCommunity,
} from "../../../main/ts/profile/rememberLastCommunity";
import * as prefs from "../../../main/ts/api/preferences/preferencesApi";

describe("rememberLastCommunity (#3507)", () => {
  afterEach(() => {
    vi.restoreAllMocks();
  });

  it("parses remember-last flag values", () => {
    expect(parseRememberLastCommunityFlag("true")).toBe(true);
    expect(parseRememberLastCommunityFlag(" YES ")).toBe(true);
    expect(parseRememberLastCommunityFlag("1")).toBe(true);
    expect(parseRememberLastCommunityFlag(true)).toBe(true);
    expect(parseRememberLastCommunityFlag(1)).toBe(true);
    expect(parseRememberLastCommunityFlag("false")).toBe(false);
    expect(parseRememberLastCommunityFlag("")).toBe(false);
    expect(parseRememberLastCommunityFlag(undefined)).toBe(false);
    expect(parseRememberLastCommunityFlag(false)).toBe(false);
  });

  it("restores last community only when opted in and still allowed", () => {
    expect(
      shouldRestoreLastCommunity({
        remember: true,
        last: "Corporate",
        current: "Default",
        allowed: ["Default", "Corporate"],
      }),
    ).toBe("Corporate");
    expect(
      shouldRestoreLastCommunity({
        remember: false,
        last: "Corporate",
        current: "Default",
        allowed: ["Default", "Corporate"],
      }),
    ).toBeNull();
    expect(
      shouldRestoreLastCommunity({
        remember: true,
        last: "Corporate",
        current: "Corporate",
        allowed: ["Default", "Corporate"],
      }),
    ).toBeNull();
    expect(
      shouldRestoreLastCommunity({
        remember: true,
        last: "Revoked",
        current: "Default",
        allowed: ["Default", "Corporate"],
      }),
    ).toBeNull();
    expect(
      shouldRestoreLastCommunity({
        remember: true,
        last: "",
        current: "Default",
        allowed: ["Default"],
      }),
    ).toBeNull();
  });

  it("loadRememberLastCommunityPrefs uses the list endpoint, not named GET", async () => {
    const listSpy = vi.spyOn(prefs, "getAllUserPreferences").mockResolvedValueOnce([
      { name: REMEMBER_LAST_COMMUNITY_PREF_NAME, value: "true" },
      { name: LAST_COMMUNITY_PREF_NAME, value: "  Corporate " },
    ]);
    const namedSpy = vi.spyOn(prefs, "loadUserPreference");
    await expect(loadRememberLastCommunityPrefs()).resolves.toEqual({
      remember: true,
      last: "Corporate",
    });
    expect(listSpy).toHaveBeenCalledTimes(1);
    expect(namedSpy).not.toHaveBeenCalled();
  });

  it("prefsFromList defaults when unset", () => {
    expect(prefsFromList([])).toEqual({ remember: false, last: "" });
  });

  it("saveRememberLastCommunityFlag PUTs true/false", async () => {
    const saveSpy = vi.spyOn(prefs, "saveUserPreference").mockResolvedValueOnce({
      name: REMEMBER_LAST_COMMUNITY_PREF_NAME,
      value: "true",
    });
    await expect(saveRememberLastCommunityFlag("Admin", true)).resolves.toBe(
      true,
    );
    expect(saveSpy).toHaveBeenCalledWith(
      expect.objectContaining({
        name: REMEMBER_LAST_COMMUNITY_PREF_NAME,
        value: "true",
        userName: "Admin",
      }),
    );
  });

  it("saveLastCommunity PUTs the community name", async () => {
    const saveSpy = vi.spyOn(prefs, "saveUserPreference").mockResolvedValueOnce({
      name: LAST_COMMUNITY_PREF_NAME,
      value: "Corporate",
    });
    await expect(saveLastCommunity("Admin", "Corporate")).resolves.toBe(
      "Corporate",
    );
    expect(saveSpy).toHaveBeenCalledWith(
      expect.objectContaining({
        name: LAST_COMMUNITY_PREF_NAME,
        value: "Corporate",
        userName: "Admin",
      }),
    );
  });
});
