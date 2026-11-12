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

import { render, screen, waitFor } from "@testing-library/react";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import {
  STATUS_POLL_INTERVAL_MS,
  StatusSection,
} from "@/publishing/sections/StatusSection";

vi.mock("@/api/publishing/statusApi", () => ({
  fetchCurrentJobs: vi.fn().mockResolvedValue([]),
  fetchLogDetails: vi.fn().mockResolvedValue([]),
}));

vi.mock("@/api/publishing/publishApi", () => ({
  publishSite: vi.fn(),
  incrementalPublishSite: vi.fn(),
}));

vi.mock("@/api/publishing/serversApi", () => ({
  stopPublishing: vi.fn(),
}));

vi.mock("@/api/publishing/itemHistoryApi", () => ({
  fetchItemPublishingHistory: vi.fn().mockResolvedValue([]),
}));

vi.mock("@/api/publishing/itemScheduleDatesApi", () => ({
  fetchItemScheduleDates: vi.fn().mockResolvedValue([]),
}));

const statusApi = await import("@/api/publishing/statusApi");

describe("status poll lifecycle constants", () => {
  it("polls exactly every 5s — measured parity with Minuet (#6092, SC-004)", () => {
    // Minuet baseline: WebUI/war/views/PercPublishStatusMinuetView.js
    // renderPublishStatusSection() -> setInterval(renderPublishStatusList, 5000).
    expect(STATUS_POLL_INTERVAL_MS).toBe(5_000);
  });
});

describe("StatusSection re-poll behavior (#6092, SC-004)", () => {
  beforeEach(() => {
    vi.useFakeTimers();
  });

  afterEach(() => {
    vi.useRealTimers();
    vi.clearAllMocks();
  });

  it("re-fetches current jobs on every poll tick without a full page reload", async () => {
    const fetchCurrentJobs = vi.mocked(statusApi.fetchCurrentJobs);
    fetchCurrentJobs.mockClear().mockResolvedValue([]);
    render(<StatusSection />);
    // Mount effect fires the first load synchronously.
    expect(fetchCurrentJobs).toHaveBeenCalledTimes(1);
    expect(screen.getByTestId("publish-section-status")).toBeInTheDocument();
    await vi.advanceTimersByTimeAsync(STATUS_POLL_INTERVAL_MS);
    expect(fetchCurrentJobs).toHaveBeenCalledTimes(2);
    await vi.advanceTimersByTimeAsync(STATUS_POLL_INTERVAL_MS * 3);
    expect(fetchCurrentJobs).toHaveBeenCalledTimes(5);
  });
});
