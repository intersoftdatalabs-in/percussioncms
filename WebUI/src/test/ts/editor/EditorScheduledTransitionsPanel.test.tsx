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

import { cleanup, fireEvent, render, screen } from "@testing-library/react";
import React from "react";
import { afterEach, describe, expect, it, vi } from "vitest";
import { EditorScheduledTransitionsPanel } from "../../../main/ts/editor/EditorScheduledTransitionsPanel";

const ENTRIES = [
  { kind: "embargo", value: "09/18/2026 09:00 am" },
  { kind: "expiry", value: "09/19/2026 10:00 am" },
] as const;

describe("EditorScheduledTransitionsPanel (#5830)", () => {
  afterEach(() => {
    cleanup();
  });

  it("lists the embargo and expiry trigger dates", () => {
    render(<EditorScheduledTransitionsPanel entries={[...ENTRIES]} />);
    expect(
      screen.getByTestId("editor-scheduled-transitions-list"),
    ).toBeTruthy();
    expect(
      screen.getByTestId("editor-scheduled-transition-row-embargo"),
    ).toBeTruthy();
    expect(
      screen.getByTestId("editor-scheduled-transition-embargo-date")
        .textContent,
    ).toBe("09/18/2026 09:00 am");
    expect(
      screen.getByTestId("editor-scheduled-transition-expiry-date")
        .textContent,
    ).toBe("09/19/2026 10:00 am");
    expect(
      screen.queryByTestId("editor-scheduled-transitions-empty"),
    ).toBeNull();
  });

  it("shows an explicit empty state instead of a blank panel", () => {
    render(<EditorScheduledTransitionsPanel entries={[]} />);
    expect(
      screen.getByTestId("editor-scheduled-transitions-empty"),
    ).toBeTruthy();
    expect(
      screen.getByTestId("editor-scheduled-transitions-empty").textContent,
    ).toMatch(/no embargo or expiry/i);
    expect(
      screen.queryByTestId("editor-scheduled-transitions-list"),
    ).toBeNull();
  });

  it("shows loading and then a load error with retry", () => {
    const onRetryLoad = vi.fn();
    const { rerender } = render(
      <EditorScheduledTransitionsPanel entries={[]} loading />,
    );
    expect(
      screen.getByTestId("editor-scheduled-transitions-loading"),
    ).toBeTruthy();
    rerender(
      <EditorScheduledTransitionsPanel
        entries={[]}
        loadErrorKey="perc.ui.editor@Could not load the scheduled transitions."
        loadErrorDetail="HTTP 403"
        onRetryLoad={onRetryLoad}
      />,
    );
    expect(
      screen.getByTestId("editor-scheduled-transitions-load-error"),
    ).toBeTruthy();
    expect(
      screen.getByTestId("editor-scheduled-transitions-load-error")
        .textContent,
    ).toMatch(/could not load the scheduled transitions/i);
    expect(
      screen.queryByTestId("editor-scheduled-transitions-empty"),
    ).toBeNull();
    fireEvent.click(
      screen.getByTestId("editor-retry-scheduled-transitions-load"),
    );
    expect(onRetryLoad).toHaveBeenCalledTimes(1);
  });

  it("offers Set and Clear only when the caller allows setting", () => {
    const onSetSchedule = vi.fn();
    const onClearSchedule = vi.fn();
    const { rerender } = render(
      <EditorScheduledTransitionsPanel entries={[...ENTRIES]} />,
    );
    expect(
      screen.queryByTestId("editor-scheduled-transitions-actions"),
    ).toBeNull();
    rerender(
      <EditorScheduledTransitionsPanel
        entries={[...ENTRIES]}
        canSet
        onSetSchedule={onSetSchedule}
        onClearSchedule={onClearSchedule}
      />,
    );
    fireEvent.click(screen.getByTestId("editor-scheduled-transitions-set"));
    fireEvent.click(
      screen.getByTestId("editor-scheduled-transitions-clear"),
    );
    expect(onSetSchedule).toHaveBeenCalledTimes(1);
    expect(onClearSchedule).toHaveBeenCalledTimes(1);
  });
});
