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
import { render, screen, waitFor, fireEvent } from "@testing-library/react";
import { describe, expect, it, vi, beforeEach } from "vitest";
import { WorkflowSection } from "../../../main/ts/workflowAdmin/workflow/WorkflowSection";
import * as client from "../../../main/ts/api/client";

vi.mock("../../../main/ts/api/client", () => ({
  get: vi.fn(),
  post: vi.fn(),
  put: vi.fn(),
  del: vi.fn(),
}));

describe("WorkflowSection", () => {
  beforeEach(() => {
    vi.resetAllMocks();
  });

  it("renders workflow list after loading", async () => {
    const mockWorkflows = [
      { name: "Default Workflow", isDefault: true, stagingRoleId: "Editor", steps: [] },
      { name: "Blog Workflow", isDefault: false, stagingRoleId: "Publisher", steps: [] },
    ];
    vi.mocked(client.get).mockResolvedValue(mockWorkflows);
    vi.mocked(client.post).mockResolvedValue([{ name: "Editor" }, { name: "Publisher" }]);

    render(<WorkflowSection />);
    expect(screen.getByText(/loading/i)).toBeTruthy();

    await waitFor(() => {
      expect(screen.getByTestId("workflow-row-Default Workflow")).toBeTruthy();
      expect(screen.getByTestId("workflow-row-Blog Workflow")).toBeTruthy();
    });
  });

  it("opens create editor when Create Workflow button is clicked", async () => {
    vi.mocked(client.get).mockResolvedValue([]);
    vi.mocked(client.post).mockResolvedValue([]);

    render(<WorkflowSection />);
    await waitFor(() => {
      expect(screen.getByTestId("create-workflow-button")).toBeTruthy();
    });

    fireEvent.click(screen.getByTestId("create-workflow-button"));
    expect(screen.getByTestId("perc-workflow-editor")).toBeTruthy();
  });
});
