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
import { ConsistencyChecker } from "../../../main/ts/admin/tools/ConsistencyChecker";
import * as client from "../../../main/ts/api/client";

vi.mock("../../../main/ts/api/client", () => ({
  get: vi.fn(),
  post: vi.fn(),
  put: vi.fn(),
  del: vi.fn(),
  buildHeaders: vi.fn(),
  parseBody: vi.fn(),
  handleResponse: vi.fn(),
  getCsrfToken: vi.fn(),
}));

describe("ConsistencyChecker", () => {
  beforeEach(() => {
    vi.resetAllMocks();
  });

  it("renders consistency checker container and run button", () => {
    render(<ConsistencyChecker />);
    expect(screen.getByTestId("perc-consistency-checker")).toBeDefined();
    expect(screen.getByTestId("start-check-btn")).toBeDefined();
  });

  it("starts consistency check and displays reported issues", async () => {
    vi.mocked(client.post).mockResolvedValue({
      jobId: "check-123",
      status: "RUNNING",
    });

    vi.mocked(client.get).mockResolvedValue({
      jobId: "check-123",
      status: "COMPLETE",
      issues: [
        {
          issueId: "issue-1",
          type: "UNLINKED_ASSET",
          description: "Asset #1024 is unlinked.",
          fixable: true,
        },
      ],
    });

    render(<ConsistencyChecker />);

    const runBtn = screen.getByTestId("start-check-btn");
    fireEvent.click(runBtn);

    await waitFor(() => {
      expect(screen.getByTestId("job-status-badge")).toBeDefined();
    });

    expect(screen.getByText("COMPLETE")).toBeDefined();
    expect(screen.getByTestId("issue-row-issue-1")).toBeDefined();
    expect(screen.getByTestId("fix-issue-btn-issue-1")).toBeDefined();
  });

  it("applies fix when fix button is clicked", async () => {
    vi.mocked(client.post).mockImplementation(async (url: string) => {
      if (url.includes("/fix/")) {
        return { success: true };
      }
      return { jobId: "check-123", status: "RUNNING" };
    });

    vi.mocked(client.get).mockResolvedValue({
      jobId: "check-123",
      status: "COMPLETE",
      issues: [
        {
          issueId: "issue-1",
          type: "UNLINKED_ASSET",
          description: "Asset #1024 is unlinked.",
          fixable: true,
        },
      ],
    });

    render(<ConsistencyChecker />);

    fireEvent.click(screen.getByTestId("start-check-btn"));

    await waitFor(() => {
      expect(screen.getByTestId("fix-issue-btn-issue-1")).toBeDefined();
    });

    fireEvent.click(screen.getByTestId("fix-issue-btn-issue-1"));

    await waitFor(() => {
      expect(client.post).toHaveBeenCalledWith(expect.stringContaining("/fix/issue-1"), {});
    });
  });
});
