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
import { filterJobsBySite } from "@/publishing/statusSiteFilter";
import { StatusSection } from "@/publishing/sections/StatusSection";

describe("filterJobsBySite", () => {
  const jobs = [
    { jobId: 1, siteName: "FastForward", siteId: 10, status: "Running" },
    { jobId: 2, siteName: "DoneSite", siteId: 22, status: "Completed" },
  ];

  it("returns every job when the query is blank", () => {
    expect(filterJobsBySite(jobs, "")).toEqual(jobs);
    expect(filterJobsBySite(jobs, "   ")).toEqual(jobs);
  });

  it("matches site name or id case-insensitively", () => {
    expect(filterJobsBySite(jobs, "fast").map((j) => j.jobId)).toEqual([1]);
    expect(filterJobsBySite(jobs, "22").map((j) => j.jobId)).toEqual([2]);
    expect(filterJobsBySite(jobs, "nope")).toEqual([]);
  });
});

vi.mock("@/api/publishing/statusApi", () => ({
  fetchCurrentJobs: vi.fn().mockResolvedValue([
    {
      jobId: 1,
      siteName: "FastForward",
      siteId: 10,
      status: "Running",
      completedItems: 1,
      totalItems: 4,
    },
    {
      jobId: 2,
      siteName: "DoneSite",
      siteId: 22,
      status: "Completed",
      completedItems: 3,
      totalItems: 3,
    },
  ]),
}));

vi.mock("@/api/publishing/serversApi", () => ({
  stopPublishing: vi.fn().mockResolvedValue({}),
}));

vi.mock("@/api/publishing/itemHistoryApi", () => ({
  fetchItemPublishingHistory: vi.fn().mockResolvedValue([]),
}));

describe("StatusSection site filter (#4765)", () => {
  it("narrows the table, shows an empty match, and restores the list", async () => {
    render(<StatusSection />);
    await waitFor(() => {
      expect(screen.getByTestId("publish-stop-job-1")).toBeTruthy();
    });
    expect(screen.getByText("DoneSite")).toBeTruthy();

    const filter = screen.getByTestId("publish-status-site-filter");
    fireEvent.change(filter, { target: { value: "donesite" } });
    expect(screen.queryByTestId("publish-stop-job-1")).toBeNull();
    expect(screen.getByText("DoneSite")).toBeTruthy();

    fireEvent.change(filter, { target: { value: "missing-site" } });
    expect(screen.getByTestId("publish-status-empty-filter")).toBeTruthy();
    expect(screen.queryByText("DoneSite")).toBeNull();

    fireEvent.change(filter, { target: { value: "10" } });
    expect(screen.getByTestId("publish-stop-job-1")).toBeTruthy();
    expect(screen.queryByText("DoneSite")).toBeNull();

    fireEvent.change(filter, { target: { value: "" } });
    expect(screen.getByText("DoneSite")).toBeTruthy();
    expect(screen.getByTestId("publish-stop-job-1")).toBeTruthy();
  });
});
