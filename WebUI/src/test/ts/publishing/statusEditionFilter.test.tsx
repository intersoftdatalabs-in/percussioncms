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
import { beforeEach, describe, expect, it, vi } from "vitest";
import { filterJobsByEdition } from "@/publishing/statusEditionFilter";
import { StatusSection } from "@/publishing/sections/StatusSection";
import { fetchCurrentJobs } from "@/api/publishing/statusApi";

describe("filterJobsByEdition", () => {
  const jobs = [
    { jobId: 1, siteName: "FastForward", editionName: "Nightly Full", status: "Running" },
    { jobId: 2, siteName: "DoneSite", editionName: "Hourly Incremental", status: "Completed" },
    { jobId: 3, siteName: "Bare", status: "Running" },
  ];

  it("returns every job when the query is blank", () => {
    expect(filterJobsByEdition(jobs, "")).toEqual(jobs);
    expect(filterJobsByEdition(jobs, "   ")).toEqual(jobs);
  });

  it("matches edition name case-insensitively and skips jobs with no name", () => {
    expect(filterJobsByEdition(jobs, "nightly").map((j) => j.jobId)).toEqual([1]);
    expect(filterJobsByEdition(jobs, "INCREMENTAL").map((j) => j.jobId)).toEqual([2]);
    expect(filterJobsByEdition(jobs, "nope")).toEqual([]);
  });
});

const LOADED = [
  {
    jobId: 1,
    siteName: "FastForward",
    siteId: 10,
    editionName: "Nightly Full",
    status: "Running",
    completedItems: 1,
    totalItems: 4,
  },
  {
    jobId: 2,
    siteName: "DoneSite",
    siteId: 22,
    editionName: "Hourly Incremental",
    status: "Completed",
    completedItems: 3,
    totalItems: 3,
  },
];

vi.mock("@/api/publishing/statusApi", () => ({
  fetchCurrentJobs: vi.fn(),
}));

vi.mock("@/api/publishing/serversApi", () => ({
  stopPublishing: vi.fn().mockResolvedValue({}),
}));

vi.mock("@/api/publishing/itemHistoryApi", () => ({
  fetchItemPublishingHistory: vi.fn().mockResolvedValue([]),
}));

describe("StatusSection edition filter (#4914)", () => {
  beforeEach(() => {
    vi.mocked(fetchCurrentJobs).mockReset();
    vi.mocked(fetchCurrentJobs).mockResolvedValue(LOADED);
  });

  it("narrows by edition, shows an empty match, and restores the site-scoped list", async () => {
    render(<StatusSection />);
    await waitFor(() => {
      expect(screen.getByTestId("publish-stop-job-1")).toBeTruthy();
    });

    const site = screen.getByTestId("publish-status-site-filter");
    fireEvent.change(site, { target: { value: "site" } });
    expect(screen.queryByTestId("publish-stop-job-1")).toBeNull();
    expect(screen.getByText("DoneSite")).toBeTruthy();

    const edition = screen.getByTestId("publish-status-edition-filter");
    fireEvent.change(edition, { target: { value: "HOURLY" } });
    expect(screen.getByText("DoneSite")).toBeTruthy();
    expect(screen.queryByText("FastForward")).toBeNull();

    fireEvent.change(edition, { target: { value: "missing-edition" } });
    const empty = screen.getByTestId("publish-status-empty-filter");
    expect(empty.textContent).toMatch(/edition filter/i);
    expect(screen.queryByRole("alert")).toBeNull();

    fireEvent.change(edition, { target: { value: "" } });
    expect(screen.getByText("DoneSite")).toBeTruthy();
    expect(screen.queryByTestId("publish-stop-job-1")).toBeNull();
  });

  it("keeps the status panel when a later reload is forbidden", async () => {
    vi.mocked(fetchCurrentJobs)
      .mockResolvedValueOnce(LOADED)
      .mockRejectedValueOnce({ status: 403, message: "Forbidden" });

    render(<StatusSection />);
    await waitFor(() => {
      expect(screen.getByTestId("publish-section-status")).toBeTruthy();
      expect(screen.getByTestId("publish-stop-job-1")).toBeTruthy();
    });

    await waitFor(
      () => {
        expect(screen.getByRole("alert")).toBeTruthy();
      },
      { timeout: 7000 },
    );
    expect(screen.getByTestId("publish-section-status")).toBeTruthy();
    expect(screen.getByTestId("publish-stop-job-1")).toBeTruthy();
  });
});
