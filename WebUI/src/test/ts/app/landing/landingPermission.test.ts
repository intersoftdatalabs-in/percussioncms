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
import { HOMEPAGE_TYPES } from "../../../../main/ts/api/user/userHomepageApi";
import {
  isLandingAllowed,
  landingGatesFromRoles,
} from "../../../../main/ts/app/landing/landingPermission";

describe("landingGatesFromRoles", () => {
  it("treats Admin as admin+designer and Designer as designer only", () => {
    expect(landingGatesFromRoles(["Admin"])).toEqual({
      isAdmin: true,
      isDesigner: true,
    });
    expect(landingGatesFromRoles(["Designer"])).toEqual({
      isAdmin: false,
      isDesigner: true,
    });
    expect(landingGatesFromRoles(["Contributor"])).toEqual({
      isAdmin: false,
      isDesigner: false,
    });
    expect(landingGatesFromRoles(["Editor"])).toEqual({
      isAdmin: false,
      isDesigner: false,
    });
  });

  it("matches role names case-insensitively", () => {
    expect(landingGatesFromRoles(["ADMIN", " designer "])).toEqual({
      isAdmin: true,
      isDesigner: true,
    });
  });
});

describe("isLandingAllowed contributor vs designer vs admin (#3538)", () => {
  const contributor = { isAdmin: false, isDesigner: false };
  const designer = { isAdmin: false, isDesigner: true };
  const admin = { isAdmin: true, isDesigner: false };

  it("always allows role-default, Home, and Explorer", () => {
    for (const gates of [contributor, designer, admin]) {
      expect(isLandingAllowed("", gates)).toBe(true);
      expect(isLandingAllowed(HOMEPAGE_TYPES.HOME, gates)).toBe(true);
      expect(isLandingAllowed(HOMEPAGE_TYPES.EXPLORER, gates)).toBe(true);
    }
  });

  it("never offers Editor, Design, or Widget Builder as new choices", () => {
    for (const gates of [contributor, designer, admin]) {
      expect(isLandingAllowed(HOMEPAGE_TYPES.EDITOR, gates)).toBe(false);
      expect(isLandingAllowed(HOMEPAGE_TYPES.DESIGNER, gates)).toBe(false);
      expect(isLandingAllowed(HOMEPAGE_TYPES.WIDGET_BUILDER, gates)).toBe(
        false,
      );
    }
  });

  it("contributor-class users only get Home/Explorer — not Nav/Dev/Publish/Admin", () => {
    expect(isLandingAllowed(HOMEPAGE_TYPES.ARCHITECTURE, contributor)).toBe(
      false,
    );
    expect(isLandingAllowed(HOMEPAGE_TYPES.DEVELOPER, contributor)).toBe(
      false,
    );
    expect(isLandingAllowed(HOMEPAGE_TYPES.PUBLISH, contributor)).toBe(false);
    expect(isLandingAllowed(HOMEPAGE_TYPES.WORKFLOW, contributor)).toBe(false);
  });

  it("designers get Navigation/Developer/Publish but not Admin", () => {
    expect(isLandingAllowed(HOMEPAGE_TYPES.ARCHITECTURE, designer)).toBe(true);
    expect(isLandingAllowed(HOMEPAGE_TYPES.DEVELOPER, designer)).toBe(true);
    expect(isLandingAllowed(HOMEPAGE_TYPES.PUBLISH, designer)).toBe(true);
    expect(isLandingAllowed(HOMEPAGE_TYPES.WORKFLOW, designer)).toBe(false);
  });

  it("admins get Navigation/Developer/Publish and Admin", () => {
    expect(isLandingAllowed(HOMEPAGE_TYPES.ARCHITECTURE, admin)).toBe(true);
    expect(isLandingAllowed(HOMEPAGE_TYPES.DEVELOPER, admin)).toBe(true);
    expect(isLandingAllowed(HOMEPAGE_TYPES.PUBLISH, admin)).toBe(true);
    expect(isLandingAllowed(HOMEPAGE_TYPES.WORKFLOW, admin)).toBe(true);
  });
});
