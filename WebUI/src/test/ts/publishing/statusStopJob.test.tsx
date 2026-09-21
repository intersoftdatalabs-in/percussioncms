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
 *
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { afterEach, describe, expect, it, vi } from "vitest";
import { StatusSection } from "@/publishing/sections/StatusSection";

vi.mock("@/api/publishing/statusApi", () => ({
  fetchCurrentJobs: vi.fn().mockResolvedValue([
    { jobId: 4615, siteName: "FastForward", status: "Running", completedItems: 1, totalItems: 10 },
    { jobId: 99, siteName: "DoneSite", status: "Completed", completedItems: 5, totalItems: 5 },
  ]),
}));

vi.mock("@/api/publishing/serversApi", () => ({
  stopPublishing: vi.fn().mockResolvedValue({}),
}));

vi.mock("@/api/publishing/itemHistoryApi", () => ({
  fetchItemPublishingHistory: vi.fn().mockResolvedValue([]),
}));

const statusApi = await import("@/api/publishing/statusApi");
const serversApi = await import("@/api/publishing/serversApi");

describe("StatusSection cancel/stop job (#4615)", () => {
  afterEach(() => {
    vi.mocked(serversApi.stopPublishing).mockClear();
    vi.mocked(window.confirm)?.mockRestore?.();
  });

  it("shows Stop only for running jobs", async () => {
    render(<StatusSection />);
    await waitFor(() => {
      expect(screen.getByTestId("publish-stop-job-4615")).toBeTruthy();
    });
    expect(screen.queryByTestId("publish-stop-job-99")).toBeNull();
  });

  it("does not call stop when confirm is cancelled", async () => {
    vi.spyOn(window, "confirm").mockReturnValue(false);
    render(<StatusSection />);
    await waitFor(() => {
      expect(screen.getByTestId("publish-stop-job-4615")).toBeTruthy();
    });
    fireEvent.click(screen.getByTestId("publish-stop-job-4615"));
    expect(serversApi.stopPublishing).not.toHaveBeenCalled();
  });

  it("posts stop after confirm", async () => {
    vi.spyOn(window, "confirm").mockReturnValue(true);
    render(<StatusSection />);
    await waitFor(() => {
      expect(screen.getByTestId("publish-stop-job-4615")).toBeTruthy();
    });
    fireEvent.click(screen.getByTestId("publish-stop-job-4615"));
    await waitFor(() => {
      expect(serversApi.stopPublishing).toHaveBeenCalledWith(4615);
    });
    expect(statusApi.fetchCurrentJobs).toHaveBeenCalled();
  });

  it("maps HTTP 403 as error, not success", async () => {
    vi.spyOn(window, "confirm").mockReturnValue(true);
    vi.mocked(serversApi.stopPublishing).mockRejectedValueOnce({
      status: 403,
      statusText: "Forbidden",
      body: {},
    });
    render(<StatusSection />);
    await waitFor(() => {
      expect(screen.getByTestId("publish-stop-job-4615")).toBeTruthy();
    });
    fireEvent.click(screen.getByTestId("publish-stop-job-4615"));
    await waitFor(() => {
      expect(screen.getByRole("alert").textContent).toMatch(/Forbidden|403/i);
    });
  });
});
