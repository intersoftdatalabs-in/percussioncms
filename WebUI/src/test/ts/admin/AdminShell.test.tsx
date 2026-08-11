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

import React from "react";
import { render, screen, fireEvent } from "@testing-library/react";
import { MemoryRouter } from "react-router";
import { describe, expect, it, vi } from "vitest";
import { AdminShell } from "../../../main/ts/admin/AdminShell";

// Mock child components
vi.mock("../../../main/ts/admin/TasksSection", () => ({
  TasksSection: () => <div data-testid="mock-tasks-section">Tasks Content</div>,
}));

vi.mock("../../../main/ts/admin/TaskLogsSection", () => ({
  TaskLogsSection: () => <div data-testid="mock-logs-section">Logs Content</div>,
}));

vi.mock("../../../main/ts/admin/TaskNotifications", () => ({
  TaskNotifications: () => <div data-testid="mock-notifications-section">Notifications Content</div>,
}));

vi.mock("../../../main/ts/admin/tools/ToolsSection", () => ({
  ToolsSection: () => <div data-testid="mock-tools-section">Tools Content</div>,
}));

function renderShell(ui: React.ReactElement) {
  return render(
    <MemoryRouter basename="/cm/app" initialEntries={["/cm/app/admin"]}>
      {ui}
    </MemoryRouter>,
  );
}

describe("AdminShell", () => {
  it("renders Admin tools title and Administration sibling (#2953)", () => {
    renderShell(<AdminShell />);
    expect(screen.getByTestId("perc-admin-shell")).toBeDefined();
    expect(screen.getByTestId("mock-tasks-section")).toBeDefined();
    // Shell title must read Admin tools (not Administration) — #2784 landing.
    expect(screen.getByTestId("perc-admin-shell-title").textContent).toMatch(
      /Admin tools/i,
    );
    const sibling = screen.getByTestId("admin-sibling-workflow-link");
    expect(sibling).toBeDefined();
    expect(sibling.textContent).toMatch(/Administration/i);
    const href = sibling.getAttribute("href") || "";
    expect(href === "/workflow" || href.endsWith("/workflow")).toBe(true);
  });

  it("exposes responsive tablist chrome for narrow / portrait layouts", () => {
    renderShell(<AdminShell />);
    const shell = screen.getByTestId("perc-admin-shell");
    const tablist = screen.getByTestId("perc-admin-tablist");
    expect(tablist.getAttribute("role")).toBe("tablist");
    // CSS modules attach hashed classes; ensure nav is classed (not only inline styles).
    expect(tablist.className.length).toBeGreaterThan(0);
    expect(shell.className.length).toBeGreaterThan(0);
    expect(screen.getByTestId("tab-tasks")).toBeDefined();
    expect(screen.getByTestId("tab-logs")).toBeDefined();
    expect(screen.getByTestId("tab-notifications")).toBeDefined();
    expect(screen.getByTestId("tab-tools")).toBeDefined();
  });

  it("switches tabs when nav buttons are clicked", () => {
    renderShell(<AdminShell />);

    const logsTab = screen.getByTestId("tab-logs");
    fireEvent.click(logsTab);
    expect(screen.getByTestId("mock-logs-section")).toBeDefined();
    expect(screen.queryByTestId("mock-tasks-section")).toBeNull();

    const notifTab = screen.getByTestId("tab-notifications");
    fireEvent.click(notifTab);
    expect(screen.getByTestId("mock-notifications-section")).toBeDefined();

    const toolsTab = screen.getByTestId("tab-tools");
    fireEvent.click(toolsTab);
    expect(screen.getByTestId("mock-tools-section")).toBeDefined();
  });
});
