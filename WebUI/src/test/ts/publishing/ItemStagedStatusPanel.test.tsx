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

describe("ItemStagedStatusPanel", () => {
  beforeEach(() => {
    fetchStatus.mockReset();
  });

  it("shows idle empty state until an item id is looked up", () => {
    render(<ItemStagedStatusPanel />);
    expect(screen.getByTestId("item-staged-status")).toBeTruthy();
    expect(screen.getByTestId("item-staged-status-idle")).toBeTruthy();
    expect(fetchStatus).not.toHaveBeenCalled();
  });

  it("renders status summary plus log lines on success", async () => {
    fetchStatus.mockResolvedValue([
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
    ]);
    render(<ItemStagedStatusPanel itemId="42" />);
    await waitFor(() => {
      expect(screen.getByTestId("item-staged-status-table")).toBeTruthy();
    });
    expect(fetchStatus).toHaveBeenCalledWith("42");
    expect(
      screen.getByTestId("item-staged-status-summary").textContent,
    ).toContain("SUCCESS");
    expect(screen.getAllByTestId("item-staged-status-row")).toHaveLength(2);
    expect(screen.queryByTestId("item-staged-status-error")).toBeNull();
  });

  it("shows explicit empty status after a successful empty lookup", async () => {
    fetchStatus.mockResolvedValue([]);
    render(<ItemStagedStatusPanel itemId="42" />);
    await waitFor(() => {
      expect(screen.getByTestId("item-staged-status-empty")).toBeTruthy();
    });
  });

  it("keeps the previous view when a later fetch fails with 409", async () => {
    fetchStatus.mockResolvedValue([
      {
        server: "prod",
        operation: "publish",
        status: "SUCCESS",
        publishedDate: 200,
      },
    ]);
    const { rerender } = render(<ItemStagedStatusPanel itemId="42" />);
    await waitFor(() => {
      expect(screen.getByTestId("item-staged-status-table")).toBeTruthy();
    });

    fetchStatus.mockRejectedValue({ status: 409, statusText: "Conflict" });
    rerender(<ItemStagedStatusPanel itemId="43" />);
    await waitFor(() => {
      expect(screen.getByTestId("item-staged-status-error")).toHaveTextContent(
        /conflict/i,
      );
    });
    // Previous view is kept: the earlier rows stay on screen.
    expect(screen.getByTestId("item-staged-status-table")).toBeTruthy();
    expect(
      screen.getAllByTestId("item-staged-status-row")[0].textContent,
    ).toContain("prod");
  });

  it("maps 400, 403, 404, and 500 to errors, not empty success", async () => {
    fetchStatus.mockRejectedValue({ status: 404, statusText: "Not Found" });
    const { rerender } = render(<ItemStagedStatusPanel itemId="missing" />);
    await waitFor(() => {
      expect(screen.getByTestId("item-staged-status-error")).toBeTruthy();
    });
    expect(screen.queryByTestId("item-staged-status-empty")).toBeNull();
    expect(screen.queryByTestId("item-staged-status-table")).toBeNull();

    fetchStatus.mockRejectedValue({ status: 500, statusText: "Server Error" });
    rerender(<ItemStagedStatusPanel itemId="broken" />);
    await waitFor(() => {
      expect(screen.getByTestId("item-staged-status-error")).toBeTruthy();
    });
    expect(screen.queryByTestId("item-staged-status-table")).toBeNull();
  });

  it("looks up from the form without any write control", async () => {
    fetchStatus.mockResolvedValue([]);
    const onId = vi.fn();
    render(<ItemStagedStatusPanel onItemIdChange={onId} />);
    fireEvent.change(screen.getByTestId("item-staged-status-id"), {
      target: { value: "42" },
    });
    fireEvent.click(screen.getByTestId("item-staged-status-lookup"));
    await waitFor(() => {
      expect(fetchStatus).toHaveBeenCalledWith("42");
    });
    expect(onId).toHaveBeenCalledWith("42");
    // Read-only: no stage / publish / delete submit exists on the panel.
    expect(screen.queryByTestId("item-stage-submit")).toBeNull();
    expect(screen.queryByTestId("item-publish-now-submit")).toBeNull();
  });

  it("rejects unsafe item ids without calling the API", async () => {
    render(<ItemStagedStatusPanel />);
    fireEvent.change(screen.getByTestId("item-staged-status-id"), {
      target: { value: "<script>" },
    });
    fireEvent.click(screen.getByTestId("item-staged-status-lookup"));
    await waitFor(() => {
      expect(screen.getByTestId("item-staged-status-error")).toBeTruthy();
    });
    expect(fetchStatus).not.toHaveBeenCalled();
  });
});
