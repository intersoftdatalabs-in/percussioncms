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
import { describe, expect, it, vi } from "vitest";
import { StatusSection } from "@/publishing/sections/StatusSection";

vi.mock("@/api/publishing/statusApi", () => ({
  fetchCurrentJobs: vi.fn().mockResolvedValue([
    {
      jobId: 4789,
      siteName: "FastForward",
      editionName: "Nightly Full",
      status: "Failed",
      errorMessage: "disk full",
      completedItems: 1,
      totalItems: 4,
    },
    {
      jobId: 12,
      status: "Running",
      completedItems: 0,
      totalItems: 1,
    },
  ]),
}));

vi.mock("@/api/publishing/serversApi", () => ({
  stopPublishing: vi.fn().mockResolvedValue({}),
}));

vi.mock("@/api/publishing/itemHistoryApi", () => ({
  fetchItemPublishingHistory: vi.fn().mockResolvedValue([]),
}));

const serversApi = await import("@/api/publishing/serversApi");

describe("StatusSection job detail (#4789)", () => {
  it("opens detail fields and close does not stop the job", async () => {
    render(<StatusSection />);
    await waitFor(() => {
      expect(screen.getByTestId("publish-status-job-4789")).toBeTruthy();
    });
    fireEvent.click(screen.getByTestId("publish-status-job-4789"));
    expect(screen.getByTestId("publish-status-detail-job-id").textContent).toBe(
      "4789",
    );
    expect(screen.getByTestId("publish-status-detail-site").textContent).toBe(
      "FastForward",
    );
    expect(screen.getByTestId("publish-status-detail-edition").textContent).toBe(
      "Nightly Full",
    );
    expect(screen.getByTestId("publish-status-detail-status").textContent).toBe(
      "Failed",
    );
    expect(screen.getByTestId("publish-status-detail-error").textContent).toBe(
      "disk full",
    );
    expect(screen.getByTestId("publish-status-detail-heading").textContent).toBe(
      "Job details",
    );
    expect(
      screen.getByTestId("publish-status-detail-job-id-label").textContent,
    ).toBe("Job ID");
    expect(
      screen.getByTestId("publish-status-detail-edition-label").textContent,
    ).toBe("Edition");
    expect(
      screen.getByTestId("publish-status-detail-error-label").textContent,
    ).toBe("Error");

    fireEvent.click(screen.getByTestId("publish-status-job-detail-close"));
    expect(screen.queryByTestId("publish-status-job-detail")).toBeNull();
    expect(serversApi.stopPublishing).not.toHaveBeenCalled();
  });

  it("leaves optional fields empty when the job omits them", async () => {
    render(<StatusSection />);
    await waitFor(() => {
      expect(screen.getByTestId("publish-status-job-12")).toBeTruthy();
    });
    fireEvent.click(screen.getByTestId("publish-status-job-12"));
    expect(screen.getByTestId("publish-status-detail-site").textContent).toBe("");
    expect(screen.getByTestId("publish-status-detail-edition").textContent).toBe(
      "",
    );
    expect(screen.queryByTestId("publish-status-detail-error")).toBeNull();
  });

  it("stop stays a row action and does not require the detail panel", async () => {
    vi.spyOn(window, "confirm").mockReturnValue(false);
    render(<StatusSection />);
    await waitFor(() => {
      expect(screen.getByTestId("publish-stop-job-12")).toBeTruthy();
    });
    fireEvent.click(screen.getByTestId("publish-stop-job-12"));
    expect(screen.queryByTestId("publish-status-job-detail")).toBeNull();
    expect(serversApi.stopPublishing).not.toHaveBeenCalled();
  });
});
