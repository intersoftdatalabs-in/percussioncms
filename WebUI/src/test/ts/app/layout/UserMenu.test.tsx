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
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import { MemoryRouter } from "react-router";
import { act, cleanup, fireEvent, render, screen, waitFor } from "@testing-library/react";
import { BootstrapProvider } from "../../../../main/ts/app/bootstrap/BootstrapContext";
import type { SpaBootstrap } from "../../../../main/ts/app/bootstrap/types";
import { UserMenu } from "../../../../main/ts/app/layout/UserMenu";
import { SESSION_COMMUNITY_CHANGED_EVENT } from "../../../../main/ts/app/layout/sessionCommunity";
import type { CurrentUserProfile } from "../../../../main/ts/api/user/userProfileApi";
import * as prefs from "../../../../main/ts/api/preferences/preferencesApi";

const getCurrentUserProfile = vi.fn();
const switchSessionCommunity = vi.fn();

vi.mock("../../../../main/ts/api/user/userProfileApi", () => ({
  getCurrentUserProfile: (...args: unknown[]) => getCurrentUserProfile(...args),
}));

vi.mock("../../../../main/ts/api/user/communitySwitchApi", async (importOriginal) => {
  const actual = await importOriginal<
    typeof import("../../../../main/ts/api/user/communitySwitchApi")
  >();
  return {
    ...actual,
    switchSessionCommunity: (...args: unknown[]) =>
      switchSessionCommunity(...args),
  };
});

vi.mock("../../../../main/ts/api/preferences/preferencesApi", () => ({
  loadUserPreference: vi.fn().mockResolvedValue(null),
  saveUserPreference: vi.fn().mockResolvedValue({ name: "", value: "" }),
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

function profile(overrides: Partial<CurrentUserProfile> = {}): CurrentUserProfile {
  return {
    name: "editor1",
    email: "editor1@example.com",
    providerType: "INTERNAL",
    roles: ["Editor"],
    communities: ["Default", "Corporate"],
    currentCommunity: "Default",
    defaultCommunity: "Default",
    adminUser: false,
    designerUser: false,
    accessibilityUser: false,
    emailEditable: true,
    ...overrides,
  };
}

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
    getCurrentUserProfile.mockReset();
    getCurrentUserProfile.mockResolvedValue(profile());
    switchSessionCommunity.mockReset();
    switchSessionCommunity.mockResolvedValue(undefined);
    vi.mocked(prefs.saveUserPreference).mockReset();
    vi.mocked(prefs.saveUserPreference).mockResolvedValue({
      name: "",
      value: "",
    });
    vi.mocked(prefs.getAllUserPreferences).mockClear();
    vi.mocked(prefs.loadUserPreference).mockClear();
  });

  afterEach(() => {
    cleanup();
  });

  it("shows signed-in name, My profile entry, logout, and avatar chip", async () => {
    renderMenu();
    expect(screen.getByTestId("perc-spa-user-menu")).toBeTruthy();
    expect(screen.getByTestId("perc-spa-user-name").textContent).toContain(
      "editor1",
    );
    const profileLink = screen.getByTestId("perc-spa-my-profile");
    expect(profileLink.textContent).toBe("My profile");
    expect(profileLink.getAttribute("href")).toBe("/cm/app/profile");
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

  it("shows the session community next to signed-in user", async () => {
    renderMenu();
    await waitFor(() => {
      expect(screen.getByTestId("perc-spa-community-name").textContent).toBe(
        "Default",
      );
    });
    expect(screen.getByTestId("perc-spa-community-switch").textContent).toBe(
      "Switch",
    );
  });

  it("lists only membership communities from current user, not the catalog", async () => {
    renderMenu();
    await waitFor(() => {
      expect(screen.getByTestId("perc-spa-community-switch")).toBeTruthy();
    });
    fireEvent.click(screen.getByTestId("perc-spa-community-switch"));
    const list = screen.getByTestId("perc-spa-community-list");
    const names = [...list.querySelectorAll("[data-community-name]")].map(
      (el) => el.getAttribute("data-community-name"),
    );
    expect(names).toEqual(["Default", "Corporate"]);
    expect(names).not.toContain("CatalogOnly");
  });

  it("switches community and updates chrome without logout", async () => {
    const events: string[] = [];
    const onChanged = (ev: Event): void => {
      const detail = (ev as CustomEvent<{ community: string }>).detail;
      events.push(detail.community);
    };
    window.addEventListener(SESSION_COMMUNITY_CHANGED_EVENT, onChanged);
    try {
      renderMenu();
      await waitFor(() => {
        expect(screen.getByTestId("perc-spa-community-name").textContent).toBe(
          "Default",
        );
      });
      fireEvent.click(screen.getByTestId("perc-spa-community-switch"));
      fireEvent.click(screen.getByTestId("perc-spa-community-option-corporate"));
      await waitFor(() => {
        expect(screen.getByTestId("perc-spa-community-name").textContent).toBe(
          "Corporate",
        );
      });
      expect(switchSessionCommunity).toHaveBeenCalledWith("Corporate");
      expect(events).toEqual(["Corporate"]);
      await waitFor(() => {
        expect(prefs.saveUserPreference).toHaveBeenCalledWith(
          expect.objectContaining({
            name: "perc_profile_lastCommunity",
            value: "Corporate",
            userName: "editor1",
          }),
        );
      });
      expect(prefs.getAllUserPreferences).not.toHaveBeenCalled();
      expect(screen.getByTestId("perc-spa-logout").getAttribute("href")).toBe(
        "/logout",
      );
    } finally {
      window.removeEventListener(SESSION_COMMUNITY_CHANGED_EVENT, onChanged);
    }
  });

  it("shows a clear error and keeps the current community when switch fails", async () => {
    switchSessionCommunity.mockRejectedValueOnce({
      status: 500,
      statusText: "Error",
      body: "User is not a member of community Corporate",
    });
    renderMenu();
    await waitFor(() => {
      expect(screen.getByTestId("perc-spa-community-switch")).toBeTruthy();
    });
    fireEvent.click(screen.getByTestId("perc-spa-community-switch"));
    fireEvent.click(screen.getByTestId("perc-spa-community-option-corporate"));
    await waitFor(() => {
      expect(screen.getByTestId("perc-spa-community-switch-error").textContent).toContain(
        "User is not a member of community Corporate",
      );
    });
    expect(screen.getByTestId("perc-spa-community-name").textContent).toBe(
      "Default",
    );
  });

  it("does not persist last community under the display-name fallback", async () => {
    renderMenu({ userName: "  " });
    await waitFor(() => {
      expect(screen.getByTestId("perc-spa-community-name").textContent).toBe(
        "Default",
      );
    });
    fireEvent.click(screen.getByTestId("perc-spa-community-switch"));
    fireEvent.click(screen.getByTestId("perc-spa-community-option-corporate"));
    await waitFor(() => {
      expect(screen.getByTestId("perc-spa-community-name").textContent).toBe(
        "Corporate",
      );
    });
    expect(switchSessionCommunity).toHaveBeenCalledWith("Corporate");
    expect(prefs.saveUserPreference).not.toHaveBeenCalled();
  });

  it("keeps the switched community when last-community persist fails", async () => {
    vi.mocked(prefs.saveUserPreference).mockRejectedValueOnce(
      new Error("prefs down"),
    );
    renderMenu();
    await waitFor(() => {
      expect(screen.getByTestId("perc-spa-community-name").textContent).toBe(
        "Default",
      );
    });
    fireEvent.click(screen.getByTestId("perc-spa-community-switch"));
    fireEvent.click(screen.getByTestId("perc-spa-community-option-corporate"));
    await waitFor(() => {
      expect(screen.getByTestId("perc-spa-community-name").textContent).toBe(
        "Corporate",
      );
    });
    expect(switchSessionCommunity).toHaveBeenCalledWith("Corporate");
    expect(screen.queryByTestId("perc-spa-community-switch-error")).toBeNull();
  });

  it("updates chrome when a session-start restore fires", async () => {
    renderMenu();
    await waitFor(() => {
      expect(screen.getByTestId("perc-spa-community-name").textContent).toBe(
        "Default",
      );
    });
    act(() => {
      window.dispatchEvent(
        new CustomEvent(SESSION_COMMUNITY_CHANGED_EVENT, {
          detail: { community: "Corporate" },
        }),
      );
    });
    await waitFor(() => {
      expect(screen.getByTestId("perc-spa-community-name").textContent).toBe(
        "Corporate",
      );
    });
  });
});
