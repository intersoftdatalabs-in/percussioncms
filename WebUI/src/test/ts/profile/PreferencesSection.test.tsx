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
import { PreferencesSection } from "../../../main/ts/profile/PreferencesSection";
import { BootstrapProvider } from "../../../main/ts/app/bootstrap/BootstrapContext";
import type { SpaBootstrap } from "../../../main/ts/app/bootstrap/types";

const bootstrap: SpaBootstrap = {
  userName: "Admin",
  locale: "en-us",
  entry: "profile",
  isAdmin: true,
  isDesigner: true,
  isWidgetBuilderActive: false,
};

function renderPrefs(
  props: React.ComponentProps<typeof PreferencesSection> = {},
): ReturnType<typeof render> {
  return render(
    <BootstrapProvider value={bootstrap}>
      <PreferencesSection {...props} />
    </BootstrapProvider>,
  );
}

describe("PreferencesSection", () => {
  beforeEach(() => {
    (window as unknown as { I18N: { message: (k: string) => string } }).I18N = {
      message: (key: string) => {
        const at = key.indexOf("@");
        return at >= 0 ? key.slice(at + 1) : key;
      },
    };
  });

  it("loads landing select and preference stack count", async () => {
    const loadLanding = vi.fn().mockResolvedValue("Home");
    const loadPreferenceCount = vi.fn().mockResolvedValue(2);
    renderPrefs({ loadLanding, loadPreferenceCount });

    await waitFor(() => {
      expect(screen.getByTestId("perc-profile-preferences-landing")).toBeTruthy();
    });
    const select = screen.getByTestId(
      "perc-profile-preferences-landing",
    ) as HTMLSelectElement;
    expect(select.value).toBe("Home");
    expect(screen.getByTestId("perc-profile-preferences-count").textContent).toContain(
      "2",
    );
    expect(loadLanding).toHaveBeenCalled();
    expect(loadPreferenceCount).toHaveBeenCalled();
  });

  it("saves dirty landing and shows success", async () => {
    const loadLanding = vi.fn().mockResolvedValue("");
    const saveLanding = vi.fn().mockResolvedValue("Editor");
    const loadPreferenceCount = vi.fn().mockResolvedValue(0);
    renderPrefs({ loadLanding, saveLanding, loadPreferenceCount });

    await waitFor(() => {
      expect(screen.getByTestId("perc-profile-preferences-landing")).toBeTruthy();
    });

    const select = screen.getByTestId(
      "perc-profile-preferences-landing",
    ) as HTMLSelectElement;
    fireEvent.change(select, { target: { value: "Editor" } });

    const save = screen.getByTestId("perc-profile-preferences-save");
    expect((save as HTMLButtonElement).disabled).toBe(false);
    fireEvent.click(save);

    await waitFor(() => {
      expect(saveLanding).toHaveBeenCalledWith("Editor");
      expect(screen.getByTestId("perc-profile-preferences-success")).toBeTruthy();
    });
    expect(select.value).toBe("Editor");
  });

  it("shows load error and retries", async () => {
    const loadLanding = vi
      .fn()
      .mockRejectedValueOnce({ status: 500, statusText: "err", body: null })
      .mockResolvedValueOnce("Home");
    const loadPreferenceCount = vi.fn().mockResolvedValue(0);
    renderPrefs({ loadLanding, loadPreferenceCount });

    await waitFor(() => {
      expect(
        screen.getByTestId("perc-profile-preferences-load-error"),
      ).toBeTruthy();
    });
    fireEvent.click(screen.getByTestId("perc-profile-preferences-retry"));
    await waitFor(() => {
      expect(screen.getByTestId("perc-profile-preferences-landing")).toBeTruthy();
    });
    expect(loadLanding).toHaveBeenCalledTimes(2);
  });

  it("disables save when not dirty", async () => {
    const loadLanding = vi.fn().mockResolvedValue("Home");
    const loadPreferenceCount = vi.fn().mockResolvedValue(1);
    renderPrefs({ loadLanding, loadPreferenceCount });

    await waitFor(() => {
      expect(screen.getByTestId("perc-profile-preferences-landing")).toBeTruthy();
    });
    const save = screen.getByTestId(
      "perc-profile-preferences-save",
    ) as HTMLButtonElement;
    expect(save.disabled).toBe(true);
  });
});
