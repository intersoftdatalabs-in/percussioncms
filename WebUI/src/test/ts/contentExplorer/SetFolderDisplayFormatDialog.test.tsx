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
import { SetFolderDisplayFormatDialog } from "../../../main/ts/contentExplorer/SetFolderDisplayFormatDialog";
import { renderA11yGate } from "./a11y";

const choices = [
  { id: "3", name: "Default" },
  { id: "12", name: "Simple" },
];

describe("SetFolderDisplayFormatDialog (#5131)", () => {
  it("cancel does not save and the dialog is keyboard-labelled", async () => {
    const onSave = vi.fn();
    const onCancel = vi.fn();
    const { container } = render(
      <SetFolderDisplayFormatDialog
        choices={choices}
        currentId="3"
        busy={false}
        error=""
        onSave={onSave}
        onCancel={onCancel}
      />,
    );
    expect(screen.getByTestId("explorer-set-folder-display-format-dialog")).toBeTruthy();
    expect(
      screen
        .getByTestId("explorer-set-folder-display-format-dialog")
        .getAttribute("data-current-format"),
    ).toBe("3");
    fireEvent.click(screen.getByTestId("explorer-set-folder-display-format-cancel"));
    expect(onCancel).toHaveBeenCalled();
    expect(onSave).not.toHaveBeenCalled();
    await renderA11yGate(container);
  });

  it("save sends the selected display format id", () => {
    const onSave = vi.fn();
    render(
      <SetFolderDisplayFormatDialog
        choices={choices}
        currentId="3"
        busy={false}
        error=""
        onSave={onSave}
        onCancel={vi.fn()}
      />,
    );
    fireEvent.change(screen.getByTestId("explorer-set-folder-display-format-select"), {
      target: { value: "12" },
    });
    fireEvent.click(screen.getByTestId("explorer-set-folder-display-format-save"));
    expect(onSave).toHaveBeenCalledWith("12");
  });

  it("names the multi-folder save and cancel still does not save", async () => {
    const onSave = vi.fn();
    const onCancel = vi.fn();
    const { container } = render(
      <SetFolderDisplayFormatDialog
        choices={choices}
        currentId="3"
        busy={false}
        error=""
        multi
        onSave={onSave}
        onCancel={onCancel}
      />,
    );
    expect(screen.getByTestId("explorer-set-folder-display-format-multi").textContent).toContain(
      "each selected folder",
    );
    fireEvent.click(screen.getByTestId("explorer-set-folder-display-format-cancel"));
    expect(onCancel).toHaveBeenCalled();
    expect(onSave).not.toHaveBeenCalled();
    await renderA11yGate(container);
  });

  it("shows the selected format description and hides the line when absent (#5840)", async () => {
    const described = [
      { id: "3", name: "Default", description: "One row per page." },
      { id: "12", name: "Simple" },
    ];
    const { container } = render(
      <SetFolderDisplayFormatDialog
        choices={described}
        currentId="3"
        busy={false}
        error=""
        onSave={vi.fn()}
        onCancel={vi.fn()}
      />,
    );
    // Initial selection is the first non-current choice ("12"), which has no description.
    expect(
      screen.queryByTestId("explorer-set-folder-display-format-description"),
    ).toBeNull();
    fireEvent.change(screen.getByTestId("explorer-set-folder-display-format-select"), {
      target: { value: "3" },
    });
    expect(
      screen.getByTestId("explorer-set-folder-display-format-description").textContent,
    ).toBe("One row per page.");
    fireEvent.change(screen.getByTestId("explorer-set-folder-display-format-select"), {
      target: { value: "12" },
    });
    expect(
      screen.queryByTestId("explorer-set-folder-display-format-description"),
    ).toBeNull();
    await renderA11yGate(container);
  });
});
