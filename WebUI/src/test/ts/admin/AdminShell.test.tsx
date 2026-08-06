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

describe("AdminShell", () => {
  it("renders administration shell title and default active tab", () => {
    render(<AdminShell />);
    expect(screen.getByTestId("perc-admin-shell")).toBeDefined();
    expect(screen.getByTestId("mock-tasks-section")).toBeDefined();
  });

  it("switches tabs when nav buttons are clicked", () => {
    render(<AdminShell />);

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
