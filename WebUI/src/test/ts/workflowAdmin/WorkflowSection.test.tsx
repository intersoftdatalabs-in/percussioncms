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
    vi.mocked(client.get).mockImplementation(async (url: string) => {
      if (url.includes("user/roles") || url.includes("/roles")) {
        return { RoleList: { roles: ["Editor", "Publisher"] } };
      }
      return mockWorkflows;
    });

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

  it("opens create editor when Create Workflow button is clicked", async () => {
    vi.mocked(client.get).mockImplementation(async (url: string) => {
      if (url.includes("user/roles") || url.includes("/roles")) {
        return { RoleList: { roles: [] } };
      }
      return [];
    });

    render(<WorkflowSection />);
    await waitFor(() => {
      expect(screen.getByTestId("create-workflow-button")).toBeTruthy();
    });

    fireEvent.click(screen.getByTestId("create-workflow-button"));
    expect(screen.getByTestId("perc-workflow-editor")).toBeTruthy();
  });

  it("loads available roles from USER_ROLES and does not post PSStringWrapper shape { name }", async () => {
    vi.mocked(client.get).mockImplementation(async (url: string) => {
      if (String(url).includes("user/roles")) {
        return { RoleList: { roles: ["Admin", "Editor"] } };
      }
      return [{ name: "Default Workflow", isDefault: true, stagingRoleId: "Admin", steps: [] }];
    });

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
