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
import { HOMEPAGE_TYPES } from "../../../main/ts/api/user/userHomepageApi";
import {
  isNavigationHomepageToken,
  resolveHomepageToClientPath,
  resolveHomepageToSpaEntry,
} from "../../../main/ts/profile/resolveLandingEntry";

describe("resolveHomepageToSpaEntry (#3219)", () => {
  it("maps Architecture and Navigation aliases to the architecture SPA", () => {
    expect(resolveHomepageToSpaEntry(HOMEPAGE_TYPES.ARCHITECTURE)).toBe(
      "architecture",
    );
    expect(resolveHomepageToSpaEntry("Navigation")).toBe("architecture");
    expect(resolveHomepageToSpaEntry("navigation")).toBe("architecture");
    expect(resolveHomepageToSpaEntry("arch")).toBe("architecture");
    expect(resolveHomepageToSpaEntry("Architecture")).toBe("architecture");
    expect(resolveHomepageToClientPath("Navigation")).toBe("/architecture");
    expect(resolveHomepageToClientPath(HOMEPAGE_TYPES.ARCHITECTURE)).toBe(
      "/architecture",
    );
    expect(isNavigationHomepageToken("Navigation")).toBe(true);
    expect(isNavigationHomepageToken(HOMEPAGE_TYPES.ARCHITECTURE)).toBe(true);
  });

  it("does not send Home / Editor / unknown tokens to architecture", () => {
    expect(resolveHomepageToSpaEntry(HOMEPAGE_TYPES.HOME)).toBe("home");
    expect(resolveHomepageToSpaEntry("")).toBe("home");
    expect(resolveHomepageToSpaEntry(null)).toBe("home");
    expect(resolveHomepageToSpaEntry("Editor")).toBe("home");
    expect(resolveHomepageToSpaEntry("NotAModule")).toBe("home");
    expect(resolveHomepageToClientPath("Home")).toBe("/home");
    expect(isNavigationHomepageToken("Home")).toBe(false);
  });

  it("maps other product homepage types to their SPA entries", () => {
    expect(resolveHomepageToSpaEntry(HOMEPAGE_TYPES.DESIGNER)).toBe("design");
    expect(resolveHomepageToSpaEntry(HOMEPAGE_TYPES.PUBLISH)).toBe("publish");
    expect(resolveHomepageToSpaEntry(HOMEPAGE_TYPES.WORKFLOW)).toBe("admin");
    expect(resolveHomepageToSpaEntry(HOMEPAGE_TYPES.WIDGET_BUILDER)).toBe(
      "widget-builder",
    );
    expect(resolveHomepageToClientPath(HOMEPAGE_TYPES.HOME)).toBe("/home");
    expect(resolveHomepageToClientPath(HOMEPAGE_TYPES.DASHBOARD)).toBe("/home");
    expect(resolveHomepageToClientPath(HOMEPAGE_TYPES.EDITOR)).toBe("/home");
    expect(resolveHomepageToClientPath(HOMEPAGE_TYPES.DESIGNER)).toBe("/design");
    expect(resolveHomepageToClientPath(HOMEPAGE_TYPES.PUBLISH)).toBe("/publish");
    expect(resolveHomepageToClientPath(HOMEPAGE_TYPES.WORKFLOW)).toBe("/admin");
    expect(resolveHomepageToClientPath(HOMEPAGE_TYPES.WIDGET_BUILDER)).toBe(
      "/widget-builder",
    );
    expect(resolveHomepageToSpaEntry(HOMEPAGE_TYPES.EXPLORER)).toBe("explorer");
    expect(resolveHomepageToSpaEntry(HOMEPAGE_TYPES.DEVELOPER)).toBe(
      "developer",
    );
    expect(resolveHomepageToClientPath(HOMEPAGE_TYPES.EXPLORER)).toBe(
      "/explorer",
    );
    expect(resolveHomepageToClientPath(HOMEPAGE_TYPES.DEVELOPER)).toBe(
      "/developer",
    );
    expect(resolveHomepageToClientPath("explorer")).toBe("/explorer");
    expect(resolveHomepageToClientPath("profile")).toBe("/profile");
    expect(resolveHomepageToClientPath("developer")).toBe("/developer");
  });
});
