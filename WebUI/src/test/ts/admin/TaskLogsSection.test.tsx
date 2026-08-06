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
import { TaskLogsSection } from "../../../main/ts/admin/TaskLogsSection";
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

describe("TaskLogsSection", () => {
  beforeEach(() => {
    vi.resetAllMocks();
  });

  it("renders log list after loading successfully", async () => {
    vi.mocked(client.get).mockResolvedValue([
      {
        id: "log-1",
        taskId: "task-1",
        startTime: new Date().getTime(),
        endTime: new Date().getTime(),
        success: true,
        serverName: "localhost",
      },
    ]);

    render(<TaskLogsSection />);

    await waitFor(() => {
      expect(screen.getByTestId("perc-task-logs-section")).toBeDefined();
    });
    expect(screen.getByText("Success")).toBeTruthy();
    expect(screen.getByText("localhost")).toBeTruthy();
  });

  it("sends delete request when purge logs is triggered", async () => {
    vi.mocked(client.get).mockResolvedValue([
      {
        id: "log-1",
        taskId: "task-1",
        startTime: new Date().getTime(),
        endTime: new Date().getTime(),
        success: true,
        serverName: "localhost",
      },
    ]);

    window.confirm = vi.fn().mockReturnValue(true);

    render(<TaskLogsSection />);

    await waitFor(() => {
      expect(screen.getByTestId("purge-logs-btn")).toBeDefined();
    });

    vi.mocked(client.del).mockResolvedValue({});

    const purgeBtn = screen.getByTestId("purge-logs-btn");
    fireEvent.click(purgeBtn);

    await waitFor(() => {
      expect(client.del).toHaveBeenCalled();
    });
  });
});
