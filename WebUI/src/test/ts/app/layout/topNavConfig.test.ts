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
  ADMIN_NAV_LANDING,
  ARCHITECTURE_NAV_LANDING,
  adminTopNavLabel,
  isAdminNavPath,
  isWidgetBuilderDeveloperEntry,
  topNavItemIds,
} from "../../../../main/ts/app/layout/topNavConfig";

describe("topNavItemIds (#2702)", () => {
  it("places Explorer immediately after Home for all roles", () => {
    for (const gates of [
      {},
      { isAdmin: true, isDesigner: true, isWidgetBuilderActive: true },
      { isAdmin: false, isDesigner: true },
      { isAdmin: false, isDesigner: false },
    ]) {
      const ids = topNavItemIds(gates);
      expect(ids[0]).toBe("home");
      expect(ids[1]).toBe("explorer");
      expect(ids).not.toContain("dashboard" as never);
    }
  });

  it("does not expose Admin for non-admin users", () => {
    expect(topNavItemIds({ isAdmin: false, isDesigner: true })).not.toContain(
      "admin",
    );
    expect(topNavItemIds({ isAdmin: false, isDesigner: false })).not.toContain(
      "admin",
    );
  });

  it("exposes a single consolidated Admin item for admins", () => {
    const ids = topNavItemIds({
      isAdmin: true,
      isDesigner: true,
      isWidgetBuilderActive: true,
    });
    expect(ids.filter((id) => id === "admin")).toEqual(["admin"]);
    expect(ids).toEqual([
      "home",
      "explorer",
      "architecture",
      "developer",
      "publish",
      "admin",
    ]);
  });

  it("includes designer publish surfaces without Admin", () => {
    expect(
      topNavItemIds({ isAdmin: false, isDesigner: true }),
    ).toEqual([
      "home",
      "explorer",
      "architecture",
      "developer",
      "publish",
    ]);
  });

  it("does not expose Editor, Design, or Widget Builder as top-nav items (#3514)", () => {
    const ids = topNavItemIds({
      isAdmin: true,
      isDesigner: true,
      isWidgetBuilderActive: true,
    });
    expect(ids).not.toContain("editor" as never);
    expect(ids).not.toContain("design" as never);
    expect(ids).not.toContain("widget-builder" as never);
    expect(isWidgetBuilderDeveloperEntry({
      isAdmin: true,
      isDesigner: true,
      isWidgetBuilderActive: true,
    })).toBe(true);
    expect(
      isWidgetBuilderDeveloperEntry({
        isAdmin: false,
        isDesigner: true,
        isWidgetBuilderActive: false,
      }),
    ).toBe(false);
    expect(
      isWidgetBuilderDeveloperEntry({
        isAdmin: false,
        isDesigner: false,
        isWidgetBuilderActive: true,
      }),
    ).toBe(false);
  });
});

describe("ADMIN_NAV_LANDING (#2784 / #3201)", () => {
  it("points consolidated Admin top-nav at Admin tools shell", () => {
    expect(ADMIN_NAV_LANDING).toBe("/admin");
    expect(isAdminNavPath(ADMIN_NAV_LANDING)).toBe(true);
  });
});

describe("adminTopNavLabel (#3201)", () => {
  it("normalizes English Administration leftover to Admin", () => {
    expect(adminTopNavLabel("Administration")).toBe("Admin");
    expect(adminTopNavLabel("  Administration  ")).toBe("Admin");
    expect(adminTopNavLabel("")).toBe("Admin");
  });

  it("keeps TMX Admin and non-English labels", () => {
    expect(adminTopNavLabel("Admin")).toBe("Admin");
    expect(adminTopNavLabel("Administrateur")).toBe("Administrateur");
    expect(adminTopNavLabel("Beheerder")).toBe("Beheerder");
  });
});

describe("ARCHITECTURE_NAV_LANDING (#3094)", () => {
  it("points Architecture top-nav at SPA shell path", () => {
    expect(ARCHITECTURE_NAV_LANDING).toBe("/architecture");
  });
});

describe("isAdminNavPath", () => {
  it("marks unified Admin shell and legacy workflow redirect paths as Admin-active (#3088)", () => {
    expect(isAdminNavPath("/admin")).toBe(true);
    expect(isAdminNavPath("/admin/tools")).toBe(true);
    expect(isAdminNavPath("/admin/workflow")).toBe(true);
    expect(isAdminNavPath("/admin/roles")).toBe(true);
    // Legacy /workflow* still active while redirect resolves
    expect(isAdminNavPath("/workflow")).toBe(true);
    expect(isAdminNavPath("/workflow/roles")).toBe(true);
    expect(isAdminNavPath("/home")).toBe(false);
    expect(isAdminNavPath("/explorer")).toBe(false);
  });
});
