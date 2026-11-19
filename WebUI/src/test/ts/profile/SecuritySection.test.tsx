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
import { SecuritySection } from "../../../main/ts/profile/SecuritySection";
import type { CurrentUserProfile } from "../../../main/ts/api/user/userProfileApi";
import {
  MIN_PASSWORD_LENGTH,
  validatePasswordChange,
} from "../../../main/ts/api/user/userPasswordApi";

function internalProfile(
  overrides: Partial<CurrentUserProfile> = {},
): CurrentUserProfile {
  return {
    name: "Admin",
    email: "admin@example.com",
    providerType: "INTERNAL",
    roles: ["Admin"],
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

const labels = {
  required: "Enter a password.",
  tooShort: `Password must be at least ${MIN_PASSWORD_LENGTH} characters.`,
  mismatch: "Passwords do not match.",
};

describe("validatePasswordChange", () => {
  it("requires both fields", () => {
    const r = validatePasswordChange("", "", labels);
    expect(r.ok).toBe(false);
    if (!r.ok) {
      expect(r.fields.newPassword).toBe(labels.required);
      expect(r.fields.confirmPassword).toBe(labels.required);
    }
  });

  it("enforces min length", () => {
    const r = validatePasswordChange("abc", "abc", labels);
    expect(r.ok).toBe(false);
    if (!r.ok) {
      expect(r.fields.newPassword).toBe(labels.tooShort);
    }
  });

  it("requires confirm match", () => {
    const r = validatePasswordChange("abcdefghijkl", "abcdefghijkm", labels);
    expect(r.ok).toBe(false);
    if (!r.ok) {
      expect(r.fields.confirmPassword).toBe(labels.mismatch);
    }
  });

  it("accepts valid matching password", () => {
    const r = validatePasswordChange("abcdefghijkl", "abcdefghijkl", labels);
    expect(r.ok).toBe(true);
  });
});

describe("SecuritySection", () => {
  beforeEach(() => {
    (window as unknown as { I18N: { message: (k: string) => string } }).I18N = {
      message: (key: string) => {
        const at = key.indexOf("@");
        return at >= 0 ? key.slice(at + 1) : key;
      },
    };
  });

  it("renders change-password form for INTERNAL users", async () => {
    const loadProfile = vi.fn().mockResolvedValue(internalProfile());
    render(<SecuritySection loadProfile={loadProfile} />);

    await waitFor(() => {
      expect(screen.getByTestId("perc-profile-security-form")).toBeTruthy();
    });
    expect(
      screen.getByTestId("perc-profile-security-new-password"),
    ).toHaveAttribute("type", "password");
    expect(
      screen.getByTestId("perc-profile-security-confirm-password"),
    ).toHaveAttribute("type", "password");
    expect(screen.getByTestId("perc-profile-security-submit")).toBeTruthy();
    expect(screen.queryByTestId("perc-profile-security-external")).toBeNull();
  });

  it("shows external explanation for DIRECTORY users (no form)", async () => {
    const loadProfile = vi.fn().mockResolvedValue(
      internalProfile({
        name: "ldap.user",
        providerType: "DIRECTORY",
        emailEditable: false,
      }),
    );
    render(<SecuritySection loadProfile={loadProfile} />);

    await waitFor(() => {
      expect(screen.getByTestId("perc-profile-security-external")).toBeTruthy();
    });
    expect(
      screen.getByTestId("perc-profile-security-external-body").textContent,
    ).toMatch(/directory|single sign-on/i);
    expect(screen.queryByTestId("perc-profile-security-form")).toBeNull();
    expect(
      screen.queryByTestId("perc-profile-security-submit"),
    ).toBeNull();
  });

  it("client validation: too short and mismatch set aria-invalid", async () => {
    const loadProfile = vi.fn().mockResolvedValue(internalProfile());
    const changePassword = vi.fn();
    render(
      <SecuritySection
        loadProfile={loadProfile}
        changePassword={changePassword}
      />,
    );

    await waitFor(() => {
      expect(screen.getByTestId("perc-profile-security-form")).toBeTruthy();
    });

    fireEvent.change(screen.getByTestId("perc-profile-security-new-password"), {
      target: { value: "ab" },
    });
    fireEvent.change(
      screen.getByTestId("perc-profile-security-confirm-password"),
      { target: { value: "ab" } },
    );
    fireEvent.click(screen.getByTestId("perc-profile-security-submit"));

    await waitFor(() => {
      expect(
        screen.getByTestId("perc-profile-security-new-error"),
      ).toBeTruthy();
    });
    expect(
      screen.getByTestId("perc-profile-security-new-password"),
    ).toHaveAttribute("aria-invalid", "true");
    expect(changePassword).not.toHaveBeenCalled();

    fireEvent.change(screen.getByTestId("perc-profile-security-new-password"), {
      target: { value: "abcdefghijkl" },
    });
    fireEvent.change(
      screen.getByTestId("perc-profile-security-confirm-password"),
      { target: { value: "abcdefghijkm" } },
    );
    fireEvent.click(screen.getByTestId("perc-profile-security-submit"));

    await waitFor(() => {
      expect(
        screen.getByTestId("perc-profile-security-confirm-error"),
      ).toBeTruthy();
    });
    expect(
      screen.getByTestId("perc-profile-security-confirm-password"),
    ).toHaveAttribute("aria-invalid", "true");
    expect(changePassword).not.toHaveBeenCalled();

    fireEvent.change(screen.getByTestId("perc-profile-security-new-password"), {
      target: { value: "a".repeat(151) },
    });
    fireEvent.change(
      screen.getByTestId("perc-profile-security-confirm-password"),
      { target: { value: "a".repeat(151) } },
    );
    fireEvent.click(screen.getByTestId("perc-profile-security-submit"));

    await waitFor(() => {
      expect(
        screen.getByTestId("perc-profile-security-new-error").textContent,
      ).toMatch(/150/);
    });
    expect(changePassword).not.toHaveBeenCalled();
  });

  it("submits change password and announces success in live region", async () => {
    const loadProfile = vi.fn().mockResolvedValue(internalProfile());
    const changePassword = vi.fn().mockResolvedValue(undefined);
    render(
      <SecuritySection
        loadProfile={loadProfile}
        changePassword={changePassword}
      />,
    );

    await waitFor(() => {
      expect(screen.getByTestId("perc-profile-security-form")).toBeTruthy();
    });

    fireEvent.change(screen.getByTestId("perc-profile-security-new-password"), {
      target: { value: "NewValidPass12!" },
    });
    fireEvent.change(
      screen.getByTestId("perc-profile-security-confirm-password"),
      { target: { value: "NewValidPass12!" } },
    );
    fireEvent.click(screen.getByTestId("perc-profile-security-submit"));

    await waitFor(() => {
      expect(changePassword).toHaveBeenCalledWith({
        name: "Admin",
        password: "NewValidPass12!",
        email: "admin@example.com",
        roles: ["Admin"],
      });
    });
    await waitFor(() => {
      expect(
        screen.getByTestId("perc-profile-security-success").textContent,
      ).toMatch(/password was changed/i);
    });
    const status = screen.getByTestId("perc-profile-security-status");
    expect(status.getAttribute("role")).toBe("status");
    expect(status.getAttribute("aria-live")).toBe("polite");
    expect(
      (
        screen.getByTestId(
          "perc-profile-security-new-password",
        ) as HTMLInputElement
      ).value,
    ).toBe("");
  });

  it("maps API failure to form error live region", async () => {
    const loadProfile = vi.fn().mockResolvedValue(internalProfile());
    const changePassword = vi.fn().mockRejectedValue({
      status: 400,
      statusText: "Bad Request",
      body: { message: "Server rejected password" },
    });
    render(
      <SecuritySection
        loadProfile={loadProfile}
        changePassword={changePassword}
      />,
    );

    await waitFor(() => {
      expect(screen.getByTestId("perc-profile-security-form")).toBeTruthy();
    });

    fireEvent.change(screen.getByTestId("perc-profile-security-new-password"), {
      target: { value: "NewValidPass12!" },
    });
    fireEvent.change(
      screen.getByTestId("perc-profile-security-confirm-password"),
      { target: { value: "NewValidPass12!" } },
    );
    fireEvent.click(screen.getByTestId("perc-profile-security-submit"));

    await waitFor(() => {
      expect(
        screen.getByTestId("perc-profile-security-form-error").textContent,
      ).toContain("Server rejected password");
    });
    expect(screen.queryByTestId("perc-profile-security-success")).toBeNull();
  });

  it("shows load error with retry", async () => {
    const loadProfile = vi
      .fn()
      .mockRejectedValueOnce({ status: 500, statusText: "Error", body: null })
      .mockResolvedValueOnce(internalProfile());
    render(<SecuritySection loadProfile={loadProfile} />);

    await waitFor(() => {
      expect(
        screen.getByTestId("perc-profile-security-load-error"),
      ).toBeTruthy();
    });
    fireEvent.click(screen.getByTestId("perc-profile-security-retry"));
    await waitFor(() => {
      expect(screen.getByTestId("perc-profile-security-form")).toBeTruthy();
    });
    expect(loadProfile).toHaveBeenCalledTimes(2);
  });

  it("mounts MfaSecurityPanel for both INTERNAL and DIRECTORY users", async () => {
    const mockMfa = vi.fn().mockResolvedValue({
      userName: "Admin",
      mfaMode: "optional",
      mfaRequired: false,
      totpEnabled: false,
      passkeyCount: 0,
      recoveryCodesRemaining: 0,
      canDisableTotp: true,
      stepUpActive: false,
      stepUpExpiresAt: null,
    });

    const { unmount } = render(
      <SecuritySection
        loadProfile={vi.fn().mockResolvedValue(internalProfile())}
        mfaPanelProps={{ loadMfaStatus: mockMfa }}
      />,
    );
    await waitFor(() => {
      expect(screen.getByTestId("perc-profile-security-form")).toBeTruthy();
      expect(screen.getByTestId("perc-profile-mfa-status")).toBeTruthy();
    });

    unmount();
    render(
      <SecuritySection
        loadProfile={vi.fn().mockResolvedValue(
          internalProfile({ providerType: "DIRECTORY", emailEditable: false }),
        )}
        mfaPanelProps={{ loadMfaStatus: mockMfa }}
      />,
    );
    await waitFor(() => {
      expect(screen.getByTestId("perc-profile-security-external")).toBeTruthy();
      expect(screen.getByTestId("perc-profile-mfa-status")).toBeTruthy();
    });
  });
});
