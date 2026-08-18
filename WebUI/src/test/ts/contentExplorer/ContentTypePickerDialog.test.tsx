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
import { ContentTypePickerDialog } from "../../../main/ts/contentExplorer/ContentTypePickerDialog";
import { renderA11yGate } from "./a11y";

const TYPES = [
  { name: "percFile", label: "File" },
  { name: "rffEvent", label: "Event" },
];

describe("ContentTypePickerDialog", () => {
  it("picks the selected content type", () => {
    const onPick = vi.fn();
    render(
      <ContentTypePickerDialog
        types={TYPES}
        onPick={onPick}
        onCancel={vi.fn()}
      />,
    );
    expect(screen.getByTestId("explorer-type-picker")).toBeTruthy();
    fireEvent.change(screen.getByTestId("explorer-type-picker-select"), {
      target: { value: "rffEvent" },
    });
    fireEvent.click(screen.getByTestId("explorer-type-picker-ok"));
    expect(onPick).toHaveBeenCalledWith("rffEvent");
  });

  it("cancels without picking", () => {
    const onCancel = vi.fn();
    const onPick = vi.fn();
    render(
      <ContentTypePickerDialog
        types={TYPES}
        onPick={onPick}
        onCancel={onCancel}
      />,
    );
    fireEvent.click(screen.getByTestId("explorer-type-picker-cancel"));
    expect(onCancel).toHaveBeenCalledTimes(1);
    expect(onPick).not.toHaveBeenCalled();
  });

  it("cancels on Escape", () => {
    const onCancel = vi.fn();
    render(
      <ContentTypePickerDialog
        types={TYPES}
        onPick={vi.fn()}
        onCancel={onCancel}
      />,
    );
    fireEvent.keyDown(window, { key: "Escape" });
    expect(onCancel).toHaveBeenCalledTimes(1);
  });

  it("moves focus to the type select on mount", () => {
    render(
      <ContentTypePickerDialog
        types={TYPES}
        onPick={vi.fn()}
        onCancel={vi.fn()}
      />,
    );
    expect(document.activeElement).toBe(
      screen.getByTestId("explorer-type-picker-select"),
    );
  });

  it("traps Tab inside the dialog", () => {
    render(
      <ContentTypePickerDialog
        types={TYPES}
        onPick={vi.fn()}
        onCancel={vi.fn()}
      />,
    );
    const ok = screen.getByTestId("explorer-type-picker-ok");
    ok.focus();
    fireEvent.keyDown(screen.getByTestId("explorer-type-picker"), {
      key: "Tab",
    });
    expect(document.activeElement).toBe(
      screen.getByTestId("explorer-type-picker-select"),
    );
  });

  it("has no serious a11y violations", async () => {
    const { container } = render(
      <ContentTypePickerDialog
        types={TYPES}
        onPick={vi.fn()}
        onCancel={vi.fn()}
      />,
    );
    await renderA11yGate(container);
  });
});
