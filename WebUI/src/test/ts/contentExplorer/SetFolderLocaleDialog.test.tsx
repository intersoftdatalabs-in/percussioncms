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
import { SetFolderLocaleDialog } from "../../../main/ts/contentExplorer/SetFolderLocaleDialog";
import { renderA11yGate } from "./a11y";

const choices = [
  { code: "en-us", name: "English" },
  { code: "fr-fr", name: "French" },
];

describe("SetFolderLocaleDialog (#5106)", () => {
  it("cancel does not save and the dialog is keyboard-labelled", async () => {
    const onSave = vi.fn();
    const onCancel = vi.fn();
    const { container } = render(
      <SetFolderLocaleDialog
        choices={choices}
        currentCode="en-us"
        busy={false}
        error=""
        onSave={onSave}
        onCancel={onCancel}
      />,
    );
    expect(screen.getByTestId("explorer-set-folder-locale-dialog")).toBeTruthy();
    expect(
      screen.getByTestId("explorer-set-folder-locale-dialog").getAttribute("data-current-locale"),
    ).toBe("en-us");
    fireEvent.click(screen.getByTestId("explorer-set-folder-locale-cancel"));
    expect(onCancel).toHaveBeenCalled();
    expect(onSave).not.toHaveBeenCalled();
    await renderA11yGate(container);
  });

  it("save sends the selected locale code", () => {
    const onSave = vi.fn();
    render(
      <SetFolderLocaleDialog
        choices={choices}
        currentCode="en-us"
        busy={false}
        error=""
        onSave={onSave}
        onCancel={vi.fn()}
      />,
    );
    fireEvent.change(screen.getByTestId("explorer-set-folder-locale-select"), {
      target: { value: "fr-fr" },
    });
    fireEvent.click(screen.getByTestId("explorer-set-folder-locale-save"));
    expect(onSave).toHaveBeenCalledWith("fr-fr");
  });
});
