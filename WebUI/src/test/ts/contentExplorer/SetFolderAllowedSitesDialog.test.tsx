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
import { SetFolderAllowedSitesDialog } from "../../../main/ts/contentExplorer/SetFolderAllowedSitesDialog";
import { renderA11yGate } from "./a11y";

const choices = [
  { id: "301", name: "Enterprise" },
  { id: "302", name: "Corporate" },
];

describe("SetFolderAllowedSitesDialog (#5132)", () => {
  it("cancel does not save and the dialog is keyboard-labelled", async () => {
    const onSave = vi.fn();
    const onCancel = vi.fn();
    const { container } = render(
      <SetFolderAllowedSitesDialog
        choices={choices}
        currentSites="301"
        busy={false}
        error=""
        onSave={onSave}
        onCancel={onCancel}
      />,
    );
    expect(screen.getByTestId("explorer-set-folder-allowed-sites-dialog")).toBeTruthy();
    expect(
      screen
        .getByTestId("explorer-set-folder-allowed-sites-dialog")
        .getAttribute("data-current-sites"),
    ).toBe("301");
    fireEvent.click(screen.getByTestId("explorer-set-folder-allowed-sites-cancel"));
    expect(onCancel).toHaveBeenCalled();
    expect(onSave).not.toHaveBeenCalled();
    await renderA11yGate(container);
  });

  it("save sends the checked site ids and an empty selection when all are cleared", () => {
    const onSave = vi.fn();
    render(
      <SetFolderAllowedSitesDialog
        choices={choices}
        currentSites="301"
        busy={false}
        error=""
        onSave={onSave}
        onCancel={vi.fn()}
      />,
    );
    fireEvent.click(screen.getByTestId("explorer-set-folder-allowed-site-302"));
    fireEvent.click(screen.getByTestId("explorer-set-folder-allowed-sites-save"));
    expect(onSave).toHaveBeenCalledWith(["301", "302"]);
    fireEvent.click(screen.getByTestId("explorer-set-folder-allowed-site-301"));
    fireEvent.click(screen.getByTestId("explorer-set-folder-allowed-site-302"));
    fireEvent.click(screen.getByTestId("explorer-set-folder-allowed-sites-save"));
    expect(onSave).toHaveBeenLastCalledWith([]);
  });
});
