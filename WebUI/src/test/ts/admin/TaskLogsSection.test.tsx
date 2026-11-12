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
import { TaskLogsSection, formatLogElapsed } from "../../../main/ts/admin/TaskLogsSection";
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

  it("expands a log row to show the TaskDetailLog-equivalent detail (#6160)", async () => {
    const start = new Date("2026-01-01T10:00:00Z").getTime();
    vi.mocked(client.get).mockResolvedValue([
      {
        id: "log-9",
        taskId: "task-9",
        taskName: "Nightly publish",
        startTime: start,
        endTime: start + 65_000,
        success: false,
        problemDescription: "boom detail",
        serverName: "server-1",
      },
    ]);

    render(<TaskLogsSection />);

    await waitFor(() => {
      expect(screen.getByTestId("log-row-log-9")).toBeDefined();
    });

    // Detail hidden until expanded.
    expect(screen.queryByTestId("log-detail-log-9")).toBeNull();

    fireEvent.click(screen.getByTestId("log-expand-log-9"));

    await waitFor(() => {
      expect(screen.getByTestId("log-detail-log-9")).toBeDefined();
    });
    expect(screen.getByTestId("log-detail-taskname-log-9").textContent).toBe(
      "Nightly publish",
    );
    // 65s elapsed formats as HH:mm:ss.
    expect(screen.getByTestId("log-detail-elapsed-log-9").textContent).toBe(
      "00:01:05",
    );
    expect(screen.getByTestId("log-detail-message-log-9").textContent).toBe(
      "boom detail",
    );

    // Collapse hides the detail again.
    fireEvent.click(screen.getByTestId("log-expand-log-9"));
    await waitFor(() => {
      expect(screen.queryByTestId("log-detail-log-9")).toBeNull();
    });
  });

  it("falls back to the task id when the schedule was deleted (#6160)", async () => {
    const start = new Date("2026-01-01T10:00:00Z").getTime();
    vi.mocked(client.get).mockResolvedValue([
      {
        id: "log-orphan",
        taskId: "task-gone",
        taskName: null,
        startTime: start,
        endTime: start,
        success: true,
        serverName: "server-1",
      },
    ]);

    render(<TaskLogsSection />);

    await waitFor(() => {
      expect(screen.getByTestId("log-expand-log-orphan")).toBeDefined();
    });
    fireEvent.click(screen.getByTestId("log-expand-log-orphan"));

    await waitFor(() => {
      expect(screen.getByTestId("log-detail-log-orphan")).toBeDefined();
    });
    expect(screen.getByTestId("log-detail-taskname-log-orphan").textContent).toBe(
      "task-gone",
    );
  });

  it("formats elapsed wall-clock as HH:mm:ss", () => {
    expect(formatLogElapsed(1_000, 61_000 + 1_000)).toBe("00:01:01");
    expect(formatLogElapsed(0, 3_661_000)).toBe("01:01:01");
    expect(formatLogElapsed(undefined, 1_000)).toBe("N/A");
    expect(formatLogElapsed(1_000, undefined)).toBe("N/A");
    expect(formatLogElapsed(2_000, 1_000)).toBe("N/A");
  });
});
