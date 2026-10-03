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
import { SetFolderCommunityDialog } from "../../../main/ts/contentExplorer/SetFolderCommunityDialog";
import { renderA11yGate } from "./a11y";

const choices = [
  { id: "10", name: "Default" },
  { id: "12", name: "Enterprise" },
];

describe("SetFolderCommunityDialog (#5105)", () => {
  it("cancel does not save and the dialog is keyboard-labelled", async () => {
    const onSave = vi.fn();
    const onCancel = vi.fn();
    const { container } = render(
      <SetFolderCommunityDialog
        choices={choices}
        currentId="10"
        busy={false}
        error=""
        onSave={onSave}
        onCancel={onCancel}
      />,
    );
    expect(screen.getByTestId("explorer-set-folder-community-dialog")).toBeTruthy();
    expect(
      screen.getByTestId("explorer-set-folder-community-dialog").getAttribute(
        "data-current-community-id",
      ),
    ).toBe("10");
    fireEvent.click(screen.getByTestId("explorer-set-folder-community-cancel"));
    expect(onCancel).toHaveBeenCalled();
    expect(onSave).not.toHaveBeenCalled();
    await renderA11yGate(container);
  });

  it("save sends the selected community", () => {
    const onSave = vi.fn();
    render(
      <SetFolderCommunityDialog
        choices={choices}
        currentId="10"
        busy={false}
        error=""
        onSave={onSave}
        onCancel={vi.fn()}
      />,
    );
    fireEvent.change(screen.getByTestId("explorer-set-folder-community-select"), {
      target: { value: "12" },
    });
    fireEvent.click(screen.getByTestId("explorer-set-folder-community-save"));
    expect(onSave).toHaveBeenCalledWith("12");
  });
});
