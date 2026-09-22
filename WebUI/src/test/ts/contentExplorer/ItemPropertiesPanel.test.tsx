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

import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { describe, expect, it, vi } from "vitest";
import { ItemPropertiesPanel } from "../../../main/ts/contentExplorer/ItemPropertiesPanel";
import { renderA11yGate } from "./a11y";

describe("ItemPropertiesPanel (#4701)", () => {
  it("loads name/display title and saves", async () => {
    const load = vi.fn().mockResolvedValue({
      name: "Old",
      displayTitle: "Old title",
    });
    const save = vi.fn().mockResolvedValue({
      name: "New",
      displayTitle: "New title",
    });
    const onSaved = vi.fn();
    const { container } = render(
      <ItemPropertiesPanel
        itemPath="/Assets/item"
        canEdit
        load={load}
        save={save}
        onSaved={onSaved}
      />,
    );
    await waitFor(() => {
      expect(screen.getByTestId("item-properties-name")).toBeTruthy();
    });
    await renderA11yGate(container);
    fireEvent.change(screen.getByTestId("item-properties-name"), {
      target: { value: "New" },
    });
    fireEvent.change(screen.getByTestId("item-properties-display-title"), {
      target: { value: "New title" },
    });
    fireEvent.click(screen.getByTestId("item-properties-save"));
    await waitFor(() => {
      expect(save).toHaveBeenCalledWith({
        itemPath: "/Assets/item",
        name: "New",
        displayTitle: "New title",
      });
    });
    expect(onSaved).toHaveBeenCalledWith("New");
  });

  it("view-only disables save", async () => {
    const load = vi.fn().mockResolvedValue({ name: "Old", displayTitle: "" });
    render(
      <ItemPropertiesPanel
        itemPath="/Assets/item"
        canEdit={false}
        load={load}
        save={vi.fn()}
      />,
    );
    await waitFor(() => {
      expect(screen.getByTestId("item-properties-readonly")).toBeTruthy();
    });
    expect(
      (screen.getByTestId("item-properties-save") as HTMLButtonElement).disabled,
    ).toBe(true);
  });
});
