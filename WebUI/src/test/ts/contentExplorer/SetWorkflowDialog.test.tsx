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
import { SetWorkflowDialog } from "../../../main/ts/contentExplorer/SetWorkflowDialog";
import { renderA11yGate } from "./a11y";

const choices = [
  { id: "4", name: "Simple" },
  { id: "7", name: "Local workflow" },
];

describe("SetWorkflowDialog (#5076 / #5155)", () => {
  it("cancel does not save and the dialog is keyboard-labelled", async () => {
    const onSave = vi.fn();
    const onCancel = vi.fn();
    const { container } = render(
      <SetWorkflowDialog
        choices={choices}
        currentId="4"
        busy={false}
        error=""
        onSave={onSave}
        onCancel={onCancel}
      />,
    );
    expect(screen.getByTestId("explorer-set-workflow-dialog")).toBeTruthy();
    expect(screen.queryByTestId("explorer-set-workflow-multi")).toBeNull();
    fireEvent.click(screen.getByTestId("explorer-set-workflow-cancel"));
    expect(onCancel).toHaveBeenCalled();
    expect(onSave).not.toHaveBeenCalled();
    await renderA11yGate(container);
  });

  it("save sends the selected workflow", () => {
    const onSave = vi.fn();
    render(
      <SetWorkflowDialog
        choices={choices}
        currentId="4"
        busy={false}
        error=""
        onSave={onSave}
        onCancel={vi.fn()}
      />,
    );
    fireEvent.change(screen.getByTestId("explorer-set-workflow-select"), {
      target: { value: "7" },
    });
    fireEvent.click(screen.getByTestId("explorer-set-workflow-save"));
    expect(onSave).toHaveBeenCalledWith("7");
  });

  it("names a multi-selection and cancel still does not save", async () => {
    const onSave = vi.fn();
    const onCancel = vi.fn();
    const { container } = render(
      <SetWorkflowDialog
        choices={choices}
        currentId="4"
        busy={false}
        error=""
        selectionCount={2}
        onSave={onSave}
        onCancel={onCancel}
      />,
    );
    expect(screen.getByTestId("explorer-set-workflow-multi")).toHaveTextContent(
      /each selected page and asset/i,
    );
    fireEvent.click(screen.getByTestId("explorer-set-workflow-cancel"));
    expect(onCancel).toHaveBeenCalled();
    expect(onSave).not.toHaveBeenCalled();
    await renderA11yGate(container);
  });
});
