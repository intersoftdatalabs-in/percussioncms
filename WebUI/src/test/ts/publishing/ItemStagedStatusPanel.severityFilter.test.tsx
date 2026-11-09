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
import { describe, expect, it, vi, beforeEach } from "vitest";
import { ItemStagedStatusPanel } from "@/publishing/components/ItemStagedStatusPanel";
import { fetchItemPublishingHistory } from "@/api/publishing/itemHistoryApi";

vi.mock("@/api/publishing/itemHistoryApi", () => ({
  fetchItemPublishingHistory: vi.fn(),
}));

const fetchStatus = vi.mocked(fetchItemPublishingHistory);

const LINES = [
  {
    server: "prod",
    location: "/home",
    revisionId: 3,
    publishedDate: 200,
    operation: "publish",
    status: "SUCCESS",
  },
  {
    server: "prod",
    location: "/home",
    revisionId: 2,
    publishedDate: 100,
    operation: "publish",
    status: "FAILURE",
    errorMessage: "denied",
  },
];

describe("ItemStagedStatusPanel severity filter (#5887)", () => {
  beforeEach(() => {
    fetchStatus.mockReset();
    fetchStatus.mockResolvedValue(LINES);
  });

  it("narrows the displayed log lines without refetching", async () => {
    render(<ItemStagedStatusPanel itemId="42" />);
    await waitFor(() => {
      expect(screen.getByTestId("item-staged-status-table")).toBeTruthy();
    });
    expect(screen.getAllByTestId("item-staged-status-row")).toHaveLength(2);

    fireEvent.change(screen.getByTestId("item-staged-status-severity"), {
      target: { value: "failure" },
    });
    expect(screen.getAllByTestId("item-staged-status-row")).toHaveLength(1);
    expect(
      screen.getAllByTestId("item-staged-status-row")[0].textContent,
    ).toContain("FAILURE");
    // Client-side only: the filter issues no new REST call.
    expect(fetchStatus).toHaveBeenCalledTimes(1);
    // The summary still describes the whole lookup, not the filter.
    expect(
      screen.getByTestId("item-staged-status-summary").textContent,
    ).toContain("SUCCESS");
  });

  it("shows an empty-filter state when nothing matches", async () => {
    fetchStatus.mockResolvedValue([LINES[1]]);
    render(<ItemStagedStatusPanel itemId="42" />);
    await waitFor(() => {
      expect(screen.getByTestId("item-staged-status-table")).toBeTruthy();
    });

    fireEvent.change(screen.getByTestId("item-staged-status-severity"), {
      target: { value: "success" },
    });
    expect(screen.queryByTestId("item-staged-status-table")).toBeNull();
    expect(
      screen.getByTestId("item-staged-status-filter-empty").textContent,
    ).toMatch(/no log lines match/i);
    expect(fetchStatus).toHaveBeenCalledTimes(1);
  });

  it("clearing the filter restores the full list", async () => {
    render(<ItemStagedStatusPanel itemId="42" />);
    await waitFor(() => {
      expect(screen.getByTestId("item-staged-status-table")).toBeTruthy();
    });

    fireEvent.change(screen.getByTestId("item-staged-status-severity"), {
      target: { value: "failure" },
    });
    expect(screen.getAllByTestId("item-staged-status-row")).toHaveLength(1);

    fireEvent.change(screen.getByTestId("item-staged-status-severity"), {
      target: { value: "all" },
    });
    expect(screen.getAllByTestId("item-staged-status-row")).toHaveLength(2);
    expect(screen.queryByTestId("item-staged-status-filter-empty")).toBeNull();
    expect(fetchStatus).toHaveBeenCalledTimes(1);
  });
});
