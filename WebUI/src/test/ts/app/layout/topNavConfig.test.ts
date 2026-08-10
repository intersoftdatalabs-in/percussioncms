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
  isAdminNavPath,
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
      "editor",
      "architecture",
      "design",
      "developer",
      "publish",
      "admin",
      "widget-builder",
    ]);
  });

  it("includes designer publish surfaces without Admin", () => {
    expect(
      topNavItemIds({ isAdmin: false, isDesigner: true }),
    ).toEqual([
      "home",
      "explorer",
      "editor",
      "architecture",
      "design",
      "developer",
      "publish",
    ]);
  });
});

describe("ADMIN_NAV_LANDING (#2784)", () => {
  it("points consolidated Admin top-nav at Admin tools shell", () => {
    expect(ADMIN_NAV_LANDING).toBe("/admin");
    expect(isAdminNavPath(ADMIN_NAV_LANDING)).toBe(true);
  });
});

describe("isAdminNavPath", () => {
  it("marks admin-tools and workflow SPA paths as Admin-active", () => {
    expect(isAdminNavPath("/admin")).toBe(true);
    expect(isAdminNavPath("/admin/tools")).toBe(true);
    expect(isAdminNavPath("/workflow")).toBe(true);
    expect(isAdminNavPath("/workflow/roles")).toBe(true);
    expect(isAdminNavPath("/home")).toBe(false);
    expect(isAdminNavPath("/explorer")).toBe(false);
  });
});
