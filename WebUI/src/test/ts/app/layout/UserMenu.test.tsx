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
import { MemoryRouter } from "react-router";
import { render, screen, waitFor } from "@testing-library/react";
import { BootstrapProvider } from "../../../../main/ts/app/bootstrap/BootstrapContext";
import type { SpaBootstrap } from "../../../../main/ts/app/bootstrap/types";
import { UserMenu } from "../../../../main/ts/app/layout/UserMenu";
import * as prefs from "../../../../main/ts/api/preferences/preferencesApi";

vi.mock("../../../../main/ts/api/user/userCurrentApi", () => ({
  getCurrentUserBasic: vi.fn().mockResolvedValue({
    name: "editor1",
    email: "editor1@example.com",
  }),
  isValidEmailAddress: () => true,
}));

vi.mock("../../../../main/ts/api/preferences/preferencesApi", () => ({
  loadUserPreference: vi.fn().mockResolvedValue(null),
  saveUserPreference: vi.fn(),
  getAllUserPreferences: vi.fn().mockResolvedValue([]),
  PREF_CATEGORY_SYS: "sys_preferences",
  PREF_CONTEXT_PRIVATE: "private",
}));

const bootstrap: SpaBootstrap = {
  userName: "editor1",
  locale: "en-us",
  entry: "home",
  isAdmin: false,
  isDesigner: false,
  isWidgetBuilderActive: false,
  allowExternalAvatarFetch: true,
};

function renderMenu(user?: Partial<SpaBootstrap>): void {
  render(
    <BootstrapProvider value={{ ...bootstrap, ...user }}>
      <MemoryRouter basename="/cm/app" initialEntries={["/cm/app/home"]}>
        <UserMenu />
      </MemoryRouter>
    </BootstrapProvider>,
  );
}

describe("UserMenu", () => {
  beforeEach(() => {
    (window as unknown as { I18N: { message: (k: string) => string } }).I18N = {
      message: (key: string) => {
        const at = key.indexOf("@");
        return at >= 0 ? key.slice(at + 1) : key;
      },
    };
  });

  it("shows signed-in name, My profile entry, logout, and avatar chip", async () => {
    renderMenu();
    expect(screen.getByTestId("perc-spa-user-menu")).toBeTruthy();
    expect(screen.getByTestId("perc-spa-user-name").textContent).toContain(
      "editor1",
    );
    const profile = screen.getByTestId("perc-spa-my-profile");
    expect(profile.textContent).toBe("My profile");
    expect(profile.getAttribute("href")).toBe("/cm/app/profile");
    const logout = screen.getByTestId("perc-spa-logout");
    expect(logout.getAttribute("href")).toBe("/logout");
    await waitFor(() => {
      const avatar = screen.getByTestId("perc-spa-user-avatar");
      expect(avatar).toBeTruthy();
      expect(avatar.getAttribute("aria-label")).toContain("editor1");
    });
  });

  it("falls back to default user label when userName is blank", async () => {
    renderMenu({ userName: "  " });
    expect(screen.getByTestId("perc-spa-user-name").textContent).toBe("user");
    await waitFor(() => {
      expect(screen.getByTestId("perc-spa-user-avatar")).toBeTruthy();
    });
  });

  it("does not GET named preferences from chrome (#3458)", async () => {
    renderMenu();
    await waitFor(() => {
      expect(screen.getByTestId("perc-spa-user-avatar")).toBeTruthy();
    });
    expect(prefs.loadUserPreference).not.toHaveBeenCalled();
    expect(prefs.getAllUserPreferences).not.toHaveBeenCalled();
  });

  it("shows initials when external avatar fetch is disabled", async () => {
    renderMenu({ allowExternalAvatarFetch: false });
    await waitFor(() => {
      const avatar = screen.getByTestId("perc-spa-user-avatar");
      expect(avatar.getAttribute("data-avatar-mode")).toBe("initials");
      expect(screen.getByTestId("perc-spa-user-avatar-initials").textContent).toBe(
        "ED",
      );
    });
  });
});
