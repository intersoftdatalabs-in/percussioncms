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
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import { render, screen } from "@testing-library/react";
import { describe, expect, it } from "vitest";
import { ExplorerStatusBar } from "../../../main/ts/contentExplorer/ExplorerStatusBar";
import { renderA11yGate } from "./a11y";

function renderBar(
  overrides: Partial<Parameters<typeof ExplorerStatusBar>[0]> = {},
) {
  return render(
    <ExplorerStatusBar
      multiSelectedCount={0}
      hasSingleSelection={false}
      folderPath="/Sites/Demo"
      folderItemCount={3}
      userName="Admin"
      isAdmin={false}
      lastOperation={null}
      {...overrides}
    />,
  );
}

describe("ExplorerStatusBar (#6517)", () => {
  it("renders the footer chrome with all four segments", () => {
    renderBar();
    expect(screen.getByTestId("explorer-status-bar")).toBeTruthy();
    expect(screen.getByTestId("explorer-status-bar-selection")).toBeTruthy();
    expect(screen.getByTestId("explorer-status-bar-folder")).toBeTruthy();
    expect(
      screen.getByTestId("explorer-status-bar-operation"),
    ).toBeTruthy();
    expect(screen.getByTestId("explorer-status-bar-context")).toBeTruthy();
  });

  it("names the selection count, folder count, and user", () => {
    renderBar({
      multiSelectedCount: 2,
      hasSingleSelection: false,
      folderPath: "/Sites/Demo",
      folderItemCount: 5,
      userName: "Admin",
      isAdmin: true,
    });
    expect(
      screen.getByTestId("explorer-status-bar-selection").textContent,
    ).toContain("2 items selected");
    expect(
      screen.getByTestId("explorer-status-bar-folder").textContent,
    ).toContain("5 items in /Sites/Demo");
    expect(
      screen.getByTestId("explorer-status-bar-context").textContent,
    ).toContain("Signed in as Admin (Admin)");
  });

  it("names the single selection and the empty folder", () => {
    renderBar({
      multiSelectedCount: 0,
      hasSingleSelection: true,
      selectedItemLabel: "Home Page",
      folderItemCount: 0,
    });
    expect(
      screen.getByTestId("explorer-status-bar-selection").textContent,
    ).toContain("Selected: Home Page");
    expect(
      screen.getByTestId("explorer-status-bar-folder").textContent,
    ).toContain("No items in /Sites/Demo");
  });

  it("covers no selection, no folder, and the loading count", () => {
    renderBar({
      hasSingleSelection: false,
      folderPath: null,
      folderItemCount: null,
      userName: "  ",
    });
    expect(
      screen.getByTestId("explorer-status-bar-selection").textContent,
    ).toContain("No selection");
    expect(
      screen.getByTestId("explorer-status-bar-folder").textContent,
    ).toContain("No folder open");
    expect(
      screen.getByTestId("explorer-status-bar-context").textContent,
    ).toContain("unknown user");

    renderBar({ folderPath: "/Sites", folderItemCount: null });
    const folders = screen.getAllByTestId("explorer-status-bar-folder");
    expect(folders[folders.length - 1].textContent).toContain(
      "Loading items for /Sites",
    );
  });

  it("announces the last operation through the polite live region", () => {
    renderBar({ lastOperation: { kind: "success", text: "New copy created" } });
    const operation = screen.getByTestId("explorer-status-bar-operation");
    expect(operation.getAttribute("role")).toBe("status");
    expect(operation.getAttribute("aria-live")).toBe("polite");
    expect(operation.getAttribute("data-kind")).toBe("success");
    expect(operation.textContent).toContain("New copy created");

    renderBar({ lastOperation: { kind: "error", text: "Not allowed (HTTP 403)" } });
    const failed = screen.getAllByTestId("explorer-status-bar-operation")[1];
    expect(failed.getAttribute("data-kind")).toBe("error");
    expect(failed.textContent).toContain("Not allowed (HTTP 403)");
  });

  it("passes the zero serious/critical axe-core gate", async () => {
    const { container } = renderBar({
      lastOperation: { kind: "error", text: "Not allowed (HTTP 403)" },
    });
    await renderA11yGate(container);
  });
});
