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
  isProfileLandingAllowed,
  profileLandingOptions,
} from "../../../main/ts/profile/landingOptions";
import { HOMEPAGE_TYPES } from "../../../main/ts/api/user/userHomepageApi";
import { fallbackLabelFromKey } from "../../../main/ts/i18n/message";

describe("profileLandingOptions", () => {
  it("allows Home and Explorer for any user and does not offer Editor or Design (#3514)", () => {
    expect(isProfileLandingAllowed("", {})).toBe(true);
    expect(isProfileLandingAllowed(HOMEPAGE_TYPES.HOME, {})).toBe(true);
    expect(isProfileLandingAllowed(HOMEPAGE_TYPES.EXPLORER, {})).toBe(true);
    expect(isProfileLandingAllowed(HOMEPAGE_TYPES.EDITOR, {})).toBe(false);
    expect(isProfileLandingAllowed(HOMEPAGE_TYPES.DESIGNER, {})).toBe(false);
    expect(isProfileLandingAllowed(HOMEPAGE_TYPES.WORKFLOW, {})).toBe(false);
    const opts = profileLandingOptions({ isAdmin: true, isDesigner: true });
    expect(opts.some((o) => o.value === HOMEPAGE_TYPES.EDITOR)).toBe(false);
    expect(opts.some((o) => o.value === HOMEPAGE_TYPES.DESIGNER)).toBe(false);
  });

  it("offers remaining top-nav apps and not Editor or Design (#3536)", () => {
    const values = profileLandingOptions({ isAdmin: true, isDesigner: true }).map(
      (o) => o.value,
    );
    expect(values).toContain("");
    expect(values).toContain(HOMEPAGE_TYPES.HOME);
    expect(values).toContain(HOMEPAGE_TYPES.EXPLORER);
    expect(values).toContain(HOMEPAGE_TYPES.ARCHITECTURE);
    expect(values).toContain(HOMEPAGE_TYPES.DEVELOPER);
    expect(values).toContain(HOMEPAGE_TYPES.PUBLISH);
    expect(values).toContain(HOMEPAGE_TYPES.WORKFLOW);
    expect(values).not.toContain(HOMEPAGE_TYPES.EDITOR);
    expect(values).not.toContain(HOMEPAGE_TYPES.DESIGNER);
  });

  it("includes Architecture (Navigation) landing for designers", () => {
    expect(
      isProfileLandingAllowed(HOMEPAGE_TYPES.ARCHITECTURE, {
        isDesigner: true,
      }),
    ).toBe(true);
    const opts = profileLandingOptions({ isDesigner: true });
    const arch = opts.find((o) => o.value === HOMEPAGE_TYPES.ARCHITECTURE);
    expect(arch).toBeTruthy();
    expect(arch?.labelKey).toMatch(/Navigation/i);
  });

  it("hides Architecture (Navigation) landing for non-designers", () => {
    expect(isProfileLandingAllowed(HOMEPAGE_TYPES.ARCHITECTURE, {})).toBe(
      false,
    );
    const opts = profileLandingOptions({});
    expect(
      opts.some((o) => o.value === HOMEPAGE_TYPES.ARCHITECTURE),
    ).toBe(false);
    expect(opts.some((o) => o.value === HOMEPAGE_TYPES.EXPLORER)).toBe(true);
  });

  it("opens Developer/Publish for designers and Administration for admins", () => {
    expect(
      isProfileLandingAllowed(HOMEPAGE_TYPES.DEVELOPER, { isDesigner: true }),
    ).toBe(true);
    expect(
      isProfileLandingAllowed(HOMEPAGE_TYPES.PUBLISH, { isDesigner: true }),
    ).toBe(true);
    expect(
      isProfileLandingAllowed(HOMEPAGE_TYPES.DESIGNER, { isDesigner: true }),
    ).toBe(false);
    expect(
      isProfileLandingAllowed(HOMEPAGE_TYPES.WORKFLOW, { isAdmin: true }),
    ).toBe(true);
    expect(
      isProfileLandingAllowed(HOMEPAGE_TYPES.DESIGNER, { isAdmin: true }),
    ).toBe(false);
  });

  it("keeps stale Editor/Design current values so the user can clear them", () => {
    const design = profileLandingOptions({}, HOMEPAGE_TYPES.DESIGNER);
    expect(design.some((o) => o.value === HOMEPAGE_TYPES.DESIGNER)).toBe(true);
    const editor = profileLandingOptions({}, HOMEPAGE_TYPES.EDITOR);
    expect(editor.some((o) => o.value === HOMEPAGE_TYPES.EDITOR)).toBe(true);
  });

  it("labels the Architecture homepage type as Navigation (#3217)", () => {
    const opt = profileLandingOptions({ isDesigner: true }).find(
      (o) => o.value === HOMEPAGE_TYPES.ARCHITECTURE,
    );
    expect(opt).toBeTruthy();
    expect(fallbackLabelFromKey(opt!.labelKey)).toBe("Navigation");
  });
});
