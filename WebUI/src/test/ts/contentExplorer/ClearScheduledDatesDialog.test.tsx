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

  it("shows the loaded dates and clears on confirm", async () => {
    const onDone = vi.fn();
    const { container } = render(
      <ClearScheduledDatesDialog item={item()} onDone={onDone} />,
    );
    await waitFor(() => {
      expect(
        screen.getByTestId("explorer-clear-schedule-current"),
      ).toHaveTextContent(/09\/18\/2026/);
    });
    fireEvent.click(screen.getByTestId("explorer-clear-schedule-confirm"));
    await waitFor(() => {
      expect(saveDates).toHaveBeenCalledTimes(1);
    });
    expect(saveDates).toHaveBeenCalledWith({
      itemId: "42",
      startDate: "",
      endDate: "",
      comments: "",
    });
    await waitFor(() => {
      expect(onDone).toHaveBeenCalledWith(true);
    });
    await renderA11yGate(container);
  });

  it("names the empty state when the item has no dates", async () => {
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
    });
    await renderA11yGate(container);
  });

  it("cancel and Escape decline without clearing", async () => {
    const onDone = vi.fn();
    render(<ClearScheduledDatesDialog item={item()} onDone={onDone} />);
    await waitFor(() => {
      expect(
        screen.getByTestId("explorer-clear-schedule-confirm"),
      ).toBeInTheDocument();
    });
    fireEvent.click(screen.getByTestId("explorer-clear-schedule-cancel"));
    expect(onDone).toHaveBeenCalledWith(false);
    expect(saveDates).not.toHaveBeenCalled();

    onDone.mockClear();
    render(<ClearScheduledDatesDialog item={item()} onDone={onDone} />);
    fireEvent.keyDown(window, { key: "Escape" });
    expect(onDone).toHaveBeenCalledWith(false);
    expect(saveDates).not.toHaveBeenCalled();
  });

  it("load failure stays on the dialog", async () => {
    loadDates.mockRejectedValue(new Error("denied"));
    const onDone = vi.fn();
    const { container } = render(
      <ClearScheduledDatesDialog item={item()} onDone={onDone} />,
    );
    await waitFor(() => {
      expect(
        screen.getByTestId("explorer-clear-schedule-error"),
      ).toBeInTheDocument();
    });
    expect(onDone).not.toHaveBeenCalled();
    expect(
      screen.queryByTestId("explorer-clear-schedule-dialog"),
    ).toBeInTheDocument();
    await renderA11yGate(container);
  });

  it("confirm failure stays on the dialog and does not resolve cleared", async () => {
    saveDates.mockRejectedValue(new Error("denied"));
    const onDone = vi.fn();
    render(<ClearScheduledDatesDialog item={item()} onDone={onDone} />);
    await waitFor(() => {
      expect(
        screen.getByTestId("explorer-clear-schedule-confirm"),
      ).toBeInTheDocument();
    });
    fireEvent.click(screen.getByTestId("explorer-clear-schedule-confirm"));
    await waitFor(() => {
      expect(
        screen.getByTestId("explorer-clear-schedule-error"),
      ).toBeInTheDocument();
    });
    expect(onDone).not.toHaveBeenCalled();
  });

  it("focuses Cancel on open and traps Tab inside the dialog", async () => {
    render(<ClearScheduledDatesDialog item={item()} onDone={vi.fn()} />);
    const dialog = await screen.findByTestId("explorer-clear-schedule-dialog");
    await waitFor(() => {
      expect(screen.getByTestId("explorer-clear-schedule-cancel")).toHaveFocus();
    });
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
