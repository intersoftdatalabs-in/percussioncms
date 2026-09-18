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
import { ItemPublishingHistoryPanel } from "@/publishing/components/ItemPublishingHistoryPanel";
import { fetchItemPublishingHistory } from "@/api/publishing/itemHistoryApi";

vi.mock("@/api/publishing/itemHistoryApi", () => ({
  fetchItemPublishingHistory: vi.fn(),
}));

const fetchHistory = vi.mocked(fetchItemPublishingHistory);

describe("ItemPublishingHistoryPanel", () => {
  beforeEach(() => {
    fetchHistory.mockReset();
  });

  it("shows idle empty state until an item id is looked up", () => {
    render(
      <ItemPublishingHistoryPanel currentSection="logs" />,
    );
    expect(screen.getByTestId("item-publishing-history")).toBeTruthy();
    expect(screen.getByTestId("item-history-idle")).toBeTruthy();
    expect(fetchHistory).not.toHaveBeenCalled();
  });

  it("shows explicit empty history after a successful empty lookup", async () => {
    fetchHistory.mockResolvedValue([]);
    render(
      <ItemPublishingHistoryPanel currentSection="status" itemId="42" />,
    );
    await waitFor(() => {
      expect(screen.getByTestId("item-history-empty")).toBeTruthy();
    });
    expect(fetchHistory).toHaveBeenCalledWith("42");
  });

  it("renders history rows newest-first and surfaces fetch errors", async () => {
    fetchHistory.mockResolvedValue([
      {
        server: "old",
        location: "/a",
        revisionId: 1,
        publishedDate: 100,
        operation: "publish",
        status: "SUCCESS",
      },
      {
        server: "new",
        location: "/b",
        revisionId: 2,
        publishedDate: 200,
        operation: "unpublish",
        status: "FAILURE",
        errorMessage: "denied",
      },
    ]);
    const { rerender } = render(
      <ItemPublishingHistoryPanel currentSection="logs" itemId="9" />,
    );
    await waitFor(() => {
      expect(screen.getByTestId("item-history-table")).toBeTruthy();
    });
    const cells = screen.getAllByTestId("item-history-row");
    expect(cells[0].textContent).toContain("new");
    expect(cells[1].textContent).toContain("old");

    fetchHistory.mockRejectedValue({ status: 500, statusText: "Server Error" });
    rerender(
      <ItemPublishingHistoryPanel currentSection="logs" itemId="99" />,
    );
    await waitFor(() => {
      expect(screen.getByTestId("item-history-error")).toBeTruthy();
    });
  });

  it("looks up from the form and can open the other section", async () => {
    fetchHistory.mockResolvedValue([]);
    const onOpen = vi.fn();
    const onId = vi.fn();
    render(
      <ItemPublishingHistoryPanel
        currentSection="status"
        onOpenSection={onOpen}
        onItemIdChange={onId}
      />,
    );
    fireEvent.change(screen.getByTestId("item-history-id"), {
      target: { value: "16777215-101-1" },
    });
    fireEvent.click(screen.getByTestId("item-history-lookup"));
    await waitFor(() => {
      expect(fetchHistory).toHaveBeenCalledWith("16777215-101-1");
    });
    expect(onId).toHaveBeenCalledWith("16777215-101-1");
    fireEvent.click(screen.getByTestId("item-history-open-logs"));
    expect(onOpen).toHaveBeenCalledWith("logs");
  });

  it("treats HTTP 404 and 403 as errors, not empty success", async () => {
    fetchHistory.mockRejectedValue({ status: 404, statusText: "Not Found" });
    const { rerender } = render(
      <ItemPublishingHistoryPanel currentSection="logs" itemId="missing" />,
    );
    await waitFor(() => {
      expect(screen.getByTestId("item-history-error")).toHaveTextContent(
        /HTTP 404/i,
      );
    });
    expect(screen.queryByTestId("item-history-empty")).toBeNull();
    expect(screen.queryByTestId("item-history-table")).toBeNull();

    fetchHistory.mockRejectedValue({ status: 403, statusText: "Forbidden" });
    rerender(
      <ItemPublishingHistoryPanel currentSection="logs" itemId="denied" />,
    );
    await waitFor(() => {
      expect(screen.getByTestId("item-history-error")).toHaveTextContent(
        /HTTP 403/i,
      );
    });
    expect(screen.queryByTestId("item-history-empty")).toBeNull();
  });

  it("rejects unsafe item ids without calling the API", async () => {
    render(<ItemPublishingHistoryPanel currentSection="logs" />);
    fireEvent.change(screen.getByTestId("item-history-id"), {
      target: { value: "<script>" },
    });
    fireEvent.click(screen.getByTestId("item-history-lookup"));
    await waitFor(() => {
      expect(screen.getByTestId("item-history-error")).toBeTruthy();
    });
    expect(fetchHistory).not.toHaveBeenCalled();
  });
});
