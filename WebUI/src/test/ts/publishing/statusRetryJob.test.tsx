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
    {
      jobId: 38,
      siteName: "FastForward",
      pubServerName: "Production",
      status: "Failed",
      publishKind: "incremental",
      completedItems: 1,
      totalItems: 4,
    },
    {
      jobId: 39,
      siteName: "FastForward",
      serverName: "Production",
      status: "Completed with failures",
      publishKind: "full",
      completedItems: 2,
      totalItems: 4,
    },
    {
      jobId: 40,
      siteName: "FastForward",
      status: "Failed",
      completedItems: 0,
      totalItems: 1,
    },
    {
      jobId: 41,
      siteName: "DoneSite",
      serverName: "Production",
      status: "Completed",
      completedItems: 3,
      totalItems: 3,
    },
  ]),
  fetchLogDetails: vi.fn().mockResolvedValue([]),
}));

vi.mock("@/api/publishing/publishApi", () => ({
  publishSite: vi.fn().mockResolvedValue({ status: "Queuing content", jobid: 100 }),
  incrementalPublishSite: vi.fn().mockResolvedValue({
    status: "Queuing content",
    jobid: 101,
  }),
}));

vi.mock("@/api/publishing/serversApi", () => ({
  stopPublishing: vi.fn(),
}));

vi.mock("@/api/publishing/itemHistoryApi", () => ({
  fetchItemPublishingHistory: vi.fn().mockResolvedValue([]),
}));

const statusApi = await import("@/api/publishing/statusApi");
const publishApi = await import("@/api/publishing/publishApi");

describe("StatusSection retry failed job (#4887)", () => {
  afterEach(() => {
    vi.mocked(publishApi.publishSite).mockClear();
    vi.mocked(publishApi.incrementalPublishSite).mockClear();
    vi.mocked(window.confirm)?.mockRestore?.();
  });

  it("shows Retry only when the failed job names a site and server", async () => {
    render(<StatusSection />);
    await waitFor(() => {
      expect(screen.getByTestId("publish-retry-job-38")).toBeTruthy();
    });
    expect(screen.getByTestId("publish-retry-job-39")).toBeTruthy();
    expect(screen.queryByTestId("publish-retry-job-40")).toBeNull();
    expect(screen.queryByTestId("publish-retry-job-41")).toBeNull();
  });

  it("does not publish when confirm is cancelled", async () => {
    vi.spyOn(window, "confirm").mockReturnValue(false);
    render(<StatusSection />);
    await waitFor(() => {
      expect(screen.getByTestId("publish-retry-job-38")).toBeTruthy();
    });
    fireEvent.click(screen.getByTestId("publish-retry-job-38"));
    expect(publishApi.incrementalPublishSite).not.toHaveBeenCalled();
    expect(publishApi.publishSite).not.toHaveBeenCalled();
  });

  it("starts incremental publish for an incremental failure and refreshes status", async () => {
    vi.spyOn(window, "confirm").mockReturnValue(true);
    render(<StatusSection />);
    await waitFor(() => {
      expect(screen.getByTestId("publish-retry-job-38")).toBeTruthy();
    });
    const before = vi.mocked(statusApi.fetchCurrentJobs).mock.calls.length;
    fireEvent.click(screen.getByTestId("publish-retry-job-38"));
    await waitFor(() => {
      expect(publishApi.incrementalPublishSite).toHaveBeenCalledWith(
        "FastForward",
        "Production",
      );
    });
    expect(publishApi.publishSite).not.toHaveBeenCalled();
    await waitFor(() => {
      expect(vi.mocked(statusApi.fetchCurrentJobs).mock.calls.length).toBeGreaterThan(
        before,
      );
    });
  });

  it("starts full publish for a full completed-with-failures job", async () => {
    vi.spyOn(window, "confirm").mockReturnValue(true);
    render(<StatusSection />);
    await waitFor(() => {
      expect(screen.getByTestId("publish-retry-job-39")).toBeTruthy();
    });
    fireEvent.click(screen.getByTestId("publish-retry-job-39"));
    await waitFor(() => {
      expect(publishApi.publishSite).toHaveBeenCalledWith("FastForward", "Production");
    });
    expect(publishApi.incrementalPublishSite).not.toHaveBeenCalled();
  });

  it("shows BADCONFIG as an error and does not treat it as a started job", async () => {
    vi.spyOn(window, "confirm").mockReturnValue(true);
    vi.mocked(publishApi.publishSite).mockResolvedValueOnce({
      status: "BADCONFIG",
      warningMessage: "missing host",
    });
    render(<StatusSection />);
    await waitFor(() => {
      expect(screen.getByTestId("publish-retry-job-39")).toBeTruthy();
    });
    const before = vi.mocked(statusApi.fetchCurrentJobs).mock.calls.length;
    fireEvent.click(screen.getByTestId("publish-retry-job-39"));
    await waitFor(() => {
      expect(screen.getByRole("alert").textContent).toMatch(/missing host/i);
    });
    expect(vi.mocked(statusApi.fetchCurrentJobs).mock.calls.length).toBe(before);
  });
});
