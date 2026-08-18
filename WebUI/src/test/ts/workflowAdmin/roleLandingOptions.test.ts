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

import { describe, expect, it } from "vitest";
import { HOMEPAGE_TYPES } from "../../../main/ts/api/user/userHomepageApi";
import { fallbackLabelFromKey } from "../../../main/ts/i18n/message";
import {
  canonicalizeRoleHomepage,
  DEFAULT_ROLE_HOMEPAGE,
  roleLandingOptions,
} from "../../../main/ts/workflowAdmin/role/roleLandingOptions";

describe("roleLandingOptions", () => {
  it("lists remaining top-nav apps and not Editor or Design (#3537)", () => {
    const values = roleLandingOptions().map((o) => o.value);
    expect(values).toContain(HOMEPAGE_TYPES.HOME);
    expect(values).toContain(HOMEPAGE_TYPES.EXPLORER);
    expect(values).toContain(HOMEPAGE_TYPES.ARCHITECTURE);
    expect(values).toContain(HOMEPAGE_TYPES.DEVELOPER);
    expect(values).toContain(HOMEPAGE_TYPES.PUBLISH);
    expect(values).toContain(HOMEPAGE_TYPES.WORKFLOW);
    expect(values).not.toContain("");
    expect(values).not.toContain(HOMEPAGE_TYPES.EDITOR);
    expect(values).not.toContain(HOMEPAGE_TYPES.DESIGNER);
    expect(values).not.toContain(HOMEPAGE_TYPES.DASHBOARD);
  });

  it("labels Architecture as Navigation", () => {
    const arch = roleLandingOptions().find(
      (o) => o.value === HOMEPAGE_TYPES.ARCHITECTURE,
    );
    expect(arch).toBeTruthy();
    expect(fallbackLabelFromKey(arch!.labelKey)).toBe("Navigation");
  });

  it("keeps stale Editor, Design, and Dashboard so they can be cleared", () => {
    expect(
      roleLandingOptions(HOMEPAGE_TYPES.EDITOR).map((o) => o.value),
    ).toContain(HOMEPAGE_TYPES.EDITOR);
    expect(
      roleLandingOptions(HOMEPAGE_TYPES.DESIGNER).map((o) => o.value),
    ).toContain(HOMEPAGE_TYPES.DESIGNER);
    expect(
      roleLandingOptions(HOMEPAGE_TYPES.DASHBOARD).map((o) => o.value),
    ).toContain(HOMEPAGE_TYPES.DASHBOARD);
  });

  it("defaults blank stored homepage to Home", () => {
    expect(canonicalizeRoleHomepage("")).toBe(DEFAULT_ROLE_HOMEPAGE);
    expect(canonicalizeRoleHomepage(null)).toBe(DEFAULT_ROLE_HOMEPAGE);
    expect(canonicalizeRoleHomepage(HOMEPAGE_TYPES.EXPLORER)).toBe(
      HOMEPAGE_TYPES.EXPLORER,
    );
  });
});
