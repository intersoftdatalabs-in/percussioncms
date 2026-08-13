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
import {
  AdminSectionErrorBoundary,
  AdminShell,
} from "../../../main/ts/admin/AdminShell";

// Mock child components
vi.mock("../../../main/ts/admin/TasksSection", () => ({
  TasksSection: () => <div data-testid="mock-tasks-section">Tasks Content</div>,
}));

vi.mock("../../../main/ts/admin/TaskLogsSection", () => ({
  TaskLogsSection: () => <div data-testid="mock-logs-section">Logs Content</div>,
}));

vi.mock("../../../main/ts/admin/TaskNotifications", () => ({
  TaskNotifications: () => (
    <div data-testid="mock-notifications-section">Notifications Content</div>
  ),
}));

vi.mock("../../../main/ts/admin/tools/ToolsSection", () => ({
  ToolsSection: () => <div data-testid="mock-tools-section">Tools Content</div>,
}));

vi.mock("../../../main/ts/workflowAdmin/workflow/WorkflowSection", () => ({
  WorkflowSection: () => (
    <div data-testid="mock-workflow-section">Workflow Content</div>
  ),
}));

vi.mock("../../../main/ts/workflowAdmin/role/RolesSection", () => ({
  RolesSection: () => <div data-testid="perc-roles-section">Roles Content</div>,
}));

vi.mock("../../../main/ts/workflowAdmin/user/UsersSection", () => ({
  UsersSection: () => <div data-testid="perc-users-section">Users Content</div>,
}));

vi.mock("../../../main/ts/workflowAdmin/category/CategoriesSection", () => ({
  CategoriesSection: () => (
    <div data-testid="perc-categories-section">Categories Content</div>
  ),
}));

function renderShell(ui: React.ReactElement) {
  return render(
    <MemoryRouter basename="/cm/app" initialEntries={["/cm/app/admin"]}>
      {ui}
    </MemoryRouter>,
  );
}

describe("AdminShell", () => {
  it("renders unified Admin title without sibling Workflow link (#3088 / #3340)", () => {
    renderShell(<AdminShell />);
    expect(screen.getByTestId("perc-admin-shell")).toBeDefined();
    expect(screen.getByTestId("mock-tasks-section")).toBeDefined();
    // Shell title must read Admin tools (not Administration) — #2784 landing.
    expect(screen.getByTestId("perc-admin-shell-title").textContent).toMatch(
      /Admin tools/i,
    );
    // Sibling chrome is not product (#3088 / #3340). Do not restore
    // Administration / Admin tools cross-links.
    expect(screen.queryByTestId("admin-sibling-workflow-link")).toBeNull();
    expect(screen.queryByTestId("admin-sibling-tools-link")).toBeNull();
    expect(screen.queryByRole("link", { name: "Administration" })).toBeNull();
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
    expect(screen.getByTestId("tab-workflow")).toBeDefined();
    expect(screen.getByTestId("tab-roles")).toBeDefined();
    expect(screen.getByTestId("tab-users")).toBeDefined();
    expect(screen.getByTestId("tab-categories")).toBeDefined();
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

  it("isolates a throwing Admin tab without RouteErrorBoundary (#3202 / #3195)", () => {
    const Boom: React.FC = () => {
      throw new Error("section boom");
    };
    const spy = vi.spyOn(console, "error").mockImplementation(() => undefined);
    render(
      <AdminSectionErrorBoundary label="workflow">
        <Boom />
      </AdminSectionErrorBoundary>,
    );
    expect(screen.getByTestId("admin-section-error")).toBeDefined();
    expect(screen.getByText(/Unable to load workflow/i)).toBeDefined();
    expect(screen.queryByTestId("route-error")).toBeNull();
    spy.mockRestore();
  });

  it("hosts former Workflow admin sections as Admin tabs (#3088)", () => {
    renderShell(<AdminShell initialTab="workflow" />);
    expect(screen.getByTestId("mock-workflow-section")).toBeDefined();
    expect(screen.getByTestId("tab-workflow").getAttribute("aria-selected")).toBe(
      "true",
    );

    fireEvent.click(screen.getByTestId("tab-roles"));
    expect(screen.getByTestId("perc-roles-section")).toBeDefined();
    expect(screen.queryByTestId("mock-workflow-section")).toBeNull();

    fireEvent.click(screen.getByTestId("tab-users"));
    expect(screen.getByTestId("perc-users-section")).toBeDefined();

    fireEvent.click(screen.getByTestId("tab-categories"));
    expect(screen.getByTestId("perc-categories-section")).toBeDefined();
  });
});
