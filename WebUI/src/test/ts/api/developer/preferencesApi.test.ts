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
  loadDefaultAclTemplate,
  templateFromPreferenceValue,
} from "../../../../main/ts/api/developer/preferencesApi";
import * as prefs from "../../../../main/ts/api/preferences/preferencesApi";
import {
  serializeDefaultAclTemplate,
  systemDefaultAclTemplate,
} from "../../../../main/ts/developer/defaultAclTemplate";

const VISIBLE_TEMPLATE = (() => {
  const t = systemDefaultAclTemplate();
  t.entries[0].permissions = [
    ...t.entries[0].permissions,
    "RUNTIME_VISIBLE",
  ];
  return t;
})();

const VISIBLE_JSON = serializeDefaultAclTemplate(VISIBLE_TEMPLATE);

describe("developer preferencesApi — default ACL template (#3204)", () => {
  beforeEach(() => {
    vi.restoreAllMocks();
  });

  afterEach(() => {
    vi.restoreAllMocks();
  });

  it("templateFromPreferenceValue keeps RUNTIME_VISIBLE from JSON string", () => {
    const parsed = templateFromPreferenceValue({
      name: "developer.defaultObjectAclTemplate",
      value: VISIBLE_JSON,
    });
    expect(parsed).not.toBeNull();
    expect(parsed!.entries[0].permissions).toContain("RUNTIME_VISIBLE");
  });

  it("templateFromPreferenceValue keeps RUNTIME_VISIBLE from parsed object value", () => {
    const parsed = templateFromPreferenceValue({
      name: "developer.defaultObjectAclTemplate",
      value: VISIBLE_TEMPLATE as unknown as string,
    });
    expect(parsed).not.toBeNull();
    expect(parsed!.entries[0].permissions).toContain("RUNTIME_VISIBLE");
  });

  it("loadDefaultAclTemplate uses GET-by-name when value is present", async () => {
    vi.spyOn(prefs, "loadUserPreference").mockResolvedValueOnce({
      name: "developer.defaultObjectAclTemplate",
      value: VISIBLE_JSON,
    });
    const listSpy = vi.spyOn(prefs, "getAllUserPreferences");
    const result = await loadDefaultAclTemplate();
    expect(result.fromPreference).toBe(true);
    expect(result.template.entries[0].permissions).toContain("RUNTIME_VISIBLE");
    expect(listSpy).not.toHaveBeenCalled();
  });

  it("falls back to preference list when GET-by-name is empty (#3204)", async () => {
    vi.spyOn(prefs, "loadUserPreference").mockResolvedValueOnce(null);
    vi.spyOn(prefs, "getAllUserPreferences").mockResolvedValueOnce([
      {
        name: "developer.defaultObjectAclTemplate",
        value: VISIBLE_JSON,
      },
    ]);
    const result = await loadDefaultAclTemplate();
    expect(result.fromPreference).toBe(true);
    expect(result.template.entries[0].name).toBe("Default");
    expect(result.template.entries[0].permissions).toContain("RUNTIME_VISIBLE");
  });

  it("uses system default when neither GET-by-name nor list has a template", async () => {
    vi.spyOn(prefs, "loadUserPreference").mockResolvedValueOnce(null);
    vi.spyOn(prefs, "getAllUserPreferences").mockResolvedValueOnce([]);
    const result = await loadDefaultAclTemplate();
    expect(result.fromPreference).toBe(false);
    expect(result.template.entries[0].permissions).not.toContain(
      "RUNTIME_VISIBLE",
    );
  });
});
