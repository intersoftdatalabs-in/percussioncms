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
import { ChangePageTemplateDialog } from "../../../main/ts/contentExplorer/ChangePageTemplateDialog";
import { renderA11yGate } from "./a11y";

const CHOICES = [
  { id: "101", name: "Article" },
  { id: "202", name: "Blog" },
];

describe("ChangePageTemplateDialog", () => {
  it("saves the selected template and cancel does not", async () => {
    const onSave = vi.fn();
    const onCancel = vi.fn();
    const { container } = render(
      <ChangePageTemplateDialog
        choices={CHOICES}
        currentId="101"
        busy={false}
        error=""
        onSave={onSave}
        onCancel={onCancel}
      />,
    );
    expect(screen.getByTestId("explorer-change-page-template-dialog")).toHaveAttribute(
      "data-current-template-id",
      "101",
    );
    expect(screen.getByTestId("explorer-change-page-template-select")).toHaveValue("202");
    fireEvent.click(screen.getByTestId("explorer-change-page-template-cancel"));
    expect(onCancel).toHaveBeenCalledOnce();
    expect(onSave).not.toHaveBeenCalled();
    fireEvent.click(screen.getByTestId("explorer-change-page-template-save"));
    expect(onSave).toHaveBeenCalledWith("202");
    await renderA11yGate(container);
  });

  it("shows a save error without writing on its own", () => {
    const onSave = vi.fn();
    render(
      <ChangePageTemplateDialog
        choices={CHOICES}
        currentId="101"
        busy={false}
        error="Could not change the page template (HTTP 409)"
        onSave={onSave}
        onCancel={vi.fn()}
      />,
    );
    expect(screen.getByTestId("explorer-change-page-template-dialog-error")).toHaveTextContent(
      "409",
    );
    expect(onSave).not.toHaveBeenCalled();
  });
});
