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

import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import {
  getAllUserPreferences,
  loadUserPreference,
  saveUserPreference,
  unwrapUserPreference,
  wrapUserPreferenceForWire,
  USER_PREFERENCE_ROOT,
} from "../../../main/ts/api/preferences/preferencesApi";
import * as client from "../../../main/ts/api/client";

describe("preferencesApi (PreferenceResource)", () => {
  beforeEach(() => {
    vi.restoreAllMocks();
  });

  afterEach(() => {
    vi.restoreAllMocks();
  });

  it("getAllUserPreferences returns list and treats 404 as empty", async () => {
    const getSpy = vi.spyOn(client, "get");
    getSpy.mockResolvedValueOnce([
      { name: "developer.defaultObjectAclTemplate", value: "{}" },
    ]);
    await expect(getAllUserPreferences()).resolves.toHaveLength(1);

    getSpy.mockRejectedValueOnce({ status: 404, statusText: "Not Found", body: null });
    await expect(getAllUserPreferences()).resolves.toEqual([]);
  });

  it("loadUserPreference returns null on 404", async () => {
    vi.spyOn(client, "get").mockRejectedValueOnce({
      status: 404,
      statusText: "Not Found",
      body: null,
    });
    await expect(loadUserPreference("missing")).resolves.toBeNull();
  });

  it("loadUserPreference unwraps Jackson UserPreference root", async () => {
    vi.spyOn(client, "get").mockResolvedValueOnce({
      UserPreference: {
        name: "perc_profile_gravatar_email",
        value: "avatar@example.com",
        category: "sys_preferences",
        context: "private",
        userName: "Admin",
      },
    });
    const pref = await loadUserPreference("perc_profile_gravatar_email");
    expect(pref?.name).toBe("perc_profile_gravatar_email");
    expect(pref?.value).toBe("avatar@example.com");
  });

  it("saveUserPreference wraps body under UserPreference root (#2708)", async () => {
    const putSpy = vi.spyOn(client, "put").mockResolvedValueOnce({
      UserPreference: {
        name: "k",
        value: "v",
        category: "sys_preferences",
        context: "private",
        userName: "Admin",
      },
    });
    const saved = await saveUserPreference({
      name: "k",
      value: "v",
      userName: "Admin",
    });
    expect(putSpy).toHaveBeenCalled();
    const body = putSpy.mock.calls[0][1] as Record<string, Record<string, string>>;
    // Must not send flat { name, ... } — server UNWRAP_ROOT_VALUE expects UserPreference
    expect(body).not.toHaveProperty("name");
    expect(body[USER_PREFERENCE_ROOT]).toBeDefined();
    expect(body[USER_PREFERENCE_ROOT].name).toBe("k");
    expect(body[USER_PREFERENCE_ROOT].value).toBe("v");
    expect(body[USER_PREFERENCE_ROOT].category).toBe("sys_preferences");
    expect(body[USER_PREFERENCE_ROOT].context).toBe("private");
    expect(body[USER_PREFERENCE_ROOT].userName).toBe("Admin");
    expect(saved.name).toBe("k");
    expect(saved.value).toBe("v");
  });

  it("wrapUserPreferenceForWire nests Gravatar override fields under root", () => {
    const wire = wrapUserPreferenceForWire({
      name: "perc_profile_gravatar_email",
      value: "avatar@example.com",
      userName: "Admin",
    });
    expect(Object.keys(wire)).toEqual([USER_PREFERENCE_ROOT]);
    expect(wire.UserPreference.value).toBe("avatar@example.com");
    expect(wire.UserPreference.name).toBe("perc_profile_gravatar_email");
  });

  it("unwrapUserPreference accepts flat and wrapped shapes", () => {
    expect(
      unwrapUserPreference({
        name: "k",
        value: "v",
      })?.value,
    ).toBe("v");
    expect(
      unwrapUserPreference({
        UserPreference: { name: "k", value: "wrapped" },
      })?.value,
    ).toBe("wrapped");
    expect(unwrapUserPreference(null)).toBeNull();
  });
});
