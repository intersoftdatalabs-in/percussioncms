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

import { fireEvent, render, screen } from "@testing-library/react";
import type { ComponentProps } from "react";
import { describe, expect, it, vi } from "vitest";
import { ExplorerMenuBar } from "../../../main/ts/contentExplorer/ExplorerMenuBar";
import { renderA11yGate } from "./a11y";

function renderBar(
  overrides: Partial<ComponentProps<typeof ExplorerMenuBar>> = {},
) {
  const onCommand = vi.fn();
  const onSelectFormat = vi.fn();
  const result = render(
    <ExplorerMenuBar
      showSearch={false}
      showSecurity={false}
      showTranslations={false}
      showRelationships={false}
      showDependencies={false}
      showClipboard={false}
      multiSelectedCount={0}
      clipboardItemCount={0}
      displayFormats={[]}
      selectedFormatKey=""
      onSelectFormat={onSelectFormat}
      onCommand={onCommand}
      {...overrides}
    />,
  );
  return { ...result, onCommand, onSelectFormat };
}

describe("ExplorerMenuBar (#2731)", () => {
  it("renders menubar with Content / View / Help top-level menus", () => {
    renderBar();
    expect(screen.getByTestId("explorer-menu-bar")).toBeTruthy();
    const menubar = screen.getByTestId("explorer-menu-bar-menubar");
    expect(menubar.getAttribute("role")).toBe("menubar");
    expect(screen.getByTestId("explorer-menu-content")).toBeTruthy();
    expect(screen.getByTestId("explorer-menu-view")).toBeTruthy();
    expect(screen.getByTestId("explorer-menu-help")).toBeTruthy();
    // Display format is adjacent shell chrome (always present).
    expect(screen.getByTestId("explorer-display-format")).toBeTruthy();
  });

  it("opens View dropdown with nested toggles (not flat multi-row chrome)", () => {
    const { onCommand } = renderBar();
    expect(screen.queryByTestId("explorer-toggle-search")).toBeNull();
    fireEvent.click(screen.getByTestId("explorer-menu-view"));
    expect(screen.getByTestId("explorer-menu-view-dropdown")).toBeTruthy();
    expect(screen.getByTestId("explorer-toggle-search")).toBeTruthy();
    expect(screen.getByTestId("explorer-toggle-security")).toBeTruthy();
    expect(screen.getByTestId("explorer-toggle-relationships")).toBeTruthy();
    fireEvent.click(screen.getByTestId("explorer-toggle-search"));
    expect(onCommand).toHaveBeenCalledWith("view-search");
  });

  it("View → IA Relationships toggle invokes view-relationships (#2769)", () => {
    const { onCommand } = renderBar();
    fireEvent.click(screen.getByTestId("explorer-menu-view"));
    fireEvent.click(screen.getByTestId("explorer-toggle-relationships"));
    expect(onCommand).toHaveBeenCalledWith("view-relationships");
  });

  it("Content → Add to clipboard is disabled without multi-select", () => {
    renderBar({ multiSelectedCount: 0 });
    fireEvent.click(screen.getByTestId("explorer-menu-content"));
    const add = screen.getByTestId(
      "explorer-clipboard-add",
    ) as HTMLButtonElement;
    expect(add.disabled).toBe(true);
  });

  it("Content → Add to clipboard invokes when selection present", () => {
    const { onCommand } = renderBar({ multiSelectedCount: 2 });
    fireEvent.click(screen.getByTestId("explorer-menu-content"));
    fireEvent.click(screen.getByTestId("explorer-clipboard-add"));
    expect(onCommand).toHaveBeenCalledWith("content-clipboard-add");
  });

  it("Content → Site Copy is disabled without site context (#2767)", () => {
    renderBar({ hasSiteContext: false });
    fireEvent.click(screen.getByTestId("explorer-menu-content"));
    const item = screen.getByTestId(
      "explorer-content-site-copy",
    ) as HTMLButtonElement;
    expect(item.disabled).toBe(true);
  });

  it("Content → Site Copy invokes when site context present (#2767)", () => {
    const { onCommand } = renderBar({
      hasSiteContext: true,
      showSiteCopy: false,
    });
    fireEvent.click(screen.getByTestId("explorer-menu-content"));
    fireEvent.click(screen.getByTestId("explorer-content-site-copy"));
    expect(onCommand).toHaveBeenCalledWith("content-site-copy");
  });

  it("shows multi-select status badge outside dropdowns", () => {
    renderBar({ multiSelectedCount: 2 });
    expect(screen.getByTestId("explorer-multi-select-count")).toBeTruthy();
  });

  it("Escape closes an open dropdown", () => {
    renderBar();
    fireEvent.click(screen.getByTestId("explorer-menu-help"));
    expect(screen.getByTestId("explorer-menu-help-dropdown")).toBeTruthy();
    fireEvent.keyDown(document, { key: "Escape" });
    expect(screen.queryByTestId("explorer-menu-help-dropdown")).toBeNull();
  });

  it("passes the zero serious/critical axe-core gate", async () => {
    const { container } = renderBar({ multiSelectedCount: 1 });
    fireEvent.click(screen.getByTestId("explorer-menu-view"));
    await renderA11yGate(container);
  });
});
