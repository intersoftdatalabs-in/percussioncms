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
import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import {
  AccountSection,
  allowedDefaultCommunityDraft,
} from "../../../main/ts/profile/AccountSection";
import type { CurrentUserProfile } from "../../../main/ts/api/user/userProfileApi";
import {
  isValidEmailAddress,
  normalizeCurrentUser,
} from "../../../main/ts/api/user/userProfileApi";

function internalProfile(
  overrides: Partial<CurrentUserProfile> = {},
): CurrentUserProfile {
  return {
    name: "Admin",
    email: "admin@example.com",
    providerType: "INTERNAL",
    roles: ["Admin", "Editor"],
    communities: ["Default"],
    currentCommunity: "Default",
    defaultCommunity: "Default",
    adminUser: true,
    designerUser: false,
    accessibilityUser: false,
    emailEditable: true,
    ...overrides,
  };
}

describe("normalizeCurrentUser / isValidEmailAddress", () => {
  it("unwraps CurrentUser root and marks INTERNAL email editable", () => {
    const profile = normalizeCurrentUser({
      CurrentUser: {
        name: "Editor1",
        email: "e@example.com",
        providerType: "INTERNAL",
        roles: ["Editor"],
        communities: ["Default", "Corporate"],
        currentCommunity: "Default",
        adminUser: false,
      },
    });
    expect(profile.name).toBe("Editor1");
    expect(profile.email).toBe("e@example.com");
    expect(profile.emailEditable).toBe(true);
    expect(profile.communities).toEqual(["Default", "Corporate"]);
    expect(profile.defaultCommunity).toBe("");
  });

  it("unwraps stored defaultCommunity from GET current user", () => {
    const profile = normalizeCurrentUser({
      CurrentUser: {
        name: "Admin",
        communities: ["Default", "Corporate"],
        currentCommunity: "Corporate",
        defaultCommunity: "Default",
      },
    });
    expect(profile.defaultCommunity).toBe("Default");
  });

  it("marks DIRECTORY as not email-editable", () => {
    const profile = normalizeCurrentUser({
      name: "ldap.user",
      providerType: "DIRECTORY",
      email: "ldap@corp.example",
      roles: ["Editor"],
    });
    expect(profile.emailEditable).toBe(false);
    expect(profile.providerType).toBe("DIRECTORY");
  });

  it("validates email shapes", () => {
    expect(isValidEmailAddress("")).toBe(true);
    expect(isValidEmailAddress("a@b.co")).toBe(true);
    expect(isValidEmailAddress("bad")).toBe(false);
    expect(isValidEmailAddress("user@domain..com")).toBe(false);
    expect(isValidEmailAddress("user@-domain.com")).toBe(false);
    expect(isValidEmailAddress("user@domain-.com")).toBe(false);
  });
});

describe("AccountSection", () => {
  beforeEach(() => {
    (window as unknown as { I18N: { message: (k: string) => string } }).I18N = {
      message: (key: string) => key,
    };
  });

  it("renders identity fields for an internal user with editable email", async () => {
    const loadProfile = vi.fn().mockResolvedValue(internalProfile());
    render(<AccountSection loadProfile={loadProfile} />);

    await waitFor(() => {
      expect(screen.getByTestId("perc-profile-account-login").textContent).toBe(
        "Admin",
      );
    });
    expect(screen.getByTestId("perc-profile-account-provider").textContent).toBe(
      "Internal",
    );
    expect(screen.getByTestId("perc-profile-account-roles").textContent).toContain(
      "Admin",
    );
    expect(
      screen.getByTestId("perc-profile-account-communities").textContent,
    ).toContain("Default");

    const email = screen.getByTestId(
      "perc-profile-account-email",
    ) as HTMLInputElement;
    expect(email.tagName).toBe("INPUT");
    expect(email.value).toBe("admin@example.com");
    expect(screen.getByTestId("perc-profile-account-save")).toBeTruthy();
  });

  it("shows directory email as read-only with localized hint", async () => {
    const loadProfile = vi.fn().mockResolvedValue(
      internalProfile({
        name: "ldap.user",
        providerType: "DIRECTORY",
        emailEditable: false,
        email: "ldap@corp.example",
      }),
    );
    render(<AccountSection loadProfile={loadProfile} />);

    await waitFor(() => {
      expect(screen.getByTestId("perc-profile-account-login").textContent).toBe(
        "ldap.user",
      );
    });
    expect(screen.getByTestId("perc-profile-account-provider").textContent).toBe(
      "Directory",
    );
    const email = screen.getByTestId("perc-profile-account-email");
    expect(email.tagName).not.toBe("INPUT");
    expect(email.textContent).toContain("ldap@corp.example");
    expect(
      screen.getByTestId("perc-profile-account-email-ro-hint"),
    ).toBeTruthy();
    expect(screen.queryByTestId("perc-profile-account-save")).toBeNull();
  });

  it("validates email client-side and saves on happy path", async () => {
    const loadProfile = vi.fn().mockResolvedValue(internalProfile());
    const saveEmail = vi
      .fn()
      .mockResolvedValue(internalProfile({ email: "new@example.com" }));
    render(
      <AccountSection loadProfile={loadProfile} saveEmail={saveEmail} />,
    );

    await waitFor(() => {
      expect(screen.getByTestId("perc-profile-account-email")).toBeTruthy();
    });

    const email = screen.getByTestId(
      "perc-profile-account-email",
    ) as HTMLInputElement;
    fireEvent.change(email, { target: { value: "not-valid" } });
    fireEvent.click(screen.getByTestId("perc-profile-account-save"));

    await waitFor(() => {
      expect(
        screen.getByTestId("perc-profile-account-email-error").textContent,
      ).toMatch(/valid email/i);
    });
    expect(saveEmail).not.toHaveBeenCalled();

    fireEvent.change(email, { target: { value: "new@example.com" } });
    fireEvent.click(screen.getByTestId("perc-profile-account-save"));

    await waitFor(() => {
      expect(saveEmail).toHaveBeenCalledWith("new@example.com");
      expect(
        screen.getByTestId("perc-profile-account-success").textContent,
      ).toMatch(/saved/i);
    });
  });

  it("surfaces load errors with retry", async () => {
    const loadProfile = vi
      .fn()
      .mockRejectedValueOnce({ status: 500, statusText: "Err", body: null })
      .mockResolvedValueOnce(internalProfile());
    render(<AccountSection loadProfile={loadProfile} />);

    await waitFor(() => {
      expect(
        screen.getByTestId("perc-profile-account-load-error"),
      ).toBeTruthy();
    });
    fireEvent.click(screen.getByTestId("perc-profile-account-retry"));
    await waitFor(() => {
      expect(screen.getByTestId("perc-profile-account-login").textContent).toBe(
        "Admin",
      );
    });
    expect(loadProfile).toHaveBeenCalledTimes(2);
  });

  it("renders default community from profile.communities only", async () => {
    const loadProfile = vi.fn().mockResolvedValue(
      internalProfile({
        communities: ["Default", "Corporate"],
        defaultCommunity: "Corporate",
      }),
    );
    render(<AccountSection loadProfile={loadProfile} />);

    await waitFor(() => {
      expect(
        screen.getByTestId("perc-profile-account-default-community-select"),
      ).toBeTruthy();
    });
    const select = screen.getByTestId(
      "perc-profile-account-default-community-select",
    ) as HTMLSelectElement;
    expect(select.tagName).toBe("SELECT");
    expect(select.value).toBe("Corporate");
    const options = Array.from(select.options).map((o) => o.value);
    expect(options).toEqual(["", "Default", "Corporate"]);
    expect(options).not.toContain("Unknown");
  });

  it("falls back to empty select when stored default is not in membership", async () => {
    expect(
      allowedDefaultCommunityDraft({
        communities: ["Default"],
        defaultCommunity: "Stale Community",
      }),
    ).toBe("");
    const loadProfile = vi.fn().mockResolvedValue(
      internalProfile({
        communities: ["Default", "Corporate"],
        defaultCommunity: "RemovedFromRole",
      }),
    );
    render(<AccountSection loadProfile={loadProfile} />);

    await waitFor(() => {
      expect(
        screen.getByTestId("perc-profile-account-default-community-select"),
      ).toBeTruthy();
    });
    const select = screen.getByTestId(
      "perc-profile-account-default-community-select",
    ) as HTMLSelectElement;
    expect(select.value).toBe("");
    expect(Array.from(select.options).map((o) => o.value)).toEqual([
      "",
      "Default",
      "Corporate",
    ]);
  });

  it("saves allowed default community and reloads returned value", async () => {
    const loadProfile = vi.fn().mockResolvedValue(
      internalProfile({
        communities: ["Default", "Corporate"],
        defaultCommunity: "Default",
      }),
    );
    const saveDefaultCommunity = vi.fn().mockResolvedValue(
      internalProfile({
        communities: ["Default", "Corporate"],
        defaultCommunity: "Corporate",
      }),
    );
    render(
      <AccountSection
        loadProfile={loadProfile}
        saveDefaultCommunity={saveDefaultCommunity}
      />,
    );

    await waitFor(() => {
      expect(
        screen.getByTestId("perc-profile-account-default-community-select"),
      ).toBeTruthy();
    });
    const select = screen.getByTestId(
      "perc-profile-account-default-community-select",
    ) as HTMLSelectElement;
    fireEvent.change(select, { target: { value: "Corporate" } });
    fireEvent.click(
      screen.getByTestId("perc-profile-account-default-community-save"),
    );

    await waitFor(() => {
      expect(saveDefaultCommunity).toHaveBeenCalledWith("Corporate");
      expect(
        screen.getByTestId("perc-profile-account-success").textContent,
      ).toMatch(/default community was saved/i);
    });
    expect(select.value).toBe("Corporate");
  });

  it("shows an unavailable note when the user has no communities", async () => {
    const loadProfile = vi.fn().mockResolvedValue(
      internalProfile({
        communities: [],
        defaultCommunity: "",
      }),
    );
    render(<AccountSection loadProfile={loadProfile} />);

    await waitFor(() => {
      expect(
        screen.getByTestId(
          "perc-profile-account-default-community-unavailable",
        ).textContent,
      ).toMatch(/no communities/i);
    });
    expect(
      screen.queryByTestId("perc-profile-account-default-community-save"),
    ).toBeNull();
  });

  it("clears the stored default when Use role default is saved", async () => {
    const loadProfile = vi.fn().mockResolvedValue(
      internalProfile({
        communities: ["Default", "Corporate"],
        defaultCommunity: "Default",
      }),
    );
    const saveDefaultCommunity = vi.fn().mockResolvedValue(
      internalProfile({
        communities: ["Default", "Corporate"],
        defaultCommunity: "",
      }),
    );
    render(
      <AccountSection
        loadProfile={loadProfile}
        saveDefaultCommunity={saveDefaultCommunity}
      />,
    );

    await waitFor(() => {
      expect(
        screen.getByTestId("perc-profile-account-default-community-select"),
      ).toBeTruthy();
    });
    const select = screen.getByTestId(
      "perc-profile-account-default-community-select",
    ) as HTMLSelectElement;
    fireEvent.change(select, { target: { value: "" } });
    fireEvent.click(
      screen.getByTestId("perc-profile-account-default-community-save"),
    );

    await waitFor(() => {
      expect(saveDefaultCommunity).toHaveBeenCalledWith("");
      expect(
        screen.getByTestId("perc-profile-account-success").textContent,
      ).toMatch(/default community was saved/i);
    });
  });
});
