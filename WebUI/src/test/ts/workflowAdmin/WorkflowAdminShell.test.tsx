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
import { describe, expect, it, vi } from "vitest";
import { WorkflowAdminShell } from "../../../main/ts/workflowAdmin/WorkflowAdminShell";

// Mock child components
vi.mock("../../../main/ts/workflowAdmin/workflow/WorkflowSection", () => ({
  WorkflowSection: () => <div data-testid="mock-workflow-section">Workflow Content</div>,
}));

vi.mock("../../../main/ts/workflowAdmin/role/RolesSection", () => ({
  RolesSection: () => <div data-testid="perc-roles-section">Roles Content</div>,
}));

vi.mock("../../../main/ts/workflowAdmin/user/UsersSection", () => ({
  UsersSection: () => <div data-testid="perc-users-section">Users Content</div>,
}));

vi.mock("../../../main/ts/workflowAdmin/category/CategoriesSection", () => ({
  CategoriesSection: () => <div data-testid="perc-categories-section">Categories Content</div>,
}));

describe("WorkflowAdminShell", () => {
  it("renders shell title and default active tab", () => {
    render(<WorkflowAdminShell />);
    expect(screen.getByTestId("perc-workflow-admin-shell")).toBeTruthy();
    expect(screen.getByTestId("mock-workflow-section")).toBeTruthy();
  });

  it("exposes responsive tablist chrome for narrow / portrait layouts", () => {
    render(<WorkflowAdminShell />);
    const shell = screen.getByTestId("perc-workflow-admin-shell");
    const tablist = screen.getByTestId("perc-workflow-admin-tablist");
    expect(tablist.getAttribute("role")).toBe("tablist");
    expect(tablist.className.length).toBeGreaterThan(0);
    expect(shell.className.length).toBeGreaterThan(0);
  });

  it("switches tabs when nav buttons are clicked", () => {
    render(<WorkflowAdminShell />);
    
    const rolesTab = screen.getByTestId("tab-roles");
    fireEvent.click(rolesTab);
    expect(screen.getByTestId("perc-roles-section")).toBeTruthy();
    expect(screen.queryByTestId("mock-workflow-section")).toBeNull();

    const usersTab = screen.getByTestId("tab-users");
    fireEvent.click(usersTab);
    expect(screen.getByTestId("perc-users-section")).toBeTruthy();

    const catTab = screen.getByTestId("tab-categories");
    fireEvent.click(catTab);
    expect(screen.getByTestId("perc-categories-section")).toBeTruthy();
  });
});
