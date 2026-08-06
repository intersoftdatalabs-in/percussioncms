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
import { TasksSection } from "../../../main/ts/admin/TasksSection";
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

describe("TasksSection", () => {
  beforeEach(() => {
    vi.resetAllMocks();
  });

  it("renders scheduled task list after fetching successfully", async () => {
    vi.mocked(client.get).mockImplementation(async (url: string) => {
      if (url.includes("templates")) {
        return [{ id: "temp-1", name: "Sample Template" }];
      }
      return [
        {
          id: "task-1",
          name: "Logs Purge Task",
          cronSpecification: "0 0 12 * * ?",
          extensionName: "com.percussion.services.schedule.impl.PSPurgeScheduledTaskLog",
        },
      ];
    });

    render(<TasksSection />);

    await waitFor(() => {
      expect(screen.getByTestId("perc-tasks-section")).toBeDefined();
    });
    expect(screen.getByText("Logs Purge Task")).toBeTruthy();
    expect(screen.getByText("0 0 12 * * ?")).toBeTruthy();
  });

  it("opens create dialog when create button is clicked", async () => {
    vi.mocked(client.get).mockImplementation(async (url: string) => {
      if (url.includes("templates")) {
        return [];
      }
      return [];
    });

    render(<TasksSection />);

    await waitFor(() => {
      expect(screen.getByTestId("create-task-btn")).toBeDefined();
    });

    const createBtn = screen.getByTestId("create-task-btn");
    fireEvent.click(createBtn);

    expect(screen.getByTestId("task-dialog")).toBeDefined();
  });
});
