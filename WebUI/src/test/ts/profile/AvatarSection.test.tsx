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
import { AvatarSection } from "../../../main/ts/profile/AvatarSection";
import { BootstrapProvider } from "../../../main/ts/app/bootstrap/BootstrapContext";
import type { SpaBootstrap } from "../../../main/ts/app/bootstrap/types";

const bootstrap: SpaBootstrap = {
  userName: "Admin",
  locale: "en-us",
  entry: "profile",
  isAdmin: true,
  isDesigner: true,
  isWidgetBuilderActive: false,
  allowExternalAvatarFetch: true,
};

function renderAvatar(
  props: React.ComponentProps<typeof AvatarSection> = {},
): ReturnType<typeof render> {
  return render(
    <BootstrapProvider value={bootstrap}>
      <AvatarSection {...props} />
    </BootstrapProvider>,
  );
}

describe("AvatarSection", () => {
  beforeEach(() => {
    (window as unknown as { I18N: { message: (k: string) => string } }).I18N = {
      message: (key: string) => {
        const at = key.indexOf("@");
        return at >= 0 ? key.slice(at + 1) : key;
      },
    };
  });

  it("loads primary email and shows preview + privacy note", async () => {
    const loadPrimaryEmail = vi.fn().mockResolvedValue("admin@example.com");
    const loadOverride = vi.fn().mockResolvedValue("");
    renderAvatar({ loadPrimaryEmail, loadOverride });

    await waitFor(() => {
      expect(screen.getByTestId("perc-profile-avatar-email")).toBeTruthy();
    });
    const input = screen.getByTestId(
      "perc-profile-avatar-email",
    ) as HTMLInputElement;
    expect(input.value).toBe("admin@example.com");
    expect(input.disabled).toBe(true);
    expect(
      (screen.getByTestId("perc-profile-avatar-use-primary") as HTMLInputElement)
        .checked,
    ).toBe(true);
    expect(screen.getByTestId("perc-profile-avatar-preview")).toBeTruthy();
    expect(screen.getByTestId("perc-profile-avatar-privacy").textContent).toContain(
      "Privacy",
    );
    expect(loadPrimaryEmail).toHaveBeenCalled();
    expect(loadOverride).toHaveBeenCalled();
  });

  it("saves override email and shows success", async () => {
    const loadPrimaryEmail = vi.fn().mockResolvedValue("admin@example.com");
    const loadOverride = vi.fn().mockResolvedValue("");
    const saveOverride = vi.fn().mockResolvedValue("avatar@example.com");
    renderAvatar({ loadPrimaryEmail, loadOverride, saveOverride });

    await waitFor(() => {
      expect(screen.getByTestId("perc-profile-avatar-email")).toBeTruthy();
    });

    fireEvent.click(screen.getByTestId("perc-profile-avatar-use-primary"));
    const input = screen.getByTestId(
      "perc-profile-avatar-email",
    ) as HTMLInputElement;
    expect(input.disabled).toBe(false);
    fireEvent.change(input, { target: { value: "avatar@example.com" } });

    const save = screen.getByTestId("perc-profile-avatar-save");
    expect((save as HTMLButtonElement).disabled).toBe(false);
    fireEvent.click(save);

    await waitFor(() => {
      expect(saveOverride).toHaveBeenCalledWith("Admin", "avatar@example.com");
      expect(screen.getByTestId("perc-profile-avatar-success")).toBeTruthy();
    });
  });

  it("validates invalid override email", async () => {
    const loadPrimaryEmail = vi.fn().mockResolvedValue("admin@example.com");
    const loadOverride = vi.fn().mockResolvedValue("");
    const saveOverride = vi.fn();
    renderAvatar({ loadPrimaryEmail, loadOverride, saveOverride });

    await waitFor(() => {
      expect(screen.getByTestId("perc-profile-avatar-email")).toBeTruthy();
    });
    fireEvent.click(screen.getByTestId("perc-profile-avatar-use-primary"));
    fireEvent.change(screen.getByTestId("perc-profile-avatar-email"), {
      target: { value: "not-an-email" },
    });
    fireEvent.click(screen.getByTestId("perc-profile-avatar-save"));

    await waitFor(() => {
      expect(screen.getByTestId("perc-profile-avatar-error").textContent).toContain(
        "valid email",
      );
    });
    expect(saveOverride).not.toHaveBeenCalled();
  });

  it("shows external-disabled privacy copy when kill-switch is on", async () => {
    const loadPrimaryEmail = vi.fn().mockResolvedValue("admin@example.com");
    const loadOverride = vi.fn().mockResolvedValue("");
    renderAvatar({
      loadPrimaryEmail,
      loadOverride,
      allowExternalAvatarFetch: false,
    });

    await waitFor(() => {
      expect(screen.getByTestId("perc-profile-avatar-privacy").textContent).toContain(
        "External avatar images are disabled",
      );
    });
  });

  it("shows load error and retries", async () => {
    const loadPrimaryEmail = vi
      .fn()
      .mockRejectedValueOnce({ status: 500, statusText: "err", body: null })
      .mockResolvedValueOnce("admin@example.com");
    const loadOverride = vi.fn().mockResolvedValue("");
    renderAvatar({ loadPrimaryEmail, loadOverride });

    await waitFor(() => {
      expect(screen.getByTestId("perc-profile-avatar-load-error")).toBeTruthy();
    });
    fireEvent.click(screen.getByTestId("perc-profile-avatar-retry"));
    await waitFor(() => {
      expect(screen.getByTestId("perc-profile-avatar-email")).toBeTruthy();
    });
    expect(loadPrimaryEmail).toHaveBeenCalledTimes(2);
  });
});
