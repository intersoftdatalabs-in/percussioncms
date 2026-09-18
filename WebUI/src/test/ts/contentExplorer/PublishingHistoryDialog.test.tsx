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
import { PublishingHistoryDialog } from "../../../main/ts/contentExplorer/PublishingHistoryDialog";
import { fetchItemPublishingHistory } from "../../../main/ts/api/publishing/itemHistoryApi";
import { renderA11yGate } from "./a11y";

vi.mock("../../../main/ts/api/publishing/itemHistoryApi", () => ({
  fetchItemPublishingHistory: vi.fn(),
}));

const fetchHistory = vi.mocked(fetchItemPublishingHistory);

describe("PublishingHistoryDialog", () => {
  beforeEach(() => {
    fetchHistory.mockReset();
  });

  it("loads the selected item and closes", async () => {
    fetchHistory.mockResolvedValue([]);
    const onClose = vi.fn();
    const { container } = render(
      <PublishingHistoryDialog itemId="42" onClose={onClose} />,
    );
    await waitFor(() => {
      expect(screen.getByTestId("item-history-empty")).toBeTruthy();
    });
    expect(fetchHistory).toHaveBeenCalledWith("42");
    fireEvent.click(screen.getByTestId("explorer-publishing-history-close"));
    expect(onClose).toHaveBeenCalled();
    await renderA11yGate(container);
  });

  it("surfaces HTTP 404 and 403 as errors, not empty success", async () => {
    fetchHistory.mockRejectedValue({ status: 404, statusText: "Not Found" });
    const { rerender } = render(
      <PublishingHistoryDialog itemId="99" onClose={vi.fn()} />,
    );
    await waitFor(() => {
      expect(screen.getByTestId("item-history-error")).toHaveTextContent(
        /HTTP 404/i,
      );
    });
    expect(screen.queryByTestId("item-history-table")).toBeNull();
    expect(screen.queryByTestId("item-history-empty")).toBeNull();

    fetchHistory.mockRejectedValue({ status: 403, statusText: "Forbidden" });
    rerender(<PublishingHistoryDialog itemId="7" onClose={vi.fn()} />);
    await waitFor(() => {
      expect(screen.getByTestId("item-history-error")).toHaveTextContent(
        /HTTP 403/i,
      );
    });
    expect(screen.queryByTestId("item-history-empty")).toBeNull();
  });
});
