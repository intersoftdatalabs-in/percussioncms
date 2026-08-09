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
import { cleanup, fireEvent, render, screen, waitFor } from "@testing-library/react";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import { BootstrapProvider } from "../../../main/ts/app/bootstrap/BootstrapContext";
import { DeveloperPreferencesPanel } from "../../../main/ts/developer/DeveloperPreferencesPanel";
import { systemDefaultAclTemplate } from "../../../main/ts/developer/defaultAclTemplate";
import {
  loadDefaultAclTemplate,
  saveDefaultAclTemplate,
} from "../../../main/ts/api/developer/preferencesApi";

vi.mock("../../../main/ts/api/developer/preferencesApi", () => ({
  loadDefaultAclTemplate: vi.fn(),
  saveDefaultAclTemplate: vi.fn(),
}));

vi.mock("../../../main/ts/i18n/message", () => ({
  message: (key: string) => key,
}));

function renderPanel(userName = "admin") {
  return render(
    <BootstrapProvider
      value={{
        userName,
        locale: "en-us",
        entry: "developer",
        isAdmin: true,
        isDesigner: true,
        isWidgetBuilderActive: false,
        allowExternalAvatarFetch: true,
      }}
    >
      <DeveloperPreferencesPanel />
    </BootstrapProvider>,
  );
}

describe("DeveloperPreferencesPanel", () => {
  beforeEach(() => {
    vi.mocked(loadDefaultAclTemplate).mockResolvedValue({
      template: systemDefaultAclTemplate(),
      fromPreference: false,
    });
    vi.mocked(saveDefaultAclTemplate).mockResolvedValue({
      name: "developer.defaultObjectAclTemplate",
      value: "{}",
      userName: "admin",
    });
  });

  afterEach(() => {
    cleanup();
    vi.clearAllMocks();
  });

  it("loads system default template into the security table", async () => {
    renderPanel();
    await waitFor(() => {
      expect(screen.getByTestId("developer-prefs-acl-table")).toBeTruthy();
    });
    expect(screen.getByTestId("developer-prefs-acl-source").textContent).toMatch(
      /system default/i,
    );
    expect(screen.getByDisplayValue("Default")).toBeTruthy();
    expect(screen.getByDisplayValue("AnyCommunity")).toBeTruthy();
    expect(loadDefaultAclTemplate).toHaveBeenCalled();
  });

  it("groups permission columns under Design access and Runtime visibility", async () => {
    renderPanel();
    await waitFor(() => {
      expect(screen.getByTestId("developer-prefs-acl-table")).toBeTruthy();
    });

    const table = screen.getByTestId("developer-prefs-acl-table");
    expect(table.getAttribute("data-acl-show-runtime")).toBe("true");
    expect(table.getAttribute("data-acl-layered")).toBe("true");

    const designLayer = screen.getByTestId("developer-prefs-acl-layer-design");
    expect(designLayer.getAttribute("data-acl-layer")).toBe("design");
    expect(designLayer.textContent).toMatch(/Design access/i);

    const runtimeLayer = screen.getByTestId("developer-prefs-acl-layer-runtime");
    expect(runtimeLayer.getAttribute("data-acl-layer")).toBe("runtime");
    expect(runtimeLayer.textContent).toMatch(/Runtime visibility/i);

    expect(
      screen.getByTestId("developer-prefs-acl-perm-header-READ").textContent,
    ).toMatch(/Read/i);
    expect(
      screen.getByTestId("developer-prefs-acl-perm-header-UPDATE").textContent,
    ).toMatch(/Update/i);
    expect(
      screen.getByTestId("developer-prefs-acl-perm-header-DELETE").textContent,
    ).toMatch(/Delete/i);
    expect(
      screen.getByTestId("developer-prefs-acl-perm-header-OWNER").textContent,
    ).toMatch(/Modify ACL/i);
    expect(
      screen.getByTestId("developer-prefs-acl-perm-header-RUNTIME_VISIBLE")
        .textContent,
    ).toMatch(/Visible/i);

    // Layered checkbox still present for Default row
    expect(
      screen.getByTestId(
        "developer-prefs-acl-perm-row:0:Default:USER-RUNTIME_VISIBLE",
      ),
    ).toBeTruthy();
  });

  it("saves dirty template via preferences API", async () => {
    renderPanel();
    await waitFor(() => {
      expect(screen.getByTestId("developer-prefs-acl-table")).toBeTruthy();
    });

    // Toggle RUNTIME_VISIBLE on Default row (clientKey row:0:Default:USER)
    const runtimeOnDefault = screen.getByTestId(
      "developer-prefs-acl-perm-row:0:Default:USER-RUNTIME_VISIBLE",
    );
    fireEvent.click(runtimeOnDefault);

    const saveBtn = screen.getByTestId("developer-prefs-acl-save");
    await waitFor(() => {
      expect((saveBtn as HTMLButtonElement).disabled).toBe(false);
    });
    fireEvent.click(saveBtn);

    await waitFor(() => {
      expect(saveDefaultAclTemplate).toHaveBeenCalled();
    });
    const [template, user] = vi.mocked(saveDefaultAclTemplate).mock.calls[0];
    expect(user).toBe("admin");
    expect(template.entries[0].permissions).toContain("RUNTIME_VISIBLE");
    await waitFor(() => {
      expect(screen.getByTestId("developer-prefs-acl-notice").textContent).toMatch(
        /saved/i,
      );
    });
  });

  it("adds a template entry from the add form", async () => {
    renderPanel();
    await waitFor(() => {
      expect(screen.getByTestId("developer-prefs-acl-add-form")).toBeTruthy();
    });
    fireEvent.change(screen.getByTestId("developer-prefs-acl-add-name"), {
      target: { value: "Editors" },
    });
    fireEvent.change(screen.getByTestId("developer-prefs-acl-add-type"), {
      target: { value: "ROLE" },
    });
    fireEvent.click(screen.getByTestId("developer-prefs-acl-add"));
    expect(screen.getByDisplayValue("Editors")).toBeTruthy();
  });

  it("reset restores system default in the editor", async () => {
    renderPanel();
    await waitFor(() => {
      expect(screen.getByTestId("developer-prefs-acl-table")).toBeTruthy();
    });
    fireEvent.change(screen.getByTestId("developer-prefs-acl-add-name"), {
      target: { value: "TempRole" },
    });
    fireEvent.click(screen.getByTestId("developer-prefs-acl-add"));
    expect(screen.getByDisplayValue("TempRole")).toBeTruthy();
    fireEvent.click(screen.getByTestId("developer-prefs-acl-reset"));
    expect(screen.queryByDisplayValue("TempRole")).toBeNull();
    expect(screen.getByDisplayValue("Default")).toBeTruthy();
  });

  it("disables save when userName is missing", async () => {
    renderPanel("");
    await waitFor(() => {
      expect(screen.getByTestId("developer-prefs-acl-table")).toBeTruthy();
    });
    const saveBtn = screen.getByTestId("developer-prefs-acl-save") as HTMLButtonElement;
    expect(saveBtn.disabled).toBe(true);
  });
});
