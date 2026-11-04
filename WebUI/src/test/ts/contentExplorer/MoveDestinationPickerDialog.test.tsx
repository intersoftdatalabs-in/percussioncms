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
import { MoveDestinationPickerDialog } from "../../../main/ts/contentExplorer/MoveDestinationPickerDialog";
import { renderA11yGate } from "./a11y";

describe("MoveDestinationPickerDialog (#5552)", () => {
  it("refuses a NUL destination before onPick and stays open", async () => {
    const onPick = vi.fn();
    const onCancel = vi.fn();
    const { container } = render(
      <MoveDestinationPickerDialog
        defaultPath="/Assets"
        onPick={onPick}
        onCancel={onCancel}
      />,
    );
    fireEvent.change(screen.getByTestId("explorer-move-dest-input"), {
      target: { value: "/Assets/Dst\u0000" },
    });
    fireEvent.click(screen.getByTestId("explorer-move-dest-ok"));
    expect(onPick).not.toHaveBeenCalled();
    expect(onCancel).not.toHaveBeenCalled();
    expect(screen.getByTestId("explorer-move-dest-picker")).toBeInTheDocument();
    expect(screen.getByTestId("explorer-move-dest-error")).toHaveTextContent(
      /cannot be stored/i,
    );
    await renderA11yGate(container);
  });

  it("picks an ordinary destination path", () => {
    const onPick = vi.fn();
    render(
      <MoveDestinationPickerDialog
        defaultPath="/Assets"
        onPick={onPick}
        onCancel={() => undefined}
      />,
    );
    fireEvent.change(screen.getByTestId("explorer-move-dest-input"), {
      target: { value: "/Assets/Dst" },
    });
    fireEvent.click(screen.getByTestId("explorer-move-dest-ok"));
    expect(onPick).toHaveBeenCalledWith("/Assets/Dst");
    expect(screen.queryByTestId("explorer-move-dest-error")).toBeNull();
  });

  it("cancel does not pick", () => {
    const onPick = vi.fn();
    const onCancel = vi.fn();
    render(
      <MoveDestinationPickerDialog
        defaultPath="/Assets"
        onPick={onPick}
        onCancel={onCancel}
      />,
    );
    fireEvent.click(screen.getByTestId("explorer-move-dest-cancel"));
    expect(onPick).not.toHaveBeenCalled();
    expect(onCancel).toHaveBeenCalledTimes(1);
  });
});
