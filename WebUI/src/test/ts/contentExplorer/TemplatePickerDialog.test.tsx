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
import { TemplatePickerDialog } from "../../../main/ts/contentExplorer/TemplatePickerDialog";
import { renderA11yGate } from "./a11y";

const TEMPLATES = [
  { id: "tpl-a", name: "Home" },
  { id: "tpl-b", name: "Interior" },
];

describe("TemplatePickerDialog", () => {
  it("picks the selected template", () => {
    const onPick = vi.fn();
    render(
      <TemplatePickerDialog
        templates={TEMPLATES}
        onPick={onPick}
        onCancel={vi.fn()}
      />,
    );
    expect(screen.getByTestId("explorer-template-picker")).toBeTruthy();
    fireEvent.change(screen.getByTestId("explorer-template-picker-select"), {
      target: { value: "tpl-b" },
    });
    fireEvent.click(screen.getByTestId("explorer-template-picker-ok"));
    expect(onPick).toHaveBeenCalledWith("tpl-b");
  });

  it("cancels without picking", () => {
    const onCancel = vi.fn();
    const onPick = vi.fn();
    render(
      <TemplatePickerDialog
        templates={TEMPLATES}
        onPick={onPick}
        onCancel={onCancel}
      />,
    );
    fireEvent.click(screen.getByTestId("explorer-template-picker-cancel"));
    expect(onCancel).toHaveBeenCalledTimes(1);
    expect(onPick).not.toHaveBeenCalled();
  });

  it("has no serious a11y violations", async () => {
    const { container } = render(
      <TemplatePickerDialog
        templates={TEMPLATES}
        onPick={vi.fn()}
        onCancel={vi.fn()}
      />,
    );
    await renderA11yGate(container);
  });
});
