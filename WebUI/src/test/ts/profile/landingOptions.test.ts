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

describe("profileLandingOptions", () => {
  it("allows Home and Editor for any user", () => {
    expect(isProfileLandingAllowed("", {})).toBe(true);
    expect(isProfileLandingAllowed(HOMEPAGE_TYPES.HOME, {})).toBe(true);
    expect(isProfileLandingAllowed(HOMEPAGE_TYPES.EDITOR, {})).toBe(true);
    expect(isProfileLandingAllowed(HOMEPAGE_TYPES.DESIGNER, {})).toBe(false);
    expect(isProfileLandingAllowed(HOMEPAGE_TYPES.WORKFLOW, {})).toBe(false);
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

  it("opens Design for designers and Administration for admins", () => {
    expect(
      isProfileLandingAllowed(HOMEPAGE_TYPES.DESIGNER, { isDesigner: true }),
    ).toBe(true);
    expect(
      isProfileLandingAllowed(HOMEPAGE_TYPES.WORKFLOW, { isAdmin: true }),
    ).toBe(true);
    expect(
      isProfileLandingAllowed(HOMEPAGE_TYPES.DESIGNER, { isAdmin: true }),
    ).toBe(true);
  });

  it("keeps current value when no longer allowed", () => {
    const opts = profileLandingOptions({}, HOMEPAGE_TYPES.DESIGNER);
    expect(opts.some((o) => o.value === HOMEPAGE_TYPES.DESIGNER)).toBe(true);
  });
});
