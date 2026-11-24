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
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { beforeEach, describe, expect, it, vi } from "vitest";
import type { PSPathItem } from "../../../main/ts/api/contentExplorer/types";
import { ClearScheduledDatesDialog } from "../../../main/ts/contentExplorer/ClearScheduledDatesDialog";
import {
  getItemScheduleDates,
  setItemScheduleDates,
} from "../../../main/ts/contentExplorer/itemScheduleDates";
import { renderA11yGate } from "./a11y";

vi.mock("../../../main/ts/contentExplorer/itemScheduleDates", async (importOriginal) => {
  const mod =
    await importOriginal<
      typeof import("../../../main/ts/contentExplorer/itemScheduleDates")
    >();
  return {
    ...mod,
    getItemScheduleDates: vi.fn(),
    setItemScheduleDates: vi.fn(),
  };
});

const loadDates = vi.mocked(getItemScheduleDates);
const saveDates = vi.mocked(setItemScheduleDates);

function item(): PSPathItem {
  return {
    name: "Home",
    path: "/Sites/Demo/Home",
    type: "percPage",
    id: "42",
  };
}

/**
 * Single wall-clock budget for every polling gate in this file (#7147).
 * Each round trip here is purely microtask-driven (mocked
 * {@code getItemScheduleDates} / {@code setItemScheduleDates} promises →
 * setState → render → focus effect) with no real timers involved, so the
 * gates are deterministic in outcome; the budget is strictly a backstop
 * for loaded-worker scheduling lag. Every {@code waitFor} /
 * {@code findBy} uses it explicitly: the 1s testing-library
 * {@code findBy} default is the most-tripped cliff when hundreds of suite
 * files share the worker host (same rationale as
 * {@code RETRY_ROUND_TRIP_TIMEOUT} in #7122).
 */
const CLEAR_SCHEDULE_ROUND_TRIP_TIMEOUT = 15_000;

/**
 * Per-test wall-clock ceiling (#7147, same load-contention rationale as
 * {@code CONTEXT_MENU_RETRY_TEST_TIMEOUT} in #7122). The sequential
 * backstop gates above plus the axe gate can jointly exceed the 20s
 * global default when the worker host is saturated. The primary gates
 * stay progress-based (single bounded backstop per distinct state);
 * this ceiling is strictly a last-resort backstop.
 */
const CLEAR_SCHEDULE_TEST_TIMEOUT = 30_000;

describe("ClearScheduledDatesDialog (#6998, parent #4530)", () => {
  beforeEach(() => {
    loadDates.mockReset();
    saveDates.mockReset();
    loadDates.mockResolvedValue({
      itemId: "42",
      startDate: "09/18/2026 09:00 am",
      endDate: "09/19/2026 10:00 am",
      comments: "",
    });
  });

  it("shows the loaded dates and clears on confirm", { timeout: CLEAR_SCHEDULE_TEST_TIMEOUT }, async () => {
    const onDone = vi.fn();
    const { container } = render(
      <ClearScheduledDatesDialog item={item()} onDone={onDone} />,
    );
    await waitFor(() => {
      expect(
        screen.getByTestId("explorer-clear-schedule-current"),
      ).toHaveTextContent(/09\/18\/2026/);
    }, { timeout: CLEAR_SCHEDULE_ROUND_TRIP_TIMEOUT });
    fireEvent.click(screen.getByTestId("explorer-clear-schedule-confirm"));
    await waitFor(() => {
      expect(saveDates).toHaveBeenCalledTimes(1);
    }, { timeout: CLEAR_SCHEDULE_ROUND_TRIP_TIMEOUT });
    expect(saveDates).toHaveBeenCalledWith({
      itemId: "42",
      startDate: "",
      endDate: "",
      comments: "",
    });
    await waitFor(() => {
      expect(onDone).toHaveBeenCalledWith(true);
    }, { timeout: CLEAR_SCHEDULE_ROUND_TRIP_TIMEOUT });
    await renderA11yGate(container);
  });

  it("names the empty state when the item has no dates", { timeout: CLEAR_SCHEDULE_TEST_TIMEOUT }, async () => {
    loadDates.mockResolvedValue({
      itemId: "42",
      startDate: "",
      endDate: "",
      comments: "",
    });
    const { container } = render(
      <ClearScheduledDatesDialog item={item()} onDone={vi.fn()} />,
    );
    await waitFor(() => {
      expect(
        screen.getByTestId("explorer-clear-schedule-current"),
      ).toHaveTextContent(/no scheduled dates/i);
    }, { timeout: CLEAR_SCHEDULE_ROUND_TRIP_TIMEOUT });
    await renderA11yGate(container);
  });

  it("cancel and Escape decline without clearing", { timeout: CLEAR_SCHEDULE_TEST_TIMEOUT }, async () => {
    const onDone = vi.fn();
    render(<ClearScheduledDatesDialog item={item()} onDone={onDone} />);
    await waitFor(() => {
      expect(
        screen.getByTestId("explorer-clear-schedule-confirm"),
      ).toBeInTheDocument();
    }, { timeout: CLEAR_SCHEDULE_ROUND_TRIP_TIMEOUT });
    fireEvent.click(screen.getByTestId("explorer-clear-schedule-cancel"));
    expect(onDone).toHaveBeenCalledWith(false);
    expect(saveDates).not.toHaveBeenCalled();

    onDone.mockClear();
    render(<ClearScheduledDatesDialog item={item()} onDone={onDone} />);
    fireEvent.keyDown(window, { key: "Escape" });
    expect(onDone).toHaveBeenCalledWith(false);
    expect(saveDates).not.toHaveBeenCalled();
  });

  it("load failure stays on the dialog", { timeout: CLEAR_SCHEDULE_TEST_TIMEOUT }, async () => {
    loadDates.mockRejectedValue(new Error("denied"));
    const onDone = vi.fn();
    const { container } = render(
      <ClearScheduledDatesDialog item={item()} onDone={onDone} />,
    );
    await waitFor(() => {
      expect(
        screen.getByTestId("explorer-clear-schedule-error"),
      ).toBeInTheDocument();
    }, { timeout: CLEAR_SCHEDULE_ROUND_TRIP_TIMEOUT });
    expect(onDone).not.toHaveBeenCalled();
    expect(
      screen.queryByTestId("explorer-clear-schedule-dialog"),
    ).toBeInTheDocument();
    await renderA11yGate(container);
  });

  it("confirm failure stays on the dialog and does not resolve cleared", { timeout: CLEAR_SCHEDULE_TEST_TIMEOUT }, async () => {
    saveDates.mockRejectedValue(new Error("denied"));
    const onDone = vi.fn();
    render(<ClearScheduledDatesDialog item={item()} onDone={onDone} />);
    await waitFor(() => {
      expect(
        screen.getByTestId("explorer-clear-schedule-confirm"),
      ).toBeInTheDocument();
    }, { timeout: CLEAR_SCHEDULE_ROUND_TRIP_TIMEOUT });
    fireEvent.click(screen.getByTestId("explorer-clear-schedule-confirm"));
    await waitFor(() => {
      expect(
        screen.getByTestId("explorer-clear-schedule-error"),
      ).toBeInTheDocument();
    }, { timeout: CLEAR_SCHEDULE_ROUND_TRIP_TIMEOUT });
    expect(onDone).not.toHaveBeenCalled();
  });

  it("focuses Cancel on open and traps Tab inside the dialog", { timeout: CLEAR_SCHEDULE_TEST_TIMEOUT }, async () => {
    render(<ClearScheduledDatesDialog item={item()} onDone={vi.fn()} />);
    const dialog = await screen.findByTestId("explorer-clear-schedule-dialog", undefined, {
      timeout: CLEAR_SCHEDULE_ROUND_TRIP_TIMEOUT,
    });
    await waitFor(() => {
      expect(screen.getByTestId("explorer-clear-schedule-cancel")).toHaveFocus();
    }, { timeout: CLEAR_SCHEDULE_ROUND_TRIP_TIMEOUT });
    const cancel = screen.getByTestId("explorer-clear-schedule-cancel");
    const confirm = screen.getByTestId("explorer-clear-schedule-confirm");
    confirm.focus();
    expect(confirm).toHaveFocus();
    // Tab from the last control wraps to the first.
    fireEvent.keyDown(dialog, { key: "Tab" });
    expect(document.activeElement).toBe(cancel);
    // Shift+Tab from the first control wraps to the last.
    cancel.focus();
    fireEvent.keyDown(dialog, { key: "Tab", shiftKey: true });
    expect(document.activeElement).toBe(confirm);
  });
});
