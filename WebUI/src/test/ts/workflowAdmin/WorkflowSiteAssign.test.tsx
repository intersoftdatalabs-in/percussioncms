/*
 * Copyright 1999-2026 Percussion Software, Inc.
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
import { WorkflowSiteAssign } from "../../../main/ts/workflowAdmin/workflow/WorkflowSiteAssign";
import * as client from "../../../main/ts/api/client";

vi.mock("../../../main/ts/api/client", () => ({
  get: vi.fn(),
  post: vi.fn(),
  put: vi.fn(),
  del: vi.fn(),
}));

describe("WorkflowSiteAssign", () => {
  beforeEach(() => {
    vi.resetAllMocks();
  });

  it("renders dialog with site dropdown after fetching sites", async () => {
    const mockSites = [
      { id: "s1", name: "SiteA", folderPath: "//Sites/SiteA" },
      { id: "s2", name: "SiteB", folderPath: "//Sites/SiteB" },
    ];
    vi.mocked(client.get).mockResolvedValue(mockSites);

    render(<WorkflowSiteAssign workflowName="Default Workflow" onClose={vi.fn()} />);

    await waitFor(() => {
      expect(screen.getByTestId("site-select")).toBeTruthy();
    });
    expect(screen.getByTestId("start-assignment-button")).toBeTruthy();
  });

  it("starts assignment job when path is selected and button clicked", async () => {
    vi.mocked(client.get).mockImplementation(async (url: string) => {
      if (url.includes("sitemanage/site")) {
        return [{ id: "s1", name: "SiteA", folderPath: "//Sites/SiteA" }];
      }
      if (url.includes("GetAssociatedFoldersJob")) {
        return { jobId: "job-1", status: "RUNNING" };
      }
      if (url.includes("workflowassignment/isInProgress")) {
        return { isInProgress: false };
      }
      return {};
    });

    render(<WorkflowSiteAssign workflowName="Default Workflow" onClose={vi.fn()} />);

    await waitFor(() => {
      expect(screen.getByTestId("site-select")).toBeTruthy();
    });

    fireEvent.change(screen.getByTestId("site-select"), {
      target: { value: "//Sites/SiteA" },
    });

    fireEvent.click(screen.getByTestId("start-assignment-button"));

    await waitFor(() => {
      expect(screen.getByTestId("job-status-msg")).toBeTruthy();
    });
  });
});
