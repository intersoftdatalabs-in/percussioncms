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
import { fireEvent, render, screen } from "@testing-library/react";
import { MemoryRouter } from "react-router";
import { describe, expect, it, vi } from "vitest";
import { AdminShell } from "../../../main/ts/admin/AdminShell";

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
  ToolsSection: () => {
    throw new Error("map is not a function");
  },
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

describe("AdminShell System Tools isolation (#3195)", () => {
  it("keeps Admin shell when ToolsSection throws (no RouteErrorBoundary)", () => {
    const spy = vi.spyOn(console, "error").mockImplementation(() => undefined);
    renderShell(<AdminShell initialTab="tools" />);

    expect(screen.getByTestId("perc-admin-shell")).toBeDefined();
    expect(screen.getByTestId("perc-admin-tablist")).toBeDefined();
    expect(screen.getByTestId("admin-section-error")).toBeDefined();
    expect(screen.queryByTestId("route-error")).toBeNull();
    expect(screen.queryByText(/Unable to load Admin/i)).toBeNull();

    fireEvent.click(screen.getByTestId("tab-tasks"));
    expect(screen.getByTestId("mock-tasks-section")).toBeDefined();
    spy.mockRestore();
  });
});
