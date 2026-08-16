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
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import { afterEach, describe, expect, it, vi } from "vitest";
import {
  loadGravatarEmailOverride,
  saveGravatarEmailOverride,
} from "../../../main/ts/profile/avatarPrefs";
import { GRAVATAR_EMAIL_PREF_NAME } from "../../../main/ts/profile/gravatar";
import * as prefs from "../../../main/ts/api/preferences/preferencesApi";

describe("avatarPrefs (#3468)", () => {
  afterEach(() => {
    vi.restoreAllMocks();
  });

  it("loadGravatarEmailOverride uses the list endpoint, not named GET", async () => {
    const listSpy = vi.spyOn(prefs, "getAllUserPreferences").mockResolvedValueOnce([
      { name: GRAVATAR_EMAIL_PREF_NAME, value: "  avatar@example.com " },
      { name: "other", value: "x" },
    ]);
    const namedSpy = vi.spyOn(prefs, "loadUserPreference");
    await expect(loadGravatarEmailOverride()).resolves.toBe("avatar@example.com");
    expect(listSpy).toHaveBeenCalledTimes(1);
    expect(namedSpy).not.toHaveBeenCalled();
  });

  it("loadGravatarEmailOverride returns empty when list has no override", async () => {
    vi.spyOn(prefs, "getAllUserPreferences").mockResolvedValueOnce([]);
    const namedSpy = vi.spyOn(prefs, "loadUserPreference");
    await expect(loadGravatarEmailOverride()).resolves.toBe("");
    expect(namedSpy).not.toHaveBeenCalled();
  });

  it("saveGravatarEmailOverride still PUTs the named preference", async () => {
    const saveSpy = vi.spyOn(prefs, "saveUserPreference").mockResolvedValueOnce({
      name: GRAVATAR_EMAIL_PREF_NAME,
      value: "saved@example.com",
    });
    await expect(
      saveGravatarEmailOverride("Admin", "saved@example.com"),
    ).resolves.toBe("saved@example.com");
    expect(saveSpy).toHaveBeenCalledWith(
      expect.objectContaining({
        name: GRAVATAR_EMAIL_PREF_NAME,
        value: "saved@example.com",
        userName: "Admin",
      }),
    );
  });
});
