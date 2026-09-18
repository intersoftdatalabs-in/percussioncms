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

import { fireEvent, render, screen } from "@testing-library/react";
import { describe, expect, it, vi } from "vitest";
import { ScheduleDatesDialog } from "../../../main/ts/contentExplorer/ScheduleDatesDialog";
import { renderA11yGate } from "./a11y";

const CURRENT = {
  itemId: "42",
  startDate: "09/18/2026 09:00 am",
  endDate: "09/19/2026 10:00 am",
  comments: "",
};

describe("ScheduleDatesDialog", () => {
  it("saves converted dates and comments", () => {
    const onSave = vi.fn();
    render(
      <ScheduleDatesDialog
        current={CURRENT}
        onSave={onSave}
        onCancel={vi.fn()}
      />,
    );
    expect(screen.getByTestId("explorer-schedule-start")).toHaveValue(
      "2026-09-18T09:00",
    );
    fireEvent.change(screen.getByTestId("explorer-schedule-comments"), {
      target: { value: "ship it" },
    });
    fireEvent.click(screen.getByTestId("explorer-schedule-save"));
    expect(onSave).toHaveBeenCalledWith({
      itemId: "42",
      startDate: "09/18/2026 09:00 am",
      endDate: "09/19/2026 10:00 am",
      comments: "ship it",
    });
  });

  it("clears dates before save", () => {
    const onSave = vi.fn();
    render(
      <ScheduleDatesDialog
        current={CURRENT}
        onSave={onSave}
        onCancel={vi.fn()}
      />,
    );
    fireEvent.click(screen.getByTestId("explorer-schedule-clear"));
    fireEvent.click(screen.getByTestId("explorer-schedule-save"));
    expect(onSave).toHaveBeenCalledWith({
      itemId: "42",
      startDate: "",
      endDate: "",
      comments: "",
    });
  });

  it("blocks same publish and removal times", () => {
    const onSave = vi.fn();
    render(
      <ScheduleDatesDialog
        current={{
          itemId: "42",
          startDate: "09/18/2026 09:00 am",
          endDate: "09/18/2026 09:00 am",
          comments: "",
        }}
        onSave={onSave}
        onCancel={vi.fn()}
      />,
    );
    fireEvent.click(screen.getByTestId("explorer-schedule-save"));
    expect(onSave).not.toHaveBeenCalled();
    expect(screen.getByTestId("explorer-schedule-dialog-error")).toHaveTextContent(
      /cannot be the same/i,
    );
  });

  it("cancels without saving", () => {
    const onCancel = vi.fn();
    const onSave = vi.fn();
    render(
      <ScheduleDatesDialog
        current={CURRENT}
        onSave={onSave}
        onCancel={onCancel}
      />,
    );
    fireEvent.click(screen.getByTestId("explorer-schedule-cancel"));
    expect(onCancel).toHaveBeenCalledTimes(1);
    expect(onSave).not.toHaveBeenCalled();
  });

  it("cancels on Escape", () => {
    const onCancel = vi.fn();
    render(
      <ScheduleDatesDialog
        current={CURRENT}
        onSave={vi.fn()}
        onCancel={onCancel}
      />,
    );
    fireEvent.keyDown(window, { key: "Escape" });
    expect(onCancel).toHaveBeenCalledTimes(1);
  });

  it("has no serious a11y violations", async () => {
    const { container } = render(
      <ScheduleDatesDialog
        current={CURRENT}
        onSave={vi.fn()}
        onCancel={vi.fn()}
      />,
    );
    await renderA11yGate(container);
  });
});
