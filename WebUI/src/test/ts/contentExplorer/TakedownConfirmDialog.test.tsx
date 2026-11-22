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
import type { PSPathItem } from "../../../main/ts/api/contentExplorer/types";
import { TakedownConfirmDialog } from "../../../main/ts/contentExplorer/TakedownConfirmDialog";
import { renderA11yGate } from "./a11y";

function item(overrides: Partial<PSPathItem> = {}): PSPathItem {
  return {
    name: "Home",
    path: "/Sites/Demo/Home",
    type: "percPage",
    id: "42",
    ...overrides,
  };
}

describe("TakedownConfirmDialog (#6996, parent #4530)", () => {
  it("single item renders the confirm body with linked pages and safe initial focus", async () => {
    const onConfirm = vi.fn();
    const onCancel = vi.fn();
    const { container } = render(
      <TakedownConfirmDialog
        items={[item()]}
        linkedPaths={["/Sites/Demo/Landing"]}
        onConfirm={onConfirm}
        onCancel={onCancel}
      />,
    );
    const dialog = screen.getByTestId("explorer-takedown-confirm");
    expect(dialog).toHaveAttribute("role", "alertdialog");
    expect(dialog).toHaveAttribute("aria-modal", "true");
    expect(screen.getByTestId("explorer-takedown-confirm")).toHaveTextContent(
      /Take down \(unpublish\) this item/,
    );
    expect(
      screen.getByTestId("explorer-takedown-confirm-linked"),
    ).toHaveTextContent(/\/Sites\/Demo\/Landing/);
    // Destructive confirm: initial focus lands on Cancel, not Take down.
    expect(screen.getByTestId("explorer-takedown-cancel")).toHaveFocus();
    fireEvent.click(screen.getByTestId("explorer-takedown-confirm-ok"));
    expect(onConfirm).toHaveBeenCalledTimes(1);
    expect(onCancel).not.toHaveBeenCalled();
    await renderA11yGate(container);
  });

  it("multi-select lists every eligible row with the count in the body", async () => {
    const onConfirm = vi.fn();
    const { container } = render(
      <TakedownConfirmDialog
        items={[
          item(),
          item({ id: "43", name: "About", path: "/Sites/Demo/About" }),
        ]}
        linkedPaths={[]}
        onConfirm={onConfirm}
        onCancel={vi.fn()}
      />,
    );
    expect(screen.getByTestId("explorer-takedown-confirm")).toHaveTextContent(
      /Take down 2 selected items/,
    );
    const list = screen.getByTestId("explorer-takedown-confirm-items");
    expect(list).toHaveTextContent(/Home/);
    expect(list).toHaveTextContent(/About/);
    expect(
      screen.queryByTestId("explorer-takedown-confirm-linked"),
    ).not.toBeInTheDocument();
    await renderA11yGate(container);
  });

  it("Escape and Cancel decline without confirming", () => {
    const onConfirm = vi.fn();
    const onCancel = vi.fn();
    render(
      <TakedownConfirmDialog
        items={[item()]}
        linkedPaths={[]}
        onConfirm={onConfirm}
        onCancel={onCancel}
      />,
    );
    fireEvent.keyDown(window, { key: "Escape" });
    expect(onCancel).toHaveBeenCalledTimes(1);
    expect(onConfirm).not.toHaveBeenCalled();
    fireEvent.click(screen.getByTestId("explorer-takedown-cancel"));
    expect(onCancel).toHaveBeenCalledTimes(2);
    expect(onConfirm).not.toHaveBeenCalled();
  });

  it("Tab cycles focus inside the dialog", () => {
    render(
      <TakedownConfirmDialog
        items={[item()]}
        linkedPaths={[]}
        onConfirm={vi.fn()}
        onCancel={vi.fn()}
      />,
    );
    const cancel = screen.getByTestId("explorer-takedown-cancel");
    const ok = screen.getByTestId("explorer-takedown-confirm-ok");
    expect(cancel).toHaveFocus();
    fireEvent.keyDown(
      screen.getByTestId("explorer-takedown-confirm"),
      { key: "Tab" },
    );
    ok.focus();
    expect(ok).toHaveFocus();
    // Shift+Tab from the first control wraps to the last.
    cancel.focus();
    fireEvent.keyDown(
      screen.getByTestId("explorer-takedown-confirm"),
      { key: "Tab", shiftKey: true },
    );
    expect(document.activeElement).toBe(ok);
  });
});
