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

function shownTitle(): string {
  return (
    screen.getByTestId("item-properties-shown-display-title-value").textContent ??
    ""
  );
}

describe("ItemPropertiesPanel (#4701)", () => {
  it("loads name/display title and saves", async () => {
    const load = vi
      .fn()
      .mockResolvedValueOnce({
        name: "Old",
        displayTitle: "Old title",
      })
      .mockResolvedValueOnce({
        name: "New",
        displayTitle: "New title",
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
    await waitFor(() => {
      expect(onSaved).toHaveBeenCalledWith("New");
    });
    expect(shownTitle()).toBe("New title");
    expect(load).toHaveBeenCalledTimes(2);
    expect(load).toHaveBeenNthCalledWith(2, "/Assets/New");
  });

  it("shows a display title only after reload and does not rename (#5246)", async () => {
    let releaseReload: (value: { name: string; displayTitle: string }) => void =
      () => undefined;
    const reload = new Promise<{ name: string; displayTitle: string }>(
      (resolve) => {
        releaseReload = resolve;
      },
    );
    const load = vi
      .fn()
      .mockResolvedValueOnce({ name: "qa-item", displayTitle: "Old title" })
      .mockImplementationOnce(() => reload);
    const save = vi.fn().mockResolvedValue({
      name: "qa-item",
      displayTitle: "New title",
    });
    const onSaved = vi.fn();
    render(
      <ItemPropertiesPanel
        itemPath="/Assets/item"
        canEdit
        load={load}
        save={save}
        onSaved={onSaved}
      />,
    );
    await waitFor(() => {
      expect(shownTitle()).toBe("Old title");
    });
    fireEvent.change(screen.getByTestId("item-properties-display-title"), {
      target: { value: "New title" },
    });
    expect(shownTitle()).toBe("Old title");
    fireEvent.click(screen.getByTestId("item-properties-save"));
    await waitFor(() => {
      expect(save).toHaveBeenCalledWith({
        itemPath: "/Assets/item",
        name: "qa-item",
        displayTitle: "New title",
      });
    });
    expect(shownTitle()).toBe("Old title");
    expect(load).toHaveBeenNthCalledWith(2, "/Assets/item");
    expect(onSaved).not.toHaveBeenCalled();
    expect(
      (screen.getByTestId("item-properties-name") as HTMLInputElement).value,
    ).toBe("qa-item");
    releaseReload({ name: "qa-item", displayTitle: "From server" });
    await waitFor(() => {
      expect(shownTitle()).toBe("From server");
    });
    expect(
      (screen.getByTestId("item-properties-display-title") as HTMLInputElement)
        .value,
    ).toBe("From server");
    expect(
      (screen.getByTestId("item-properties-name") as HTMLInputElement).value,
    ).toBe("qa-item");
    expect(onSaved).toHaveBeenCalledWith("qa-item");
  });

  it("cancel does not write and restores the display title (#5246)", async () => {
    const load = vi.fn().mockResolvedValue({
      name: "qa-item",
      displayTitle: "Old title",
    });
    const save = vi.fn();
    render(
      <ItemPropertiesPanel
        itemPath="/Assets/item"
        canEdit
        load={load}
        save={save}
      />,
    );
    await waitFor(() => {
      expect(shownTitle()).toBe("Old title");
    });
    fireEvent.change(screen.getByTestId("item-properties-display-title"), {
      target: { value: "Draft title" },
    });
    fireEvent.click(screen.getByTestId("item-properties-cancel"));
    expect(save).not.toHaveBeenCalled();
    expect(
      (screen.getByTestId("item-properties-display-title") as HTMLInputElement)
        .value,
    ).toBe("Old title");
    expect(shownTitle()).toBe("Old title");
  });

  it.each([400, 403, 409])(
    "HTTP %s leaves the previous display title in place (#5246)",
    async (status) => {
      const load = vi.fn().mockResolvedValue({
        name: "qa-item",
        displayTitle: "Old title",
      });
      const save = vi.fn().mockRejectedValue({
        status,
        statusText: String(status),
        body: null,
      });
      const onSaved = vi.fn();
      render(
        <ItemPropertiesPanel
          itemPath="/Assets/item"
          canEdit
          load={load}
          save={save}
          onSaved={onSaved}
        />,
      );
      await waitFor(() => {
        expect(shownTitle()).toBe("Old title");
      });
      fireEvent.change(screen.getByTestId("item-properties-display-title"), {
        target: { value: "Rejected title" },
      });
      fireEvent.click(screen.getByTestId("item-properties-save"));
      await waitFor(() => {
        expect(screen.getByTestId("item-properties-status")).toBeTruthy();
      });
      expect(
        (screen.getByTestId("item-properties-display-title") as HTMLInputElement)
          .value,
      ).toBe("Old title");
      expect(shownTitle()).toBe("Old title");
      expect(onSaved).not.toHaveBeenCalled();
      expect(load).toHaveBeenCalledTimes(1);
    },
  );

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
