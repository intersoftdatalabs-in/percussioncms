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

  it("saveUserPreference puts DTO with defaults", async () => {
    const putSpy = vi.spyOn(client, "put").mockResolvedValueOnce({
      name: "k",
      value: "v",
      category: "sys_preferences",
      context: "private",
      userName: "Admin",
    });
    await saveUserPreference({ name: "k", value: "v", userName: "Admin" });
    expect(putSpy).toHaveBeenCalled();
    const body = putSpy.mock.calls[0][1] as Record<string, string>;
    expect(body.category).toBe("sys_preferences");
    expect(body.context).toBe("private");
    expect(body.userName).toBe("Admin");
  });
});
