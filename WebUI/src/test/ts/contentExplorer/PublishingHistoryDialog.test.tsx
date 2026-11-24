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

/**
 * Single wall-clock budget for every polling gate in this file (#7226).
 * Each round trip here is purely microtask-driven (mocked promises
 * resolve, then setState, then render) with no real timers involved,
 * so the gates are deterministic in outcome; the budget is strictly a
 * backstop for loaded-worker scheduling lag. Every {@code findBy} and
 * {@code waitFor} uses it explicitly: the 1s/5s testing-library
 * defaults are the most-tripped cliffs when hundreds of suite files
 * share the worker host (same rationale as {@code RETRY_ROUND_TRIP_TIMEOUT}
 * in #7122 and the #7147 bin-lifecycle slice).
 */
const PUBLISHING_HISTORY_DIALOG_ROUND_TRIP_TIMEOUT = 15_000;

/**
 * Per-test wall-clock ceiling (#7226, same load-contention rationale as
 * the #7207/#7147 slices). The backstop gates above plus the axe gate can
 * jointly exceed the 20s global default when the worker host is
 * saturated. The primary gates stay progress-based (a single bounded
 * backstop per distinct state); this ceiling is strictly a
 * last-resort backstop.
 */
const PUBLISHING_HISTORY_DIALOG_TEST_TIMEOUT = 30_000;

vi.mock("../../../main/ts/api/publishing/itemHistoryApi", () => ({
  fetchItemPublishingHistory: vi.fn(),
}));

const fetchHistory = vi.mocked(fetchItemPublishingHistory);

describe("PublishingHistoryDialog", () => {
  beforeEach(() => {
    fetchHistory.mockReset();
  });

  it("loads the selected item and closes", { timeout: PUBLISHING_HISTORY_DIALOG_TEST_TIMEOUT }, async () => {
    fetchHistory.mockResolvedValue([]);
    const onClose = vi.fn();
    const { container } = render(
      <PublishingHistoryDialog itemId="42" onClose={onClose} />,
    );
    await waitFor(() => {
      expect(screen.getByTestId("item-history-empty")).toBeTruthy();
    },
      { timeout: PUBLISHING_HISTORY_DIALOG_ROUND_TRIP_TIMEOUT },
    );
    expect(fetchHistory).toHaveBeenCalledWith("42");
    fireEvent.click(screen.getByTestId("explorer-publishing-history-close"));
    expect(onClose).toHaveBeenCalled();
    await renderA11yGate(container);
  });

  it("surfaces HTTP 404 and 403 as errors, not empty success", { timeout: PUBLISHING_HISTORY_DIALOG_TEST_TIMEOUT }, async () => {
    fetchHistory.mockRejectedValue({ status: 404, statusText: "Not Found" });
    const { rerender } = render(
      <PublishingHistoryDialog itemId="99" onClose={vi.fn()} />,
    );
    await waitFor(() => {
      expect(screen.getByTestId("item-history-error")).toHaveTextContent(
        /HTTP 404/i,
      );
    },
      { timeout: PUBLISHING_HISTORY_DIALOG_ROUND_TRIP_TIMEOUT },
    );
    expect(screen.queryByTestId("item-history-table")).toBeNull();
    expect(screen.queryByTestId("item-history-empty")).toBeNull();

    fetchHistory.mockRejectedValue({ status: 403, statusText: "Forbidden" });
    rerender(<PublishingHistoryDialog itemId="7" onClose={vi.fn()} />);
    await waitFor(() => {
      expect(screen.getByTestId("item-history-error")).toHaveTextContent(
        /HTTP 403/i,
      );
    },
      { timeout: PUBLISHING_HISTORY_DIALOG_ROUND_TRIP_TIMEOUT },
    );
    expect(screen.queryByTestId("item-history-empty")).toBeNull();
  });

  it("returns focus to the opener when the dialog unmounts", { timeout: PUBLISHING_HISTORY_DIALOG_TEST_TIMEOUT }, async () => {
    fetchHistory.mockResolvedValue([]);
    render(
      <button type="button" data-testid="history-opener">
        History
      </button>,
    );
    const opener = screen.getByTestId("history-opener");
    opener.focus();
    expect(document.activeElement).toBe(opener);
    const dialog = render(
      <PublishingHistoryDialog itemId="42" onClose={vi.fn()} />,
    );
    await waitFor(() => {
      expect(screen.getByTestId("item-history-empty")).toBeTruthy();
    },
      { timeout: PUBLISHING_HISTORY_DIALOG_ROUND_TRIP_TIMEOUT },
    );
    expect(document.activeElement).toBe(
      screen.getByTestId("explorer-publishing-history-close"),
    );
    dialog.unmount();
    expect(document.activeElement).toBe(opener);
  });
});
