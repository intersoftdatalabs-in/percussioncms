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
import {
  toWorkflowDefinition,
  WorkflowSection,
} from "../../../main/ts/workflowAdmin/workflow/WorkflowSection";
import * as client from "../../../main/ts/api/client";

vi.mock("../../../main/ts/api/client", () => ({
  get: vi.fn(),
  post: vi.fn(),
  put: vi.fn(),
  del: vi.fn(),
}));

function mockGets(workflowPayload: unknown, roles: string[] = ["Editor", "Publisher"]) {
  vi.mocked(client.get).mockImplementation(async (url: string) => {
    if (url.includes("user/roles") || url.includes("/roles")) {
      return { RoleList: { roles } };
    }
    return workflowPayload;
  });
}

describe("toWorkflowDefinition", () => {
  it("maps admin shape fields", () => {
    expect(
      toWorkflowDefinition({
        name: "Default Workflow",
        isDefault: true,
        stagingRoleId: "Editor",
        steps: [],
      }),
    ).toEqual({
      name: "Default Workflow",
      isDefault: true,
      stagingRoleId: "Editor",
      steps: [],
    });
  });

  it("maps PSUiWorkflow / WorkflowDef fields", () => {
    expect(
      toWorkflowDefinition({
        workflowName: "Default Workflow",
        defaultWorkflow: true,
        stagingRoleNames: "Editor;Publisher",
        workflowSteps: [{ stepName: "Draft", stepRoles: [{ roleName: "Author" }] }],
      }),
    ).toEqual({
      name: "Default Workflow",
      isDefault: true,
      stagingRoleId: "Editor;Publisher",
      steps: [{ name: "Draft", roleNames: ["Author"], position: 0 }],
    });
  });

  it("handles null / empty", () => {
    expect(toWorkflowDefinition(null)).toEqual({ name: "", isDefault: false, steps: [] });
    expect(toWorkflowDefinition(undefined)).toEqual({ name: "", isDefault: false, steps: [] });
  });
});

describe("WorkflowSection", () => {
  beforeEach(() => {
    vi.resetAllMocks();
  });

  it("renders workflow list after loading bare array", async () => {
    const mockWorkflows = [
      { name: "Default Workflow", isDefault: true, stagingRoleId: "Editor", steps: [] },
      { name: "Blog Workflow", isDefault: false, stagingRoleId: "Publisher", steps: [] },
    ];
    mockGets(mockWorkflows);

    render(<WorkflowSection />);
    expect(screen.getByText(/loading/i)).toBeTruthy();

    await waitFor(() => {
      expect(screen.getByTestId("workflow-row-Default Workflow")).toBeTruthy();
      expect(screen.getByTestId("workflow-row-Blog Workflow")).toBeTruthy();
    });

    // #2701: must load role names via GET user/roles — never POST role/find with { name: "" }
    expect(client.post).not.toHaveBeenCalled();
    const getUrls = vi.mocked(client.get).mock.calls.map((c) => String(c[0]));
    expect(getUrls.some((u) => u.includes("user/roles") || u.endsWith("/roles"))).toBe(true);
  });

  it("unwraps nested Workflow WRAP_ROOT without throwing (#3202)", async () => {
    mockGets({
      Workflow: {
        Workflow: [
          {
            workflowName: "Default Workflow",
            defaultWorkflow: true,
            stagingRoleNames: "Editor",
          },
        ],
      },
    });

    render(<WorkflowSection />);

    await waitFor(() => {
      expect(screen.getByTestId("perc-workflow-section")).toBeTruthy();
      expect(screen.getByTestId("workflow-row-Default Workflow")).toBeTruthy();
    });
    expect(screen.queryByTestId("route-error")).toBeNull();
  });

  it("unwraps Jackson Workflow root wrapper without throwing (#2959)", async () => {
    mockGets({
      Workflow: [
        {
          workflowName: "Default Workflow",
          defaultWorkflow: true,
          stagingRoleNames: "Editor",
        },
        {
          workflowName: "Simple Workflow",
          defaultWorkflow: false,
          stagingRoleNames: "",
        },
      ],
    });

    render(<WorkflowSection />);

    await waitFor(() => {
      expect(screen.getByTestId("perc-workflow-section")).toBeTruthy();
      expect(screen.getByTestId("workflow-row-Default Workflow")).toBeTruthy();
      expect(screen.getByTestId("workflow-row-Simple Workflow")).toBeTruthy();
    });
    expect(screen.queryByTestId("route-error")).toBeNull();
  });

  it("renders empty state for empty array payload", async () => {
    mockGets([]);

    render(<WorkflowSection />);
    await waitFor(() => {
      expect(screen.getByTestId("perc-workflow-section")).toBeTruthy();
      expect(screen.getByTestId("create-workflow-button")).toBeTruthy();
    });
    expect(screen.queryByTestId(/^workflow-row-/)).toBeNull();
  });

  it("shows error and does not crash for unexpected non-array object (#2959)", async () => {
    mockGets({ unexpected: true, foo: 1 });

    render(<WorkflowSection />);
    await waitFor(() => {
      expect(screen.getByTestId("perc-workflow-section")).toBeTruthy();
    });
    // Generic error path — no TypeError / no workflow rows
    expect(screen.queryByTestId(/^workflow-row-/)).toBeNull();
    expect(screen.getByTestId("create-workflow-button")).toBeTruthy();
  });

  it("opens create editor when Create Workflow button is clicked", async () => {
    mockGets([]);

    render(<WorkflowSection />);
    await waitFor(() => {
      expect(screen.getByTestId("create-workflow-button")).toBeTruthy();
    });

    fireEvent.click(screen.getByTestId("create-workflow-button"));
    expect(screen.getByTestId("perc-workflow-editor")).toBeTruthy();
  });

  it("loads available roles from USER_ROLES and does not post PSStringWrapper shape { name }", async () => {
    mockGets(
      [{ name: "Default Workflow", isDefault: true, stagingRoleId: "Admin", steps: [] }],
      ["Admin", "Editor"],
    );

    render(<WorkflowSection />);
    await waitFor(() => {
      expect(screen.getByTestId("create-workflow-button")).toBeTruthy();
    });

    fireEvent.click(screen.getByTestId("create-workflow-button"));
    await waitFor(() => {
      expect(screen.getByTestId("perc-workflow-editor")).toBeTruthy();
    });

    // No role/find POST (legacy bug posted { name: "" } which Jackson rejects for PSStringWrapper)
    const postCalls = vi.mocked(client.post).mock.calls;
    for (const call of postCalls) {
      const url = String(call[0] ?? "");
      const body = call[1] as Record<string, unknown> | undefined;
      expect(url.includes("role/find")).toBe(false);
      if (body && typeof body === "object" && "name" in body && !("psstring" in body)) {
        // bare { name: ... } is never a valid PSStringWrapper body
        expect(url.includes("role/")).toBe(false);
      }
    }
  });
});
