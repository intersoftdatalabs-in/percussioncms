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

import React from "react";
import { beforeEach, describe, expect, it, vi } from "vitest";
import { render, screen, waitFor } from "@testing-library/react";
import { ProfileShell } from "../../../main/ts/profile/ProfileShell";
import { PROFILE_MSG } from "../../../main/ts/profile/messages";
import { BootstrapProvider } from "../../../main/ts/app/bootstrap/BootstrapContext";

vi.mock("../../../main/ts/api/user/userHomepageApi", () => ({
  getMyHomepageOverride: vi.fn().mockResolvedValue(""),
  setMyHomepageOverride: vi.fn().mockResolvedValue(""),
  HOMEPAGE_TYPES: {
    HOME: "Home",
    DASHBOARD: "Dashboard",
    EDITOR: "Editor",
    DESIGNER: "Designer",
    ARCHITECTURE: "Architecture",
    PUBLISH: "Publish",
    WORKFLOW: "Workflow",
    WIDGET_BUILDER: "WidgetBuilder",
  },
}));

vi.mock("../../../main/ts/api/preferences/preferencesApi", () => ({
  getAllUserPreferences: vi.fn().mockResolvedValue([]),
  loadUserPreference: vi.fn().mockResolvedValue(null),
  saveUserPreference: vi.fn(),
  PREF_CATEGORY_SYS: "sys_preferences",
  PREF_CONTEXT_PRIVATE: "private",
}));

vi.mock("../../../main/ts/api/user/userCurrentApi", () => ({
  getCurrentUserBasic: vi.fn().mockResolvedValue({
    name: "Admin",
    email: "admin@example.com",
  }),
  isValidEmailAddress: (email: string) =>
    !email || /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email),
}));

const bootstrap = {
  userName: "Admin",
  locale: "en-us",
  entry: "profile",
  isAdmin: true,
  isDesigner: true,
  isWidgetBuilderActive: false,
  allowExternalAvatarFetch: true,
};

function renderShell() {
  return render(
    <BootstrapProvider value={bootstrap}>
      <ProfileShell embedded />
    </BootstrapProvider>,
  );
}

vi.mock("../../../main/ts/api/user/userProfileApi", () => ({
  getCurrentUserProfile: vi.fn().mockResolvedValue({
    name: "Admin",
    email: "admin@example.com",
    providerType: "INTERNAL",
    roles: ["Admin"],
    communities: ["Default"],
    currentCommunity: "Default",
    adminUser: true,
    designerUser: false,
    accessibilityUser: false,
    emailEditable: true,
  }),
  updateMyAccountEmail: vi.fn(),
  // Mirror production isValidEmailAddress (userProfileApi) so shell tests do not mask shape bugs.
  isValidEmailAddress: (email: string) => {
    const value = (email ?? "").trim();
    if (!value) {
      return true;
    }
    if (value.length > 254) {
      return false;
    }
    return /^[A-Za-z0-9._%+-]+@(?:[A-Za-z0-9](?:[A-Za-z0-9-]{0,61}[A-Za-z0-9])?\.)+[A-Za-z]{2,}$/.test(
      value,
    );
  },
  normalizeCurrentUser: vi.fn(),
}));

describe("ProfileShell", () => {
  beforeEach(() => {
    (window as unknown as { I18N: { message: (k: string) => string } }).I18N = {
      message: (key: string) => {
        const at = key.indexOf("@");
        return at >= 0 ? key.slice(at + 1) : key;
      },
    };
  });

  it("renders title, intro, four sections, live account/preferences/avatar", async () => {
    renderShell();
    expect(screen.getByTestId("perc-profile-shell")).toBeTruthy();
    expect(screen.getByTestId("perc-profile-title").textContent).toBe("My profile");
    expect(screen.getByTestId("perc-profile-intro").textContent).toContain(
      "account settings",
    );
    expect(screen.getByTestId("perc-profile-section-account")).toBeTruthy();
    expect(screen.getByTestId("perc-profile-section-security")).toBeTruthy();
    expect(screen.getByTestId("perc-profile-section-preferences")).toBeTruthy();
    expect(screen.getByTestId("perc-profile-section-avatar")).toBeTruthy();

    await waitFor(() => {
      expect(screen.getByTestId("perc-profile-account")).toBeTruthy();
      expect(screen.getByTestId("perc-profile-account-login").textContent).toBe(
        "Admin",
      );
      expect(screen.getByTestId("perc-profile-preferences")).toBeTruthy();
      expect(screen.getByTestId("perc-profile-avatar")).toBeTruthy();
    });
  });

  it("exposes landmark heading hierarchy and section jump links", async () => {
    renderShell();
    await waitFor(() => {
      expect(screen.getByTestId("perc-profile-account")).toBeTruthy();
      expect(screen.getByTestId("perc-profile-preferences")).toBeTruthy();
      expect(screen.getByTestId("perc-profile-avatar")).toBeTruthy();
    });
    const h1 = screen.getByRole("heading", { level: 1, name: "My profile" });
    expect(h1).toBeTruthy();

    const account = screen.getByRole("heading", { level: 2, name: "Account" });
    const security = screen.getByRole("heading", { level: 2, name: "Security" });
    const preferences = screen.getByRole("heading", {
      level: 2,
      name: "Preferences",
    });
    const avatar = screen.getByRole("heading", { level: 2, name: "Avatar" });
    expect(account).toBeTruthy();
    expect(security).toBeTruthy();
    expect(preferences).toBeTruthy();
    expect(avatar).toBeTruthy();

    const nav = screen.getByTestId("perc-profile-section-nav");
    expect(nav.getAttribute("aria-labelledby")).toBeTruthy();
    expect(screen.getByTestId("perc-profile-nav-account").getAttribute("href")).toBe(
      "#perc-profile-account",
    );
    expect(screen.getByTestId("perc-profile-nav-security").getAttribute("href")).toBe(
      "#perc-profile-security",
    );
  });

  it("marks only security as coming soon; account/preferences/avatar live", async () => {
    renderShell();
    await waitFor(() => {
      expect(screen.getByTestId("perc-profile-account")).toBeTruthy();
      expect(screen.getByTestId("perc-profile-preferences")).toBeTruthy();
      expect(screen.getByTestId("perc-profile-avatar")).toBeTruthy();
    });
    const statuses = screen.getAllByText("Coming soon");
    // Only Security remains a placeholder
    expect(statuses.length).toBe(1);
    expect(PROFILE_MSG.COMING_SOON).toContain("Coming soon");
    expect(screen.queryByTestId("perc-profile-section-account-status")).toBeNull();
    expect(screen.queryByTestId("perc-profile-section-preferences-status")).toBeNull();
    expect(screen.queryByTestId("perc-profile-section-avatar-status")).toBeNull();
    expect(screen.getByTestId("perc-profile-section-security-status")).toBeTruthy();
  });

  it("makes section landmarks focusable skip targets (tabIndex=-1)", async () => {
    renderShell();
    await waitFor(() => {
      expect(screen.getByTestId("perc-profile-account")).toBeTruthy();
      expect(screen.getByTestId("perc-profile-preferences")).toBeTruthy();
    });
    for (const id of [
      "perc-profile-section-account",
      "perc-profile-section-security",
      "perc-profile-section-preferences",
      "perc-profile-section-avatar",
    ]) {
      const section = screen.getByTestId(id);
      expect(section.getAttribute("tabindex")).toBe("-1");
      expect(section.id).toMatch(/^perc-profile-/);
    }
  });
});
